hermione.skip.in(/searchapp|chrome-phone/, 'skipped by problem with scroll');
specs({
    feature: 'LcCheckout',
}, () => {
    hermione.only.notIn('safari13');
    it('Должен переключаться способ оплаты на mobile', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCheckout(), 'Checkout не появилась')
            .assertView('pickup-mobile', PO.lcCheckout())
            .click(PO.lcSelect.mobile())
            .click(PO.lcSelect.secondOptionMobile())
            .assertView('delivery', PO.lcCheckout());
    });
});
