'use-strict';

const POSimilar = require('../../../SimilarCompanies/SimilarCompanies.test/SimilarCompanies.page-object/index@desktop');
const PO = require('../OrgContacts.page-object')('desktop');

specs({
    feature: 'Одна организация',
    type: 'Контакты и телефон',
}, function() {
    it('Номер телефона', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.OrgContacts());

        const value = await browser.getText(PO.OrgContacts.Phone());
        assert.isTrue(/\+7 \(495\) 739-XX-XX\n? Показать/.test(value), 'Неправильный текст в кнопке номера телефона');
        await browser.yaCheckBaobabCounter(PO.OrgContacts.Phone(), {
            path: '/$page/$parallel/$result/composite/tabs/about/contacts/phones/phone[@action = "phone"]',
            behaviour: { type: 'dynamic' },
        });
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1018907821', event: 'call', goal: 'make-call' },
            'Ошибка счетчика Метрики после клика на телефон',
        );
        const value2 = await browser.getText(PO.OrgContacts.Phone());
        assert.equal(value2, '+7 (495) 739-00-33', 'Не показался полный номер телефона');
    });

    it('Длинный номер телефона', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: '',
            data_filter: 'companies',
            foreverdata: 618154855,
        }, PO.OrgContacts());

        await this.browser.assertView('plain', PO.OrgContacts());
    });

    it('Скрытие телефона в попапе', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            // запрос подобран так, чтобы не приходил колдунщик картинок, см http://st/SERP-91408
            text: 'нотариус резникова',
            data_filter: 'companies',
        }, PO.oneOrg());

        await browser.click(POSimilar.oneOrg.similarCompanies.scroller.thirdItem());
        await browser.yaWaitForVisible(PO.oneOrgModal(), 'Не открылся попап организации');

        await browser.assertView('phone-covered', PO.oneOrgModal.about.OrgContacts.PhoneItem.Link());

        await browser.yaCheckBaobabCounter(PO.oneOrgModal.about.OrgContacts.PhoneItem.Link(), {
            path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/contacts/phones/phone [@action = "phone"]',
            behaviour: { type: 'dynamic' },
        });

        await browser.assertView('phone-shown', PO.oneOrgModal.about.OrgContacts.PhoneItem());
    });
});
