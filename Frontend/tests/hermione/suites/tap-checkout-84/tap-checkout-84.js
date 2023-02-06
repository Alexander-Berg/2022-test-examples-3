const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-84.json');
const errorTestData = require('./tap-checkout-error-84.json');

describe('tap-checkout-84: Доставка. Обработка ошибки в адресе доставки в момент оплаты заказа', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
        this.errorData = cloneDeep(errorTestData);
    });

    it('Должна отобразиться ошибка при условии, что на событие "paymentStart" отправлена ошибка в городе и после исправления ошибки оплата должна пройти успешно', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        const errorData = this.errorData;

        await checkoutPage.open(bro, testData);
        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.selectCityFromDefaultList(bro, 'Москва');

        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertVisible(checkoutPage.orderSummaryButton);

        await checkoutPage.clickOrderSummaryButton(bro);
        await bro.handleCheckoutEvent('paymentStart', errorData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.assertVisible(checkoutPage.orderSummaryButtonDisable);
        await bro.assertVisible(checkoutPage.deliveryCityError);

        await bro.assertView('deliveryCityError', checkoutPage.root);

        await checkoutPage.selectCityFromDefaultList(bro, 'Екатеринбург');
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.assertHidden(checkoutPage.deliveryCityError);
        await bro.assertHidden(checkoutPage.orderSummaryButtonDisable);

        await bro.submitOrderAndCheckResult();
    });
});
