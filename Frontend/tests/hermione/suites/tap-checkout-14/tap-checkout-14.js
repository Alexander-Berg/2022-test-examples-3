const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-14.json');

describe('tap-checkout-14: Получатель. Переход на экран редактирования с возвратом назад', function() {
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

    it('Должен работать переход к экрану "Получатель"', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactContactsButton(bro);

        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.compactContactsButton, 5000);
        await bro.waitForVisible(checkoutPage.contactsScreen, 1000, true);

        await checkoutPage.clickCompactContactsButton(bro);

        await bro.click(checkoutPage.screenButton);
        await bro.waitForVisible(checkoutPage.compactContactsButton, 5000);
        await bro.waitForVisible(checkoutPage.contactsScreen, 1000, true);

        await checkoutPage.clickCompactContactsButton(bro);

        await bro.back();
        await bro.waitForVisible(checkoutPage.compactContactsButton, 5000);
        await bro.waitForVisible(checkoutPage.contactsScreen, 1000, true);
    });
});
