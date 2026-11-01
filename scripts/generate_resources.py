"""Reproducible local assets. pip install Pillow Babel pycountry requests.
Original provisional covers are explicitly documented; reviewed Commons files override them.
"""
from pathlib import Path
import json, sys, random, math
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.tools/python'))
from PIL import Image, ImageDraw, ImageFont
from babel import Locale
import pycountry
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/historicalpassports'
CODES = '''AF AL DZ AD AO AG AR AM AU AT AZ BS BH BD BB BY BE BZ BJ BT BO BA BW BR BN BG BF BI CV KH CM CA CF TD CL CN CO KM CG CD CR CI HR CU CY CZ DK DJ DM DO EC EG SV GQ ER EE SZ ET FJ FI FR GA GM GE DE GH GR GD GT GN GW GY HT HN HU IS IN ID IR IQ IE IL IT JM JP JO KZ KE KI KP KR KW KG LA LV LB LS LR LY LI LT LU MG MW MY MV ML MT MH MR MU MX FM MD MC MN ME MA MZ MM NA NR NP NL NZ NI NE NG MK NO OM PK PW PA PG PY PE PH PL PT QA RO RU RW KN LC VC WS SM ST SA SN RS SC SL SG SK SI SB SO ZA SS ES LK SD SR SE CH SY TJ TZ TH TL TG TO TT TN TR TM TV UG UA AE GB US UY UZ VU VA VE VN YE ZM ZW PS'''.split()
assert len(CODES) == len(set(CODES)) == 195
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
def font(size, bold=False):
    for path in [Path('C:/Windows/Fonts') / ('georgiab.ttf' if bold else 'georgia.ttf'), Path('/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf')]:
        if path.exists(): return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()
def centered(d, xy, text, f, fill):
    box=d.textbbox((0,0),text,font=f); d.text((xy[0]-(box[2]-box[0])/2,xy[1]),text,font=f,fill=fill)
def save(im, path):
    path.parent.mkdir(parents=True,exist_ok=True); im.save(path,optimize=True)
def main():
    en, tr = Locale('en'), Locale('tr')
    names={'TR':('Türkiye','Türkiye'),'VA':('Holy See (Vatican City)','Vatikan'),'PS':('State of Palestine','Filistin'),'CD':('Democratic Republic of the Congo','Kongo Demokratik Cumhuriyeti'),'CG':('Republic of the Congo','Kongo Cumhuriyeti')}
    green=set('DZ BD BF CI CM EG GA GH GN GW ID LY MA MR NG PK SA SN TG TM TZ ZM'.split())
    blue=set('AR AU BS BB BZ BR BW CA CD CR CU DO EC FJ GT HN HT IL IN IQ JM KE KI KR LB LC MH FM NA NI NR PA PG PY PW RW SB SO SS SV SY TO TT TV UA US UY VE WS'.split())
    black=set('NZ AO MW TJ'.split())
    countries=[]; lang_en={'itemGroup.historicalpassports':'Historical Passports','block.historicalpassports.inspection_desk':'Passport Inspection Desk'}
    lang_tr={'itemGroup.historicalpassports':'Tarihî Pasaportlar','block.historicalpassports.inspection_desk':'Pasaport Kontrol Masası'}
    originals=[]
    for code in sorted(CODES):
        n_en,n_tr=names.get(code,(en.territories[code],tr.territories[code]))
        c=pycountry.countries.get(alpha_2=code)
        color='#164c37' if code in green else '#172e50' if code in blue else '#20252a' if code in black else '#692836'
        row=dict(code=code,name_en=n_en,name_tr=n_tr,official_name_en=getattr(c,'official_name',c.name),passport_name=n_en+' Passport',color=color,texture=f'historicalpassports:item/passport_{code.lower()}',asset_status='provisional_original',
            visual_details='Provisional original: gold border, country monogram, guilloche seal; not an authentic national emblem.',validity_days=0,historical=False)
        countries.append(row)
        lang_en['country.historicalpassports.'+code.lower()]=n_en
        lang_tr['country.historicalpassports.'+code.lower()]=n_tr
        lang_en['item.historicalpassports.passport_'+code.lower()]=n_en+' Passport'
        lang_tr['item.historicalpassports.passport_'+code.lower()]=n_tr+' Pasaportu'
        write(ASSETS/f'models/item/passport_{code.lower()}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'historicalpassports:item/passport_{code.lower()}'}})
        # Paint at high resolution, then sample to a transparent 64px inventory sprite.
        im=Image.new('RGBA',(256,256)); d=ImageDraw.Draw(im); gold=(202,174,103)
        d.rounded_rectangle((43,9,217,247),radius=7,fill=color)
        rng=random.Random(code)
        for _ in range(7000):
            x,y=rng.randint(48,212),rng.randint(14,242)
            base=tuple(int(color[i:i+2],16) for i in (1,3,5)); delta=rng.randint(-9,8)
            d.point((x,y),fill=tuple(max(0,min(255,v+delta)) for v in base))
        d.line((50,16,50,240),fill=(15,15,15),width=3); d.rounded_rectangle((59,24,201,230),radius=3,outline=gold,width=2)
        centered(d,(130,40),code,font(23,True),gold)
        for a in range(0,360,15):
            rad=math.radians(a); x,y=130+30*math.cos(rad),122+30*math.sin(rad)
            d.ellipse((x-4,y-4,x+4,y+4),outline=gold,width=1)
        d.ellipse((111,103,149,141),outline=gold,width=2)
        centered(d,(130,111),code,font(14,True),gold)
        centered(d,(130,177),'PASSPORT',font(14,True),gold)
        d.rectangle((120,210,140,220),outline=gold,width=2); d.ellipse((126,211,134,219),outline=gold,width=1)
        save(im.resize((64,64),Image.Resampling.LANCZOS),ASSETS/f'textures/item/passport_{code.lower()}.png')
        originals.append(code)
    write(RES/'data/historicalpassports/countries/index.json',['modern.json'])
    write(RES/'data/historicalpassports/countries/modern.json',countries)
    translations={
      'hp.title':('PASSPORT / PASAPORT','PASAPORT / PASSPORT'),
      'hp.tab.identity':('Identity','Kimlik'),'hp.tab.stamps':('Stamps','Damgalar'),'hp.tab.history':('History','Geçmiş'),'hp.tab.visas':('Visas','Vizeler'),
      'hp.field.holder':('Holder','Belge sahibi'),'hp.field.number':('Document number','Belge numarası'),'hp.field.expires':('Valid until (UTC)','Son geçerlilik (UTC)'),
      'hp.field.issued':('Issued (UTC)','Düzenlenme (UTC)'),'hp.field.authority':('Issuing authority','Düzenleyen makam'),'hp.field.nationality':('Nationality','Uyruk'),
      'hp.field.status':('Document status','Belge durumu'),'hp.field.uuid':('Holder UUID','Oyuncu UUID'),
      'hp.status.valid':('VALID','GEÇERLİ'),'hp.status.expired':('EXPIRED','SÜRESİ DOLMUŞ'),'hp.status.revoked':('REVOKED','İPTAL EDİLMİŞ'),
      'hp.status.sample':('UNISSUED SPECIMEN','DÜZENLENMEMİŞ ÖRNEK'),'hp.status.unverified':('UNVERIFIED DOCUMENT','DOĞRULANAMAYAN BELGE'),
      'hp.tooltip.verify':('Open to verify with the server','Sunucuda doğrulamak için açın'),
      'hp.tooltip.last_status':('Last server check: %s','Son sunucu denetimi: %s'),
      'hp.approve':('APPROVE','ONAYLA'),'hp.deny':('DENY','REDDET'),'hp.approved':('APPROVED','ONAYLANDI'),'hp.denied':('DENIED','REDDEDİLDİ'),
      'hp.entry':('ENTRY','GİRİŞ'),'hp.exit':('EXIT','ÇIKIŞ'),'hp.checkpoint':('Checkpoint (required)','Kontrol noktası (zorunlu)'),
      'hp.reason':('Reason (optional)','Gerekçe (isteğe bağlı)'),'hp.return':('Return to owner','Sahibine iade et'),'hp.close':('Close','Kapat'),
      'hp.prev':('Previous','Önceki'),'hp.next':('Next','Sonraki'),'hp.officer':('Officer','Memur'),
      'hp.history.empty':('No inspections recorded.','Henüz kontrol kaydı yok.'),'hp.visas.empty':('No visas issued. Reserved for future visa extensions.','Vize düzenlenmemiş. Gelecekteki vize eklentileri için ayrılmıştır.'),
      'hp.sample.info':('Creative specimens are not official documents. An administrator must issue a passport.','Yaratıcı menü örnekleri resmî belge değildir. Pasaportu bir yönetici düzenlemelidir.'),
      'hp.registry':('SERVER VERIFIED RECORD','SUNUCUDA DOĞRULANMIŞ KAYIT'),
      'hp.history.count':('Records %s–%s / %s','Kayıt %s–%s / %s'),
      'hp.expiration.disabled':('Expiration disabled by server','Sunucuda süre denetimi kapalı'),
      'hp.error.permission':('You are not authorized for this action.','Bu işlem için yetkiniz yok.'),
      'hp.error.desk_disabled':('New desk inspections are disabled. Sneak-use to reclaim your document.','Yeni masa işlemleri kapalı. Belgenizi almak için eğilerek kullanın.'),
      'hp.error.submit_owner':('Submit your own officially issued passport.','Kendi adınıza düzenlenmiş resmî pasaportu teslim edin.'),
      'hp.error.invalid':('This passport is unverified, expired or revoked; approval is not allowed.','Pasaport doğrulanamıyor, süresi dolmuş veya iptal edilmiş; onaylanamaz.'),
      'hp.error.session':('Access ended. Reopen the passport near the desk or from your inventory.','Erişim sona erdi. Pasaportu masanın yakınında veya envanterinizden yeniden açın.'),
      'hp.error.checkpoint':('Enter a checkpoint name before stamping.','Damgalamadan önce kontrol noktası adını girin.'),
      'hp.error.history_full':('History is full. Issue a replacement passport; previous records are retained.','Kayıt sınırına ulaşıldı. Yeni pasaport düzenleyin; eski kayıtlar korunur.'),
      'hp.error.country':('Unknown or disabled country. Use /passport countries.','Ülke bilinmiyor veya devre dışı. /passport countries kullanın.'),
      'hp.error.not_found':('Record not found.','Kayıt bulunamadı.'),
      'hp.desk.submitted':('Passport submitted. Sneak-use the desk to reclaim it.','Pasaport teslim edildi. Almak için masayı eğilerek kullanın.'),
      'hp.desk.returned':('Passport returned to its owner or queued for delivery.','Pasaport sahibine iade edildi veya teslim kuyruğuna alındı.'),
      'hp.command.issued':('Issued passport to %s: %s','%s için pasaport düzenlendi: %s'),
      'hp.command.revoked':('Revoked passport: %s','Pasaport iptal edildi: %s'),
      'hp.command.officer_granted':('Customs officer appointed: %s','Gümrük memuru atandı: %s'),
      'hp.command.officer_revoked':('Customs officer role removed: %s','Gümrük memurluğu kaldırıldı: %s'),
      'hp.command.officers':('Customs officers: %s','Gümrük memurları: %s'),
      'hp.command.help':('/passport countries | collect | issue <player> <ISO> | revoke <id> | inspect <id> | officer grant/revoke <player> | officer list. Submit at a desk; sneak-use returns the document. Registry inspection is read-only.', '/passport countries | collect | issue <oyuncu> <ISO> | revoke <no> | inspect <no> | officer grant/revoke <oyuncu> | officer list. Masaya teslim edin; eğilerek kullanmak belgeyi iade eder. Sicilden inceleme salt okunurdur.')
    }
    for k,(a,b) in translations.items(): lang_en[k]=a; lang_tr[k]=b
    write(ASSETS/'lang/en_us.json',lang_en); write(ASSETS/'lang/tr_tr.json',lang_tr)
    # Original paper, stitch and guilloche pattern.
    paper=Image.new('RGBA',(880,448)); d=ImageDraw.Draw(paper); d.rounded_rectangle((0,0,879,447),radius=15,fill='#352d28')
    rng=random.Random(1948)
    for l,r in [(12,438),(442,868)]:
        d.rectangle((l,12,r,432),fill='#e9dfc5')
        for _ in range(18000):
            x,y=rng.randint(l,r),rng.randint(12,432); v=rng.randint(-12,8); d.point((x,y),fill=(233+v,223+v,197+v))
        for off in range(-300,500,10):
            pts=[(x,max(25,min(419,230+math.sin((x+off)/35)*35+off/2))) for x in range(l+14,r-13,2)]
            d.line(pts,fill='#d7d2b9',width=1)
        d.rectangle((l+12,24,r-12,420),outline='#a69a78',width=1)
    for x in range(430,450):
        a=int(70*(1-abs(x-440)/10)); d.line((x,12,x,432),fill=(70,50,25,a+100))
    save(paper,ASSETS/'textures/gui/booklet.png')
    for name,label,color in [('approved','APPROVED','#2e714f'),('denied','DENIED','#a13b38'),('approved_tr','ONAYLANDI','#2e714f'),('denied_tr','REDDEDİLDİ','#a13b38')]:
        im=Image.new('RGBA',(256,96)); d=ImageDraw.Draw(im)
        d.rounded_rectangle((5,5,250,90),radius=9,outline=color,width=5); d.rectangle((12,12,243,83),outline=color,width=2)
        centered(d,(128,18),label,font(27,True),color); d.line((24,60,232,60),fill=color,width=2)
        rng=random.Random(name)
        for _ in range(1200):
            x,y=rng.randrange(256),rng.randrange(96); d.rectangle((x,y,x+rng.randrange(1,3),y),fill=(0,0,0,0))
        save(im,ASSETS/f'textures/gui/{name}.png')
    # Default pixel portrait, no personal information.
    im=Image.new('RGBA',(32,32),'#82939a');d=ImageDraw.Draw(im);d.rectangle((9,5,22,20),fill='#bca084');d.rectangle((8,4,23,9),fill='#423d35');d.rectangle((5,23,26,31),fill='#5c7770');d.rectangle((11,12,12,13),fill='#373936');d.rectangle((19,12,20,13),fill='#373936'); save(im,ASSETS/'textures/gui/portrait.png')
    wood=Image.new('RGB',(32,32),'#604436');d=ImageDraw.Draw(wood)
    for y in range(32): d.line((0,y,31,y),fill=(90+y%5*4,61+y%5*3,43+y%5*2))
    save(wood,ASSETS/'textures/block/desk_wood.png')
    top=wood.copy(); d=ImageDraw.Draw(top); d.rectangle((4,3,23,27),fill='#30584f');d.rectangle((7,7,19,22),fill='#ded3af');d.line((13,7,13,22),fill='#9c8c6f');d.rectangle((25,9,29,15),fill='#9e3534');save(top,ASSETS/'textures/block/desk_top.png')
    faces={s:{'texture':'#wood'} for s in ['north','south','east','west','up','down']}
    elements=[]
    for low,high in [([0,12,0],[16,16,16]),([1,0,1],[4,12,15]),([12,0,1],[15,12,15])]:
        f=json.loads(json.dumps(faces))
        if high[1]==16: f['up']['texture']='#top'
        elements.append({'from':low,'to':high,'faces':f})
    write(ASSETS/'models/block/inspection_desk.json',{'parent':'minecraft:block/block','textures':{'wood':'historicalpassports:block/desk_wood','top':'historicalpassports:block/desk_top','particle':'historicalpassports:block/desk_wood'},'elements':elements})
    write(ASSETS/'models/item/inspection_desk.json',{'parent':'historicalpassports:block/inspection_desk'})
    write(ASSETS/'blockstates/inspection_desk.json',{'variants':{'':{'model':'historicalpassports:block/inspection_desk'}}})
    write(RES/'data/historicalpassports/loot_tables/blocks/inspection_desk.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'historicalpassports:inspection_desk'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    write(RES/'data/historicalpassports/recipes/inspection_desk.json',{'type':'minecraft:crafting_shaped','pattern':['PPP','BIB','P P'],'key':{'P':{'tag':'minecraft:planks'},'B':{'item':'minecraft:book'},'I':{'item':'minecraft:iron_ingot'}},'result':{'item':'historicalpassports:inspection_desk'}})
    write(RES/'data/minecraft/tags/blocks/mineable/axe.json',{'replace':False,'values':['historicalpassports:inspection_desk']})
    print(f'Generated {len(countries)} countries, localized names, original provisional covers and GUI assets.')
if __name__=='__main__': main()
