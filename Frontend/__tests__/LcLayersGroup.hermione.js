specs({
    feature: 'LcLayersGroup',
}, () => {
    hermione.only.notIn('safari13');
    it('Слоёная группа', function() {
        return this.browser
            .url('/turbo?stub=lclayersgroup/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не отобразилась')
            .assertView('plain', PO.lcLayersGroup());
    });
});
