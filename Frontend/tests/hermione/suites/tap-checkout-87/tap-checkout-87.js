const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-87.json');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tap-checkout-87: Доставка. Отображение данных нулевого саджеста после смены учетной записи', function() {
    it('Должен отображаться нулевой саджест, который соответствует пользователю', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-86');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitForVisible(checkoutPage.deliveryAddressForm);

        await checkoutPage.searchAndClickInput(bro, 'address');
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestTitle, 10000);
        await bro.assertViewAfterLockFocusAndHover('deliveryAddressSuggest-user-86', checkoutPage.root);

        await bro.logout();
        await bro.auth('tap-checkout-87');

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.deliveryAddressForm);

        await checkoutPage.searchAndClickInput(bro, 'address');
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestTitle, 10000);
        await bro.assertViewAfterLockFocusAndHover('deliveryAddressSuggest-user-87', checkoutPage.root);
    });
});
