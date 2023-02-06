specs({
    feature: 'EmcColumns',
}, () => {
    hermione.only.notIn('safari13');
    it('Три колонки с равной шириной', function() {
        return this.browser
            .url('/turbo?stub=emccolumns/3-columns.json')
            .yaWaitForVisible(PO.emcColumns(), 'Колонки не появились')
            .assertView('emccolumns', PO.emcColumns());
    });

    hermione.only.notIn('safari13');
    it('Две колонки с равной шириной и запретом переноса на мобильных устройствах', function() {
        return this.browser
            .url('/turbo?stub=emccolumns/2-columns-prevent-stacking-on-mobile.json')
            .yaWaitForVisible(PO.emcColumns(), 'Колонки не появились')
            .assertView('emccolumns', PO.emcColumns());
    });

    hermione.only.notIn('safari13');
    it('Две колонки с фиксированной шириной в пикселях', function() {
        return this.browser
            .url('/turbo?stub=emccolumns/2-columns-with-width-px.json')
            .yaWaitForVisible(PO.emcColumns(), 'Колонки не появились')
            .assertView('emccolumns', PO.emcColumns());
    });

    hermione.only.notIn('safari13');
    it('Три колонки с фиксированной шириной в процентах', function() {
        return this.browser
            .url('/turbo?stub=emccolumns/3-columns-with-width-percent.json')
            .yaWaitForVisible(PO.emcColumns(), 'Колонки не появились')
            .assertView('emccolumns', PO.emcColumns());
    });

    hermione.only.notIn('safari13');
    it('Три колонки с несколькими секциями в каждой', function() {
        return this.browser
            .url('/turbo?stub=emccolumns/3-columns-many-sections.json')
            .yaWaitForVisible(PO.emcColumns(), 'Колонки не появились')
            .assertView('emccolumns', PO.emcColumns());
    });
});
