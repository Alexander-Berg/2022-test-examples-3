'use strict';

const PO = require('../OrgActions.page-object').desktop;

specs({
    feature: 'Колдунщик 1орг',
    type: 'Кнопка телефона недвижимости',
}, function() {
    it('Проверка попапа', async function() {
        await this.browser.yaOpenSerp({ text: 'ЖК бунинские луга', data_filter: 'companies' }, PO.oneOrg());
        await this.browser.assertView('realty-phone-button', PO.oneOrg.buttons.realty());
        await this.browser.yaCheckRealtyCounter({ expected: 0 });
        await this.browser.click(PO.oneOrg.buttons.realty());
        await this.browser.yaWaitForVisible(PO.realtyPopup());
        await this.browser.yaCheckRealtyCounter();
    });
});
