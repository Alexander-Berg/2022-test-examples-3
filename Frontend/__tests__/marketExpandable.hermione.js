specs({
    feature: 'marketExpandable',
}, () => {
    beforeEach(function() {
        return this.browser.url('/turbo?stub=marketexpandable%2Fdefault.json');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.marketExpandable());
    });

    hermione.only.notIn('safari13');
    it('Разворачивание / сворачивание полного текста по клику', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .click(PO.blocks.marketExpandable.control())
            .assertView('full-text', PO.blocks.marketExpandable())
            .click(PO.blocks.marketExpandable.control())
            .assertView('plain', PO.blocks.marketExpandable());
    });
});
