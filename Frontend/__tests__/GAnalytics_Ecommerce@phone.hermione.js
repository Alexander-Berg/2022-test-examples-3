const _ = require('lodash');
const PO = require('../../../../hermione/page-objects');

hermione.only.in('chrome-phone', 'Ускоряем браузеронезависимые тесты');
specs(
    {
        feature: 'GAnalytics Ecommerce',
    },
    () => {
        const product1 = {
            id: '3242424234',
            name: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
            price: 1790,
        };
        const addProduct1Command = {
            command: 'ec:addProduct',
            params: product1,
        };

        const product2 = {
            id: '3242424',
            name: 'Мультимедиа-платформа Яндекс.Станция, фиолетовая',
            price: 9990,
        };
        const addProduct2Command = {
            command: 'ec:addProduct',
            params: product2,
        };

        hermione.only.notIn('safari13');
        it('Отправка информации для инициализации', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=productspage%2Findex-server-for-screens.json&exp_flags=analytics-disabled=0&hermione_turbo_in_turbo=disable'
                ))
                .yaWaitForVisible(PO.pageJsInited())
                .yaCheckPostMessage({
                    action: 'init',
                    alias: 'ga',
                    namespace: 'YAnalytics',
                    params: {
                        cookieDomain: 'none',
                        id: 'UA-122962992-1',
                        provider: 'google-analytics-ecommerce',
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека просмотра списков товаров', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=productspage%2Findex-server-for-screens.json&exp_flags=analytics-disabled=0&hermione_turbo_in_turbo=disable'
                ))
                .yaWaitForVisible(PO.blocks.products.item())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            {
                                command: 'ec:addImpression',
                                params: product1,
                            },
                            {
                                command: 'ec:addImpression',
                                params: product2,
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'view_item_list',
                        },
                    },
                })
                .click(PO.blocks.products.item())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            addProduct1Command, {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'click',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'click_item',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека перехода в карточку товара по кнопке "Выбрать"', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=productspage%2Findex-server-for-screens.json&exp_flags=analytics-disabled=0&hermione_turbo_in_turbo=disable'
                ))
                .yaWaitForVisible(PO.blocks.products.item())
                .click(PO.blocks.products.item.footer.button())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            addProduct1Command, {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'click',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'click_item',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека add при переходе во внешнюю корзину со списка товаров', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '/turbo?text=mytoys.ru%2Fyandexturbocatalog%2F&category_id=26073&exp_flags=analytics-disabled=0&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS'
                ))
                // Клик открывает новую вкладку браузера и переключается на неё.
                .click(PO.blocks.productAddToExternalCart())
                // Возвращаемся к первой вкладке с нашими товарами.
                .switchTab()
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            {
                                command: 'ec:addProduct',
                                params: {
                                    id: '7319979',
                                    name: 'Мягкая игрушка подушка Кот Басик Budi Basa, 40 см',
                                    price: 1299,
                                },
                            },
                            {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'add',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'add_to_cart',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека просмотра списка товаров при догрузке страниц', function() {
            const commonTrack = {
                alias: 'ga',
                action: 'track',
                params: {
                    ecommerce: [
                        {
                            command: 'ec:addImpression',
                        },
                    ],
                    track: {
                        hitType: 'event',
                        eventCategory: 'ecommerce',
                        eventAction: 'view_item_list',
                    },
                },
            };

            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?text=kupicase.ru%2Fyandexturbocatalog%2F&srcrwr=SAAS%3Aman1-1464.search.yandex.net%3A17002%3A10000&patch=setTestingGAnalytics&exp_flags=analytics-disabled=0'
                ))
                .yaWaitForVisible(PO.blocks.products.item())
                .yaCheckPostMessage(commonTrack, { clear: true })
                .yaScrollPageToBottom()
                .yaWaitForVisible(PO.blocks.products.item.link() + '[data-page="1"]')
                .yaCheckPostMessage(commonTrack, { clear: true });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека со страницы информации о товаре', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=productpage/product-2.json&exp_flags=analytics-disabled=0'
                ))
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            addProduct2Command, {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'detail',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'view_item',
                        },
                    },
                })
                .click(PO.blocks.productAddToCart())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            addProduct2Command, {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'add',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'add_to_cart',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека add при автоматическом добавлении товара в корзину', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?text=https%3A%2F%2Foris-parquet.ru%2Fmarket%2Fgoods%2Fpaneli_mdf_vstavka&exp_flags=analytics-disabled=0&exp_flags=turboforms_endpoint=/&patch=setTestingGAnalytics&autoadd=norelocate'
                ))
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            {
                                command: 'ec:addProduct',
                                params: {
                                    id: '4348',
                                    name: 'Стеновые панели Finitura Dekor (Финитура Декор) 2D Панели МДФ + вставка (под покраску)',
                                    price: 4422,
                                    quantity: 1,
                                },
                            },
                            {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'add',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'add_to_cart',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка трека add при переходе во внешнюю корзину со страницы информации о товаре', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?text=https://mytoys.ru/product/7319979&exp_flags=analytics-disabled=0'
                ))
                // Клик открывает новую вкладку браузера и переключается на неё.
                .click(PO.blocks.productAddToExternalCart())
                // Возвращаемся к первой вкладке с нашими товарами.
                .switchTab()
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            {
                                command: 'ec:addProduct',
                                params: {
                                    id: '7319979',
                                    name: 'Мягкая игрушка подушка Кот Басик Budi Basa, 40 см',
                                    price: 1000,
                                },
                            },
                            {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'add',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'add_to_cart',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка треков чекаута', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=ecomcartpage/default.json&exp_flags=analytics-disabled=0;turboforms_endpoint=/'
                ))
                .yaWaitForVisible(PO.blocks.cartItem())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            _.merge({}, addProduct1Command, { params: { quantity: 1 } }), {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'checkout',
                                    step: 1,
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'checkout',
                        },
                    },
                })
                .click(PO.blocks.cart.confirm())
                .yaWaitForVisible('#form', 'Не открылось окно оформления заказа')
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            _.merge({}, addProduct1Command, { params: { quantity: 1 } }), {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'checkout',
                                    step: 2,
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'checkout',
                        },
                    },
                })
                .yaWatchInnerHeight(function() {
                    return this.yaIndexify(PO.blocks.radioGroup.radioItem())
                        .yaIndexify(PO.blocks.inputText())
                        .setValue(PO.orderForm.nameField.control(), 'test')
                        .setValue(PO.orderForm.phoneField.control(), '88001234567')
                        .setValue(PO.orderForm.emailField.control(), 'test@test.ru')
                        .setValue(PO.orderForm.address(), 'Адрес')
                        .click(PO.orderForm.submit());
                })
                .yaWaitForVisible(PO.blocks.trustIframe(), 'Не перешли на страницу оплат')
                .yaWaitForVisible(PO.blocks.turboStatusScreenOrderSuccessOnlinePaid(), 'Не появилось сообщение об успехе оформления заказа')
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            _.merge({}, addProduct1Command, { params: { quantity: 1 } }), {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'purchase',
                                    id: '1234',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'purchase',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка треков при удалении товара из корзины', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=ecomcartpage/default.json&exp_flags=analytics-disabled=0;turboforms_endpoint%3D%2F'
                ))
                .yaWaitForVisible(PO.blocks.cartItem())
                .click(PO.blocks.cartItem.remove())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            _.merge({}, addProduct1Command, { params: { quantity: 1 } }), {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'remove',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'remove_from_cart',
                        },
                    },
                });
        });

        hermione.only.notIn('safari13');
        it('Отправка треков изменения количества товаров', function() {
            return this.browser
                .then(getUrlWithResourceWatcher(
                    '?stub=ecomcartpage/default.json&exp_flags=analytics-disabled=0;turboforms_endpoint%3D%2F'
                ))
                .yaWaitForVisible(PO.blocks.cartItem())
                .click(PO.blocks.cartItem.amountPicker.incrButton())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            _.merge({}, addProduct1Command, { params: { quantity: 1 } }), {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'add',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'add_to_cart',
                        },
                    },
                })
                .click(PO.blocks.cartItem.amountPicker.decrButton())
                .yaCheckPostMessage({
                    alias: 'ga',
                    action: 'track',
                    params: {
                        ecommerce: [
                            _.merge({}, addProduct1Command, { params: { quantity: 1 } }), {
                                command: 'ec:setAction',
                                params: {
                                    ecAction: 'remove',
                                },
                            },
                        ],
                        track: {
                            hitType: 'event',
                            eventCategory: 'ecommerce',
                            eventAction: 'remove_from_cart',
                        },
                    },
                });
        });
    }
);

/**
 * Запросить страницу с перехватом запросов
 *
 * @param {string} url - адрес страницы
 * @returns {Function}
 */
function getUrlWithResourceWatcher(url) {
    return function() {
        return this
            .url(url)
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .waitForExist('#YAnalyticsFrame');
    };
}
