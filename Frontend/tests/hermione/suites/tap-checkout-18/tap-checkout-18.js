const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-18.json');
const errorTestData = require('./tap-checkout-error-18.json');

describe('tap-checkout-18: Получатель. Обработка ошибки в момент оплаты заказа', function() {
    beforeEach(function() {
        this.defaultData = cloneDeep(defaultTestData);
        this.errorData = cloneDeep(errorTestData);
    });

    it('Должна прерваться оплата при условии, что на событие "paymentStart" отправлена ошибка заполнения контактов и после исправления ошибок оплата должна проийти успешно', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        const errorData = this.errorData;

        await checkoutPage.open(bro, testData);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await checkoutPage.searchAndFillInput(bro, 'email', 'email@example.com');
        await checkoutPage.clickOrderSummaryButton(bro);

        await bro.handleCheckoutEvent('paymentStart', errorData);
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('contactsFormError', checkoutPage.root);
        await bro.assertVisible(checkoutPage.orderSummaryButtonDisable);

        await checkoutPage.searchAndClearFillInput(bro, 'name', 'Edit name');
        await checkoutPage.searchAndClearFillInput(bro, 'phone', '89456456454');
        await checkoutPage.searchAndClearFillInput(bro, 'email', 'edited-email@example.com');
        await bro.assertViewAfterLockFocusAndHover('contactsFormAfterEdit', checkoutPage.root);

        await bro.submitOrderAndCheckResult();
    });
});
