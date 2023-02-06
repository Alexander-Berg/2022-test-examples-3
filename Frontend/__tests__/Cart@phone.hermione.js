specs({
    feature: 'Cart',
}, () => {
    describe('Данные с сервера (дефолт)', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .assertView('plain', PO.cart());
        });

        hermione.only.notIn('safari13');
        it('Внешний вид (кастомные цвета)', function() {
            return this.browser
                .url('/turbo?stub=cart/custom-theme.json&exp_flags=turboforms_endpoint=/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .assertView('plain', PO.cart());
        });

        hermione.only.notIn('safari13');
        it('Указывать суммарную стоимость у каждого товара с учетом количества', function() {
            return this.browser
                .url('/turbo?stub=cart/with-total-price-for-items.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .yaWaitUntil('firstItem цена не отобразилась', () =>
                    this.browser.getText(PO.cost())
                        .then(costs => costs[0] === '65 980 ₽')
                )
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.incrButton())
                .yaWaitUntil('firstItem цена не изменилась', () =>
                    this.browser.getText(PO.cost())
                        .then(costs => costs[0] === '98 970 ₽')
                );
        });

        it('Проверка ссылок', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .yaCheckLink({
                    selector: PO.cartItem.thumbLink(),
                    message: 'Неправильная ссылка на товар',
                    target: '_self',
                    url: {
                        href: '/turbo?stub=productpage/product-1-server-for-screens.json',
                        ignore: ['protocol', 'hostname'],
                    },
                })
                .yaCheckLink({
                    selector: PO.cartItem.description(),
                    message: 'Неправильная ссылка на товар',
                    target: '_self',
                    url: {
                        href: '/turbo?stub=productpage/product-1-server-for-screens.json',
                        ignore: ['protocol', 'hostname'],
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Пустые данные', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/empty/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .assertView('plain', PO.cart())
                .yaCheckLink({
                    selector: 'a',
                    message: 'Неправильная ссылка на каталог',
                    target: '_self',
                    url: {
                        href: '/turbo?exp_flags=platform%3Dtouch&stub=productspage/index-server-for-screens.json',
                        ignore: ['protocol', 'hostname'],
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Удаление элементов', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .click(PO.cart.list.secondItem.remove())
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 2, 'Товар из корзины не удалился');
                })
                .click(PO.cart.list.firstItem.remove())
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 1, 'Товар из корзины не удалился');
                })
                .click(PO.cart.list.firstItem.remove())
                .yaShouldBeVisible(PO.cart.empty())
                .yaShouldNotBeVisible(PO.cart.list());
        });

        hermione.only.notIn('safari13');
        it('Удаление элементов через инпут (количество = 0)', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .setValue(PO.cart.list.thirdItem.amountPicker.input(), 0)
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 2, 'Товар из корзины не удалился');
                })
                .setValue(PO.cart.list.secondItem.amountPicker.input(), 0)
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 1, 'Товар из корзины не удалился');
                })
                .setValue(PO.cart.list.firstItem.amountPicker.input(), 0)
                .yaShouldBeVisible(PO.cart.empty())
                .yaShouldNotBeVisible(PO.cart.list());
        });

        hermione.only.notIn('safari13');
        it('Удаление элементов переключением количества до 0', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .yaClickButtonIfEnabled(PO.cart.list.thirdItem.amountPicker.decrButton())
                .yaClickButtonIfEnabled(PO.cart.list.thirdItem.amountPicker.decrButton())
                .yaClickButtonIfEnabled(PO.cart.list.thirdItem.amountPicker.decrButton())
                .yaClickButtonIfEnabled(PO.cart.list.thirdItem.amountPicker.decrButton())
                .yaClickButtonIfEnabled(PO.cart.list.thirdItem.amountPicker.decrButton())
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 2, 'Товар из корзины не удалился');
                })
                .yaClickButtonIfEnabled(PO.cart.list.secondItem.amountPicker.decrButton())
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 1, 'Товар из корзины не удалился');
                })
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.decrButton())
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.decrButton())
                .yaShouldBeVisible(PO.cart.empty())
                .yaWaitForHidden(PO.cart.list());
        });

        hermione.only.notIn('safari13');
        it('Переключение количества', function() {
            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.decrButton())
                .yaWaitUntil('firstItem количество не изменилось', ()=>
                    this.browser.getValue(PO.cart.list.firstItem.amountPicker.input())
                        .then(data=>data === '1'))
                .yaClickButtonIfEnabled(PO.cart.list.secondItem.amountPicker.incrButton())
                .yaWaitUntil('firstItem количество не изменилось', ()=>
                    this.browser.getValue(PO.cart.list.secondItem.amountPicker.input())
                        .then(data=>data === '2'))
                .yaClickButtonIfEnabled(PO.cart.list.secondItem.amountPicker.incrButton())
                .yaWaitUntil('secondItem количество не изменилось', ()=>
                    this.browser.getValue(PO.cart.list.secondItem.amountPicker.input())
                        .then(data=>data === '3'))
                .yaClickButtonIfEnabled(PO.cart.list.thirdItem.amountPicker.incrButton())
                .yaWaitUntil('thirdItem количество не изменилось', ()=>
                    this.browser.getValue(PO.cart.list.thirdItem.amountPicker.input())
                        .then(data=>data === '6'));
        });

        hermione.only.notIn('safari13');
        it('Переключение количества через инпут', function() {
            const backspace = '\uE003';

            return this.browser
                .url('/turbo?stub=cart/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
                .setValue(PO.cart.list.thirdItem.amountPicker.input(), backspace)
                .assertView('clear', PO.cart.list.thirdItem.amountPicker())
                .click(PO.cart.summary()) // просто клик куда-нибудь, расфокус
                .assertView('blur', PO.cart.list.thirdItem.amountPicker());
        });

        hermione.only.notIn('safari13');
        it('Рекомендованные товары', async function() {
            const bro = this.browser;

            await bro.url('/turbo?text=farkop.ru/yandexturbocart/&exp_flags=turboforms_endpoint=/multiple/delay-3000/&patch=ecomCartRecommends');
            await bro.yaShouldNotBeVisible(PO.cart.list(), 'Товары корзины уже появились на странице');
            await bro.yaIndexify(PO.blocks.productsCarousel());
            await bro.yaShouldNotBeVisible(PO.blocks.firstProductsCarousel(), 'Рекомендации появились раньше товаров в корзине');
            await bro.yaShouldNotBeVisible(PO.blocks.secondProductsCarousel(), 'Рекомендации появились раньше товаров в корзине');
            await bro.yaWaitForVisible(PO.blocks.productsCarousel(), 7000, 'Рекомендации не появились');
            const { value: carousels } = await bro.elements(PO.blocks.productsCarousel());
            assert.lengthOf(carousels, 2, 'На странице не две карусели рекомендаций');
        });
    });

    describe('Данные из localstorage', function() {
        hermione.only.notIn('safari13');
        it('data=null удаляет данные из localStorage', function() {
            return this.browser
                .url('/')
                .execute(function() {
                    localStorage.setItem(
                        'turbo-ecomm-mvideo',
                        JSON.stringify({
                            shopId: 'mvideo',
                            loading: false,
                            items: [{
                                product: {
                                    id: '1',
                                    description: '',
                                    price: {
                                        current: 1000,
                                    },
                                    thumb: {
                                        src: 'src',
                                        height: 10,
                                        width: 10,
                                    },
                                    meta: 'meta_product',
                                },
                                count: 1,
                                meta: 'meta_item',
                            }],
                            catalogueUrl: '/turbo?exp_flags=platform%3Dtouch&stub=productspage%2Findex.json',
                            etag: 0,
                            summary: {
                                count: 12,
                                cost: 10000,
                            },
                        }));
                })
                .url('/turbo?stub=cart/ls-clear-data.json')
                .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
                .execute(function() {
                    return localStorage.getItem('turbo-ecomm-mvideo');
                })
                .then(({ value }) => assert.equal(
                    value,
                    '{"shopId":"mvideo","loading":false,"catalogueUrl":"/turbo?exp_flags=platform%3Dtouch&stub=productspage%2Findex.json","items":[],"etag":0,"summary":{"count":0,"cost":0,"costWithDelivery":0}}',
                    'Товары не обнулились'
                ));
        });

        hermione.only.notIn('safari13');
        it('Проверка работы', function() {
            return this.browser
                .url('/turbo?stub=cart/ls-set-data.json')
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 3, 'Изначально на странице не 3 товара');
                })
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.incrButton())
                .click(PO.cart.list.secondItem.remove())
                .url('/turbo?stub=cart/ls-read-data.json')
                .yaWaitForVisible(PO.cart.list(), 'Не загрузились данные')
                .yaWaitUntil('firstItem не сохранилось увеличение количества', ()=>
                    this.browser.getValue(PO.cart.list.firstItem.amountPicker.input())
                        .then(data=>data === '3'))
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 2, 'Не сохранилось удаление товара');
                })
                .yaClickButtonIfEnabled(PO.cart.list.secondItem.amountPicker.decrButton())
                .refresh()
                .yaWaitForVisible(PO.cart.list(), 'Не загрузились данные')
                .yaWaitUntil('secondItem не сохранилось уменьшение количества', ()=>
                    this.browser.getValue(PO.cart.list.secondItem.amountPicker.input())
                        .then(data=>data === '4'))
                .click(PO.cart.list.firstItem.remove())
                .click(PO.cart.list.firstItem.remove())
                .refresh()
                .yaWaitForVisible(PO.cart.empty(), 'Корзина должна быть пустой');
        });

        hermione.only.notIn('safari13');
        it('Поддержка нескольких shopId', function() {
            return this.browser
                .url('/turbo?stub=cart/ls-set-data.json')
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 3, 'На первой странице не 3 товара');
                })
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.incrButton())
                .url('/turbo?stub=cart/ls-set-data-another-shopid.json')
                .yaClickButtonIfEnabled(PO.cart.list.firstItem.amountPicker.incrButton())
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 2, 'На второй странице не 2 товара');
                })
                .url('/turbo?stub=cart/ls-read-data.json')
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 3, 'Для первого магазина сохранена не его корзина');
                })
                .getValue(PO.cart.list.firstItem.amountPicker.input())
                .then(data => {
                    assert.equal(data, '3', 'Количество товара из заказа с первого магазина неверное');
                })
                .assertView('from-storage', PO.cart())
                .url('/turbo?stub=cart/ls-read-data-another-shopid.json')
                .elements(PO.cartItem())
                .then(({ value }) => {
                    assert.lengthOf(value, 2, 'Для второго магазина сохранена не его корзина');
                })
                .getValue(PO.cart.list.firstItem.amountPicker.input())
                .then(data => {
                    assert.equal(data, '100', 'Количество товара из заказа со второго магазина неверное');
                })
                .assertView('from-storage-2', PO.cart());
        });
    });

    hermione.only.notIn('safari13');
    it('Блокировка оплаты онлайн при сумме за товары более 150к рублей', function() {
        return this.browser
            .url('?text=ymturbo.t-dir.com/yandexturbocart/&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
            .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
            .setValue(PO.cart.list.firstItem.amountPicker.input(), '84')
            .click(PO.cart.confirm())
            .yaWaitForVisible(PO.orderForm(), 'Форма оформления заказа не появилась')
            // Вручную подскроливаем к элементу, иначе он чаще всего скриншотится обрезанным.
            .yaScrollPage(PO.orderForm.paymentMethod())
            .yaWaitForVisible(PO.orderForm.paymentMethod.grayText(), 'Предупреждение не появилось')
            .assertView('products-amount-online-disabled', PO.orderForm.paymentMethod())
            // Закрываем форму оформления заказа, чтобы сократить количество единиц товара
            // и проверить, что радио-кнопка выбора онлайн-оплаты станет доступна к выбору.
            .back()
            .click(PO.cart.list.firstItem.amountPicker.decrButton())
            .click(PO.cart.confirm())
            .yaWaitForHidden(PO.orderForm.paymentMethod.grayText(), 'Предупреждение не исчезло')
            .yaWaitForVisible(PO.orderForm(), 'Форма оформления заказа не появилась')
            // Задержка, чтобы дождаться уменьшения высоты захватываемого элемента после скрытия предупреждения,
            // иначе гермиона по старой памяти скриншотит область крупнее и в неё попадают лишние соседние элементы.
            .pause(600)
            .assertView('products-amount-online-enabled', PO.orderForm.paymentMethod());
    });

    hermione.only.notIn('safari13');
    it('Блокировка оплаты онлайн при сумме заказа с доставкой более 150к рублей', function() {
        return this.browser
            .url('?text=ymturbo.t-dir.com/yandexturbocart/&exp_flags=turboforms_endpoint=state-1/')
            .yaIndexify(PO.blocks.radioGroup.radioItem())
            .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
            .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
            .click(PO.cart.confirm())
            .yaWaitForVisible(PO.orderForm(), 'Форма оформления заказа не появилась')
            .yaWaitForVisible(PO.orderForm.paymentMethod.grayText(), 'Предупреждение не появилось')
            // Вручную подскроливаем к элементу, иначе он чаще всего скриншотится обрезанным.
            .yaScrollPage(PO.orderForm.paymentMethod())
            .assertView('delivery-amount-online-disabled', PO.orderForm.paymentMethod())
            // Меняем доставку на бесплатную при заказе от 5к.
            .click(PO.orderForm.deliveryByCourier2())
            .yaWaitForHidden(PO.orderForm.paymentMethod.grayText(), 'Предупреждение не исчезло')
            .assertView('delivery-amount-online-enabled', PO.orderForm.paymentMethod());
    });

    hermione.only.notIn('safari13');
    it('Обновление корзины в мультипейдже', function() {
        return this.browser
            .yaOpenInIframe('?stub=cart/default.json&fallback=1&check_swipe=1&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
            .yaShouldBeVisible(PO.cart.spinner())
            .execute(() => {
                // Эмулируем отправку сообщения от оверлея
                window.postMessage(JSON.stringify({ action: 'overlay-slider-visible' }), '*');
            })
            .yaWaitForHidden(PO.cart.spinner(), 'Корзина не загрузилась')
            .yaShouldBeVisible(PO.cart.list());
    });
});
