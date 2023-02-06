specs({
    feature: 'LcClipart',
}, () => {
    hermione.only.in(['chrome-phone', 'iphone', 'searchapp', 'chrome-desktop', 'firefox']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcclipart/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcClipart());
    });

    hermione.only.in(['chrome-phone', 'iphone', 'searchapp', 'chrome-desktop', 'firefox']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока с маленькой картинкой', function() {
        return this.browser
            .url('/turbo?stub=lcclipart/small.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('small', PO.lcClipart());
    });
});
