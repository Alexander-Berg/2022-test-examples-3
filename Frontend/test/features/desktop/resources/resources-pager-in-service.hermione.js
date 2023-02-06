const PO = require('./PO');

describe('Ресурсы /', function() {
    describe('Страница ресурсов сервиса', function() {
        describe('Положительные', function() {
            it('1. Переключение между страницами на списке ресурсов', async function() {
                // открыть страницу со списком ресурсов сервиса "Потомок 1"
                await this.browser.openIntranetPage({
                    pathname: '/services/descendant1/resources/',
                    query: { layout: 'table' },
                }, {
                    user: 'robot-abc-001',
                });
                // внизу страницы есть кнопки "показать ещё" и "дальше"
                await this.browser.waitForVisible(PO.pagerBlock.loadMoreButton(), 5000);
                await this.browser.waitForVisible(PO.pagerBlock.nextPageButton(), 100);
                // таблица с ресурсами загрузилась
                await this.browser.waitForVisible(PO.resourceSpinner(), 10000, true);
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // количество ресурсов на странице до клика на "Показать ещё" равно 20
                let resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(resourcesCount.length === 20, `На странице должно быть 20 строк с ресурсами (а получили ${resourcesCount.length})`);
                // кликнуть на кнопку "показать ещё"
                await this.browser.click(PO.pagerBlock.loadMoreButton());
                // покрутился и пропал спиннер
                await this.browser.waitForVisible(PO.resourceSpinner(), 20000, true);
                // таблица с ресурсами загрузилась
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // в URL появился параметр "page"
                let parsedUrl = await this.browser.yaGetParsedUrl();
                assert(parsedUrl.searchParams.has('page'), 'В URL должен был появиться параметр page');
                // кнопки "показать ещё" и "дальше" пропали, есть только кнопка "назад"
                await this.browser.waitForVisible(PO.pagerBlock.prevPageButton(), 5000);
                await this.browser.waitForVisible(PO.pagerBlock.loadMoreButton(), 100, true);
                await this.browser.waitForVisible(PO.pagerBlock.nextPageButton(), 100, true);
                // открыта вторая страница с ресурсами
                resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(
                    resourcesCount.length > 20 && resourcesCount.length <= 40,
                    `На странице должно быть больше 20, но меньше 40 ресурсов, а получили ${resourcesCount.length}`,
                );
                // кликнуть на кнопку "назад"
                await this.browser.click(PO.pagerBlock.prevPageButton());
                // подгрузилась первая страница с ресурсами
                await this.browser.waitForVisible(PO.resourceSpinner(), 20000, true);
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // в URL остался параметр "page"
                parsedUrl = await this.browser.yaGetParsedUrl();
                assert(parsedUrl.searchParams.has('page'), 'В URL должен был остаться параметр page');
                // внизу страницы есть кнопки "показать ещё" и "дальше", кнопки "назад" нет
                await this.browser.waitForVisible(PO.pagerBlock.loadMoreButton(), 5000);
                await this.browser.waitForVisible(PO.pagerBlock.nextPageButton(), 100);
                await this.browser.waitForVisible(PO.pagerBlock.prevPageButton(), 100, true);
                // на странице показано 20 ресурсов
                resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(resourcesCount.length === 20, `На странице должно быть 20 строк с ресурсами (а получили ${resourcesCount.length})`);
                // кликнуть на кнопку "дальше"
                await this.browser.click(PO.pagerBlock.nextPageButton());
                // открыта вторая страница с ресурсами
                await this.browser.waitForVisible(PO.resourceSpinner(), 20000, true);
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // в URL появился параметр "page"
                parsedUrl = await this.browser.yaGetParsedUrl();
                assert(parsedUrl.searchParams.has('page'), 'В URL должен был остаться параметр page');
                // кнопки "показать ещё" и "дальше" пропали, есть только кнопка "назад"
                await this.browser.waitForVisible(PO.pagerBlock.prevPageButton(), 5000);
                await this.browser.waitForVisible(PO.pagerBlock.loadMoreButton(), 100, true);
                await this.browser.waitForVisible(PO.pagerBlock.nextPageButton(), 100, true);
                // на странице показано меньше 20 ресурсов
                resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(resourcesCount.length < 20, `На странице должно быть меньше 20 строк с ресурсами (а получили ${resourcesCount.length})`);
            });
            it('2. Если на странице меньше 20 записей про ресурсы, то переключатель страниц отсутствует', async function() {
                // открыть страницу со списком ресурсов сервиса "autotest-no-resources-pager"
                await this.browser.openIntranetPage({
                    pathname: '/services/autotest-no-resources/resources/',
                    query: { layout: 'table' },
                }, {
                    user: 'robot-abc-001',
                });
                // таблица с ресурсами загрузилась
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // на странице не больше 20 ресурсов
                let resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(resourcesCount.length <= 20, `На странице должно быть 20 строк с ресурсами (а получили ${resourcesCount.length})`);
                // внизу страницы нет кнопки "Показать ещё" и переключателя с номерами страниц
                await this.browser.waitForVisible(PO.pagerBlock(), 1000, true);
            });
        });
    });
});
