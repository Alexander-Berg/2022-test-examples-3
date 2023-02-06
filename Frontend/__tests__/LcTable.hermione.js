specs({
    feature: 'LcTable',
}, () => {
    hermione.only.notIn('safari13');
    it('Обычная таблица', function() {
        return this.browser
            .url('/turbo?stub=lctable/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcTable());
    });

    hermione.only.notIn('safari13');
    it('Таблица с разными стилями у элементов', function() {
        return this.browser
            .url('/turbo?stub=lctable/different-styles.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcTable());
    });
});
