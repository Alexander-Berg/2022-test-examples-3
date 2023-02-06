specs({
    feature: 'LcAddToCart',
}, () => {
    hermione.only.notIn('safari13');
    it('Добавление товара в корзину', function() {
        return this.browser
            .url('/turbo?stub=lccart/clear-data.json')
            .yaWaitForVisible(PO.cart(), 'На странице нет корзины')
            .url('/turbo?stub=lcaddtocart/default.json')
            .yaWaitForVisible(PO.lcAddToCart(), 'На странице нет кнопки добавления товара')
            .assertView('empty', PO.lcAddToCart())
            .click(PO.lcAddToCart())
            .assertView('first', PO.lcAddToCart())
            .click(PO.lcAddToCart())
            .assertView('second', PO.lcAddToCart())
            .refresh()
            .assertView('refresh', PO.lcAddToCart());
    });
});
