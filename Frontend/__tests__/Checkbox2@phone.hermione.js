specs({
    feature: 'checkbox2',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=checkbox2/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.row');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока (Кастомные стили)', function() {
        return this.browser
            .url('/turbo?stub=checkbox2/custom-theme.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', '.row');
    });

    hermione.only.notIn('safari13');
    it('Выбор значений', function() {
        return this.browser
            .url('/turbo?stub=checkbox2/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.checkbox2First())
            .click(PO.checkbox2Second())
            .click(PO.checkbox2Third())
            .click(PO.checkbox2Fourth())
            .assertView('clicked', '.row')
            .click(PO.checkbox2First())
            .click(PO.checkbox2Second())
            .click(PO.checkbox2Third())
            .click(PO.checkbox2Fourth())
            .assertView('plain', '.row');
    });

    hermione.only.notIn('safari13');
    it('Снятие значений', function() {
        return this.browser
            .url('/turbo?stub=checkbox2/all-checked.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.checkbox2First())
            .click(PO.checkbox2Second())
            .click(PO.checkbox2Third())
            .click(PO.checkbox2Fourth())
            .assertView('plain', '.row')
            .click(PO.checkbox2First())
            .click(PO.checkbox2Second())
            .click(PO.checkbox2Third())
            .click(PO.checkbox2Fourth())
            .assertView('checked', '.row');
    });
});
