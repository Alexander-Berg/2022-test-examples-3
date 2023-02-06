const PO = require('./VisitsHistogram.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Гистограмма посещаемости',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies"]//visits-histogram',
            PO.oneOrg.visitsHistogram(),
            [
                '/search/?text=трц европолис спб&lr=213',
                '/search/?text=трц европейский&lr=213',
                '/search/?text=кафе пушкин&lr=213',
            ],
        );
    });

    it('Проверка в гистограммы на морде', async function() {
        await this.browser.yaShouldBeVisible(PO.oneOrg.visitsHistogram());
    });

    it('Проверка в гистограммы в попапе', async function() {
        await this.browser.click(PO.oneOrg.orgPrices.title());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg(), 'Попап не открылся');
        await this.browser.click(PO.popup.oneOrg.tabsMenu.firstItem());
        await this.browser.yaWaitForVisible(PO.popup.oneOrg.visitsHistogram(), 'Нет гистограмы');
    });
});
