"""
compare_scenarios_infeasible_global.py

Same idea as compare_scenarios_global.py, but for INFEASIBLE scenario
exports (scenarios_out_infeasible). There is no
ground_truth_optimal_weights key here -- the network genuinely has no
optimal solution -- so this script only compares:

  1. AI model (with even-split fallback for any invalid output)
  2. Even split (naive baseline)
  3. Single path (naive baseline)

...against each other, using the same global load accumulation and
per-node breakdown as the feasible comparison script, so you get the
same "Overall Max" style output your Java sim prints, but there's no
"Optimal (LP ground truth)" block to compare against because none
exists for an infeasible run.

Usage:
    python compare_scenarios_infeasible_global.py /path/to/scenarios_out_infeasible
"""

import os
import sys
import json
import glob
from collections import defaultdict
from dotenv import load_dotenv

load_dotenv()
os.environ["HF_HUB_ENABLE_HF_TRANSFER"] = "0"

try:
    from google.colab import userdata
    hf_token = userdata.get("HF_TOKEN")
except ImportError:
    hf_token = os.environ.get("HF_TOKEN")

if not hf_token:
    raise RuntimeError("No HF token found. Set HF_TOKEN env var first.")

from unsloth import FastLanguageModel

print("Loading base model + adapter...")
model, tokenizer = FastLanguageModel.from_pretrained(
    model_name="unsloth/Llama-3.2-3B-Instruct",
)
model.load_adapter("Zii05/sdn-loadbalance-llama-lora", token=hf_token)
FastLanguageModel.for_inference(model)
if tokenizer.pad_token_id is None:
    tokenizer.pad_token = tokenizer.eos_token
print("Model loaded.\n")

SYSTEM_PROMPT = (
    "You are a network load-balancing assistant for a hybrid "
    "SDN-label switching system. Given a policy and the current "
    "network demand, output the traffic weight distribution across "
    "the listed alternative paths in the exact JSON format shown in "
    "examples. Only use path_ids listed in the input. Weights must "
    "sum to 1."
)


def normalize_plan(plan):
    assignments = plan.get("assignments", [])
    total = sum(a["weight"] for a in assignments)
    if total <= 0:
        return None
    for a in assignments:
        a["weight"] = round(a["weight"] / total, 4)
    return plan


def ask_model(scenario_dict):
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": json.dumps({
            "edge_switch": scenario_dict["edge_switch"],
            "policy": scenario_dict["policy"],
            "alternative_paths": scenario_dict["alternative_paths"],
            "total_traffic": scenario_dict["total_traffic"],
        })}
    ]
    inputs = tokenizer.apply_chat_template(
        messages, tokenize=True, add_generation_prompt=True, return_tensors="pt"
    ).to("cuda")
    attention_mask = (inputs != tokenizer.pad_token_id).long()

    n_paths = len(scenario_dict["alternative_paths"])
    max_new_tokens = min(1500, 150 + 30 * n_paths)

    outputs = model.generate(
        inputs,
        attention_mask=attention_mask,
        max_new_tokens=max_new_tokens,
        do_sample=False,
    )
    response = tokenizer.decode(outputs[0][inputs.shape[1]:], skip_special_tokens=True)
    try:
        plan = json.loads(response)
        return normalize_plan(plan)
    except json.JSONDecodeError:
        return None


def is_valid_plan(plan, scenario):
    if not plan:
        return False, "not valid JSON / normalization failed"
    assignments = plan.get("assignments", [])
    if not assignments:
        return False, "no assignments key"
    weights_sum = sum(a.get("weight", 0) for a in assignments)
    if not (0.9 <= weights_sum <= 1.1):
        return False, f"weights sum to {weights_sum:.3f}, not ~1"
    valid_ids = {p["id"] for p in scenario["alternative_paths"]}
    used_ids = {a.get("path_id") for a in assignments}
    if not used_ids.issubset(valid_ids):
        return False, f"used invalid path_ids: {used_ids - valid_ids}"
    return True, "ok"


def even_split_weights(scenario):
    paths = scenario["alternative_paths"]
    n = len(paths)
    return {p["id"]: 1.0 / n for p in paths}


def single_path_weights(scenario):
    paths = scenario["alternative_paths"]
    return {paths[0]["id"]: 1.0}


def accumulate(global_load, weights_by_path_id, scenario):
    path_mb = {p["id"]: p["middleboxes"] for p in scenario["alternative_paths"]}
    total_traffic = scenario["total_traffic"]
    for path_id, weight in weights_by_path_id.items():
        traffic_on_path = weight * total_traffic
        for mb in path_mb.get(path_id, []):
            global_load[mb] += traffic_on_path


def run():
    if len(sys.argv) < 2:
        print("Usage: python compare_scenarios_infeasible_global.py /path/to/scenarios_out_infeasible")
        sys.exit(1)

    scenario_dir = sys.argv[1]
    files = sorted(glob.glob(os.path.join(scenario_dir, "*.json")))
    files = [f for f in files if not f.endswith("comparison_results.json")]
    if not files:
        print(f"No scenario .json files found in {scenario_dir}")
        sys.exit(1)

    print(f"Found {len(files)} scenario files (infeasible network state).\n")

    global_load_ai = defaultdict(float)
    global_load_even = defaultdict(float)
    global_load_single = defaultdict(float)

    invalid_count = 0

    for i, filepath in enumerate(files):
        with open(filepath) as f:
            scenario = json.load(f)

        print(f"[{i+1}/{len(files)}] {os.path.basename(filepath)}...", end=" ")

        ai_plan = ask_model(scenario)
        valid, reason = is_valid_plan(ai_plan, scenario)

        even_weights = even_split_weights(scenario)
        single_weights = single_path_weights(scenario)

        accumulate(global_load_even, even_weights, scenario)
        accumulate(global_load_single, single_weights, scenario)

        if valid:
            ai_weights = {a["path_id"]: a["weight"] for a in ai_plan["assignments"]}
            accumulate(global_load_ai, ai_weights, scenario)
            print("ok")
        else:
            # Fall back to even split for this pool so one bad output
            # doesn't just silently vanish from the AI's global total.
            accumulate(global_load_ai, even_weights, scenario)
            invalid_count += 1
            print(f"INVALID ({reason}) -- using even split as fallback for this pool")

    def mb_type(mb_id):
        for prefix in ("FW", "IDS", "TM", "WP"):
            if mb_id.startswith(prefix):
                return prefix
        return None

    def mb_sort_key(mb_id):
        digits = "".join(c for c in mb_id if c.isdigit())
        return int(digits) if digits else 0

    def print_block(label, load_map):
        by_type = defaultdict(list)
        for mb_id, load in load_map.items():
            t = mb_type(mb_id)
            if t:
                by_type[t].append((mb_id, load))

        print(f"\n{label}")
        overall_max_vals = []
        overall_min_vals = []
        for t in ("FW", "IDS", "TM", "WP"):
            nodes = sorted(by_type.get(t, []), key=lambda x: mb_sort_key(x[0]))
            if not nodes:
                print(f"  {t}: n/a")
                continue
            for mb_id, load in nodes:
                print(f"  {mb_id:6s} {load:>12,.0f}")
            t_min = min(load for _, load in nodes)
            t_max = max(load for _, load in nodes)
            print(f"  {t:3s} Min: {t_min:,.0f}  Max: {t_max:,.0f}")
            overall_max_vals.append(t_max)
            overall_min_vals.append(t_min)

        overall_max = max(overall_max_vals) if overall_max_vals else 0
        overall_min = min(overall_min_vals) if overall_min_vals else 0
        print(f"Overall Max: {overall_max:,.0f}")
        print(f"Overall Min: {overall_min:,.0f}")
        return overall_max

    print("\n" + "=" * 70)
    print(f"NETWORK-WIDE COMPARISON -- INFEASIBLE STATE  "
          f"({len(files) - invalid_count} valid AI outputs / {len(files)} scenarios)")
    print("=" * 70)
    print("No 'Optimal (LP ground truth)' block here -- the network is genuinely")
    print("overloaded for this saved state, so no LP optimum exists to compare to.")
    print("This only ranks the AI model against the two naive baselines.")

    overall_max_single = print_block("SINGLE PATH (naive baseline)", global_load_single)
    overall_max_even = print_block("EVEN SPLIT (naive baseline)", global_load_even)
    overall_max_ai = print_block("AI MODEL", global_load_ai)

    ranked = sorted(
        [("AI model", overall_max_ai),
         ("Even split", overall_max_even),
         ("Single path", overall_max_single)],
        key=lambda x: x[1]
    )
    ai_rank = [name for name, _ in ranked].index("AI model") + 1

    print("\n" + "=" * 70)
    print("RANKING (lower Overall Max = better)")
    print("=" * 70)
    for i, (name, load) in enumerate(ranked, 1):
        marker = "  <-- AI" if name == "AI model" else ""
        print(f"  #{i}  {name:12s}  Overall Max: {load:>12,.0f}{marker}")

    print(f"\n>>> AI model ranks #{ai_rank}/3 network-wide (no LP optimum available) <<<")

    if invalid_count > 0:
        print(f"\nNote: {invalid_count} scenario(s) had invalid AI output and were "
              f"scored with an even-split fallback for that pool, which is generous "
              f"to the AI's ranking -- worth checking which ones failed.")


if __name__ == "__main__":
    run()