specs({
    feature: 'marketSection',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketsection/with_text.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('with_text', PO.page());
    });
});
