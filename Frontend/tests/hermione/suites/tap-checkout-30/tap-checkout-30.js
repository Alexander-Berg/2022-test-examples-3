const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-30.json');

describe('tap-checkout-30: Доставка. Переход на экран "Новый адрес" с возвратом назад', function() {
    beforeEach(async function() {
        await precondition(this.browser);
    });

    async function precondition(bro) {
        await bro.auth('tap-checkout-54');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();

        await bro.click(checkoutPage.deliveryMethodsFirstOption);
        await bro.submitOrderAndCheckResult();
    }

    it('Должен работать переход к экрану "Новый адрес"', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitIconsLoad();

        await checkoutPage.clickCompactDeliveryButton(bro);

        await bro.click(checkoutPage.deliveryProfileButtonNewAddresses);
        await bro.waitForVisible(checkoutPage.addressScreen, 5000);
        await bro.waitForVisible(checkoutPage.deliveryScreen, 1000, true);

        await bro.click(checkoutPage.getHeaderBackButtonByScreen(checkoutPage.addressScreen));
        await bro.waitForVisible(checkoutPage.deliveryScreen, 5000);
        await bro.waitForVisible(checkoutPage.addressScreen, 1000, true);

        await bro.click(checkoutPage.deliveryProfileButtonNewAddresses);
        await bro.waitForVisible(checkoutPage.addressScreen, 5000);
        await bro.waitForVisible(checkoutPage.deliveryScreen, 1000, true);

        await bro.back();
        await bro.waitForVisible(checkoutPage.deliveryScreen, 5000);
        await bro.waitForVisible(checkoutPage.addressScreen, 1000, true);
    });
});
