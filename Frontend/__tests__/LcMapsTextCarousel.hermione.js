specs({
    feature: 'LcMapsTextCarousel',
}, () => {
    hermione.only.notIn('safari13');
    it('Текстовая карусель без анимации', function() {
        return this.browser
            .url('/turbo?stub=lcmapstextcarousel/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcMapsTextCarousel(), 'Tекстовая карусель не появилась')
            .assertView('plain', PO.page());
    });
});
