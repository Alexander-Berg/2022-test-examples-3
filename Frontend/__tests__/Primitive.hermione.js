specs({
    feature: 'Primitive',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид негидрированного блока', function() {
        return this.browser.url('?stub=primitive/default.json')
            .yaWaitForVisible(PO.page())
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид гидрированного блока', function() {
        return this.browser.url('?stub=primitive/hydrated.json')
            .yaWaitForVisible(PO.page())
            .assertView('hydrated', PO.page());
    });
});
