specs({
    feature: 'beruDiscountBadge',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид бейджа', function() {
        return this.browser
            .url('/turbo?stub=berudiscountbadge/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
