hermione.skip.in(/searchapp|chrome-phone/, 'skipped by problem with scroll');
specs({
    feature: 'LcCheckout',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид с оплатой и доставкой', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/default.json')
            .yaWaitForVisible(PO.lcCheckout(), 'На странице нет LcCheckout')
            .pause(700)
            .assertView('full', PO.lcCheckout());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид без оплаты', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/without-payment.json')
            .yaWaitForVisible(PO.lcCheckout(), 'На странице нет LcCheckout')
            .assertView('noPayment', PO.lcCheckout());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид без оплаты и доставки', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/without-payment-and-delivery.json')
            .yaWaitForVisible(PO.lcCheckout(), 'На странице нет LcCheckout')
            .assertView('short', PO.lcCheckout());
    });

    hermione.only.notIn('safari13');
    it('Выбор оплаты заблокирован, если есть только один вариант оплаты', function() {
        return this.browser
            .url('/turbo?stub=lccheckout/with-single-payment.json')
            .yaWaitForVisible(PO.lcCheckout(), 'На странице нет LcCheckout')
            .assertView('singlePaymentSelect', PO.lcCheckout.paymentSelect());
    });
});
