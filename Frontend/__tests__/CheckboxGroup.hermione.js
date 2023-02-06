specs({
    feature: 'CheckboxGroup',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=checkboxgroup/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.checkboxGroup());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока (Кастомные стили)', function() {
        return this.browser
            .url('/turbo?stub=checkboxgroup/custom-theme.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.checkboxGroup());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Внешний вид блока (landscape)', function() {
        return this.browser
            .setOrientation('landscape')
            .url('/turbo?stub=checkboxgroup/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('landscape', PO.checkboxGroup());
    });

    hermione.only.notIn('safari13');
    it('Изменение чекбоксов', function() {
        return this.browser
            .url('/turbo?stub=checkboxgroup/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.checkboxGroup.showAll())
            .assertView('expand', PO.checkboxGroup())
            .yaIndexify(PO.checkboxGroup.item())
            .click(PO.checkboxGroup.itemEighth.box())
            .click(PO.checkboxGroup.itemTenth.checkbox())
            .click(PO.checkboxGroup.itemEleventh.checkbox())
            .click(PO.blocks.filtersActions())
            .assertView('collapse', PO.checkboxGroup())
            .click(PO.checkboxGroup.itemFirst.checkbox())
            .click(PO.checkboxGroup.itemThird.checkbox())
            .click(PO.checkboxGroup.itemFifth.checkbox())
            .assertView('collapse-unchecked', PO.checkboxGroup());
    });
});
