specs({
    feature: 'beru-header',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока по умолчанию', function() {
        return this.browser
            .url('/turbo?stub=beruheader/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Проверка кликабельности логотипа и корзины', function() {
        return this.browser
            .url('/turbo?stub=beruheader/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.blocks.beruHeader.logo(),
                'Логотип должен быть кликабельный и открываться в новом окне',
                { target: '_blank' }
            )
            .then(url => {
                assert.include(url.href, 'https://m.pokupki.market.yandex.ru', 'Неверная ссылка');
            })
            .yaCheckLinkOpener(
                PO.blocks.beruHeader.cart(),
                'Корзина должна быть кликабельна и открываться в новом окне',
                { target: '_blank' }
            )
            .then(url => {
                assert.include(url.href, 'https://m.pokupki.market.yandex.ru/my/cart', 'Неверная ссылка');
            });
    });
});
