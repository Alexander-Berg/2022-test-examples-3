specs({
    feature: 'LcGroup',
}, () => {
    hermione.only.notIn('safari13');
    it('Содержимое LcGroup корректно отображается', function() {
        return this.browser
            .url('/turbo?stub=lcgroup/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не отобразилась')
            .assertView('plain', PO.lcGroup());
    });
    hermione.only.notIn('safari13');
    it('Содержимое LcGroup корректно отображается с каруселью', function() {
        return this.browser
            .url('/turbo?stub=lcgroup/carousel.json')
            .yaWaitForVisible(PO.page(), 'Страница не отобразилась')
            .assertView('carousel', PO.lcGroup());
    });
});
