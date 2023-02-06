const URL = require('url');

specs({
    feature: 'Страница корзины',
}, () => {
    hermione.only.notIn('safari13');
    it('Переходы по истории', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm(), 'Не видно формы по клику на "Оформить заказ"')
            .back()
            .yaShouldBeVisible(PO.blocks.cart(), 'Не вернулись на корзину по браузерному "Назад"')
            .yaShouldNotBeVisible(PO.orderForm(), 'Не вернулись на корзину по браузерному "Назад"')
            .forward()
            .yaShouldBeVisible(PO.orderForm(), 'Не вернулись на форму по браузерному "Вперед"');
    });

    hermione.only.notIn('safari13');
    it('Валидация полей', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .assertView('plain', PO.page())
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .assertView('form-screen', PO.page())
            .click(PO.orderForm.submit())
            .assertView('form-errors', PO.page())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .yaWatchInnerHeight(function() {
                return this.setValue(PO.orderForm.nameField.control(), 'name')
                    .setValue(PO.orderForm.phoneField.control(), '88001234567')
                    .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
                    .click('.turbo-screen-title');
            })
            .click(PO.orderForm.payCashe())
            .click(PO.orderForm.deliveryByPickup())
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.statusModal(), 'Не прошла валидация при заполненных полях');
    });

    hermione.only.in('chrome-phone', 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Валидация телефона и почты', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .yaWatchInnerHeight(function() {
                return this.setValue(PO.orderForm.nameField.control(), 'name')
                    .setValue(PO.orderForm.emailField.control(), 'email@examplecom')
                    .click('.turbo-screen-title');
            })
            .click(PO.orderForm.payCashe())
            .click(PO.orderForm.deliveryByPickup())
            .yaWaitForHidden(PO.orderForm.phoneFieldError(), 'В поле ввода номера телефона не должно быть ошибки')
            .yaWaitForHidden(PO.orderForm.emailFieldError(), 'В поле ввода почты телефона не должно быть ошибки')
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.orderForm.phoneFieldError(), 'Валидация телефона не должна была пройти')
            .yaWaitForVisible(PO.orderForm.emailFieldError(), 'Валидация почты не должна была пройти');
    });

    hermione.only.notIn('safari13');
    it('Стоимость и способ доставки', function() {
        let orderButtonText;
        let summaryValue;

        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .getText(PO.cart.summary.value())
            .then(value => summaryValue = value)
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .getText(PO.orderForm.submit())
            .then(value => {
                orderButtonText = value;

                assert.notInclude(
                    value,
                    summaryValue,
                    'Доставка не учитывается при способе доставки "Курьер"'
                );
            })
            .click(PO.orderForm.deliveryByPickup())
            .getText(PO.orderForm.submit())
            .then(value => {
                assert.notEqual(
                    value,
                    orderButtonText,
                    'Не поменялась стоимость доставки после изменения способа на самовывоз'
                );
                assert.include(
                    value,
                    summaryValue,
                    'При самовывозе не бесплатная доставка'
                );
            })
            .click(PO.orderForm.deliveryByCourier2())
            .getText(PO.orderForm.submit())
            .then(value => {
                assert.notInclude(
                    value,
                    summaryValue,
                    'При доставке способом courier2 не прибавляется доставка'
                );
            })
            .click(PO.orderForm.deliveryByCourier())
            .getText(PO.orderForm.submit())
            .then(value => {
                assert.equal(
                    value,
                    orderButtonText,
                    'При возвращенни способа доставки на первый вариант - цена не такая, как изначально'
                );
            })
            .back()
            .click(PO.cart.list.firstItem.amountPicker.incrButton())
            .getText(PO.cart.summary.value())
            .then(value => summaryValue = value)
            .click(PO.blocks.turboButtonThemeBlue())
            .getText(PO.orderForm.submit())
            .then(value => assert.notInclude(
                value,
                summaryValue,
                'Указана стоимость без учета доставки'
            ))
            .click(PO.orderForm.deliveryByCourier2())
            .getText(PO.orderForm.submit())
            .then(value => assert.include(
                value,
                summaryValue,
                'При выборе условия, когда доставка беспланая, она все равно учитывается в общей стоимости'
            ));
    });

    hermione.only.notIn('safari13');
    it('Ошибка оформления', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'error')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .click(PO.orderForm.payCashe())
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderLoading(), 'Не показался статус-скрин со спинером')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderFail(), 'Не показался статус-скрин неудачного заказа')
            .click(PO.blocks.turboStatusScreenOrderFail.buttonClose())
            .yaWaitForHidden(PO.blocks.statusModal(), 'Не скрылся статус-скрин ошибки')
            .yaShouldBeVisible(PO.orderForm(), 'После закрытия статус-скрина ушли с формы');
    });

    hermione.only.notIn('safari13');
    it('Успешное оформление', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'test')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .click(PO.orderForm.payCashe())
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderLoading(), 'Не показался статус-скрин со спинером')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderSuccess(), 'Не показался статус-скрин успешного заказа')
            .assertView('success-cash', PO.blocks.statusModal())
            .back()
            .yaWaitForHidden(PO.blocks.statusModal(), 'Не скрылся статус-скрин успеха')
            .yaShouldNotBeVisible(PO.orderForm(), 'После закрытия статус-скрина не ушли с формы')
            .yaShouldBeVisible(PO.blocks.cart.empty(), 'Не перешли на страницу корзины')
            .forward()
            .yaShouldNotBeVisible(PO.orderForm(), 'После успешной отправки можем передти на форму')
            .yaShouldBeVisible(PO.blocks.cart.empty(), 'После успешной отправки можем передти на форму');
    });

    hermione.only.notIn('safari13');
    it('Ошибка получения ссылки на оплату', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'error')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.trustIframe(), 'Не перешли на страницу оплат')
            .yaShouldNotBeVisible(PO.orderForm(), 'Не перешли на страницу оплат')
            .yaAssertViewportView('trust-iframe-load')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderFail(), 'Не показался статус-скрин неудачного заказа')
            .click(PO.blocks.turboStatusScreenOrderFail.buttonClose())
            .yaShouldNotBeVisible(PO.blocks.trustIframe(), 'Не перешли обратно на форму')
            .yaShouldBeVisible(PO.orderForm(), 'Не перешли обратно на форму')
            .forward()
            .yaShouldNotBeVisible(PO.blocks.trustIframe(), 'Перешли не оплату через историю')
            .yaShouldBeVisible(PO.orderForm(), 'Перешли не оплату через историю');
    });

    hermione.only.notIn('safari13');
    it('Ошибка оплаты', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'error-pay')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.trustIframe(), 'Не перешли на страницу оплат')
            .yaShouldNotBeVisible(PO.orderForm(), 'Не перешли на страницу оплат')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderFail(), 'Не показался статус-скрин неудачного заказа')
            .click(PO.blocks.turboStatusScreenOrderFail.buttonClose())
            .yaShouldNotBeVisible(PO.blocks.trustIframe(), 'Не перешли обратно на форму')
            .yaShouldBeVisible(PO.orderForm(), 'Не перешли обратно на форму')
            .forward()
            .yaShouldNotBeVisible(PO.blocks.trustIframe(), 'Перешли на оплату через историю')
            .yaShouldBeVisible(PO.orderForm(), 'Перешли на оплату через историю');
    });

    hermione.only.notIn('safari13');
    it('Успех оплаты', function() {
        let host;

        return this.browser
            .timeouts('script', 3000)
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                [],
            ))
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'test')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.trustIframe(), 'Не перешли на страницу оплат')
            .yaShouldNotBeVisible(PO.orderForm(), 'Не перешли на страницу оплат')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderSuccessOnlinePaid(), 'Не показался экран успешного заказа')
            .assertView('success-online-paid', PO.blocks.statusModal())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(requests => {
                const lastRequest = JSON.parse(requests[requests.length - 1].text);

                assert.strictEqual(lastRequest.data, null, 'После успеха оплаты не почистили корзину');
            });
    });

    hermione.only.notIn('safari13');
    it('Заказ уже оформлен', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'already-sent')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.trustIframe(), 'Не перешли на страницу оплат')
            .yaShouldNotBeVisible(PO.orderForm(), 'Не перешли на страницу оплат')
            .yaWaitForVisible(
                PO.blocks.turboStatusScreenAlreadySent(),
                'Не показался статус-скрин уже оформленного заказа'
            );
    });

    hermione.only.notIn('safari13');
    it('Проверка отправки подписи', function() {
        let host;

        return this.browser
            .timeouts('script', 3000)
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                [
                    {
                        url: `https://${host}/submit/shopping-cart/`,
                        response: {
                            status: 'success',
                            cart: {
                                data: {
                                    items: [{
                                        count: 2,
                                        product: {
                                            description: 'GoPro',
                                            price: { current: '1790', currencyId: 'RUR' },
                                            href: '/turbo?stub=productpage/product-1.json&exp_flags=platform=touch',
                                            meta: 'CiBC2hxN7fyAthVvJbZoRD9TpcLvOSyQVG6cD9VjdhOODxKFAgj3wp3oBRL8AQoKMzI0MjQyNDIzNBI+R29Qcm8gbWluaVVTQiDQutCw0LHQtdC70Ywg0LTQu9GPINC/0L7QtNC60LsuINC6INCiViBBQ01QUy0zMDEaPy90dXJibz9zdHViPXByb2R1Y3RwYWdlL3Byb2R1Y3QtMS5qc29uJmV4cF9mbGFncz1wbGF0Zm9ybT10b3VjaCJeClZodHRwOi8vYXZhdGFycy1pbnQubWRzdC55YW5kZXgubmV0L2dldC10dXJiby81MTUwLzJhMDAwMDAxNjdmNDQwMzkwM2IzYjA0OTk5ZjRjNTY1YWFiMBDYBBjYBCoEMTc5MDoDUlVSQgI1JQ==',
                                            id: '3242424234',
                                            thumb: { src: '', height: 600, block: 'image', width: 600 },
                                        },
                                    }],
                                },
                                block: 'cart',
                            },
                        },
                    },
                    {
                        url: `https://${host}/submit/shopping-cart/final/`,
                        response: { status: 'success', delay: 3000, turbo_order_id: 1234 },
                    },
                ],
            ))
            .url('?text=oris-parquet.ru%2Fyandexturbocart%2F&srcrwr=SAAS%3Aman1-1464.search.yandex.net%3A17002%3A10000&patch=setActionKey&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.cartItem(), 'Корзина не загрузилась')
            .click(PO.blocks.cartItem.amountPicker.decrButton())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(checkSk)
            .click(PO.blocks.cart.confirm())
            .yaWaitForVisible('#form', 'Не открылось окно оформления заказа')
            .yaWatchInnerHeight(function() {
                return this.yaIndexify(PO.blocks.radioGroup.radioItem())
                    .yaIndexify(PO.blocks.inputText())
                    .setValue(PO.orderForm.nameField.control(), 'test')
                    .setValue(PO.orderForm.phoneField.control(), '88001234567')
                    .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
                    .setValue(PO.orderForm.address(), 'Адрес')
                    .click(PO.orderForm.submit());
            })
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/final/`)
            )
            .then(checkSk);
    });

    hermione.only.notIn('safari13');
    it('Post-message на загрузку товаров в корзине', function() {
        return this.browser
            .yaOpenInIframe('?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.cartItem())
            .frameParent()
            .yaCheckPostMessage({
                type: 'ecom-cart-loaded',
            });
    });

    hermione.only.notIn('safari13');
    it('При наличии товара под заказ прячем выбор способа оплаты, а в данных отправляем "наличными"', function() {
        let host;

        return this.browser
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                [
                    {
                        url: `https://${host}/multiple/submit/shopping-cart/final/`,
                        response: { status: 'success', delay: 3000, turbo_order_id: 1234 },
                    },
                ],
            ))
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=multiple/')
            .yaWaitForVisible(PO.blocks.cartItem(), 'Корзина не загрузилась')
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaShouldNotBeVisible(PO.orderForm.paymentMethod(), 'Выбор способа оплаты не должен быть виден')
            .yaWaitForVisible('#form', 'Не открылось окно оформления заказа')
            .yaWatchInnerHeight(function() {
                return this.yaIndexify(PO.blocks.radioGroup.radioItem())
                    .yaIndexify(PO.blocks.inputText())
                    .setValue(PO.orderForm.nameField.control(), 'test')
                    .setValue(PO.orderForm.phoneField.control(), '88001234567')
                    .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
                    .setValue(PO.orderForm.address(), 'Адрес')
                    .click(PO.orderForm.submit());
            })
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/multiple/submit/shopping-cart/final/`)
            )
            .then(requests => {
                assert.equal(requests.length === 1, true, 'Должен быть сделан один запрос отправки формы');

                const requestBody = JSON.parse(requests[0].text);

                assert.equal(requestBody.payment_method, 'cash', 'Способ оплаты должен быть наличными');
            });
    });

    describe('Расположение бейджа безопасности', function() {
        hermione.only.in('iphone', 'Ускоряем браузеронезависимые тесты');
        hermione.only.notIn('safari13');
        it('В корзине с товарами', function() {
            return this.browser
                .url('/turbo?stub=ecomcartpage/with-safe.json&exp_flags=turboforms_endpoint=/')
                .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
                .assertView('plain', [
                    PO.blocks.turboButtonThemeBlue(),
                    PO.ecomSecureTransactionNotice(),
                    PO.turboMeta(),
                ], {
                    ignoreElements: [
                        PO.blocks.turboButton(),
                        PO.ecomSecureTransactionNotice(),
                    ],
                })
                .yaCheckLink({
                    selector: PO.turboMeta.link(),
                    message: 'Неправильная ссылка с Яндекс.Маркет',
                    target: '_blank',
                    url: {
                        href: 'https://yandex.ru/legal/market_guarantee',
                    },
                })
                .click(PO.blocks.turboButtonThemeBlue())
                .yaWaitForVisible(PO.orderForm())
                .yaScrollPage(PO.ecomSecureTransactionNotice())
                .assertView('second-screen', [
                    PO.orderForm.submit(),
                    PO.ecomSecureTransactionNotice(),
                    PO.turboAgreement(),
                ], {
                    ignoreElements: [
                        PO.blocks.turboButton(),
                        PO.ecomSecureTransactionNotice(),
                        PO.turboAgreement(),
                    ],
                });
        });

        hermione.only.in('iphone', 'Ускоряем браузеронезависимые тесты');
        hermione.only.notIn('safari13');
        it('В корзине без товаров', function() {
            return this.browser
                .url('/turbo?stub=ecomcartpage/with-safe.json&exp_flags=turboforms_endpoint=/empty/')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .yaShouldNotBeVisible(PO.ecomSecureTransactionNotice());
        });
    });

    hermione.only.notIn('safari13');
    it('С минимальной стоимостью заказа', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/with-min-order-cost.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.turboButtonThemeBlueDisabled(), 'Корзина не загрузилась')
            .yaIndexify(PO.turboButtonThemeBlue())
            .assertView('disabledButton', [PO.turboButtonThemeBlueDisabled(), PO.ecomArrowLink()])
            .yaCheckLink({
                selector: PO.ecomArrowLink.link(),
                message: 'Неправильная ссылка на каталог',
                target: '_self',
                url: {
                    href: '/turbo?stub=productspage/index.json',
                    ignore: ['protocol', 'hostname'],
                },
            })
            .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.incrButton())
            .yaWaitForHidden(PO.turboButtonThemeBlueDisabled(), 'Кнопка заказа осталась неактивной')
            .yaShouldNotBeVisible(PO.ecomArrowLink(), 'Не пропала ссылка на каталог')
            .yaShouldBeVisible(PO.firstTurboButtonThemeBlue(), 'Не появилась активная кнопка');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с темой', function() {
        return this.browser
            .url('/turbo?stub=ecomcartpage/default.json&exp_flags=turboforms_endpoint=/&patch=setEcomTheme')
            .yaWaitForVisible(PO.blocks.turboButtonThemeBlue(), 'Нет кнопки "Оформить заказ"')
            .assertView('open-form-button', PO.blocks.turboButtonThemeBlue())
            .click(PO.blocks.turboButtonThemeBlue())
            .yaWaitForVisible(PO.orderForm())
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaIndexify(PO.blocks.inputText())
            .setValue(PO.orderForm.nameField.control(), 'test')
            .setValue(PO.orderForm.phoneField.control(), '88001234567')
            .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
            .setValue(PO.orderForm.address(), 'Адрес')
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .assertView('pay-button', PO.orderForm.submit())
            .click(PO.orderForm.payCashe())
            .assertView('pay-cache', PO.orderForm.submit())
            .click(PO.orderForm.payOnline())
            .click(PO.orderForm.submit())
            .yaWaitForVisible(PO.blocks.trustIframe(), 'Не перешли на страницу оплат')
            .yaShouldNotBeVisible(PO.orderForm(), 'Не перешли на страницу оплат')
            .yaWaitForVisible(PO.blocks.turboStatusScreenOrderSuccessOnlinePaid(), 'Не показался экран успешного заказа')
            .assertView('success-online-paid', PO.blocks.statusModal());
    });
});

function checkSk(requests) {
    assert.isOk(requests.length, 'Нет запросов к ручке корзины');

    requests.forEach(request => {
        const requestBody = JSON.parse(request.text);
        assert.isOk(requestBody.sk, `Запрос не содержит sk: ${request.url} ${request.text}`);
    });
}
