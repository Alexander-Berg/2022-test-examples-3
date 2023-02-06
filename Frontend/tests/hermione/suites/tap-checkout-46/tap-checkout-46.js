const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-46.json');

describe('tap-checkout-46: Выбор даты и времени. Изменение даты доставки без сохранения изменений', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Изменения не должны сохранится', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.handleCheckoutEvent('datetimeOptionChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        const expectedDeliveryTimeDateInfo = await bro.getText(checkoutPage.deliveryTimeDateInfo);

        await checkoutPage.openDeliveryTimeScreen(bro);
        await checkoutPage.searchAndClickDeliveryDay(bro, '17');
        await bro.click(checkoutPage.screenTimeOptionsFirstRadiobox);
        await bro.waitForVisible(checkoutPage.screenTimeOptionsCheckedRadiobox, 5000);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.screenDateOptionsDate, 5000, true);

        const currentDeliveryTimeDateInfo = await bro.getText(checkoutPage.deliveryTimeDateInfo);
        assert.strictEqual(currentDeliveryTimeDateInfo, expectedDeliveryTimeDateInfo, `На главном экране должна отображаться дата и времня доставки "${expectedDeliveryTimeDateInfo}", а отображается "${currentDeliveryTimeDateInfo}"`);
    });
});
