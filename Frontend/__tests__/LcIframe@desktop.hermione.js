specs({
    feature: 'LcIframe',
}, () => {
    it('Фрейм с текстом', function() {
        return this.browser
            .setViewportSize(({
                width: 1100,
                height: 800,
            }))
            .url('/turbo?stub=lciframe/default.json')
            .yaWaitForVisible(PO.lcPage(), 'Секция LcIframe не появилась')
            .assertView('plain', PO.lcPage());
    });
});
