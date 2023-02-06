const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-39.json');

describe('tap-checkout-39: Доставка. Переход к экрану редактирования адреса с возвратом назад', function() {
    it('Должен работать переход к экрану редактирования адреса с основного экрана', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-54');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.waitForVisible(checkoutPage.deliveryProfileButtonAddressesEdit);

        await checkoutPage.clickAddressesEditButton(bro);
        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.addressScreen, 1000, true);

        await checkoutPage.clickAddressesEditButton(bro);
        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.addressScreen, 1000, true);
    });
});
