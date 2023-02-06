const URL = require('url');
const { assert } = require('chai');

const __ym = {
    turbo_page: 1,
    doc_ui: 'touch-phone',
};

describe('Ecom-tap', function() {
    it('Внешний вид страницы листинга', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            bottomBar: 'hide',
            expFlags: {
                'turbo-app-new-template': 1,
            },
        });

        await browser.yaMockImages();
        await browser.assertView('page', ['.Cover', '.ScreenContent']);
    });

    it('Сохраняет фильтры при шаге назад', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'super01.ru',
            pageType: 'catalog',
            query: { category_id: 13292 },
        });

        function getFiltersLength() { return document.querySelectorAll('.CategoryList-ItemContainer').length }

        await browser.yaMockImages();
        await browser.assertView('category-list-initial', '.CategoryList');

        await browser.click('.CategoryList-ItemMore');
        await browser.yaWaitForHidden('.CategoryList-ItemMore', 'Не скрылась кнопка "показать еще"');
        await browser.assertView('category-list-opened', '.CategoryList');
        const { value: totalFiltersLength } = await browser.execute(getFiltersLength);

        await browser.click('.ProductItem');
        await browser.yaWaitForVisible('.EcomScreen_type_product .Slider', 'Не загрузился товар');

        await browser.back();
        await browser.yaWaitForVisible('.CategoryList', 'Не показались категории');
        const { value: totalFiltersLengthAfterReturn } = await browser.execute(getFiltersLength);

        assert.strictEqual(totalFiltersLengthAfterReturn, totalFiltersLength, 'Фильтры не раскрылись при шаге назад');
    });

    it('Сохраняет фильтры при перезагрузке', async function() {
        const { browser } = this;
        await browser.yaOpenEcomSpa({
            service: 'super01.ru',
            pageType: 'catalog',
            query: { category_id: 13292 },
        });

        function getFiltersLength() { return document.querySelectorAll('.CategoryList-ItemContainer').length }

        await browser.yaMockImages();
        await browser.assertView('category-list-initial', '.CategoryList');

        await browser.click('.CategoryList-ItemMore');
        await browser.yaWaitForHidden('.CategoryList-ItemMore', 'Не скрылась кнопка "показать еще"');
        await browser.assertView('category-list-opened', '.CategoryList');
        const { value: totalFiltersLength } = await browser.execute(getFiltersLength);

        const { value: reqid } = await browser.execute(function() { return window.KOTIK_REQID });
        await browser.execute(function() { location.reload() });
        await browser.yaWaitUntil('Страница не перезагрузилась', async() => {
            const { value: newReqid } = await browser.execute(function() { return window.KOTIK_REQID });
            return newReqid !== reqid;
        });

        await browser.yaWaitForVisible('.CategoryList', 'Не показались категории');
        const { value: totalFiltersLengthAfterReload } = await browser.execute(getFiltersLength);

        assert.strictEqual(totalFiltersLengthAfterReload, totalFiltersLength, 'Фильтры не раскрылись при перезагрузке');
    });

    it('Отображается соответствующий текст если ничего не найдено', function() {
        return this.browser
            .yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { query: 'кккк' },
            })
            .yaWaitForVisibleWithinViewport('.ProductList-Empty')
            .yaShouldBeVisible('.ProductList-Empty', 'Текст "Ни чего не найденно не появился" не появился')
            .assertView('plain', '.ProductList-Empty');
    });

    it('Показывается заглушка при отсутствии фотографий товара', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: '19.digitalserv.ru',
            pageType: 'catalog',
            query: { category_id: 3320 },
            expFlags: { 'disable-image-proxy': 1 },
        });

        // Проверяем внешний вид карточки товара
        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaScrollPage('.ProductItem', 0.3);
        await browser.assertView('product-item', '.ProductItem');
    });

    describe('Опечаточник', function() {
        it('Появляется при вводе запроса с опечатками', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { query: 'поисковой запрос в котором есть опичатка' },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.misspell());
            await browser.yaShouldBeVisible(PO.blocks.misspell(), 'Опечаточник не появился');
            await browser.assertView('plain', PO.blocks.misspell());
        });

        it('Заменяет текст в поисковой строке на исправленный', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { query: 'фореоп' },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.misspell());
            await browser.yaShouldBeVisible(PO.blocks.misspell(), 'Опечаточник не появился');
            const elementText = await browser.getValue('.Textinput-Control');
            assert.strictEqual(elementText, 'фаркоп', 'Текст в поисковой строке не заменился');
        });

        it('Кнопка отменить работает корректно', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { query: 'фореоп' },
            });

            await browser.yaWaitForVisibleWithinViewport(PO.blocks.misspell());
            await browser.yaMockFetch({
                status: 200,
                delay: 100,
                urlDataMap: {
                    '&isAjax=true': '{}',
                },
            });
            await browser.click('.Misspell-Link');
            await browser.yaWaitForHidden(PO.blocks.misspell());
            await browser.yaShouldNotBeVisible(PO.blocks.misspell(), 'Опечаточник должен исчезнуть');
            await browser.yaWaitUntil(
                'Текст в поисковой строке не заменился',
                async function() {
                    const elementText = await browser.getValue('.Textinput-Control');
                    assert.strictEqual(elementText, 'фореоп', 'Текст в поисковой строке не заменился');

                    return true;
                }
            );
        });
    });

    describe('Скелеты товаров', function() {
        it('Внешний вид', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { hermione_autoload: 0 },
            });

            await browser.yaMockFetch({
                status: 200,
                delay: 3000,
                urlDataMap: {
                    page: '{}',
                },
            });

            await browser.yaWaitForHidden('.CategoryList_skeleton');
            await browser.yaScrollPage('.ProductItem:last-child', 0);

            await browser.assertView('skeleton', '.ProductItem_skeleton');
        });

        it('Должно быть не больше 4х на странице', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                query: {
                    category_id: 5871,
                    hermione_autoload: 0,
                },
            });

            await browser.yaMockFetch({
                status: 200,
                delay: 3000,
                urlDataMap: {
                    page: '{}',
                },
            });

            await browser.yaWaitForHidden('.CategoryList_skeleton');
            await browser.yaScrollPage('.ProductItem:last-child', 0);

            const { value } = await browser.execute(function() {
                const skeletonsCount = window.document.querySelectorAll('.ProductItem_skeleton').length;
                return skeletonsCount;
            });

            assert.strictEqual(value, 4);
        });

        it('Не должно быть на странице если все товары загруженны', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'super01.ru',
                pageType: 'catalog',
                query: {
                    category_id: 5871,
                    hermione_autoload: 0,
                },
            });

            await browser.yaMockFetch({
                status: 200,
                delay: 0,
                urlDataMap: {
                    page: '{}',
                },
            });

            await browser.yaWaitForHidden('.CategoryList_skeleton');
            await browser.click('.CategoryList-ItemContainer:last-child');
            await browser.yaWaitForHidden('.CategoryList_skeleton');
            await browser.yaScrollPage('.ProductItem:last-child', 0);
            await browser.yaShouldNotBeVisible('.ProductItem_skeleton', 'На странице не должно быть скелетов для товаров');
        });
    });

    it('Показывает попап при добавлении с листинга', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            expFlags: {
                turboforms_endpoint: '/empty/',
            },
        });
        await browser.yaIndexify('.ProductItem-Action');

        // добавляем первый товар
        await browser.yaScrollPage('.ProductItem-Action[data-index="0"]', 0.3);
        await browser.click('.ProductItem-Action[data-index="0"]');
        // Скролим в самый низ страницы, чтобы скриншотить попап на белом фоне.
        await browser.yaScrollPageToBottom();
        await browser.yaWaitForVisible('.BottomBar-ItemPopup', 2000, 'Попап не показался');
        await browser.assertView('bottom-bar-popup', ['.BottomBar-Item_type_cart', '.BottomBar-ItemPopup']);
        await browser.yaWaitForHidden('.BottomBar-ItemPopup', 5000, 'Попап не скрылся');
        await browser.yaWaitForVisible(
            '.BottomBar-Item_type_cart .CountBadge_visible',
            2000, 'Бейджик не показался',
        );
        await browser.assertView('bottom-bar-badge', [
            '.BottomBar-Item_type_cart',
            '.BottomBar-Item_type_cart .CountBadge_visible',
        ]);

        // добавляем второй и третий товар
        await browser.yaScrollPage('.ProductItem-Action[data-index="2"]', 0.3);
        await browser.click('.ProductItem-Action[data-index="1"]');
        await browser.click('.ProductItem-Action[data-index="2"]');
        await browser.yaWaitForVisible('.BottomBar-ItemPopup', 2000, 'Попап не показался');
        await browser.yaWaitForHidden('.BottomBar-ItemPopup', 5000, 'Попап не скрылся');
        await browser.yaWaitForVisible(
            '.BottomBar-Item_type_cart .CountBadge_visible',
            2000, 'Бейджик не показался',
        );
        const badgeText = await browser.getText('.BottomBar-Item_type_cart .CountBadge_visible');
        assert.strictEqual(badgeText, '3', 'Количество товаров на бейджике не соответствует действительности');

        // в четвёртый раз попап не должен показаться
        await browser.yaScrollPage('.ProductItem-Action[data-index="3"]', 0.3);
        await browser.click('.ProductItem-Action[data-index="3"]');

        let didPopupFired = true;
        try {
            await browser.yaWaitForVisible('.BottomBar-ItemPopup', 2000, 'Попап не показался');
        } catch (e) {
            didPopupFired = false;
        }

        assert.isFalse(didPopupFired, 'Попап показался в третий раз, хотя не должен был');
    });

    it('Пробрасывать параметр для рекомендаций в следующие страницы', async function() {
        let host;
        const browser = this.browser;

        await browser.url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                []
            ));

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: {
                hermione_autoload: 0,
                patch: 'setTurboSessionId',
            },
        });

        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaScrollPage('.ProductItem:last-child', 0);

        await browser.yaWaitUntil(
            'не ушел запрос за второй страницей',
            () => browser
                .yaGetExternalResourcesRequests(`https://${host}/turbo/spideradio.github.io/n/yandexturbocatalog/`)
                .then(requests => {
                    const requestBySecondPage = requests.find(({ url }) => url.indexOf('page=1') !== -1);

                    if (!requestBySecondPage) {
                        return false;
                    }

                    assert.include(
                        requestBySecondPage.url,
                        'turbo_listing_session_id=some_id_0',
                        'В запросе должен быть параметр'
                    );

                    return true;
                })
        );

        await browser.yaWaitForVisible('.ProductItem:nth-child(11)', 3000);
        await browser.yaScrollPage('.ProductItem:last-child', 0);

        await browser.yaWaitUntil(
            'не ушел запрос за третьей страницей',
            () => browser
                .yaGetExternalResourcesRequests(`https://${host}/turbo/spideradio.github.io/n/yandexturbocatalog/`)
                .then(requests => {
                    const requestBySecondPage = requests.find(({ url }) => url.indexOf('page=2') !== -1);

                    if (!requestBySecondPage) {
                        return false;
                    }

                    assert.include(
                        requestBySecondPage.url,
                        'turbo_listing_session_id=some_id_0',
                        'В запросе должен быть параметр'
                    );

                    return true;
                })
        );
    });

    it('Избранное в листинге', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            expFlags: { turboforms_endpoint: '/multiple/' },
            query: {
                category_id: 9,
                patch: 'setBlackboxData',
            },
        });

        await browser.yaWaitForVisible('.ProductItem:first-child');

        await browser.yaMockImages();
        await browser.yaMockFetch({
            urlDataMap: {
                '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
            },
        });

        await browser.assertView('plain', '.ProductItem:first-child');

        await browser.click('.ProductItem:first-child .FavoriteButton');
        await browser.assertView('saved', '.ProductItem:first-child');

        await browser.click('.ProductItem:first-child .FavoriteButton');
        await browser.assertView('deleted', '.ProductItem:first-child');
    });

    it('Сворачиваются категории если их больше 30', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'moskva.satom.ru',
            pageType: 'catalog',
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaWaitForVisible('.CategoryList-ItemMore');

        const { value } = await browser.execute(() => {
            return document.querySelectorAll('.CategoryList-ItemContainer').length;
        });

        assert.equal(value, 3, 'Категории не свернуты');
    });

    it('Скрытие кол-ва товаров в категориях в листинге', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            expFlags: { 'turbo-app-hide-category-count': 1 },
        });

        await browser.assertView('categories', '.CategoryList', {
            ignoreElements: ['.EcomBottomBar'],
        });
    });

    hermione.only.notIn('iphone', 'Страница отзывов не работает на iphone < 12');
    it('Рейтинг товара', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: {
                patch: 'setProductReviews',
                productReviews: '100,4.5',
            },
            expFlags: {
                'analytics-disabled': '0',
            },
        });

        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaMockImages();

        await browser.yaScrollPage('.ProductItem', 0.3);
        await browser.assertView('product-item', '.ProductItem', {
            ignoreElements: ['.EcomBottomBar'],
        });

        await browser.click('.ProductItem');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'open-product-item',
            params: { ecom_spa: 1, __ym, rating: 4.5, review_count: 100, pos: 'thumb' },
        });
    });

    hermione.only.notIn('iphone', 'Страница отзывов не работает на iphone < 12');
    it('Рейтинг товара и кол-во отзывов в описании', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: {
                patch: 'setProductReviews',
                productReviews: '100,4.5',
            },
            expFlags: {
                'turbo-app-listing-reviews': '1',
                'turbo-app-product-list-reviews': '1',
                'analytics-disabled': '0',
            },
        });

        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaMockImages();

        await browser.yaScrollPage('.ProductItem', 0.3);
        await browser.assertView('product-item', '.ProductItem', {
            ignoreElements: ['.EcomBottomBar'],
        });

        await browser.click('.ProductItem');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'open-product-item',
            params: { ecom_spa: 1, __ym, rating: 4.5, review_count: 100, pos: 'info' },
        });
    });

    it('Блоки главной на странице пустого поиска', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            expFlags: { 'turbo-app-global-search': 1 },
            query: {
                query: 'empty123456',
            },
            bottomBar: 'hide'
        });

        await browser.yaWaitForVisible('.ProductListScreen-MainBlocks .ProductList');
        await browser.getText('.ProductListScreen-MainBlocks .ProductList:nth-child(1) .Title')
            .then(text => assert.equal(text, 'Популярные товары', 'Нет блока с названием "Популярные товары"'));

        await browser.getText('.ProductListScreen-MainBlocks .ProductList:nth-child(2) .Title')
            .then(text => assert.equal(text, 'Выгодно сегодня', 'Нет блока с названием "Выгодно сегодня"'));

        await browser.yaWaitForVisible('.GlobalProductList');
        await browser.getText('.GlobalProductList .Title')
            .then(text => assert.equal(text, 'Все товары', 'Нет блока с названием "Все товары"'));

        await browser.yaMockImages();
        await browser.assertView('global-product-list', '.GlobalProductList');
    });

    it('Рекомендательные карточки в выдаче', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: {
                patch: ['setListRecommendationCard', 'setReqid'],
            },
            expFlags: {
                'analytics-disabled': '0',
                'turbo-app-new-template': '1',
            },
        });

        await browser.yaWaitForVisible('.ProductItem');
        await browser.yaScrollPage('.ProductItem');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-list-seen',
            params: {
                host: 'spideradio.github.io',
                originalUrl: 'http://spideradio.github.io',
                place: 'listingPage',
                reqid: 'test-reqid',
                ecom_spa: 1,
                __ym,
            },
        });

        const checkProductUrl = (name, goal) => {
            const productUrl = URL.parse(goal.productUrl);
            const actualProductUrl = productUrl.pathname + productUrl.search;
            const expectedProductUrl = '/turbo/spideradio.github.io/s/rnd/c7hj4fr?utm_source=turbo_turbo';
            assert.deepEqual(
                actualProductUrl,
                expectedProductUrl,
                `Неправильная ссылка в productUrl цели "${name}"`,
            );
        };

        const seenGoal = await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-seen',
            params: {
                host: 'spideradio.github.io',
                originalUrl: 'http://spideradio.github.io',
                place: 'listingPage',
                offerId: '107',
                source: 'sm',
                ecom_spa: 1,
                reqid: 'test-reqid',
                __ym,
            },
        });
        checkProductUrl('product-item-seen', seenGoal);

        await browser.click('.ProductItem .Button.ProductItem-Action');
        const addToCartGoal = await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-click-add-to-cart',
            params: {
                host: 'spideradio.github.io',
                originalUrl: 'http://spideradio.github.io',
                place: 'listingPage',
                offerId: '107',
                source: 'sm',
                ecom_spa: 1,
                reqid: 'test-reqid',
                __ym,
            },
        });
        checkProductUrl('product-item-click-add-to-cart', addToCartGoal);

        await browser.click('.ProductItem .Link');
        const clickGoal = await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-click',
            params: {
                host: 'spideradio.github.io',
                originalUrl: 'http://spideradio.github.io',
                place: 'listingPage',
                offerId: '107',
                source: 'sm',
                ecom_spa: 1,
                reqid: 'test-reqid',
                __ym,
            },
        });
        checkProductUrl('product-item-click', clickGoal);
    });
});
