"""Discover remaining covers from passport article infoboxes; reject files absent from Commons.
Wikipedia-only non-free images are NEVER downloaded by this script.
"""
from commons_assets import *
from bs4 import BeautifulSoup
from urllib.parse import unquote
ARTICLES={
'BB':'Barbadian passport','BS':'Bahamian passport','BT':'Bhutanese passport','BW':'Botswana passport','CL':'Chilean passport',
'FJ':'Fijian passport','GM':'Gambian passport','GQ':'Equatoguinean passport','GR':'Greek passport','KI':'Kiribati passport',
'LS':'Lesotho passport','ML':'Malian passport','MU':'Mauritian passport','MW':'Malawian passport','NR':'Nauruan passport',
'NZ':'New Zealand passport','OM':'Omani passport','PW':'Palauan passport','PY':'Paraguayan passport','SB':'Solomon Islands passport',
'SR':'Surinamese passport','SS':'South Sudanese passport','SV':'Salvadoran passport','TO':'Tongan passport','UY':'Uruguayan passport',
'WS':'Samoan passport','ZM':'Zambian passport','ZW':'Zimbabwean passport','AM':'Armenian passport'}
path=ROOT/'assets/additional_discovery.json';rows=json.loads(path.read_text(encoding='utf-8')) if path.exists() else []
for code,article in ARTICLES.items():
    if any(c['code']==code for c in rows):continue
    try:
        url='https://en.wikipedia.org/wiki/'+article.replace(' ','_');r=SESSION.get(url,timeout=30);r.raise_for_status()
        soup=BeautifulSoup(r.text,'html.parser');a=soup.select_one('.infobox-image a.mw-file-description') or soup.select_one('.infobox-image a')
        if a:
            title=unquote(a.get('href','')).split('/wiki/')[-1].removeprefix('./').replace('_',' ')
            im=a.select_one('img');preview=im.get('src','') if im else '';preview='https:'+preview if preview.startswith('//') else preview
            rows.append({'code':code,'title':title,'preview_url':preview,'discovery_source':url})
            print(code,title,flush=True)
        else:rows.append({'code':code,'error':'No infobox cover found','discovery_source':url})
    except Exception as e:print(code,str(e),flush=True)
    write(path,rows);time.sleep(1.5)
titles=[c['title'] for c in rows if 'title' in c]
meta_path=ROOT/'assets/additional_metadata.json'
if not meta_path.exists():write(meta_path,api(titles='|'.join(titles),prop='imageinfo',iiprop='url|extmetadata|size',redirects=1))
meta=json.loads(meta_path.read_text(encoding='utf-8'))['query'];by={p['title'].replace('_',' '):p.get('imageinfo',[None])[0] for p in meta['pages'].values()}
redirects={p['from'].replace('_',' '):p['to'].replace('_',' ') for p in meta.get('normalized',[])+meta.get('redirects',[])}
for c in rows:
    if 'title' not in c:continue
    title=c['title']
    for _ in range(4):
        if title in redirects and redirects[title]!=title:title=redirects[title]
        else:break
    c['verified_metadata']=by.get(title)
    if rejection_reason(title):c['status']='Rejected: '+rejection_reason(title);continue
    if not c['verified_metadata']:
        c['status']='Not available on Commons. Wikipedia-only/non-free or missing source; NOT imported.';continue
    if license_issue(c['verified_metadata']):c['status']=license_issue(c['verified_metadata']);continue
    license=plain(c['verified_metadata'].get('extmetadata',{}).get('LicenseShortName',{}).get('value','')).lower()
    if not any(s in license for s in ['cc by','cc0','public domain','pd']):c['status']='License unresolved';continue
    c['status']='Awaiting visual verification'
    preview_path=ROOT/f'assets/gallery_previews/{c["code"]}.png'
    if not preview_path.exists():
        try:
            r=SESSION.get(c['preview_url'],timeout=30);r.raise_for_status();save(Image.open(io.BytesIO(r.content)).convert('RGBA'),preview_path);time.sleep(1.5)
        except Exception as e:c['status']='Preview failed: '+str(e)
write(path,rows)
print('Reusable-license metadata:',sum(bool(c.get('verified_metadata')) for c in rows),'/',len(rows))
