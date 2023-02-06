'use-strict';

const PO = require('../OrgContacts.page-object')('touch-phone');

specs('Контакты организации', function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'ikea москва 41 километр',
            data_filter: 'companies',
        }, PO.OrgContacts());
    });

    describe('Адрес', function() {
        it('Должен открыться сайдблок с картой при клике в адрес', async function() {
            await this.browser.yaCheckBaobabCounter(PO.OrgContacts.AddressItem.Link(), {
                path: '/$page/$main/$result/composite/contacts/address',
            });

            await this.browser.yaWaitForVisible(PO.overlay(), 'Не открылся сайдблок с картой');
        });
    });
});
