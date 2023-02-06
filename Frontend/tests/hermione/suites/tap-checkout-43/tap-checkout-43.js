const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-43.json');

describe('tap-checkout-43: Доставка. Переход к экрану выбора даты и времени доставки с возвратом назад', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна быть возможность вернуться на основной экран с экрана "Даты и времени доставки"', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.openDeliveryTimeScreen(bro);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.screenDateOptionsDate, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryPrimaryOrderTime, 5000);

        await checkoutPage.openDeliveryTimeScreen(bro);
        await bro.back();

        await bro.waitForVisible(checkoutPage.screenDateOptionsDate, 5000, true);
        await bro.waitForVisible(checkoutPage.deliveryPrimaryOrderTime, 5000);
    });
});
