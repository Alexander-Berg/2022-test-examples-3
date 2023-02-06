const PO = require('./PO');

describe('Верхнее меню', function() {
    describe('Положительные', function() {
        it('1. Внешний вид верхнего меню для пользователя с расширенной ролью', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-002'
            });
            // в верхнем меню есть логотип "АВС", пункты "Все сервисы",
            // "Подтверждения", "Ресурсы", "Заказ железа", кнопка "Создать сервис",
            // поле "Поиск сервисов" с кнопкой "Найти", иконки мессенджера и колокольчика, аватар
            await this.browser.waitForVisible(PO.header(), 5000);
            // нужно подождать, пока появится всё на странице, в том числе и скролл,
            // который меняет ширину верхнего меню
            await this.browser.waitForVisible(PO.services(), 15000);
            await this.browser.assertView('base-role-menu', PO.header());
        });
        it('2. Поиск сервиса в шапке по названию', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-002'
            });
            // в верхнем меню есть поле "Поиск сервисов" с кнопкой "Найти"
            await this.browser.waitForVisible(PO.header.search(), 5000);
            // ввести в поле "Поиск сервисов" текст "автотестовый сервис"
            // если не кликнуть в поле ввода, то саджеста не будет
            await this.browser.click(PO.header.search.input());
            await this.browser.customSetValue(PO.header.search.input(), 'автотестовый сервис');
            // появился саджест, в котором все предложенные варианты
            // содержат словосочетание "автотестовый сервис" без учёта регистра
            await this.browser.waitForVisible(PO.searchSuggest(), 10000);
            await this.browser.waitForVisible(PO.searchSuggest.autotestService(), 5000);
            await this.browser.assertPopupView(PO.searchSuggest(), 'search-suggest', PO.searchSuggest());
            // кликнуть в саджесте на вариант "Автотестовый сервис для запроса ролей 1"
            await this.browser.click(PO.searchSuggest.autotestService());
            // произошёл переход на страницу сервиса "Автотестовый сервис для запроса ролей 1"
            const url = await this.browser.yaGetParsedUrl();
            assert(url.pathname === '/services/autotestserviceforrolereq1/',
                'Не произошел редирект на страницу сервиса "Автотестовый сервис для запроса ролей 1"',
            );
        });
        it('3. Поиск сервиса в шапке по слагу', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-002'
            });
            // в верхнем меню есть поле "Поиск сервисов" с кнопкой "Найти"
            await this.browser.waitForVisible(PO.header.search(), 5000);
            // ввести в поле "Поиск сервисов" текст "autotestserviceforrolereq1"
            await this.browser.click(PO.header.search.input());
            await this.browser.customSetValue(PO.header.search.input(), 'autotestserviceforrolereq1');
            // появился саджест, в котором только один вариант - "Автотестовый сервис для запроса ролей 1"
            await this.browser.waitForVisible(PO.searchSuggest(), 10000);
            await this.browser.waitForVisible(PO.searchSuggest.autotestService(), 5000);
            // кликнуть в саджесте на вариант "Автотестовый сервис для запроса ролей 1"
            await this.browser.click(PO.searchSuggest.autotestService());
            // произошёл переход на страницу сервиса "Автотестовый сервис для запроса ролей 1"
            const url = await this.browser.yaGetParsedUrl();
            assert(url.pathname === '/services/autotestserviceforrolereq1/',
                'Не произошел редирект на страницу сервиса "Автотестовый сервис для запроса ролей 1"',
            );
        });
        it('4. Поиск сервиса в шапке по id сервиса', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-002'
            });
            // в верхнем меню есть поле "Поиск сервисов" с кнопкой "Найти"
            await this.browser.waitForVisible(PO.header.search(), 5000);
            // ввести в поле "Поиск сервисов" текст "4424"
            await this.browser.click(PO.header.search.input());
            await this.browser.customSetValue(PO.header.search.input(), '4424');
            // появился саджест, в котором есть только один вариант - "Автотестовый сервис для запроса ролей 1"
            await this.browser.waitForVisible(PO.searchSuggest(), 10000);
            await this.browser.waitForVisible(PO.searchSuggest.autotestService(), 5000);
            // кликнуть в саджесте на вариант "Автотестовый сервис для запроса ролей 1"
            await this.browser.click(PO.searchSuggest.autotestService());
            // произошёл переход на страницу сервиса "Автотестовый сервис для запроса ролей 1"
            const url = await this.browser.yaGetParsedUrl();
            assert(url.pathname === '/services/autotestserviceforrolereq1/',
                'Не произошел редирект на страницу сервиса "Автотестовый сервис для запроса ролей 1"',
            );
        });
        it('5. Переход на страницу создания нового сервиса', async function() {
            // открыть ABC (/)
            await this.browser.openIntranetPage({
                pathname: '/',
            }, {
                user: 'robot-abc-002'
            });
            // в верхнем меню есть кнопка "Создать сервис"
            await this.browser.waitForVisible(PO.header.createServiceButton(), 5000);
            // кликнуть на кнопку "Создать сервис"
            await this.browser.click(PO.header.createServiceButton());
            // произошёл переход на страницу создания нового сервиса
            const url = await this.browser.yaGetParsedUrl();
            assert(url.pathname === '/create-service',
                'Не произошел редирект на страницу создания нового сервиса',
            );
        });
    });
});
