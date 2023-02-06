specs({
    feature: 'beruSearchList',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=berusearchlist/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.beruSearchList());
    });

    hermione.only.notIn('safari13');
    it('Проверка кликабельности cниппета', function() {
        return this.browser
            .url('/turbo?stub=berusearchlist/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.blocks.beruSearchList.firstSnippetLink(),
                'Cниппет должен быть кликабельным и отрываться в этом же окне',
                { target: '_self' }
            )
            .then(url => {
                const reg = new RegExp(`/turbo\\?text\=${encodeURIComponent('https://m.pokupki.market.yandex.ru/product/')}.*`);

                assert.match(url.href, reg, 'Неверная ссылка');
            });
    });

    hermione.only.notIn('safari13');
    it('Проверка кликабельности кнопки "В коризну"', function() {
        // Для разных сред запуска используются разные ссылки из офера(encoded, direct)
        // Поэтому в тесте сделан воркараунт для отладки теста и запуска его в боевом окружении(в PR тесты запускаются в другом окружении)
        let expectedHref = process.env.NODE_ENV === 'development' ?
            'https://beru.ru/bundle/100131944742?schema=type,objId,count&data=offer,mp0pfcAKzQ8MED0qtUM_uA,1&fromTurbo=1&clid=926&lr=213' :
            'https://market-click2.yandex.ru/test';

        return this.browser
            .url('/turbo?stub=berusearchlist/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.blocks.beruSearchList.firstSnippetAddToCartButton(),
                'Кнопка "В корзину" должна быть кликабельна и открываться в новом окне',
                { target: '_blank' }
            )
            .then(url => {
                assert.equal(url.href, expectedHref, 'Не верная ссылка "В корзину"');
            });
    });

    hermione.only.notIn('safari13');
    it('Клик по сниппету должен инициировать отправку гола в метрику', function() {
        return this.browser
            .url('/turbo?stub=berusearchlist/default.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            // блокируем переход по ссылке иначе в этой же вкладке откроется новая страница при клике и метрику не получится проверить.
            .execute(function(selector) {
                const elem = document.querySelector(selector);

                elem.setAttribute('href', 'javascript:void(0)');
            }, PO.blocks.beruSearchList.firstSnippetLink())
            .click(PO.blocks.beruSearchList.firstSnippetLink())
            .yaCheckMetrikaGoal({
                name: 'SNIPPET-NAVIGATE',
                counterId: '47628343',
                params: {
                    isTest: true,
                    skuId: '100131944742',
                    offerId: 'f239fk3',
                    marketSkuCreator: 'market',
                    price: 4800,
                    oldPrice: 9473,
                    showPlaceId: 'dfjsf7',
                },
            });
    });

    hermione.only.notIn('safari13');
    it('Клик по кнопке "В корзину" должен инициировать отправку гола в метрику', function() {
        return this.browser
            .url('/turbo?stub=berusearchlist/default.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            .click(PO.blocks.beruSearchList.firstSnippetAddToCartButton())
            .yaCheckMetrikaGoal({
                name: 'ADD-TO-CART',
                counterId: '47628343',
                params: {
                    isTest: true,
                    skuId: '100131944742',
                    offerId: 'f239fk3',
                    marketSkuCreator: 'market',
                    price: 4800,
                    oldPrice: 9473,
                    showPlaceId: 'dfjsf7',
                },
            });
    });

    hermione.only.notIn('safari13');
    it('При попадании сниппета в видимую область экрана, должен отправлять гол видимости элемента', function() {
        return this.browser
            .url('/turbo?stub=berusearchlist/default.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            .yaCheckMetrikaGoal({
                name: 'SNIPPET-VISIBLE',
                counterId: '47628343',
                params: {
                    isTest: true,
                    skuId: '100131944742',
                    offerId: 'f239fk3',
                    marketSkuCreator: 'market',
                    price: 4800,
                    oldPrice: 9473,
                    showPlaceId: 'dfjsf7',
                },
            })
            .yaCheckMetrikaGoal({
                name: 'SNIPPET-VISIBLE',
                counterId: '47628343',
                params: {
                    isTest: true,
                    skuId: '100131944741',
                    offerId: '32dfs234',
                    marketSkuCreator: 'market',
                    price: 9473,
                    showPlaceId: 'dfjsf7',
                },
            });
    });
});
