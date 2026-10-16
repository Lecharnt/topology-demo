"""
compare_scenarios.py

Runs every scenario JSON exported by ScenarioExporter.java through the
AI fallback model and compares its predicted weight split against:
  - Ground truth: the LP's actual optimal split (best possible)
  - Even split:   naive baseline, equal weight on every path
  - Single path:  naive baseline, dump all traffic on the first path

For each scenario we compute the resulting MAX PER-MIDDLEBOX LOAD under
each strategy (lower is better — that's literally what OptimalLP is
minimizing via lambda). Then we rank the 4 candidates per scenario and
see how often the AI comes in 2nd (i.e. best non-optimal strategy).

Usage:
    python compare_scenarios.py /path/to/scenarios_out
"""

import os
import sys
import json
import glob
from collections import defaultdict
from dotenv import load_dotenv

load_dotenv()
os.environ["HF_HUB_ENABLE_HF_TRANSFER"] = "0"

# -----------------------------------------------------------------
# Load token + model (same as test_sdn_model.py)
# -----------------------------------------------------------------
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
    outputs = model.generate(inputs, max_new_tokens=1500, temperature=0.1)
    response = tokenizer.decode(outputs[0][inputs.shape[1]:], skip_special_tokens=True)
    try:
        plan = json.loads(response)
        plan = normalize_plan(plan)
        return plan
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


def max_load_from_weights(weights_by_path_id, scenario):
    """Given {path_id: weight}, compute the resulting max load on any single
    middlebox, assuming total_traffic is split by those weights."""
    load = defaultdict(float)
    total_traffic = scenario["total_traffic"]
    path_mb = {p["id"]: p["middleboxes"] for p in scenario["alternative_paths"]}
    for path_id, weight in weights_by_path_id.items():
        traffic_on_path = weight * total_traffic
        for mb in path_mb.get(path_id, []):
            load[mb] += traffic_on_path
    return max(load.values()) if load else 0.0


def even_split_weights(scenario):
    paths = scenario["alternative_paths"]
    n = len(paths)
    return {p["id"]: 1.0 / n for p in paths}


def single_path_weights(scenario):
    paths = scenario["alternative_paths"]
    return {paths[0]["id"]: 1.0}


def run():
    if len(sys.argv) < 2:
        print("Usage: python compare_scenarios.py /path/to/scenarios_out")
        sys.exit(1)

    scenario_dir = sys.argv[1]
    files = sorted(glob.glob(os.path.join(scenario_dir, "*.json")))
    if not files:
        print(f"No .json files found in {scenario_dir}")
        sys.exit(1)

    print(f"Found {len(files)} scenario files.\n")

    rank_counts = defaultdict(int)  # how many times AI ranks 1st/2nd/3rd/4th
    invalid_count = 0
    results_rows = []

    for i, filepath in enumerate(files):
        with open(filepath) as f:
            scenario = json.load(f)

        n_paths = len(scenario["alternative_paths"])
        print(f"[{i+1}/{len(files)}] {os.path.basename(filepath)} ({n_paths} paths, "
              f"traffic={scenario['total_traffic']})...", end=" ")

        ai_plan = ask_model(scenario)
        valid, reason = is_valid_plan(ai_plan, scenario)

        if not valid:
            invalid_count += 1
            print(f"INVALID ({reason})")
            continue

        ai_weights = {a["path_id"]: a["weight"] for a in ai_plan["assignments"]}
        gt_weights = scenario["ground_truth_optimal_weights"]

        ai_max_load = max_load_from_weights(ai_weights, scenario)
        gt_max_load = max_load_from_weights(gt_weights, scenario)
        even_max_load = max_load_from_weights(even_split_weights(scenario), scenario)
        single_max_load = max_load_from_weights(single_path_weights(scenario), scenario)

        candidates = [
            ("Optimal LP", gt_max_load),
            ("AI model", ai_max_load),
            ("Even split", even_max_load),
            ("Single path", single_max_load),
        ]
        candidates.sort(key=lambda x: x[1])  # lower max load = better

        ai_rank = [name for name, _ in candidates].index("AI model") + 1
        rank_counts[ai_rank] += 1

        print(f"AI ranked #{ai_rank}/4 (max loads: "
              + ", ".join(f"{name}={load:.0f}" for name, load in candidates) + ")")

        results_rows.append({
            "file": os.path.basename(filepath),
            "n_paths": n_paths,
            "ai_rank": ai_rank,
            "optimal_max_load": gt_max_load,
            "ai_max_load": ai_max_load,
            "even_split_max_load": even_max_load,
            "single_path_max_load": single_max_load,
        })

    print("\n" + "=" * 60)
    print(f"SUMMARY  ({len(files) - invalid_count} valid / {len(files)} total scenarios)")
    print("=" * 60)
    for rank in [1, 2, 3, 4]:
        label = {1: "1st (beat everything, incl. optimal — check for LP tie)",
                  2: "2nd (best non-optimal — this is the target outcome)",
                  3: "3rd",
                  4: "4th (worst)"}[rank]
        print(f"  Rank {rank}: {rank_counts[rank]} scenarios  -- {label}")
    print(f"  Invalid AI outputs: {invalid_count}")

    out_path = os.path.join(scenario_dir, "comparison_results.json")
    with open(out_path, "w") as f:
        json.dump(results_rows, f, indent=2)
    print(f"\nFull per-scenario results saved to {out_path}")


if __name__ == "__main__":
    run()