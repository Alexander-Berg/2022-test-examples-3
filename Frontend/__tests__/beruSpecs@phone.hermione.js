specs({
    feature: 'beruSpecs',
}, () => {
    hermione.only.in('iphone', 'yaTouchScroll в мобильных браузерах');
    hermione.only.notIn('safari13');
    it('Отображение модального окна по тапу (mode = label)', function() {
        return this.browser
            .url('/turbo?stub=beruspecs/label.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.beruSpecs.label(), 'Компонент BeruSpecsLabel не загрузился')
            .assertView('label', PO.blocks.beruSpecs.label())
            .click(PO.blocks.beruSpecs.label())
            .yaWaitForVisible(PO.blocks.beruSpecsModal(), 'Модальное окно не появилось')
            .yaCheckClientErrors()
            .assertView('title', PO.blocks.beruSpecsModal.title())
            .yaTouchScroll(PO.blocks.beruSpecsModal.description(), 0, 0)
            .assertView('description', PO.blocks.beruSpecsModal.description())
            .yaTouchScroll(PO.blocks.beruSpecsModal.firstGroupSpecs(), 0, 0)
            .assertView('first-group-specs', PO.blocks.beruSpecsModal.firstGroupSpecs())
            .yaTouchScroll(PO.blocks.beruSpecsModal.secondGroupSpecs(), 0, 0)
            .assertView('second-group-specs', PO.blocks.beruSpecsModal.secondGroupSpecs());
    });

    hermione.only.in('iphone', 'yaTouchScroll в мобильных браузерах');
    hermione.only.notIn('safari13');
    it('Отображение модального окна по тапу (mode = preview)', function() {
        return this.browser
            .url('/turbo?stub=beruspecs/preview.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.beruSpecs.preview(), 'Компонент "Характеристики и описание" не отобразился')
            .assertView('preview', PO.blocks.beruSpecs.preview())
            .click(PO.blocks.beruSpecs.preview.moreButton())
            .yaWaitForVisible(PO.blocks.beruSpecsModal(), 'Модальное окно не появилось')
            .yaCheckClientErrors()
            .assertView('title', PO.blocks.beruSpecsModal.title())
            .yaTouchScroll(PO.blocks.beruSpecsModal.description(), 0, 0)
            .assertView('description', PO.blocks.beruSpecsModal.description())
            .yaTouchScroll(PO.blocks.beruSpecsModal.firstGroupSpecs(), 0, 0)
            .assertView('first-group-specs', PO.blocks.beruSpecsModal.firstGroupSpecs())
            .yaTouchScroll(PO.blocks.beruSpecsModal.secondGroupSpecs(), 0, 0)
            .assertView('second-group-specs', PO.blocks.beruSpecsModal.secondGroupSpecs());
    });
});
