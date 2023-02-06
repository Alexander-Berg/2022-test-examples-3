specs({
    feature: 'marketVisualFilters',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketvisualfilters/default.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.marketSection());
    });
});
