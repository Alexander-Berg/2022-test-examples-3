'use strict';

const PO = require('./OrgPrices.page-object').touchPhone;

function hideZaloginPopup() {
    $('.zalogin-app').css('display', 'none');
}

specs({
    feature: 'Одна организация',
    type: 'Врезка с ценами',
}, function() {
    beforeEach(async function() {
        const fallbackUrl = '/search/touch?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/prices/title',
            PO.oneOrg.orgPrices(),
            fallbackUrl,
            { srcskip: 'YABS_DISTR' },
        );
    });

    it('Наличие элементов', async function() {
        await this.browser.yaShouldBeVisible(PO.oneOrg.orgPrices.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.oneOrg.orgPrices.list(), 'Нет прайсов');
    });

    it('Открытие оверлея организации', async function() {
        // Скрываем попап залогина чтобы кликнуть на организацию в списке
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.oneOrg.orgPrices.title());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg.tabPrices());
    });
});
