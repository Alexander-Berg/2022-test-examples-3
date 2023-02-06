specs({
    feature: 'LcCheckout',
}, () => {
    it('Должен переключаться способ оплаты на desktop', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCheckout(), 'Checkout не появилась')
            .assertView('pickup-desktop', PO.lcCheckout())
            .click(PO.lcSelect.desktop())
            .click(PO.lcSelect.secondOption())
            .assertView('delivery', PO.lcCheckout());
    });

    it('LcCheckout при выбранной онлайн оплате', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/with-only-online-payment.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCheckout(), 'Checkout не появилась')
            .assertView('online-payment', PO.lcCheckout());
    });
});
