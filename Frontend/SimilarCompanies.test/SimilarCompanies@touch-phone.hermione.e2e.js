'use strict';

const PO = require('./SimilarCompanies.page-object').touchPhone;

function hideZaloginPopup() {
    $('.zalogin-app').css('display', 'none');
}

specs({
    feature: 'Одна организация',
    type: 'Похожие организации',
}, () => {
    beforeEach(async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/similar/scroller/topic/topic',
            PO.oneOrg.similarCompanies(),
            ['/search/touch?text=кафе пушкин'],
            { srcskip: 'YABS_DISTR' },
        );
    });

    it('Наличие элементов', async function() {
        await this.browser.yaShouldBeVisible(PO.oneOrg.similarCompanies.title(), 'Нет тайтла');
        await this.browser.yaShouldBeVisible(PO.oneOrg.similarCompanies.scroller.firstItem(), 'Нет карточек');
    });

    it('Открытие оверлея организации', async function() {
        // Скрываем попап залогина чтобы кликнуть на организацию в списке
        await this.browser.execute(hideZaloginPopup);

        await this.browser.click(PO.oneOrg.similarCompanies.scroller.firstItem());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg());
        await this.browser.yaWaitForVisible(PO.overlayOneOrg.similarCompanies());
    });
});
