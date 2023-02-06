'use strict';

const { MarketCarousel } = require('../../../Market.test/Market.page-object');
const { productCard, productCard2 } = require('../../../../../components/ProductCard/ProductCard.test/ProductCard.page-object/index@common');

specs({
    feature: 'Товарная галерея',
}, () => {
    hermione.only.in(['chrome-desktop', 'iphone']);
    it('Доступность', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            exp_flags: 'a11y_validate=1',
            foreverdata: 2055513564,
        }, MarketCarousel());

        await browser.yaCheckElementA11y(MarketCarousel());
    });

    describe('С лейблом скидки', async function() {
        it('Базовые проверки', async function() {
            const { browser } = this;
            const platform = await browser.getMeta('platform');

            await browser.yaOpenSerp({
                text: ' ',
                foreverdata: '1677229915',
            }, MarketCarousel());
            platform !== 'desktop' && await browser.yaScroll(productCard());
            await browser.assertView('tg_with_discount_label', productCard());
        });

        it('С дисклеймером', async function() {
            const { browser } = this;
            const platform = await browser.getMeta('platform');

            await browser.yaOpenSerp({
                text: ' ',
                foreverdata: '2651060933',
            }, MarketCarousel());

            platform !== 'desktop' && await browser.yaScroll(productCard2());
            await browser.assertView('tg_with_discount_label_with_disclaimer_intersection', productCard2());
        });
    });

    hermione.only.in(['chrome-desktop', 'chrome-phone']);
    describe('CGI параметры для маркета', async function() {
        it('Маркетная ссылка', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({ text: 'foreverdata' }, this.PO.page());
            await browser.setCookie({ name: 'i', value: 'wow' });
            await browser.yaOpenSerp({
                foreverdata: '934258065',
            }, MarketCarousel());

            await browser.click(productCard.title.link());
            const href = await browser.getAttribute(productCard.title.link(), 'href');

            await assert.include(href, 'src_pof=', 'В ссылке отсутствует src_pof');
            await assert.include(href, 'icookie=', 'В ссылке отсутствует icookie');
            await assert.include(href, 'wprid=', 'В ссылке отсутствует wprid');
        });
        it('Не маркетная ссылка', async function() {
            const { browser } = this;

            await browser.yaOpenSerp({ text: 'foreverdata' }, this.PO.page());
            await browser.setCookie({ name: 'i', value: 'wow' });
            await browser.yaOpenSerp({
                foreverdata: '934258065',
            }, MarketCarousel());

            await browser.click(productCard2.title.link());
            const href = await browser.getAttribute(productCard2.title.link(), 'href');

            await assert.notInclude(href, 'src_pof=', 'В ссылке присутствует src_pof');
            await assert.notInclude(href, 'icookie=', 'В ссылке присутствует icookie');
            await assert.notInclude(href, 'wprid=', 'В ссылке присутствует wprid');
        });
    });
});
