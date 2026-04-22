"""v3b — refinements informed by 25-car held-out validation.

Outlier analysis from v3 held-out (MAD=18.0%):

Biggest misses identified:
  +61% PICANTO Y11.9 Kia     — very old Korean budget
  +61% SPACE STAR Y6.9 Mitsu — Mid-reliable Y7 under
  +42% COROLLA Y8.9 Toyota   — Y5-10 curve too steep for Premium-reliable
  +31% i30 Y17.9 Hyundai     — Y15+ Korean still OK? actually mid 8600 <LY
  +28% SX4 Y11.9 Suzuki      — old Suzuki under
  -26% KONA Supreme Y0.9     — Y<1 over-boost (>catalog price!)
  +19% HILUX Y1.9 Toyota     — Y<2 Premium delay hurts here
  -17% TIVOLI Y7.9 SsangYong — classified Standard, should be lower
  -16% ACCORD Y17.4 Honda    — Y15+ Premium-reliable still boosts

Tweaks:
  A) Remove Y<2 delay on Premium-reliable boost → HILUX helped, Crosstrek stays OK
  B) Premium-reliable Y15+: remove boost (1.10 → 1.00) → Accord helped
  C) Y0-1 cap: mid ≤ catalog × 0.95 → KONA Supreme helped
  D) SsangYong → new "budget-Korean-old" tier at 0.92
  E) Mid-reliable Y5+: 1.02 → 1.08 → Space Star helped
  F) Y5-10 curve softened slightly: 0.83 → 0.84 per year
"""
from __future__ import annotations
import sys, json
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT/"tools"))

from tune_v3 import (load_combined, stats, base_price, age_years,
    _body_factor, _trim_factor, _safety, _emission, _originality,
    _owners_factor, _hand_count_v3, _prefix_match, round_clean)
from tune_v4 import is_performance, is_commercial, OLD_GENERIC
from tune_v5 import CHINESE_V5, PREMIUM_REL_V5, KOREAN_V5, GERMAN_LUX_V5, WEAK_V5, MID_RELIABLE_V5


def af(age, fuel, model, country):
    if country == "IL":
        if age <= 1.0:  yr = 0.92 - 0.07 * age
        elif age <= 5.0:  yr = 0.85 * 0.925 ** (age - 1.0)
        elif age <= 10.0: yr = 0.622 * 0.84 ** (age - 5.0)  # softened 0.83 → 0.84
        else:             yr = 0.279 * 0.85 ** (age - 10.0)  # new Y10 anchor = 0.279
    else:
        if age <= 1.0:  yr = 0.82 - 0.06 * age
        elif age <= 3.0:  yr = 0.76 * 0.88 ** (age - 1.0)
        else:             yr = 0.76 * 0.88**2 * 0.92 ** (age - 3.0)
    f = (fuel or "").lower(); mdl = (model or "").upper()
    is_hyb = any(t in mdl for t in ("HSD","HEV","HYBRID","PHEV","SELF-CHARGING","HYBRYD")) or "hybrid" in f or "היבר" in f
    if is_hyb:
        fm = 1.15 if (country == "IL" and age >= 3.0) else (1.10 if country == "IL" else 1.02)
    elif "חשמל" in f or "electric" in f:
        fm = 0.75 if age <= 1 else (0.85 if age <= 3 else 0.95)
    elif "דיזל" in f or "diesel" in f: fm = 0.92 if age >= 5 else 0.98
    else: fm = 1.0
    return yr * fm


def mile(km, age, country):
    if not km or float(km) <= 0 or age <= 0: return 1.0
    avg = 13000 if country == "NL" else 15000
    adj = -0.01 * ((float(km) - avg*age) / 10000)
    return 1.0 + max(-0.05, min(0.08, adj))


# v3b: new tier for SsangYong / older Korean-budget
SSANGYONG = {"SSANGYONG", "SANGYONG", "סאנגיונג", "סאנגיוג"}


def brand(make, model, age, country):
    m = (make or "").upper().strip()
    mdl = (model or "").upper()

    if is_performance(mdl) and _prefix_match(m, GERMAN_LUX_V5):
        if age < 3: return 0.95, "Performance-Lux"
        if age < 5: return 0.82, "Performance-Lux"
        return 0.70, "Performance-Lux"

    if is_commercial(mdl):
        if age < 5: return 1.00, "Commercial"
        return 1.15, "Commercial"

    if country == "IL" and _prefix_match(m, CHINESE_V5):
        if age <= 1.0: return 1.00, "Chinese-IL"
        if age <= 3.0: return 0.95, "Chinese-IL"
        if age <= 5.0: return 0.88, "Chinese-IL"
        return 0.78, "Chinese-IL"

    # SsangYong — treat as Weak-resale (lower prestige than Korean-IL)
    if _prefix_match(m, SSANGYONG):
        return 0.92, "SsangYong"

    if _prefix_match(m, PREMIUM_REL_V5):
        # Changed: no Y<2 delay. Y15+ boost removed (reverts to 1.00).
        if age >= 15: return 1.00, "Premium-reliable"
        return 1.10, "Premium-reliable"

    if m.startswith("סוזוקי") or m.startswith("SUZUKI"):
        if age >= 15: return 1.00, "Suzuki-solid"
        return 1.05, "Suzuki-solid"

    if country == "IL" and _prefix_match(m, KOREAN_V5):
        if age >= 15: return 1.00, "Korean-IL"  # Y15+ Koreans lose prestige boost
        return 1.10, "Korean-IL"

    if _prefix_match(m, GERMAN_LUX_V5):
        if age < 3: return 1.00, "Premium-Lux"
        if age < 5: return 0.92, "Premium-Lux"
        return 0.85, "Premium-Lux"

    if _prefix_match(m, WEAK_V5):
        if age > 10: return 0.78, "Weak-resale"
        return 0.92, "Weak-resale"

    if _prefix_match(m, OLD_GENERIC):
        if age > 10: return 0.70, "Old-generic"
        return 1.00, "Old-generic"

    # Mid-reliable bumped Y5+
    if _prefix_match(m, MID_RELIABLE_V5):
        if age >= 5: return 1.08, "Mid-reliable"
        return 1.02, "Mid-reliable"

    return 1.00, "Standard"


def est_v3b(info):
    base = base_price(info)
    age = age_years(info)
    if base is None or age is None: return None
    c = info.get("country") or "IL"
    f_af = af(age, info.get("fuelType"), info.get("model"), c)
    f_own, hc = _owners_factor(info, _hand_count_v3)
    f_mile = mile(info.get("lastTestKm") or info.get("km"), age, c)
    f_body = _body_factor(info.get("bodyType"))
    f_brand, tier = brand(info.get("manufacturer") or info.get("mfg"),
                           info.get("model"), age, c)
    f_trim = _trim_factor(info.get("trimLevel") or info.get("trim"))
    f_safe = _safety(info.get("safetyScore"))
    f_emi = _emission(info)
    f_or = _originality(info)
    f_lpg = 0.88 if info.get("lpgAdded") in (True,"כן",1) else 1.0
    factor = f_af*f_own*f_mile*f_body*f_brand*f_trim*f_safe*f_emi*f_or*f_lpg
    mid = max(base * factor, 8000)
    # v3b: Y<1 cap — no brand new car worth more than 95% catalog
    if age <= 1.0:
        mid = min(mid, base * 0.95)
    return {"mid": round_clean(mid), "tier": tier, "age": age,
            "hand": hc, "f_af": f_af, "f_own": f_own, "f_mile": f_mile,
            "f_brand": f_brand, "factor": factor}


def main():
    # Load all 80 cars — but compute Train vs Held-out metrics separately
    train_rows = load_combined()
    from apply_levi_and_analyze import LY_PRICES
    held_data = json.load(open(ROOT/"scan_history_v3_test.json", encoding='utf-8'))
    held_rows = []
    for info in held_data:
        p = str(info.get('plateNumber'))
        if p in LY_PRICES:
            held_rows.append({"info": info, "levi": LY_PRICES[p]})

    def eval_batch(rows, label):
        ds = []
        results = []
        for r in rows:
            e = est_v3b(r['info'])
            if not e: continue
            d = (r['levi'] - e['mid']) / e['mid']
            ds.append(d); results.append((r, e, d))
        stats(ds, label)
        return results

    print("=== v3b on Train (55 cars — seen during tuning) ===")
    train_r = eval_batch(train_rows, "  ")
    print("\n=== v3b on Held-out (25 cars — NEW validation) ===")
    held_r = eval_batch(held_rows, "  ")
    all_ds = [d for r,e,d in train_r+held_r]
    print("\n=== v3b on ALL 80 cars combined ===")
    stats(all_ds, "  ")

    print(f"\n=== Held-out top outliers (v3b) ===")
    held_r.sort(key=lambda x: -abs(x[2]))
    print(f"{'plate':<10} {'mfg':<13} {'model':<18} {'age':<5} {'cat':<9} "
          f"{'mid':<8} {'LY':<8} {'delta':<7} {'tier':<16}")
    for r, e, d in held_r[:12]:
        info = r['info']
        mfg = str(info.get('manufacturer') or '')[:12]
        model = str(info.get('model') or '')[:17]
        cat = base_price(info)
        print(f"{str(info.get('plateNumber')):<10} {mfg:<13} {model:<18} "
              f"{e['age']:<5.1f} {(str(int(cat)) if cat else '-'):<9} "
              f"{e['mid']:<8} {r['levi']:<8} {d*100:+6.1f}% {e['tier']:<16}")


if __name__ == "__main__":
    main()
