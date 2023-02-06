specs({
    feature: 'marketRatingDistribution',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('turbo?stub=marketratingdistribution%2Fdefault.json')
            .yaWaitForVisible(PO.blocks.marketRatingDestribution(), 'Компонента нет на странице')
            .assertView('plain', PO.blocks.marketRatingDestribution());
    });
});
