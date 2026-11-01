"""Download an explicit selection from cached Commons metadata into a review contact sheet.
This does not import or approve candidates for use in the mod.
"""
from commons_assets import *
from PIL import ImageDraw
SELECT={
 'AF':'File:Afghan Passport first issued in 2017.jpg',
 'AU':'File:R-series-cover-front.jpg',
 'BA':'File:Bosnian Passport Cover.jpg',
 'BD':'File:Bangladeshi Passport Cover.jpg',
 'BG':'File:Bulgarian Passport Front.jpg',
 'KR':'File:South Korea New Passport Cover.png',
 'KZ':'File:KAZ passport.jpg',
 'LA':'File:Cover of Lao Passport.jpg',
 'LB':'File:Lebanese passport.jpg',
 'LI':'File:FürstentumLiechtensteinReisepassCover.png',
 'LK':'File:SL Passport Oct 2025.png'
}
items=[]
for code,title in SELECT.items():
    result=json.loads((ROOT/f'assets/research/{code}.json').read_text(encoding='utf-8'))['result']['query']['pages']
    page=next(p for p in result.values() if p['title']==title);info=page['imageinfo'][0]
    path=ROOT/f'assets/candidates/{code}.png'
    if not path.exists():
        try:
            r=SESSION.get(info['url'],timeout=45);r.raise_for_status();save(Image.open(io.BytesIO(r.content)).convert('RGBA'),path)
        except Exception as e: print(code,str(e),flush=True);continue
        time.sleep(3)
    items.append((code,path,info,title))
sheet=Image.new('RGB',(6*170,2*265),'#dedede');d=ImageDraw.Draw(sheet)
for i,(code,path,info,title) in enumerate(items):
    im=Image.open(path);im.thumbnail((160,235));x=(i%6)*170;y=(i//6)*265
    sheet.paste(im,(x,y+20),im);d.text((x+5,y+3),code,fill='black')
save(sheet,ROOT/'assets/candidate-review.png')
write(ROOT/'assets/candidate_metadata.json',[{'code':code,'title':title,'verified_metadata':info} for code,path,info,title in items])
