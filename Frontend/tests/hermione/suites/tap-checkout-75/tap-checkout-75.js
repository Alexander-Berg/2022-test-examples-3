const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-75.json');

describe('tap-checkout-75: Выбор даты и времени. Отображение разной валюты на экране', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна отображаться соответствующая валюта на экране выбора даты и времени', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        // Ждем события выбора первого значения из списка возможных способов доставки
        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.openDeliveryTimeScreen(bro);

        await bro.click(checkoutPage.screenDateOptionsDate);
        await bro.waitForVisible(checkoutPage.screenDateOptionsDateSelected, 5000);
        await bro.waitForVisible(checkoutPage.screenTimeOptionsContainer, 5000);

        await bro.hideElement(checkoutPage.stickyScreenButton);
        await bro.assertView('screenTimeOptionsCurrency', checkoutPage.root);
    });
});
