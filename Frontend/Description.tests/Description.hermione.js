specs({
    feature: 'Description',
    experiment: 'Реализация на react',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=description/react.json&exp_flags=alternative-sociality-controls=1&hermione_commentator=stub')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.socialityControlsIdle(), 'Не появился блок социальности')
            .assertView('plain', PO.descriptionReact());
    });
});
