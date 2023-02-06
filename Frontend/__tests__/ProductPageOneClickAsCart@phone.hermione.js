const URL = require('url');

specs({
    feature: 'product-page',
    type: 'с кнопкой "купить в 1 клик" на инфраструктуре корзины',
}, () => {
    hermione.only.notIn('safari13');
    it('На странице с корзиной', function() {
        let host;
        let etagOnLoad;

        return this.browser
            .timeouts('script', 3000)
            .url('/')
            .url().then(res => {
                const url = URL.parse(res.value);
                host = url.hostname;
            })
            .then(() => this.browser.yaStartResourceWatcher(
                '/static/turbo/hermione/mock-external-resources.sw.js',
                [],
            ))
            .url('?stub=productpage/new-one-click-buy-with-cart.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .execute(function() {
                return window.Ya.store.getState().cart.etag;
            })
            .then(({ value }) => {
                etagOnLoad = value;
            })
            .click(PO.oneClickBuyButton())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(requests => {
                assert.equal(requests.length, 2, 'Количество запросов не совпадает');

                const requestToCartSave = JSON.parse(requests[requests.length - 1].text);

                assert.strictEqual(requestToCartSave.etag, undefined, 'Передали etag в 1 клик');
                assert.equal(requestToCartSave.sk, '', 'В запросе нет sk');
            })
            .yaWaitForVisible(PO.modal(), 'Модальное окно не открылось')
            .execute(function() {
                return window.Ya.store.getState().cart.etag;
            })
            .then(({ value }) => {
                assert.equal(value, etagOnLoad, 'При добавлении товара в 1 клик поменялся etag');
            })
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.oneClickForm.inputText())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Модальное окно с формой не показалось')
            .setValue(PO.firstOneClickForm.nameField.control(), 'error')
            .setValue(PO.firstOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.firstOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.firstOneClickForm.address.control(), 'Адрес')
            .click(PO.firstOneClickForm.submit())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/final/`)
            )
            .then(requests => {
                assert.equal(requests.length, 1, 'Засабмитили форму 1 клика не в ручку корзины');
            });
    });

    hermione.only.notIn('safari13');
    it('На странице без корзины', function() {
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
                [],
            ))
            .url('?stub=productpage/new-one-click-buy-no-cart.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.oneClickBuyButton())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/`)
            )
            .then(requests => {
                assert.equal(requests.length, 2, 'Количество запросов не совпадает');

                const requestToCartSave = JSON.parse(requests[requests.length - 1].text);

                assert.strictEqual(requestToCartSave.etag, undefined, 'Передали etag в 1 клик');
                assert.equal(requestToCartSave.sk, '', 'В запросе нет sk');
            })
            .yaWaitForVisible(PO.modal(), 'Модальное окно не открылось')
            .yaIndexify(PO.oneClickForm())
            .yaIndexify(PO.oneClickForm.inputText())
            .yaWaitForVisible(PO.firstOneClickForm(), 'Модальное окно с формой не показалось')
            .setValue(PO.firstOneClickForm.nameField.control(), 'error')
            .setValue(PO.firstOneClickForm.phoneField.control(), '88001234567')
            .setValue(PO.firstOneClickForm.emailField.control(), 'test@test.ru')
            .setValue(PO.firstOneClickForm.address.control(), 'Адрес')
            .click(PO.firstOneClickForm.submit())
            .then(() => this.browser
                .yaGetExternalResourcesRequests(`https://${host}/submit/shopping-cart/final/`)
            )
            .then(requests => {
                assert.equal(requests.length, 1, 'Засабмитили форму 1 клика не в ручку корзины');
            });
    });
});
