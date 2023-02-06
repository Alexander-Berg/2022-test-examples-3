specs({
    feature: 'LcMarketAffiliate',
}, () => {
    hermione.only.notIn('safari13');
    it('Виджеты маркета', function() {
        return this.browser
            .url('/turbo?stub=lcmarketaffiliate/default.json')
            .yaWaitForVisible(PO.lcMarketAffiliate(), 'Секция LcMarketAffiliate не появилась')
            .yaWaitForVisible(PO.lcMarketAffiliate.widgetItem(), 'Виджет не появился')
            .assertView('plain', PO.lcMarketAffiliate(), {
                ignoreElements: PO.lcMarketAffiliate.widgetItem(),
            });
    });
});
