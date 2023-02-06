'use strict';

const PO = require('./OrgPrices.page-object').desktop;

specs({
    feature: 'Одна организация',
    type: 'Врезка с ценами',
}, function() {
    beforeEach(async function() {
        const fallbackUrl = '/search?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result/composite/tabs/about/prices/title',
            PO.oneOrg.orgPrices(),
            fallbackUrl,
            { srcskip: 'YABS_DISTR' },
        );
    });

    it('Наличие элементов', async function() {
        await this.browser.yaShouldBeVisible(PO.oneOrg.orgPrices.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.oneOrg.orgPrices.list(), 'Нет прайсов');
    });

    it('Открытие попапа организации', async function() {
        await this.browser.click(PO.oneOrg.orgPrices.title());
        await this.browser.yaWaitForVisible(PO.popup());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg.tabPrices());
    });
});
