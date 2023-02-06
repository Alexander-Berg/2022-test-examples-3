specs({
    feature: 'LcMap',
}, () => {
    it('Карта с маршрутом', function() {
        return this.browser
            .setViewportSize(({
                width: 1100,
                height: 800,
            }))
            .url('/turbo?stub=lcmap/default.json')
            .yaWaitForVisible(PO.lcPage(), 'Секция LcMap не появилась')
            .assertView('plain', PO.lcPage(), { ignoreElements: PO.lcIframe.iframe() });
    });
});
