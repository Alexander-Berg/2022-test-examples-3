const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-103.json');

describe('tap-checkout-103: Доставка. Нажатие на кнопку "Укажите адрес" с последующим указанием города', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
    });

    hermione.only.in('chrome-ignore-permission-geolocation');
    hermione.also.in('chrome-ignore-permission-geolocation');
    it('Должен произойти автопроскролл к блоку "Доставка" по нажатию на кнопку "Укажите город"', async function() {
        const bro = this.browser;
        const testData = this.defaultData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await bro.scrollTo(checkoutPage.orderSummaryButton);
        await bro.assertVisible(checkoutPage.orderSummaryButtonDisable);

        const textOrderSummaryButtonDisable = await bro.getText(checkoutPage.orderSummaryButton);
        assert.strictEqual(textOrderSummaryButtonDisable, 'Укажите город', `В кнопке оформления заказа должен отображаться текст "Укажите город", а отображается "${textOrderSummaryButtonDisable}"`);

        await bro.click(checkoutPage.orderSummaryButton);
        await bro.assertView('scrollToDelivery', checkoutPage.root);

        await checkoutPage.selectCityFromDefaultList(bro, 'Москва');
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.assertView('deliveryCityAfterSelectCity', checkoutPage.root);
        await bro.assertHidden(checkoutPage.orderSummaryButtonDisable);

        const textOrderSummaryButton = await bro.getText(checkoutPage.orderSummaryButton);
        assert.strictEqual(textOrderSummaryButton, 'Оформить заказ', `В кнопке оформления заказа должен отображаться текст "Оформить заказ", а отображается "${textOrderSummaryButton}"`);
    });
});
