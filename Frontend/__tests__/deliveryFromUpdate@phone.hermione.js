const LS_KEY = 'turbo-app-ecom--spideradio.github.io';
const URL = require('url');

const __ym = {
    turbo_page: 1,
    doc_ui: 'touch-phone',
};

describe('Ecom-tap', function() {
    describe('Доставки по ajax', function() {
        it('Успешная загрузка', async function() {
            let host;
            const browser = this.browser;

            await browser
                .url('/')
                .url().then(res => {
                    const url = URL.parse(res.value);
                    host = url.hostname;
                })
                .then(() => this.browser.yaStartResourceWatcher(
                    '/static/turbo/hermione/mock-external-resources.sw.js',
                    [],
                ));

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: ['withDynamicDeliverySupport', 'setRegion'],
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'market-delivery-candidate',
            });
            await browser.yaWaitForVisible('.CartForm-RadioItem_type_delivery');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'checkout-delivery-market',
                params: { ecom_spa: 1, __ym, market_delivery_status: 'success' },
            });

            await browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/update/`)
                .then(requests => {
                    const requestData = JSON.parse(requests[0].text);

                    assert.isTrue(requestData.sk && requestData.sk !== '', 'Нет sk в запросе к update');
                });

            await browser.yaMockImages();
            await browser.assertView('plain', '.CartForm-RadioGroup');

            await browser.yaShouldBeVisible(
                '.ShippingAddressFields',
                'Не видно поле адреса при доставке type=courier'
            );

            await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            // заполняем адрес доставки
            await browser.yaMockFetch({
                urlDataMap: {
                    'address/': JSON.stringify({
                        address: { id: 'randomId' },
                        status: 'success',
                    }),
                },
            });
            await browser.setValue('input[name="locality"]', 'москв');
            await browser.yaScrollPage('.Suggest-Item', 0.3);
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="street"]', 'street');
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="building"]', '22');

            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');
            await browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/final/`)
                .then(requests => {
                    const requestData = JSON.parse(requests[0].text);
                    const deliveryData = requestData.delivery;
                    const expectedData = {
                        buyerCurrency: 'RUR',
                        deliveryOption: {
                            dates: {
                                toDate: '22-09-2020',
                                fromDate: '22-09-2020',
                                relativeFromDate: 5,
                                relativeToDate: 5,
                            },
                            deliveryServiceId: 99,
                            type: 'DELIVERY',
                            buyerPrice: 299,
                            serviceName: 'Курьер',
                            deliveryOptionId: '10460423_DELIVERY_Курьер_22-09-2020_noreserve_22-09-2020_null_8_99_SHOP',
                        },
                    };

                    assert.isTrue(requestData.sk && requestData.sk !== '', 'Нет sk в запросе к update');
                    assert.deepEqual(deliveryData, expectedData);
                });
        });

        it('Дефолтное значение пункта на карте из ls', async function() {
            let host;
            const browser = this.browser;

            await browser
                .url('/')
                .url().then(res => {
                    const url = URL.parse(res.value);
                    host = url.hostname;
                })
                .then(() => this.browser.yaStartResourceWatcher(
                    '/static/turbo/hermione/mock-external-resources.sw.js',
                    [],
                ));

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');

            await browser.execute(function(LS_KEY) {
                localStorage.setItem(LS_KEY, JSON.stringify({
                    delivery: {
                        selectedPointId: '202267532',
                        selectedOptionId: '10460423_PICKUP_Самовывоз_21-09-2020_noreserve_21-09-2020_null_8_99_SHOP',
                        type: 'market-pickup',
                    },
                }));
            }, LS_KEY);

            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaWaitForHidden('.CartForm-RadioGroup .Spin');
            await browser.yaShouldBeVisible('.CartForm-RadioItem_type_delivery');
            await browser.yaMockImages();
            await browser.yaShouldBeVisible(
                '.CartForm-RadioItem:nth-child(2) .RadioItemDeliveryMap_active',
                'Сохраненный в LS пункт не выбран'
            );
            await browser.assertView('active', '.CartForm-RadioItem:nth-child(2)');
            await browser.yaShouldNotBeVisible(
                '.Textarea-Control[name=shipping_address]',
                'Видно поле адреса при способе доставки pickup'
            );

            await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');
            await browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/final/`)
                .then(requests => {
                    const deliveryData = JSON.parse(requests[0].text).delivery;
                    const expectedData = {
                        buyerCurrency: 'RUR',
                        deliveryOption: {
                            dates: {
                                toDate: '21-09-2020',
                                fromDate: '21-09-2020',
                                relativeFromDate: 4,
                                relativeToDate: 4,
                            },
                            deliveryServiceId: 99,
                            type: 'PICKUP',
                            buyerPrice: 0,
                            outlets: [{
                                serviceName: 'Собственная служба',
                                coords: { latitude: '55.733548', longitude: '37.59083' },
                                name: 'Точка раз',
                                address: { fullAddress: 'Москва, Фрунзе, д. 10' },
                                id: '202267532',
                                workingTime: [{
                                    fromTime: '09:00',
                                    daysTo: '7',
                                    daysFrom: '1',
                                    toTime: '18:00',
                                }],
                            }, {
                                serviceName: 'Собственная служба',
                                coords: { latitude: '55.497954', longitude: '37.591306' },
                                name: 'В точку!',
                                address: { fullAddress: 'Москва, Гагарина, д. 1' },
                                id: '206911018',
                                workingTime: [{
                                    fromTime: '00:00',
                                    daysTo: '5',
                                    daysFrom: '1',
                                    toTime: '24:00',
                                }],
                            }],
                            serviceName: 'Самовывоз',
                            deliveryOptionId: '10460423_PICKUP_Самовывоз_21-09-2020_noreserve_21-09-2020_null_8_99_SHOP',
                        },
                        selectedPointId: '202267532',
                    };

                    assert.deepEqual(deliveryData, expectedData);
                });
        });

        it('Внешний вид неактивного выбранного пункта выдачи', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');

            await browser.execute(function(LS_KEY) {
                localStorage.setItem(LS_KEY, JSON.stringify({
                    delivery: {
                        selectedPointId: '202267532',
                        selectedOptionId: '10460423_PICKUP_Самовывоз_21-09-2020_noreserve_21-09-2020_null_8_99_SHOP',
                        type: 'market-pickup',
                    },
                }));
            }, LS_KEY);

            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaWaitForHidden('.CartForm-RadioGroup .Spin');
            await browser.yaShouldBeVisible('.CartForm-RadioItem_type_delivery');
            await browser.yaShouldBeVisible(
                '.CartForm-RadioItem:nth-child(2) .RadioItemDeliveryMap_active',
                'Сохраненный в LS пункт не выбран'
            );

            await browser.click('.CartForm-RadioItem:nth-child(1)');

            await browser.yaScrollPage('.ShippingAddressFields', 0.3);
            await browser.yaShouldBeVisible(
                '.ShippingAddressFields',
                'Не видно поле адреса при доставке type=courier'
            );

            await browser.assertView('not-active', '.CartForm-RadioItem:nth-child(2)');
        });

        it('Доставки только с картой', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/pickup-delivery/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaWaitForHidden('.CartForm-RadioGroup .Spin');
            await browser.yaShouldBeVisible('.CartForm-RadioGroup');
            await browser.yaShouldNotBeVisible(
                '.CartForm-RadioItem:nth-child(1) .RadioItemDeliveryMap_active',
                'Выбран пункт самовывоза картой'
            );

            await browser.yaShouldNotBeVisible(
                '.ShippingAddressFields',
                'Не видно поле адреса при доставке type=courier'
            );

            await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            await browser.click('.CartForm-SubmitButton');

            await browser.pause(500);

            await browser.yaShouldNotBeVisible('.EcomOrderSuccess', 'Форма отправилась без обязательного поля');
        });

        it('Пустые доставки', async function() {
            let host;
            const browser = this.browser;

            await browser
                .url('/')
                .url().then(res => {
                    const url = URL.parse(res.value);
                    host = url.hostname;
                })
                .then(() => this.browser.yaStartResourceWatcher(
                    '/static/turbo/hermione/mock-external-resources.sw.js',
                    [],
                ));

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/empty-delivery/delay-3000/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: ['withDynamicDeliverySupport', 'setRegion'],
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaShouldBeVisible('.CartForm-RadioGroup .Spin');
            await browser.assertView('loading', '.CartForm-RadioGroup');
            await browser.yaWaitForHidden('.CartForm-RadioItem');
            await browser.yaWaitForVisible('.GeoSuggest-Error');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'checkout-delivery-market',
                params: { ecom_spa: 1, __ym, market_delivery_status: 'no_delivery_to_city' },
            });
            await browser.yaShouldBeVisible(
                '.ShippingAddressFields',
                'Не видно поле адреса при доставке type=courier'
            );

            await browser.assertView('suggest', '.EcomCartForm-Suggest');

            await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            // заполняем адрес доставки
            await browser.yaMockFetch({
                urlDataMap: {
                    'address/': JSON.stringify({
                        address: { id: 'randomId' },
                        status: 'success',
                    }),
                },
            });
            await browser.setValue('input[name="locality"]', 'москв');
            await browser.yaScrollPage('.Suggest-Item', 0.3);
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="street"]', 'street');
            await browser.click('.Suggest-Item'); // кликаем вне инпута чтобы сбросить выпадашку
            await browser.setValue('input[name="building"]', '22');

            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');

            await browser
                .yaGetExternalResourcesRequests(`https://${host}/empty-delivery/delay-3000/submit/shopping-cart/final/`)
                .then(requests => {
                    const deliveryData = JSON.parse(requests[0].text).delivery;

                    assert.equal(deliveryData, undefined, 'В запросе отправляются данные доставки');
                });
        });

        it('Ошибка получения доставок (status 404)', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/error-delivery/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: ['withDynamicDeliverySupport', 'setRegion'],
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'market-delivery-candidate',
            });
            await browser.yaWaitForHidden('.CartForm-RadioGroup .Spin');
            await browser.yaShouldBeVisible('.CartForm-RadioGroup');
            await browser.assertView('plain', '.CartForm-RadioGroup');
            await browser.yaShouldBeVisible(
                '.ShippingAddressFields',
                'Не видно поле адреса при доставке type=courier'
            );

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'checkout-delivery-market',
                params: { ecom_spa: 1, __ym, market_delivery_status: 'market_error' },
            });
        });

        it('Ошибка получения доставок (status 425)', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/error-delivery/425/',
                    'turbo-app-designe': 1,
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'checkout-delivery-market',
                params: { ecom_spa: 1, __ym, market_delivery_status: 'no_feed_id' },
            });
        });

        it('Отправляем цель для магазина, поддерживающего доставки, не под экспериментом', async function() {
            const browser = this.browser;
            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaShouldNotBeVisible('.EcomCartForm-Suggest');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'market-delivery-candidate',
            });
        });

        it('Предвыбираем пункт самовывоза - если он один', async function() {
            let host;
            const browser = this.browser;

            await browser
                .url('/')
                .url().then(res => {
                    const url = URL.parse(res.value);
                    host = url.hostname;
                })
                .then(() => this.browser.yaStartResourceWatcher(
                    '/static/turbo/hermione/mock-external-resources.sw.js',
                    [],
                ));

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/one-pickup-point/',
                    'turbo-app-designe': 1,
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaWaitForVisible('.CartForm-RadioItem_type_delivery');
            await browser.yaMockImages();

            await browser.assertView('selected-point', '.CartForm-RadioItem:nth-child(2)');

            await browser.click('.CartForm-RadioItem:nth-child(2)');
            await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');
            await browser
                .yaGetExternalResourcesRequests(`https://${host}/one-pickup-point/submit/shopping-cart/final/`)
                .then(requests => {
                    const deliveryData = JSON.parse(requests[0].text).delivery;
                    const expectedData = {
                        buyerCurrency: 'RUR',
                        deliveryOption: {
                            dates: {
                                toDate: '18-09-2020',
                                fromDate: '18-09-2020',
                                relativeFromDate: 1,
                                relativeToDate: 1,
                            },
                            deliveryServiceId: 99,
                            type: 'PICKUP',
                            buyerPrice: 200,
                            outlets: [{
                                serviceName: 'PickPoint',
                                coords: { latitude: '55.759106', longitude: '37.59683' },
                                name: 'Точка Pick Point',
                                address: { fullAddress: 'Москва, Малая Бронная, д. 5' },
                                id: '206911017',
                                workingTime: [{
                                    fromTime: '08:30',
                                    daysTo: '5',
                                    daysFrom: '1',
                                    toTime: '17:30',
                                }],
                            }],
                            serviceName: 'Самовывоз',
                            deliveryOptionId: '10460423_PICKUP_Самовывоз_18-09-2020_noreserve_18-09-2020_null_0_99_SHOP',
                        },
                        selectedPointId: '206911017',
                    };

                    assert.deepEqual(deliveryData, expectedData);
                });
        });

        it('Выбирать способом доставки самовывоз, если способ один и пункт один', async function() {
            let host;
            const browser = this.browser;

            await browser
                .url('/')
                .url().then(res => {
                    const url = URL.parse(res.value);
                    host = url.hostname;
                })
                .then(() => this.browser.yaStartResourceWatcher(
                    '/static/turbo/hermione/mock-external-resources.sw.js',
                    [],
                ));

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/only-one-pickup-point/',
                    'turbo-app-designe': 1,
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                },
                query: {
                    patch: ['withDynamicDeliverySupport', 'setRegion'],
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.yaWaitForVisible('.RadioItemDeliveryMap-Radio');
            await browser.yaMockImages();

            await browser.assertView('selected-point', '.CartForm-RadioItem:nth-child(1)');

            await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
            await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
            await browser.setValue('input[name=customer_email]', 'call@example.ru');

            await browser.click('.CartForm-SubmitButton');

            await browser.yaWaitForVisible('.EcomOrderSuccess');
            await browser
                .yaGetExternalResourcesRequests(`https://${host}/only-one-pickup-point/submit/shopping-cart/final/`)
                .then(requests => {
                    const deliveryData = JSON.parse(requests[0].text).delivery;
                    const expectedData = {
                        buyerCurrency: 'RUR',
                        deliveryOption: {
                            dates: {
                                toDate: '18-09-2020',
                                fromDate: '18-09-2020',
                                relativeFromDate: 1,
                                relativeToDate: 1,
                            },
                            deliveryServiceId: 99,
                            type: 'PICKUP',
                            buyerPrice: 200,
                            outlets: [{
                                serviceName: 'PickPoint',
                                coords: { latitude: '55.759106', longitude: '37.59683' },
                                name: 'Точка Pick Point',
                                address: { fullAddress: 'Москва, Малая Бронная, д. 5' },
                                id: '206911017',
                                workingTime: [{
                                    fromTime: '08:30',
                                    daysTo: '5',
                                    daysFrom: '1',
                                    toTime: '17:30',
                                }],
                            }],
                            serviceName: 'Самовывоз',
                            deliveryOptionId: '10460423_PICKUP_Самовывоз_18-09-2020_noreserve_18-09-2020_null_0_99_SHOP',
                        },
                        selectedPointId: '206911017',
                    };

                    assert.deepEqual(deliveryData, expectedData);
                });
        });
    });

    describe('Доставки на карте', function() {
        hermione.only.in(['chrome-phone'], 'Браузеронезависимо, а карта стабильнее в хроме');
        it('Карты, пины и описание пункта самовывоза', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-designe': 1,
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');

            await browser.yaWaitForVisible('.RadioItemDeliveryMap-MapButton');
            await browser.click('.RadioItemDeliveryMap-MapButton');
            await browser.yaWaitForVisible('.RadioItemDeliveryMap-Modal');
            await browser.yaWaitForVisible('.PickupMap-Marker');
            await browser.yaMockImages({ shouldObserve: true });
            await browser.assertView('map', '.RadioItemDeliveryMap-Modal');

            await browser.yaIndexify('.PickupMap-Marker');
            await browser.click('.PickupMap-Marker[data-index="1"]');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'outlet-map-outlet-info-clicked',
            });
            await browser.yaWaitForVisible('.RadioItemDeliveryMap-Modal');
            await browser.assertView('selected-point', '.RadioItemDeliveryMap-Modal');

            await browser.click('.PickupMapSubmit-Button');

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'outlet-map-outlet-selected',
            });

            await browser.yaWaitForHidden('.RadioItemDeliveryMap-Modal', 'Модалка с картой не скрылась');

            await browser.yaShouldBeVisible('.RadioItemDeliveryMap_active', 'Выбранный пункт самовывоза не отобразился');
        });
    });

    describe('Саджест выбора города', function() {
        hermione.only.notIn('searchapp', 'Браузеронезависимый, а в searchapp плавает');
        it('Внешний вид', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                },
                query: {
                    patch: ['withDynamicDeliverySupport', 'setRegion'],
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.yaMockFetch({
                urlDataMap: {
                    '/suggest-geo': JSON.stringify([
                        'Череповец',
                        [
                            {
                                name: 'Череповец, Вологодская область, Россия',
                                name_short: 'Череповец',
                                kind: 'locality',
                                lat: 59.122612,
                                lon: 37.90346527,
                                geoid: 968,
                                url: '\/\/yandex.ru\/pogoda\/968\/',
                                hl: [[0, 9]],
                            },
                            {
                                name: 'садоводческое товарищество № 8 Череповецметаллургхимстрой, Череповец, Вологодская область, Россия',
                                name_short: 'садоводческое товарищество № 8 Череповецметаллургхимстрой',
                                kind: 'locality',
                                lat: 59.15697479,
                                lon: 37.99799347,
                                geoid: 217121,
                                url: '\/\/yandex.ru\/pogoda\/217121\/',
                                hl: [[31, 40], [59, 68]],
                            },
                            {
                                name: 'садоводческое некоммерческое товарищество № 10 ЧМХС, деревня Ирдоматка, Череповецкий район, Вологодская область, Россия',
                                name_short: 'садоводческое некоммерческое товарищество № 10 ЧМХС',
                                kind: 'locality',
                                lat: 59.16239929,
                                lon: 38.0088501,
                                geoid: 122489,
                                url: '\/\/yandex.ru\/pogoda\/122489\/',
                                hl: [[72, 81]],
                            },
                        ],
                    ]),
                },
            });

            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.click('.GeoSuggest .Textinput-Control');
            await browser.yaWaitForVisible('.Suggest-Popup');
            await browser.assertView('suggest', ['.Suggest-Input', '.Suggest-Popup', '.GeoSuggest']);
        });

        it('Цели', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-designe': 1,
                    'turbo-app-cart-city-suggest': 1,
                    'turbo-app-cart-market-delivery': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'withDynamicDeliverySupport',
                },
            });

            await browser.yaWaitForVisible('.CartButton');
            await browser.click('.CartButton');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');
            await browser.click('.GeoSuggest .Textinput-Control');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'suggest-city-focus-field',
            });

            await browser.setValue('.GeoSuggest .Textinput-Control', 'Тверь');

            await browser.click('.Suggest-Menu > .Suggest-Item:nth-child(2)');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'suggest-city-selected',
            });
        });
    });
});
