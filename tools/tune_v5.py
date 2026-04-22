"""v5 — fixes brand-matching bugs found in v4:
- 'מזדה' spelled without aleph (data.gov.il) wasn't matching 'מאזדה' → Mazda got Standard instead of Premium-reliable
- 'ב מ וו' with spaces (data.gov.il) wasn't matching 'ב.מ.וו' → BMW got Standard instead of German-Lux
- 'מרצדס בנץ' format verified (matches 'מרצדס' prefix — OK)
- Also adds Volkswagen (mass reliable), Mitsubishi.
"""
from __future__ import annotations
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding='utf-8')
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT/"tools"))
from tune_v3 import (load_combined, stats, base_price, age_years,
    _body_factor, _trim_factor, _safety, _emission, _originality,
    _owners_factor, _hand_count_v3, _prefix_match, round_clean)
from tune_v4 import (_age_fuel_v4, _mile_factor_v4, is_commercial, is_performance,
    COMMERCIAL_MODELS, PERFORMANCE_TAGS, OLD_GENERIC)

# Real IL data-gov format observed in 55-car dataset:
#   'מזדה יפן'          Mazda (no aleph)
#   'ב מ וו גרמניה'     BMW (spaces between letters)
#   'מרצדס בנץ ארהב'   Mercedes (matches 'מרצדס' prefix)
#   'וולבו בלגיה'       Volvo (matches 'וולבו')
#   'אופל-ספרד'         Opel (dash)
#   'סוזוקי-יפן'        Suzuki (dash) — matches 'סוזוקי' prefix
#   'ג'אקו סין'         Jaecoo — matches 'ג'אקו' prefix (needs to be in CHINESE)

CHINESE_V5 = {"BYD","CHERY","GEELY","MG","JAECOO","OMODA","GREAT WALL","HAVAL","NIO",
    "XPENG","LEAPMOTOR","DONGFENG","MAXUS","ZEEKR","SAIC","ROEWE",
    "צ'רי","ביי די","ליפמוטור","אומודה","מ.ג","ג'אקו","זיקר","ג'יקו"}
PREMIUM_REL_V5 = {"TOYOTA","LEXUS","HONDA","MAZDA","SUBARU",
    "טויוטה","לקסוס","הונדה","מאזדה","מזדה","סובארו"}
KOREAN_V5 = {"HYUNDAI","KIA","GENESIS","יונדאי","קיה","גנסיס"}
GERMAN_LUX_V5 = {"BMW","MERCEDES","MERCEDES-BENZ","AUDI","PORSCHE",
    "ב.מ.וו","ב.מ.ו","ב מ וו","בי אם דבליו","מרצדס","אאודי","אודי","פורשה",
    "VOLVO","LAND ROVER","RANGE ROVER","JAGUAR",
    "וולבו","לנד רובר","ריינג רובר","רובר","יגואר"}
WEAK_V5 = {"FIAT","ALFA","ALFA ROMEO","RENAULT","CITROEN","PEUGEOT","DACIA","LANCIA",
    "פיאט","אלפא","רנו","סיטרואן","פיג'ו","דאצ'יה","דאציה"}
# Mid-reliable: VW/Ford/Skoda/Mitsubishi/Nissan — volume brands that don't crash
# in price the way Weak-resale does but aren't Premium-reliable either.
MID_RELIABLE_V5 = {"VW","VOLKSWAGEN","SKODA","FORD","MITSUBISHI","NISSAN",
    "פולקסווגן","סקודה","פורד","מיצובישי","ניסאן"}


def _brand_v5(make, model, age, country):
    m = (make or "").upper().strip()
    mdl = (model or "").upper()

    if is_performance(mdl) and _prefix_match(m, GERMAN_LUX_V5):
        if age < 3: return 0.95, "Performance-Lux"
        if age < 5: return 0.82, "Performance-Lux"
        return 0.70, "Performance-Lux"

    if is_commercial(mdl):
        if age < 5: return 1.00, "Commercial"
        return 1.12, "Commercial"

    if country == "IL" and _prefix_match(m, CHINESE_V5):
        if age <= 1.0: return 1.00, "Chinese-IL"
        if age <= 3.0: return 0.95, "Chinese-IL"
        if age <= 5.0: return 0.88, "Chinese-IL"
        return 0.78, "Chinese-IL"

    if _prefix_match(m, PREMIUM_REL_V5):
        return 1.10, "Premium-reliable"

    if m.startswith("סוזוקי"):
        return 1.05, "Suzuki-solid"

    if country == "IL" and _prefix_match(m, KOREAN_V5):
        return 1.08, "Korean-IL"

    if _prefix_match(m, GERMAN_LUX_V5):
        if age < 3: return 1.00, "Premium-Lux"
        if age < 5: return 0.92, "Premium-Lux"
        return 0.85, "Premium-Lux"

    if _prefix_match(m, WEAK_V5):
        if age > 10: return 0.85, "Weak-resale"
        return 0.92, "Weak-resale"

    if _prefix_match(m, OLD_GENERIC):
        if age > 10: return 0.85, "Old-generic"
        return 1.00, "Old-generic"

    if _prefix_match(m, MID_RELIABLE_V5):
        return 1.02, "Mid-reliable"   # small boost vs pure Standard

    return 1.00, "Standard"


def estimate_v5(info):
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
    f_brand, tier = _brand_v5(info.get("manufacturer") or info.get("mfg"), model, age, country)
    f_trim = _trim_factor(info.get("trimLevel") or info.get("trim"))
    f_safety = _safety(info.get("safetyScore"))
    f_emiss = _emission(info)
    f_orig = _originality(info)
    f_lpg = 0.88 if info.get("lpgAdded") in (True,"כן",1) else 1.0

    factor = f_agefuel*f_own*f_mile*f_body*f_brand*f_trim*f_safety*f_emiss*f_orig*f_lpg
    mid = max(base * factor, 8000)
    return {"mid": round_clean(mid), "factor": factor, "tier": tier, "hand": hc,
            "f_agefuel": f_agefuel, "f_own": f_own, "f_mile": f_mile,
            "f_brand": f_brand, "f_body": f_body, "age": age}


def main():
    rows = load_combined()
    deltas_all = []; results = []
    for r in rows:
        e = estimate_v5(r['info'])
        if not e: continue
        d = (r['levi'] - e['mid']) / e['mid']
        deltas_all.append(d)
        results.append((r, e, d))

    print(f"=== v5 ===")
    stats(deltas_all, "  ALL ")
    stats([d for r,e,d in results if r['source']=='old'], "  old ")
    stats([d for r,e,d in results if r['source']=='new'], "  new ")

    print(f"\n=== top 20 outliers ===")
    results.sort(key=lambda x: -abs(x[2]))
    print(f"{'plate':<10} {'mfg':<13} {'model':<18} {'age':<5} {'cat':<9} "
          f"{'km':<7} {'mid':<9} {'LY':<9} {'delta':<7} {'tier':<16}")
    for r, e, d in results[:20]:
        info = r['info']
        mfg = str(info.get('manufacturer') or info.get('mfg') or '')[:12]
        model = str(info.get('model') or '')[:17]
        cat = base_price(info)
        km = info.get('lastTestKm') or info.get('km')
        print(f"{str(info.get('plateNumber') or ''):<10} "
              f"{mfg:<13} {model:<18} {e['age']:<5.1f} "
              f"{(str(int(cat)) if cat else '-'):<9} "
              f"{(str(int(km)) if km else '-'):<7} "
              f"{e['mid']:<9,} {int(r['levi']):<9,} {d*100:+6.1f}% {e['tier']:<16}")

    # Show tier distribution
    from collections import Counter
    tiers = Counter(e['tier'] for _,e,_ in results)
    print(f"\nTier distribution: {dict(tiers)}")

if __name__ == "__main__":
    main()
