describe('Ecom-tap', function() {
    it('Внешний вид главной страницы', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaMockImages();
        await browser.assertView('page', ['.Cover', '.ScreenContent']);
    });

    it('Наличие бесконечной ленты на главной странице', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            expFlags: { 'turbo-app-morda-redesign': 1 },
        });

        await browser.yaMockImages();
        await browser.yaWaitForVisible('.GlobalProductList', 'Список продуктов не загрузился');
    });

    it('Внешний вид шапки с длинным текстом', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            query: { patch: 'longTitleAndDescription' },
        });

        await browser.yaMockImages();
        await browser.assertView('page', '.Cover', { ignoreElements: ['.Slider.turbo-card-slider'] });
    });

    it('Внешний вид главной страницы после перехода с каталога', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
        });

        await browser.click('.BottomBar-Item:nth-child(1)');
        await browser.yaWaitForVisible('.Collection');

        await browser.yaMockImages();
        await browser.assertView('page', ['.Cover', '.ScreenContent']);
    });

    it('При клике в баннер открывается в той же вкладке', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'zymbo.ru',
            pageType: 'main',
            query: {
                stub: 'ecom-tap/internal-zymbo.json',
                turbo_enable_cgi_forwarding: '1',
            },
        });

        const slideSelector = '.Slider-Slide:nth-child(1)';

        await browser.yaWaitForVisible(slideSelector);
        await browser.yaScrollPage(slideSelector);
        await browser.click(slideSelector);

        await browser.yaWaitForVisible('.NavigationTransition_state_entered');
    });

    it('При клике в баннер открывается в другой вкладке', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'zymbo.ru',
            pageType: 'main',
            query: {
                stub: 'ecom-tap/external-zymbo.json',
                turbo_enable_cgi_forwarding: '1',
            },
        });

        const slideSelector = '.Slider-Slide:nth-child(1)';

        await browser.yaWaitForVisible(slideSelector);
        await browser.yaScrollPage(slideSelector);

        await browser.yaCheckNewTabOpen(
            () => browser.click(slideSelector),
            async() => {
                const { value: location } = await browser.execute(function() { return location });
                assert.isTrue(location.pathname === '/search/touch/' && location.hostname === 'yandex.ru');
            },
        );
    });

    describe('Проверка внешнего вида коллеций на главной странице', function() {
        it('Перенос названия коллекции', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'petdog.ru',
                pageType: 'main',
            });

            await browser.yaWaitForVisible('.ScreenContent-Inner');
            await browser.yaMockImages();
            await browser.assertView('collections-title', '.Collection');
        });

        hermione.only.in(['iphone'], 'Нужен маленький экран');
        it('Обрезание длинного поля количества товаров', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'petdog.ru',
                pageType: 'main',
            });

            await browser.yaWaitForVisible('.ScreenContent-Inner');
            await browser.yaMockImages();
            await browser.assertView('collections-count', '.Collection');
        });
    });

    hermione.only.notIn('iphone', 'В тестовом iphone нестабильно работает подскролл');
    it('Сохранение скролла в блоке рекомендаций на Главной', async function() {
        const { browser } = this;
        const scrollX = 50;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        await browser.yaWaitForVisible('.EcomScreen_type_main', null, 'Не главная страница');
        await browser.yaScrollPage('.ProductList', 0);
        await browser.yaShouldBeScrollable('.ProductList .ProductList-Content', { h: true });
        await browser.yaScrollElement('.ProductList .ProductList-Content', scrollX);

        // Пробуем стабилизировать тест, чтобы изменение сохранилось, перед уходом со страницы
        await browser.yaWaitUntil('Карусель не прокручена до нужного места первый раз', () =>
            browser.execute(function() {
                return document.querySelector('.ProductList .ProductList-Content').scrollLeft;
            })
                .then(({ value }) => value === scrollX),
        );
        await browser.click('.ProductList-Content .ProductItem:nth-child(2)');
        await browser.yaWaitForVisible('.EcomScreen_type_product', null, 'Не появилась страница товара');
        await browser.back();
        await browser.yaWaitForVisible('.EcomScreen_type_main', null, 'Не появилась главная страница после клика назад');
        await browser.yaWaitUntil('Карусель не прокручена до нужного места', () =>
            browser.execute(function() {
                return document.querySelector('.ProductList .ProductList-Content').scrollLeft;
            })
                .then(({ value }) => value === scrollX),
        );
    });

    it('Скрытие кол-ва товаров в категориях и коллекциях на главной', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            expFlags: { 'turbo-app-hide-category-count': 1 },
        });

        await browser.yaMockImages();
        await browser.assertView('categories', '.CategoryList');
        await browser.assertView('collection', '.Collection');
    });

    it('Рекомендации на главной странице', async function() {
        const { browser } = this;
        const __ym = {
            turbo_page: 1,
            doc_ui: 'touch-phone',
        };

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            query: {
                patch: ['addMainRecommendations', 'setReqid'],
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
                originalUrl: 'http://spideradio.github.io',
                place: 'mainPage',
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
                originalUrl: 'http://spideradio.github.io',
                place: 'mainPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Dfnu7e56jbff',
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
                metrikaIds: ['65243191'],
                offerId: '292',
                originalUrl: 'http://spideradio.github.io',
                place: 'mainPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Dfnu7e56jbff',
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
                metrikaIds: ['65243191'],
                offerId: '292',
                originalUrl: 'http://spideradio.github.io',
                place: 'mainPage',
                productUrl: 'https://yandex.ru/turbo/spideradio.github.io/s/?pcgi=rnd%3Dfnu7e56jbff',
                recommendationType: 'personal',
                reqid: 'test-reqid',
                turbo_app_enabled: 1,
                __ym,
            },
        });
    });
});
