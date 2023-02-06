specs({
    feature: 'EcomArrowLink',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=ecomarrowlink/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.ecomArrowLink());
    });
});
