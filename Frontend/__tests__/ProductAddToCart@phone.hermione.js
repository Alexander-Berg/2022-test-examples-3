const URL = require('url');

specs({
    feature: 'ProductAddToCart',
}, () => {
    hermione.only.notIn('safari13');
    it('Добавление товара в корзину', function() {
        return this.browser
            .url('/turbo?stub=productaddtocart/default.json&exp_flags=turboforms_endpoint=/empty/')
            .yaWaitForVisible(PO.productAddToCart(), 'На странице нет кнопки добавления товара')
            .yaIndexify(PO.productAddToCart())
            .assertView('empty', [
                PO.firstProductAddToCart(),
                PO.secondProductAddToCart(),
            ])
            .yaShouldNotBeVisible(PO.firstProductAddToCartAdded())
            .yaShouldNotBeVisible(PO.secondProductAddToCartAdded())
            .click(PO.firstProductAddToCart())
            .yaShouldBeVisible(PO.firstProductAddToCartAdded())
            .yaShouldNotBeVisible(PO.secondProductAddToCartAdded())
            .assertView('first', [
                PO.firstProductAddToCart(),
                PO.secondProductAddToCart(),
            ])
            .click(PO.secondProductAddToCart())
            .yaShouldBeVisible(PO.firstProductAddToCartAdded())
            .yaShouldBeVisible(PO.secondProductAddToCartAdded())
            .yaCheckLink({
                selector: PO.firstProductAddToCartAdded(),
                message: 'Неправильная ссылка на корзину в первой кнопке',
                target: '_self',
                url: {
                    href: '/turbo?stub=ecomcartpage/default.json',
                    ignore: ['protocol', 'hostname'],
                },
            })
            .yaCheckLink({
                selector: PO.secondProductAddToCartAdded(),
                message: 'Неправильная ссылка на корзину во второй кнопке',
                target: '_self',
                url: {
                    href: '/turbo?stub=ecomcartpage/default.json',
                    ignore: ['protocol', 'hostname'],
                },
            });
    });

    hermione.only.notIn('safari13');
    it('Товар добавлен в корзину при загрузке', function() {
        return this.browser
            .url('/turbo?stub=productaddtocart/default.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.productAddToCart(), 'На странице нет кнопки добавления товара')
            .yaIndexify(PO.productAddToCart())
            .yaShouldBeVisible(PO.firstProductAddToCartAdded());
    });

    hermione.only.notIn('safari13');
    it('Проверка отправки подписи', function() {
        let host;

        return this.browser
            .timeouts('script', 3000)
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                [
                    {
                        url: `https://${host}/submit/shopping-cart/`,
                        response: {
                            status: 'success',
                            cart: {},
                        },
                    },
                ],
            ))
            .url('/turbo?text=https%3A%2F%2Fkupicase.ru%2Fproducts%2Fzaschitnoe-steklo-dlya-zte-blade-20-smart-s-otstupami-ot-kraya-ekrana&patch=setActionKey&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.productAddToCart(), 'На странице нет кнопки добавления товара')
            .click(PO.productAddToCart(), 'На странице нет кнопки добавления товара')
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(requests => {
                assert.isOk(requests.length, 'Нет запросов к ручке корзины');

                requests.forEach(request => {
                    const requestBody = JSON.parse(request.text);
                    assert.isOk(requestBody.sk, `Запрос не содержит sk: ${request.url} ${request.text}`);
                });
            });
    });
});
