const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-16.json');

describe('tap-checkout-16: Получатель. Удаление данных на экране редактирования с последующим возвратом назад', function() {
    beforeEach(async function() {
        await precondition(this.browser);
    });

    async function precondition(bro) {
        await bro.auth('tap-checkout-54');

        await checkoutPage.open(bro, defaultTestData);
        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await checkoutPage.searchAndFillInput(bro, 'email', 'tap-checkout-54@example.com');
        await bro.submitOrderAndCheckResult();
    }

    it('При удалении данных текст в кнопке оформления должен меняться', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactContactsButton(bro);

        await checkoutPage.searchAndClearInput(bro, 'email', checkoutPage.contactsScreenInputButtonClear);
        await bro.assertViewAfterLockFocusAndHover('contactsScreenClearEmailInput', checkoutPage.root);

        await checkoutPage.searchAndClearInput(bro, 'phone', checkoutPage.contactsScreenInputButtonClear);
        await bro.assertViewAfterLockFocusAndHover('contactsScreenClearPhoneInput', checkoutPage.root);

        await checkoutPage.searchAndClearInput(bro, 'name', checkoutPage.contactsScreenInputButtonClear);
        await bro.assertViewAfterLockFocusAndHover('contactsScreenClearNameInput', checkoutPage.root);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.compactContactsButton, 5000);
        await bro.waitForVisible(checkoutPage.contactsScreen, 1000, true);
    });
});
