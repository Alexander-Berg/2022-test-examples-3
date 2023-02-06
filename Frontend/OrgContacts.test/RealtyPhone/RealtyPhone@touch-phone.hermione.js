'use-strict';

const PO = require('../OrgContacts.page-object')('touch-phone');

specs('Контакты организации', function() {
    describe('Кнопка телефона в недвижимости', function() {
        hermione.also.in(['iphone-dark']);
        it('Должен открыться попап после клика в номер телефона', async function() {
            await this.browser.yaOpenSerp({
                text: 'жк бунинские луга',
                data_filter: 'companies',
            }, PO.OrgContacts());

            await this.browser.yaCheckRealtyCounter({ expected: 0 });
            await this.browser.yaCheckBaobabCounter(PO.OrgContacts.Phone(), {
                path: '/$page/$main/$result/composite/contacts/realty/phone[@action = "phone"]',
                behaviour: { type: 'dynamic' },
            });
            await this.browser.yaWaitForVisible(PO.RealtyPopup());
            await this.browser.assertView('realty-popup', PO.RealtyPopup.Content());
            await this.browser.yaCheckRealtyCounter();
            await this.browser.click(PO.RealtyPopup.Button());
            await this.browser.yaCheckVacuum(
                { type: 'reach-goal', orgid: '109944240168', event: 'call', goal: 'make-call' },
                'Не сработала метрика на клик в кнопку телефон',
            );
        });
    });
});
