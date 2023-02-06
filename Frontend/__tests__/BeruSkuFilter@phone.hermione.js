specs({
    feature: 'beruSkuFilter',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=beruskufilter/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока, только текст', function() {
        return this.browser
            .url('/turbo?stub=beruskufilter/text.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('text', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Sku фильтры должны проматываться в iframe', function() {
        return this.browser
            .yaOpenInIframe('?stub=beruskufilter/default.json')
            .yaWaitForVisible(PO.blocks.beruSkuFilters(), 'Страница не загрузилась')
            .yaTouchScroll(PO.blocks.beruSkuFilters(), 100)
            .assertView('iframe-scroll', PO.blocks.beruSkuFilters());
    });
});
