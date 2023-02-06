specs({
    feature: 'LcHeader',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcheader/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeader());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока c корзиной', function() {
        return this.browser
            .url('/turbo?stub=lcheader/with-cart.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeader());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид кнопки корзины с рамкой', function() {
        return this.browser
            .url('/turbo?stub=lcheader/with-cart-border.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeader.cartButton());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с попапом', function() {
        return this.browser
            .url('/turbo?stub=lcheader/add-to-cart.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.lcHeader.cartButton())
            .assertView('empty', PO.page())
            .click(PO.lcHeader.cartButton())
            .click(PO.lcAddToCart())
            .click(PO.lcAddToCart())
            .click(PO.lcHeader.cartButton())
            .assertView('full', PO.page())
            .click(PO.cart.list.firstItem.amountPicker.decrButton())
            .pause(200) // дожидаемся пока разрешится promise в CartData
            .click(PO.cart.list.firstItem.amountPicker.incrButton())
            .pause(200) // дожидаемся пока разрешится promise в CartData
            .click(PO.cart.list.firstItem.amountPicker.incrButton())
            .pause(200) // дожидаемся пока разрешится promise в CartData
            .click(PO.cart.list.firstItem.amountPicker.incrButton())
            .pause(200) // дожидаемся пока разрешится promise в CartData
            .assertView('changed-with-buttons', PO.page())
            .click(PO.cart.list.firstItem.remove())
            .assertView('deleted', PO.page());
    });
});
