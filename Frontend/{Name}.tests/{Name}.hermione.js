specs({
    feature: '{Name}',
}, () => {
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub={Name}/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.{ClsName}');
    });
});
