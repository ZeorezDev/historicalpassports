# Historical Passports 1.1.0 — Vize sistemi

Minecraft 1.20.1 / Forge 47.4.28 / Java 17. İstemci ve sunucuda aynı 1.1.0
JAR kullanılmalıdır. Güncellemeden önce dünyanızı yedekleyin.
Eski pasaport/eşya kimlikleri ve `historicalpassports.dat` korunur. Yeni
vizeler, başvurular, vize memurları ve ülke kuralları
`data/historicalpassports_visas.dat` dosyasında tutulur.

## Başvuru ve inceleme

1. Vize Başvuru Masasını yaratıcı sekmeden alın veya pasaport kontrol masası,
   kâğıt ve mürekkep kesesiyle üretin.
2. Kendi sunucu kayıtlı pasaportunuzu ana elinizde tutarak masaya sağ tıklayın.
3. Hedef ülkenin ISO kodunu, türü, seyahat amacını ve kalış süresini girin.
   Başvuruyu gönderin. Pasaport başına en fazla 8 bekleyen başvuru kabul edilir.
4. Yönetici `/visa officer grant Alex` ile **ayrı** vize memuru rolünü verir.
5. Memur `/visa applications` ile bekleyen dosyaları açar; Önceki/Sonraki
   düğmeleriyle ilerler. Vize türünü, başlangıca kalan UTC gün sayısını,
   geçerlilik gününü, kalış gününü ve giriş sayısını belirleyip onaylar veya
   isteğe bağlı gerekçeyle reddeder. `entries=-1` sınırsız giriştir; sıfır geçersizdir.
6. Oyuncu pasaportunu tekrar açınca Vizeler sayfasında etiket görünür.
   Etiketin üzerine gelince tam kimlikler, seyahat amacı ve yetkili görünür.

Vize türleri: `tourist`, `business`, `student`, `work`, `transit`,
`diplomatic`, `official`, `special`. Başvuru kararı vize durumundan ayrıdır.
Vize onayı otomatik giriş onayı değildir. İptal edilen vizeler silinmez.
Bir pasaportta 128 vize saklanabilir; sınıra ulaşınca yeni vize reddedilir.

## Komutlar

```text
/visa help
/visa officer grant <oyuncu>
/visa officer revoke <isim-veya-UUID>
/visa officer list
/visa applications
/visa issue <passport_id> <country> <type> <valid_days> <entries> <stay_days>
/visa revoke <visa_id>
/visa inspect <visa_id>
/visa check <passport_id> <country>
/visa policy set <passport_country> <destination_country> <policy>
/visa policy list <country>
```

Rol ve politika yönetimi OP 2 gerektirir. Vize düzenleme/iptal ve başvuru
inceleme vize memuru gerektirir; OP olmak tek başına bu rolü sağlamaz.
Gümrük memuru vize kontrolü yapabilir. Normal oyuncu yalnızca kendi vizesini
`inspect` ile görebilir. Ülke, tür ve politika argümanlarında öneriler vardır.

## Sınır kapısı

Pasaport kontrol ekranının altındaki iki harfli hedef alanına ülke kodunu
yazıp **Ülkeyi kontrol et** düğmesine basın. Sonuç altta görünür.
Ardından eski GİRİŞ/ÇIKIŞ ve ONAYLA/REDDET düğmelerini kullanın.
Politika tanımlanmış sunucularda hedef seçilmeden giriş onaylanamaz.
Hiç politika tanımlanmamış eski dünyalarda mevcut damgalama davranışı sürer.

Politikalar:

- `free`: vizesiz; vize sayacı değişmez.
- `required`: geçerli ve giriş hakkı kalan hedef ülke vizesi gerekir.
- `prohibited`: giriş onayı reddedilir.
- `special`: geçerli `special` vizesi gerekir.

Örnek sunucu kuralı: `/visa policy set TR DE required`.
Tanımlanmamış çiftler ve varsayılan olarak aynı ülke vatandaşları vizesizdir.
Bunlar gerçek ülkelerin güncel vize politikaları değildir.

Giriş onayı sunucuda yeniden doğrulanır; başarılı işlemden sonra sayaç artar.
Oturum anahtarı yenilenir; aynı paketin tekrarı ikinci kez giriş tüketmez.
Ret vizeyi iptal etmez. Çıkış vize giriş hakkı tüketmez.
Kalış bitişi UTC olarak kaydedilir; bu sürüm otomatik sınır alanı, ışınlama,
ülke toprakları veya kalış aşımına otomatik ceza sistemi içermez.

## Dosyadan kurallar

Dünya açılınca `serverconfig/historicalpassports-visa-rules.json` oluşturulur.
Sunucu kapalıyken düzenleyin; yeniden açılınca okunur.

```json
{
  "types": {
    "tourist": {"max_validity_days": 90, "max_stay_days": 30, "max_entries": 5}
  },
  "policy_overrides": {"TR:DE": "required", "FR:DE": "free"}
}
```

Dosyadaki çiftler kayıtlı politikaların üzerine yazılır. Dosyada yer almayan
komutla atanmış çiftler korunur; kaldırmak için `free` atayın. Geçersiz dosya
sunucuyu açık hata ile durdurur; sessizce vizesiz moda dönmez. `max_entries`
sonlu giriş sayısını sınırlar; `-1` sınırsız seçeneği ayrıca kullanılabilir.

## Görseller ve doğrulama sınırları

Wikimedia Commons [vize kategorisi](https://commons.wikimedia.org/wiki/Category:Visas)
incelendi. Yeni etiket arka planı özgün güvenlik çizgileri/filigran dokusudur;
gerçek bir belgenin kişisel bilgileri kopyalanmadı. Ülke görselleri mevcut,
atıfları kayıtlı kapaklardan gelir. Damgalar mevcut özgün tasarımlar ve
kullanıcının sağladığı damga sesiyle çalışır. Yeni etiket AGPL-3.0 kapsamındadır.

İlk bütünleşik doğrulamada 12/12 Forge GameTest geçti: eski 8 test ve vize
durumları/kayıt, politika/iptal, giriş tüketimi/tekrar ve başvuru yetki/tekrar
testleri. Gerçek istemcide eski dünya açıldı, başvuru gönderildi, memur
onayladı ve vize etiketi görüntülendi. 854×512 ve 1920×1032 pencere
görünümleri incelendi. Son derleme kanıtı `docs/test-evidence/visa-final-tests.log`.

İki ayrı gerçek istemciyle uzak sunucu kabul testi henüz yapılmadı.
Görsel kontrol Türkçe istemcide yapıldı; İngilizce anahtarlar kaynak kontrolüyle
doğrulandı. Başvuru sahibinin geçmiş başvurularını listeleyen ayrı ekran,
ülke başına konsolosluk yetki kısıtları ve ülkeye özel vize etiket taramaları
bu sürümde bulunmaz. Tarihî ülke paketleri önceki sürümdeki gibi kapsam dışıdır.
