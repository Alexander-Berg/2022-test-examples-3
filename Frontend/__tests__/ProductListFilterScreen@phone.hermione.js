async function openFilter(browser, config) {
    await browser.yaOpenEcomSpa({
        service: 'spideradio.github.io',
        ...config,
        pageType: 'catalog',
    });
    await browser.yaWaitForVisible('.ScreenContent');

    // переход к фильтрам
    await browser.yaScrollPage('.ProductListControl-Option_filter', 0.3);
    await browser.click('.ProductListControl-Option_filter');
    await browser.yaWaitForHidden('.NavigationTransition_state_entering');
    await browser.waitForVisible('.EcomListFilter', 'Фильтры пустые');
}

describe('ProductListFilterScreen', function() {
    it('Внешний вид страницы фильтров', async function() {
        const browser = this.browser;
        await openFilter(browser);
        await browser.assertView('filter-page', ['.ScreenContent']);
    });

    it('Проверка выбора фильтров', async function() {
        const browser = this.browser;
        await openFilter(browser);

        await browser.yaScrollPage('#p1 [data-value="1"]', 0.3);
        await browser.click('#p1 [data-value="1"]');
        await browser.yaScrollPage('#p2 [data-value="2"]', 0.3);
        await browser.click('#p2 [data-value="2"]');

        await browser.assertView('filter-page', ['.ScreenContent']);
    });

    it('Проверка выбора фильтров через "показать все"', async function() {
        const browser = this.browser;
        await openFilter(browser, { service: 'super01.ru' });
        // открываем подробный просмотр
        await browser.yaScrollPage('#p10 .EcomListFilter-Item-GroupItem_more', 0.3);
        await browser.click('#p10 .EcomListFilter-Item-GroupItem_more');
        await browser.yaWaitForVisible('.EcomListFilter-Detail', '"Показать все" не открылось');
        await browser.assertView('filter-detail-page', '.ScreenContent');
        // выбираем соседние пункты
        await browser.yaScrollPage('[data-value="6"]', 0.3);
        await browser.click('[data-value="6"]');
        await browser.yaScrollPage('[data-value="4"]', 0.3);
        await browser.click('[data-value="4"]');
        await browser.assertView('selected-filters', ['[data-value="6"]', '[data-value="4"]']);
        // выбираем еще один пункт
        await browser.yaScrollPage('[data-value="1"]', 0.3);
        await browser.click('[data-value="1"]');
        // нажимаем кнопку применить
        await browser.click('.Button2_view_action');
        await browser.yaWaitForVisible('.EcomListFilter');

        await browser.assertView('filter-page', ['.ScreenContent']);
    });

    it('Внешний вид при переходе с листинга', async function() {
        const { browser } = this;
        await openFilter(browser, { query: { category_id: '9' } });

        await browser.assertView('filter-page', ['.ScreenContent']);
    });

    it('Выбор параметров основных фильтров: цена и бренд', async function() {
        const { browser } = this;

        await openFilter(browser, { expFlags: { 'turbo-app-main-filters-only': 1 } });

        await browser.yaMockImages();
        // ingoreElements для iphone, который кнопку "Показать N товаров" добавляет в скриншот
        await browser.assertView('non-selected-filters', ['.EcomListFilter'], { ignoreElements: '.EcomListFilter-Action' });
        // открываем подробный просмотр для брендов
        await browser.yaScrollPage('#v .EcomListFilter-Item-GroupItem_more');
        await browser.click('#v .EcomListFilter-Item-GroupItem_more');
        await browser.yaWaitForVisible('.EcomListFilter-Detail', '"Показать все" не открылось');
        await browser.yaAssertViewportView('filter-detail-page');
        // выбираем соседние пункты (проверяем, что кликаются бренды)
        await browser.yaScrollPage('[data-value="1"]');
        await browser.click('[data-value="1"]');
        await browser.yaScrollPage('[data-value="13"]');
        await browser.click('[data-value="13"]');
        await browser.assertView('selected-filters', ['[data-value="1"]', '[data-value="13"]']);
        // нажимаем кнопку применить
        await browser.click('.Button2_view_action');
        await browser.yaWaitForVisible('.EcomListFilter');
        // выбираем еще один пункт (проверяем, что бренды кликаются и на экране свернутых фильтров)
        await browser.yaScrollPage('#v [data-value="10"]');
        await browser.click('#v [data-value="10"]');

        // проверяем порядок следования фильтров (выбранные - сверху)
        await browser.assertView('filter-page', ['.EcomListFilter']);

        await browser.yaWaitUntil('Число отфильтрованных товаров не загрузилось', () =>
            browser.execute(() => {
                const text = document.querySelector('.EcomListFilter-Action button').innerText;
                return !text.startsWith('Показать');
            })
        , 5000);
        await browser.click('.EcomListFilter-Action button');
        const currentUrl = new URL(await browser.getUrl());
        assert.strictEqual(
            currentUrl.pathname,
            '/turbo/spideradio.github.io/n/yandexturbocatalog/',
            'url страницы не соответствует странице каталога - не произошел переход при применении фильтров'
        );
        assert.strictEqual(
            currentUrl.searchParams.get('filters'),
            'v:1,13,10',
            'выбранные фильтры не отобразились в url или не применились вовсе'
        );
    });
});
