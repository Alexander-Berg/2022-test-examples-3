const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-17.json');

describe('tap-checkout-17: Получатель. Изменение данных на экране редактирования с их последующим сохранением', function() {
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

    it('Должны сохранится внесенные изменения в данные получателя', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactContactsButton(bro);

        await checkoutPage.searchAndClearFillInput(bro, 'name', 'Edit name');
        await checkoutPage.searchAndClearFillInput(bro, 'phone', '89456456454');
        await checkoutPage.searchAndClearFillInput(bro, 'email', 'edited-email@example.com');
        await bro.assertViewAfterLockFocusAndHover('contactsFormAfterEdit', checkoutPage.root);

        await bro.click(checkoutPage.screenButton);
        await bro.waitForVisible(checkoutPage.compactContactsButton, 5000);
        await bro.waitForVisible(checkoutPage.contactsScreen, 1000, true);

        await bro.assertView('compactContactsAfterEdit', checkoutPage.root);

        await bro.click(checkoutPage.mainScreenHeaderButtonClose);
        await bro.waitForVisible(checkoutPage.mainScreen, 5000, true);

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);
        await bro.assertView('compactContactsAfterOpen', checkoutPage.root);
    });
});
