specs({
    feature: 'Form2',
    type: 'LayoutEcommerce',
}, () => {
    hermione.only.notIn('safari13');
    it('Сохранение полей в LocalStorage', function() {
        return this.browser
            .url('/turbo?stub=form2/ecom-layout-form.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.inputText())
            .yaIndexify(PO.radioGroup.radioItem())
            .setValue(PO.orderForm.nameField.control(), 'MrFreeman')
            .setValue(PO.orderForm.phoneField.control(), '88888888888')
            .setValue(PO.orderForm.emailField.control(), 'freeman@yandex.ru')
            .click(PO.orderForm.deliveryByCourier2())
            .click(PO.orderForm.deliveryByPickup())
            .execute(function() {
                return localStorage.getItem('turbo-ecomm---pay');
            })
            .then(({ value }) => assert.equal(
                value,
                '{"name":{"value":"MrFreeman","required":true},"customer_phone":{"value":"88888888888","required":true},"customer_email":{"value":"freeman@yandex.ru","required":true},"delivery":{"value":"pickup_1","checked":true,"required":false},"payment_method":{"value":"cash","checked":true,"required":false},"undefined":{}}',
                'Поля не сохранились'
            ))
            .refresh()
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.inputText())
            .yaIndexify(PO.radioGroup.radioItem())
            .getValue(PO.orderForm.nameField.control())
            .then(value => {
                assert.equal(value, 'MrFreeman', 'Не сохранилось имя');
            });
    });

    hermione.only.notIn('safari13');
    it('Не автозаполняем радиогруппу, если в ls лежит несуществующее значение', function() {
        return this.browser
            .url('/')
            .execute(function() {
                localStorage.setItem('turbo-ecomm---pay', JSON.stringify({
                    name: { value: 'И И', required: true },
                    customer_phone: { value: '79270000001', required: true },
                    customer_email: { value: 'ok@yandex.ru', required: true },
                    delivery: { value: 'courier_0', checked: false },
                    payment_method: { value: 'online', checked: true, required: false },
                }));
            })
            .url('?stub=form2/ecom-layout-form.json')
            .yaWaitForVisible(PO.page())
            .yaIndexify(PO.radioGroup())
            .getValue(PO.firstRadioGroup.radioItem.inputChecked())
            .then(value => {
                assert.equal(value, 'pickup_0', 'Выбран неправильный способ доставки');
            })
            .execute(function() {
                return localStorage.getItem('turbo-ecomm---pay');
            })
            .then(({ value }) => {
                const lsData = JSON.parse(value);

                assert.equal(lsData.delivery.value, 'pickup_0', 'В LS записан неправильный способ доставки');
            });
    });

    hermione.only.notIn('safari13');
    it('С кастомизацией', function() {
        return this.browser
            .url('?stub=form2/ecom-layout-form-custom-theme.json')
            .yaWaitForVisible(PO.page())
            .yaIndexify(PO.radioGroup())
            .assertView('radio', PO.firstRadioGroup())
            .assertView('button', PO.blocks.turboButtonThemeBlue());
    });
});
