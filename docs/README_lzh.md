# Hello Minecraft! Launcher

<!-- #BEGIN COPY -->
<!-- #PROPERTY NAME=BADGES -->
[![CNB](https://img.shields.io/badge/cnb-mirror-ff6200?logo=cloudnativebuild)](https://github.com/cofedish/cofemine_launcher)
[![Downloads](https://img.shields.io/github/downloads/cofedish/cofemine_launcher/total?label=Downloads&style=flat)](https://github.com/cofedish/cofemine_launcher/releases)
![Stars](https://img.shields.io/github/stars/cofedish/cofemine_launcher?style=flat)
[![Discord](https://img.shields.io/badge/Discord-join-5865F2?label=&logo=discord&logoColor=ffffff)](https://discord.gg/zyKM2XAnXW)
[![Telegram](https://img.shields.io/badge/Telegram-CofeMine-26A5E4?label=&logo=telegram&logoColor=ffffff&logoSize=auto)](https://t.me/+rfvGU6sDSvEwZDgy)
<!-- #END COPY -->

<!-- #BEGIN LANGUAGE_SWITCHER -->
English ([Standard](README.md), [uʍoᗡ ǝpᴉsd∩](README_en_Qabs.md)) | **中文** ([简体](README_zh.md), [繁體](README_zh_Hant.md), **文言**) | [日本語](README_ja.md) | [español](README_es.md) | [русский](README_ru.md) | [українська](README_uk.md)
<!-- #END LANGUAGE_SWITCHER -->

### 概說

HMCL 者，開源之礦藝啟者也。能理改囊，善於遊戲之自定，且自動安裝諸如 Forge、NeoForge、Cleanroom、Fabric、Quilt、LiteLoader、OptiFine 諸改囊，亦可製作改囊集，界面亦可隨意更易。

HMCL 跨域甚廣。無論 Windows、Linux、macOS、FreeBSD 諸常見械綱，抑或 x86、ARM、RISC-V、MIPS、LoongArch 諸大構處理器，皆可運行。
君可憑此，於諸算機間自如遊戲。

若欲詳知 HMCL 於諸算機之支援，請觀[此表](PLATFORM_zh_Hant.md)。

## 下載

請自 [HMCL 官網](https://cofemine.ru) 取其最新版。

亦可於 [GitHub Releases](https://github.com/cofedish/cofemine_launcher/releases) 得其新者。

雖非強制，然猶勸自官網取之。

## 開源之約

詳見 [README_zh_Hant.md](README_zh_Hant.md#開源協議)。

## 貢獻

若欲獻 Pull Request，須遵下列：

* IDE 用 IntelliJ IDEA
* 編譯器用爪哇十七以上

### 編造

請觀[編造指南](./Building_zh.md)。

## 爪哇虛機之通弦（以資勘誤）

| 參數                                           | 解釋                                                      |
|----------------------------------------------|---------------------------------------------------------|
| `-Dhmcl.home=<path>`                         | 易 HMCL 之用戶目錄                                            |
| `-Dhmcl.self_integrity_check.disable=true`   | 檢查更新時不驗本體之全                                             |
| `-Dhmcl.bmclapi.override=<url>`              | 易 BMCLAPI 之 API 根，預設為 `https://bmclapi2.bangbang93.com` |
| `-Dhmcl.font.override=<font family>`         | 易書體                                                     |
| `-Dhmcl.version.override=<version>`          | 易版                                                      |
| `-Dhmcl.update_source.override=<url>`        | 易 HMCL 之更新所                                             |
| `-Dhmcl.authlibinjector.location=<path>`     | 用所指之 authlib-injector，毋需下載                              |
| `-Dhmcl.openjfx.repo=<maven repository url>` | 增 OpenJFX 下載之自定 Maven 庫                                 |
| `-Dhmcl.native.encoding=<encoding>`          | 易本地編碼                                                   |
| `-Dhmcl.microsoft.auth.id=<App ID>`          | 易 Microsoft OAuth 之 App ID                              |
| `-Dhmcl.microsoft.auth.secret=<App Secret>`  | 易 Microsoft OAuth 之金鑰                                   |
| `-Dhmcl.curseforge.apikey=<Api Key>`         | 易 CurseForge 之 API 金鑰                                   |
