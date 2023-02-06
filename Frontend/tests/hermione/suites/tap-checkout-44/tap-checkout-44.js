const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-44.json');

describe('tap-checkout-44: Выбор даты и времени. Свайп списка дат', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должен работать свайп списка дат', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.openDeliveryTimeScreen(bro);
        await bro.assertView('deliveryTimeScreenDefault', checkoutPage.root);

        await bro.swipeLeft(checkoutPage.screenDateOptionsDateContent);
        await bro.assertView('deliveryTimeScreenSwipeLeft', checkoutPage.root);

        await bro.swipeRight(checkoutPage.screenDateOptionsDateContent);
        await bro.assertView('deliveryTimeScreenSwipeRight', checkoutPage.root);
    });
});
