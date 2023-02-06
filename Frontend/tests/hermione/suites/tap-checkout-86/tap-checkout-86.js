const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-86.json');

// eslint-disable-next-line mocha/no-skipped-tests
describe.skip('tap-checkout-86: Доставка. Отображение данных нулевого саджеста при установки фокуса в поле ввода "Адрес"', function() {
    it('Должен быть возможность выбора адреса из нулевого саджеста', async function() {
        const bro = this.browser;
        await bro.auth('tap-checkout-86');

        await checkoutPage.open(bro, defaultTestData);
        await bro.waitForVisible(checkoutPage.deliveryAddressForm, 5000);

        await checkoutPage.searchAndClickInput(bro, 'address');
        await bro.waitForVisible(checkoutPage.deliveryAddressSuggestTitle, 10000);
        await bro.assertView('deliveryAddressSuggest', checkoutPage.root);

        await checkoutPage.clickAddressInSearchResult(bro, 'улица Хохрякова, 10');
        await bro.waitForVisible(checkoutPage.pageLoader, 5000);

        await bro.handleCheckoutEvent('shippingAddressChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('deliveryAddressAfterSelect', checkoutPage.root);
    });
});
