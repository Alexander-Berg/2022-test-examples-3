specs({
    feature: 'LcCart',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=lccart/default.json')
            .yaWaitForVisible(PO.lcCart(), 'На странице нет корзины')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .assertView('plain', PO.lcCart());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с разными соотношениями сторон изображений товара', function() {
        return this.browser
            .url('/turbo?stub=lccart/image-ratios.json')
            .yaWaitForVisible(PO.lcCart(), 'На странице нет корзины')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .assertView('ratios', PO.lcCart());
    });

    hermione.skip.in(['chrome-phone', 'searchapp'], 'Тесты проходят нестабильно.');
    hermione.only.notIn('safari13');
    it('При клике на кнопку открывается lcCheckout', function() {
        return this.browser
            .url('/turbo?stub=lccart/with-checkout.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCart(), 'На странице нет корзины')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .click(PO.lcCart.confirmButton())
            .yaWaitForVisible(PO.lcCheckout())
            .assertView('checkout', PO.lcCheckout());
    });

    hermione.only.notIn('safari13');
    it('Ошибка отправки заказа', function() {
        return this.browser
            .url('/turbo?stub=lccart/with-checkout.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCart(), 'На странице нет корзины')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .click(PO.lcCart.confirmButton())
            .yaWaitForVisible(PO.lcCheckout(), 'Не отобразилась форма заказа')
            .click(PO.lcInput.text.input())
            .keys('fail'.split(''))
            .click(PO.lcForm.submit())
            .yaWaitForVisible(PO.orderFail(), 'Не показался статус-скрин Ошибка отправки')
            .assertView('orderFail', PO.orderFail());
    });

    hermione.only.notIn('safari13');
    it('Успех оплаты', function() {
        return this.browser
            .url('/turbo?stub=lccart/with-checkout.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.lcCart(), 'На странице нет корзины')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .click(PO.lcCart.confirmButton())
            .yaWaitForVisible(PO.lcCheckout())
            .click(PO.lcInput.text.input())
            .keys('online'.split(''))
            .click(PO.lcForm.submit())
            .yaWaitForVisible(PO.orderLoading(), 'Не показался статус-скрин Подтверждение заказа')
            .yaWaitForVisible(PO.blocks.trustIframe(), 'Не показалася фрейм оплаты')
            .yaWaitForVisible(PO.paymentSuccess(), 'Не показалася статус-скрин успешной оплаты')
            .assertView('paymentSuccess', PO.paymentSuccess());
    });
});
