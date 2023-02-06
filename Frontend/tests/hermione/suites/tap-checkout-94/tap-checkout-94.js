const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-94.json');

describe('tap-checkout-94: Доставка. Автоматическое изменение города при смене адреса', function() {
    it('Должен измениться город при смене адреса', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-94');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.waitForVisible(checkoutPage.deliveryProfileAddressActive, 20000);
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndClickDeliveryAddress(bro, 'улица Ленина, 17');
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.assertView('deliveryAddressAfterChange', checkoutPage.root);

        await checkoutPage.searchAndClickDeliveryAddress(bro, 'улица Александра Невского, 69');
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.assertView('deliveryAddressAfterReChange', checkoutPage.root);
    });
});
