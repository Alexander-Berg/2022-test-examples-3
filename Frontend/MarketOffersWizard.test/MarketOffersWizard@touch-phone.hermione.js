'use strict';

const { MarketOffersWizard } = require('../../../Market.test/Market.page-object');
const { MockDataFirstAutoloadResponse } = require('./test-stubs/mockDataFirstAutoloadResponse');
const ajaxUrl = 'search/report_market';
const ajaxDelay = 1000;
const mockOptionsFirstResponse = {
    timeout: ajaxDelay,
    recordData: [ajaxUrl],
    urlDataMap: {
        [ajaxUrl]: MockDataFirstAutoloadResponse,
    },
};

specs({
    feature: 'Маркет. Офферный колдунщик',
}, () => {
    it('Проверка внешнего вида', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 246877604,
        }, MarketOffersWizard());

        await this.browser.assertView('plain', MarketOffersWizard());
    });

    it('Проверка внешнего вида с VT=market_offers_wizard_center_incut', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 4250366517,
        }, MarketOffersWizard());

        await this.browser.assertView('plain', MarketOffersWizard());
    });

    it('Проверка внешнего вида с тайтлом более 3х строк', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 531460829,
        }, MarketOffersWizard());

        await this.browser.assertView('plain', MarketOffersWizard());
    });

    // Ссылки не являются браузерозависимыми.
    hermione.only.in(['chrome-phone']);
    describe('Проверка ссылки', () => {
        it('В кнопке «ещё»', async function() {
            const bro = this.browser;

            await bro.yaOpenSerp({
                foreverdata: 1677495901,
            }, MarketOffersWizard());

            const href = await bro.getAttribute(MarketOffersWizard.marketItemsContainer.scroller.moreItem.link(), 'href');
            assert.include(href, '/products/search', 'в ссылке некорректный путь');

            const { searchParams } = new URL(href);
            assert.isTrue(searchParams.has('rs'), 'в ссылке нет параметра rs');
            assert.isTrue(searchParams.has('parent-reqid'), 'в ссылке нет параметра parent-reqid');
            assert.strictEqual(searchParams.get('utm_source_service'), 'web', 'в ссылке нет параметра utm_source_service или у него неверное значение');
        });
    });

    it('Скрытие кнопки «ещё»', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 925983392,
            exp_flags: 'PRODUCTS_2x2_hide_button=1',
        }, MarketOffersWizard());

        await this.browser.yaShouldNotBeVisible(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore(), 'Кнопка «ещё» не скрылась');
    });

    //Так как запрос дозагрузки не работает в searchapp-phone, написан текст на расхлоп без дозапроса
    hermione.only.notIn(['chrome-phone', 'iphone'], 'Тест без дозагрузки для searchapp-phone');
    it('Внешний вид c расхлопом (searchapp-phone)', async function() {
        await this.browser.yaOpenSerp({
            // В данных 10 карточек
            foreverdata: 939423507,
        }, MarketOffersWizard());
        await this.browser.yaMockXHR({
            timeout: ajaxDelay,
            recordData: [ajaxUrl],
            urlDataMap: {
                [ajaxUrl]: JSON.stringify({
                    data: [],
                    requeryUrl: '',
                    isTheEnd: true,
                }),
            },
        });

        await this.browser.click(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore());
        await this.browser.assertView('after-click-more', MarketOffersWizard());

        await this.browser.click(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore());
        await this.browser.assertView('after-second-click-more', MarketOffersWizard());
    });

    it('Внешний вид c одной строчкой', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 925983392,
            exp_flags: 'PRODUCTS_expanded_initial_rows_count=1',
        }, MarketOffersWizard());

        await this.browser.assertView('one-line', MarketOffersWizard());
        await this.browser.click(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore());
        await this.browser.assertView('one-line-expanded', MarketOffersWizard());
    });

    it('С недостаточным количеством офферов', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3744070323,
        }, MarketOffersWizard());
        await this.browser.assertView('not-enough-offers', MarketOffersWizard());
    });

    it('Внешний вид c дозагрузкой', async function() {
        await this.browser.yaOpenSerp({
            // В данных уже 8 карточек, количество = замена дозапроса при инициализации компонента
            foreverdata: 2132253131,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(mockOptionsFirstResponse);

        await this.browser.yaWaitUntil('Кнопка станет доступной для клика', async () => {
            const disabled = await this.browser.yaGetAttributes(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore(), 'aria-disabled');
            return disabled[0] === 'false';
        });
        await this.browser.click(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore());
        await this.browser.yaWaitUntil('Кнопка станет доступной для клика', async () => {
            const disabled = await this.browser.yaGetAttributes(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore(), 'aria-disabled');
            return disabled[0] === 'false';
        });
        await this.browser.assertView('after-click-more', MarketOffersWizard());

        await this.browser.click(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore());
        await this.browser.yaWaitUntil('Кнопка станет доступной для клика', async () => {
            const disabled = await this.browser.yaGetAttributes(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore(), 'aria-disabled');
            return disabled[0] === 'false';
        });
        await this.browser.assertView('after-second-click-more', MarketOffersWizard());

        await this.browser.click(MarketOffersWizard.productCardsShowcase.expandContainer.buttonMore());
        await this.browser.assertView('after-third-click-more', MarketOffersWizard());
    });
});
