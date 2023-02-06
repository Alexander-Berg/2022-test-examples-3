specs({
    feature: 'LcPrice',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcprice/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с английской локалью', function() {
        return this.browser
            .url('/turbo?stub=lcprice/localization.json&l10n=en')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPrice());
    });
});
