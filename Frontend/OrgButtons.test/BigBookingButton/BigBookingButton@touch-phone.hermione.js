'use strict';

const PO = require('../OrgButtons.page-object')('touch-phone');

specs('Кнопка Выбрать Номер', () => {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'мета москва',
            data_filter: 'companies',
        }, PO.oneOrg());
    });

    it('Внешний вид', async function() {
        await this.browser.yaWaitForVisible(PO.oneOrg.wideBookingButton());
        await this.browser.assertView('plain', PO.oneOrg.wideBookingButton());
    });
});
