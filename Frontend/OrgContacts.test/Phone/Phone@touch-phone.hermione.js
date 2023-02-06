'use-strict';

const PO = require('../OrgContacts.page-object')('touch-phone');

specs({
    feature: 'Одна организация',
    type: 'Контакты и телефон',
}, function() {
    it('Номер телефона', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'ikea москва 41 километр',
            data_filter: 'companies',
        }, PO.OrgContacts());
        const value = await browser.getText(PO.OrgContacts.PhoneItem());
        assert.isTrue(
            new Set([
                '+7 (495) 737-30-07\n8 (800) 234-55-66\n+7 (495) 221-33-11\nЕщё 1',
                '8 (800) 234-55-66\n+7 (495) 737-30-07\n+7 (495) 221-55-66\nЕщё 1',
            ]).has(value),
            `Неправильный текст в кнопке номера телефона (${value})`,
        );
        await browser.yaCheckBaobabCounter(PO.OrgContacts.PhoneItem.More(), {
            path: '/$page/$main/$result/composite/contacts/phones/more',
            behaviour: { type: 'dynamic' },
        });
        const value2 = await browser.getText(PO.OrgContacts.PhoneItem());
        const phones = new Set(value2.split('\n'));
        assert.isTrue(
            ['8 (800) 234-55-66', '+7 (495) 737-30-07', '+7 (495) 221-55-66', '+7 (495) 221-33-11'].every(phone => phones.has(phone)),
            `Не показался полный список номеров телефонов (${value2})`,
        );

        // Клик по ссылке с телефоном вызывает экран вызова абонента в searchapp,
        // поэтому превентим дефолтное поведение
        await browser.execute(function(selector) {
            document.querySelector(selector).onclick = function(e) {
                e.preventDefault();
            };
        }, PO.OrgContacts.Phone());

        await browser.yaCheckBaobabCounter(PO.OrgContacts.Phone(), {
            path: '/$page/$main/$result/composite/contacts/phones/phone[@action="phone" and @behaviour@type="dynamic"]',
        });
        await browser.yaCheckVacuum(
            { type: 'reach-goal', orgid: '1063375283', event: 'call', goal: 'make-call' },
            'Ошибка счетчика Метрики после клика на телефон',
        );
    });
});
