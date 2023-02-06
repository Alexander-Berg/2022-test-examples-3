'use strict';

const { MarketOffersWizard, ExtralinksPopup } = require('../../../Market.test/Market.page-object');

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

specs({
    feature: 'Маркет. Офферный колдунщик в центре',
}, () => {
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3854672498,
        }, MarketOffersWizard());
        await this.browser.yaMockXHR(mockOptions);
        await this.browser.assertView('plain', MarketOffersWizard());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('loader', MarketOffersWizard());

        // добавляем +100ms чтобы быть уверенными что ajax запрос отработал и контент отрисовался
        await this.browser.pause(ajaxDelay + 100);

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('after-load', MarketOffersWizard());
    });

    it('Отображение попапа с экстралинками', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 416986862,
        }, MarketOffersWizard());

        await this.browser.click(MarketOffersWizard.extralinks());
        await this.browser.yaWaitForVisible(ExtralinksPopup(), 'Попап со ссылками не показался');
        await this.browser.assertView('extralinks_popup', ExtralinksPopup());
        await this.browser.click(MarketOffersWizard.extralinks());
        await this.browser.yaWaitForHidden(ExtralinksPopup(), 'Попап со ссылками не скрылся');
    });

    it('Дозагрузка без скролла, когда пришло 3 карточки и меньше', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 4036021508,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(mockOptions);
        await this.browser.assertView('with-autoload-loader', MarketOffersWizard());
        await this.browser.yaWaitForHidden(MarketOffersWizard.productCardLoader());
        await this.browser.assertView('with-autoload-loaded', MarketOffersWizard());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('with-autoload-full', MarketOffersWizard());
    });

    it('Если карточек изначально больше двух, isTheEnd=true - показываем кнопку "Еще предложения"', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3491423774,
            data_filter: 'market-constr',
        }, MarketOffersWizard());

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('with-more-arrow', MarketOffersWizard());
    });

    it('Если карточек изначально три, isTheEnd=true - не показываем кнопку "Еще предложения"', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3659887570,
            data_filter: 'market-constr',
        }, MarketOffersWizard());

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('without-more-arrow', MarketOffersWizard());
    });

    it('Бесконечная карусель, ссылка с бэка', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 1440582004,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(mockOptions);
        await this.browser.assertView('plain', MarketOffersWizard());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('loader', MarketOffersWizard());
        await this.browser.yaWaitForHidden(MarketOffersWizard.productCardLoader());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('full', MarketOffersWizard());
    });

    it('Бесконечная карусель в дебрендированном колдунщике', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3854672498,
        }, MarketOffersWizard());

        await this.browser.yaMockXHR(mockOptions);
        await this.browser.assertView('plain', MarketOffersWizard());
        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('loader', MarketOffersWizard());

        // добавляем +100ms чтобы быть уверенными что ajax запрос отработал и контент отрисовался
        await this.browser.pause(ajaxDelay + 100);

        await this.browser.yaScrollContainer(MarketOffersWizard.productCardsShowcase.scroller.wrap(), 9999);
        await this.browser.assertView('after-load', MarketOffersWizard());
    });

    it('Подзаголовок из значения флага', async function() {
        const encodedSubitle = encodeURIComponent('Заголовок');
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: [
                `PRODUCTS_offers_wizard_subtitle=${encodedSubitle}`,
            ],
        }, MarketOffersWizard());

        const subtitle = await this.browser.getText(MarketOffersWizard.debrandingSubtitle());
        assert.strictEqual(subtitle, decodeURIComponent(encodedSubitle));
    });

    it('Внешний вид с увеличенными миниатюрами', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: [
                'PRODUCTS_with_enlarged_thumbs=1',
            ],
        }, MarketOffersWizard());

        await this.browser.assertView('with_enlarged_thumbs', MarketOffersWizard());
    });

    it('Зажигание заголовка, всегда и только его', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: [
                'PRODUCTS_title_only_hover=1',
            ],
        }, MarketOffersWizard());

        await this.browser.moveToObject(MarketOffersWizard.productCard.greenUrl.link());
        await this.browser.assertView('with_enlarged_thumbs', MarketOffersWizard.productCard());
    });

    it('Увеличение миниатюр при наведении на товар', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 3356472073,
            exp_flags: [
                'PRODUCTS_animate_thumbs_on_hover=1',
            ],
        }, MarketOffersWizard());

        await this.browser.moveToObject(MarketOffersWizard.productCard.greenUrl.link());
        await this.browser.assertView('with_enlarged_thumbs', MarketOffersWizard());
    });

    /**
     * Тест для врезки в правой колонке лежит здесь, в тестах для врезки в центральной колонке,
     * так как используется именно адаптер и компоненты этой врезки, а не MarketOffersWizardRightIncut.
     */
    it('Не рендерить сниппет при отсутствии контента внутри в правой колонке', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 2187942577,
        }, this.PO.content());
        const found = await this.browser.$$(MarketOffersWizard());
        assert.lengthOf(found, 0, 'Отрисовался сниппет врезки без предложений внутри');
    });

    // Ссылки не являются браузерозависимыми.
    hermione.only.in(['chrome-desktop']);
    describe('Проверка ссылки', () => {
        it('В кнопке «ещё»', async function() {
            const bro = this.browser;

            await bro.yaOpenSerp({
                foreverdata: 2284006376,
            }, MarketOffersWizard());

            const href = await bro.getAttribute(MarketOffersWizard.marketItemsContainer.scroller.moreItem.link(), 'href');
            assert.include(href, '/products/search', 'в ссылке некорректный путь');

            const { searchParams } = new URL(href);
            assert.isTrue(searchParams.has('rs'), 'в ссылке нет параметра rs');
            assert.isTrue(searchParams.has('parent-reqid'), 'в ссылке нет параметра parent-reqid');
            assert.strictEqual(searchParams.get('utm_source_service'), 'web', 'в ссылке нет параметра utm_source_service или у него неверное значение');
        });
    });
});
