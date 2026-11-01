# 🛂 HISTORICAL PASSPORTS
### Your Papers. Your Identity. Your Borders.

**Welcome to Historical Passports!** Transform your Minecraft world into an immersive international passport and border control experience!

Issue passports, apply for visas, inspect documents, stamp approvals or denials, and control who crosses your borders!

## 🌍 FEATURES

- 🌐 **Worldwide Passports** — Country-specific passports with realistic covers.
- 📖 **Interactive Passport GUI** — Explore detailed passports with personal information, visas, and stamps.
- 🛂 **Border Control** — Inspect documents and approve or deny entry.
- 🛃 **Customs Officers** — Assign authorized players to manage border checkpoints.
- 📋 **Visa System** — Apply for, issue, approve, and revoke visas.
- 🏛️ **Inspection Desks** — Manage immigration and customs procedures.
- 💾 **Persistent Records** — Passport information, visas, and inspection history are saved.
- 🌐 **Multiplayer Ready** — Perfect for geopolitical, military, nations, and roleplay servers.
- 🇬🇧 🇹🇷 **Languages** — English & Turkish.

## ⚙️ HOW TO PLAY

**1. Issue a Passport**

`/passport issue <player> <country>`

Example: `/passport issue Steve TR`

**2. Assign Customs Officers**

`/passport officer grant <player>`

`/passport officer revoke <player>`

**3. Assign Visa Officers**

`/visa officer grant <player>`

`/visa officer revoke <player>`

**4. Issue a Visa**

`/visa issue <passport_id> <country> <type> <valid_days> <entries> <stay_days>`

**5. Inspect Documents**

Right-click a passport to open its interactive GUI. Authorized officers can inspect documents, verify visas, and approve or deny border entry.

**Additional Commands:**

`/passport help`

`/visa help`

## 📦 REQUIREMENTS

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17

---

### 🛂 EVERY PASSPORT TELLS A STORY. EVERY BORDER HAS A DECISION.

**Historical Passports — Borders are more than just lines on a map.**

## Development and contributions

Source release: **1.1.0**, Minecraft **1.20.1**, Forge **47.4.28**, Java **17**.

- [Contributing](CONTRIBUTING.md)
- [Türkçe kurulum ve geliştirme rehberi](README_TR.md)
- [Visa system guide](VISA_GUIDE.md)
- [Test report and limitations](TEST_REPORT.md)
- [Asset credits](ASSET_CREDITS.md) and [sound credits](SOUND_CREDITS.md)

Build with `./gradlew build` (`gradlew.bat build` on Windows). Run server
tests with `./gradlew runGameTestServer`. Output: `build/libs/`.

195 countries are registered; 165 covers use reviewed Wikimedia sources and
30 are original provisional artwork. Twelve server tests passed locally;
a two-client remote multiplayer acceptance test has not yet been performed.

## License

Project code is distributed under AGPL-3.0 (see LICENSE). Third-party assets
retain their individual licenses; see the credit files. The supplied stamp
recording has no specified license and is not covered by the code license.
