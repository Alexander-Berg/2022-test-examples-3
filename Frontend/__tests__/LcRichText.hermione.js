specs({
    feature: 'LcRichText',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока в uc', function() {
        return this.browser
            .url('/turbo?stub=lcrichtext/uc.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcRichText());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока в lpc', function() {
        return this.browser
            .url('/turbo?stub=lcrichtext/lpc.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcRichText());
    });
});
