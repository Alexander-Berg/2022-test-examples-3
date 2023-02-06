# Как это работает

## Компоненты, участвующие во взаимодействии

### Со стороны автотестов (Test)

**Hermione** - [Гермиона](https://github.com/gemini-testing/hermione), фреймворк автоматического интеграционного тестирования, используемый на сервисах Маркета
**Plugin** - [плагин](https://a.yandex-team.ru/arcadia/market/front/apps/kadavrique/plugin) для Гермионы, реализованый в Кадаврике.
**Test** - исходный код автотеста.

### Со стороны клиента (Selenium)

**Browser** - браузер.

### Со стороны сервера (FrontEnd)

**Stout** - [node.js фреймворк](https://a.yandex-team.ru/arcadia/market/front/monomarket/lib/lib/stout), обрабатывающий запросы к фронтенду сервисов Маркета.
**Resource** - библиотека для унификации запросов к бэкендам. Созданные с её помощью адаптеры для бэкендов тоже называются «ресурсами».

### Со стороны Кадаврика (Kadavrique)

**API** - HTTP-сервер, обрабатывающий запросы к Кадаврику.
**State** - стейт приложения.
**Backend** - моки бекендов.

## Схема взаимодействия компонентов

![Схема взаимодействия компонентов](https://wiki.yandex-team.ru/users/b-vladi/kadavrik/.files/kada1.png)

## Описание шагов

### Первый этап: создание сессии

По событию Гермионы о начале теста **(1)** плагином Кадаврика генерируется уникальный идентификатор сессии **(2)** и сохраняется в объекте теста **(3)**. После этого плагин открывает пустую HTML-страницу сервиса **(4)** и сохраняет сгенерированный ID сессии в куки **(5)**, используя API Webdriver IO. Далее идентификатор отправляется в API Кадаврика через ручку `PUT /session/<id>`, которая создаёт новую сессию **(6)** для текущего теста. После этого шага работа плагина завершается **(7)**.

### Второй этап: выполнение теста

Гермиона запускает тест на выполнение **(8)**. Как правило, перед началом выполнения логики кейса теста происходит конфигурирование стейта приложения. Для этого из теста происходит обращение к JavaScript API плагина Кадаврика через вызов метода `browser.setState()` **(9)**. Плагин принимает новые данные для стейта и отправляет их в Кадаврик ручкой `POST /session/<id>/state/<path>` **(10)**, где они сохраняются в стейте приложения **(11)**. Стейт с данными привязан к сессии, поэтому другие тесты не будут иметь к нему доступа.


Тест может обращаться к API Кадаврика на любом этапе выполнения и столько раз, сколько это необходимо.


Во время выполнения теста через API Webdriver IO происходят различные манипуляции с интерфейсом сервиса **(12)**, часть из которых инициирует запросы к фронтенду **(13)**. Вместе с запросом фронтенд принимает куку, выставленную ранее на этапе создания сессии **(5)**.


По логике того или иного запроса могут происходить обращения к ресурсам **(14)** для получения данных. Базовая обертка ресурсов в Mandrel-е направляет запрос на предварительно сконфигурированный хост и порт Кадаврика **(15)**, в котором, помимо прочего, передаёт дополнительный заголовок, содержащий ID сессии из куки. HTTP-сервер Кадаврика принимает запрос, производит поиск мока, метода и сессии, исходя из параметров запроса. Если запрос успешно проходит проверки, Кадаврик вызывает соответствующий метод мока **(16)**. Мок обращается за данными в стейт **(17)**, формирует на их основе результат и возвращает Кадаврику **(18)**. Полученный результат Кадаврик возвращает в ответ на запрос ресурса **(19)**, откуда он передаётся в слой бизнес-логики обработки запроса.


После получения и обработки необходимых данных, фронтенд возвращает результат браузеру **(20)**, в котором содержатся данные из стейта приложения. Тест продолжает свою работу и по завершении выполнения всех необходимых действий **(12)** делает финальную проверку результата **(21)**. Для этого автор теста может использовать данные стабов, которые он передавал при их создании **(9)**. Далее тест завершает свою работу **(22)** и наступает последний этап работы Кадаврика.

### Третий этап: удаление сессии

По событию Гермионы завершения работы теста **(23)**, плагин Кадаврика выполняет запрос к API Кадаврика через ручку `DELETE /session/<id>` **(24)**. Вызов этой ручки очищает данные из стейта **(25)** для указанной сессии и удаляет сессию из списка открытых сессий Кадаврика. После чего плагин завершает свою работу **(26)**.
