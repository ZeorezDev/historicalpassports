"""Batch Commons license lookup and bounded preview download for human visual review.
Never imports candidates into game resources; review and explicit approval are separate.
"""
from commons_assets import *
from bs4 import BeautifulSoup
from urllib.parse import unquote
import unicodedata
def norm(s):return ''.join(c for c in unicodedata.normalize('NFKD',s.casefold()) if c.isalnum())
countries=json.loads((RES/'data/historicalpassports/countries/modern.json').read_text(encoding='utf-8'))
lookup={norm(c['name_en']):c['code'] for c in countries}
lookup.update({norm(k):v for k,v in {'Congo':'CG','DR Congo':'CD','Côte d’Ivoire':'CI','Turkey':'TR','Vatican City':'VA','Palestine':'PS','Micronesia':'FM','Cabo Verde':'CV','North Korea':'KP','South Korea':'KR','Antigua and Barbuda':'AG','Bosnia and Herzegovina':'BA','Saint Kitts and Nevis':'KN','Saint Lucia':'LC','Myanmar':'MM','São Tomé and Príncipe':'ST','Trinidad and Tobago':'TT','Saint Vincent and the Grenadines':'VC'}.items()})
soup=BeautifulSoup((ROOT/'assets/research/passport_gallery.html').read_text(encoding='utf-8'),'html.parser')
regions={'Africa','North and Central America','South America','Asia','Europe','Oceania'}
candidates=[]
for box in soup.select('.gallerybox'):
    heading=box.find_previous(['h2','h3']);section=heading.get_text(' ',strip=True) if heading else ''
    if section not in regions:continue
    caption=box.select_one('.gallerytext');a=box.select_one('.thumb a');img=box.select_one('img')
    if not caption or not a or not img:continue
    code=lookup.get(norm(caption.get_text(' ',strip=True)))
    if not code:continue
    title=unquote(a.get('href','')).split('/wiki/')[-1].removeprefix('./').replace('_',' ')
    if 'proposed' in title.lower():continue
    url=img.get('src','');url='https:'+url if url.startswith('//') else url
    candidates.append({'code':code,'title':title,'preview_url':url,'caption':caption.get_text(' ',strip=True),'section':section})
print(f'{len(candidates)} ordinary-country candidates',flush=True)
cache=ROOT/'assets/gallery_metadata';cache.mkdir(parents=True,exist_ok=True)
for start in range(0,len(candidates),40):
    path=cache/f'batch-{start//40}.json'
    if not path.exists():
        result=api(titles='|'.join(c['title'] for c in candidates[start:start+40]),prop='imageinfo',iiprop='url|extmetadata|size',redirects=1)
        write(path,result);print('Metadata batch '+str(start//40),flush=True);time.sleep(6)
metadata_map={}
redirects={}
for path in cache.glob('*.json'):
    q=json.loads(path.read_text(encoding='utf-8'))['query']
    redirects.update({norm(x['from']):norm(x['to']) for x in q.get('normalized',[])+q.get('redirects',[])})
    for p in q['pages'].values():
        if 'imageinfo' in p: metadata_map[norm(p['title'])]=p['imageinfo'][0]
missing=[c for c in candidates if norm(c['title']) not in metadata_map and norm(c['title']) not in redirects]
if missing:
    result=api(titles='|'.join(c['title'] for c in missing),prop='imageinfo',iiprop='url|extmetadata|size',redirects=1)
    write(cache/'supplement.json',result)
    for p in result['query']['pages'].values():
        if 'imageinfo' in p:metadata_map[norm(p['title'])]=p['imageinfo'][0]
for c in candidates:
    key=norm(c['title'])
    for _ in range(5):
        if key in redirects and redirects[key]!=key:key=redirects[key]
        else:break
    c['verified_metadata']=metadata_map.get(key)
write(ROOT/'assets/gallery_candidates.json',candidates)
if '--download' in sys.argv:
    for c in candidates:
        path=ROOT/f'assets/gallery_previews/{c["code"]}.png'
        if path.exists():continue
        info=c['verified_metadata']
        if not info:continue
        if license_issue(info) or rejection_reason(c['title']):continue
        license=plain(info.get('extmetadata',{}).get('LicenseShortName',{}).get('value','')).lower()
        if not any(s in license for s in ['cc by','cc0','public domain','pd']):continue
        try:
            r=SESSION.get(c['preview_url'],timeout=30)
            if r.status_code==429:print('Preview download rate-limited; stopping, rerun later.',flush=True);break
            r.raise_for_status();im=Image.open(io.BytesIO(r.content)).convert('RGBA');save(im,path)
            print('Preview '+c['code'],flush=True);time.sleep(1.5)
        except Exception as e:print(c['code'],str(e),flush=True)
    from PIL import ImageDraw
    files=sorted((ROOT/'assets/gallery_previews').glob('*.png'))
    for start in range(0,len(files),20):
        sheet=Image.new('RGB',(5*170,4*245),'#d0d0d0');d=ImageDraw.Draw(sheet)
        for i,path in enumerate(files[start:start+20]):
            im=Image.open(path);im.thumbnail((160,220));x=i%5*170;y=i//5*245
            sheet.paste(im,(x,y+20),im);d.text((x+4,y+3),path.stem,fill='black')
        save(sheet,ROOT/f'assets/gallery-review-{start//20}.png')
