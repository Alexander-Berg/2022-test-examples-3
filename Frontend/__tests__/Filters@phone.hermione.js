const { URL } = require('url');

const RESPONSE_DELAY = 3000;

specs({
    feature: 'Filters',
}, () => {
    hermione.only.notIn('safari13');
    it('Кнопка фильтра', function() {
        let url;

        return this.browser
            .url('/turbo?text=technopark.ru/yandexturbocatalog/&page_type=filters')
            .yaWaitForVisible(PO.blocks.filtersActions.button(), 'Кнопка не появилась на странице')
            .getUrl()
            .then(currentUrl => url = currentUrl)
            .yaMockFetch({
                delay: RESPONSE_DELAY,
                urlDataMap: {
                    '&page_type=filter_count&ajax=1': '{"count":17,"url":"/turbo?text=technopark.ru/yandexturbocatalog/&filters=price%3A%2C100"}',
                },
            })
            .click(PO.blocks.filters.priceRange.secondInput())
            .keys('100')
            .pause(300)
            .assertView('loading', PO.blocks.filtersActions.button(), {
                hideElements: [PO.blocks.widgetFeedback.callButton()],
            })
            .click(PO.blocks.filtersActions.button())
            .getUrl()
            .then(currentUrl => {
                assert.strictEqual(currentUrl, url, 'Кнопка должна быть не кликабельна при обновлении кол-ва товаров');
            })
            .pause(RESPONSE_DELAY)
            .assertView('ready', PO.blocks.filtersActions.button(), {
                hideElements: [PO.blocks.widgetFeedback.callButton()],
            })
            .click(PO.blocks.filtersActions.button())
            .getUrl()
            .then(currentUrl => {
                assert.strictEqual(new URL(currentUrl).searchParams.get('filters'), 'price:,100', 'URL страницы не содержит параметр фильтра');
            });
    });
});
