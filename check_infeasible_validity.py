"""
check_infeasible_validity.py

For infeasible scenarios there's no ground-truth optimal split to compare
against -- the network is genuinely overloaded, so "optimal" doesn't exist.
This script only checks whether the AI still returns a structurally sane
response (valid JSON, weights sum to ~1, only uses real path ids) even when
there's no good answer to give -- a basic robustness/sanity check rather
than an accuracy test.

Usage:
    python check_infeasible_validity.py /path/to/scenarios_out/infeasible_1
"""

import os
import sys
import json
import glob
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
        return normalize_plan(plan), response
    except json.JSONDecodeError:
        return None, response


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


def run():
    if len(sys.argv) < 2:
        print("Usage: python check_infeasible_validity.py /path/to/scenarios_out/infeasible_X")
        sys.exit(1)

    scenario_dir = sys.argv[1]
    files = sorted(glob.glob(os.path.join(scenario_dir, "*.json")))
    if not files:
        print(f"No scenario .json files found in {scenario_dir}")
        sys.exit(1)

    print(f"Found {len(files)} scenario files (infeasible network state).\n")

    valid_count = 0
    invalid_examples = []

    for i, filepath in enumerate(files):
        with open(filepath) as f:
            scenario = json.load(f)

        n_paths = len(scenario["alternative_paths"])
        print(f"[{i+1}/{len(files)}] {os.path.basename(filepath)} "
              f"({n_paths} paths, traffic={scenario['total_traffic']})...", end=" ")

        plan, raw_response = ask_model(scenario)
        valid, reason = is_valid_plan(plan, scenario)

        if valid:
            valid_count += 1
            print("VALID")
        else:
            print(f"INVALID ({reason})")
            invalid_examples.append({
                "file": os.path.basename(filepath),
                "reason": reason,
                "raw_response": raw_response[:300],
            })

    print("\n" + "=" * 60)
    print(f"SUMMARY: {valid_count}/{len(files)} scenarios produced a valid response")
    print(f"         on an INFEASIBLE network (no correct answer exists,")
    print(f"         this is purely a sanity/robustness check).")
    print("=" * 60)

    if invalid_examples:
        print("\nInvalid examples:")
        for ex in invalid_examples[:5]:
            print(f"  - {ex['file']}: {ex['reason']}")
            print(f"    raw: {ex['raw_response']}")


if __name__ == "__main__":
    run()