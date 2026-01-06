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
English ([Standard](README.md), [uʍoᗡ ǝpᴉsd∩](README_en_Qabs.md)) | 中文 ([简体](README_zh.md), [繁體](README_zh_Hant.md), [文言](README_lzh.md)) | [日本語](README_ja.md) | [español](README_es.md) | [русский](README_ru.md) | **українська**
<!-- #END LANGUAGE_SWITCHER -->

## Вступ

HMCL — це відкритий, кросплатформний лаунчер для Minecraft, який підтримує керування модами, налаштування гри, встановлення модлоадерів (Forge, NeoForge, Cleanroom, Fabric, Quilt, LiteLoader та OptiFine), створення модпаків, налаштування інтерфейсу та багато іншого.

HMCL має чудові кросплатформні можливості. Він працює не лише на різних операційних системах, таких як Windows, Linux, macOS і FreeBSD, а й підтримує різні архітектури процесорів, такі як x86, ARM, RISC-V, MIPS і LoongArch. Ви можете легко насолоджуватися Minecraft на різних платформах за допомогою HMCL.

Щодо підтримуваних систем і архітектур процесорів дивіться [цю таблицю](PLATFORM.md).

## Завантаження

Завантажте останню версію з [офіційного сайту](https://cofemine.ru).

Також ви можете знайти останню версію HMCL у [релізах GitHub](https://github.com/cofedish/cofemine_launcher/releases).

Хоча це не обовʼязково, рекомендується завантажувати релізи лише з офіційних сайтів, зазначених вище.

## Ліцензія

Дивіться [README.md](README.md#license).

## Внесок

Якщо ви хочете надіслати pull request, ознайомтеся з наступними вимогами:

* IDE: IntelliJ IDEA
* Компілятор: Java 17+

### Компіляція

Дивіться сторінку [Посібник зі збірки](./Building.md).

## JVM Options (for debugging)

| Parameter                                    | Description                                                                                   |
|----------------------------------------------|-----------------------------------------------------------------------------------------------|
| `-Dhmcl.home=<path>`                         | Override HMCL directory                                                                       |
| `-Dhmcl.self_integrity_check.disable=true`   | Bypass the self integrity check when checking for updates                                     |
| `-Dhmcl.bmclapi.override=<url>`              | Override API Root of BMCLAPI download provider. Defaults to `https://bmclapi2.bangbang93.com` |
| `-Dhmcl.font.override=<font family>`         | Override font family                                                                          |
| `-Dhmcl.version.override=<version>`          | Override the version number                                                                   |
| `-Dhmcl.update_source.override=<url>`        | Override the update source for HMCL itself                                                    |
| `-Dhmcl.authlibinjector.location=<path>`     | Use the specified authlib-injector (instead of downloading one)                               |
| `-Dhmcl.openjfx.repo=<maven repository url>` | Add custom Maven repository for downloading OpenJFX                                           |
| `-Dhmcl.native.encoding=<encoding>`          | Override the native encoding                                                                  |
| `-Dhmcl.microsoft.auth.id=<App ID>`          | Override Microsoft OAuth App ID                                                               |
| `-Dhmcl.microsoft.auth.secret=<App Secret>`  | Override Microsoft OAuth App Secret                                                           |
| `-Dhmcl.curseforge.apikey=<Api Key>`         | Override CurseForge API Key                                                                   |
