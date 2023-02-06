specs({
    feature: 'CartIcon',
}, () => {
    function screenshotIcon(browser) {
        return browser
            .yaWaitForVisible(PO.cartIcon(), 'На странице нет иконки корзины')
            .assertView('plain', PO.cartIcon());
    }

    describe('Данные с сервера (дефолт)', function() {
        hermione.only.notIn('safari13');
        it('Внешний вид с товарами', function() {
            return this.browser
                .url('/turbo?stub=carticon/default.json&exp_flags=turboforms_endpoint=/multiple/')
                .then(() => screenshotIcon(this.browser));
        });

        hermione.only.notIn('safari13');
        it('Внешний вид с пустой корзиной', function() {
            return this.browser
                .url('/turbo?stub=carticon/default.json&exp_flags=turboforms_endpoint=/empty/')
                .then(() => screenshotIcon(this.browser));
        });

        hermione.only.notIn('safari13');
        it('Ссылка на корзину', function() {
            return this.browser
                .url('/turbo?stub=carticon/default.json&exp_flags=turboforms_endpoint=/')
                .yaWaitForVisible(PO.cartIcon(), 'На странице нет иконки корзины')
                .yaCheckLink({
                    selector: PO.cartIcon(),
                    message: 'Неправильная ссылка на корзину',
                    target: '_self',
                    url: {
                        href: '/turbo?stub=cart/default.json',
                        ignore: ['protocol', 'hostname'],
                    },
                });
        });
    });

    describe('Данные из localstorage', function() {
        hermione.only.notIn('safari13');
        it('Проверка работы', function() {
            return this.browser
                .url('/turbo?stub=cart/ls-clear-data.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .url('/turbo?stub=carticon/ls-read-data.json')
                .yaWaitForVisible(PO.cartIcon(), 'На странице нет иконки корзины')
                .getText(PO.cartIcon())
                .then(value => {
                    assert.equal(value, '', 'После чистки данных корзина не пустая');
                })
                .url('/turbo?stub=cart/ls-set-data.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .url('/turbo?stub=cart/ls-set-data-another-shopid.json')
                .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
                .url('/turbo?stub=carticon/ls-read-data.json')
                .yaWaitForVisible(PO.cartIcon(), 'На странице нет иконки корзины')
                .getText(PO.cartIcon())
                .then(value => {
                    assert.equal(value, '8', 'Неправильно установилось количество товаров для первого магазина');
                })
                .url('/turbo?stub=carticon/ls-read-data-another-shopId.json')
                .yaWaitForVisible(PO.cartIcon(), 'На странице нет иконки корзины')
                .getText(PO.cartIcon())
                .then(value => {
                    assert.equal(value, '102', 'Неправильно установилось количество товаров для второго магазина');
                })
                .assertView('plain', PO.page());
        });
    });
});
