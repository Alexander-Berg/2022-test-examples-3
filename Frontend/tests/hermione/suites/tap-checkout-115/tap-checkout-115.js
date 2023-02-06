const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-115.json');

describe('tap-checkout-115: Доставка. Отображение сообщения "Необходимо указать адрес доставки" при условии, что на экране доставки был выбран город, отличный от адреса доставки', function() {
    beforeEach(async function() {
        await precondition(this.browser);
    });

    async function precondition(bro) {
        await bro.auth('tap-checkout-115');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitDeliveryProfileAddressesLoad();

        await bro.click(checkoutPage.deliveryMethodsFirstOption);
        await bro.submitOrderAndCheckResult();
    }

    it('Должно отображаться сообщение "Необходимо указать адрес доставки"', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);

        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactDeliveryButton(bro);
        await checkoutPage.selectCityFromDefaultList(bro, 'Екатеринбург');
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.assertView('deliveryScreenAfterChangeCity', checkoutPage.root);

        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.assertView('deliveryCompactError', checkoutPage.root);

        await checkoutPage.clickCompactDeliveryButton(bro);
        await bro.waitForVisible(checkoutPage.deliveryProfileAddressesError, 5000);

        await bro.click(checkoutPage.deliveryProfileAddressesFirstOption);
        await bro.waitDeliveryProfileAddressesLoad();
        await bro.assertView('deliveryScreenAfterFixError', checkoutPage.root);

        await bro.back();
        await bro.waitForVisible(checkoutPage.mainScreen, 5000);
        await bro.assertView('deliveryCompactAfterFixError', checkoutPage.root);
    });
});
