# Contributing / Katkıda bulunma

Contributions and bug reports in English or Turkish are welcome.

## Development

Use Java 17, Minecraft 1.20.1 and Forge 47.4.28. Fork this repository,
clone your fork and create a branch for your change.

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

On Windows use `gradlew.bat`. Set `JAVA_HOME` to your Java 17 installation.
The first build downloads the Forge development dependencies.
The mod JAR is written to `build/libs/`.

For resource checks, install `scripts/requirements.txt` with Python and run
`python scripts/validate_resources.py`. Resource generators can overwrite assets;
read their inputs and outputs before using them. Run `generate_visa_resources.py`
after regenerating the base language files to restore visa translations.

## Pull requests

- Describe the problem, the change and the checks you actually ran.
- Preserve existing item IDs, save compatibility and server-side validation.
- Keep client-only code out of dedicated-server initialization.
- Update both English and Turkish translation files.
- Include focused server tests for permissions, persistence or network changes.
- Provide screenshots for GUI changes and note window size / GUI scale.
- Do not commit worlds, credentials, build caches or raw passport data pages.
- Include source URLs and licenses for contributed assets. Do not assume
  code licensing applies to Wikimedia media or the supplied stamp recording.

See `ASSET_CREDITS.md`, `SOUND_CREDITS.md`, `TEST_REPORT.md` and `VISA_GUIDE.md`.
Project code and original project artwork use AGPL-3.0; third-party assets retain their
individual terms. The stamp recording's license has not been specified.

## Hata bildirimi

Minecraft/Forge/mod sürümünü, tekrar üretme adımlarını, beklenen ve gerçekleşen
davranışı belirtin. Günlük paylaşmadan önce kişisel bilgileri ve sunucu
adreslerini temizleyin. Güvenlik açığında herkese açık çalışır saldırı örneği
yerine önce kısa bir sorun açıklaması paylaşın.

The previous MIT notice is retained in `LICENSE-MIT-legacy.txt` for the earlier
source distribution. The current repository uses the existing AGPL-3.0 license.
