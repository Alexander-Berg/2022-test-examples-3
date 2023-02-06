const { assert } = require('console');
const PO = require('./PO');

describe('Ресурсы /', function() {
    describe('Общая страница ресурсов', function() {
        describe('Положительные', function() {
            it('1. Переключение между страницами на общем списке ресурсов', async function() {
                // открыть страницу со списком ресурсов (/resources/)
                await this.browser.openIntranetPage({ pathname: '/resources/' }, { user: 'robot-abc-001' });
                // внизу страницы есть кнопки "показать ещё" и "дальше"
                await this.browser.waitForVisible(PO.pagerBlock(), 10000);
                await this.browser.assertView('resources-pager-block', PO.pagerBlock());
                // таблица с ресурсами загрузилась
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 15000);
                // количество ресурсов на странице до клика на "Показать ещё" равно 20
                let resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(resourcesCount.length === 20, `На странице должно быть 20 строк с ресурсами (а получили ${resourcesCount.length})`);
                // кликнуть на кнопку "показать ещё"
                await this.browser.click(PO.pagerBlock.loadMoreButton());
                // покрутился и пропал спиннер
                await this.browser.waitForVisible(PO.resourceSpinner(), 25000, true);
                // таблица с ресурсами загрузилась
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // проверяем, что подгрузилась вторая страница с ресурсами
                resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(
                    resourcesCount.length > 20 && resourcesCount.length <= 40,
                    `На странице должно быть больше 20, но меньше 40 ресурсов, а получили ${resourcesCount.length}`,
                );
                // в URL появился параметр "page"
                let parsedUrl = await this.browser.yaGetParsedUrl();
                assert(parsedUrl.searchParams.has('page'), 'В URL должен был появиться параметр page');
                // кнопки "показать ещё" и "дальше" не пропали, добавилась кнопка "назад"
                await this.browser.assertView('back-button', PO.pagerBlock());
                // кликнуть в переключателе на кнопку "дальше"
                await this.browser.click(PO.pagerBlock.nextPageButton());
                // покрутился и пропал спиннер
                await this.browser.waitForVisible(PO.resourceSpinner(), 25000, true);
                // подгрузилась следующая страница с ресурсами
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // на странице показано не больше 20 ресурсов
                resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(resourcesCount.length === 20, `На странице должно быть 20 строк с ресурсами (а получили ${resourcesCount.length})`);
                // в URL остался параметр "page"
                parsedUrl = await this.browser.yaGetParsedUrl();
                assert(parsedUrl.searchParams.has('page'), 'В URL должен был остаться параметр page');
                // кликнуть на кнопку "назад" два раза
                await this.browser.click(PO.pagerBlock.prevPageButton());
                // подгрузилась следующая страница с ресурсами
                await this.browser.waitForVisible(PO.resourceSpinner(), 25000, true);
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                await this.browser.click(PO.pagerBlock.prevPageButton());
                // подгрузилась следующая страница с ресурсами
                await this.browser.waitForVisible(PO.resourceSpinner(), 25000, true);
                await this.browser.waitForVisible(PO.resourcesTable.rowsInResourceTable(), 10000);
                // на странице показано не больше 20 ресурсов
                resourcesCount = await this.browser.$$(PO.resourcesTable.rowsInResourceTable());
                assert(
                    resourcesCount.length === 20,
                    `На странице должно быть 20 ресурсов, а получили ${resourcesCount.length}`,
                );
                // в URL остался параметр "page"
                parsedUrl = await this.browser.yaGetParsedUrl();
                assert(parsedUrl.searchParams.has('page'), 'В URL должен был остаться параметр page');
                // кнопка "назад" пропала, есть только "показать ещё" и "дальше"
                await this.browser.waitForVisible(PO.pagerBlock.loadMoreButton(), 1000);
                await this.browser.waitForVisible(PO.pagerBlock.nextPageButton(), 1000);
                await this.browser.waitForVisible(PO.pagerBlock.prevPageButton(), 1000, true);
            });
        });
    });
});
