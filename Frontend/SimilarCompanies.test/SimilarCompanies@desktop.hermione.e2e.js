'use strict';

const PO = require('./SimilarCompanies.page-object').desktop;

specs({
    feature: 'Одна организация',
    type: 'Похожие организации',
}, () => {
    beforeEach(async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result//similar/scroller/topic/topic',
            PO.oneOrg.similarCompanies(),
            ['/search/touch?text=кафе пушкин'],
            { srcskip: 'YABS_DISTR' },
        );
    });

    it('Наличие элементов', async function() {
        await this.browser.yaShouldBeVisible(PO.oneOrg.similarCompanies.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.oneOrg.similarCompanies.scroller.firstItem(), 'Нет карточек');
    });

    it('Открытие попапа организации', async function() {
        await this.browser.click(PO.oneOrg.similarCompanies.scroller.firstItem());
        await this.browser.yaWaitForVisible(PO.popup());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg());
    });
});
