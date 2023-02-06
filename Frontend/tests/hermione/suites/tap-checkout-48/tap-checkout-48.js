const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-48.json');

describe('tap-checkout-48: Оплата. Переход к экрану выбора способа оплаты с последующим возвратом назад', function() {
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

    it('Должна быть возможность вернуться на основной экран с экрана "Выбора способа оплаты"', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactPaymentMethodsButton(bro);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.paymentMethodsScreen, 5000, true);
        await bro.waitForVisible(checkoutPage.compactPaymentMethodsButton, 5000);

        await checkoutPage.clickCompactPaymentMethodsButton(bro);

        await bro.back();
        await bro.waitForVisible(checkoutPage.paymentMethodsScreen, 5000, true);
        await bro.waitForVisible(checkoutPage.compactPaymentMethodsButton, 5000);
    });
});
