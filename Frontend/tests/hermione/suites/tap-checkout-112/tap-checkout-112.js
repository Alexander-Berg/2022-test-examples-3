const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-112.json');
const errorTestData = require('./tap-checkout-112-error.json');

describe('tap-checkout-112: Доставка. Отображение ошибки под схлопнутом блоке при условии, что ошибка произошла в адресе доставки', function() {
    beforeEach(async function() {
        await precondition(this.browser);
    });

    async function precondition(bro) {
        await bro.auth('tap-checkout-112');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();

        await bro.click(checkoutPage.deliveryMethodsFirstOption);
        await bro.submitOrderAndCheckResult();
    }

    it('Ошибка должна отображаться под схлопнутым блоком и под адресом доставки', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitIconsLoad();

        await checkoutPage.clickCompactDeliveryButton(bro);
        await checkoutPage.searchAndClickDeliveryAddress(bro, 'улица Малышева, 51');

        // Делаем паузу, чтобы успел скрыться лоадер,
        // так как он показывается через 250мс в течение минимум 750мс
        await bro.pause(1000);
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.handleCheckoutEvent('shippingAddressChange', errorTestData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryProfileAddressesError);
        await bro.assertView('deliveryScreenError', checkoutPage.root);

        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.assertView('deliveryCompactError', checkoutPage.root);

        await checkoutPage.clickCompactDeliveryButton(bro);
        await checkoutPage.searchAndClickDeliveryAddress(bro, 'улица Ленина, 65А');
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.assertView('deliveryScreenAfterFixError', checkoutPage.root);

        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.assertView('deliveryCompactAfterFixError', checkoutPage.root);
    });
});
