specs({
    feature: 'beruNativeScroll',
}, () => {
    hermione.only.notIn('safari13');
    it('Элементы должны проматываться в iframe', function() {
        return this.browser
            .yaOpenInIframe('?stub=berunativescroll/default.json')
            .yaWaitForVisible(PO.blocks.beruNativeScroll(), 'Страница не загрузилась')
            .yaTouchScroll(PO.blocks.beruNativeScroll(), 100)
            .assertView('iframe-scroll', PO.blocks.beruNativeScroll());
    });
});
