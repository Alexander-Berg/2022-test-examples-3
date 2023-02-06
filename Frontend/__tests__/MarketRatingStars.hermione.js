specs({
    feature: 'marketRatingStars',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketratingstars/default.json')
            .yaWaitForVisible(PO.blocks.marketRatingStars(), 'Компонента нет на странице')
            .assertView('plain', PO.blocks.marketRatingStars())
            .yaCheckClientErrors();
    });
});
