'use-strict';

const PO = require('../OrgContacts.page-object')('desktop');

specs('Контакты организации', function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'отель высоцкий',
            data_filter: 'companies',
            srcskip: 'YABS_DISTR',
        }, PO.OrgContacts());
    });

    describe('Адрес', function() {
        it('Должен открыться попап с открытой вкладкой "На карте"', async function() {
            await this.browser.yaCheckBaobabCounter(PO.OrgContacts.AddressItem.Link(), {
                path: '/$page/$parallel/$result/tabs/about/contacts/address',
            });

            await this.browser.yaWaitForVisible(this.PO.modalContent.map2(), 'Не открылся попап с картой');
        });

        it('Должна открыться вкладка "На карте" при клике в адрес в попапе', async function() {
            await this.browser.click(this.PO.oneOrg.header.title.link());
            await this.browser.yaWaitForVisible(PO.oneOrgModal.about.OrgContacts());

            await this.browser.yaCheckBaobabCounter(PO.oneOrgModal.about.OrgContacts.AddressItem.Link(), {
                path: '$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/contacts/address',
            });

            await this.browser.yaWaitForVisible(this.PO.modalContent.map2(), 'Не открылся таб с картой');
        });
    });
});
