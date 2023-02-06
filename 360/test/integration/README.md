## Интеграционное тестирование

В интеграционных тестах генерируются входящие http-запросы и мокаются исходящие.

Тесты пишутся в файлах `*.integration.js`, моки лежат в папках `internal-lib/services/*/__nocks__`.

Моки проверяют, что было сделано заданное количество запросов в бэкенды и с заданными параметрами.

Ответы тестируются с помощью `jest`-снэпшотов, куда записываются статус, тело и ограниченный набор ответных заголовков.

### Запуск интеграционных тестов

`npm run integration`

Под капотом запускается `jest`, поэтому работают все его параметры:

`npm run integration -u` — обновить снэпшоты

`npm run integration --coverage=true` — посчитать покрытие

`npm run integration --testPathPattern=auth` — прогнать только тестовые файлы по маске пути

### Отладка интеграционных тестов

`DEBUG=core:* npm run integration` — выводить ошибки Duffman’а

`DEBUG=nock.* npm run integration` — выводить ошибки nock‘а

`DEBUG=* npm run integration` — выводить все ошибки

### Генерация http-моков

Для создания http-моков можно включить запись исходящих http-запросов с помощью переменной окружения `NOCK_REC=true`:

`NOCK_REC=true npm start`

После этого все исходящие запросы будут сохранены в корневую папку под именами `0.yaml`, `1.yaml` и т.д. Эти файлы можно использовать в качестве заготовок для моков соответствюущих сервисов.