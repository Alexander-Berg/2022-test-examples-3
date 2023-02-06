specs({
    feature: 'LcPage',
}, () => {
    hermione.only.notIn('safari13');
    it('Содержимое LcPage корректно отображается', function() {
        return this.browser
            .url('/turbo?stub=lcpage/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPage());
    });
});
