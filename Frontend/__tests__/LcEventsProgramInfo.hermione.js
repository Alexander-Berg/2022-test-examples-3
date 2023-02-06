specs({
    feature: 'LcEventsProgramInfo',
}, () => {
    hermione.only.notIn('safari13');
    it('Инфо элемента программы', function() {
        return this.browser
            .url('/turbo?stub=lceventsprograminfo/default.json')
            .yaWaitForVisible(PO.page(), 'Cтраница не загрузилась')
            .assertView('plain', PO.lcEventsProgramInfo());
    });
});
