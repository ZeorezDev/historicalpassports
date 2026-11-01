"""Contact sheet of shipped 64px item textures; labels show actual source coverage."""
from pathlib import Path
import json,sys
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'.tools/python'))
from PIL import Image,ImageDraw,ImageFont
countries=json.loads((ROOT/'src/main/resources/data/historicalpassports/countries/modern.json').read_text(encoding='utf-8'))
sheet=Image.new('RGB',(13*76,15*87),'#202928');draw=ImageDraw.Draw(sheet)
for i,c in enumerate(countries):
    x=i%13*76;y=i//13*87
    im=Image.open(ROOT/f'src/main/resources/assets/historicalpassports/textures/item/passport_{c["code"].lower()}.png')
    sheet.paste(im,(x+6,y),im)
    color='#e4cf94' if c['asset_status']=='reviewed_commons' else '#c88878'
    draw.text((x+17,y+65),c['code']+(' *' if c['asset_status']!='reviewed_commons' else ''),fill=color)
path=ROOT/'docs/screenshots/all-covers.png';path.parent.mkdir(exist_ok=True,parents=True);sheet.save(path)
print('Saved',path)
