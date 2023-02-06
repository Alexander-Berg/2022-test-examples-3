describe('ProductPage', function() {
    describe('Оффер', function() {
        it('Внешний вид без характеристик', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/1701611514/sku/101633134145?promo=nomooa&exp_flags=disable_product_specs');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Content_top_flat, .Card-RightCol');
        });

        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/offer/VO-QkIvnyqHoEznylufFwA?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card');
            assert.strictEqual(await bro.getTitle(), 'Лист а/ц (шифер волновой) 1750х1130х5,8мм (1,97 кв.м.) / Лист асбестоцементный (шифер кровельный волновой) 1130х1750х5,8мм (1,97 кв.м.) — Цены', 'не установился нужный заголовок вкладки');
        });

        it('Внешний вид б/у оффер', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/offer/rIeYtjtNi8KWlSsN51hbQw?promo=nomooa&is_used=1&exp_flags=used_goods%3D1');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card');
        });

        it('Цель Метрики по переходу', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/offer/dWyaaz1vxjQQekL3I2INbA?promo=nomooa&exp_flags=analytics_disabled=0');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.click('.ShopItem-Link');
            await bro.yaCheckMetrikaGoal({
                counterId: '84153601',
                name: 'from-offer-to-shop-link',
                params: {
                    offerId: 'dWyaaz1vxjQQekL3I2INbA',
                    url: 'https://appleshop-service.ru/products/iphone-xr-64gb/?attribute_pa_color=black',
                },
            });
            await bro.click('.Card-Button');
            await bro.yaCheckMetrikaGoal({
                counterId: '84153601',
                name: 'from-offer-to-shop-button',
                params: {
                    offerId: 'dWyaaz1vxjQQekL3I2INbA',
                    url: 'https://appleshop-service.ru/products/iphone-xr-64gb/?attribute_pa_color=black',
                },
            });
        });

        it('Проверка проставления метки utm_referrer', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/LWHF1wRHNpfzlhFX1tR-HA?retpath=%2Fsearch%3Ftext%3Diphone%252012');
            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            const button = await bro.$('.Card-Button');
            const buttonUrl = new URL(await button.getAttribute('href'));
            assert(
                buttonUrl.searchParams.has('utm_referrer'),
                'В ссылке нет метки utm_referrer',
            );

            const buttonReferrer = new URL(buttonUrl.searchParams.get('utm_referrer') as string);
            assert.equal(buttonReferrer.pathname, '/products/search', 'Неверный путь в значении utm_referrer');
            assert.equal(buttonReferrer.searchParams.get('text'), 'iphone 12', 'Неверный текст запроса в значении utm_referrer');

            const offer = await bro.$('.ShopItem-Link');
            const offerUrl = new URL(await offer.getAttribute('href'));
            assert(
                offerUrl.searchParams.has('utm_referrer'),
                'В предложении из списка магазинов есть метка utm_referrer',
            );

            const offerReferrer = new URL(buttonUrl.searchParams.get('utm_referrer') as string);
            assert.equal(offerReferrer.pathname, '/products/search', 'Неверный путь в значении utm_referrer');
            assert.equal(offerReferrer.searchParams.get('text'), 'iphone 12', 'Неверный текст запроса в значении utm_referrer');
        });

        it('Проверка отсутствия проставления метки utm_referrer для магазина, неподдерживающего метку', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/offer/9nQrXlcWN9lC_5p-oWshjQ?retpath=%2Fsearch%3Ftext%3DСмартфоны');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            const button = await bro.$('.Card-Button');
            const buttonUrl = new URL(await button.getAttribute('href'));
            assert.isFalse(
                buttonUrl.searchParams.has('utm_referrer'),
                'В кнопке перехода на магазин есть метка utm_referrer',
            );

            const firstOffer = await bro.$('.ShopItem-Link');
            const firstOfferUrl = new URL(await firstOffer.getAttribute('href'));
            assert.isFalse(
                firstOfferUrl.searchParams.has('utm_referrer'),
                'В предложении из списка магазинов есть метка utm_referrer',
            );
        });

        it('Проверка проставления SC куки', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/dWyaaz1vxjQQekL3I2INbA?retpath=%2Fsearch%3Ftext%3Diphone');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.yaMockCookies();

            await bro.click('.Card-Button');

            const SCCookie = await bro.yaGetCookie(/^sc_/);

            assert(Boolean(SCCookie), 'Нет SC куки');

            const value = SCCookie?.split(';')[0];

            const [text, destination, handler] = (value?.split(':') || []).map(decodeURIComponent);

            assert.deepEqual([text, destination, handler], ['iphone', 'appleshop-service.ru', '/products/search'], 'Неверные значения SC куки');
        });
    });

    describe('Модель', function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/8480393?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card');
            assert.strictEqual(await bro.getTitle(), 'Чайник Bosch TWK 3A011/3A013/3A014/3A017 — Цены', 'не установился нужный заголовок вкладки');
        });

        it('Дозагрузка офферов', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/1414986413');
            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            const { length: lengthBeforeExpand } = await bro.$$('.ShopList-Item');

            assert.equal(lengthBeforeExpand, 5, 'В свернутом состоянии должно быть 5 офферов');

            await tryToExpandOfferList(bro, 20, 'После первого клика должно получится 20 офферов');
            await tryToExpandOfferList(bro, 30, 'После дозагрузки должно получится 30 офферов');
        });

        it('Цель Метрики по переходу', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/1414986413?promo=nomooa&exp_flags=analytics_disabled=0');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.click('.ShopItem-Link');
            await bro.yaCheckMetrikaGoal({
                counterId: '84153601',
                name: 'from-model-to-shop-link',
                params: {
                    offerId: '6Dgo0f4tTGHkqTDFlYWWQw',
                    url: 'https://store77.net/apple_iphone_13/telefon_apple_iphone_13_128gb_starlight/?utm_source=ecatalog&utm_medium=cpc&utm_campaign=apple_iphone_13',
                },
            });
        });
    });

    describe('SKU', () => {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/558171067/sku/101106238734?promo=nomooa');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card');
            assert.strictEqual(
                await bro.getTitle(),
                'Смартфон Apple iPhone 11 128 ГБ RU, фиолетовый, Slimbox — Цены',
                'не установился нужный заголовок вкладки',
            );
        });

        it('Внешний вид с избранным', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/558171067/sku/101106238734?promo=nomooa&exp_flags=enable_favorites=1');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            await bro.assertView('plain', '.Card', { compositeImage: false, allowViewportOverflow: true });
        });

        it('Дозагрузка офферов', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/722974019/sku/101077348745');
            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            const { length: lengthBeforeExpand } = await bro.$$('.ShopList-Item');

            assert.equal(lengthBeforeExpand, 5, 'В свернутом состоянии должно быть 5 офферов');

            await tryToExpandOfferList(bro, 20, 'После первого клика должно получится 20 офферов');
            await tryToExpandOfferList(bro, 30, 'После дозагрузки должно получится 30 офферов');
        });

        it('Цель Метрики по переходу', async function() {
            const bro = this.browser;
            await bro.yaOpenPageByUrl('/products/product/722974019/sku/101077348745?promo=nomooa&exp_flags=analytics_disabled=0');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.click('.ShopItem-Link');
            await bro.yaCheckMetrikaGoal({
                counterId: '84153601',
                name: 'from-sku-to-shop-link',
                params: {
                    offerId: 'ffPjfzT995Txo0YFJghcvg',
                    url: 'https://berudevice.ru/products/smartfon-apple-iphone-12-128gb-belyj-ru',
                },
            });
        });

        describe('Показывает график с динамикой цен', () => {
            it('По умолчанию', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177753?promo=nomooa&exp_flags=price_charts_enabled');

                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.yaScroll('.Card-PriceTrend');
                await bro.waitForVisible('.PriceTrend-Chart', 3000, 'график динамики цен не появился');

                await bro.assertView('price-trend-default-chart', '.Card-PriceTrend');
            });

            it('После редизайна', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177753?promo=nomooa&exp_flags=price_charts_enabled&exp_flags=price_trend_redesign');

                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.yaScroll('.Card-PriceTrend');
                await bro.waitForVisible('.PriceTrend-Chart', 3000, 'график динамики цен не появился');

                await bro.assertView('price-trend-redesign-chart', '.Card-PriceTrend');
            });
        });

        describe('Нотификация об изменении цены, после перехода по ссылке из уведомления', function() {
            it('Показывается, если цена изменилась', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177752?promo=nomooa&exp_flags=price_charts_enabled=1;enable_price_subscription=1;sku_subscriptions_enabled=1&expected_price=20999');

                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.yaWaitForVisible('.NotificationList');

                assert.strictEqual(await bro.getText('.NotificationList'), 'Цена на товар успела измениться');
            });

            it('Не показывается, если цена не изменилась', async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl('/products/product/1414986413/sku/101446177752?promo=nomooa&exp_flags=price_charts_enabled;enable_price_subscription;sku_subscriptions_enabled&expected_price=62000');

                await bro.yaWaitForVisible('.Card', 'карточка товара не появилась');
                await bro.yaShouldNotBeVisible('.NotificationList');
            });
        });

        it('Подписка на снижение цены', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/752262035/sku/101100443755?promo=nomooa&exp_flags=price_charts_enabled&exp_flags=enable_price_subscription&exp_flags=sku_subscriptions_enabled');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
            await bro.yaScroll('.Card-PriceSubscription');
            await bro.waitForVisible('.Card-PriceSubscription', 3000, 'раздел подписки на снижение цены не появился');

            assert.strictEqual(
                await bro.getText('.PriceSubscription-OpenButton'), 'Следить за снижением цены',
            );

            await bro.click('.PriceSubscription-OpenButton');
            await bro.yaWaitForVisible('.PriceSubscription-Modal', 3000, 'всплывающее окно "Подписка на снижение цены" не появилось');

            await bro.yaMockXHR({
                urlDataMap: {
                    '\/products\/api\/ext\/subscriptions': '{"status":"ok","data":{"id":"sku\/101100443755\/3","email":"rus.nick.kar@yandex.ru","ru_price":99990,"start_tracking":1655997078}}',
                },
                status: 200,
            });

            await bro.click('.PriceSubscription-SubscribeButton');
            await bro.yaWaitForHidden('.PriceSubscription-Modal', 3000, 'всплывающее окно "Подписка на снижение цены" не скрылось');

            assert.strictEqual(
                await bro.getText('.PriceSubscription-UnsubscribeButton'), 'Отменить подписку',
            );

            await bro.yaMockXHR({
                urlDataMap: {
                    '\/products\/api\/ext\/subscriptions': '{"status":"ok","data":{"id":"sku\/101100443755\/3"}}',
                },
                status: 200,
            });

            await bro.click('.PriceSubscription-UnsubscribeButton');
            await bro.yaWaitForVisible('.Notification-Message', 3000, 'всплывающее сообщение нотификации не появилось');

            assert.strictEqual(
                await bro.getText('.Notification-Message'), 'Подписка на изменение цены отменена',
            );
        });

        describe('Подписка на снижение цены для неавторизованных пользователей', function() {
            it('Авторизация пользователя', async function() {
                const bro = this.browser;
                const pagePathname = '/products/product/752262035/sku/101100443755';
                const pageUrl = `${pagePathname}?promo=nomooa&exp_flags=price_trend_redesign;price_charts_enabled;sku_subscriptions_enabled;enable_price_subscription;enable_price_subscription_unauthorized&lr=213`;

                await bro.yaOpenPageByUrl(pageUrl);
                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

                await bro.yaScroll('.Card-PriceSubscription');
                await bro.yaWaitForVisible('.Card-PriceSubscription', 3000, 'раздел подписки на снижение цены не появился');
                await bro.yaWaitForVisible('.PriceSubscription-OpenButton', 3000, 'кнопка "Следить за снижением цены" не появилась');

                assert.strictEqual(await bro.getText('.PriceSubscription-OpenButton'), 'Следить за снижением цены');

                await bro.click('.PriceSubscription-OpenButton');
                await bro.yaWaitForPageLoad();

                const authURL = new URL(await bro.getUrl());
                const authRetpathURL = new URL(authURL.searchParams.get('retpath') ?? '');

                assert.strictEqual(authURL.origin, 'https://passport.yandex.ru');
                assert.strictEqual(authURL.searchParams.get('origin'), 'products_subscription');
                assert.strictEqual(authRetpathURL.pathname, pagePathname);
                assert.strictEqual(authRetpathURL.hash, '#price_subscription');
            });

            it('Первоначальная подписка', async function() {
                const bro = this.browser;

                await bro.authOnRecord('plain-user');
                await bro.yaOpenPageByUrl('/products/product/752262035/sku/101100443755?promo=nomooa&exp_flags=price_trend_redesign;price_charts_enabled;sku_subscriptions_enabled;enable_price_subscription;enable_price_subscription_unauthorized&lr=213#price_subscription');

                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.yaWaitForVisible('.Card-PriceSubscription', 3000, 'раздел подписки на снижение цены не появился');
                await bro.yaWaitForVisible('.PriceSubscription-Modal', 3000, 'всплывающее окно "Подписка на снижение цены" не появилось');

                await bro.yaMockXHR({
                    urlDataMap: {
                        '\/products\/api\/ext\/subscriptions': '{"status":"ok","data":{"id":"sku\/101100443755\/3","email":"yndx-products-plain-user@yandex.ru","ru_price":99990,"start_tracking":1655997078}}',
                    },
                    status: 200,
                });
                await bro.click('.PriceSubscription-SubscribeButton');
                await bro.yaWaitForHidden('.PriceSubscription-Modal', 3000, 'всплывающее окно "Подписка на снижение цены" не скрылось');

                assert.strictEqual(await bro.getText('.PriceSubscription-UnsubscribeButton'), 'Отменить подписку');
            });

            it('Повторная подписка', async function() {
                const bro = this.browser;

                await bro.authOnRecord('plain-user');
                await bro.yaOpenPageByUrl('/products/product/1486873434/sku/101517459750?promo=nomooa&exp_flags=price_trend_redesign;price_charts_enabled;sku_subscriptions_enabled;enable_price_subscription;enable_price_subscription_unauthorized&lr=213#price_subscription');

                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.yaWaitForVisible('.Card-PriceSubscription', 3000, 'раздел подписки на снижение цены не появился');
                await bro.yaShouldNotBeVisible('.PriceSubscription-Modal', 'всплывающее окно "Подписка на снижение цены" не должно отображаться');
                await bro.yaWaitForVisible('.PriceSubscription-UnsubscribeButton', 3000, 'кнопка "Отменить подписку" не появилась');

                assert.strictEqual(await bro.getText('.PriceSubscription-UnsubscribeButton'), 'Отменить подписку');

                await bro.yaMockXHR({
                    urlDataMap: {
                        '\/products\/api\/ext\/subscriptions': '{"status":"ok","data":{"id":"sku\/101517459750\/3"}}',
                    },
                    status: 200,
                });
                await bro.click('.PriceSubscription-UnsubscribeButton');
                await bro.yaWaitForVisible('.Notification-Message', 3000, 'всплывающее сообщение нотификации не появилось');

                assert.strictEqual(await bro.getText('.Notification-Message'), 'Подписка на изменение цены отменена');
            });
        });

        it('Проверка проставления метки utm_referrer', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/product/722974019/sku/101077347753?retpath=%2Fsearch%3Ftext%3Diphone%252012');

            await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');

            const shopItem = await bro.$('.ShopItem-Link');
            const url = new URL(await shopItem.getAttribute('href'));
            assert(
                url.searchParams.has('utm_referrer'),
                'В ссылке нет метки utm_referrer',
            );

            const referrer = new URL(url.searchParams.get('utm_referrer') as string);
            assert.equal(referrer.pathname, '/products/search', 'Неверный путь в значении utm_referrer');
            assert.equal(referrer.searchParams.get('text'), 'iphone 12', 'Неверный текст запроса в значении utm_referrer');
        });
    });

    describe('Восстановление поискового запроса', () => {
        it('Из параметра retpath', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/hDx2JIZJelmnHtODxIM6NQ?retpath=%2Fsearch%3Ftext%3Diphone');
            const input = await bro.$('.Header .mini-suggest__input');

            assert.strictEqual(await input.getValue(), 'iphone', 'Поисковый запрос не взят из параметра retpath');
        });

        it('Из параметра retpath с опечаткой', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/search?text=мужской залат');
            await bro.yaWaitForVisible('.Misspell', 3000, 'Опечаточник не появился');
            await bro.click('.ProductCard');
            await bro.refresh();

            await bro.yaWaitForVisible('.Header', 'Хэдер не отобразился');
            const input = await bro.$('.Header .mini-suggest__input');

            assert.strictEqual(await input.getValue(), 'мужской халат', 'Неверный поисковый запрос');
        });

        it('Из параметра text', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/hDx2JIZJelmnHtODxIM6NQ?text=iphone');
            const input = await bro.$('.Header .mini-suggest__input');

            assert.strictEqual(await input.getValue(), 'iphone', 'Поисковый запрос не взят из параметра text');
        });

        it('Из категории', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/hDx2JIZJelmnHtODxIM6NQ');
            const input = await bro.$('.Header .mini-suggest__input');

            assert.strictEqual(await input.getValue(), 'Мобильные телефоны', 'Поисковый запрос не взят из категории');
        });
    });

    // тесты на счетчики не браузерозависимы, для ускорения гоняем только на двух платформах
    hermione.only.in(['linux-chrome', 'linux-chrome-iphone']);
    describe('Баобаб', () => {
        [
            { name: 'оффера', url: '/products/offer/HcUgV37NWyQ2ixFLRnL28g', subservice: 'offer' },
            { name: 'модели', url: '/products/product/1414858424', subservice: 'product' },
            { name: 'sku', url: '/products/product/558171067/sku/101116062742', subservice: 'sku' },
        ].forEach(({ name, url, subservice }) => {
            it(`Отправка дерева в blockstat для страницы ${name}`, async function() {
                const bro = this.browser;

                await bro.yaOpenPageByUrl(url);
                await bro.yaWaitForPageLoad();

                const reqid = await bro.yaGetReqId();

                const { client } = await bro.getCounters(reqid);
                const serverTree = await bro.yaGetBaobabTree();
                const currentSubservice = serverTree?.tree?.attrs?.subservice;

                assert.strictEqual(serverTree.event, 'show', 'дерево с событием show не отправилось в blockstat');
                assert.strictEqual(currentSubservice, subservice, 'не правильный subservice в атрибутах дерева');
                assert.isEmpty(client, 'при загрузке страницы логи не должны отправляться с клиента');
            });

            it(`Соответствие деревьев на клиенте и на сервере для страницы ${name}`, async function() {
                const bro = this.browser;

                await bro.yaOpenPageByUrl(url);
                await bro.yaWaitForPageLoad();
                await bro.yaCompareServerAndClientBaobabTree();
            });

            it(`Логирование перехода по офферу на странице ${name}`, async function() {
                const bro = this.browser;

                await bro.yaOpenPageByUrl(url);
                await bro.yaWaitForPageLoad();
                await bro.click('.ShopItem');

                await bro.yaCheckBaobabEvent({ path: '$page.$main.card.shopList.listControls.shopItem.link' });
            });

            it(`Отправка parent-reqid в url-атрибуте $page на странице ${name}`, async function() {
                const bro = this.browser;

                await bro.yaOpenPageByUrl(`${url}?parent-reqid=123`);
                await bro.yaWaitForPageLoad();

                const serverTree = await bro.yaGetBaobabTree();
                const loggedUrl = new URL(serverTree.tree.attrs?.url);
                assert.equal(loggedUrl.searchParams.get('parent-reqid'), '123', 'неверный parent-reqid в url-атрибуте $page');

                const currentUrl = await bro.getUrl();
                const { searchParams } = new URL(currentUrl);
                assert(!searchParams.has('parent-reqid'), 'parent-reqid не удалился из url страницы');
            });
        });

        it('Логирование перехода по кнопке "В магазин" на странице оффера', async function() {
            const bro = this.browser;

            await bro.yaOpenPageByUrl('/products/offer/HcUgV37NWyQ2ixFLRnL28g');
            await bro.yaWaitForPageLoad();
            await bro.click('.Card-Button');

            await bro.yaCheckBaobabEvent({ path: '$page.$main.card.externalShopLink' });
        });
    });

    describe('Пожаловаться', function() {
        [
            { name: 'оффера', url: '/products/offer/RYI0Y-D2hwebM5lgVzo7Bw', subservice: 'offer' },
            { name: 'модели', url: '/products/product/1414858424', subservice: 'product' },
            { name: 'sku', url: '/products/product/10822064/sku/10822064', subservice: 'sku' },
        ].forEach(({ name, url }) => {
            hermione.skip.in('linux-chrome-iphone');
            it(`Вид с кнопкой на странице ${name}`, async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl(`${url}?promo=nomooa`);

                await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.assertView('inactive-layout', '.Card-ShopListWrapper', {
                    ignoreElements: ['.Card-ShopList'],
                });

                const feedbackTogglerBaobabPath = '$page.$main.card.feedbackMode';
                const feedbackModeBaobabNode = await bro.yaGetBaobabNode({ path: feedbackTogglerBaobabPath });
                assert.isOk(feedbackModeBaobabNode, 'В баобаб не залогирована нода переключения в режим "Пожаловаться"');

                /** клик по "Пожаловаться" */
                await bro.click('.Card-Link');

                await bro.yaCheckBaobabEvent({ path: feedbackTogglerBaobabPath });

                await bro.assertView('active-layout', '.Card-ShopListWrapper', {
                    ignoreElements: ['.Card-ShopList'],
                });

                const link = await bro.$('.ShopItem-Link');
                const href = await link.getAttribute('href');
                assert(href !== '', 'В режиме "Пожаловаться" у оффера в списке предложений должна быть ссылка');

                const parsed = new URL(href, 'https://yandex.ru');
                const expectedOrigin = 'https://forms.yandex.ru';
                assert.equal(parsed.origin, expectedOrigin, `Ссылка в режиме "Пожаловаться" должна вести на ${expectedOrigin}`);
                assert.isTrue(
                    parsed.searchParams.has('offerId'),
                    'В ссылке на форму пожаловаться нет обязательного параметра offerId',
                );
                await bro.assertView('active-shop-item', '.ShopItem', {
                    ignoreElements: ['.ShopItem-Favicon', '.ShopItem-NameWrapper', '.ShopItem-PriceWrapper'],
                });
            });

            // тесты на счетчики не браузерозависимы, для ускорения гоняем только на двух платформах
            hermione.only.in(['linux-chrome', 'linux-chrome-iphone']);
            it(`Клик на странице ${name} логируется один раз`, async function() {
                const bro = this.browser;
                await bro.yaOpenPageByUrl(`${url}?promo=nomooa`);

                // await bro.yaWaitForVisible('.Card', 3000, 'карточка товара не появилась');
                await bro.yaWaitForPageLoad();

                /** клик по "Пожаловаться" */
                await bro.click('.Card-Link');
                /** клик по офферу */
                await bro.click('.ShopItem');

                /** Записи в лог долетают не сразу */
                await bro.pause(2000);
                const clickEvents = await bro.yaGetBaobabSentEvents('click');

                /** Ожидаем, что будет залогировано 2 клика, по пожаловаться и по офферу */
                assert.lengthOf(clickEvents, 2, 'Залогировано больше или меньше кликов, чем ожидается');
            });
        });
    });
});

const tryToExpandOfferList = async(
    bro: WebdriverIO.Browser,
    expectLengthAfterExpand: number,
    errorText: string,
) => {
    const { length: currentLength } = await bro.$$('.ShopList-Item');
    await bro.scroll(0, 20000);
    await bro.click('.Card-ShopList .ListCut-CutControl');
    await bro.yaCheckBaobabEvent({ path: '$page.$main.card.shopList.listControls' });

    let lengthAfterExpand;

    await bro.waitUntil(async() => {
        const { length } = await bro.$$('.ShopList-Item');
        if (length > currentLength) {
            lengthAfterExpand = length;
            return true;
        }
        return false;
    }, {
        timeout: 5000,
        timeoutMsg: 'Список офферов не развернулся',
    });

    assert.equal(lengthAfterExpand, expectLengthAfterExpand, errorText);
};
