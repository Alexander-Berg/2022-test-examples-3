specs({
    feature: 'Sale',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид', function() {
        return this.browser
            .url('?stub=sale/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Редизайн', function() {
        return this.browser
            .url('?stub=sale/default.json&exp_flags=ecommerce-design=1')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
