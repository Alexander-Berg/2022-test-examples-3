specs({
    feature: 'beruCheckbox',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока - тип default', function() {
        return this.browser
            .url('/turbo?stub=berucheckbox/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('default', PO.page());
    });
    hermione.only.notIn('safari13');
    it('Внешний вид блока - тип color', function() {
        return this.browser
            .url('/turbo?stub=berucheckbox/color.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('color', PO.page());
    });
    hermione.only.notIn('safari13');
    it('Внешний вид блока - тип multicolor', function() {
        return this.browser
            .url('/turbo?stub=berucheckbox/multicolor.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('multicolor', PO.page());
    });
});
