specs({
    feature: 'marketRatingSpread',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('turbo?stub=marketratingspread%2Fdefault.json')
            .yaWaitForVisible(PO.blocks.marketRatingSpread(), 'Компонента нет на странице')
            .assertView('plain', PO.blocks.marketRatingSpread())
            .yaCheckClientErrors();
    });
});
