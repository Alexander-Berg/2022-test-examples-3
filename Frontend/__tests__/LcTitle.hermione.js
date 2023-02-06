specs({
    feature: 'LcTitle',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lctitle/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcTitle());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с дополнительными свойствами', function() {
        return this.browser
            .url('/turbo?stub=lctitle/extended.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcTitle());
    });
});
