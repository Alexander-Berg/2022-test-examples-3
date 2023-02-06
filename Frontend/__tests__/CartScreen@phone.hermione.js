const URL = require('url');

const __ym = {
    turbo_page: 1,
    doc_ui: 'touch-phone',
};

describe('CartScreen', function() {
    it('Внешний вид пустой корзины', function() {
        return this.browser
            .yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: { turboforms_endpoint: '/empty/' },
            })
            .yaWaitForVisible('.CartMeta')
            .yaMockImages()
            .assertView('emptyCart', '.CartMeta');
    });

    it('Проверка ссылки на каталог в пустой корзине', function() {
        return this.browser
            .yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: { turboforms_endpoint: '/empty/' },
            })
            .yaWaitForVisible('.CartMeta')
            .click('.CartMeta .CartHead-Link')
            .yaWaitForVisible('.EcomScreen.EcomScreen_type_product-list', 'Не загрузился каталог');
    });

    it('Внешний вид корзины с товарами', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            expFlags: { turboforms_endpoint: '/multiple/' },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaMockImages();
        await browser.waitForVisible('.CartHead');

        // Оставляем только первые два товара, чтобы влез итог.
        await browser.yaScrollPage('.ProductItem:nth-child(3) .CartHead-Remove', 0.3);
        await Promise.all([
            browser.yaWaitForHidden('.ProductItem:nth-child(3)'),
            browser.click('.ProductItem:nth-child(3) .CartHead-Remove'),
        ]);

        await browser.assertView('Carts', '.CartHead');
    });

    it('Внешний вид корзины с товаром под заказ', function() {
        return this.browser
            .yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: { turboforms_endpoint: '/multiple/' },
            })
            .yaWaitForVisible('.ScreenContent')
            .yaMockImages()
            .yaWaitForVisible('.CartHead')
            .assertView('plain', '.CartHead');
    });

    it('При наличии товара под заказ прячем выбор способа оплаты, а в данных отправляем "наличными"', async function() {
        const browser = this.browser;
        let host;

        // Устанавливаем Service Worker который перехватит запрос
        await browser.url('/');
        await browser.url().then(res => {
            const url = URL.parse(res.value);
            host = url.hostname;
        });
        await browser.then(() => this.browser.yaStartResourceWatcher(
            '/static/turbo/hermione/mock-external-resources.sw.js',
            [
                {
                    url: `https://${host}/multiple/submit/shopping-cart/final/`,
                    response: { status: 'success', delay: 3000, turbo_order_id: 1234 },
                }
            ],
        ));

        // Открываем нужную страницу
        await browser.yaOpenEcomSpa({
            service: 'ymturbo.t-dir.com',
            pageType: 'cart',
            expFlags: { turboforms_endpoint: '/multiple/' },
            query: { mock_payment_requests: '1' },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaScrollPage('.CartButton', 0.3);
        await browser.click('.CartButton');
        await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
        await browser.yaWaitForHidden('.NavigationTransition_state_entering');

        // В корзине товар "под заказ", поэтому выбора способа оплаты быть не должно
        await browser.yaShouldNotBeVisible('input[name=payment_method]:first-of-type', 'Выбор способа оплаты не должен быть виден');

        await browser.setValue('input[name=name]', 'name');
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
        await browser.setValue('input[name="building"]', '22');

        await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
        await browser.click('.CartForm-SubmitButton');

        // Проверяем отсутсвие поля payment_method=online в данных запроса
        const requests = await browser.yaGetExternalResourcesRequests(`https://${host}/multiple/submit/shopping-cart/final/`);

        assert.equal(requests.length, 1, 'Должен быть сделан один запрос отправки формы');

        const requestBody = JSON.parse(requests[0].text);

        assert.notEqual(requestBody.payment_method, 'online', 'Способ оплаты не должен быть онлайн');
    });

    it('Учитывать валюту товара', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: '72',
                pcgi: 'rnd=do4t',
            },
        });

        await browser.yaScrollPage('.ProductScreen-Actions-Button_toCart', 0.3);
        await browser.click('.ProductScreen-Actions-Button_toCart');

        await browser.yaWaitForVisible('.ProductScreen-Actions-Button_inCart', 'Товар не добавился в корзину');
        await browser.click('.ProductScreen-Actions-Button_inCart');

        await browser.yaWaitForVisible('.ProductItem .Cost');
        await browser.getText('.ProductItem .Cost').then(text => assert.deepEqual(text, ['1 790 ₽', '15 $']));
        await browser.getText('.Total-Value_summary .Cost').then(text => assert.equal(text, '1 805 ₽'));

        await browser.click('.CartHead-Remove');
        await browser.yaWaitUntil('Товар не удалился из корзины', () =>
            browser.execute(() => document.querySelectorAll('.ProductItem').length === 1)
                .then(({ value }) => value),
        );

        await browser.getText('.ProductItem .Cost').then(text => assert.equal(text, '15 $'));
        await browser.getText('.Total-Value_summary .Cost').then(text => assert.equal(text, '15 $'));
    });

    it('Рекомендации', async function() {
        const bro = this.browser;

        await bro.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            query: { patch: 'ecomCartRecommends' },
            expFlags: { turboforms_endpoint: '/multiple/delay-3000/' },
        });

        await bro.yaShouldNotBeVisible('.CartHead', 'Товары корзины уже появились на странице');
        await bro.yaIndexify('.ProductList');
        await bro.yaShouldNotBeVisible('.ProductList[data-index="0"]', 'Рекомендации появились раньше товаров в корзине');
        await bro.yaShouldNotBeVisible('.ProductList[data-index="1"]', 'Рекомендации появились раньше товаров в корзине');
        await bro.yaWaitForVisible('.ProductList', 6000, 'Рекомендации не появились');
        const { value: carousels } = await bro.elements('.ProductList');
        assert.lengthOf(carousels, 2, 'На странице не две карусели рекомендаций');
    });

    it('Избранное в корзине', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            query: { patch: 'setBlackboxData' },
            expFlags: { turboforms_endpoint: '/' },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaWaitForVisible('.CartHead');
        await browser.yaMockImages();
        await browser.assertView('plain', '.CartHead');

        await browser.yaMockFetch({
            urlDataMap: {
                '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
            },
        });

        await browser.click('.FavoriteButton');
        await browser.assertView('saved', '.CartHead');

        await browser.click('.FavoriteButton');
        await browser.assertView('deleted', '.CartHead');
    });

    describe('Удаление товара', () => {
        beforeEach(async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: '202',
                    pcgi: 'rnd=2lum7hf3',
                },
            });

            // Эмулируем добавление в корзину
            await browser.execute(function() {
                Ya.store.dispatch({
                    type: '@@cart/FETCH_SUCCESS',
                    payload: {
                        items: [{
                            count: 1,
                            id: '202',
                        }],
                        etag: 1,
                    },
                });
            });
            await browser.waitForVisible('.ProductScreen-Actions-Button_inCart', 'Товар не добавлен в корзину');
            await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
            await browser.click('.ProductScreen-Actions-Button_inCart');
            await browser.yaWaitForVisible('.ProductItem_in-cart', 3000, 'Товар в корзине не появился');
            // Без паузы почему-то элемент не кликабельный в iphone
            await browser.pause(1000);
        });

        it('кнопкой Удалить', async function() {
            const browser = this.browser;
            await browser.click('.CartHead-Remove');
            await browser.yaWaitForHidden('.ProductItem_in-cart', 'Товар не удалился из корзины');
        });

        it('изменением количества', async function() {
            const browser = this.browser;
            await browser.click('.CartHead-SelectMinusIcon');
            await browser.yaWaitForHidden('.ProductItem_in-cart', 'Товар не удалился из корзины');
        });
    });

    describe('Изменение цен и количества товаров', () => {
        beforeEach(async function() {
            const { browser } = this;
            let host;

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

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/spideradio/',
                    'turbo-app-cart-fresher': 1,
                },
            });

            await browser.yaWaitForVisible('.ProductItem', 3000, 'Товары в корзине не появились');
            const { value: productItems } = await browser.elements('.ProductItem');
            assert.lengthOf(productItems, 3, 'В первоначальном состоянии должно быть три товара в корзине');
            const counts = await browser.getAttribute('.CartHead-SelectInput', 'value');
            assert.deepEqual(counts, ['2', '3', '4'], 'Неверное первоначальное количество товаров');
            const prices = await browser.getText('.CartHead .Price-Cost_current');
            assert.deepEqual(prices, ['849 ₽', '749 ₽', '730 ₽'], 'Неверные первначальные цены на товары');

            // Скролим страницу в самый низ к кнопке «Оформить заказ».
            await browser.yaScrollElement('html', 0, 1000);
            // Ждём скрытия StickyPanel, чтобы она перестала перекрывать кнопку.
            await browser.pause(1500);
            await browser.click('.EcomScreen_type_cart .CartButton');
            await browser.yaWaitForVisible('.EcomCartForm', 1000, 'Не открылась форма оформления заказа');
            // Скролим страницу в самый низ к кнопке «Отправить».
            await browser.yaScrollElement('html', 0, 1000);
            // Ждём окончания эффекта скрола, иначе гермиона иногда будет считать, что кнопка не кликабельна.
            await browser.pause(1000);
            await browser.click('.CartForm-SubmitButton');
            await browser.yaWaitForVisible('.CartFresher', 3000, 'Не появился язычок');

            await browser
                .yaGetExternalResourcesRequests(`https://${host}/spideradio/submit/shopping-cart/update/`)
                .then(requests => {
                    const requestData = JSON.parse(requests[0].text);

                    assert.isTrue(requestData.sk && requestData.sk !== '', 'Нет sk в запросе к update');
                });

            // Скролим экран обратно вверх, чтобы не сползали скринщоты в chrome-phone и searchapp.
            await browser.yaScrollElement('html', 0, 0);
            await browser.pause(1000);
        });

        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaMockImages();
            await browser.assertView('fresher', '.Drawer-Curtain');
        });

        it('Переход на страницу изменившегося товара', async function() {
            const { browser } = this;

            await browser.click('.CartFresher .ProductItem');
            await browser.yaWaitForVisible('.EcomScreen_type_product', 1000, 'Не открылся экран товара');
            const productName = await browser.getText('.EcomScreen_type_product h1');
            assert.strictEqual(productName, 'labore ullamco Lorem enim cillum', 'Открылся не первый товар');

            await browser.click('.BottomBar-Item_type_cart');
            await browser.yaWaitForVisible('.EcomScreen_type_cart', 1000, 'Не открылся экран корзины');
            const { value: productItems } = await browser.elements('.ProductItem');
            assert.lengthOf(productItems, 3, 'В корзине не три товара');
        });

        it('Пересчёт корзины', async function() {
            const { browser } = this;

            // Ждём, когда навесится обработчик на кнопку закрытия язычка.
            await browser.pause(500);
            await browser.click('.CartFresher .EcomDrawer-Footer .Button2');
            await browser.yaWaitForHidden('.CartFresher', 2000, 'Язычок не скрылся');
            await browser.yaWaitForHidden('.EcomCartForm', 2000, 'Не скрылся экран формы оформления заказа');
            await browser.yaWaitForVisible('.EcomScreen_type_cart', 1000, 'Не открылся экран корзины');

            const { value: productItems } = await browser.elements('.ProductItem');
            assert.lengthOf(productItems, 2, 'После пересчёта в корзине осталось не два товара');
            const counts = await browser.getAttribute('.CartHead-SelectInput', 'value');
            assert.deepEqual(counts, ['2', '4'], 'После пересчёта в корзине должен поменяться состав товаров');
            const prices = await browser.getText('.CartHead .Price-Cost_current');
            assert.deepEqual(prices, ['1 500 ₽', '730 ₽'], 'После пересёта должны поменяться цены на товары');

            await browser.yaWaitForVisible('.Toast', 2000, 'Появился тост');
            const toastText = await browser.getText('.Toast');
            assert.strictEqual(toastText, 'Корзина пересчитана');
            await browser.yaWaitForHidden('.Toast', 4000, 'Тост скрылся через 3 секунды');
        });
    });

    describe('Промокоды', async function() {
        /**
         * testpalm не полностью соответствует спецификации теста
         * в части добавления товара в корзину - в hermione используется патчинг данных,
         * что позволяет делать тест более unit-овым
         */
        it('Успешное применение промокода', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/promo/',
                    'turbo-app-cart-fresher': 1,
                    'analytics-disabled': '0',
                },
            });

            await browser.yaScrollElement('html', 0, 2000);

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'promo-code-application',
                params: { ecom_spa: 1, __ym },
            });

            await browser.setValue('.CartPromoCode input', 'SUCCESS');

            const buttonText = await browser.getText('.CartPromoCode-Button');
            assert.deepEqual(buttonText, 'Применить');
            await browser.click('.CartPromoCode-Button');

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'promo-code-apply-click',
                params: { ecom_spa: 1, __ym },
            });

            // Ждем, когда промокод применится
            await browser.yaWaitForVisible('.CartPromoCode input[disabled]', 5000);

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'promo-code-apply-success',
                params: { ecom_spa: 1, __ym, 'is-full-cart-applied': false },
            });

            const buttonDeleteText = await browser.getText('.CartPromoCode-Button');
            assert.deepEqual(buttonDeleteText, 'Удалить');

            await browser.yaScrollElement('html', 0, 2000);

            await browser.yaMockImages();
            await browser.assertView('promo', '.CartHead-LoadingArea');
        });

        it('Неудачное применение промокода', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/promo/',
                    'turbo-app-cart-fresher': 1,
                    'analytics-disabled': '0',
                },
            });

            await browser.yaScrollElement('html', 0, 2000);

            await browser.setValue('.CartPromoCode input', 'ERROR');

            const buttonText = await browser.getText('.CartPromoCode-Button');
            assert.deepEqual(buttonText, 'Применить');
            await browser.click('.CartPromoCode-Button');
            // Ждем вывода ошибки
            await browser.yaWaitForVisible('.CartPromoCode-Info_error');

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'promo-code-apply-error',
                params: { ecom_spa: 1, __ym, 'error-type': 'not-applicable-to-cart' },
            });

            await browser.yaScrollElement('html', 0, 2000);

            await browser.yaMockImages();
            await browser.assertView('promo', '.CartHead-LoadingArea');
        });

        it('В пустой корзине нет поля промокода', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    'turbo-app-cart-fresher': 1,
                },
            });

            await browser.yaWaitForHidden('.Spin');

            const isPromoCodeVisible = await browser.isVisible('.CartPromoCode');
            assert.strictEqual(isPromoCodeVisible, false);
        });

        it('В корзине, к которой нельзя применить промокод, нет поля промокода', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/',
                    'turbo-app-cart-fresher': 1,
                },
            });

            await browser.yaWaitForHidden('.Spin');

            const isPromoCodeVisible = await browser.isVisible('.CartPromoCode');
            assert.strictEqual(isPromoCodeVisible, false);
        });

        it('Поле промокода не пропадает при удалении единственного товара в корзине с промокодом', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/promo/delete-no-promo/',
                    'turbo-app-cart-fresher': 1,
                    'analytics-disabled': '0',
                },
            });

            await browser.yaWaitForHidden('.Spin');

            const isPromoCodeVisible = await browser.isVisible('.CartPromoCode');
            assert.strictEqual(isPromoCodeVisible, true);

            await browser.click('.ProductItem:nth-child(1) .CartHead-Remove');

            const isPromoCodeStillVisible = await browser.isVisible('.CartPromoCode');
            assert.strictEqual(isPromoCodeStillVisible, true);
        });

        it('Изменение условий применения промокода', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/promo/change/',
                    'turbo-app-cart-fresher': 1,
                },
            });

            await browser.yaScrollElement('html', 0, 2000);

            await browser.setValue('.CartPromoCode input', 'SUCCESS');

            await browser.click('.CartPromoCode-Button');
            // Ждем, когда промокод применится
            await browser.yaWaitForVisible('.CartPromoCode input[disabled]');

            // Скролим страницу в самый низ к кнопке «Оформить заказ».
            await browser.yaScrollElement('html', 0, 2000);
            // Ждём скрытия StickyPanel, чтобы она перестала перекрывать кнопку.
            await browser.pause(1500);
            await browser.click('.EcomScreen_type_cart .CartButton');
            await browser.yaWaitForVisible('.EcomCartForm', 1000, 'Не открылась форма оформления заказа');
            // Скролим страницу в самый низ к кнопке «Отправить».
            await browser.yaScrollElement('html', 0, 2000);
            // Ждём окончания эффекта скрола, иначе гермиона иногда будет считать, что кнопка не кликабельна.
            await browser.pause(1000);
            await browser.click('.CartForm-SubmitButton');
            await browser.yaWaitForVisible('.CartFresher', 3000, 'Не появился язычок');

            // Скролим экран обратно вверх, чтобы не сползали скринщоты в chrome-phone и searchapp.
            await browser.yaScrollElement('html', 0, 0);

            await browser.yaMockImages();
            await browser.assertView('fresher', '.CartFresher');
        });

        it('Изменение условий применения промокода и числа товаров', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/promo/change/count/',
                    'turbo-app-cart-fresher': 1,
                },
            });

            // Скролим страницу в самый низ к кнопке «Оформить заказ».
            await browser.yaScrollElement('html', 0, 2000);

            // Ждём скрытия StickyPanel, чтобы она перестала перекрывать кнопку.
            await browser.pause(1500);
            await browser.click('.EcomScreen_type_cart .CartButton');
            await browser.yaWaitForVisible('.EcomCartForm', 1000, 'Не открылась форма оформления заказа');
            // Скролим страницу в самый низ к кнопке «Отправить».
            await browser.yaScrollElement('html', 0, 2000);
            // Ждём окончания эффекта скрола, иначе гермиона иногда будет считать, что кнопка не кликабельна.
            await browser.pause(1000);
            await browser.click('.CartForm-SubmitButton');
            await browser.yaWaitForVisible('.CartFresher', 3000, 'Не появился язычок');

            // Скролим экран обратно вверх, чтобы не сползали скринщоты в chrome-phone и searchapp.
            await browser.yaScrollElement('html', 0, 0);

            await browser.yaMockImages();
            await browser.assertView('fresher', '.CartFresher');
        });

        it('Саммери в форме после промокода', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'cart',
                expFlags: {
                    turboforms_endpoint: '/multiple/promo/success/',
                    'turbo-app-cart-fresher': 1,
                },
            });

            await browser.yaScrollElement('html', 0, 2000);

            await browser.setValue('.CartPromoCode input', 'SUCCESS');
            await browser.click('.CartPromoCode-Button');

            // Ждем, когда промокод применится
            await browser.yaWaitForVisible('.CartPromoCode input[disabled]', 5000);

            await browser.yaScrollElement('html', 0, 2000);
            await browser.click('.CartButton');

            await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
            await browser.yaWaitForHidden('.NavigationTransition_state_entering');

            await browser.assertView('plain', '.CartForm-Total');
        });
    });

    it('Происходит редирект к корзину при прямом заходе на страницу чекаута', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            query: { page_type: 'payment' },
        });

        await browser.yaWaitForVisible('.CartHead', 5000, 'Не произошел редирект в корзину');
    });

    it('Рекомендации в корзине', async function() {
        const { browser } = this;
        const __ym = {
            turbo_page: 1,
            doc_ui: 'touch-phone',
        };

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'cart',
            query: {
                patch: ['addCartRecommendations', 'setReqid'],
            },
            expFlags: {
                'analytics-disabled': '0',
                turboforms_endpoint: '/',
            },
        });

        await browser.yaWaitForVisible('.ProductList');
        await browser.yaScrollPage('.ProductList');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-carousel-seen',
            params: {
                ecom_spa: 1,
                host: 'spideradio.github.io',
                metrikaIds: ['65243191'],
                place: 'cartPage',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            }
        });

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-seen',
            params: {
                ecom_spa: 1,
                host: 'spideradio.github.io',
                metrikaIds: ['65243191'],
                offerId: '292',
                place: 'cartPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Dfnu7e56jbff',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });

        await browser.click('.ProductList .ProductItem button.ProductItem-Action');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-click-add-to-cart',
            params: {
                ecom_spa: 1,
                host: 'spideradio.github.io',
                metrikaIds: ['65243191'],
                offerId: '292',
                place: 'cartPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Dfnu7e56jbff',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });

        await browser.click('.ProductList .ProductItem');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-click',
            params: {
                ecom_spa: 1,
                host: 'spideradio.github.io',
                metrikaIds: ['65243191'],
                offerId: '292',
                place: 'cartPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Dfnu7e56jbff',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });
    });

    it('Валюта минимального заказа из товара', async function() {
        const browser = this.browser;
        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/rnd/do4t',
            query: {
                product_id: '72',
            },
            expFlags: { turboforms_endpoint: '/empty/' },
        });

        await browser.yaScrollPage('.ProductScreen-Actions-Button_toCart', 0.3);
        await browser.click('.ProductScreen-Actions-Button_toCart');

        await browser.yaWaitForVisible('.ProductScreen-Actions-Button_inCart', 'Товар не добавился в корзину');
        await browser.click('.ProductScreen-Actions-Button_inCart');

        await browser.yaWaitForVisible('.EcomScreen-CartButton');
        await browser.getText('.EcomScreen-CartButton').then(text => assert.equal(text, 'Минимальный заказ 400 $'));
    });
});
