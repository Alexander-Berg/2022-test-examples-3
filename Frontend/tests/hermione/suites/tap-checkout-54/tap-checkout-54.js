const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-54.json');

describe('tap-checkout-54: Доставка. Переход к экрану "Доставка" с последующим возвратом назад', function() {
    beforeEach(async function() {
        await precondition(this.browser);
    });

    it('Должен работать переход к экрану доставки', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);

        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.clickCompactDeliveryButton(bro);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.deliveryScreen, 1000, true);

        await checkoutPage.clickCompactDeliveryButton(bro);

        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.deliveryScreen, 1000, true);
    });

    async function precondition(bro) {
        await bro.auth('tap-checkout-54');

        await checkoutPage.open(bro, defaultTestData);

        // У пользователя tap-checkout-54 преднастроен один адрес доставки, проверяем что он загрузился
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.click(checkoutPage.deliveryMethodsFirstOption);
        await bro.submitOrderAndCheckResult();
    }
});
