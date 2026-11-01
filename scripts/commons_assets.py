"""Research every country, but import ONLY explicitly reviewed covers in assets/approved_sources.json.
Never automatically equate search relevance or a Commons upload with permission or authenticity.
python scripts/commons_assets.py research  /  import  / report
"""
from pathlib import Path
import json, sys, time, re, html, io, hashlib
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'.tools/python'))
import requests
from PIL import Image
from generate_resources import ASSETS, RES, write, save
SESSION=requests.Session();SESSION.headers['User-Agent']='HistoricalPassportsAssetResearch/1.0 (Minecraft educational roleplay; local asset audit)'
API='https://commons.wikimedia.org/w/api.php'
def api(**params):
    r=SESSION.get(API,params=dict(action='query',format='json',**params),timeout=40)
    if r.status_code == 429:
        raise SystemExit('Commons rate limit reached. Research is resumable; wait before retrying. No further requests sent.')
    r.raise_for_status();return r.json()
def plain(v): return html.unescape(re.sub('<[^>]+>','',v or '')).strip()
def rejection_reason(title):
    path=ROOT/'assets/rejected_sources.json'
    if not path.exists():return None
    return next((r['reason'] for r in json.loads(path.read_text(encoding='utf-8')) if r['title'].replace('_',' ').casefold()==title.replace('_',' ').casefold()),None)
def license_issue(info):
    meta=info.get('extmetadata',{})
    categories=plain(meta.get('Categories',{}).get('value','')).lower()
    restrictions=plain(meta.get('Restrictions',{}).get('value','')).lower()
    if restrictions=='ai' or 'ai-generated' in categories or 'ai generated' in categories:
        return 'AI-generated depiction: authenticity as an issued cover has not been established'
    for flag in ['disputed copyright','deletion request','copyright violation','missing permission','without a source','without a license']:
        if flag in categories:return 'Unresolved Commons rights warning: '+flag
    license=plain(meta.get('LicenseShortName',{}).get('value','')).lower()
    if not any(s in license for s in ['cc by','cc0','public domain','pd']):return 'Unreviewed license: '+license
    return None
def metadata(title):
    result=api(titles=title,prop='imageinfo',iiprop='url|extmetadata|size',iiurlwidth=600)
    page=next(iter(result['query']['pages'].values()))
    return page['imageinfo'][0]
def trim_neutral_background(im):
    """Remove only light neutral pixels connected to an outer edge, then crop alpha bounds."""
    from collections import deque
    im=im.copy();w,h=im.size;px=im.load();seen=set();queue=deque()
    for x in range(w):queue.extend([(x,0),(x,h-1)])
    for y in range(h):queue.extend([(0,y),(w-1,y)])
    while queue:
        x,y=queue.popleft()
        if (x,y) in seen or not (0<=x<w and 0<=y<h):continue
        seen.add((x,y));r,g,b,a=px[x,y]
        if a==0 or (min(r,g,b)>170 and max(r,g,b)-min(r,g,b)<30):
            px[x,y]=(r,g,b,0);queue.extend([(x-1,y),(x+1,y),(x,y-1),(x,y+1)])
    box=im.getchannel('A').getbbox()
    return im.crop(box) if box else im
def research():
    rows=json.loads((RES/'data/historicalpassports/countries/modern.json').read_text(encoding='utf-8'))
    dest=ROOT/'assets/research';dest.mkdir(parents=True,exist_ok=True)
    for i,c in enumerate(rows):
        path=dest/(c['code']+'.json')
        if path.exists(): continue
        try:
            result=api(generator='search',gsrsearch=f'{c["name_en"]} passport cover -diplomatic -visa',gsrnamespace=6,gsrlimit=4,prop='imageinfo',iiprop='url|extmetadata|size')
            write(path,{'country':c['code'],'query':c['name_en']+' passport cover','result':result,'status':'candidates require human visual and license review'})
            print(f'{i+1}/195 {c["code"]}',flush=True)
        except Exception as e: print(f'{c["code"]}: {e}',flush=True)
        time.sleep(6)
def import_assets():
    approved=json.loads((ROOT/'assets/approved_sources.json').read_text(encoding='utf-8'))
    done=[]
    for a in approved:
        try:
            if rejection_reason(a['title']):raise ValueError(rejection_reason(a['title']))
            info=a.get('verified_metadata') or metadata(a['title']); meta=info.get('extmetadata',{})
            license=plain(meta.get('LicenseShortName',{}).get('value',''))
            if license_issue(info): raise ValueError(license_issue(info))
            if meta.get('Restrictions',{}).get('value'): print('Restrictions: '+plain(meta['Restrictions']['value']))
            cache=ROOT/f'assets/sources/{a["code"]}.png'
            candidate=ROOT/f'assets/candidates/{a["code"]}.png'
            reviewed=ROOT/a['local_source'] if a.get('local_source') else None
            if reviewed and reviewed.exists():source=Image.open(reviewed).convert('RGBA')
            elif a.get('preview_url'):
                response=SESSION.get(a['preview_url'],timeout=60);response.raise_for_status();source=Image.open(io.BytesIO(response.content)).convert('RGBA')
            elif cache.exists() or candidate.exists(): source=Image.open(cache if cache.exists() else candidate).convert('RGBA')
            else:
                url=info.get('thumburl',info['url']); response=SESSION.get(url,timeout=60);response.raise_for_status()
                source=Image.open(io.BytesIO(response.content)).convert('RGBA')
            if a.get('source_pixel_sha256') and hashlib.sha256(source.tobytes()).hexdigest()!=a['source_pixel_sha256']:
                raise ValueError('Source pixels changed since visual review; review the new file before importing.')
            save(source,ROOT/f'assets/sources/{a["code"]}.png')
            if 'quad' in a:
                w,h=source.size;quad=tuple(v*(w if i%2==0 else h) for i,v in enumerate(a['quad']))
                source=source.transform((176,250),Image.Transform.QUAD,quad,Image.Resampling.BICUBIC)
            if 'crop' in a:
                w,h=source.size; box=a['crop'];source=source.crop(tuple(round(v*(w if i%2==0 else h)) for i,v in enumerate(box)))
            if a.get('trim_background'):source=trim_neutral_background(source)
            source.thumbnail((42,58),Image.Resampling.LANCZOS)
            output=Image.new('RGBA',(64,64));output.alpha_composite(source,((64-source.width)//2,(64-source.height)//2))
            save(output,ASSETS/f'textures/item/passport_{a["code"].lower()}.png')
            transform='Reviewed front cover; proportional Lanczos downsample into transparent 64×64 canvas.'
            if a.get('quad'):transform+=' Perspective rectification to the ordinary booklet aspect ratio.'
            if a.get('trim_background'):transform+=' Removed light neutral edge-connected background.'
            if a.get('crop'):transform+=' Explicit normalized cover crop.'
            entry={**a,'source_url':info['url'],'file_page':info['descriptionurl'],'author':plain(meta.get('Artist',{}).get('value','')),'license':license,'license_url':meta.get('LicenseUrl',{}).get('value','') or info['descriptionurl']+'#Licensing','metadata':meta,'transformation':transform}
            done.append(entry); print('Imported '+a['code'],flush=True)
        except Exception as e: print('IMPORT FAILED '+a['code']+': '+str(e),flush=True)
    write(ROOT/'assets/imported_sources.json',done);report()
def report():
    path=ROOT/'assets/imported_sources.json'; imported=json.loads(path.read_text(encoding='utf-8')) if path.exists() else []
    countries=json.loads((RES/'data/historicalpassports/countries/modern.json').read_text(encoding='utf-8'))
    by={a['code']:a for a in imported}
    from PIL import ImageStat
    for c in countries:
        c['texture']=f'historicalpassports:item/passport_{c["code"].lower()}'
        c['asset_status']='reviewed_commons' if c['code'] in by else 'provisional_original'
        if c['code'] in by:
            c['visual_details']=by[c['code']]['review']
            c['source']=by[c['code']]['file_page']
            source=ROOT/f'assets/sources/{c["code"]}.png'
            if source.exists():
                rgb=ImageStat.Stat(Image.open(source).convert('RGB')).median
                c['color']='#'+''.join(f'{v:02x}' for v in rgb)
    write(RES/'data/historicalpassports/countries/modern.json',countries)
    lines=['# Asset credits / Görsel atıfları','','Original code and generated provisional covers, booklet, desk, portrait and stamps: Historical Passports project, AGPL-3.0 (prior MIT notice retained in LICENSE-MIT-legacy.txt). Provisional covers use a country monogram, NOT a national coat of arms. No private passport data pages are used.','', 'Wikimedia sources retain their individual licenses. For CC BY-SA derivatives, the processed texture remains under that same license. No endorsement implied.','', 'Stamp layout references (no copied pixels): https://commons.wikimedia.org/wiki/Category:Passport_stamps','']
    for a in imported:
        lines += ['## '+a['code']+' — '+a['title'],'',f'- Source: {a["file_page"]}',f'- Author: {a["author"]}',f'- License: {a["license"]} — {a["license_url"]}',f'- Changes: {a["transformation"]}',f'- Review: {a["review"]}','']
        restrictions=plain(a.get('metadata',{}).get('Restrictions',{}).get('value',''))
        if restrictions:lines += ['- Additional source notice: '+restrictions+' (see the linked file page).','']
    (ROOT/'ASSET_CREDITS.md').write_text('\n'.join(lines),encoding='utf-8')
    lines=['# Passport cover coverage','','Every country has a local 64×64 texture. Authenticity is tracked separately from file coverage.',f'Imported reviewed covers: {len(by)}/195. Original provisional substitutes: {195-len(by)}/195.',
        '','Research: after an initial HTTP 429 interruption, the ordinary-passport gallery supplied 166 country candidates and remaining countries were investigated separately. Every imported cover was visually reviewed, matched to its country, checked for a reusable individual file license and Commons warning categories. Sources include both photos/scans and vector depictions, not a guarantee of the latest edition.',
        '','Disputed licenses, proposed designs, AI-generated depictions and data-page images were excluded; see assets/rejected_sources.json. Wikipedia-only images without a verified Commons license were not imported. The remaining substitutes mean a suitable reusable cover was not verified in this research, not that no such image can exist.','', '| ISO | Country | Coverage |','|---|---|---|']
    for c in countries:
        status='Reviewed Commons cover; see credits' if c['code'] in by else 'ORIGINAL PROVISIONAL SUBSTITUTE — authentic modern ordinary cover not verified/imported'
        lines.append(f'| {c["code"]} | {c["name_en"]} | {status} |')
    (ROOT/'ASSET_COVERAGE.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
if __name__=='__main__': {'research':research,'import':import_assets,'report':report}[sys.argv[1]]()
