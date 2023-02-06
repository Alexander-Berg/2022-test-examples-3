const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-82.json');
const errorTestData = require('./tap-checkout-error-82.json');

describe('tap-checkout-82: Доставка. Обработка ошибки в городе в ответе на событие cityChange', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
        this.errorData = cloneDeep(errorTestData);
    });

    it('Должна отобразиться ошибка при условии, что на событие "cityChange" отправлена ошибка в городе и после исправления ошибки оплата должна пройти успешно', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        const errorData = this.errorData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.deliveryCityButton, 5000);

        await checkoutPage.selectCityFromDefaultList(bro, 'Москва');
        await bro.handleCheckoutEvent('cityChange', errorData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.assertView('deliveryCityError', checkoutPage.root);
        await bro.assertVisible(checkoutPage.orderSummaryButtonDisable);

        await checkoutPage.selectCityFromDefaultList(bro, 'Екатеринбург');
        await bro.handleCheckoutEvent('cityChange', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.assertView('deliveryCityAfterFixError', checkoutPage.root);
        await bro.assertHidden(checkoutPage.orderSummaryButtonDisable);

        await bro.submitOrderAndCheckResult();
    });
});
