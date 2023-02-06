specs({
    feature: 'marketShopInfo',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketshopinfo/default.json')
            .yaWaitForVisible(PO.blocks.marketShopInfo(), 'Компонента нет на странице')
            .assertView('plain', PO.blocks.marketShopInfo())
            .yaCheckClientErrors();
    });
});
