specs({
    feature: 'LcBadgeComponent',
}, () => {
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcbadgecomponent/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPage());
    });
});
