specs({
    feature: 'marketProductRating',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketproductrating%2Fdefault.json')
            .yaWaitForVisible(PO.blocks.marketSection(), 'Компонента нет на странице')
            .assertView('plain', PO.blocks.marketSection())
            .yaCheckClientErrors();
    });
});
