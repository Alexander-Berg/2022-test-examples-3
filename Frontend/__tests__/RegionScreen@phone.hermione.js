const __ym = {
    doc_ui: 'touch-phone',
    turbo_page: 1,
};

describe('RegionScreen', function() {
    it('Переход на экран выбора города доставки', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'user',
            expFlags: {
                'turbo-app-ask-user-city': 1,
                'analytics-disabled': '0',
            },
            query: { patch: 'setBlackboxData' },
        });

        await browser.yaWaitForVisible('.EcomDrawer');
        await browser.click('.EcomDrawer-FooterControl button');
        await browser.yaWaitForHidden('.EcomDrawer');
        await browser.click('.UserScreen-MenuItem:nth-child(2) .CategoryList-Item');

        await browser.yaWaitForVisible('.RegionScreen');

        await browser.click('.ScreenHeaderBack');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'select-another-region-back-clicked',
            params: { ecom_spa: 1, __ym },
        });

        await browser.yaWaitForVisible('.UserScreen');
        await browser.assertView('plain', '.UserScreen-City', { ignoreElements: '.UserScreen-City p' });
    });

    it('Внешний вид экрана выбора города доставки', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'region',
            expFlags: {
                'turbo-app-ask-user-city': 1,
            },
        });

        /** время на появление попапа выбора города (удостоверимся, что он не показался) */
        await browser.pause(500);
        await browser.yaShouldNotBeVisible('.EcomDrawer');

        /** не фиксируем input, так как внутри может содержаться название произвольного города */
        await browser.assertView('plain', '.RegionScreen', { ignoreElements: ['input'] });
    });

    it('Навигация по кнопке \'назад\' на главную', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'region',
            expFlags: {
                'turbo-app-ask-user-city': 1,
            },
        });

        await browser.waitForVisible('.ScreenHeaderBack');
        await browser.click('.ScreenHeaderBack');

        /** Проверяем, что перешли на главную страницу */
        await browser.yaWaitForVisible('.EcomScreen_type_main');
    });

    it('Изменение города доставки', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'user',
            expFlags: {
                'turbo-app-ask-user-city': 1,
                'analytics-disabled': '0',
            },
            query: { patch: 'setBlackboxData' },
        });

        await browser.yaWaitForVisible('.EcomDrawer');
        await browser.click('.EcomDrawer-FooterControl button');
        await browser.yaWaitForHidden('.EcomDrawer');

        await browser.click('.UserScreen-City');

        await browser.yaWaitForVisible('.RegionScreen');
        assert.isTrue(
            await browser.isEnabled('.RegionScreen .RegionScreen-GeoSuggest~button'),
            'Кнопку по-умолчанию должно быть можно нажать, не внося никаких изменений',
        );

        /** Выставляем несуществующий город и не выбираем из списка */
        await browser.setValue('.RegionScreen input', 'abc');

        /** Сброс фокуса с input */
        await browser.execute(function() {
            document.querySelector('.RegionScreen-Description').blur();
        });

        assert.isFalse(
            await browser.isEnabled('.RegionScreen .RegionScreen-GeoSuggest~button'),
            'После изменения input кнопка должна быть disabled',
        );

        await browser.setValue('.RegionScreen input', 'Челябинск');

        await browser.yaWaitForVisible('.SuggestItem');
        await browser.yaScrollPage('.SuggestItem');
        await browser.click('.SuggestItem');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'select-another-region-city-suggest-clicked',
            params: { ecom_spa: 1, __ym },
        });

        assert.isTrue(
            await browser.isEnabled('.RegionScreen .RegionScreen-GeoSuggest~button'),
            'Кнопка подтверждения выбора города должна быть кликабельна после выбора города из саджеста'
        );
        await browser.click('.RegionScreen .RegionScreen-GeoSuggest~button');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'select-another-region-save-clicked',
            params: { ecom_spa: 1, __ym, geoid: 56, name: 'Челябинск' },
        });

        const text = await browser.getText('.UserScreen-City p');
        assert.strictEqual(text, 'Челябинск');
    });
});
