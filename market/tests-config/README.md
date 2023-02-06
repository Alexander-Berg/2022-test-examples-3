## Внешний конфиг для автотестов Маркета


### Скипы тестов

Заскипанные тесты разнесены по двум проектам (Маркет/Покупки). В каждой директории существует отдельный файл для каждой из платформ (desktop/touch).
#### skipped/\<PROJECT\>/desktop.json
Содержит список заскипанных тестов для десктопа

#### skipped/\<PROJECT\>/touch.json
Содержит список заскипанных тестов для тача

#### skipped/hermione2/desktop.json
Содержит список заскипанных тестов для десктопа вторая гермиона

#### skipped/hermione2/touch.json
Содержит список заскипанных тестов для тача вторая гермиона

#### Как это работает
1. Когда на фронте запускаются автотесты, они скачивают себе **мастер** репы со скипами и скипают тесты, которые есть **в мастере** этого конфига.
> Что это значит: Это значит, что если вы не смержите PR в этот репозиторий в мастер, то применяться он не будет никогда.

2. Если имя ветки, в которой запускаются тесты в репе фронта, совпадает с полем **branchWithFix**, то тогда (и только тогда) скип не применяется.
> Что это значит: Это значит, что если вы не удалите скип из репозитория после релиза тикета с фиксом, то тест так и останется скипнутым.

----
В каждой платформе 2 вида скипов:

`actual` - скипы, актуальные для всех веток

`deprecated` - скипы, которые нужны в старых ветках. Эти скипы не будут участовать в проверке на починившиеся тесты.
Используется, например, при изменении fullName тестов в результатет рефакторинга.

Описание скипа:

`issue` - тикет на расскип

`reason` - причина скипа

`fullNames` - список полных названий тесто**в**

`branchWithFix` - ветка, для которой скип будет игнорироваться, необязательное поле

### Добавить новый скип

- Отредактирую файл соответствующей платформы  
- Сделай PR  
- Дождись прохождения проверок  
- Смёржи в мастер  

### Расскипать тест, который был исправлен в тикете (но тикет ещё не в мастере)

#### Вариант 1: Есть ветка `users/<name>/my-awesome-fix` с фиксом теста
- Найди скип по имени теста в файле соответствующей платформы
- Удали строчку с именем теста в объекте скипа
- Если тестов в этом объекте скипа больше не осталось - удали и сам объект.
- Закоммить изменения **в ветку с фиксом** (прям в той же ветке `users/<name>/my-awesome-fix`, да)
- Дожидись прохождения тестов в ветке
- Убедись, что тест зелёный, отдай в QA

#### Вариант 2: Тест локально зелёный без каких-либо фиксов

В мультитестинге [стенд с мастером](https://tsum.yandex-team.ru/pipe/projects/front-market-unified/multitestings/environments/marketfront-17830) после каждого релиза скипнутые тесты прогоняются по стенду с транком.

Сначала запускаются все скипнутые тесты, в следующем кубике перезапускаются все зелёные после первого прогона, потом ещё раз, ещё раз и ещё раз

Посмотри на первый и последний кубик
![](https://jing.yandex-team.ru/files/lengl/uhura_2022-06-27T12%3A44%3A20.561572.jpg)

Если тест зелёный после 5 перепрогонов - в целом его можно расскипать полностью (см. следующий пункт).
Но лучше всё-таки разобраться почему тест ломался/флапал в первую очередь.

### Полностью расскипать тест

- Найди скип по имени теста в файле соответствующей платформы
- Удали строчку с именем теста в объекте скипа
- Если тестов в этом объекте скипа больше не осталось - удали и сам объект, а тикет на расскип закрой.
- Сделай PR в эту репу, дождись проверок, смёржи PR в мастер этой репы.

Тогда тест будет расскипан везде.

### Расскипать тесты локально (при прогоне на своей машинке)

(если он у вас есть)
В файл пользовательского конфига GINNY_USER_CONFIG положить `suiteManager.skips: []`

![скрин](https://jing.yandex-team.ru/files/lengl/uhura_2021-11-10T12%3A10%3A26.771241.png)

Либо отредактировать `marketfront/configs/hermione/tests-config/skipped/market/(desktop|touch).json` локально

![скрин](https://jing.yandex-team.ru/files/lengl/uhura_2021-11-10T12%3A11%3A09.480410.png)

### Если не проходит обязательная проверка

Вариант 1: PR создан из форка, а не в репозитории.

**Как опознать:** В заголовке PR есть двоеточия в названии веток и часть до двоеточия у веток различается ![скрин](https://jing.yandex-team.ru/files/lengl/uhura_2021-09-13T10%3A52%3A59.727256.png)

**В чём причина:** Скорее всего у вас нет прав на запись в репозиорий и гит предложил вам сделать PR из форка. Джоба в Sandbox не может в PR-ы из форка и крашится.

**Как лечить:** Запросить права на запись у [@lengl](https://staff.yandex-team.ru/lengl) или в чатике `Market front infra support`, пересоздать PR с новой веткой - уже внутри репозитория, а не из форка.

Вариант 2: Нарушен json-формат файла

**Как опознать:** В проверке написано "Неудача! Смотри отчёт". В отчёте - красные строки, иногда с понятным описанием, иногда нет.

**Как лечить:** Поправить формат файла. Убрать лишние запятые в конце массива, обернуть в двойные кавычки вместо одинарных, и т.п.

### Как работает автоматика вокруг репозитория

#### Скипы по кнопке
В [релизном пайплайне](https://tsum.yandex-team.ru/pipe/projects/front-market-unified/pipelines/front-release) фронта Маркета есть джобы `WhiteMarketAutotestSkipJob`. Они:
  1. Стоят после перезапуска автотестов, находят последнюю джобу типа `MARKET_AUTOTEST_HERMIONE`, запущенную с фикс-версией релиза.
  2. Запускают Sandbox-джобу `MARKET_FRONT_AUTOTEST_SKIP`
  3. Джоба смотрит, какие тесты упали в файлике `data/suites.json` в ресурсе `MARKET_AUTOTEST_REPORT` у этой джобы
  4. Джоба клонирует себе этот репозиторий, читает JSON, создаёт новый тикет, добавляет скипы в объект скипов и сохраняет его в тот же JSON-файл
  5. После чего джоба коммитит новые скипы

#### Автоматические расскипы
СЕЙЧАС НЕ РАБОТАЮТ (отключены @lengl) т.к. расскипывают много флапающих тестов.

Как работет когда включено:

После того, как фронтовый релиз завершается (или на любой коммит в мастер) - ЦУМ пересобирает [среду с мастером](https://tsum.yandex-team.ru/pipe/projects/front-market-unified/multitestings/environments/marketfront-17830).

В пайплайне этой среды джоба `MARKET_AUTOTESTS` запускается с переменными окружения `"CHECK_AUTOTEST_STATUS_FOR_SKIP": 1` и `"hermione-broken-tests_collect_passed": "true"`

Первая заставляет джобу запускать не все тесты фронта, а только скипнутые тесты (тесты из этого репозитория)
Вторая - заставляет в файлик `broken_tests.txt` складывать не упавшие тесты, а зелёные тесты

Зелёные тесты последовательно перезапускаются несколько раз (чтобы флапающие тесты пожелтели, и остались только стабильно зелёные тесты).

После чего запускается SANDBOX-джоба `MARKET_FRONT_AUTOTEST_SKIP` в режиме `mode: unskip`

Джоба точно так же читает из файлика `data/suites.json`, но уже зелёные тесты.
Клонирует репозиторий, читайт JSON скипов
Ищет в нём скипнутые зелёные тесты и удаляет их из объекта
Если после удаления в объекте не осталось скипов - то указанный в `issue` тикет - автоматически закрывается

После чего изменения коммитятся в репозиторий скипов