# Historical Passports — Forge 1.20.1

Minecraft **1.20.1**, Forge **47.4.28**, Java **17** için gerçek kaynak proje.
195 ülke pasaportu, sunucuda doğrulanan belge sicili, gümrük memurluğu,
çok sayfalı kitapçık, kalıcı kontrol kararları ve teslim/iade masası içerir.

Bu sürüm **geliştirme önizlemesidir**. Ülke/eşya kapsamı 195/195 olsa da
kapakların tamamı otantik değildir. Gerçek kapak ve geçici çizim ayrımı
`ASSET_COVERAGE.md` dosyasındadır. Son testlerin kapsamını `TEST_REPORT.md`
belirtir; otomatik testler canlı çok oyunculu kabul testi yerine geçmez.

Son kapsam: **165 ülke için lisansı ve görüntüsü kontrol edilmiş Wikimedia
kapak fotoğrafı/taraması/vektör çizimi; 30 ülke için özgün geçici kapak**.
Bazıları önceki sıradan pasaport baskılarını temsil eder. Lisans itirazlı,
yapay zekâ üretimi, öneri tasarımı ve kişisel veri sayfası olan adaylar elendi.

## Kurulum

1. Minecraft 1.20.1 ve Forge 47.4.28 kurun; Java 17 kullanın.
2. `dist/historicalpassports-1.1.0.jar` (kendiniz derlerseniz
   `build/libs/historicalpassports-1.1.0.jar`) dosyasını hem istemcinin hem
   sunucunun `mods` klasörüne koyun. İki tarafta aynı sürüm gereklidir.
3. Dünyayı/sunucuyu açın. Oynarken Wikimedia veya başka bir görsel sunucusuna
   bağlantı kurulmaz; bütün mod dokuları JAR içindedir.

## İlk kullanım

Yönetici olarak:

```text
/passport issue Steve TR
/passport officer grant Alex
```

Steve kendi pasaportuna sağ tıklayarak kimlik bilgilerini görebilir. Memur
pasaportu envanterine alabilir veya masadan inceleyebilir. Her karar için önce
**GİRİŞ/ÇIKIŞ** seçin, kontrol noktası girin ve **ONAYLA/REDDET** kullanın.
Gerekçe isteğe bağlıdır. Onay/ret ile belgenin iptal edilmesi ayrı işlemlerdir.
Eski bir ret, geçerli pasaportu kalıcı olarak geçersiz yapmaz.

### Komutlar

| Komut | Yetki / davranış |
|---|---|
| `/passport issue <player> <ISO>` | OP 2; sunucu kayıtlı yeni belge düzenler |
| `/passport revoke <passport_id>` | OP 2; belge kimliğinin bütün kopyalarını iptal eder |
| `/passport inspect <passport_id>` | Memur veya OP 2; sicilden salt okunur inceleme |
| `/passport officer grant <player>` | OP 2; çevrimiçi oyuncuya kalıcı rol verir |
| `/passport officer revoke <name-or-UUID>` | OP 2; çevrimdışı memurun rolünü de kaldırır |
| `/passport officer list` | OP 2; memurları gösterir |
| `/passport countries` | Ülke kodlarını listeler |
| `/passport collect` | Envanterde yer varsa bekleyen iadeleri alır |
| `/passport help` | Dil ayarına uygun yardım |

Ülke kodları büyük/küçük harfe duyarsızdır. Belge numarası düzenleme mesajında
ve pasaportta görünür. Yaratıcı sekmedeki pasaportlar **örnektir**; kendiliğinden
geçerli sayılmaz. Varsayılan ayarda OP olmak tek başına damgalama yetkisi vermez;
memur rolü ayrıca atanır.

Eşya ipucundaki durum, envanterdeyken sunucunun saniyede bir yenilediği son
denetim bilgisidir. Güvenlik kararları bu ipucuna dayanmaz; kitapçık açılırken
ve her işlemde asıl sunucu sicili yeniden doğrulanır.

### Pasaport kontrol masası

Yaratıcı sekmeden alınabilir veya 5 tahta, 2 kitap ve 1 demir külçesiyle
üretilebilir (tarif dizilimi: `PPP / BIB / P P`).
Elinizde kendi resmî pasaportunuzla boş masaya sağ tıklayın. Tek belgelik
teslim yuvası sunucuda tutulur. Memur masaya sağ tıklayıp inceleyebilir.
**Sahibine iade et** düğmesi veya eğilerek sağ tıklama belgeyi teslim eden
oyuncuya gönderir. Memurun envanterine aktarılmaz.

Oyuncu çevrimdışıysa ya da envanteri doluysa belge dünya kaydındaki teslim
kuyruğunda kalır. Sonraki girişte veya `/passport collect` ile alınır.
Masa kırıldığında da aynı iade yolu kullanılır. Hopper/otomatik envanter
erişimi ve istemciden slot taşıma paketleri sunulmaz.

### Kitapçık

- **Kimlik:** ülke kapağı, oyuncu portresi, ad/UUID, numara, tarihler ve durum.
- **Damgalar:** yeşil/kırmızı özgün damga dokuları, tarih, giriş/çıkış, memur.
- **Geçmiş:** sunucudaki bütün kayıtlar dörderli sayfalarla; ayrıntı için
  kaydın üzerine gelin.
- **Vizeler:** ülkelere ait kalıcı vize etiketleri, durum ve giriş kullanım bilgileri.
  Başvuru, memur incelemesi ve politika ayarları için `VISA_GUIDE.md` belgesine bakın.

Çevrimiçi oyuncunun mevcut skin portresi kullanılır. Skin bilgisi yoksa yerel
özgün varsayılan portre gösterilir. İstemcinin seçili dili `tr_tr` veya `en_us`
olduğunda tüm etiketler ilgili dilde gösterilir.

## Derleme ve test

Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

Alternatif: `scripts/build.ps1 -JavaHome <JDK17> -GameTests`.
Linux/macOS: `JAVA_HOME=/path/to/jdk17 ./gradlew build`.
İlk derleme Gradle, Forge ve Minecraft geliştirme bağımlılıklarını indirir.
JAR, ForgeGradle `reobfJar` ile oyun adlarına dönüştürülür.

Görsel kaynaklarını yeniden üretmek için:

```text
python -m pip install -r scripts/requirements.txt
python scripts/generate_resources.py
python scripts/commons_assets.py research
python scripts/commons_assets.py import
python scripts/validate_resources.py
```

`generate_resources.py` önce özgün geçici kapakları üretir; `import` doğrulanmış
Commons kapaklarını bunların üzerine işler. Araştırma sonucu otomatik onay
anlamına gelmez. Kullanılacak kaynaklar, dosya lisansı ve görsel incelendikten
sonra `assets/approved_sources.json` içinde açıkça seçilir. Araştırma kaldığı
yerden devam eder ve 429 yanıtında durur. `assets/imported_sources.json`
gerçekte işlenen kaynakları kaydeder; dosya lisansları JAR'a da eklenir.
Toplu galeri araştırması için sırasıyla `gallery_research.py` ve
`gallery_candidates.py --download` kullanılabilir. `missing_cover_research.py`
kalan ülkeleri araştırır; yalnızca Wikipedia'da bulunan ve Commons lisansı
doğrulanamayan dosyaları indirmez. `assets/rejected_sources.json` bilinen
uygunsuz adayları kaydeder. Yeni kaynaklarda insan/görsel inceleme gereklidir;
`approve_reviewed_covers.py` yalnızca 31 Ekim 2026'da incelenen sabit listedir.
İncelenmiş kaynak piksel özeti değişirse içe aktarma yeniden inceleme ister.

## Ayarlar ve kayıtlar

Sunucu: `<world>/serverconfig/historicalpassports-server.toml`.
İstemci: `config/historicalpassports-client.toml`.

Sunucu seçenekleri: süre kontrolü, varsayılan geçerlilik günü, masa kullanımı,
tarihî ülke düzenleme, azami geçmiş uzunluğu ve memura özel damgalama.
`officerOnlyStamping=false` yalnızca OP 2 kullanıcılarını da yetkili kılar;
sıradan oyunculara hiçbir zaman resmî damga yetkisi vermez.
Geçmiş sınırına ulaşıldığında eski kararlar silinmez; yeni karar reddedilir.
İstemci seçenekleri: damga sesi/animasyonu ve kitapçık ülke adları için dil
tercihi (`auto`, `en_us`, `tr_tr`). Genel arayüz dili Minecraft ayarını izler.

Sicil, roller ve teslim kuyruğu: `<world>/data/historicalpassports.dat`.
Masadaki teslimler normal blok varlığı NBT verisinde saklanır. Dünya
yedeğinde hem bölge dosyalarını hem bu sicili birlikte yedekleyin.

## Güvenlik sınırları

Sunucu her karar sırasında oyuncuyu, rolü, oturum anahtarını, belgeyi ve
fiziksel erişimi yeniden denetler. Masada mesafe ve yüklü chunk kontrol edilir.
Bir karar kabul edilince oturum anahtarı değiştirilir; aynı paket ikinci kez
damga oluşturmaz. Belge sahibinin adı ve kararlar istemci NBT'sinden okunmaz.
Kimlik ile rastgele mühür anahtarı sicille eşleşmelidir.

Tam bir eşya kopyası aynı resmî kimliğe işaret eder; yeni kimlik üretmez.
Bu, kopya eşyayı fiziksel olarak silme sistemi değildir. Sunucu dosyalarına
erişebilen yöneticilere veya başka modların doğrudan sunucu verisini
değiştirmesine karşı güvenlik sınırı oluşturmaz. Ani işlem/işletim sistemi
çökmesinde Minecraft'ın son kaydettiği dünya durumu geçerlidir; ayrı işlem
günlüğü veya çökme anında atomik çok dosyalı kayıt garantisi yoktur.

## Genişletme ve kapsam

JSON mimarisi, kayıtlar ve kaynak üretimi: `docs/COUNTRY_EXTENSIONS.md`.
Tarihî SU/YU kapakları bu sürümde paketlenmedi. Doğum tarihi isteğe bağlı
rol alanı ve vize düzenleme gelecekte eklenebilir; mevcut arayüz gerçek kişisel
pasaport verisi kullanmaz.

Resmî API başvuruları:
[Forge SimpleImpl](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/),
[SavedData](https://docs.minecraftforge.net/en/1.20.1/datastorage/saveddata/),
[GameTest](https://docs.minecraftforge.net/en/1.20.1/misc/gametest/).

Kod ve özgün varlıklar MIT; Wikimedia türevleri kendi lisanslarıyla dağıtılır.
`ASSET_CREDITS.md`, `LICENSE` ve MDK'nin `LICENSE.txt` dosyalarına bakın.

## 1.1.0 vize güncellemesi

Yeni sistemin kullanımı ve test sınırları: [Vize rehberi](VISA_GUIDE.md).
İstemci ve sunucuyu birlikte güncelleyin; ağ protokolü sürüm 2 oldu.
Eski pasaport kayıt dosyası değiştirilmez; vizeler ayrı SavedData dosyasındadır.
