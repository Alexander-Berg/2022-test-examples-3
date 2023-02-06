specs({
    feature: 'marketDelivery',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketdelivery/with_day_benefit.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.marketDelivery(), 'На странице нет компонента MarketDelivery')
            .assertView('with_day_benefit', PO.blocks.marketDelivery());
    });
});
