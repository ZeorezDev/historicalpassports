# Ülke ve tarihî pasaport genişletmeleri

Ülkeler `src/main/resources/data/historicalpassports/countries/index.json` içinde
listelenen JSON dosyalarından, Forge kayıt aşamasından önce okunur. Veri paketinin
`/reload` işlemi yeni eşya kaydedemez; yeni ülkeler iki tarafta aynı mod derlemesini
veya yeni eşya/model/dil dosyalarını içeren bir yeniden derlemeyi gerektirir.

Örnek yeni dosya (`historical.json`):

```json
[
  {
    "code": "SU",
    "name_en": "Soviet Union",
    "name_tr": "Sovyetler Birliği",
    "official_name_en": "Union of Soviet Socialist Republics",
    "passport_name": "Soviet Passport",
    "color": "#702c33",
    "visual_details": "Historical ordinary passport; supply a separately reviewed cover",
    "validity_days": 3650,
    "historical": true
  }
]
```

Dosyayı indekse ekleyin; `passport_su.json` eşya modeli, `passport_su.png` yerel
kapak dokusu ve her iki dilde ülke/eşya adını ekleyin. `SU` gibi tarihî kodların
güncel ISO kodu olmadığını kendi içerik raporunuzda belirtin. Sunucuda
`allowHistoricalPassports=true` ayarlanmadan tarihî belgeler düzenlenemez.

Temel paket yalnızca 195 güncel ülkeyi içerir. SU/YU kaynakları henüz pakete
eklenmemiştir; örnek tanım hazır tarihî içerik olarak sunulmaz.

`validity_days: 0` sunucu varsayılanını kullanır. Pozitif değer ülkeye özel
geçerlilik süresidir. Süreler gerçek UTC takvim günleriyle hesaplanır; oyun
günüyle değil. Önceden düzenlenen belgenin son tarihi sonradan değiştirilmez.
