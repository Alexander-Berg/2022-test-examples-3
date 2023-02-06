const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-45.json');

describe('tap-checkout-45: Выбор даты и времени. Выбор даты и времени с последующим отображением данных на основном экране', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна быть возможность изменить дату и время доставки', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.openDeliveryTimeScreen(bro);
        await bro.assertView('deliveryTimeScreenDefault', checkoutPage.root);

        await checkoutPage.searchAndClickDeliveryDay(bro, '17');
        await bro.assertView('deliveryTimeScreenAfterChangeDay', checkoutPage.root);

        await bro.click(checkoutPage.screenTimeOptionsFirstRadiobox);
        await bro.waitForVisible(checkoutPage.screenTimeOptionsCheckedRadiobox, 5000);
        await bro.assertView('deliveryTimeScreenAfterSelectTime', checkoutPage.root);

        await bro.click(checkoutPage.screenButton);
        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.screenDateOptionsDate, 5000, true);

        const currentDeliveryTimeDateInfo = await bro.getText(checkoutPage.deliveryTimeDateInfo);
        const expectedDeliveryTimeDateInfo = 'Суббота, 17 октября, 9.00 - 12.00';
        assert.strictEqual(currentDeliveryTimeDateInfo, expectedDeliveryTimeDateInfo, `На главном экране должна отображаться дата и времня доставки "${expectedDeliveryTimeDateInfo}", а отображается "${currentDeliveryTimeDateInfo}"`);
    });
});
