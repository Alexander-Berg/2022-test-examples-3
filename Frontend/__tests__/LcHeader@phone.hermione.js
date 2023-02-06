specs({
    feature: 'LcHeader',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока с полупрозрачным фоном', function() {
        return this.browser
            .url('/turbo?stub=lcheader/custom-background.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcHeader())
            .click(PO.lcHeader.cartButton())
            .assertView('cart-opened', PO.lcHeader.cartPopup())
            .click(PO.lcHeader.menuButton())
            .assertView('modal-opened', PO.lcHeaderModal());
    });
});
