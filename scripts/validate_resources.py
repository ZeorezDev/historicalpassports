from pathlib import Path
import json,re,sys,zipfile
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'.tools/python'))
from PIL import Image
RES=ROOT/'src/main/resources'; A=RES/'assets/historicalpassports'
countries=json.loads((RES/'data/historicalpassports/countries/modern.json').read_text(encoding='utf-8'))
assert len(countries)==195 and len({c['code'] for c in countries})==195
languages=[json.loads((A/f'lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['en_us','tr_tr']]
assert languages[0].keys()==languages[1].keys(),'Translation key mismatch'
for c in countries:
    key=c['code'].lower();im=Image.open(A/f'textures/item/passport_{key}.png')
    assert im.size==(64,64) and im.mode=='RGBA'
    assert im.getpixel((0,0))[3]==0
    model=json.loads((A/f'models/item/passport_{key}.json').read_text())
    assert model['textures']['layer0']==f'historicalpassports:item/passport_{key}'
    for lang in languages: assert lang[f'country.historicalpassports.{key}'] and lang[f'item.historicalpassports.passport_{key}']
for path in RES.rglob('*.json'): json.loads(path.read_text(encoding='utf-8'))
for path in (ROOT/'src/main/java').rglob('*.java'):
    source=path.read_text(encoding='utf-8')
    for key in re.findall(r'"(hp\.[a-z_.]+)"',source):
        if key.endswith('.'):continue
        assert key in languages[0],f'Missing translation {key} in {path}'
from commons_assets import license_issue,rejection_reason
imported=json.loads((ROOT/'assets/imported_sources.json').read_text(encoding='utf-8'))
assert len({c['code'] for c in imported})==len(imported)
for c in imported:
    assert not license_issue(c['verified_metadata']),c['title']
    assert not rejection_reason(c['title']),c['title']
assert sum(c['asset_status']=='reviewed_commons' for c in countries)==len(imported)
print(f'PASS: {len(imported)} reviewed imports; rejected/disputed/AI sources excluded; country coverage metadata matches.')
print('PASS: 195 unique countries; 195 local RGBA covers and matching models; EN/TR translation parity; JSON syntax; literal UI keys.')
