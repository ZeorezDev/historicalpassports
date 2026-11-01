# Test raporu

## 1.1.0 vize güncellemesi — 1 Kasım 2026

`docs/test-evidence/visa-final-tests.log`: 12 zorunlu Forge GameTest ve derleme.
İlk sekiz test mevcut pasaport regresyon testleridir; dört yeni test vize
durumları ve kalıcılığı, politika ve pasaport iptalini, giriş sayacı/paket
tekrarını ve başvuru yetkisi/onay tekrarını kapsar. Eski damga testine hedef
ülke seçimi eklendi; önceki güvenlik kontrolleri kaldırılmadı.

Gerçek istemcide önceki test dünyası açıldı; yeni masadan DE turist vizesi
başvurusu gönderildi, ayrı memur rolü verildi, konsolosluk ekranında onaylandı
ve mevcut Türk pasaportunun Vizeler sayfasında görüntülendi. Onay animasyonu
görüldü. Ses işitsel olarak değerlendirilmedi. Vize etiketi 854×512 ve
1920×1032 pencerelerde incelendi. Görseller `docs/screenshots/visa-*.png`,
istemci kaydı `docs/test-evidence/visa-client.log` içindedir.

Son küçük ekran/komut düzeltmeleri derlenip sunucu testlerinden geçirildi;
ekran görüntüleri bu son düzeltmelerden önceki çalışan istemciye aittir.
İki gerçek istemciyle uzak sunucu testi henüz yapılmadı. İngilizce ekran
görsel olarak incelenmedi; dil anahtar eşitliği kontrol edildi. Özellik ve
test sınırlarının ayrıntıları `VISA_GUIDE.md` dosyasındadır.

## 1.0.0 temel sürüm — 31 Ekim 2026

Ortam: Windows, Oracle JDK 17.0.12, Gradle 8.8, Minecraft 1.20.1,
resmî Forge MDK / Forge 47.4.28. Testler gerçekten çalıştırıldı.

## Geçen otomatik kontroller

`gradlew.bat build runGameTestServer`: **BUILD SUCCESSFUL**.
Dağıtım JAR'ı `reobfJar` adımından geçti. Normal `test` görevi JUnit testi
bulunmadığı için `NO-SOURCE` verir; aşağıdaki testler gerçek Forge GameTest
sunucusunda çalışır.

**8/8 zorunlu GameTest geçti:**

1. **countryRegistry:** 195 temel ülke ve karşılık gelen Forge eşya kayıtları.
2. **forgeryAndClones:** yaratıcı örneklerin reddi; resmî kayıt eşleşmesi;
   sahte mühür, isim değişikliği ve ülke değiştirme girişimleri; kopyanın aynı
   kimliğe bağlanması ve iptalin bütün kopyalara yansıması; değiştirilmiş eşya
   durum ipucunun sunucunun gerçek iptal bilgisiyle yeniden yazılması.
3. **diskPersistence:** gerçek sıkıştırılmış NBT dosyasına yazma/yeniden okuma;
   roller, onay/ret geçmişi, iptal ve çevrimdışı iade; sona erme sınırı ve süre
   kontrolünü kapatma; önceki reddin belgeyi iptal etmemesi.
4. **rolesAndUnsolicitedPackets:** yetkisiz oyuncu, memur atama/kaldırma ve
   mevcut oturumu olmayan işlem paketi.
5. **deskEscrowAndOfflineReturn:** tek yuva, envanterden eksiltme, masa NBT
   kaydı, dolu masaya ikinci belgeyi reddetme ve iki kez iade ile çoğalmama.
6. **historyLimitPreservesRecords:** sınır dolduğunda yeni kararı reddetme ve
   eski kaydı koruma.
7. **sessionSecurityAndStamping:** gerçek sunucu işlem yolunda yetkisiz damga
   reddi, yetkili onay ve ret, giriş/çıkış ayrımı, slot değişikliği, aynı
   oturum anahtarını tekrar kullanma, işlem ortasında rol kaldırma ve
   envanterden çıkarılmış belgeye erişimin kesilmesi.
8. **deskOwnershipAndBlockRemoval:** gerçek blok kullanım yolunda başkasının
   belgesini teslim etmenin reddi, sahibin teslimi, yabancı oyuncunun iadeyi
   tetikleyememesi, masa kırılınca çevrimdışı iade ve Brigadier yönetici
   komutlarının yetki sınırları.

`python scripts/validate_resources.py`: **PASS**. 195 benzersiz ülke,
64×64 RGBA/şeffaf kapaklar, doğru model eşleştirmesi, JSON sözdizimi,
Türkçe/İngilizce anahtar eşitliği ve kodda kullanılan sabit çeviri anahtarları.

## Gerçek istemcide yapılan kontroller

- Forge 47.4.28 geliştirme istemcisi açıldı ve yerel yaratıcı dünya oluşturuldu.
- `/passport issue Dev DE` ve `/passport officer grant Dev` çalıştı.
- Pasaporta sağ tıklamak iki sayfalı kitapçığı açtı; Alman kapağı ve skin
  portresi görüntülendi.
- Memur rolü olmadan karar düğmeleri görünmedi; rol verildikten sonra göründü.
- Kontrol noktası yazıldı; ONAYLA giriş damgası, REDDET çıkış damgası oluşturdu.
- Kimlik, Damgalar ve Geçmiş sayfaları arasında gezinildi.
- İstemci tamamen kapatıldı; dünya ve uygulama yeniden açıldı. Memur rolü ve
  iki karar korundu. Önceki ret belgenin GEÇERLİ durumunu değiştirmedi.
- `tr_tr` ile ülke adı, alanlar, düğmeler, giriş/çıkış ve geçmiş doğru Türkçe
  karakterlerle görüntülendi. 854×512 pencere boyutunda kontroller erişilebildi.
- Son istemci çalıştırmasında özgün **ONAYLANDI / REDDEDİLDİ** Türkçe damga
  dokuları da oyun içinde görüntülenip ekran görüntüsü alındı.
- Yerel mod dokularıyla yükleme tamamlandı; modun eksik model/doku hatası
  istemci kaydında görülmedi.
- 165 Wikimedia kapağını içeren son kaynaklarla istemci yeniden açıldı.
  `/passport issue Dev TR` çalıştı; Türk pasaportunun kapağı, kimlik alanları
  ve Türkçe düğmeleri oyun içinde doğrulandı.

Kanıtlar: `docs/test-evidence/final-verification.log`,
`docs/screenshots/identity-tr.png`, `docs/screenshots/stamps-tr.png`,
`docs/screenshots/stamps-en.png`, `docs/screenshots/turkish-passport.png`.
Son kapaklarla istemci çalıştırma kaydı `docs/test-evidence/final-assets-build.log`,
195 teslim edilen kapağın toplu görünümü `docs/screenshots/all-covers.png` içindedir.

## Henüz doğrulanmayanlar ve sınırlar

- İki ayrı gerçek oyuncu istemcisiyle uzak sunucuda uçtan uca çok oyunculu
  kabul testi yapılmadı. Masa işlemleri Forge sunucu testleriyle sınandı.
- Ayrı bir üretim Forge kurulumunda paketlenmiş JAR ile uzun süreli yük testi
  yapılmadı. Dedicated-server tarafı Forge GameTestServer'da başlatılıp
  çalıştırıldı; istemci sınıfı yükleme çökmesi yaşanmadı.
- Sesin doğru olayla çağrılması ve yapılandırılması kodda mevcut; bu ortamda
  işitsel kalite dinlenerek değerlendirilmedi.
- Görsel doğrulama tek pencere boyutunda yapıldı; bütün GUI ölçekleri ve
  çözünürlük kombinasyonları test edilmedi.
- Ani güç kaybı/işlem çökmesi sırasında farklı dünya dosyaları arasında atomik
  kayıt garantisi yoktur; standart Minecraft kaydetme davranışı kullanılır.
- **165/195** kapak Wikimedia'dan lisans/görüntü kontrolüyle işlendi;
  **30/195** kapak açıkça belirtilmiş özgün geçici çizimlerdir. İlk HTTP 429
  kesintisinden sonra galeri üzerinden toplu araştırma sürdürüldü. Lisans
  itirazlı, yapay zekâ üretimi, öneri tasarımı ve veri sayfası adayları elendi.
  Kalan ülkelerde uygun yeniden kullanılabilir kapak doğrulanamadı; bunun
  hiçbir yerde böyle bir görsel bulunmadığı anlamına geldiği iddia edilmiyor.
  Bazı kapaklar önceki sıradan pasaport baskılarını veya bunların vektör
  betimlemelerini temsil eder; tamamı en son baskı olarak sunulmaz.
- Vize düzenleme ve paketlenmiş tarihî SU/YU içerikleri bulunmuyor.

İlk test çalıştırmasında yapay oyuncu bağlantısı nedeniyle bir test başarısız
oldu; Forge FakePlayer düzeneğine geçildikten sonra geçti. Başarısız ilk
çalıştırma başarılıymış gibi sayılmadı. Son doğrulama sekiz testin tamamını
içerir.
