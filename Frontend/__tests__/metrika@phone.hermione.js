const { set } = require('lodash');

const __ym = {
    turbo_page: 1,
    doc_ui: 'touch-phone',
};

async function openPage({
    browser,
    service = 'spideradio.github.io',
    pageType,
    url,
    query = {},
    expFlags = {}
}) {
    await browser.yaOpenEcomSpa({
        service, pageType, query, url,
        expFlags: Object.assign({}, expFlags, { 'analytics-disabled': '0' }),
    });

    await browser.yaWaitUntil('Метрика не загрузилась', () =>
        browser.execute(() => window.Ya && window.Ya.Metrika)
            .then(({ value }) => value),
    );

    // Ожидаем отправки хита, он отправляется с задержкой в секунду
    await browser.pause(1000);
}

async function fillCart(browser, submit = true) {
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

    if (submit) {
        await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
        await browser.click('.CartForm-SubmitButton');
    }
}

async function fillOneClickCart(browser, submit = true) {
    await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
    await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
    await browser.setValue('input[name=customer_email]', 'call@example.ru');
    await browser.setValue('.Textarea textarea', 'Адрес доставки');

    if (submit) {
        await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
        await browser.click('.CartForm-SubmitButton');
    }
}

async function stubAuthorization(browser) {
    await browser.execute(() => {
        // стаб метода залогина СуперАппа
        // если его не будет, то при клике в профиль происходит переход на паспорт
        // и проверка цели не успевает отработать
        window.yandex = { private: { portalAuth: { login: () => Promise.resolve() } } };
    });
}

async function stubVisitParams(browser) {
    await browser.execute(() => {
        window.__receivedParamsList = [];
        window.__sentParamsList = [];
        // стаб метода отправки параметров визита
        window.yaCounter53911873.params = params => window.__sentParamsList.push(params);
    });

    // Сохраняем данные о параметрах визита, которые пришли от бэка
    await browser.yaProxyFetch({
        test: 'isAjax=true',
        contentType: 'json',
        then: function(url, data) {
            window.__receivedParamsList.push(
                data.meta.metrikaParams[53911873],
            );
        },
    });
}

async function getVisitParams(browser) {
    // Ожидаем отправки параметров,
    // они могут отправляется с задержкой после анимации перехода между экранами
    await browser.pause(1000);
    const { value } = await browser.execute(() => {
        return [window.__sentParamsList, window.__receivedParamsList];
    });
    return value;
}

async function getLayers(browser, index = 0) {
    let dataLayers = [];
    const getErrorMessage = () => (index ?
        `В datalayer нет элемента с индексом ${index}` :
        'Datalyer не появился') + `\ndataLayers: ${JSON.stringify(dataLayers, null, '  ')}`;

    await browser.yaWaitUntil(getErrorMessage, async function() {
        ({ value: dataLayers } = await browser.execute(function() {
            return [window.dataLayer, window.yaDataLayer];
        }));

        if (!dataLayers[0]) {
            return false;
        }
        return index < dataLayers[0].length;
    });

    return dataLayers.map(layer => layer[index]);
}

/**
 * функция для проверка data-layers
 * @param {object} browser - объект гермионы
 * @param {object} params - опции
 * @param {string} params.path - путь до объекта products в data-layer, например 'ecommerce.add.products[0]'
 * @param {string} params.layerId - id указанный в layer
 * @param {string} params.dataLayerId - id указанный в dataLayer
 */
async function assertLayers(browser, { path, layerId, dataLayerId, data, more = [], index = 0 }) {
    const [layer, yaLayer] = await getLayers(browser, index);

    const expectedLayer = set({}, path, { id: layerId, ...data });
    const expectedYaLayer = set({}, path, { id: dataLayerId, ...data });

    more.forEach(({ path, value }) =>
        [expectedLayer, expectedYaLayer].forEach(
            layer => set(layer, path, value),
        ),
    );

    try {
        assert.deepEqual(layer, expectedLayer);
    } catch (e) {
        throw new Error(`expected ${JSON.stringify(layer, null, '  ')} to deeply equal ${JSON.stringify(expectedLayer, null, '  ')} `);
    }

    try {
        assert.deepEqual(yaLayer, expectedYaLayer);
    } catch (e) {
        throw new Error(`expected ${JSON.stringify(yaLayer, null, '  ')} to deeply equal ${JSON.stringify(expectedYaLayer, null, '  ')} `);
    }
}

describe('Ecom-tap', () => {
    describe('Метрика', () => {
        it('Добавляет utm-параметры к метричному урлу', async function() {
            const url = 'https://spideradio.github.io/?rnd=c7hj4fr';

            const utmParamsObj = { rs: 'yamarket3_21578227_74475', ymclid: '15936813805346869332500002', clid: '928' };
            const utmParamsString = Object.entries(utmParamsObj).map(([key, value]) => `${key}=${value}`).join('&');

            const browser = this.browser;
            await openPage({
                browser,
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 107,
                    pcgi: 'rnd=c7hj4fr',
                    ...utmParamsObj,
                },
            });

            await browser.yaAssertLastMetrikaHit(53911873, [url + '&' + utmParamsString, {
                referer: 'https://yandex.ru',
                title: await browser.getTitle(),
                params: {
                    __ym,
                    ecom_spa: 1,
                    ecom_product_card: 1,
                    turbo_app_enabled: 1,
                },
            }]);
        });

        it('Привязывает эксперименты к визиту метрики', async function() {
            const browser = this.browser;
            await openPage({
                browser,
                url: '/turbo/spideradio.github.io/s/',
                query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
            });

            const { value: expectedExperiment } = await browser.execute(function() {
                return window.Ya.store.getState().meta.experiments;
            });

            await browser.yaCheckMetrikaExperiment({
                counterId: 53911873,
                hash: expectedExperiment,
            });
        });

        describe('Data-layer', function() {
            it('Событие detail', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await assertLayers(browser, {
                    path: 'ecommerce.detail.products[0]',
                    layerId: '107',
                    dataLayerId: 'spideradio.github.io_107',
                    data: {
                        name: 'irure duis laborum esse mollit',
                        price: 3999,
                    },
                });
            });

            it('Событие add', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await browser.yaScrollPage('.Button2_view_action', 0.3);
                await browser.click('.Button2_view_action');

                await assertLayers(browser, {
                    path: 'ecommerce.add.products[0]',
                    layerId: '107',
                    dataLayerId: 'spideradio.github.io_107',
                    data: {
                        name: 'irure duis laborum esse mollit',
                        price: 3999,
                        quantity: 1,
                    },
                    index: 1,
                });
            });

            it('Событие add при изменении количества в корзине', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartHead-SelectPlusIcon');

                await assertLayers(browser, {
                    path: 'ecommerce.add.products[0]',
                    layerId: '3242424234',
                    dataLayerId: 'spideradio.github.io_3242424234',
                    data: {
                        name: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
                        price: 1790,
                        quantity: 1,
                    },
                });
            });

            it('Событие add при покупке с витрины', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.yaScrollPage('.Button2_view_action', 0.3);
                await browser.click('.Button2_view_action');

                await assertLayers(browser, {
                    path: 'ecommerce.add.products[0]',
                    layerId: '107',
                    dataLayerId: 'spideradio.github.io_107',
                    data: {
                        name: 'irure duis laborum esse mollit',
                        price: 3999,
                        quantity: 1,
                    },
                });
            });

            it('Событие remove', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartHead-Remove');

                await assertLayers(browser, {
                    path: 'ecommerce.remove.products[0]',
                    layerId: '3242424234',
                    dataLayerId: 'spideradio.github.io_3242424234',
                    data: {
                        name: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
                        price: 1790,
                        quantity: 1,
                    },
                });
            });

            it('Событие remove при изменении количества', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartHead-SelectPlusIcon');
                await browser.pause(1000); // тротлинг
                await browser.click('.CartHead-SelectMinusIcon');

                await assertLayers(browser, {
                    path: 'ecommerce.remove.products[0]',
                    layerId: '3242424234',
                    dataLayerId: 'spideradio.github.io_3242424234',
                    data: {
                        name: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
                        price: 1790,
                        quantity: 1,
                    },
                    index: 1,
                });
            });

            it('Событие purchase', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.Button2_view_action');
                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillCart(browser);
                await assertLayers(browser, {
                    path: 'ecommerce.purchase.products[0]',
                    layerId: '3242424234',
                    dataLayerId: 'spideradio.github.io_3242424234',
                    data: {
                        name: 'GoPro miniUSB кабель для подкл. к ТV ACMPS-301',
                        price: 1790,
                        quantity: 1,
                    },
                    more: [{
                        path: 'ecommerce.purchase.actionField',
                        value: { id: 1234 },
                    }],
                });
            });

            it('Событие purchase в покупке в 1 клик', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await browser.yaScrollPage('.Button2_view_default', 0.3);
                await browser.click('.Button2_view_default');

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillOneClickCart(browser);
                await assertLayers(browser, {
                    path: 'ecommerce.purchase.products[0]',
                    layerId: '107',
                    dataLayerId: 'spideradio.github.io_107',
                    data: {
                        name: 'irure duis laborum esse mollit',
                        price: 3999,
                        quantity: 1,
                    },
                    more: [{
                        path: 'ecommerce.purchase.actionField',
                        value: { id: 1234 },
                    }],
                    index: 1,
                });
            });

            it('Отправка промокода в событии purchase', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'cart',
                    expFlags: {
                        turboforms_endpoint: '/multiple/promo/success/',
                        'turbo-app-cart-fresher': 1,
                        'analytics-disabled': '0',
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

                await fillCart(browser);

                const [layer, yaLayer] = await getLayers(browser);

                const expectedLayer = {
                    ecommerce: {
                        purchase: {
                            actionField: {
                                id: 1234,
                                coupon: 'SUCCESS',
                            },
                            products: [{
                                coupon: 'SUCCESS',
                                id: '79601',
                                name: 'Портативное цифровое пианино Yamaha P-45B',
                                price: 30225,
                                quantity: 2,
                            },
                            {
                                id: '106295',
                                name: 'Укулеле Veston KUS 15GR Veston KUS 15GR Veston KUS 15GR Veston KUS 15GR',
                                price: 2360,
                                quantity: 1,
                            },
                            {
                                id: '128790',
                                name: 'Гитарный преамп Orange Bax Bangeetar (BLK)',
                                price: 30990,
                                quantity: 5,
                            }],
                        },
                    },
                };
                const expectedYaLayer = {
                    ...expectedLayer,
                    ecommerce: {
                        ...expectedLayer.ecommerce,
                        purchase: {
                            ...expectedLayer.ecommerce.purchase,
                            products: expectedLayer.ecommerce.purchase.products.map(product => ({
                                ...product,
                                id: 'spideradio.github.io_' + product.id,
                            })),
                        },
                    },
                };
                try {
                    assert.deepEqual(layer, expectedLayer);
                } catch (e) {
                    throw new Error(`expected ${JSON.stringify(layer, null, '  ')} to deeply equal ${JSON.stringify(expectedLayer, null, '  ')} `);
                }

                try {
                    assert.deepEqual(yaLayer, expectedYaLayer);
                } catch (e) {
                    throw new Error(`expected ${JSON.stringify(yaLayer, null, '  ')} to deeply equal ${JSON.stringify(expectedYaLayer, null, '  ')} `);
                }
            });

            // #TODO: событие автодобавления -- autoadd
        });

        describe('Главная страница', () => {
            it('Показ страницы', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });

                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_main: 1 },
                }]);
            });

            it('Ввод поискового запроса', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'sunt');
                await browser.click('.SuggestSearch-Button');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-submit-via-button',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-input-focus',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-submit',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-submit-main',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_catalog: 1, ecom_catalog_with_search: 1 },
                }]);
            });

            it('Отправляются параметры визита после загруки данных', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await stubVisitParams(browser);

                await browser.click('.BottomBar-Item_type_main');
                await browser.yaWaitForVisible('.ScreenContent');

                await browser.pause(1000);
                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_main: 1 },
                }]);

                const [sentParams, receivedParams] = await getVisitParams(browser);
                assert.deepEqual(sentParams, receivedParams);
            });

            describe('Взаимодествие с елементами на странице', () => {
                beforeEach(async function() {
                    const browser = this.browser;
                    await openPage({ browser, pageType: 'main' });
                });

                it('Клик в фасет', async function() {
                    const browser = this.browser;
                    await browser.click('.CategoryList .CategoryList-Item');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'open-facet-main',
                        params: {
                            __ym,
                            ecom_spa: 1,
                        },
                    });
                });

                it('Клик в баннер', async function() {
                    const browser = this.browser;
                    await browser.click('.turbo-card-slider .EcomPromo-Text');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'promo-banner-click',
                        params: {
                            __ym,
                            ecom_spa: 1,
                        },
                    });
                });

                it('Клик в блок скидочных и популярных категорий', async function() {
                    const browser = this.browser;
                    await browser.click('.Collection .Collection-Item');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'collection-item-click',
                        params: {
                            __ym,
                            ecom_spa: 1,
                        },
                    });
                });

                it('Клик в блок популярных товаров', async function() {
                    const browser = this.browser;
                    await browser.yaIndexify('.ProductList');
                    await browser.click('.ProductList[data-index="0"] .ProductItem');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'carousel-item-click',
                        params: {
                            __ym,
                            ecom_spa: 1,
                            type_recommendation: 'Популярные товары',
                        },
                    });
                });

                it('Клик в блок товаров "выгодно сегодня"', async function() {
                    const browser = this.browser;
                    await browser.yaIndexify('.ProductList');
                    await browser.click('.ProductList[data-index="1"] .ProductItem');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'carousel-item-click',
                        params: {
                            __ym,
                            ecom_spa: 1,
                            type_recommendation: 'Выгодно сегодня',
                        },
                    });
                });
            });

            describe('Редизайн', () => {
                beforeEach(async function() {
                    const browser = this.browser;

                    await openPage({
                        browser,
                        pageType: 'main',
                        expFlags: { 'turbo-app-morda-redesign': 1 },
                    });
                });

                it('Клик в блок скидочных и популярных категорий', async function() {
                    const browser = this.browser;
                    await browser.click('.RedesignedCollection .RedesignedCollection-Item');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'collection-item-click',
                        params: {
                            __ym,
                            ecom_spa: 1,
                        },
                    });
                });

                it('Клик в блок с бесконечным каталогом', async function() {
                    const browser = this.browser;

                    await browser.yaWaitForVisible('.GlobalProductList', 'Список продуктов не загрузился');
                    await browser.click('.GlobalProductList .ProductItem');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'main-global-product-list-click',
                        params: {
                            __ym,
                            ecom_spa: 1,
                        },
                    });
                });
            });

            it('Клик в блок с красивыми категориями', async function() {
                const browser = this.browser;

                await openPage({
                    browser,
                    pageType: 'main',
                    query: { patch: 'addPrettyCategory' },
                    expFlags: { 'turbo-app-morda-redesign': 1 },
                });

                await browser.yaWaitForVisible('.PrettyCategory-Info', 'Красивая категория не загрузилась');
                await browser.yaScrollPage('.PrettyCategory-Info', 0.3);
                await browser.click('.PrettyCategory-Info');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'pretty-category-click',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });
            });
        });

        describe('Каталог', () => {
            it('Показ страницы', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_catalog: 1 },
                }]);
            });

            it('Ввод поискового запроса', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'пантолеты');
                await browser.click('.SuggestSearch-Button');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-submit-via-button',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-input-focus',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'products-search-submit',
                    params: { __ym, ecom_spa: 1 },
                });

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_catalog: 1, ecom_catalog_with_search: 1 },
                }]);
            });

            it('Отправка целей опечаточника', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'сообрашения');
                await browser.click('.SuggestSearch-Button');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'search-misspell',
                    params: { __ym, ecom_spa: 1 },
                });

                await browser.yaWaitForVisible('.Misspell-Link', 'Не появился блок про исправление опечатки');
                await browser.click('.Misspell-Link');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'search-misspell-cancel',
                    params: { __ym, ecom_spa: 1 },
                });
            });

            it('Отправка цели про пустые результаты поиска', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'ese');
                await browser.click('.SuggestSearch-Button');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'search-empty-results',
                    params: { __ym, ecom_spa: 1 },
                });
            });

            it('Отправка цели про повторный поиск', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'esse');
                await browser.click('.SuggestSearch-Button');

                // Дожидаемся окончания поиска по скрытию скелетона и появлению списка товаров
                await browser.yaWaitForHidden('.CategoryList_skeleton');
                await browser.yaWaitForVisible('.ProductList', 'Не появился список товаров');

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'sunt');
                await browser.click('.SuggestSearch-Button');

                // Дожидаемся окончания поиска по появлению ссылки на категорию,
                //  которой не было в ответе на прошлый поиск
                await browser.yaWaitForVisible(
                    'a.SearchCategoryListItem[href*="category_id=11"]',
                    'Не появилась ссылка на категорию "Спортивная одежда"'
                );

                await browser.click('.SuggestSearch-Input');
                await browser.setValue('.SuggestSearch-Input input', 'esse');
                await browser.click('.SuggestSearch-Button');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'search-repeated-request',
                    params: { __ym, ecom_spa: 1 },
                });
            });

            it('Клик по категории', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.click('.CategoryList-Item');
                await browser.yaAssertLastMetrikaGoal(53911873, ['open-facet', { __ym, ecom_spa: 1 }]);

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_catalog: 1 },
                }]);
            });

            it('Изменение сортировки', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.selectByValue('.ProductListControl-Option select', 'price');
                await browser.yaAssertLastMetrikaGoal(53911873, ['sort-products', { __ym, ecom_spa: 1 }]);

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_catalog: 1 },
                }]);
            });

            it('Добавление и переход в корзину', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.yaScrollPage('.ProductItem-Actions .ProductItem-Action', 0.3);
                await browser.click('.ProductItem-Actions .ProductItem-Action');
                await browser.yaWaitForVisible('.ProductItem-Actions .ProductItem-Action_inCart', 'Товар не добавился в корзину');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'add-to-cart',
                    params: { __ym, ecom_spa: 1 },
                });

                await browser.click('.ProductItem-Actions .ProductItem-Action_inCart');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'open-cart-from-product',
                    params: { __ym, ecom_spa: 1 },
                });

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_cart: 1,
                        turbo_app_enabled: 1,
                    },
                }]);
            });

            it('Переход в товар', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'catalog' });

                await browser.click('.ProductItem');
                await browser.yaAssertLastMetrikaGoal(53911873, ['open-product-item', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['https://spideradio.github.io/rnd/oqr9w', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_product_card: 1,
                        turbo_app_enabled: 1,
                    },
                }]);
            });

            it('Отправляются параметры визита после загруки данных', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });
                await stubVisitParams(browser);

                await browser.yaWaitForVisible('.CategoryList-ItemContainer');
                await browser.click('.CategoryList-ItemContainer');

                await browser.pause(1000);
                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, ecom_catalog: 1, turbo_app_enabled: 1 },
                }]);

                const [sentParams, receivedParams] = await getVisitParams(browser);
                assert.deepEqual(sentParams, receivedParams);
            });
        });

        describe('Корзина', () => {
            it('Показ страницы', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'cart',
                    expFlags: {
                        turboforms_endpoint: '/empty/',
                    },
                });

                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_cart: 1,
                        turbo_app_enabled: 1,
                    },
                }]);
            });

            it('Оформление заказа, оплата наличными', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart', service: 'ymturbo.t-dir.com' });

                await browser.click('.CartButton');
                await browser.yaAssertLastMetrikaGoal(53911873, ['open-check-out-form-from-cart', {
                    __ym,
                    ecom_spa: 1,
                    turbo_app_enabled: 1,
                }]);

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');
                await browser.yaWaitForVisible('.CartForm-SubmitButtonWrapper');

                await fillCart(browser, false);
                await browser.yaScrollPage('[for="cash-payment_method"] div', 0.3);
                await browser.click('[for="cash-payment_method"] div');
                await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
                await browser.click('.CartForm-SubmitButton');

                await browser.yaWaitForVisible('.EcomOrderSuccess');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-success-from-cart',
                    params: { payment: 'cash', __ym, ecom_spa: 1 },
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'typ-registration',
                    params: { __ym, ecom_spa: 1 },
                });
            });

            hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
            it('Оформление заказа, альбомная ориентация', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartButton');

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await browser.setOrientation('landscape');

                await browser.setValue('input[name=name]', 'Иванов Павел Васильевич');
                await browser.setValue('input[name=customer_phone]', '+7 800 800 80 80');
                await browser.setValue('input[name=customer_email]', 'call@example.ru');
                await browser.click('.CartForm-RadioItem_type_delivery:last-child');

                await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
                await browser.click('.CartForm-SubmitButton');

                await browser.yaWaitForVisible('.EcomOrderSuccess');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'send-check-out-form-from-cart',
                    params: { orientation: 'landscape', payment: 'cash', __ym, ecom_spa: 1 },
                });
            });

            hermione.skip.in(['iphone'], 'Нативных оплат на ios нет');
            it('Оформление заказа, оплата картой, успешно', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'cart',
                    service: 'ymturbo.t-dir.com',
                    query: { mock_payment_requests: '1' },
                });

                await browser.click('.CartButton');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'open-check-out-form-from-cart',
                    params: { __ym, ecom_spa: 1 },
                });

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillCart(browser);
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'send-check-out-form-from-cart',
                    params: { payment: 'online', __ym, ecom_spa: 1 },
                });

                await browser.yaWaitForVisible('.EcomOrderSuccess');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-success-from-cart',
                    params: { payment: 'online', __ym, ecom_spa: 1 },
                });
                await browser.yaAssertLastMetrikaGoal(53911873, ['typ-registration', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);
            });

            hermione.skip.in(['iphone'], 'Нативных оплат на ios нет');
            it('Оформление заказа, оплата картой, неуспешно', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'cart',
                    service: 'ymturbo.t-dir.com',
                    query: { mock_payment_requests_throw: '1' },
                });

                await browser.click('.CartButton');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'open-check-out-form-from-cart',
                    params: { __ym, ecom_spa: 1 },
                });

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillCart(browser);
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'send-check-out-form-from-cart',
                    params: {
                        payment: 'online',
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaWaitForVisible('.CartMeta');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-fail-from-cart',
                    params: {
                        payment: 'online',
                        error: 'UNKNOWN',
                        __ym,
                        ecom_spa: 1,
                    },
                });
            });

            hermione.skip.in(['iphone'], 'Нативных оплат на ios нет');
            it('Оформление заказа, оплата картой, отмена', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'cart',
                    service: 'ymturbo.t-dir.com',
                    query: { mock_payment_requests_throw_not_started: '1' },
                });

                await browser.click('.CartButton');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'open-check-out-form-from-cart',
                    params: {
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillCart(browser);
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'send-check-out-form-from-cart',
                    params: {
                        payment: 'online',
                        __ym,
                        ecom_spa: 1,
                    },
                });

                await browser.yaWaitForVisible('.CartMeta');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-fail-from-cart',
                    params: {
                        payment: 'online',
                        error: 'UNKNOWN',
                        __ym,
                        ecom_spa: 1,
                    },
                });
            });

            it('Оформление заказа, изменение данных в форме', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart', service: 'ymturbo.t-dir.com' });

                await browser.click('.CartButton');

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillCart(browser, false);

                await browser.yaScrollPage('#courier_2-delivery+label', 0.3);
                await browser.click('#courier_2-delivery+label');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-focus-field-name',
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-focus-field-customer_phone',
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-focus-field-customer_email',
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-change-field-delivery',
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'user-addresses-form-focus-field-locality',
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'user-addresses-form-focus-field-street',
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'user-addresses-form-focus-field-building',
                });
                await browser.yaScrollPage('[for="cash-payment_method"]', 0.3);
                await browser.click('[for="cash-payment_method"]');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-change-field-payment_method',
                });
            });

            it('Оформление заказа, Кнопка "Оформить заказ" попала в видимую область', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartButton');

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-submit-button-in-viewport',
                });
            });

            it('Оформление заказа, Сработала валидация', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartButton');
                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await browser.yaScrollPage('.CartForm-SubmitButton', 0.3);
                await browser.click('.CartForm-SubmitButton');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-validation-error',
                });
            });

            it('Оформление заказа, Возврат в корзину по хлебной крошке', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await browser.click('.CartButton');
                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await browser.click('.ScreenHeaderBack');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-back-to-cart',
                });
            });

            it('Оформление заказа, Сработало автозаполнение с Blackbox', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'cart',
                    query: { patch: 'setBlackboxData' },
                });

                await browser.click('.CartButton');
                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-load-data-from-storage',
                });
            });

            it('Отправляются параметры визита после загруки данных', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });

                await stubVisitParams(browser);

                await browser.click('.BottomBar-Item_type_cart');
                await browser.yaWaitForVisible('.CartHead');

                await browser.pause(1000);
                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_cart: 1,
                        turbo_app_enabled: 1,
                    },
                }]);

                const [sentParams, receivedParams] = await getVisitParams(browser);
                assert.deepEqual(sentParams, receivedParams);
            });
        });

        describe('Пользовательские адреса', () => {
            describe('Без адресов', () => {
                it('Редактирование адреса', async function() {
                    const browser = this.browser;
                    await browser.yaOpenEcomSpa({
                        service: 'spideradio.github.io',
                        pageType: 'cart',
                        expFlags: {
                            turboforms_endpoint: '/multiple/',
                            'analytics-disabled': '0',
                        },
                    });

                    await browser.yaWaitForVisible('.ScreenContent');
                    await browser.yaScrollPage('.CartButton');
                    await browser.click('.CartButton');
                    await browser.yaWaitForVisible('.ShippingAddressFields');

                    await browser.yaScrollPage('input[name="locality"]');
                    await browser.setValue('input[name="locality"]', 'locality');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-locality',
                    });

                    await browser.click('.Title'); // кликаем вне инпута чтобы сбросить выпадашку

                    await browser.yaScrollPage('input[name="street"]');
                    await browser.setValue('input[name="street"]', 'street');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-street',
                    });

                    await browser.yaScrollPage('input[name="building"]');
                    await browser.setValue('input[name="building"]', '22');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-building',
                    });

                    await browser.yaScrollPage('input[name="room"]');
                    await browser.setValue('input[name="room"]', '21');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-room',
                    });

                    await browser.yaScrollPage('input[name="entrance"]');
                    await browser.setValue('input[name="entrance"]', '12');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-entrance',
                    });

                    await browser.yaScrollPage('input[name="floor"]');
                    await browser.setValue('input[name="floor"]', '33');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-floor',
                    });

                    await browser.yaScrollPage('input[name="intercom"]');
                    await browser.setValue('input[name="intercom"]', '32');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-intercom',
                    });
                });
            });

            describe('Предзаполненные адреса', () => {
                beforeEach(async function() {
                    const browser = this.browser;
                    await browser.yaOpenEcomSpa({
                        service: 'spideradio.github.io',
                        pageType: 'cart',
                        query: {
                            patch: ['setUserAddressesData'],
                        },
                        expFlags: {
                            turboforms_endpoint: '/multiple/',
                            'analytics-disabled': '0',
                        },
                    });

                    await browser.yaWaitForVisible('.ScreenContent');
                    await browser.yaScrollPage('.CartButton');
                    await browser.click('.CartButton');
                    await browser.yaWaitForVisible('.ShippingAddresses');
                });

                it('Форма загрузилась с адресами', async function() {
                    const browser = this.browser;
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'checkout-addresses-got-from-server',
                        params: {
                            count: 4,
                            hasPassport: 1,
                            hasMarket: 1,
                        },
                    });
                });

                it('Выбор адреса', async function() {
                    const browser = this.browser;
                    await browser.yaWaitForVisible('.ShippingAddresses');
                    await browser.yaScrollPage('.ShippingAddresses .CartForm-RadioItem:nth-child(4) label', 0.3);
                    await browser.click('.ShippingAddresses .CartForm-RadioItem:nth-child(4) label');

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-clicked-on-bullet',
                    });
                });

                it('Клик по кнопке "Изменить"', async function() {
                    const browser = this.browser;
                    await browser.yaWaitForVisible('.ShippingAddresses');
                    await browser.pause(1000);
                    await browser.yaScrollPage('.ShippingAddresses .CartForm-RadioItem:nth-child(3) .ShippingAddresses-ChangeLink', 0.3);
                    await browser.click('.ShippingAddresses .CartForm-RadioItem:nth-child(3) .ShippingAddresses-ChangeLink');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-clicked-on-change',
                    });
                });

                it('Клик по кнопке "Добавить новый адрес"', async function() {
                    const browser = this.browser;
                    await browser.yaWaitForVisible('.ShippingAddresses');
                    await browser.yaScrollPage('.ShippingAddresses .ShippingAddresses-NewAddress .Link', 0.3);
                    await browser.click('.ShippingAddresses .ShippingAddresses-NewAddress .Link');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-clicked-on-new',
                    });
                });

                it('Редактирование адреса', async function() {
                    const browser = this.browser;
                    await browser.yaWaitForVisible('.ShippingAddresses');
                    await browser.yaScrollPage('.ShippingAddresses .ShippingAddresses-NewAddress', 0.3);
                    await browser.click('.ShippingAddresses .ShippingAddresses-NewAddress .Link');
                    await browser.yaWaitForVisible('.AddressForm');

                    await browser.yaScrollPage('input[name="locality"]');
                    await browser.setValue('input[name="locality"]', 'locality');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-locality',
                    });

                    await browser.click('.Title'); // кликаем вне инпута чтобы сбросить выпадашку

                    await browser.yaScrollPage('input[name="street"]');
                    await browser.setValue('input[name="street"]', 'street');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-street',
                    });

                    await browser.yaScrollPage('input[name="building"]');
                    await browser.setValue('input[name="building"]', '22');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-building',
                    });

                    await browser.yaScrollPage('input[name="room"]');
                    await browser.setValue('input[name="room"]', '21');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-room',
                    });

                    await browser.yaScrollPage('input[name="entrance"]');
                    await browser.setValue('input[name="entrance"]', '12');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-entrance',
                    });

                    await browser.yaScrollPage('input[name="floor"]');
                    await browser.setValue('input[name="floor"]', '33');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-floor',
                    });

                    await browser.yaScrollPage('input[name="intercom"]');
                    await browser.setValue('input[name="intercom"]', '32');
                    await browser.pause(1000);
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-form-focus-field-intercom',
                    });
                });

                it('Сохранение паспортного адреса', async function() {
                    const browser = this.browser;
                    await browser.yaScrollPage('.ShippingAddresses .CartForm-RadioItem:nth-child(4) .ShippingAddresses-ChangeLink', 0);
                    await browser.click('.ShippingAddresses .CartForm-RadioItem:nth-child(4) .ShippingAddresses-ChangeLink');
                    await browser.yaWaitForVisibleWithinViewport('.AddressForm');

                    await browser.yaMockFetch({
                        urlDataMap: {
                            'address/': JSON.stringify({
                                address: { id: 'randomId' },
                                status: 'success',
                            }),
                        },
                    });
                    await browser.setValue('input[name=building]', '911');
                    await browser.click('.AddressForm-FormActions button');
                    // Ждем анимацию возврата
                    await browser.yaWaitForVisible(PO.blocks.ecomCartForm(), 3000);

                    await browser.yaScrollPage('.ShippingAddresses .CartForm-RadioItem:nth-child(1) .ShippingAddresses-ChangeLink', 0.3);

                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-saved',
                    });
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'user-addresses-passport-saved',
                    });
                });
            });
        });

        describe('Страница товара', () => {
            it('Показ страницы', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await browser.yaWaitForVisible('.EcomScreen_type_product .Slider');
                await browser.yaAssertLastMetrikaHit(53911873, ['https://spideradio.github.io/?rnd=c7hj4fr', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_product_card: 1,
                        turbo_app_enabled: 1,
                    },
                }]);
            });

            it('Добавление и переход в корзину', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await browser.yaScrollPage('.ProductScreen-Actions-Button_toCart', 0.3);
                await browser.click('.ProductScreen-Actions-Button_toCart');
                await browser.yaWaitForVisible('.ProductScreen-Actions-Button_inCart', 'Товар не добавился в корзину');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'add-to-cart',
                    params: { __ym, ecom_spa: 1 }
                });

                await browser.click('.ProductScreen-Actions-Button_inCart');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'open-cart-from-product',
                    params: { __ym, ecom_spa: 1 }
                });

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_cart: 1,
                        turbo_app_enabled: 1,
                    },
                }]);
            });

            it('Быстрый заказ', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await browser.yaScrollPage('.ScreenContent-Inner .ProductScreen-Actions-Button_oneClick', 0.3);
                await browser.click('.ScreenContent-Inner .ProductScreen-Actions-Button_oneClick');
                await browser.yaAssertLastMetrikaGoal(53911873, ['open-check-out-form', {
                    __ym,
                    ecom_spa: 1,
                    turbo_app_enabled: 1,
                }]);

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillOneClickCart(browser);

                await browser.yaWaitForVisible('.EcomOrderSuccess');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'check-out-form-success',
                    params: { payment: 'cash', __ym, ecom_spa: 1 },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'typ-registration',
                    params: { __ym, ecom_spa: 1 },
                });
            });

            hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
            it('Быстрый заказ, альбомная ориентация', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                });

                await browser.setOrientation('landscape');
                await browser.yaScrollPage('.ScreenContent-Inner .ProductScreen-Actions-Button_oneClick', 0.3);
                await browser.click('.ScreenContent-Inner .ProductScreen-Actions-Button_oneClick');

                await browser.yaWaitForVisible('.EcomScreen_type_cart .EcomCartForm');
                await browser.yaWaitForHidden('.NavigationTransition_state_entering');

                await fillOneClickCart(browser);

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'send-check-out-form',
                    params: { orientation: 'landscape', payment: 'cash', __ym, ecom_spa: 1 },
                });
            });

            it('Клик на бейдж проверено маркетом', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: { product_id: 107, pcgi: 'rnd=c7hj4fr' },
                    expFlags: { 'turbo-app-tested-by-market': 1 },
                });

                await browser.yaWaitForVisible('.YandexMarketCheckLink');
                await browser.yaScrollPage('.YandexMarketCheckLink', 0);
                await browser.click('.YandexMarketCheckLink');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'click-market-check-link',
                    params: {
                        __ym,
                        ecom_spa: 1,
                        page: 'ProductScreen',
                    },
                });
            });

            it('Отправляются параметры визита после загруки данных', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });

                await stubVisitParams(browser);

                await browser.yaScrollPage('.ProductList', 0.3);
                await browser.click('.ProductItem a');

                await browser.yaWaitForVisible('.NavigationTransition_state_entered .EcomScreen_type_product');

                await browser.pause(2000);
                const url = new URL(await browser.getUrl());
                const rnd = url.pathname.split('/').pop();

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, [`https://spideradio.github.io/rnd/${rnd}`, {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_product_card: 1,
                        turbo_app_enabled: 1,
                    },
                }]);

                const [sentParams, receivedParams] = await getVisitParams(browser);

                assert.deepEqual(sentParams, receivedParams);
            });
        });

        describe('Страница фильтров', () => {
            beforeEach(async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'filters' });
            });

            hermione.only.in('chrome-phone');
            it('Показ страницы', async function() {
                const browser = this.browser;

                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                }]);
            });

            hermione.only.in('chrome-phone');
            it('Применение фильтров', async function() {
                const browser = this.browser;

                await browser.click('.EcomListFilter-Action button');
                await browser.yaAssertLastMetrikaGoal(53911873, ['apply-filters', {
                    __ym,
                    ecom_spa: 1,
                    turbo_app_enabled: 1,
                }]);

                await browser.yaWaitMetrikaHitsLength('Второй хит не был отправлен', 53911873, 2);
                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: {
                        __ym,
                        ecom_spa: 1,
                        ecom_catalog: 1,
                        turbo_app_enabled: 1,
                    },
                }]);
            });
        });

        describe('О магазине', () => {
            it('Показ страницы', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'about' });

                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                }]);
            });

            it('Клик на бейдж проверено маркетом', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'about',
                    expFlags: { 'turbo-app-tested-by-market': 1 },
                });

                await browser.yaWaitForVisible('.YandexMarketCheckLink');
                await browser.yaScrollPage('.YandexMarketCheckLink', 0);
                await browser.click('.YandexMarketCheckLink .Link');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'click-market-check-link',
                    params: {
                        __ym,
                        ecom_spa: 1,
                        page: 'AboutShopScreen',
                    },
                });
            });
        });

        describe('О магазине (подробности)', () => {
            it('Показ страницы', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'about_detail' });

                await browser.yaAssertLastMetrikaHit(53911873, ['http://spideradio.github.io', {
                    referer: 'https://yandex.ru',
                    title: await browser.getTitle(),
                    params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                }]);
            });
        });

        describe('Goals', () => {
            hermione.only.notIn('iphone', 'Нативный залогин временно откючен на iphone');
            it('Отправляются при клике в BottomBar', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'about' });
                await stubAuthorization(browser);

                await browser.click('.BottomBar-Item_type_main');
                await browser.yaAssertLastMetrikaGoal(53911873, ['navbar-main', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);

                await browser.click('.BottomBar-Item_type_catalog');
                await browser.yaAssertLastMetrikaGoal(53911873, ['navbar-catalog', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);

                await browser.click('.BottomBar-Item_type_cart');
                await browser.yaAssertLastMetrikaGoal(53911873, ['navbar-cart', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);

                await browser.click('.BottomBar-Item_type_about');
                await browser.yaAssertLastMetrikaGoal(53911873, ['navbar-about', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);

                await browser.click('.BottomBar-Item_type_user');
                await browser.yaAssertLastMetrikaGoal(53911873, ['navbar-auth', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);
            });

            it('Отправляется корректная цель при клике в иконку профиля, если пользователь залогинен', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'about',
                    query: { patch: 'setBlackboxData' }
                });

                await browser.click('.BottomBar-Item:nth-child(5)');
                await browser.yaAssertLastMetrikaGoal(53911873, ['navbar-profile', { __ym, ecom_spa: 1, turbo_app_enabled: 1 }]);
            });

            hermione.only.notIn('iphone', 'Страница рейтинга не работает на ios < 12');
            it('Отправляются при клике в блок с отзывами', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'about',
                    query: {
                        patch: 'setShopReviewsData',
                        shopReviews: '123,4.5',
                    },
                    expFlags: {
                        'analytics-disabled': '0',
                    },
                });

                await browser.yaWaitForVisible('.ShopReviewsLink');
                await browser.click('.ShopReviewsLink');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'shop-review-button-click',
                    params: { ecom_spa: 1, __ym: { turbo_page: 1, doc_ui: 'touch-phone' } },
                });
            });

            it('Отправляются при клике добавлении/удалении товара в избранное', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: 107,
                        pcgi: 'rnd=c7hj4fr',
                        patch: 'setBlackboxData',
                    },
                });

                await browser.yaWaitForVisible('.FavoriteButton');

                await browser.yaMockFetch({
                    urlDataMap: {
                        '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                        '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
                    },
                });

                await browser.click('.FavoriteButton');
                await browser.yaAssertLastMetrikaGoal(53911873, ['save-favorite-from-product', {
                    __ym,
                    ecom_spa: 1,
                    turbo_app_enabled: 1,
                }]);

                await browser.yaWaitForVisible('.FavoriteButton_saved');
                await browser.click('.FavoriteButton_saved');

                await browser.waitForExist('.FavoriteButton_saved', 1000, true);
                await browser.yaAssertLastMetrikaGoal(53911873, ['delete-favorite-from-product', {
                    __ym,
                    ecom_spa: 1,
                    turbo_app_enabled: 1,
                }]);
            });

            it('Отправляются при добавлении/удалении в избранное в корзине', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'cart',
                    query: {
                        patch: 'setBlackboxData',
                    },
                });
                await browser.yaWaitForVisible('.FavoriteButton');

                await browser.yaMockFetch({
                    urlDataMap: {
                        '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                        '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
                    },
                });

                await browser.click('.FavoriteButton');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'save-favorite-from-cart',
                });

                await browser.yaWaitForVisible('.FavoriteButton_saved');
                await browser.click('.FavoriteButton_saved');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'delete-favorite-from-cart',
                });
            });

            it('Отправляются при добавлении/удалении в избранное из листинга', async function() {
                const browser = this.browser;
                await openPage({
                    browser,
                    pageType: 'catalog',
                    query: {
                        category_id: 9,
                        patch: 'setBlackboxData',
                    },
                });

                await browser.yaWaitForVisible('.FavoriteButton');

                await browser.yaMockFetch({
                    urlDataMap: {
                        '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                        '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
                    },
                });

                await browser.click('.FavoriteButton');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'save-favorite-from-item',
                });

                await browser.yaWaitForVisible('.FavoriteButton_saved');
                await browser.click('.FavoriteButton_saved');

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'delete-favorite-from-item',
                });
            });

            hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
            it('Отправляется при смене ориентации', async function() {
                const browser = this.browser;
                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                    expFlags: {
                        'analytics-disabled': '0',
                    },
                });
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'init-screen-orientation',
                    params: {
                        portrait: 1,
                    },
                });
                await browser.setOrientation('landscape');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'change-screen-orientation',
                    params: {
                        landscape: 1,
                    },
                });
                await browser.setOrientation('portrait');
                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'change-screen-orientation',
                    params: {
                        portrait: 1,
                    },
                });
            });

            describe('Под флагом turbo-app-bottombar', () => {
                hermione.only.notIn('iphone', 'Нативный залогин временно откючен на iphone');
                it('Отправляются при клике в BottomBar', async function() {
                    const browser = this.browser;
                    await openPage({
                        browser,
                        pageType: 'main',
                        expFlags: { 'turbo-app-bottombar': 1 },
                    });

                    await stubAuthorization(browser);

                    await browser.click('.BottomBar-Item_type_catalog');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'navbar-catalog',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });

                    await browser.click('.BottomBar-Item_type_favorites');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'navbar-favorites-auth',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });

                    await browser.click('.BottomBar-Item_type_cart');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'navbar-cart',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });

                    await browser.click('.BottomBar-Item_type_about');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'navbar-about',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });
                });

                it('Отправляется при клике в избранное для залогина', async function() {
                    const browser = this.browser;
                    await openPage({
                        browser,
                        pageType: 'main',
                        query: { patch: 'setBlackboxData' },
                        expFlags: { 'turbo-app-bottombar': 1 },
                    });

                    await browser.click('.BottomBar-Item_type_favorites');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'navbar-favorites',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });
                });

                it('Отправляется при клике в профиль в шапке для залогина', async function() {
                    const browser = this.browser;
                    await openPage({
                        browser,
                        pageType: 'main',
                        query: { patch: 'setBlackboxData' },
                        expFlags: { 'turbo-app-bottombar': 1 },
                    });

                    await browser.click('.UserIcon');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'open-profile-from-header',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });
                });

                hermione.only.notIn('iphone', 'Нативный залогин временно откючен на iphone');
                it('Отправляется при клике в профиль в шапке для незалогина', async function() {
                    const browser = this.browser;
                    await openPage({
                        browser,
                        pageType: 'main',
                        expFlags: { 'turbo-app-bottombar': 1 },
                    });

                    await stubAuthorization(browser);

                    await browser.click('.UserIcon');
                    await browser.yaCheckMetrikaGoal({
                        counterId: 53911873,
                        name: 'auth-from-header',
                        params: { __ym, ecom_spa: 1, turbo_app_enabled: 1 },
                    });
                });
            });
        });

        describe('Параметр визита для показа рейтинга/отзывов на товар', () => {
            it('Отправляется с главной страницы, если в карусели есть товары с рейтингом', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart', service: 'bealab.ru' });
                await stubVisitParams(browser);

                await browser.click('.BottomBar-Item_type_main');
                await browser.yaWaitForVisible('.ScreenContent');

                await browser.yaWaitUntil(
                    'Параметр "shown-product-review-feat" не был отправлен',
                    async() => {
                        const [sentParams] = await getVisitParams(browser);
                        return sentParams.some(params => (
                            JSON.stringify(params) === '{"shown-product-review-feat":1}')
                        );
                    },
                    5000,
                    1000,
                );
            });

            it('Не отправляется с главной страницы, если в карусели нет товаров с рейтингом', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });

                await stubVisitParams(browser);

                await browser.click('.BottomBar-Item_type_main');
                await browser.yaWaitForVisible('.ScreenContent');

                await browser.yaWaitUntil(
                    'Параметр "shown-product-review-feat" отправился',
                    async() => {
                        const [sentParams] = await getVisitParams(browser);
                        return sentParams.every(params => (
                            JSON.stringify(params) !== '{"shown-product-review-feat":1}')
                        );
                    },
                    5000,
                    1000,
                );
            });

            it('Отправляется с листинга, если есть товары с рейтингом', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart', service: 'bealab.ru' });
                await stubVisitParams(browser);

                await browser.click('.BottomBar-Item_type_catalog');
                await browser.yaWaitForVisible('.ScreenContent');

                await browser.yaWaitUntil(
                    'Параметр "shown-product-review-feat" не был отправлен',
                    async() => {
                        const [sentParams] = await getVisitParams(browser);
                        return sentParams.some(params => (
                            JSON.stringify(params) === '{"shown-product-review-feat":1}')
                        );
                    },
                    5000,
                    1000,
                );
            });

            it('Не отправляется с листинга, если нет товаров с рейтингом', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'cart' });
                await stubVisitParams(browser);

                await browser.click('.BottomBar-Item_type_catalog');
                await browser.yaWaitForVisible('.ScreenContent');

                await browser.yaWaitUntil(
                    'Параметр "shown-product-review-feat" отправился',
                    async() => {
                        const [sentParams] = await getVisitParams(browser);
                        return sentParams.every(params => (
                            JSON.stringify(params) !== '{"shown-product-review-feat":1}')
                        );
                    },
                    5000,
                    1000,
                );
            });

            it('Отправляется с карточки товара, если есть рейтинг и отзывы', async function() {
                const productItemSelector = '.ShopInfo + .ProductList .ProductItem';
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });
                await stubVisitParams(browser);

                await browser.yaWaitForVisible(productItemSelector);
                await browser.yaScrollPage(productItemSelector, 0.3);

                await browser.yaMockFetch({
                    status: 200,
                    delay: 200,
                    urlDataMap: {
                        '&isAjax=true': JSON.stringify({
                            entities: {
                                products: {
                                    '212': {
                                        reviews: {
                                            link: 'https://reviews.yandex.ru/ugcpub/object-digest',
                                            count: 120,
                                        },
                                        rating: 5,
                                    },
                                },
                            },
                            pagesMeta: {},
                        }),
                    },
                });

                await browser.click(productItemSelector);

                await browser.yaWaitUntil(
                    'Параметр "shown-product-review-feat" не был отправлен',
                    async() => {
                        const [sentParams] = await getVisitParams(browser);
                        return sentParams.some(params => (
                            JSON.stringify(params) === '{"shown-product-review-feat":1}')
                        );
                    },
                    5000,
                    1000,
                );
            });

            it('Не отправляется с карточки товара, если нет рейтинга и отзывов', async function() {
                const browser = this.browser;
                await openPage({ browser, pageType: 'main' });
                await stubVisitParams(browser);

                await browser.yaWaitForVisible('.ProductItem');
                await browser.yaScrollPage('.ProductItem', 0.3);

                await browser.click('.ProductItem');

                await browser.yaWaitUntil(
                    'Параметр "shown-product-review-feat" отправился',
                    async() => {
                        const [sentParams] = await getVisitParams(browser);
                        return sentParams.every(params => (
                            JSON.stringify(params) !== '{"shown-product-review-feat":1}')
                        );
                    },
                    5000,
                    1000,
                );
            });
        });
    });
});
