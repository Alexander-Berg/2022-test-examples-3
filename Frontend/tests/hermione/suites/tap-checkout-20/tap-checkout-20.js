const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-20.json');

describe('tap-checkout-20: Получатель. Отображение ошибки при не заполнении обязательных полей с последующим их заполнением', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна отсутствовать возможность оформления заказа без заполнения обязательных полей', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.click(checkoutPage.orderSummaryButton);
        await bro.waitForVisible(checkoutPage.contactsBlockFormError, 5000);

        await bro.assertView('contactsFormError', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await bro.assertViewAfterLockFocusAndHover('contactsFormErrorAfterFillUserName', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await bro.assertViewAfterLockFocusAndHover('contactsFormErrorAfterFillPhone', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'email', 'email@example.com');
        await bro.assertViewAfterLockFocusAndHover('contactsFormErrorAfterFillEmail', checkoutPage.root);

        await bro.submitOrderAndCheckResult();
    });
});
