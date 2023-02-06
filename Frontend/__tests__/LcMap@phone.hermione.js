specs({
    feature: 'LcMap',
}, () => {
    hermione.only.notIn('safari13');
    it('Карта с маршрутом', function() {
        return this.browser
            .url('/turbo?stub=lcmap/default.json')
            .yaWaitForVisible(PO.lcPage(), 'Секция LcMap не появилась')
            .assertView('plain', PO.lcPage(), { ignoreElements: PO.lcIframe.iframe() });
    });
});
