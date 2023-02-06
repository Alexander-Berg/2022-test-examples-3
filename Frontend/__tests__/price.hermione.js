specs({
    feature: 'price',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=price/default.json')
            .yaWaitForVisible(PO.price(), 'Блок цены не появился на странице')
            .assertView('plain', PO.price());
    });

    hermione.only.notIn('safari13');
    it('Полный вид блока', function() {
        return this.browser
            .url('/turbo?stub=price/full.json')
            .yaWaitForVisible(PO.price(), 'Блок цены не появился на странице')
            .assertView('plain', PO.price());
    });

    hermione.only.notIn('safari13');
    it('Полный вид блока locale = en', function() {
        return this.browser
            .url('/turbo?stub=price/full.json&l10n=en')
            .yaWaitForVisible(PO.price(), 'Блок цены не появился на странице')
            .assertView('plain', PO.price());
    });

    describe('Денежные единицы', function() {
        hermione.only.notIn('safari13');
        it('locale = ru', function() {
            return this.browser
                .url('/turbo?stub=price/currency.json')
                .yaWaitForVisible(PO.price(), 'Блок цены не появился на странице')
                .assertView('plain', PO.page());
        });

        hermione.only.notIn('safari13');
        it('locale = en', function() {
            return this.browser
                .url('/turbo?stub=price/currency.json&l10n=en')
                .yaWaitForVisible(PO.price(), 'Блок цены не появился на странице')
                .assertView('plain', PO.page());
        });

        hermione.only.notIn('safari13');
        it('locale = uk', function() {
            return this.browser
                .url('/turbo?stub=price/currency.json&l10n=uk')
                .yaWaitForVisible(PO.price(), 'Блок цены не появился на странице')
                .assertView('plain', PO.page());
        });
    });
});
