const helpers = require('../../../../hermione/utils/baobab');

specs({
    feature: 'products-carousel',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('?stub=productscarousel/default.json')
            .yaWaitForVisible(PO.blocks.productsCarousel())
            .assertView('plain', PO.blocks.page())
            .yaIndexify(PO.blocks.productsCarousel())
            .yaTouchScroll(PO.blocks.firstProductsCarousel.turboNativeScroll.inner(), 9999)
            .assertView('scrolled', PO.blocks.page());
    });

    hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
    describe('Baobab', () => {
        /** Проверяем интеграцию в baobab-дереве для ProductItem */
        async function checkBaobabProductItem(tree, offerId) {
            /** Проверка, что ProductItem с одинаковым offerId залогирован в каждой из 2-х каруселей */
            const productItemsLog = helpers.query(
                '$page.$main.$result.products-carousel.product-item',
                tree,
                { offerId, pos: offerId },
            );
            assert.lengthOf(productItemsLog, 2, `Залогированы не все ProductItem с offerId="${offerId}"`);
        }

        beforeEach(function() {
            return this.browser
                .url('/turbo?stub=productscarousel/default.json')
                .yaWaitForVisible(PO.blocks.productsCarousel());
        });

        hermione.only.notIn('safari13');
        it('Дерево', async function() {
            const bro = this.browser;

            /** Серверное дерево baobab */
            const { tree } = await bro.yaGetBaobabTree();

            /** Проверяем счетчик карусели */
            const productsCarouselLog = helpers.query('$page.$main.$result.products-carousel', tree);
            assert.lengthOf(productsCarouselLog, 2, 'Залогированы не все ProductsCarousel');

            await checkBaobabProductItem(tree, '5055');
            await checkBaobabProductItem(tree, '6596');
            await checkBaobabProductItem(tree, '4880');
        });

        hermione.only.notIn('safari13');
        it('Видимость продукта', async function() {
            const bro = this.browser;

            await bro.yaCheckBaobabCounter(
                () => {},
                {
                    path: '$page.$main.$result.products-carousel.product-item',
                    event: 'tech',
                    type: 'product-item-seen',
                    data: {
                        productUrl: '/turbo?utm_source=turbo_turbo&text=https%3A//www.betonproject.ru/products/ks-10-9%3Fvrid%3D6596',
                        offerId: '6596',
                        metrikaIds: ['45135375'],
                    },
                },
                { message: 'Не отправился счётчик просмотра первого продукта' }
            );
        });

        hermione.only.notIn('safari13');
        it('Клик по продукту', async function() {
            const bro = this.browser;

            await bro.yaIndexify(PO.blocks.productItem());
            await bro.yaCheckBaobabCounter(
                () => bro.yaClickStubLink(PO.blocks.firstProductItem.link()),
                {
                    path: '$page.$main.$result.products-carousel.product-item.link',
                    event: 'click',
                    type: 'product-item-click',
                    data: {
                        productUrl: '/turbo?utm_source=turbo_turbo&text=https%3A//www.betonproject.ru/products/ks-10-9%3Fvrid%3D6596',
                        offerId: '6596',
                        metrikaIds: ['45135375'],
                    },
                },
                { message: 'Не отправился счётчик нажатия по первому товару' }
            );
        });

        hermione.only.notIn('safari13');
        it('Клик по кнопке добавления в корзину', async function() {
            const bro = this.browser;

            await bro.yaIndexify(PO.blocks.productItem());
            await bro.yaCheckBaobabCounter(
                () => bro.yaClickStubLink(PO.blocks.secondProductItem.footer.button()),
                {
                    path: '$page.$main.$result.products-carousel.product-item.button',
                    event: 'click',
                    type: 'product-item-click-add-to-cart',
                },
                { message: 'Не отправился счётчик нажатия по кнопке добавления товара в корзину' }
            );
        });
    });

    hermione.only.in(['chrome-phone'], 'Ускоряем браузеронезависимые тесты');
    describe('Metrika', () => {
        beforeEach(async function() {
            const bro = this.browser;
            await bro.url('/turbo?stub=productscarousel/default.json&exp_flags=analytics-disabled=0');
            await bro.yaWaitForVisible(PO.blocks.productsCarousel());
        });

        hermione.only.notIn('safari13');
        it('Цель на рендеринг карусели рекомендаций', async function() {
            const bro = this.browser;
            await bro.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'ecom_recommendations_rendered',
                params: {
                    type: 'personal',
                    place: 'product_card',
                },
            });
        });

        hermione.only.notIn('safari13');
        it('Цель на видимость продуктов в каруселе рекомендаций', async function() {
            const goals = {
                item0: {
                    counterId: 53911873,
                    name: 'ecom_recommendations_seen_by_user',
                    params: {
                        type: 'personal',
                        place: 'product_card',
                        offerId: '6596',
                        index: 0,
                    },
                },
                item1: {
                    counterId: 53911873,
                    name: 'ecom_recommendations_seen_by_user',
                    params: {
                        type: 'personal',
                        place: 'product_card',
                        offerId: '5055',
                        index: 1,
                    },
                },
                item3: {
                    counterId: 53911873,
                    name: 'ecom_recommendations_seen_by_user',
                    params: {
                        type: 'personal',
                        place: 'product_card',
                        offerId: '8990',
                        index: 3,
                    },
                },
            };
            const bro = this.browser;
            await bro.yaCheckMetrikaGoal(goals.item0);
            await bro.yaCheckMetrikaGoal(goals.item1);

            await bro.yaIndexify(PO.blocks.productItem());
            let isVisibleItem4 = await bro.isVisibleWithinViewport(PO.blocks.forthProductItem());
            assert.isFalse(isVisibleItem4, 'четвёртый элемент изначально уже в области видимости');
            const goalItem4 = (await bro.yaGetMetrikaGoals(goals.item3))[0];
            assert.isUndefined(goalItem4, 'цель на показ четвёртого элемента уже отправилась');

            // Скролим до конца вправо.
            await bro.yaScrollElement(PO.blocks.turboNativeScroll.itemsList(), 777);
            isVisibleItem4 = await bro.isVisibleWithinViewport(PO.blocks.forthProductItem());
            assert.isTrue(isVisibleItem4, 'четвёртый элемент не появился в области видимости');
            await bro.yaCheckMetrikaGoal(goals.item3);
        });

        hermione.only.notIn('safari13');
        it('Цель на клик по продукту в каруселе рекомендаций', async function() {
            const bro = this.browser;
            await bro.yaIndexify(PO.blocks.productItem());
            await bro.yaClickStubLink(PO.blocks.firstProductItem.link());
            await bro.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'ecom_recommendations_clicked',
                params: {
                    type: 'personal',
                    place: 'product_card',
                    offerId: '6596',
                    index: 0,
                },
            });
        });

        hermione.only.notIn('safari13');
        it('Цель на клик по добавлению продукта в корзину в каруселе рекомендаций', async function() {
            const bro = this.browser;
            await bro.yaIndexify(PO.blocks.productItem());
            await bro.click(PO.blocks.secondProductItem.footer.button());
            await bro.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'ecom_recommendations_add_to_cart',
                params: {
                    type: 'personal',
                    place: 'product_card',
                    offerId: '5055',
                    index: 1,
                },
            });
        });
    });

    hermione.only.in(['iphone'], 'Ускоряем браузеронезависимые тесты');
    describe('На странице паблишеров', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид', function() {
            return this.browser
                .url('?stub=page/page-with-ecom-recom.json')
                .yaWaitForVisible(PO.blocks.productsCarouselWithUnitRect())
                .assertView('plain', PO.blocks.productsCarouselWithUnitRect());
        });

        hermione.only.notIn('safari13');
        it('Баобаб', async function() {
            const browser = this.browser;

            await browser.url('?stub=page/page-with-ecom-recom.json');
            await browser.yaWaitForVisible(PO.blocks.productsCarousel());
            await browser.yaScrollPage(PO.blocks.productsCarousel());
            await browser.yaCheckBaobabCounter(
                () => {},
                {
                    path: '$page.$main.$result.products-carousel.product-item',
                    event: 'tech',
                    type: 'product-item-seen',
                    data: {
                        productUrl: '/turbo?utm_source=turbo_turbo&text=https%3A//www.betonproject.ru/products/ks-10-9%3Fvrid%3D6596',
                        offerId: '6596',
                        metrikaIds: ['11111111'],
                        originalUrl: 'https://ria.ru/20201208/vaktsinatsiya-1588217037.html',
                        place: 'publishing',
                        recommendationType: 'personal'
                    },
                },
                { message: 'Не отправился счётчик просмотра первого продукта' }
            );

            await browser.yaIndexify(PO.blocks.productItem());
            await browser.yaCheckBaobabCounter(
                () => browser.yaClickStubLink(PO.blocks.firstProductItem.link()),
                {
                    path: '$page.$main.$result.products-carousel.product-item.link',
                    event: 'click',
                    data: {
                        productUrl: '/turbo?utm_source=turbo_turbo&text=https%3A//www.betonproject.ru/products/ks-10-9%3Fvrid%3D6596',
                        offerId: '6596',
                        metrikaIds: ['11111111'],
                        originalUrl: 'https://ria.ru/20201208/vaktsinatsiya-1588217037.html',
                        place: 'publishing',
                        type: 'product-item-click',
                        recommendationType: 'personal'
                    },
                },
                { message: 'Не отправился счётчик нажатия по первому товару' }
            );
        });
    });
});
