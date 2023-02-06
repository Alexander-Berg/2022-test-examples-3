specs({
    feature: 'marketProductTitle',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketproducttitle/default.json')
            .yaWaitForVisible(PO.blocks.marketProductTitle(), 'На странице нет компонента MarketProductTitle')
            .assertView('plain', PO.blocks.marketProductTitle());
    });
});
