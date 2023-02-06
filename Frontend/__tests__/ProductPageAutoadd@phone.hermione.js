const URL = require('url');

specs({
    feature: 'product-page',
    type: 'С автоматическим добавлением в корзину (autoadd)',
}, () => {
    hermione.only.notIn('safari13');
    it('Без перехода в корзину', function() {
        let host;

        return this.browser
            .timeouts('script', 3000)
            .url('/')
            .url()
            .then(res => {
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
            .url('?stub=productpage/product-2-server.json&autoadd=norelocate&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.cartIcon.count())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(response => {
                assert.isOk(response.length, 2, 'При открытии страницы не отправили запрос на добавление товара');
            });
    });

    hermione.only.notIn('safari13');
    it('С переходом в корзину', function() {
        return this.browser
            .url('?stub=productpage/product-2-server.json&autoadd=tocart&exp_flags=turboforms_endpoint=/&utm_source=button')
            .yaWaitUntil('Не произошло редиректа на страницу корзины', () =>
                this.browser
                    .getUrl()
                    .then(url => url.includes('ecomcartpage%2Fdefault.json') && url.includes('utm_source=button'))
            );
    });

    hermione.only.notIn('safari13');
    it('Не добавляем, если товар уже есть в корзине', function() {
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
                            cart: {
                                data: {
                                    items: [{
                                        count: 1,
                                        product: {
                                            description: 'GoPro',
                                            price: { current: '1790', currencyId: 'RUR' },
                                            href: '/turbo?stub=productpage/product-1.json&exp_flags=platform=touch',
                                            meta: 'CiBC2hxN7fyAthVvJbZoRD9TpcLvOSyQVG6cD9VjdhOODxKFAgj3wp3oBRL8AQoKMzI0MjQyNDIzNBI+R29Qcm8gbWluaVVTQiDQutCw0LHQtdC70Ywg0LTQu9GPINC/0L7QtNC60LsuINC6INCiViBBQ01QUy0zMDEaPy90dXJibz9zdHViPXByb2R1Y3RwYWdlL3Byb2R1Y3QtMS5qc29uJmV4cF9mbGFncz1wbGF0Zm9ybT10b3VjaCJeClZodHRwOi8vYXZhdGFycy1pbnQubWRzdC55YW5kZXgubmV0L2dldC10dXJiby81MTUwLzJhMDAwMDAxNjdmNDQwMzkwM2IzYjA0OTk5ZjRjNTY1YWFiMBDYBBjYBCoEMTc5MDoDUlVSQgI1JQ==',
                                            id: '13482',
                                            thumb: { src: '', height: 600, block: 'image', width: 600 },
                                        },
                                    }],
                                },
                                block: 'cart',
                            },
                        },
                    },
                ],
            ))
            .url('?stub=productpage/product-2-server.json&autoadd=norelocate&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.cartIcon.count())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(response => {
                assert.isOk(response.length, 1, 'Отправился запрос на добавление, хотя товар уже в корзине');
            });
    });
});
