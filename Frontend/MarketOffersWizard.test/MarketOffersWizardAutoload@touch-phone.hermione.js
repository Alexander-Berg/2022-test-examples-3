'use strict';
const url = require('url');
const { MarketOffersWizard } = require('../../../Market.test/Market.page-object');
const mockData = require('./test-stubs/mockData');
const ajaxUrl = 'search/report_market';
const ajaxDelay = 1000;
const mockOptions = {
    timeout: ajaxDelay,
    recordData: [ajaxUrl],
    urlDataMap: {
        [ajaxUrl]: mockData,
    },
};

function checkIgnoreIdsInLastRequest(browser, ignoreIds) {
    return async () => {
        const requestsData = await browser.yaGetXHRRecords(ajaxUrl);
        const lastRequestData = requestsData[requestsData.length - 1];

        const urlData = url.parse(lastRequestData.url, true);
        const ignoreIdsParam = urlData.query.market_slider_ignore_doc;

        // Проверяем что все id были переданы в запрос
        ignoreIds.forEach(id => {
            assert.include(ignoreIdsParam, id);
        });
    };
}

specs({
    feature: 'Маркет. Офферный колдунщик',
    type: 'Бесконечная карусель',
}, () => {
    hermione.only.notIn('searchapp-phone', 'Не работает задержка перед дозагрузкой');
    it('Внешний вид', async function() {
        // offerId из foreverdata
        const offerIds = ['u-5cg7cPbco2YPG0eEfowQ', 'TeAAJpC82yumxcjiO9dyTQ', 'AiXhJc07CGWdwEwQ-ZEeYQ'];

        const firstRequestMock = {
            timeout: ajaxDelay,
            recordData: [ajaxUrl],
            urlDataMap: {
                [ajaxUrl]: { ...mockData, isTheEnd: false },
            },
        };

        const secondRequestMock = {
            timeout: ajaxDelay,
            recordData: [ajaxUrl],
            urlDataMap: {
                [ajaxUrl]: { data: [], isTheEnd: true },
            },
        };

        await this.browser.yaOpenSerp({
            foreverdata: 945016434,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(firstRequestMock);
        await this.browser.assertView('plain', MarketOffersWizard());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('loader', MarketOffersWizard());

        // Проверяем что в первом запросе передались id офферов из foreverdata
        await checkIgnoreIdsInLastRequest(this.browser, offerIds);

        // Т.к. isTheEnd=false, то должен отправиться второй запрос
        await this.browser.yaMockXHR(secondRequestMock);

        // Дожидаемся окончания первого запроса
        await this.browser.pause(ajaxDelay + 100);

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);

        // Дожидаемся окончания второго запроса
        await this.browser.pause(ajaxDelay + 100);

        // Проверяем что во втором запросе передались id офферов которые пришли после ajax запроса
        await checkIgnoreIdsInLastRequest(this.browser, mockData.data.map(offer => offer.offerId));

        await this.browser.assertView('full', MarketOffersWizard());
    });

    hermione.only.notIn(['chrome-phone', 'chrome-pad', 'searchapp-phone'], 'Не работает задержка перед дозагрузкой');
    it('Дозагрузка без скролла, когда пришло 3 карточки и меньше', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3298921152,
            data_filter: 'market-constr',
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(mockOptions);
        await this.browser.assertView('loader', MarketOffersWizard());
        await this.browser.yaWaitForHidden(MarketOffersWizard.productCardLoader());
        await this.browser.assertView('loaded', MarketOffersWizard());
    });

    it('Если карточек изначально больше двух, isTheEnd=true - показываем кнопку "Еще предложения"', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3901188321,
            data_filter: 'market-constr',
        }, MarketOffersWizard());

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('with-more-arrow', MarketOffersWizard());
    });

    it('Если карточек изначально две, isTheEnd=true - не показываем кнопку "Еще предложения"', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 1074095947,
            data_filter: 'market-constr',
        }, MarketOffersWizard());

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('without-more-arrow', MarketOffersWizard());
    });

    hermione.only.notIn('searchapp-phone', 'Работает нестабильно');
    it('Бесконечная карусель, ссылка с бэка', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 1650512788,
            data_filter: 'market-constr',
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(mockOptions);
        await this.browser.assertView('plain', MarketOffersWizard());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('loader', MarketOffersWizard());
        await this.browser.yaWaitForHidden(MarketOffersWizard.productCardLoader());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('full', MarketOffersWizard());
    });
});
