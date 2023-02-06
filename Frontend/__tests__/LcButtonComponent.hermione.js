specs({
    feature: 'LcButtonComponent',
}, () => {
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcbuttoncomponent/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcButtonComponent());
    });
});
