specs({
    feature: 'cost',
}, () => {
    describe('Внешний вид', function() {
        hermione.only.notIn('safari13');
        it('locale = ru', function() {
            return this.browser
                .url('/turbo?stub=cost/cost.json')
                .yaWaitForVisible(PO.cost(), 'Блок стоимости не появился на странице')
                .assertView('plain', PO.hermioneContainer());
        });

        hermione.only.notIn('safari13');
        it('locale = en', function() {
            return this.browser
                .url('/turbo?stub=cost/cost.json&l10n=en')
                .yaWaitForVisible(PO.cost(), 'Блок стоимости не появился на странице')
                .assertView('plain', PO.hermioneContainer());
        });

        hermione.only.notIn('safari13');
        it('locale = uk', function() {
            return this.browser
                .url('/turbo?stub=cost/cost.json&l10n=uk')
                .yaWaitForVisible(PO.cost(), 'Блок стоимости не появился на странице')
                .assertView('plain', PO.hermioneContainer());
        });
    });
});
