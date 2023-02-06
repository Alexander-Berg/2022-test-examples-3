specs({
    feature: 'beruSpecs',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока (mode = label)', function() {
        return this.browser
            .url('/turbo?stub=beruspecs/label.json')
            .yaWaitForVisible(PO.page(), 'Страница не загружена')
            .yaCheckClientErrors()
            .yaWaitForVisible(PO.blocks.beruSpecs.label(), 'Компонент BeruSpecsLabel не отобразился')
            .assertView('label', PO.blocks.beruSpecs.label());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока (mode = preview)', function() {
        return this.browser
            .url('/turbo?stub=beruspecs/preview.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .yaWaitForVisible(PO.blocks.beruSpecs.preview(), 'Компонент "Характеристики и описание" не отобразился')
            .assertView('preview', PO.blocks.beruSpecs.preview());
    });
});
