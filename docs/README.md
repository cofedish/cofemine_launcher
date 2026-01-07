# CofeMine Launcher

[![Release](https://img.shields.io/github/v/release/cofedish/cofemine_launcher?style=flat)](https://github.com/cofedish/cofemine_launcher/releases)
[![Downloads](https://img.shields.io/github/downloads/cofedish/cofemine_launcher/total?label=Downloads&style=flat)](https://github.com/cofedish/cofemine_launcher/releases)
![Stars](https://img.shields.io/github/stars/cofedish/cofemine_launcher?style=flat)

## О проекте

CofeMine Launcher — кастомный UI и набор фичей для сервера CofeMine.
Сфокусирован на удобном запуске и управлении сборкой CofeMine без потери базового функционала.

Основные возможности:
- запуск Minecraft с поддержкой модлоадеров (Forge, NeoForge, Fabric, Quilt, LiteLoader, OptiFine)
- управление версиями, профилями и модпаками
- кофейная светлая тема и панель CofeMine (статус сервера, сайт, установка/обновление модпака)

## Скачивание

- Релизы: https://github.com/cofedish/cofemine_launcher/releases
- Сайт: https://cofemine.ru

## Лицензия

Проект распространяется под GPLv3 с дополнительными условиями исходного проекта (см. `../LICENSE`).

Важные требования GPLv3 Section 7 (унаследовано от исходного проекта):
1. При распространении модифицированной версии нужно разумно изменить имя или номер версии, чтобы отличать её от оригинала.
2. Нельзя удалять отображаемые в приложении копирайты.

Имя и версия изменяются в исходниках `Metadata.java`.

## Сборка

Требования: Java 17+.

Команды:
```bash
./gradlew build
./gradlew run
```

## JVM параметры (для отладки)

Префикс параметров сохранён как `hmcl.*` для совместимости с базовой кодовой базой.

| Параметр | Описание |
| --- | --- |
| `-Dhmcl.home=<path>` | Переопределить директорию данных лаунчера |
| `-Dhmcl.self_integrity_check.disable=true` | Отключить проверку целостности при обновлении |
| `-Dhmcl.bmclapi.override=<url>` | Задать альтернативный API BMCLAPI |
| `-Dhmcl.font.override=<font family>` | Переопределить шрифт |
| `-Dhmcl.version.override=<version>` | Переопределить версию |
| `-Dhmcl.update_source.override=<url>` | Переопределить источник обновлений |
| `-Dhmcl.authlibinjector.location=<path>` | Указать локальный authlib-injector |
| `-Dhmcl.openjfx.repo=<maven repository url>` | Доп. репозиторий Maven для OpenJFX |
| `-Dhmcl.native.encoding=<encoding>` | Переопределить нативную кодировку |
| `-Dhmcl.microsoft.auth.id=<App ID>` | Microsoft OAuth App ID |
| `-Dhmcl.microsoft.auth.secret=<App Secret>` | Microsoft OAuth App Secret |
| `-Dhmcl.curseforge.apikey=<Api Key>` | CurseForge API Key |
