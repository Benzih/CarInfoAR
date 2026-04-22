"""
v4 tuning — builds on v3 with targeted refinements based on per-car analysis:

Findings from v3 (MAD=16.7%, 55 LY cars):

1. CHINESE Y0-3 penalty too harsh (MG EHS PHEV +40%, ZEEKR +26%, Chery FX EV +28%)
   → Remove Y0-1 penalty, softer Y1-3 penalty, moderate Y3+ penalty.

2. WEAK-RESALE Y10+ cars systematically overestimated (Fiat 500 -22%, 308 -20%,
   Sandero -22%) → Additional steepening for Weak-resale × Y10+.

3. Chevrolet/Opel classified as Standard but behave like Weak-resale past Y10
   (CRUZE -36%, SONIC -27%, CORSA -25%) → Add "Old-generic" tier for
   Chevrolet/Opel/Daewoo past Y10.

4. Commercial vans (BERLINGO +46%) — business-use vehicles hold value differently
   → Add commercial-van model detection for +12% boost.

5. Suzuki at "Premium-reliable 1.10" maybe too much for budget Suzuki (CELERIO -19%).
   Try 1.05 instead of 1.10.

6. BMW M-series/AMG etc. depreciate harder than regular Premium-Lux
   (M850I +38%) → Detect M/AMG/S-class badge, use 0.75 instead of 0.85 past Y5.
"""
from __future__ import annotations
import json, sys
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8')
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT/"tools"))

from tune_v3 import (load_combined, stats, base_price, age_years,
    _body_factor, _trim_factor, _safety, _emission, _originality,
    _owners_factor, _hand_count_v3, _prefix_match,
    CHINESE, PREMIUM_REL_BASE, KOREAN, GERMAN_LUX, WEAK, round_clean)


def _age_fuel_v4(age, fuel, model, country):
    if country == "IL":
        if age <= 1.0:  yr = 0.92 - 0.07 * age
        elif age <= 5.0:  yr = 0.85 * 0.925 ** (age - 1.0)
        elif age <= 10.0: yr = 0.622 * 0.83 ** (age - 5.0)
        else:             yr = 0.249 * 0.85 ** (age - 10.0)
    else:
        if age <= 1.0:  yr = 0.82 - 0.06 * age
        elif age <= 3.0:  yr = 0.76 * 0.88 ** (age - 1.0)
        else:             yr = 0.76 * 0.88**2 * 0.92 ** (age - 3.0)
    f = (fuel or "").lower()
    mdl = (model or "").upper()
    if any(tag in mdl for tag in ("HSD","HEV","HYBRID","PHEV","SELF-CHARGING","HYBRYD","MILD HYBRID")):
        fm = 1.08 if country == "IL" else 1.02
    elif "חשמל" in f or "electric" in f:
        fm = 0.75 if age <= 1 else (0.85 if age <= 3 else 0.95)
    elif "hybrid" in f or "היבר" in f or "hybride" in f:
        fm = 1.08 if country == "IL" else 1.02
    elif "דיזל" in f or "diesel" in f:
        fm = 0.92 if age >= 5 else 0.98
    else:
        fm = 1.0
    return yr * fm


def _mile_factor_v4(km, age, country):
    """Same as v3: -5% cap negative, +8% cap positive."""
    if not km or float(km) <= 0 or age <= 0: return 1.0
    avg = 13_000 if country == "NL" else 15_000
    delta = float(km) - avg * age
    adj = -0.02 * (delta / 10_000.0)
    return 1.0 + max(-0.05, min(0.08, adj))


# Commercial/light-commercial vans — hold value differently (business fleet,
# tradesmen; demand exceeds depreciation curve for passenger cars).
COMMERCIAL_MODELS = {
    "BERLINGO", "PARTNER", "KANGOO", "CADDY", "TRANSIT", "VIVARO", "TRAFIC",
    "EXPERT", "JUMPER", "BOXER", "DUCATO", "MASTER", "MOVANO", "VITO",
    "SPRINTER", "VIANO", "CONNECT", "DOBLO", "COMBO",
    "ברלינגו", "פרטנר", "קאנגו", "קדי", "טרנזיט", "ויוואר", "טראפיק",
    "אקספרט", "דוקאטו", "ויטו", "ספרינטר",
}

# BMW M-series, Mercedes AMG, Audi RS etc — performance halo depreciates
# harder because tiny buyer pool + maintenance cost.
PERFORMANCE_TAGS = ("M8", "M850", "M5 ", " M5", "M3 ", " M3", "M4 ", " M4",
                     "AMG", " RS ", " RS4", " RS6", " RS7", " S4", " S6", " S8")

def is_commercial(model):
    m = (model or "").upper()
    return any(c in m for c in COMMERCIAL_MODELS)

def is_performance(model):
    m = (model or "").upper()
    return any(t in m for t in PERFORMANCE_TAGS)


# Extra "old-generic" set — American/Opel budget brands that depreciate
# hard past Y10 but aren't classic Weak-resale (Fiat/Renault/etc.)
OLD_GENERIC = {"CHEVROLET","OPEL","DAEWOO","HOLDEN",
               "שברולט","אופל","דוו","דיאו"}


def _brand_v4(make, model, age, country):
    """v4 brand classifier with multiple refinements."""
    m = (make or "").upper().strip()
    mdl = (model or "").upper()

    # Performance luxury (M/AMG/RS) — extra steep past Y5
    if is_performance(mdl) and _prefix_match(m, GERMAN_LUX):
        if age < 3: return 0.95, "Performance-Lux"
        if age < 5: return 0.82, "Performance-Lux"
        return 0.70, "Performance-Lux"

    # Commercial vans — lighter depreciation
    if is_commercial(mdl):
        if age < 5: return 1.00, "Commercial"
        return 1.12, "Commercial"   # 12% boost past Y5

    # Chinese — softened from v3
    if country == "IL" and _prefix_match(m, CHINESE):
        if age <= 1.0: return 1.00, "Chinese-IL"   # Y0-1 no penalty
        if age <= 3.0: return 0.95, "Chinese-IL"   # Y1-3 mild
        if age <= 5.0: return 0.88, "Chinese-IL"   # Y3-5 moderate
        return 0.78, "Chinese-IL"                   # Y5+ stronger

    # Premium-reliable (Toyota/Lexus/Mazda/Honda/Subaru)
    if _prefix_match(m, PREMIUM_REL_BASE):
        return 1.10, "Premium-reliable"

    # Suzuki — softer than premium, but stronger than standard
    if m.startswith("סוזוקי"):
        return 1.05, "Suzuki-solid"

    # Korean — 1.08 (bumped from 1.06)
    if country == "IL" and _prefix_match(m, KOREAN):
        return 1.08, "Korean-IL"

    # German/European luxury — steeper past Y5
    if _prefix_match(m, GERMAN_LUX):
        if age < 3: return 1.00, "Premium-Lux"
        if age < 5: return 0.92, "Premium-Lux"
        return 0.85, "Premium-Lux"

    # Weak-resale — now with Y10+ steeper penalty
    if _prefix_match(m, WEAK):
        if age > 10: return 0.85, "Weak-resale"
        return 0.92, "Weak-resale"

    # Old-generic (Chevrolet/Opel/Daewoo) — like Weak-resale but only past Y10
    if _prefix_match(m, OLD_GENERIC):
        if age > 10: return 0.85, "Old-generic"
        return 1.00, "Old-generic"

    return 1.00, "Standard"


def estimate_v4(info):
    base = base_price(info)
    age = age_years(info)
    if base is None or age is None: return None
    country = info.get("country") or "IL"
    fuel = info.get("fuelType")
    model = info.get("model")

    f_agefuel = _age_fuel_v4(age, fuel, model, country)
    f_own, hc = _owners_factor(info, _hand_count_v3)
    f_mile = _mile_factor_v4(info.get("lastTestKm") or info.get("km"), age, country)
    f_body = _body_factor(info.get("bodyType"))
    f_brand, tier = _brand_v4(info.get("manufacturer") or info.get("mfg"), model, age, country)
    f_trim = _trim_factor(info.get("trimLevel") or info.get("trim"))
    f_safety = _safety(info.get("safetyScore"))
    f_emiss = _emission(info)
    f_orig = _originality(info)
    f_lpg = 0.88 if info.get("lpgAdded") in (True,"כן",1) else 1.0

    factor = f_agefuel*f_own*f_mile*f_body*f_brand*f_trim*f_safety*f_emiss*f_orig*f_lpg
    mid = max(base * factor, 8000)
    return {"mid": round_clean(mid), "factor": factor, "tier": tier, "hand": hc,
            "f_agefuel": f_agefuel, "f_own": f_own, "f_mile": f_mile,
            "f_brand": f_brand, "f_body": f_body, "f_trim": f_trim,
            "age": age}


def main():
    rows = load_combined()
    print(f"Combined: {len(rows)} cars\n")

    deltas_all = []
    results = []
    fails = []
    for r in rows:
        e = estimate_v4(r['info'])
        if not e:
            fails.append(r['info'].get('plateNumber')); continue
        d = (r['levi'] - e['mid']) / e['mid']
        deltas_all.append(d)
        results.append((r, e, d))

    print(f"=== v4 (fails: {len(fails)}) ===")
    stats(deltas_all, "  ALL ")
    stats([d for r,e,d in results if r['source']=='old'], "  old ")
    stats([d for r,e,d in results if r['source']=='new'], "  new ")

    print(f"\n=== v4 per-car (sorted by abs delta) ===")
    results.sort(key=lambda x: -abs(x[2]))
    print(f"{'plate':<10} {'mfg':<13} {'model':<18} {'yr':<5} {'age':<5} "
          f"{'cat':<9} {'km':<7} {'mid':<9} {'LY':<9} {'delta':<7} {'tier':<16}")
    for r, e, d in results[:25]:
        info = r['info']
        mfg = str(info.get('manufacturer') or info.get('mfg') or '')[:12]
        model = str(info.get('model') or '')[:17]
        cat = base_price(info)
        km = info.get('lastTestKm') or info.get('km')
        print(f"{str(info.get('plateNumber') or ''):<10} "
              f"{mfg:<13} {model:<18} {info.get('year','')!s:<5} "
              f"{e['age']:<5.1f} "
              f"{(str(int(cat)) if cat else '-'):<9} "
              f"{(str(int(km)) if km else '-'):<7} "
              f"{e['mid']:<9,} {int(r['levi']):<9,} {d*100:+6.1f}% {e['tier']:<16}")

if __name__ == "__main__":
    main()
