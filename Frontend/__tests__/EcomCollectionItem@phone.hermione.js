specs({
    feature: 'EcomCollectionItem',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('?stub=ecomcollectionitem/default.json')
            .yaWaitForVisible(PO.page())
            .yaIndexify(PO.ecomCollectionItem())
            .assertView('plain', [PO.firstEcomCollectionItem(), PO.secondEcomCollectionItem()]);
    });

    hermione.only.notIn('safari13');
    it('Редизайн', function() {
        return this.browser
            .url('?stub=ecomcollectionitem/default.json&exp_flags=ecommerce-design=1')
            .yaWaitForVisible(PO.page())
            .yaIndexify(PO.ecomCollectionItem())
            .assertView('plain', [PO.firstEcomCollectionItem(), PO.secondEcomCollectionItem()]);
    });
});
