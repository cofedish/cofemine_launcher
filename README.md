# CofeMine Launcher

[![Release](https://img.shields.io/github/v/release/cofedish/cofemine_launcher?style=flat)](https://github.com/cofedish/cofemine_launcher/releases)
[![Downloads](https://img.shields.io/github/downloads/cofedish/cofemine_launcher/total?label=Downloads&style=flat)](https://github.com/cofedish/cofemine_launcher/releases)
![Stars](https://img.shields.io/github/stars/cofedish/cofemine_launcher?style=flat)

## О проекте

CofeMine Launcher — кастомный UI и набор фичей для сервера CofeMine, основанный на HMCL.
Все ключевые возможности HMCL сохранены, добавлены брендирование и удобные функции для CofeMine.

## Ссылки

- Сайт: https://cofemine.ru
- Релизы: https://github.com/cofedish/cofemine_launcher/releases
- Telegram: https://t.me/+rfvGU6sDSvEwZDgy
- Discord: https://discord.gg/zyKM2XAnXW
- Issues: https://github.com/cofedish/cofemine_launcher/issues

## Лицензия

Проект распространяется под GPLv3 с дополнительными условиями HMCL (см. `LICENSE`).
Требования GPLv3 Section 7 (унаследовано от HMCL):
1. При распространении модифицированной версии нужно разумно изменить имя или номер версии, чтобы отличать её от оригинала.
2. Нельзя удалять отображаемые в приложении копирайты.

Имя и версия изменяются в `HMCL/src/main/java/org/jackhuang/hmcl/Metadata.java`.

## Сборка

Требования: Java 17+.

Команды:
```bash
./gradlew build
./gradlew :HMCL:run
```
