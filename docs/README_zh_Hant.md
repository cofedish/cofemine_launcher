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
English ([Standard](README.md), [uʍoᗡ ǝpᴉsd∩](README_en_Qabs.md)) | **中文** ([简体](README_zh.md), **繁體**, [文言](README_lzh.md)) | [日本語](README_ja.md) | [español](README_es.md) | [русский](README_ru.md) | [українська](README_uk.md)
<!-- #END LANGUAGE_SWITCHER -->

## 簡介

CofeMine Launcher 是一款開源、跨平臺的 Minecraft 啟動器，支援模組管理、遊戲客製化、遊戲自動安裝 (Forge、NeoForge、Cleanroom、Fabric、Quilt、LiteLoader 和 OptiFine)、模組包建立、介面客製化等功能。

CofeMine Launcher 有著強大的跨平臺能力。它不僅支援 Windows、Linux、macOS、FreeBSD 等常見的作業系統，同時也支援 x86、ARM、RISC-V、MIPS、LoongArch 等不同的 CPU 架構。你可以使用 CofeMine Launcher 在不同平臺上輕鬆地遊玩 Minecraft。

如果你想要了解 CofeMine Launcher 對不同平臺的支援程度，請參見 [此表格](PLATFORM_zh_Hant.md)。

## 下載

請從 [CofeMine Launcher 官網](https://cofemine.ru) 下載最新版本的 CofeMine Launcher。

你也可以在 [GitHub Releases](https://github.com/cofedish/cofemine_launcher/releases) 中下載最新版本的 CofeMine Launcher。

雖然並不強制，但仍建議透過 CofeMine Launcher 官網下載啟動器。

## 開源協議

該程式在 [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) 開源協議下發布，同時附有以下附加條款。

### 附加條款 (依據 GPLv3 開源協議第七條)

1. 當你分發該程式的修改版本時，你必須以一種合理的方式修改該程式的名稱或版本號，以示其與原始版本不同。(依據 [GPLv3, 7(c)](https://github.com/cofedish/cofemine_launcher/blob/main/LICENSE))

   該程式的名稱及版本號可在 [此處](https://github.com/cofedish/cofemine_launcher/blob/main/HMCL/src/main/java/org/jackhuang/hmcl/Metadata.java) 修改。

2. 你不得移除該程式所顯示的版權宣告。(依據 [GPLv3, 7(b)](https://github.com/cofedish/cofemine_launcher/blob/main/LICENSE))

## 貢獻

如果你想提交一個 Pull Request，必須遵守如下要求：

* IDE：IntelliJ IDEA
* 編譯器：Java 17+

### 編譯

參閱[構建指南](./Building_zh.md)頁面。

## JVM 選項 (用於除錯)

| 參數                                         | 簡介                                                                 |
| -------------------------------------------- | -------------------------------------------------------------------- |
| `-Dhmcl.home=<path>`                         | 覆蓋 CofeMine Launcher 使用者目錄                                                 |
| `-Dhmcl.self_integrity_check.disable=true`   | 檢查更新時不檢查本體完整性                                           |
| `-Dhmcl.bmclapi.override=<url>`              | 覆蓋 BMCLAPI 的 API Root，預設值為 `https://bmclapi2.bangbang93.com` |
| `-Dhmcl.font.override=<font family>`         | 覆蓋字族                                                             |
| `-Dhmcl.version.override=<version>`          | 覆蓋版本號                                                           |
| `-Dhmcl.update_source.override=<url>`        | 覆蓋 CofeMine Launcher 更新來源                                                   |
| `-Dhmcl.authlibinjector.location=<path>`     | 使用指定的 authlib-injector (而非下載一個)                           |
| `-Dhmcl.openjfx.repo=<maven repository url>` | 添加用於下載 OpenJFX 的自訂 Maven 倉庫                               |
| `-Dhmcl.native.encoding=<encoding>`          | 覆蓋原生編碼                                                         |
| `-Dhmcl.microsoft.auth.id=<App ID>`          | 覆蓋 Microsoft OAuth App ID                                          |
| `-Dhmcl.microsoft.auth.secret=<App Secret>`  | 覆蓋 Microsoft OAuth App 金鑰                                        |
| `-Dhmcl.curseforge.apikey=<Api Key>`         | 覆蓋 CurseForge API 金鑰                                        |
