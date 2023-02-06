hermione.only.in(['iphone', 'chrome-phone', 'searchapp']);

specs({
    feature: 'beruDisclaimer',
}, () => {
    beforeEach(function() {
        return this.browser.url('/turbo?stub=berudisclaimer/default.json');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
