specs({
    feature: 'marketSnippet',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketsnippet%2Fdefault.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .yaWaitForVisible(PO.blocks.marketSnippet(), 'Компонент карточки модели')
            .assertView('plain', PO.blocks.marketSnippet());
    });
});
