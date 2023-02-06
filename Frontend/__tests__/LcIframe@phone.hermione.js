specs({
    feature: 'LcIframe',
}, () => {
    hermione.only.notIn('safari13');
    it('Фрейм с текстом', function() {
        return this.browser
            .url('/turbo?stub=lciframe/default.json')
            .yaWaitForVisible(PO.lcIframe(), 'Секция LcIframe не появилась')
            .assertView('plain', PO.lcIframe());
    });
});
