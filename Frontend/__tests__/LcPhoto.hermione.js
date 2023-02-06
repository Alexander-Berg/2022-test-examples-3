specs({
    feature: 'LcPhoto',
}, () => {
    hermione.only.in(['chrome-phone', 'iphone', 'searchapp', 'chrome-desktop', 'firefox']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcphoto/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcPhoto());
    });
});
