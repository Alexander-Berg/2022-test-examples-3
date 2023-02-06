const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-108.json');

describe('tap-checkout-108: Основной. Переход на экран "Новый адрес" с возвратом назад', function() {
    it('Должен работать переход к экрану "Новый адрес"', async function() {
        const bro = this.browser;

        await bro.auth('tap-checkout-54');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();

        await bro.click(checkoutPage.deliveryProfileButtonNewAddresses);
        await bro.waitForVisible(checkoutPage.addressScreen, 5000);
        await bro.waitForVisible(checkoutPage.mainScreen, 1000, true);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.addressScreen, 1000, true);

        await bro.click(checkoutPage.deliveryProfileButtonNewAddresses);
        await bro.waitForVisible(checkoutPage.addressScreen, 5000);
        await bro.waitForVisible(checkoutPage.mainScreen, 1000, true);

        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.addressScreen, 1000, true);
    });
});
