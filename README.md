# Отчёт по HW4: IR и базовые оптимизации

## Файлы hw4

`CFG`, `GraphColoring`, `IR`, `IRGenerator`, `Liveness`, `Optimizer`, `Main` (с тестами).

## 1. Формат IR

### 1.1 Инструкции

Трёхадресный код: `dst = a op b`, `dst = const v`, `goto L`, `if cond goto L`, `call`, `return`.

- Операнды — имена переменных (`x`, `r`, `a`) и временных (`t0`, `t1`, …).
- Метки — `L0`, `L1`, …
- Соглашение: `if cond goto L` означает «прыгнуть, если условие ложно»; иначе — fallthrough.

### 1.2 Набор операций (IR.Op)

| Группа | Операции |
| --- | --- |
| Данные | `CONST`, `COPY` |
| Арифметика | `ADD`, `SUB`, `MUL`, `DIV`, `NEG` |
| Логика | `NOT`, `AND`, `OR`, `IMPLIES` |
| Сравнения | `EQ`, `NE`, `LT`, `LE`, `GT`, `GE` |
| Управление | `LABEL`, `JUMP`, `BRANCH` |
| Функции | `CALL`, `RET`, `PARAM` |
| Массивы | `ARRAY_GET`, `ARRAY_SET`, `LENGTH` |
| Контракты | `ASSERT`, `ASSUME` |

### 1.3 Структуры

- `IR.Instr` — одна инструкция: `op`, `dst`, `a`, `b`, `iconst`, `bconst`, `label`, `callName`, `argc` + фабрики + `toString()`.
- `IR.Program` — функция: `name`, `returnVar`, `code: List<Instr>`, генераторы `newTmp()` / `newLabel()`.

## 2. Генерация IR (IRGenerator)

### 2.1 Покрытие конструкций

| AST | IR |
| --- | --- |
| Литералы | `CONST` во временную |
| Переменная | имя как есть (без копии) |
| `-x`, `not x` | `NEG`, `NOT` |
| `+ - * / == != < <= > >= and or ->` | `ADD` … `IMPLIES` |
| `a[i]` | `ARRAY_GET` |
| `length(a)` | `LENGTH` |
| `f(args)` | `PARAM` для каждого аргумента, затем `CALL` |
| `x = e` / `a[i] = v` | `COPY` / `ARRAY_SET` |
| `if` / `while` | `BRANCH` + `LABEL` + `JUMP` |
| `assert` / `assume` | `ASSERT` / `ASSUME` |

### 2.2 Ключевые решения

- Литералы материализуются в `CONST` — все операнды IR становятся именами (единый формат).
- Переменные не копируются — `x` в `y = x + 1` используется напрямую (экономия временных).
- Аргументы вызова вычисляются до `PARAM` — сохраняется порядок вычислений.
- Один `RET` в конце функции — язык не имеет раннего `return`; тело пишет в `returns`-переменную.
- Контракты не генерируются — `requires`/`ensures`/`invariant`/формулы `=>` — спецификации, а не исполняемый код.

## 3. CFG (CFG)

### 3.1 Структура

`Block`: `label`, `instrs`, `succ`, `pred`. `CFG`: `blocks`, `entry`.

### 3.2 Построение

Фаза 1 — разрезание по `LABEL` + fallthrough при смене блока.

Фаза 2 — связывание переходов:

| Последняя инструкция | Рёбра |
| --- | --- |
| `JUMP L` | `b → block(L)` |
| `BRANCH cond, L` | `b → block(L)` и `b → следующий` (fallthrough) |
| `RET` | нет (выход) |
| обычная | `b → следующий` |

## 4. Оптимизации (Optimizer)

### 4.1 Флаги и порядок

```text
constantPropagation, algebraicSimplify, deadCodeElimination, cse
```

Порядок: `propagateConstants` (глобально) → для каждого блока `localCSE` → `simplifyBlock` → `localDCE`.

Счётчики: `constFolded`, `simplified`, `eliminated`, `cseReplaced`.

### 4.2 Распространение констант

Таблицы `ints` / `bools`; `CONST`/`COPY` переносят известность; арифметика над известными операндами заменяется на `CONST` (`DIV` защищён от нуля); `default` сбрасывает `dst`.

### 4.3 Алгебраические упрощения

| Шаблон | Результат | Шаблон | Результат |
| --- | --- | --- | --- |
| `x + 0`, `0 + x`, `x − 0` | `x` | `x and true` | `x` |
| `x * 1`, `1 * x` | `x` | `x and false` | `false` |
| `x * 0`, `0 * x` | `0` | `x or false` | `x` |
| `x − x` | `0` | `x or true` | `true` |

Реализовано через `collectIntConsts`/`collectBoolConsts` (O(n) на блок).

### 4.4 CSE в пределах блока

`Map<"op|a|b", dst>`; повторное вычисление → `COPY`. Инвалидация при `CALL`/`ARRAY_GET`/`ARRAY_SET`.

### 4.5 Локальный DCE

Итеративно удаляет `dst` без использования и без побочных эффектов (`CALL`, `RET`, `ARRAY_SET`, `ASSERT`, `ASSUME`, `JUMP`, `BRANCH`, `LABEL`, `PARAM` — неудаляемые). Защита `returnVar`.

## 6. Живость и раскраска

### 6.1 Liveness

| Поле | Смысл |
| --- | --- |
| `use[b]` | upward-exposed uses |
| `def[b]` | переопределения (кроме `isReadOnlyTarget`) |
| `liveIn[b]` | `use[b] ∪ (liveOut[b] \ def[b])` |
| `liveOut[b]` | `⋃ liveIn[succ]` |

Итерация до неподвижной точки (backward, обратный порядок блоков).

### 6.2 Граф конфликтов

Обратный проход по блоку от `liveOut[b]`: `dst` конфликтует со всеми живыми; операнды оживляются. `graphToDot` печатает каждое ребро один раз.

### 6.3 Жадная раскраска (Welsh–Powell)

Сортировка вершин по убыванию степени; для каждой — минимальный свободный цвет среди соседей.

Результат: `color` (имя → регистр), `colors` (метрика), `report()` — детерминированная таблица «имя → rN».
