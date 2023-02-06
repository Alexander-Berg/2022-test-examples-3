const { URL } = require('url');

async function visitCart(browser, { name, phone = '+7 800 800 80 80', email = 'call@example.ru' }) {
    // кликаем по "добавить в корзину"
    await browser.yaScrollPage('.Button2_view_action', 0.3);
    await browser.click('.Button2_view_action');
    await browser.waitForVisible('.ProductScreen-Actions-Button_inCart');

    // переходим в корзину
    await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
    await browser.click('.ProductScreen-Actions-Button_inCart');
    await browser.waitForVisible('.EcomScreen_type_cart');

    // переходим в форму оплаты
    await browser.yaWaitForHidden('.BottomBar-ItemPopup');
    await browser.yaScrollPage('.CartButton', 0.3);
    await browser.click('.CartButton');

    // скриним форму оплаты
    await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
    await browser.yaWaitForHidden('.NavigationTransition_state_entering');
    await browser.yaWaitForVisible('input[name=name]');

    await browser.setValue('input[name=name]', name);
    await browser.setValue('input[name=customer_phone]', phone);
    await browser.setValue('input[name=customer_email]', email);

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

    await browser.assertView('cart-form-page-filled', '.EcomScreen_type_cart .ScreenContent');

    await browser.yaScrollPage('.CartForm-RadioItem:nth-of-type(2)', 0.3);
    await browser.click('.CartForm-RadioItem:nth-of-type(2) label');

    await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
    await browser.click('.CartForm-SubmitButton');
}

describe('Ecom-tap', function() {
    it('Отдает валидный манифест', async function() {
        const browser = this.browser;

        // открываем страницу
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            skipInit: true,
            query: { manifest: 1, 'no-assets': '1' },
        });

        const browserUrl = await browser.getUrl();
        const { host } = new URL(browserUrl);

        // вытаскиваем ту же страницу через fetch
        const { value: manifest } = await browser.executeAsync(function(done) {
            return fetch(location.href)
                .then(r => r.json())
                .then(r => done(r));
        });

        // меняется между сборками
        delete manifest.yandex.app_version;

        // мы кэшируем js, так что его тоже удаляем
        manifest.yandex.cache.resources.splice(0, 1);

        // вырезаем лишние srcrwr что навязивает hermione
        manifest.start_url = manifest.start_url.replace(/[&?](srcrwr|testRunId|tpid)=[^&]*/g, '');
        manifest.yandex.cache.resources[0] = manifest.yandex.cache.resources[0]
            .replace(/[&?](srcrwr|testRunId|tpid)=[^&]*/g, '');

        const expected = {
            background_color: '#fff',
            description: 'Интернет-магазин',
            icons: [
                {
                    sizes: '576x576',
                    src: '/image?width=888&height=888&format=svg&patternSize=8',
                    type: 'image/png',
                },
                {
                    sizes: '144x144',
                    src: '/image?width=216&height=216&format=svg&patternSize=8',
                    type: 'image/png',
                },
            ],
            name: 'Spideradio',
            short_name: 'Spideradio',
            start_url: `https://${host}/turbo/spideradio.github.io/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1&exp_flags=turbo-app-any-ua%3D1&exp_flags=turbo-app-test-preset%3Dlocal&exp_flags=disable-turboapp%3D0&exp_flags=test_tool%3Dhermione`,
            theme_color: '#fff',
            yandex: {
                app_id: 'spideradio.github.io',
                base_url: `https://${host}/turbo/spideradio.github.io/`,
                cache: {
                    resources: [
                        `https://${host}/turbo/spideradio.github.io/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1&exp_flags=turbo-app-any-ua%3D1&exp_flags=turbo-app-test-preset%3Dlocal&exp_flags=disable-turboapp%3D0&exp_flags=test_tool%3Dhermione`,
                        'https://mc.yandex.ru/metrika/tag_turboapp.js',
                        'https://yastatic.net/s3/gdpr/popup/v2/ru.js',
                    ],
                },
                manifest_version: 1,
                metrika_id: 65243191,
                prefetch: {
                    entries: [
                        {
                            credentials_mode: 'include',
                            period_minutes: 30,
                            url: `https://${host}/turbo/spideradio.github.io/n/yandexturbocatalog/main/?morda=1&ecommerce_main_page_preview=1&isTap=1&isAjax=true`,
                        },
                        {
                            credentials_mode: 'include',
                            period_minutes: 600,
                            url: `https://${host}/turbo/spideradio.github.io/n/yandexturbocatalog/about/?isAjax=true`,
                        },
                    ],
                },
                splash_screen_color: '#ffffff',
            },
        };

        assert.deepEqual(manifest, expected, 'Манифест сайта неправильный');
    });

    it('Полный чекаут корзины', async function() {
        const browser = this.browser;

        // открываем страницу
        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: { product_id: '214' },
        });

        // делаем снимок экрана, на случай если
        await browser.yaScrollPage('.Button2_view_action', 0.3);
        await browser.yaMockImages();
        await browser.yaAssertViewportView('initial-product-page');

        // кликаем по "добавить в корзину"
        await browser.click('.Button2_view_action');
        await browser.waitForVisible('.ProductScreen-Actions-Button_inCart');
        await browser.yaMockImages();
        await browser.yaAssertViewportView('product-page-item-added');

        // переходим в корзину
        await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
        await browser.click('.ProductScreen-Actions-Button_inCart');
        await browser.waitForVisible('.EcomScreen_type_cart');

        // корзина открылась, все хорошо
        await browser.yaMockImages();
        await browser.yaWaitForHidden('.Popup');
        await browser.yaAssertViewportView('cart-page');

        // переходим в форму оплаты
        await browser.yaScrollPage('.CartButton', 0.3);
        await browser.click('.CartButton');

        // скриним форму оплаты
        await browser.yaWaitForHidden('.NavigationTransition_state_entering');
        await browser.assertView('cart-form-page', '.EcomScreen_type_cart .ScreenContent');

        assert.isTrue(await browser.isExisting('input[name=customer_phone]'), 'Нет инпута с type phone для телефона');
        assert.isTrue(await browser.isExisting('input[name=customer_email]'), 'Нет инпута с type email для электронной почты');

        await browser.setValue('input[name=name]', 'better');
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

        await browser.yaScrollPage('#cash-payment_method + label', 0.3);
        await browser.click('#cash-payment_method + label');

        await browser.assertView('cart-form-page-filled', '.EcomScreen_type_cart .ScreenContent');
        await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
        await browser.click('.CartForm-SubmitButton');

        await browser.yaWaitForVisible('.EcomOrderSuccess');
        await browser.yaAssertViewportView('success-payment');
    });

    it('Корзина, которая уже была отправлена', async function() {
        const browser = this.browser;
        // открываем страницу
        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: { product_id: '214' },
        });

        await visitCart(browser, { name: 'already-sent' });

        await browser.yaWaitForVisible('.Modal');
        await browser.yaAssertViewportView('already-sent-form');
    });

    it('Корзина с ошибками', async function() {
        const browser = this.browser;
        // открываем страницу
        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: { product_id: '214' },
        });

        await visitCart(browser, { name: 'error' });

        await browser.yaWaitForVisible('.Modal');
        await browser.yaAssertViewportView('error-form');
    });

    hermione.skip.in(['iphone'], 'Нативных оплат на ios нет');
    it('Paymentapi, успешно', async function() {
        const browser = this.browser;
        // открываем страницу
        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: {
                product_id: '214',
                mock_payment_requests: '1',
            },
        });

        await visitCart(browser, { name: 'name' });

        await browser.yaWaitForVisible('.EcomOrderSuccess');
        await browser.yaMockImages();
        await browser.assertView('success-payment', ['.Cover', '.ScreenContent']);
    });

    hermione.skip.in(['iphone'], 'Нативных оплат на ios нет');
    it('Paymentapi, неудачно', async function() {
        const browser = this.browser;
        // открываем страницу
        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: {
                product_id: '214',
                mock_payment_requests_throw: '1',
            },
        });

        await visitCart(browser, { name: 'name' });

        await browser.yaWaitForVisible('.CartMeta');
        await browser.yaMockImages();
        await browser.yaAssertViewportView('fail-payment');
    });

    hermione.skip.in(['iphone'], 'Нативных оплат на ios нет');
    it('Paymentapi, отмена', async function() {
        const browser = this.browser;
        // открываем страницу
        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: {
                product_id: '214',
                mock_payment_requests_throw_not_started: '1',
            },
        });

        await visitCart(browser, { name: 'name' });

        await browser.yaWaitForVisible('.CartMeta');
        await browser.yaMockImages();
        await browser.yaAssertViewportView('cancel-payment');
    });

    it('Подскролл на морде', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.Cover');
        await browser.yaMockImages();
        await browser.assertView('cover', '.Cover');

        await browser.yaScrollPage(50);
        await browser.pause(50); // Чтобы js успел поменять стиль
        await browser.yaMockImages();
        await browser.assertView('cover-scrolled', '.Cover', { ignoreElements: '.turbo-card-slider' });

        await browser.refresh();

        await browser.yaWaitUntil(
            'страница не проинициализировалсь',
            () => browser.execute(function() {
                return window.Ya && window.Ya.TAP_LOADED;
            })
        );
        await browser.yaWaitForVisible('.Cover');
        await browser.yaTouchScroll('body', 0, 0);

        await browser.yaWaitUntil(
            'страница не проскроллилась вверх',
            () => browser.execute(function() {
                return window.pageYOffset === 0;
            })
        );
        await browser.yaMockImages();
        await browser.assertView('cover-reload', '.Cover', { ignoreElements: '.turbo-card-slider' });
    });

    it('Быстрый заказ', async function() {
        const browser = this.browser;
        let host;

        await browser
            .url('/')
            .url().then(res => {
                const url = new URL(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                [],
            ));

        await browser.yaOpenEcomSpa({
            url: '/turbo/ymturbo.t-dir.com/s/catalog/slippers/slippers-favorite-sport/',
            query: { product_id: '214' },
            exp_flags: { turboforms_endpoint: '/empty/' },
        });

        await browser.yaWaitForVisible('.EcomBottomBar_visible');

        // Жмем "Быстрый заказ"
        await browser.yaScrollPage('.Button2_view_default', 0.3);
        await browser.click('.Button2_view_default');
        await browser.waitForVisible('.EcomScreen_type_cart');
        await browser.assertView('order-form', '.EcomScreen_type_cart .ScreenContent');

        await browser
            .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            .then(requests => {
                const addToCartData = JSON.parse(requests[requests.length - 1].text);

                assert.isTrue(addToCartData.sk && addToCartData.sk !== '', 'Нет sk при открытии формы');
            });

        // Заполняем поля
        await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
        await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
        await browser.setValue('input[name=customer_email]', 'call@example.ru');
        await browser.setValue('.Textarea textarea', 'Адрес доставки');

        // Делаем скриншот и отправляем форму
        await browser.assertView('order-form-filled', '.EcomScreen_type_cart .ScreenContent');
        await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
        await browser.click('.CartForm-SubmitButton');

        await browser.yaWaitForVisible('.EcomOrderSuccess');
        await browser.yaAssertViewportView('order-success');

        await browser
            .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/final/`)
            .then(requests => {
                const addToCartData = JSON.parse(requests[requests.length - 1].text);

                assert.isTrue(addToCartData.sk && addToCartData.sk !== '', 'Нет sk при откправке формы');
            });
    });

    it('Селекторы с длинным текстом корректно отрисовываются', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'super01.ru',
            pageType: 'catalog',
            query: { query: 'Детский костюм' },
        });

        await browser.yaMockFetch({
            status: 200,
            delay: 3000,
            urlDataMap: {
                'isAjax=true': JSON.stringify({
                    entities: {
                        products: {
                            '9783-1': {
                                variations: [{
                                    label: 'Размер',
                                    value: 0,
                                    items: [
                                        {
                                            value: 'https://super01.ru/products/kostium-kharli-kvin-otryad-samoubiits?size=96',
                                            text: '120 RU Очень длинный вариант ну прям очень длинный',
                                        },
                                        {
                                            value: 'https://super01.ru/products/kostium-kharli-kvin-otryad-samoubiits',
                                            text: '110 RU',
                                        },
                                    ],
                                }],
                            },
                        },
                    },
                    pagesMeta: {},
                }),
            },
        });
        await browser.click('.ProductItem:first-child');
        await browser.yaWaitForVisible('.ProductsFilter-SelectContainer');

        // Проверяем внешний вид select
        await browser.yaScrollPage('.ProductsFilter-SelectContainer', 0.3);
        await browser.assertView('select', '.ProductsFilter-SelectContainer');
    });

    describe('Переход в корзину', () => {
        it('По кнопке уже добавленного товара', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: '202',
                    pcgi: 'rnd%3D2lum7hf3',
                },
            });

            // Сэмулируем добавление в коризну
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

            // Дождемся прогрузки корзины и попробуем перейти
            await browser.waitForVisible('.ProductScreen-Actions-Button_inCart', 'Товар не добавлен в корзину');
            await browser.yaScrollPage('.ProductScreen-Actions-Button_inCart', 0.3);
            await browser.click('.ProductScreen-Actions-Button_inCart');
            await browser.waitForVisible('.EcomScreen_type_cart', 'Корзина не открылась');
        });
    });

    describe('Магазин без внешней и внутренней корзины', () => {
        it('Нет кнопок добавления в корзину в карусели товаров на главной странице', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'zenbox.ru',
                pageType: 'main',
            });

            const isProductItemsExist = await browser.isExisting('.ProductList .ProductItem');
            const isProductActionsExist = await browser.isExisting('.ProductList .ProductItem .ProductItem-Action');

            assert.isTrue(isProductItemsExist, 'На странице нет карточек товаров');
            assert.isFalse(isProductActionsExist, 'На карточках товаров есть кнопка');
        });

        it('Нет кнопок добавления в корзину на листинге', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'zenbox.ru',
                pageType: 'catalog',
                query: {
                    category_id: '9',
                    category_count: '176',
                },
            });

            const isProductItemsExist = await browser.isExisting('.ProductList .ProductItem');
            const isProductActionsExist = await browser.isExisting('.ProductList .ProductItem .ProductItem-Action');

            assert.isTrue(isProductItemsExist, 'На странице нет карточек товаров');
            assert.isFalse(isProductActionsExist, 'На карточках товаров есть кнопка');
        });

        it('Нет кнопки добавления в корзину на странице товара', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo/zenbox.ru/s/product/smartfon-xiaomi-mi-10-8128-gb-zelenyy-01482',
                query: { product_id: '790' },
            });

            const isProductActionExist = await browser.isExisting('.ProductScreen-Actions-Button_toCart');
            assert.isFalse(isProductActionExist, 'На странице товара есть кнопка добавления в корзину');
        });
    });

    describe('Presearch', () => {
        const assertCover = async browser => {
            await browser.yaMockImages();
            await browser.assertView('cover', '.Cover');

            await browser.yaScrollPage(50);
            await browser.pause(50); // Чтобы js успел поменять стиль
            await browser.yaMockImages();
            await browser.assertView('cover-scrolled', '.Cover', { ignoreElements: '.turbo-card-slider' });
        };

        it('Главная страница', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                exp_flags: { 'turbo-app-presearch-enabled': '1' },
            });

            await browser.yaWaitForVisible('.Cover');
            await browser.yaWaitForVisible('.Collection');
            await browser.yaWaitForVisible('.ProductList ');

            await assertCover(browser);
        });

        it('Каталог', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: {
                    category_id: '12',
                    category_count: '6',
                },
                exp_flags: { 'turbo-app-presearch-enabled': '1' },
            });

            await browser.yaWaitForVisible('.Cover');
            await browser.yaWaitForVisible('.ProductList');
            await browser.yaWaitForVisible('.ProductListControl');

            await assertCover(browser);
        });
    });
});
