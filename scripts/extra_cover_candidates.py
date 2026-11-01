from commons_assets import *
chosen={'TR':'File:Epasaport turkey.jpg','CL':'File:Chilean Biometric Passport Cover.jpg','AM':'File:Հայկական անձնագիր.jpg'}
result=api(titles='|'.join(chosen.values()),prop='imageinfo',iiprop='url|extmetadata|size',iiurlwidth=250)
write(ROOT/'assets/extra_metadata.json',result)
by={p['title']:p.get('imageinfo',[None])[0] for p in result['query']['pages'].values()}
rows=[]
for code,title in chosen.items():
    info=by.get(title)
    if not info or license_issue(info):continue
    url=info.get('thumburl',info['url'])
    try:
        r=SESSION.get(url,timeout=40);r.raise_for_status();save(Image.open(io.BytesIO(r.content)).convert('RGBA'),ROOT/f'assets/extra_previews/{code}.png')
        rows.append({'code':code,'title':title,'verified_metadata':info,'preview_url':url});print('Downloaded '+code,flush=True)
    except Exception as e:print(code,str(e),flush=True)
    time.sleep(3)
write(ROOT/'assets/extra_candidates.json',rows)
