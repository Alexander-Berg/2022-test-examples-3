'use strict';

const PO = require('./OfferInfo.page-object');

specs({
    feature: 'Офферы',
    type: 'Офферное описание',
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: '4200449830',
            data_filter: 'offer_info',
            srcskip: 'YABS_DISTR',
        }, PO.serpItem());

        await this.browser.assertView('offer-info', PO.serpItem());
    });

    it('Оффер с тумбой', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: '1278455664',
            data_filter: 'offer_info',
            srcskip: 'YABS_DISTR',
        }, PO.serpItem());

        await this.browser.assertView('offer-thumb', PO.serpItem());
    });

    it('Собственная ссылка у тумбы', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            foreverdata: '1278455664',
            data_filter: 'offer_thumb',
            srcskip: 'YABS_DISTR',
        }, PO.serpItem());

        const thumbLink = await browser.getAttribute(PO.serpItem.thumb(), 'href');
        const titleLink = await browser.getAttribute(PO.serpItem.url(), 'href');

        assert.notEqual(thumbLink, titleLink);
    });
});
