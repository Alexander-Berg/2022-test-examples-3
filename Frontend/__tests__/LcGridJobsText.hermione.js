specs({
    feature: 'LcGridJobsText',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=lcgridjobstext/default.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });
});
