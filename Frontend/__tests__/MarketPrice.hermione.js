specs({
    feature: 'marketPrice',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока size=m', function() {
        return this.browser
            .url('/turbo?stub=marketprice/size_m.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.marketPrice(), 'Компонент не отрендерился')
            .assertView('size_m', PO.blocks.marketPrice());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока size=s', function() {
        return this.browser
            .url('/turbo?stub=marketprice/size_s.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.marketPrice(), 'Компонент не отрендерился')
            .assertView('size_s', PO.blocks.marketPrice());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока size=xs', function() {
        return this.browser
            .url('/turbo?stub=marketprice/size_xs.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.marketPrice(), 'Компонент не отрендерился')
            .assertView('size_xs', PO.blocks.marketPrice());
    });
});
