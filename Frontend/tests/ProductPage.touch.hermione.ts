describe('ProductPage', function() {
    describe('После СПАС-перехода', function() {
        it('Внешний вид модели', async function() {
            const bro = this.browser;
            await bro.yaOpenSpasUrl('/products/product/8480393?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card');
            assert.strictEqual(await bro.getTitle(), 'Чайник Bosch TWK 3A011/3A013/3A014/3A017 — Цены', 'не установился нужный заголовок вкладки');
        });

        it('Внешний вид SKU', async function() {
            const bro = this.browser;
            await bro.yaOpenSpasUrl('/products/product/558171067/sku/101106238734?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card');
            assert.strictEqual(
                await bro.getTitle(),
                'Смартфон Apple iPhone 11 128 ГБ RU, фиолетовый, Slimbox — Цены',
                'не установился нужный заголовок вкладки',
            );
        });

        it('Нажатие на крестик c поисковым запросом', async function() {
            const bro = this.browser;
            await bro.yaOpenSpasUrl('/products/product/824253112?promo=nomooa&text=iphone');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'iphone', 'не сохранился запрос при переходе на выдачу');
        });
    });

    hermione.skip.in('appium-chrome-phone', 'не умеет делать setValue()');
    it('Поиск', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/offer/X6Ha41qz5hqDe5B_YDphiQ?promo=nomooa');
        const input = await bro.$('.Header .mini-suggest__input');

        assert.strictEqual(await input.getValue(), '', 'поисковая строка не пустая');

        await input.setValue('iphone');

        await bro.click('.Header .mini-suggest__button');
        const searchUrl = await bro.getUrl();

        await bro.yaWaitForVisible('.SearchPage-Products', 2000, 'выдача не появилась');

        const parsed = new URL(searchUrl);
        assert.strictEqual('iphone', parsed.searchParams.get('text'), 'неожиданный текст запроса при поиске');
    });

    describe('Модель', function() {
        it('Нажатие на крестик без поискового запроса', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/824253112?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'Холодильники', 'не подставилась категория в запрос');
        });

        it('Нажатие на крестик c поисковым запросом', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/824253112?promo=nomooa&text=iphone');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'iphone', 'не сохранился запрос при переходе на выдачу');
        });

        it('Избранное', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/1701611514?promo=nomooa&exp_flags=enable_favorites=1');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.CardFavoriteButton', 3000, 'кнопка Избранное не появилась');

            await bro.assertView('favorites', '.CardFavoriteButton');

            await bro.yaShouldBeVisible('.CardFavoriteButton', 'точка входа в избранное не видна');
        });
    });

    describe('SKU', function() {
        it('Нажатие на крестик без поискового запроса', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/752262035/sku/101100443755?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'Ноутбуки', 'не подставилась категория в запрос');
        });

        it('Нажатие на крестик c поисковым запросом', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/752262035/sku/101100443755?promo=nomooa&text=iphone');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'iphone', 'не сохранился запрос при переходе на выдачу');
        });

        it('Переход к форме "Подписка на снижение цены"', async function() {
            const bro = this.browser;

            await bro.authOnRecord('plain-user');
            await bro.yaOpenPageByUrl('/products/product/752262035/sku/101100443755?promo=nomooa&exp_flags=price_trend_redesign;price_charts_enabled;sku_subscriptions_enabled;enable_price_subscription;enable_price_subscription_unauthorized&lr=213#price_subscription');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-PriceSubscription', 3000, 'раздел подписки на снижение цены не появился');
            await bro.yaWaitForVisible('.PriceSubscription-Modal', 3000, 'всплывающее окно "Подписка на снижение цены" не появилось');

            const { priceSubscriptionRect, clientHeight, clientWidth } = await bro.execute(() => ({
                priceSubscriptionRect: document.getElementById('price_subscription')?.getBoundingClientRect(),
                clientHeight: document.documentElement.clientHeight,
                clientWidth: document.documentElement.clientWidth,
            }));

            const isPriceSubscriptionInViewport = priceSubscriptionRect && (
                priceSubscriptionRect.top >= 0 &&
                priceSubscriptionRect.left >= 0 &&
                priceSubscriptionRect.bottom <= clientHeight &&
                priceSubscriptionRect.right <= clientWidth
            );

            assert.isTrue(isPriceSubscriptionInViewport, 'раздел подписки на снижение цены находится вне viewport');
        });
    });

    describe('Оффер', function() {
        it('Нажатие на крестик без поискового запроса', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/offer/X6Ha41qz5hqDe5B_YDphiQ?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'Мобильные телефоны', 'не подставилась категория в запрос');
        });

        it('Нажатие на крестик c поисковым запросом', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/offer/X6Ha41qz5hqDe5B_YDphiQ?promo=nomooa&text=iphone');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaWaitForVisible('.Card-CloseButton', 1000, 'крестик  не появился');

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForPageLoad();

            const { searchParams } = new URL(await bro.getUrl());

            assert.strictEqual(searchParams.get('text'), 'iphone', 'не сохранился запрос при переходе на выдачу');
        });

        it('Избранное', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/VO-QkIvnyqHoEznylufFwA?promo=nomooa&exp_flags=enable_favorites=1');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('favorites', '.FavoritesButtonWrapper');

            await bro.yaShouldBeVisible('.FloatFavoritesButton', 'точка входа в избранное не видна');
        });
    });

    // Тесты на метрики не браузерозависимы, для ускорения гоняем только в одном браузере.
    hermione.only.in('linux-chrome-iphone');
    describe('Mertics', async function() {
        it('Запросы', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177753');
            await bro.yaWaitForPageLoad();

            await bro.click('.Card-CloseButton');
            await bro.yaWaitForHidden('.ProductCardsList_loading', 3000, 'выдача не загрузилась');

            await bro.yaCheckMetrics({
                'products.all_requests': 2,
                'products.requests': 1, // Запросы к поисковой выдаче
                'products.sku_requests': 1,
            });

            await bro.click('.ProductCard');
            await bro.yaWaitForHidden('.Spin2_progress', 3000, 'карточка товара не загрузилась');

            await bro.yaCheckMetrics({
                'products.all_requests': 3,
                'products.requests': 1,
                'products.sku_requests': 2,
            });
        });

        it('Главная → Выдача → SKU → Внешний магазин', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products');
            await bro.yaWaitForPageLoad();

            await bro.click('.CategoriesList-Item');
            await bro.yaWaitForSearchPage();

            await bro.scroll(0, 20000);
            await bro.pause(5000);

            await bro.scroll(0, 20000);
            await bro.pause(5000);

            await bro.click('.ProductCard_type_sku');
            await bro.yaWaitForSKUPage();

            await bro.yaCheckNewTabOpen({
                async action() {
                    await bro.click('.ShopList-Item .Link');
                },
            });

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
            await bro.yaWaitForSearchPage();

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
    hermione.also.in('iphone-dark');
    describe('Проверенный магазин', function() {
        hermione.skip.in('iphone-dark');
        it('Страница sku', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/558171067/sku/101106266734?promo=nomooa&exp_flags=trusted_merchant_icon=1');
            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.scroll('.ShopItem_verified');
            await bro.assertView('inactive-layout', '.ShopItem_verified');
            await bro.click('.ShopItem-Verified');
            await bro.assertView('active-layout', ['.ShopItem_verified', '.VerifiedModal']);
            await bro.click('.VerifiedModal');
        });

        hermione.only.in('iphone-dark');
        it('Страница sku с настройкой темы "dark"', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrlWithColorScheme('/products/product/558171067/sku/101106266734?text=iphone&promo=nomooa&exp_flags=dark_theme_touch=dark;trusted_merchant_icon=1');
            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.scroll('.ShopItem_verified');
            await bro.assertView('inactive-layout', '.ShopItem_verified');
            await bro.click('.ShopItem-Verified');
            // Закрываем все что под модалкой
            // @ts-ignore
            await bro.execute(() => { document.querySelector('.VerifiedModal .Modal-Overlay, .VerifiedModal .Drawer-Overlay').style.background = 'black' });
            await bro.assertView(
                'active-layout',
                ['.ShopItem_verified', '.VerifiedModal'],
                {
                    // здесь немного плавают шрифты
                    antialiasingTolerance: 8,
                }
            );
            await bro.click('.VerifiedModal');
        });
    });
});
