from pathlib import Path
import json,math
from PIL import Image,ImageDraw
root=Path('src/main/resources');a=root/'assets/historicalpassports'
def put(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
put(a/'blockstates/visa_application_desk.json',{'variants':{'':{'model':'historicalpassports:block/visa_application_desk'}}})
put(a/'models/block/visa_application_desk.json',{'parent':'historicalpassports:block/inspection_desk'})
put(a/'models/item/visa_application_desk.json',{'parent':'historicalpassports:block/visa_application_desk'})
put(root/'data/historicalpassports/loot_tables/blocks/visa_application_desk.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'historicalpassports:visa_application_desk'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
put(root/'data/historicalpassports/recipes/visa_application_desk.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'historicalpassports:inspection_desk'},{'item':'minecraft:paper'},{'item':'minecraft:ink_sac'}],'result':{'item':'historicalpassports:visa_application_desk'}})
im=Image.new('RGBA',(368,302),(230,224,197,255));d=ImageDraw.Draw(im)
for i in range(0,300,8):d.line([(x,i+int(7*math.sin(x/18+i))) for x in range(368)],fill=(185,200,180,255),width=1)
for r in range(12,140,9):d.ellipse((184-r,151-r,184+r,151+r),outline=(197,203,183,255))
d.rectangle((3,3,364,298),outline=(68,94,86,255),width=3);d.rectangle((8,8,359,293),outline=(129,144,122,255));im.save(a/'textures/gui/visa_label.png')
translations={
'application':('Visa application','Vize başvurusu'),'review':('Consular review','Konsolosluk incelemesi'),'country':('Country ISO','Ülke ISO'),'type':('Visa type','Vize türü'),'purpose':('Purpose / decision reason','Amaç / karar gerekçesi'),'stay':('Stay (days)','Kalış (gün)'),'days':('Validity days','Geçerlilik günü'),'entries':('Entries','Giriş sayısı'),'delay':('Starts in days','Başlangıç günü'),'approve':('Approve visa','Vizeyi onayla'),'deny':('Deny visa','Vizeyi reddet'),'submit':('Submit application','Başvuruyu gönder'),'prev':('Previous','Önceki'),'next':('Next','Sonraki'),'close':('Close','Kapat'),'unlimited':('-1 entries = unlimited; 0 start = today (UTC)','-1 giriş = sınırsız; 0 başlangıç = bugün (UTC)'),
'pending':('Application pending','Başvuru beklemede'),'approved':('Visa approved','Vize onaylandı'),'denied':('Visa denied','Vize reddedildi'),'invalid':('Invalid fields or document','Alanlar veya belge geçersiz'),'permission':('Visa officer permission required','Vize memuru yetkisi gerekli'),'saved':('Saved','Kaydedildi'),'issued':('Visa issued: %s','Vize düzenlendi: %s'),'result':('%s','%s'),'default_free':('Unlisted pairs are visa-free. Server roleplay rules only.','Listelenmeyen çiftler vizesizdir. Yalnızca sunucu rol yapma kurallarıdır.'),'help':('/visa: officer, applications, issue, revoke, inspect, check, policy. entries=-1: unlimited.','/visa: officer, applications, issue, revoke, inspect, check, policy. entries=-1: sınırsız.'),
'number':('Visa number','Vize numarası'),'status':('Status','Durum'),'holder':('Holder','Belge sahibi'),'passport':('Passport number','Pasaport numarası'),'issued':('Issued: %s','Düzenlendi: %s'),'validity':('Valid from / until','Başlangıç / bitiş'),'officer':('Issuing officer','Düzenleyen memur'),'empty':('No visas issued. Apply at a visa application desk.','Vize yok. Vize başvuru masasına başvurun.'),'check':('Check country','Ülkeyi kontrol et'),
'check.passport_invalid':('Passport invalid','Pasaport geçersiz'),'check.country_invalid':('Select a destination country first','Önce hedef ülkeyi seçin'),'check.prohibited':('Entry prohibited','Giriş yasak'),'check.free':('Visa-free entry','Vizesiz giriş'),'check.valid':('Valid visa available','Geçerli vize mevcut'),'check.visa_invalid':('Required visa missing or invalid','Gerekli vize eksik veya geçersiz')}
for key,en,tr in [('valid','VALID','GEÇERLİ'),('expired','EXPIRED','SÜRESİ DOLMUŞ'),('revoked','REVOKED','İPTAL EDİLMİŞ'),('not_yet_valid','NOT YET VALID','HENÜZ GEÇERLİ DEĞİL'),('exhausted','ENTRIES EXHAUSTED','GİRİŞ HAKKI TÜKENMİŞ')]:translations['status.'+key]=(en,tr)
for key,en,tr in [('tourist','Tourist visa','Turist vizesi'),('business','Business visa','Ticari vize'),('student','Student visa','Öğrenci vizesi'),('work','Work visa','Çalışma vizesi'),('transit','Transit visa','Transit vize'),('diplomatic','Diplomatic visa','Diplomatik vize'),('official','Official visa','Resmî görev vizesi'),('special','Special visa','Özel vize')]:translations['type.'+key]=(en,tr)
for i,lang in enumerate(['en_us','tr_tr']):
 p=a/'lang'/f'{lang}.json';v=json.loads(p.read_text(encoding='utf-8'));v.update({'visa.'+k:t[i] for k,t in translations.items()});v['block.historicalpassports.visa_application_desk']=['Visa application desk','Vize başvuru masası'][i];v['visa.issued_date']=['Issue date','Düzenleme tarihi'][i];put(p,v)

for lang in ["en_us","tr_tr"]:
 p=a/"lang"/f"{lang}.json";v=json.loads(p.read_text(encoding="utf-8"))
 for state in ["expired","revoked","not_yet_valid","exhausted"]:v["visa.check."+state]=("Vize: " if lang=="tr_tr" else "Visa: ")+v["visa.status."+state]
 put(p,v)
