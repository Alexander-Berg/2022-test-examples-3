specs({
    feature: 'marketPropsList',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketpropslist%2Fdefault.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.marketPropsList());
    });
});
