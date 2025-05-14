# LuckyBlocks — плагин и конфигурация

## Описание

LuckyBlocks — расширяемый плагин для Minecraft, реализующий уникальные лакиблоки с гибкой системой событий, уровней и типов. Все детали управляются YAML-конфигами, что максимально удобно для кастомизации и интеграции на сервер.

---

## Конфиг-файлы

### `luckyblocks.yml`

Основной файл для определения типов лакиблоков, уровней, набора событий и поведения при активации.

**Пример структуры:**
```yaml
types:
  positive:
    item:
      material: SPONGE
      custom-model-data: 1001
    levels:
      1:
        display-name: "&aПоложительный ЛакиБлок &eI уровня"
        events:
          EXPLOSION:
            enabled: false
            weight: 100
            power: 4.0
          DROP_ITEM:
            - enabled: true
              weight: 500
              items:
                - item: GOLDEN_APPLE
                  custom-model-data: 2
                  name: "Золотое яблоко"
                  lore: "Лучший ужин!"
                  amount: 2
          # остальные события (PLACE_BLOCK, PLACE_CHEST ...)
```
**Поля:**
- `types` — ключи (positive, negative и др.) — название типа лакиблока  
- `item.material` — отображаемый материал (SPONGE, DIAMOND блок и др.)
- `item.custom-model-data` — для ресурспаков (необязательно)
- `levels` — уровни; для каждого уровня свой display-name и набор events  
- `events` — поддерживаемые события (EXPLOSION, DROP_ITEM, PLACE_BLOCK, PLACE_CHEST)
  - `enabled` — включено/нет
  - `weight` — “шанс” выпадения, влияет на выбор рандомного события
  - параметры события (например, power, items, blocks и т.д.)

---

### `chests.yml`

Таблицы лута для событий типа PLACE_CHEST. Встраивается в конфиг через `item-table` (см. examples).

**Пример:**
```yaml
positive:
  main_loot:
    slots: 27
    loot:
      - items:
          - item: DIAMOND
            amount: 5
            name: "§bАлмаз"
        weight: 50
negative:
  bad_loot:
    slots: 27
    loot:
      - items:
          - item: DIRT
            amount: 5
            name: "&8Плохая грязь"
        weight: 10
```
**Поля:**
- `[type]` — positive/negative и т.д., должны совпадать с luckyblocks.yml
- `[item-table]` — название таблицы (main_loot, bad_loot и др.)
- `slots` — размер генерируемого сундука
- `loot` — список вариантов "набора лута" (у каждого weight и список items)

---

## Атрибуты предметов (items)

В любом месте, где фигурирует объект `items`, поддерживаются:
- item: (название предмета, материал Minecraft)
- amount: количество
- name: название (можно с цветами — &a, §b)
- lore: описание (одной строкой или через | многострочно)
- custom-model-data: id для ресурспака
- attributes: список атрибутов (урон, здоровье и др.), например:
```yaml
attributes:
  - attribute: GENERIC_ATTACK_DAMAGE
    amount: 4
    operation: ADD_NUMBER
```

---

## Флаг отладки

Весь отладочный вывод теперь контролируется единственным статическим глобальным флагом:
```java
LuckyBlockPlugin.debug
```
- По умолчанию (false) никакая отладочная информация не выводится вообще.
- Если выставить debug=true (например, руками или через отладочный механизм), все подробные логи (процесс генерации сундуков, диагностика событий, дампы лута, действия с лакиблоками и др.) будут записываться только в серверный лог (и никогда не игроку!).
- Критические ошибки и предупреждения (`warning`, `severe`) всегда выводятся вне зависимости от debug.

---

## Загрузка и перезагрузка конфигов

Плагин автоматически выгружает дефолтные luckyblocks.yml и chests.yml при первом запуске.  
Для применения новых настроек используйте команду перезагрузки (если реализовано).

---

## Пример быстрого старта

1. Скопируйте исходные luckyblocks.yml и chests.yml в папку data вашего сервера.
2. Можно свободно добавлять свои типы, уровни, кастомные события, не меняя структуру ядра.
3. Для включения отладки выставьте `LuckyBlockPlugin.debug = true` (например, через консоль/IDE).

---

## Все события и параметры (`events`)

В конфиге для каждого уровня лакиблока указываются поддерживаемые события (events) и их параметры. Вот полный список:

### EXPLOSION
**Описание:** Вызывает взрыв в точке лакиблока (аналог TNT, но с контролем параметров).
**Пример:**
```yaml
EXPLOSION:
  enabled: true
  weight: 100
  power: 4.0
```
**Параметры:**
- `enabled` (bool) — использовать или нет данное событие
- `weight` (int) — “шанс” выпадения события
- `power` (float) — сила взрыва (по аналогии с TNT = 4.0)
- (дополнительно могут быть: зажечь/не зажечь игроков, custom эффекты)

---

### DROP_ITEM
**Описание:** Выдаёт игроку один или несколько предметов.
**Пример:**
```yaml
DROP_ITEM:
  enabled: true
  weight: 200
  item: DIAMOND
  amount: 3
```
или, если много вариантов:
```yaml
DROP_ITEM:
  - enabled: true
    weight: 400
    items:
      - item: DIAMOND
        amount: 2
      - item: GOLDEN_APPLE
        amount: 1
```
**Параметры:**  
- `enabled` (bool)
- `weight` (int)
- `item` (или `items`) — название предмета или список предметов (см. раздел “Атрибуты предметов”)
- `amount` (int) — количество
- Поддерживает все параметры из “атрибутов предметов” (custom-model-data, name, lore, attributes и др.)

---

### PLACE_BLOCK
**Описание:** Устанавливает блоки (или несколько блоков) возле лакиблока (например, бриллиантовый блок сверху).
**Пример:**
```yaml
PLACE_BLOCK:
  enabled: true
  weight: 50
  blocks:
    - material: DIAMOND_BLOCK
      relative-coords: { x: 0, y: 1, z: 0 }
    - material: EMERALD_BLOCK
      relative-coords: { x: 2, y: 0, z: 0 }
```
**Параметры:**
- `enabled` (bool)
- `weight` (int)
- `material`/`blocks` — название блока или блоков
- `relative-coords` — смещение относительно лакиблока (`x`, `y`, `z`)
- `custom-model-data` — для ресурспака (не обязательно)

---

### PLACE_CHEST
**Описание:** Создаёт сундук с лутом по указанной таблице из `chests.yml`.
**Пример:**
```yaml
PLACE_CHEST:
  enabled: true
  weight: 40
  item-table: main_loot
  relative-coords: { x: 1, y: 0, z: 0 }
```
**Параметры:**
- `enabled` (bool)
- `weight` (int)
- `item-table` (строка) — таблица из chests.yml
- `relative-coords` (объект) — смещение
- [опционально] дополнительные параметры лута (если расширено)

---

### COMMAND_EXEC
**Описание:** Выполняет указанные команды от имени сервера.
**Пример:**
```yaml
COMMAND_EXEC:
  enabled: true
  weight: 30
  command: "give %player% minecraft:diamond 3"
```
**Параметры:**
- `enabled` (bool)
- `weight` (int)
- `command` (или `commands`) — строка или список команд (допускаются плейсхолдеры: `%player%` и др.)

---

### PLAYER_COMMAND
**Описание:** Выполняет команду от имени игрока.
**Пример:**
```yaml
PLAYER_COMMAND:
  enabled: true
  weight: 25
  command: "say Я поймал лакиблок!"
```
**Параметры:**
- `enabled` (bool)
- `weight` (int)
- `command` / `commands` (строка или список)

---

### HEAL
**Описание:** Лечит игрока и/или даёт доп. эффекты.
**Пример:**
```yaml
HEAL:
  enabled: true
  weight: 10
  amount: 8
  effects:
    - REGENERATION
    - ABSORPTION
```
**Параметры:**
- `enabled` (bool)
- `weight` (int)
- `amount` (int) — сколько единиц здоровья восстановить
- `effects` — список эффектов зелья (по стандарту Minecraft)

---

### DEFAULT
**Описание:** Подстраховочное действие, если не получилось подобрать событие. Обычно ничего не делает или выдаёт небольшой эффект.

---

### Собственные события/кастомизация

Можно создавать собственные события, подключив их в коде плагина (через eventRegistry и самостоятельный Java-класс, реализующий ICustomEvent).

---


## Технологии и поддержка

- Bukkit/Spigot API (Java)
- YAML-конфиги
- Гибкая система ивентов для расширений

---

**Автор:**  
Ayrongames и Aeris совместно для Ayronix
