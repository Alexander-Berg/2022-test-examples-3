specs({
    feature: 'beruFiltersLayout',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока со всеми опциями', function() {
        return this.browser
            .url('/turbo?stub=berufilterslayout/all-options.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('all-options', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока c кнопкой в состоянии загрузки', function() {
        return this.browser
            .url('/turbo?stub=berufilterslayout/load-state.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('load-state', PO.page());
    });
});
