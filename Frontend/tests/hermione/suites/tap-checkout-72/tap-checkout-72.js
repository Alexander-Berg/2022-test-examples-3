const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-72.json');

describe('tap-checkout-72: Доставка. Отображение разной валюты в способах доставки', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна отображаться соответствующая валюта в способах доставки', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.handleCheckoutEvent('shippingOptionChange', {});

        await bro.waitForVisible(checkoutPage.deliveryPrimaryOrder, 5000);
        await bro.assertView('deliveryPrimaryOrderCurrencyPartOne', checkoutPage.root);

        await bro.scrollTo(checkoutPage.orderSummaryButton);
        await bro.hideElement(checkoutPage.mainScreenStickyHeader);
        await bro.assertView('deliveryPrimaryOrderCurrencyPartTwo', checkoutPage.root);
    });
});
