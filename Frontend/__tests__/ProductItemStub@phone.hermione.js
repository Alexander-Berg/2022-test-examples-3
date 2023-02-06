specs({
    feature: 'ProductItemStub',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=productitemstub/default.json')
            .yaWaitForVisible(PO.products(), 'Заглушки не появились на странице')
            .assertView('default', PO.products.list());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид, тип list', function() {
        return this.browser
            .url('/turbo?stub=productitemstub/list.json')
            .yaWaitForVisible(PO.products(), 'Заглушки не появились на странице')
            .assertView('list', PO.products.list());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид, тип big-list', function() {
        return this.browser
            .url('/turbo?stub=productitemstub/big-list.json')
            .yaWaitForVisible(PO.products(), 'Заглушки не появились на странице')
            .assertView('big-list', PO.products.list());
    });
});
