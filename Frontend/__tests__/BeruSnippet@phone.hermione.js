specs({
    feature: 'beruSnippet',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с кнопкой "В корзину"', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/add-to-cart.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaMockImages()
            .assertView('add-to-cart', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Кнопка "В корзину" должна быть кликабельна и вести на переданный url', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/add-to-cart.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.blocks.beruSnippetAddToCart(),
                'Кнопка "В корзину" должна быть кликабельна'
            )
            .then(url => {
                assert.equal(url.href, 'https://yandex.ru/', 'Кнопка должна вести на https://yandex.ru/');
            });
    });

    hermione.only.notIn('safari13');
    it('Цель должна отправляться в метрику при клике по кнопке "В корзину"', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/add-to-cart.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            .click(PO.blocks.beruSnippetAddToCart())
            .yaCheckMetrikaGoal({
                name: 'ADD_TO_CART',
                counterId: '11111111',
                prams: {
                    one: 1,
                },
            });
    });

    hermione.only.notIn('safari13');
    it('Сниппет должен быть кликабельным и вести на переданный url', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/add-to-cart.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.blocks.beruSnippetLink(),
                'Cниппет должен быть кликабельным',
            ).then(url => {
                assert.equal(url.href, 'https://beru.ru/', 'Сниппет должен вести на https://beru.ru/');
            });
    });

    hermione.only.notIn('safari13');
    it('Клик по сниппету должен инициировать отправку гола в метрику', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/add-to-cart.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            .click(PO.blocks.beruSnippetLink())
            .yaCheckMetrikaGoal({
                name: 'NAVIGATE_GOAL',
                counterId: '11111111',
                params: {
                    one: 1,
                    two: 2,
                },
            });
    });

    hermione.only.notIn('safari13');
    it('При попадании сниппета в видимую область экрана, должен отправлять гол видимости элемента', function() {
        return this.browser
            .url('/turbo?stub=berusnippet/add-to-cart.json&exp_flags=analytics-disabled=0')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
            .yaCheckMetrikaGoal({
                name: 'VISIBILITY_GOAL',
                counterId: '11111111',
                params: {
                    one: 1,
                    two: 2,
                },
            })
            .yaIndexify(PO.blocks.beruSnippet())
            .yaScrollPage(`${PO.blocks.beruSnippet()}[data-index="2"]`)
            .yaCheckMetrikaGoal({
                name: 'VISIBILITY_GOAL',
                counterId: '11111111',
                params: {
                    three: 3,
                },
            });
    });
});
