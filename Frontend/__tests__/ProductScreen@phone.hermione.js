async function getElementTop(browser, selector) {
    const result = await browser.execute(function(selector) {
        return window.document.querySelector(selector).getBoundingClientRect().top + window.pageYOffset;
    }, selector);

    return result.value;
}

describe('Ecom-tap', function() {
    it('Внешний вид страницы товара', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: { product_id: 92, pcgi: 'rnd=tvowcsnngw' },
        });

        await browser.yaMockImages();
        await browser.assertView('page', ['.Cover', '.ScreenContent']);
    });

    it('Внешний вид длинного названия на странице товара', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: { product_id: 92, pcgi: 'rnd=fwj3pt', patch: 'setLongProductTitle' },
        });

        await browser.assertView('long', '.ScreenContent h1');
    });

    it('Подскрол картинки в слайдере по параметру sliderimg в адресе страницы', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 92,
                pcgi: 'rnd=tvowcsnngw',
                sliderimg: '//avatars.mds.yandex.net/get-turbo/3322662/rthac4b88805a760cdda749cbbfe8625eff/',
            },
        });

        const activeDotIndex = await browser.yaGetElementIndex('.bullet-wrapper__bullet_active');
        assert.deepEqual(activeDotIndex, 3, 'Неверный индекс текущей точки');

        await browser.yaWaitForVisibleWithinViewport(
            '.Slider-Slide:nth-of-type(3) img',
            null,
            'Не подскролило к нужной картинке',
            true
        );
    });

    it('Не показываем слайдер при отсутствии фотографий товара', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: { product_id: 147, pcgi: 'rnd=9j2' },
        });

        await browser.yaAssertViewportView('page');
    });

    hermione.only.notIn('iphone', 'В тестовом iphone нестабильно работает подскролл');
    it('Сохранение скролла в блоке рекомендаций на странице Товара', async function() {
        const { browser } = this;
        const scrollX = 50;

        function getScrollLeft() {
            return document.querySelector('.ProductList .ProductList-Content').scrollLeft;
        }

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 92,
                pcgi: 'rnd=tvowcsnngw',
                patch: 'setRecommendationBlock',
            },
        });

        await browser.yaWaitForVisible('.EcomScreen_type_product .ProductList', 2000, 'Не появились рекомендации');
        await browser.yaScrollPage('.ProductList', 0);
        await browser.yaShouldBeScrollable('.ProductList .ProductList-Content', { h: true });
        await browser.yaScrollElement('.ProductList .ProductList-Content', scrollX);
        // Пробуем стабилизировать тест, чтобы изменение сохранилось, перед уходом со страницы
        await browser.yaWaitUntil(
            'Карусель не прокручена до нужного места первый раз',
            async() => (await browser.execute(getScrollLeft)).value === scrollX,
            2000,
        );
        const pageTitle1 = await browser.getText('.EcomScreen_type_product .ScreenContent h1');
        const pageTitle2 = await browser.getText('.ProductList-Content .ProductItem:nth-child(2) .ProductItem-Info p');
        await browser.click('.ProductList-Content .ProductItem:nth-child(2)');
        await browser.yaWaitUntil(
            'Не появилась вторая страница товара',
            async() => await browser.getText('.EcomScreen_type_product .ScreenContent h1') === pageTitle2,
            3000,
        );
        await browser.back();
        await browser.yaWaitUntil(
            'Не появилась первая страница товара после клика назад',
            async() => await browser.getText('.EcomScreen_type_product .ScreenContent h1') === pageTitle1,
            3000,
        );
        await browser.yaWaitUntil(
            'Карусель не прокручена до нужного места после возврата назад по истории',
            async() => (await browser.execute(getScrollLeft)).value === scrollX,
            3000,
        );
    });

    it('Слайдер скролится корректно после длительной загрузки карточек', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: { category_id: 9 },
        });

        await browser.yaMockFetch({
            status: 200,
            delay: 3000,
            urlDataMap: {
                '&isAjax=true': JSON.stringify({
                    entities: {
                        products: {
                            '127': {
                                pictures: [
                                    {
                                        src: '//avatars.mds.yandex.net/get-turbo/2446839/rth39528ab928e7d70439d2698c62d2b224/',
                                        width: 1000,
                                        height: 666,
                                    },
                                    {
                                        src: '//avatars.mds.yandex.net/get-turbo/2947822/rth19feea7befa9497cc66bd972e19d838d/',
                                        width: 1000,
                                        height: 666,
                                    },
                                    {
                                        src: '//avatars.mds.yandex.net/get-turbo/2910686/rtha186c458936634809f3c803cce04a77a/',
                                        width: 1000,
                                        height: 666,
                                    },
                                ],
                                noticeInfo: 'Установка фаркопа в Москве и СПб от 2500 руб.!',
                            },
                        },
                    },
                    pagesMeta: {},
                }),
            },
        });

        await browser.click('.ProductItem:first-child');

        await browser.yaWaitForVisible('.Slider-Bullets');
        await browser.yaTouchScroll('.Slider-Slide', 300);
        await browser.getCssProperty('.turbo-card-slider__scroll', 'transform')
            .then(prop => assert.notEqual(prop.value, 'none', 'Слайдер не двигался'));
    });

    it('Позиция кнопок после подгрузки данных', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: { category_id: 9 },
        });

        await browser.yaMockFetch({
            status: 200,
            delay: 3000,
            urlDataMap: {
                '&isAjax=true': JSON.stringify({
                    entities: {
                        products: {
                            '137': {
                                pictures: [
                                    {
                                        src: '//avatars.mds.yandex.net/get-turbo/2446839/rth39528ab928e7d70439d2698c62d2b224/',
                                        width: 1000,
                                        height: 666,
                                    },
                                    {
                                        src: '//avatars.mds.yandex.net/get-turbo/2947822/rth19feea7befa9497cc66bd972e19d838d/',
                                        width: 1000,
                                        height: 666,
                                    },
                                    {
                                        src: '//avatars.mds.yandex.net/get-turbo/2910686/rtha186c458936634809f3c803cce04a77a/',
                                        width: 1000,
                                        height: 666,
                                    },
                                ],
                                noticeInfo: 'Установка фаркопа в Москве и СПб от 2500 руб.!',
                            },
                        },
                    },
                    pagesMeta: {},
                }),
            },
        });

        await browser.click('.ProductItem:first-child');
        await browser.yaWaitForVisible('.EcomScreen_type_product .Slider');

        const initialTop = await getElementTop(browser, '.EcomScreen_type_product .Button2_view_action');

        await browser.yaMockImages();
        await browser.yaAssertViewportView('before-load');

        await browser.yaWaitForVisible('.EcomScreen_type_product .Slider-Bullets');

        await browser.yaMockImages();
        await browser.yaAssertViewportView('after-load');

        const resultTop = await getElementTop(browser, '.EcomScreen_type_product .Button2_view_action');

        assert.strictEqual(resultTop, initialTop, 'Позиция кнопок изменилась');
    });

    it('Показывает попап при добавлении товара с карточки продукта', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: { product_id: 92, pcgi: 'rnd=tvowcsnngw' },
        });

        await browser.yaScrollPage('.ProductScreen-Actions-Button_toCart', 0.3);
        await browser.click('.ProductScreen-Actions-Button_toCart');
        await browser.yaWaitForVisible('.BottomBar-Item_type_cart .BottomBar-ItemPopup', 2000, 'Попап не показался');
    });

    it('Залипающая кнопка на странице товара', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: { product_id: 92, pcgi: 'rnd=tvowcsnngw' },
        });

        await browser.yaWaitForVisible('.EcomScreen_type_product .Slider');
        await browser.yaScrollAndSleep('.ProductScreen-Actions-BuyButtonCover');
        await browser.yaScrollPage('.ProductScreen-Actions-BuyButtonCover', -1);

        await browser.yaWaitForVisible('.StickyPanel');
        await browser.assertView('page', ['.EcomBottomBar_visible']);
    });

    it('Кнопка добавления в избранное', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 92,
                pcgi: 'rnd=tvowcsnngw',
                patch: 'setBlackboxData',
            },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaWaitForVisible('.FavoriteButton');
        await browser.yaMockImages();

        await browser.assertView('initial', '.FavoriteButton');

        await browser.yaMockFetch({
            urlDataMap: {
                '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
            },
        });

        await browser.click('.FavoriteButton');
        await browser.assertView('saved', '.FavoriteButton');

        let record = await browser.execute(function() {
            return localStorage.getItem('turbo-app-ecom--spideradio.github.io/favorites/123');
        });

        assert.strictEqual(record.value, '{"92":{"id":"92","cardId":"test-card-id"}}', 'Данные не сохранились в localStorage');

        await browser.refresh();

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaWaitForVisible('.FavoriteButton');
        await browser.yaMockImages();

        const favoriteButtonClasses = await browser.getAttribute('.FavoriteButton', 'class');
        assert.include(favoriteButtonClasses, 'FavoriteButton_saved');

        await browser.yaMockFetch({
            urlDataMap: {
                '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
            },
        });

        await browser.click('.FavoriteButton');
        await browser.assertView('deleted', '.FavoriteButton');

        record = await browser.execute(function() {
            return localStorage.getItem('turbo-app-ecom--spideradio.github.io/favorites/123');
        });

        assert.equal(record.value, '{}', 'Данные не удалились из localStorage');
    });

    it('Показывает попап при добавлении в избранное с карточки продукта', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 92,
                pcgi: 'rnd=tvowcsnngw',
                patch: 'setBlackboxData',
            },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaWaitForVisible('.FavoriteButton');
        await browser.yaMockImages();

        await browser.yaMockFetch({
            urlDataMap: {
                '/collections/api/v1.0/csrf-token': '{"csrf-token":"1"}',
                '/collections/api/v1.0/cards': '{"id":"test-card-id"}',
            },
        });

        await browser.click('.FavoriteButton');
        // Скролим в самый низ страницы, чтобы скриншотить попап на белом фоне.
        await browser.yaScrollPageToBottom();

        await browser.yaWaitForVisible('.BottomBar-ItemPopup', 2000, 'Попап не показался');
        await browser.assertView('bottom-bar-popup', ['.BottomBar-Item_type_user', '.BottomBar-ItemPopup']);
        await browser.yaWaitForHidden('.BottomBar-ItemPopup', 4000, 'Попап не скрылся');

        await browser.yaWaitForVisible(
            '.BottomBar-Item_type_user .CountBadge_visible',
            2000, 'Бейджик не показался',
        );
        await browser.assertView('bottom-bar-badge', [
            '.BottomBar-Item_type_user',
            '.BottomBar-Item_type_user .CountBadge_visible',
        ]);
    });

    it('Рендерятся несколько фильтров товара', async function() {
        const browser = this.browser;

        // TODO: переписать на spideradio, когда прорастут товары с селектом из фида
        await browser.yaOpenEcomSpa({
            url: '/turbo/lixma.ru/s/product/stul-shkolnyj-stil-pljus-regulir-2/',
            query: {
                product_id: 5140,
                pcgi: 'turboofferid=5140',
            },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        const { value: productsFilters } = await browser.elements('.ProductsFilter');
        assert.lengthOf(productsFilters, 3, 'На странице не корректное количество фильтров');
    });

    it('Лейбл "под заказ" на карточке товара', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 172,
                pcgi: 'rnd%3D8t7herpitk',
                patch: 'setProductIsNotAvailable',
            },
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.yaWaitForVisible('.ProductScreen-Label');
        await browser.assertView('label', '.ProductScreen-Label');
    });

    it('Рекомендации на странице товара', async function() {
        const { browser } = this;
        const __ym = {
            turbo_page: 1,
            doc_ui: 'touch-phone',
        };

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/',
            query: {
                product_id: 202,
                pcgi: 'rnd%3D2lum7hf3',
                patch: ['addProduct202SpideradioRecommendations', 'setReqid'],
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
                mainOfferId: '202',
                metrikaIds: ['65243191'],
                place: 'productPage',
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
                mainOfferId: '202',
                metrikaIds: ['65243191'],
                offerId: '127',
                originalUrl: 'https://spideradio.github.io/?rnd=2lum7hf3',
                place: 'productPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Djy7b4qqsed',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });

        await browser.click('.ProductItem button.ProductItem-Action');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-click-add-to-cart',
            params: {
                ecom_spa: 1,
                host: 'spideradio.github.io',
                mainOfferId: '202',
                metrikaIds: ['65243191'],
                offerId: '127',
                originalUrl: 'https://spideradio.github.io/?rnd=2lum7hf3',
                place: 'productPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Djy7b4qqsed',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });

        await browser.click('.ProductItem');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'product-item-click',
            params: {
                ecom_spa: 1,
                host: 'spideradio.github.io',
                mainOfferId: '202',
                metrikaIds: ['65243191'],
                offerId: '127',
                originalUrl: 'https://spideradio.github.io/?rnd=2lum7hf3',
                place: 'productPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Djy7b4qqsed',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });
    });

    it('Рекомендации с тем же товаром', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/rnd/jy7b4qqsed',
            query: {
                product_id: 127,
                patch: ['addProduct202SpideradioRecommendations', 'setReqid'],
            },
        });

        await browser.yaIndexify('.ProductList .ProductItem');
        const firstRecomUrl = await browser.getAttribute('.ProductItem[data-index="0"] .Link', 'href');

        assert.include(firstRecomUrl, 'jy7b4qqsed', 'Первая карточка не тот же товар');

        await browser.yaWaitForVisible('.DeliveryInfo');
        await browser.yaShouldBeVisible('.ProductScreen-Actions-BuyButtonCover .ProductScreen-Actions-Button_toCart');
        await browser.yaShouldNotBeVisible('.ProductScreen-Actions-BuyButtonCover .ProductScreen-Actions-Button_loading', 'Кнопка В корзину в процессе загрузки');
    });

    it('Кнопки выбора комплектации и быстрого заказа у магазина без корзины', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/strelka11.ru/s/zashhita-radiatora/standart/nissan-x-trail-iii-restajling-2017-2019-984',
            query: {
                product_id: '984000059',
            },
        });

        await browser.assertView('plain', ['.Price', '.ProductInfo-Notice']);
    });

    describe('Эксперимент turbo-app-product-image-design', () => {
        it('Растягивание картинки товара', async function() {
            const { browser } = this;

            await browser.onRecord(() => browser.auth('tap-user'));
            await browser.yaOpenEcomSpa({
                url: '/turbo/super01.ru/s/products/kruzhka-s-artom-soul-knight',
                query: {
                    product_id: '19708',
                },
                expFlags: {
                    'turbo-app-product-image-design': '1',
                    'turbo-app-listing-design': '1',
                },
            });

            await browser.assertView('image', ['.ProductScreen-Image', '.Price']);
        });

        it('Квадратная картинка со скидкой', async function() {
            const { browser } = this;

            await browser.onRecord(() => browser.auth('tap-user'));
            await browser.yaOpenEcomSpa({
                url: '/turbo/super01.ru/s/products/myagkaya-igrushka-bolshoi-pikachu-pokemon-pikachu-80-sm-utsenka',
                bottomBar: 'hide',
                query: {
                    product_id: '17493_Y',
                },
                expFlags: {
                    'turbo-app-product-image-design': '1',
                    'turbo-app-listing-design': '1',
                },
            });

            await browser.assertView('image', ['.ProductScreen-Image', '.Price']);

            await browser.click('.ProductScreen-Image');
            await browser.yaWaitForVisible('.viewer_visible');
            await browser.assertView('viewer', '.viewer_visible');
        });

        it('Много картинок в слайдере', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo/aptekabarato.ru/s/barnaul/dlya-sluha/otilor/',
                query: {
                    product_id: '79',
                },
                expFlags: {
                    'turbo-app-product-image-design': '1',
                    'turbo-app-listing-design': '1',
                },
            });

            await browser.assertView('image', ['.ProductScreen-Image', '.Price']);
        });

        it('Товар без картинки', async function() {
            const { browser } = this;

            await browser.onRecord(() => browser.auth('tap-user'));
            await browser.yaOpenEcomSpa({
                url: '/turbo',
                query: {
                    text: 'https://spideradio.github.io/rnd/9j2',
                    product_id: '147',
                },
                expFlags: {
                    'turbo-app-product-image-design': '1',
                    'turbo-app-listing-design': '1',
                },
            });

            await browser.assertView('image', ['.ProductScreen-Image', '.Price']);
        });

        it('Выбор размера и цвета', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo',
                bottomBar: 'hide',
                query: {
                    text: 'https://ymturbo.t-dir.com/catalog/underwear/lingerie-intimate-evening/',
                    turboofferid: '126',
                },
                expFlags: {
                    'turbo-app-product-image-design': '1',
                    'turbo-app-listing-design': '1',
                },
            });

            await browser.assertView('filters', ['.Price', '.ProductScreen-Actions-Button']);
        });
    });
});
