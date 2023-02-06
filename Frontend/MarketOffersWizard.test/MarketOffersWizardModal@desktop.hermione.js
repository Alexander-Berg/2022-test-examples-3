'use strict';

const { MarketOffersWizard, ProductCardsModal } = require('../../../Market.test/Market.page-object');

hermione.only.in('none', 'GOODS-1269: скип тестов неактуального флага во время раскатки замены Маркета на Товары');
specs({
    feature: 'Маркет. Офферный колдунщик в центре',
    type: 'Модальное окно в дебрендированном колдунщике',
}, () => {
    it('Внешний вид окна по клику на заголовок', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3520138098,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR({
            timeout: 5000,
            recordData: ['search/report_market'],
            urlDataMap: {
                ['search/report_market']: [],
            },
        });

        await this.browser.click(MarketOffersWizard.debrandingTitle());
        await this.browser.yaWaitForVisible(ProductCardsModal(), 'Модальное окно');
        await this.browser.assertView('modal_after_click_more', ProductCardsModal.scrollContainer());

        await this.browser.setViewportSize({ width: 770, height: 2200 });
        await this.browser.assertView('modal_768', ProductCardsModal.scrollContainer());

        await this.browser.setViewportSize({ width: 700, height: 2200 });
        await this.browser.assertView('modal_700', ProductCardsModal.scrollContainer());
    });

    it('Кнопка все предложения', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3520138098,
        }, MarketOffersWizard());

        await this.browser.execute(function(scrollerWrap) {
            $(scrollerWrap).scrollLeft(9999);
        }, MarketOffersWizard.scroller.wrap());
        await this.browser.assertView('button_all', MarketOffersWizard());

        await this.browser.yaMockXHR({
            timeout: 5000,
            recordData: ['search/report_market'],
            urlDataMap: {
                ['search/report_market']: [],
            },
        });

        await this.browser.click(MarketOffersWizard.productCard.more());
        await this.browser.yaWaitForVisible(ProductCardsModal(), 'Модальное окно');
        await this.browser.assertView('modal_after_click_more', ProductCardsModal.scrollContainer());
    });

    it('Внешний вид с большими карточками', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3520138098,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR({
            timeout: 5000,
            recordData: ['search/report_market'],
            urlDataMap: {
                ['search/report_market']: [],
            },
        });

        await this.browser.click(MarketOffersWizard.debrandingTitle());
        await this.browser.yaWaitForVisible(ProductCardsModal(), 'Модальное окно');
        await this.browser.assertView('modal_after_click_more', ProductCardsModal.scrollContainer());

        await this.browser.setViewportSize({ width: 770, height: 2200 });
        await this.browser.assertView('modal_768', ProductCardsModal.scrollContainer());

        await this.browser.setViewportSize({ width: 700, height: 2200 });
        await this.browser.assertView('modal_700', ProductCardsModal.scrollContainer());
    });
});
