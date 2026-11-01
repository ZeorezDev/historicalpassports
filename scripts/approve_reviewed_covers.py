"""Frozen visual-review decisions made 2026-10-31. Does not auto-approve future gallery entries.
All listed covers were inspected on contact sheets against their country captions and metadata.
"""
from commons_assets import *
REVIEWED=set('''AD AE AF AG AL AO AR AT AU AZ BA BD BE BF BG BH BI BJ BN BO BR BY CA CD CF CG CH CI CM CN CO CR CU CV CY CZ DE DJ DK DM DO DZ EC EE EG ER ES ET FI FM FR GA GB GD GE GH GN GT GW HN HR HT HU ID IE IL IN IQ IR IS IT JM JO JP KE KG KH KM KN KP KR KW KZ LA LB LC LI LK LR LT LU LV LY MA MC MD ME MG MH MK MM MN MR MT MV MX MY MZ NA NE NG NI NL NO NP PA PE PG PH PK PL PS PT QA RO RS RU RW SA SC SD SE SG SI SK SL SM SN SO ST SY SZ TD TG TH TJ TL TM TN TT TV TZ UA UG US UZ VA VE VN VU YE ZA'''.split())
old=json.loads((ROOT/'assets/approved_sources.json').read_text(encoding='utf-8'))
approved={c['code']:c for c in old if c['code']=='BG'}
gallery=json.loads((ROOT/'assets/gallery_candidates.json').read_text(encoding='utf-8'))
for c in gallery:
    if c['code'] not in REVIEWED or c['code']=='BG':continue
    path=ROOT/f'assets/gallery_previews/{c["code"]}.png'
    assert path.exists(),c['code']
    assert c['verified_metadata'] and not license_issue(c['verified_metadata']) and not rejection_reason(c['title']),c['code']
    entry={k:c[k] for k in ['code','title','verified_metadata','preview_url']}
    entry.update(local_source=str(path.relative_to(ROOT)).replace('\\','/'),trim_background=True,
        source_pixel_sha256=hashlib.sha256(Image.open(path).convert('RGBA').tobytes()).hexdigest(),
        review='2026-10-31 visual review: front cover depicts the named country and ordinary passport type; no personal data page. Commons file license and warning categories checked. Photo/scan or vector depiction as identified by the source file; edition is not guaranteed to be the newest.')
    if c['code']=='CM':entry['quad']=[.035,.065,.035,.945,.945,.945,.92,.02]
    if c['code']=='KN':entry['quad']=[.15,.02,.01,.975,.99,.975,.86,.005]
    approved[c['code']]=entry
extras=json.loads((ROOT/'assets/extra_candidates.json').read_text(encoding='utf-8'))
for c in extras:
    assert c['code'] in {'TR','AM','CL'}
    path=ROOT/f'assets/extra_previews/{c["code"]}.png'
    c.update(local_source=str(path.relative_to(ROOT)).replace('\\','/'),trim_background=True,
        source_pixel_sha256=hashlib.sha256(Image.open(path).convert('RGBA').tobytes()).hexdigest(),
        review='2026-10-31 visually verified ordinary biometric front cover; no personal data. Historical ordinary edition still used as a visual reference, not claimed as the latest design. Alternative to disputed, proposed or unavailable image.')
    approved[c['code']]=c
write(ROOT/'assets/approved_sources.json',list(sorted(approved.values(),key=lambda c:c['code'])))
print('Explicitly approved covers:',len(approved))

