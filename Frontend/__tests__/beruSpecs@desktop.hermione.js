specs({
    feature: 'beruSpecs',
}, () => {
    it('Отображение модального окна по клику (mode = label)', function() {
        return this.browser
            .url('/turbo?stub=beruspecs/label.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.beruSpecs.label(), 'Компонент BeruSpecsLabel не загрузился')
            .assertView('label', PO.blocks.beruSpecs.label())
            .click(PO.blocks.beruSpecs.label())
            .yaWaitForVisible(PO.blocks.beruSpecsModal(), 'Модальное окно не появилось')
            .yaCheckClientErrors()
            .assertView('title', PO.blocks.beruSpecsModal.title())
            .yaScrollPage(PO.blocks.beruSpecsModal.description(), 0)
            .assertView('description', PO.blocks.beruSpecsModal.description())
            .yaScrollPage(PO.blocks.beruSpecsModal.firstGroupSpecs(), 0)
            .assertView('first-group-specs', PO.blocks.beruSpecsModal.firstGroupSpecs())
            .yaScrollPage(PO.blocks.beruSpecsModal.secondGroupSpecs(), 0)
            .assertView('second-group-specs', PO.blocks.beruSpecsModal.secondGroupSpecs());
    });

    it('Отображение модального окна по клику (mode = preview)', function() {
        return this.browser
            .url('/turbo?stub=beruspecs/preview.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.beruSpecs.preview(), 'Компонент "Характеристики и описание" не отобразился')
            .assertView('preview', PO.blocks.beruSpecs.preview())
            .click(PO.blocks.beruSpecs.preview.moreButton())
            .yaWaitForVisible(PO.blocks.beruSpecsModal(), 'Модальное окно не появилось')
            .yaCheckClientErrors()
            .assertView('title', PO.blocks.beruSpecsModal.title())
            .yaScrollPage(PO.blocks.beruSpecsModal.description(), 0)
            .assertView('description', PO.blocks.beruSpecsModal.description())
            .yaScrollPage(PO.blocks.beruSpecsModal.firstGroupSpecs(), 0)
            .assertView('first-group-specs', PO.blocks.beruSpecsModal.firstGroupSpecs())
            .yaScrollPage(PO.blocks.beruSpecsModal.secondGroupSpecs(), 0)
            .assertView('second-group-specs', PO.blocks.beruSpecsModal.secondGroupSpecs());
    });
});
