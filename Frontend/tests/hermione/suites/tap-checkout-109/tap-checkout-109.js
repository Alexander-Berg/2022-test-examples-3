const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-109.json');

describe('tap-checkout-109: tap-checkout-109: Доставка. Отображение активного адреса при первом открытии чекаута при условии, что запрошен адрес и город доставки', function() {
    it('Должен быть выбран адрес и город доставки', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-109');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.waitForVisible(checkoutPage.deliveryAddressProfile, 10000);

        await bro.assertView('deliveryAddressProfileActive', checkoutPage.root);
    });
});
