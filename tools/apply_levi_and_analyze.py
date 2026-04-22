"""Apply LY prices extracted from screenshots and show v3 delta analysis."""
from __future__ import annotations
import sys, json
from pathlib import Path
sys.stdout.reconfigure(encoding='utf-8')
ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT/"tools"))

from build_v3_validation_xlsx import est_v3
from tune_v3 import base_price, stats

# Prices extracted from screenshots (plate → LY price)
LY_PRICES = {
    "78332502": 71400,    # Hyundai Accent 2022
    "62012901": 76000,    # Suzuki Jimny 2019
    "12245204": 144400,   # Hyundai Kona Premium Hybrid 2025
    "82865301": 29400,    # Dacia Logan Laureate 2019
    "39334303": 108500,   # Subaru XV Cross Style 2023
    "16083004": 130680,   # Subaru Crosstrek Touring 2025
    "4661768":  11400,    # Honda Accord Comfort 2008
    "92444001": 35500,    # SsangYong Tivoli XLV EX 2018
    "9311381":  47000,    # Toyota Corolla Sedan 2017
    "3503666":  11300,    # Hyundai i30 Inspire 2008
    "74240603": 211080,   # Toyota Hilux Double-cab Diesel 2024
    "61497901": 34000,    # Fiat Tipo Comfort 2019
    "4608475":  15400,    # Mazda 3 Active 2012
    "53455302": 109920,   # Nissan X-Trail Acenta 2021
    "59270801": 81000,    # Toyota C-HR Hybrid FLOW 2019
    "6415076":  12300,    # Seat Ibiza Reference 2012
    "2161478":  20800,    # Chrysler/Jeep Compass Sport 4X4 2011
    "26377701": 49000,    # Alfa Romeo Giulietta 2017
    "7150232":  29400,    # Suzuki SX4 CrossOver 2014
    "23619304": 154200,   # Hyundai Kona Supreme Hybrid 2025
    "7038131":  25000,    # Kia Picanto LX 2014
    "12044403": 107400,   # Mitsubishi Eclipse Cross 2022
    "6492279":  16434,    # Kia Rio LX 2013
    "73732801": 47500,    # Mitsubishi Space Star Supreme 2019
    "6121152":  28200,    # Toyota Corolla 2013
}

# NOT in screenshots (user said "חוץ מ 2 נראה לי" — actually 3):
NOT_CAPTURED = {"1036160", "1452910", "85768501"}

data = json.load(open(ROOT/"scan_history_v3_test.json", encoding='utf-8'))
rows = []
for info in data:
    plate = str(info.get('plateNumber'))
    if plate not in LY_PRICES:
        continue
    e = est_v3(info)
    if not e: continue
    levi = LY_PRICES[plate]
    d = (levi - e['mid']) / e['mid']
    rows.append({"info": info, "est": e, "levi": levi, "delta": d})

print(f"=== v3 validation on 25 held-out LY cars ===\n")
deltas = [r['delta'] for r in rows]
stats(deltas, "v3 vs LY")
print()

# Sort by abs delta for easy inspection
rows.sort(key=lambda r: -abs(r['delta']))
print(f"{'plate':<10} {'mfg':<14} {'model':<18} {'age':<5} {'cat':<9} "
      f"{'km':<8} {'v3_mid':<8} {'LY':<8} {'delta':<7} {'tier':<16}")
for r in rows:
    info = r['info']
    e = r['est']
    mfg = str(info.get('manufacturer') or '')[:13]
    model = str(info.get('model') or '')[:17]
    cat = base_price(info)
    km = info.get('lastTestKm') or '-'
    print(f"{str(info.get('plateNumber')):<10} {mfg:<14} {model:<18} "
          f"{e['age']:<5.1f} {(str(int(cat)) if cat else '-'):<9} "
          f"{str(km):<8} {e['mid']:<8} {r['levi']:<8} "
          f"{r['delta']*100:+6.1f}% {e['tier']:<16}")

# Combined with all 55 cars from before
print()
print("=" * 90)
print("\n=== Combined analysis: 55 old LY cars + 25 new LY held-out = 80 total ===\n")
from tune_v3 import load_combined
old_rows = load_combined()
old_deltas = []
for r in old_rows:
    e = est_v3(r['info'])
    if not e: continue
    old_deltas.append((r['levi'] - e['mid']) / e['mid'])

all_deltas = old_deltas + deltas
stats(all_deltas, "  ALL (n=80)")
stats(old_deltas, "  Train set (n=55)")
stats(deltas,     "  Held-out (n=25)")

# Save results
out = ROOT / "v3_heldout_results.json"
json.dump({
    "cars": [{"plate": r['info']['plateNumber'],
              "mfg": r['info'].get('manufacturer'),
              "model": r['info'].get('model'),
              "year": r['info'].get('year'),
              "cat": base_price(r['info']),
              "km": r['info'].get('lastTestKm'),
              "age": r['est']['age'],
              "tier": r['est']['tier'],
              "mid_v3": r['est']['mid'],
              "levi": r['levi'],
              "delta_pct": round(r['delta']*100, 1)} for r in rows],
    "summary": {
        "n": len(rows),
        "mad": round(sum(abs(d) for d in deltas) / len(deltas) * 100, 2),
        "mean": round(sum(deltas)/len(deltas)*100, 2),
        "within_10": round(sum(1 for d in deltas if abs(d)<=0.10)/len(deltas), 2),
        "within_20": round(sum(1 for d in deltas if abs(d)<=0.20)/len(deltas), 2),
    }
}, out.open("w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(f"\nSaved {out}")
