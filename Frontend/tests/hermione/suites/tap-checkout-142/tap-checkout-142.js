const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-142-default.json');
const pickupTestData = require('./tap-checkout-142-pickup.json');

describe('tap-checkout-142: Доставка. Отображение информации о постамате в схлопнутом блоке', function() {
    beforeEach(async function() {
        await precondition(this.browser);
    });

    async function precondition(bro) {
        await bro.auth('tap-checkout-142');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();

        await bro.click(checkoutPage.deliveryMethodsFirstOption);
        await bro.submitOrderAndCheckResult();
    }

    it('Информация о выбранном постамате должна отображаться в схлопнуом блоке', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);

        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactDeliveryButton(bro);
        await checkoutPage.searchAndClickDeliveryMethod(bro, 'Самовывоз');
        await bro.handleCheckoutEvent('shippingOptionChange', pickupTestData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('deliveryScreenAfterChangeDeliveryMethod', checkoutPage.root);

        await bro.click(checkoutPage.deliveryPickupOptionControl);
        await bro.waitForVisible(checkoutPage.deliveryPickupOptionControl, 5000, true);
        await bro.waitForVisible(checkoutPage.pickupScreen, 5000);

        await checkoutPage.searchAndClickPickupOptionLabel(bro, 'Москва Жулебинский');
        await bro.handleCheckoutEvent('pickupOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('deliveryScreenAfterSelectPickup', checkoutPage.root);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.waitForVisible(checkoutPage.deliveryScreen, 1000, true);
        await bro.assertView('compactPickup', checkoutPage.root);

        await bro.submitOrderAndCheckResult();
    });
});
