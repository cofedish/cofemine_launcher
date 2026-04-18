# Участие в разработке

Спасибо за интерес к CofeMine Launcher! Это кастомный форк HMCL,
ориентированный на сервер CofeMine, поэтому крупные архитектурные
изменения принимаются выборочно — если идея не совпадает с
направлением форка, её лучше сперва обсудить в issue.

## Как сообщить о проблеме

1. Проверьте, нет ли уже открытого issue по той же теме:
   https://github.com/cofedish/cofemine_launcher/issues
2. Если нет — создайте новый. Для ошибок прикладывайте:
   - версию лаунчера (About → Version);
   - версию Java и ОС;
   - шаги воспроизведения;
   - фрагмент лога (`%APPDATA%/.cofemine/logs/` на Windows,
     `~/.cofemine/logs/` на Linux/macOS).

## Pull requests

- Ветка по умолчанию — `main`. Отправляйте PR на неё.
- Старайтесь держать изменения сфокусированными: один PR — одна тема.
- Сохраняйте стиль кода upstream-проекта HMCL — код уровня
  `HMCL/`, `HMCLCore/`, `HMCLBoot/` трогайте только при необходимости.
- Изменения, относящиеся исключительно к CofeMine-специфичным
  настройкам, складывайте в `config/` либо в файлы уровня форка.

## Сборка нативных инсталляторов

Помимо обычного `./gradlew build` (который производит JAR и
exe/sh-обёртки), можно собрать полноценный установщик с встроенной JRE
через `jpackage` (входит в JDK ≥ 14). Кросс-сборка не поддерживается —
каждую цель нужно собирать на соответствующей ОС.

```bash
# Общая команда: собирает инсталлятор(ы) для текущей ОС.
./gradlew :HMCL:packageInstaller

# Или явно под конкретную платформу (запускать на этой платформе):
./gradlew :HMCL:packageInstallerWindows     # -> HMCL/build/installer/*.exe
./gradlew :HMCL:packageInstallerMac         # -> HMCL/build/installer/*.dmg
./gradlew :HMCL:packageInstallerLinuxDeb    # -> HMCL/build/installer/*.deb
./gradlew :HMCL:packageInstallerLinuxRpm    # -> HMCL/build/installer/*.rpm
```

Требования на host-машине:

| ОС      | Нужно                                              |
| ------- | -------------------------------------------------- |
| Windows | WiX Toolset v3 на PATH; ImageMagick (для `.ico`).  |
| macOS   | Xcode Command Line Tools (`sips`, `iconutil`).     |
| Linux   | `fakeroot`, `dpkg` для .deb; `rpmbuild` для .rpm.  |

Иконки `.ico` и `.icns` автоматически не коммитятся — CI генерирует их
из `HMCL/image/hmcl.png`. Для локальной сборки под Windows/macOS
сгенерируйте их заранее или положите вручную в `HMCL/image/`.

Сборки без git-тега вида `vX.Y.Z` получают fallback-версию `1.0.0`,
так как jpackage не принимает snapshot-строки вроде `3.dev-abc1234`.

Подпись исполняемых файлов пока не настроена (требует платных
сертификатов Code Signing / Apple Developer). При установке Windows
покажет SmartScreen, macOS — Gatekeeper; это ожидаемое поведение для
неподписанных билдов.

## Лицензия

Отправляя PR, вы соглашаетесь, что ваш вклад распространяется на тех
же условиях, что и проект — GPLv3 с дополнительными условиями HMCL
(см. `LICENSE`).
