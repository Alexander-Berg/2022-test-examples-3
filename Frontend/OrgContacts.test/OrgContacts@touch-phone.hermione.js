'use-strict';

const PO = require('./OrgContacts.page-object')('touch-phone');

specs({
    feature: 'Одна организация',
    experiment: 'Контакты',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.OrgContacts());
    });

    it('Внешний вид', async function() {
        await this.browser.assertView('plain', PO.OrgContacts());
    });

    it('Проверка счетчиков ссылок', async function() {
        await this.browser.yaCheckBaobabCounter(PO.OrgContacts.AddressItem.Link(), {
            path: '/$page/$main/$result/composite/contacts/address',
        });

        await this.browser.yaWaitForVisible(PO.overlay());
        await this.browser.click(PO.overlay.navBar.back());
        await this.browser.yaWaitForHidden(PO.overlay());
        await this.browser.yaWaitForVisible(PO.OrgContacts());

        await this.browser.yaCheckBaobabCounter(PO.OrgContacts.PhoneItem.Link(), {
            path: '/$page/$main/$result/composite/contacts/phones/phone[@action = "phone"]',
        });
    });
});
