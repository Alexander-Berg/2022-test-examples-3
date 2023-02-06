describe('ProductPage', function() {
    describe('Модальное окно', function() {
        it('Загрузка и ошибка', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=iphone');
            await bro.yaWaitForVisible('.ProductCardsList', 3000, 'выдача не появилась');
            await bro.yaMockXHR({
                urlDataMap: '\/products\/(offer|product)',
                status: 404,
                timeout: 5000,
            });
            await bro.click('.ProductCard');

            await bro.yaWaitForVisible('.ProductEntityModal', 3000, 'модальное окно не появилось');
            await bro.yaMakeBlackOverlay();
            await bro.assertView('loading', '.ProductEntityModal');

            await bro.yaWaitForVisible('.ProductEntityModal-ErrorMessage', 20000, 'ошибка загрузки не отобразилась');
            await bro.assertView('failed', '.ProductEntityModal');
        });
    });

    describe('Оффер', function() {
        it('Избранное', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/VO-QkIvnyqHoEznylufFwA?promo=nomooa&exp_flags=enable_favorites=1');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('favorites', '.FavoritesButtonWrapper');
        });
    });

    describe('Модель', function() {
        it('Избранное', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/1701611514?promo=nomooa&exp_flags=enable_favorites=1');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.CardFavoriteButton', 3000, 'кнопка Избранное не появилась');

            await bro.assertView('favorites', '.CardFavoriteButton');

            await bro.yaShouldBeVisible('.CardFavoriteButton', 'точка входа в избранное не видна');
        });
    });

    // Тесты на метрики не браузерозависимы, для ускорения гоняем только в одном браузере.
    hermione.only.in(['linux-chrome']);
    describe('Mertics', async function() {
        it('Запросы', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177753');
            await bro.yaWaitForPageLoad();

            await bro.yaWaitForSearchPage();
            await bro.click('.ProductEntityModal-CloseButton');

            await bro.yaCheckMetrics({
                'products.all_requests': 2,
                'products.sku_requests': 1,
                'products.requests': 1, // Запросы к поисковой выдаче
            });

            await bro.click('.ProductCard');
            await bro.yaWaitForHidden('.Spin2_progress', 3000, 'карточка товара не загрузилась');

            await bro.yaCheckMetrics({
                'products.all_requests': 3,
                'products.sku_requests': 2,
                'products.requests': 1,
            });
        });

        it('Главная → Выдача → SKU → Внешний магазин', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products');
            await bro.yaWaitForPageLoad();

            await bro.click('.CategoriesList-Item');
            await bro.yaWaitForSearchPage();

            await bro.click('.ProductCard_type_sku');
            await bro.yaWaitForSKUPage();

            await bro.yaCheckNewTabOpen({
                async action() {
                    await bro.click('.ShopList-Item .Link');
                },
            });

            // Запросы и клики на главной сейчас не приходят в метриках.
            await bro.yaCheckMetrics({
                // Запрос за выдачей.
                'products.requests': 1,
                // Клик по SKU на выдаче.
                'products.product_card_sku_clicks': 1,
                'products.product_card_clicks': 1,

                // Запрос за страницей SKU.
                'products.sku_requests': 1,
                // Клик по внешнему магазину.
                'products.sku_external_clicks': 1,
                'products.sku_total_clicks': 1,

                // Клик на выдаче.
                'products.total_clicks': 1,
                // Запрос на главную, за выдачей и за SKU.
                'products.all_requests': 3,
                // Клик по внешнему магазину.
                'products.all_external_clicks': 1,
            });
        });

        it('Выдача → Оффер → Внешний магазин', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/search?text=рубашка в полосочку');
            await bro.yaWaitForSearchPage({ waitMore: true });

            await bro.click('.ProductCard_type_offer');
            await bro.yaWaitForOfferPage();

            await bro.yaCheckNewTabOpen({
                async action() {
                    await bro.click('.ShopList-Item .Link');
                },
            });

            await bro.yaCheckMetrics({
                // Запрос за страницей выдачи.
                'products.requests': 1,
                // Клик по офферу на выдаче.
                'products.product_card_offer_clicks': 1,
                'products.product_card_clicks': 1,

                // Запрос за страницей оффера.
                'products.offer_requests': 1,
                // Клик по внешнему магазину.
                'products.offer_external_clicks': 1,
                'products.offer_total_clicks': 1,

                // Клик на выдаче.
                'products.total_clicks': 1,
                // Запрос за выдачей и оффером.
                'products.all_requests': 2,
                // Клик по внешнему магазину.
                'products.all_external_clicks': 1,
            });
        });
    });

    hermione.skip.in('linux-chrome-iphone');
    describe('Проверенный магазин', function() {
        it('Страница sku', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/558171067/sku/101106266734?promo=nomooa&exp_flags=trusted_merchant_icon=1');
            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.scroll('.ShopItem_verified');
            await bro.assertView('inactive-layout', '.ShopItem_verified');
            await bro.click('.ShopItem-Verified');
            await bro.assertView('active-layout', ['.ShopItem_verified', '.VerifiedModal-Content']);
            await bro.click('.VerifiedModal');
        });
    });
});
