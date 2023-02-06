const assert = require('assert');

const checkoutPage = require('../../page-objects/checkout');

const defaultTestData = require('./tap-checkout-21.json');

describe('tap-checkout-21: Получатель. Отображение ошибки валидации под полем "Почта" на экране редактирования', function() {
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

    it('Под полем "Почта" должна отображаться ошибка валидации', async function() {
        const bro = this.browser;

        await checkoutPage.open(bro, defaultTestData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await checkoutPage.clickCompactContactsButton(bro);
        await checkoutPage.searchAndClearFillInput(bro, 'email', 'edit-tap-checkout-54@');
        await bro.click(checkoutPage.screenButton);
        await bro.waitForVisible(checkoutPage.contactsScreenFormError, 5000);
        await bro.assertViewAfterLockFocusAndHover('contactsScreenValidationError', checkoutPage.root);

        const newEmail = 'edit-tap-checkout-54@example.com';
        await checkoutPage.searchAndClearFillInput(bro, 'email', newEmail);
        await bro.assertViewAfterLockFocusAndHover('contactsScreenAfterFixValidationError', checkoutPage.root);

        await bro.click(checkoutPage.screenButton);
        await bro.waitForVisible(checkoutPage.contactsScreen, 5000, true);
        await bro.waitForVisible(checkoutPage.compactContactsButton, 5000);

        const currentInfoContacts = await bro.getText(checkoutPage.compactContactsSubtitle);
        assert.strictEqual(currentInfoContacts, `8 912 312-31-21, ${newEmail}`, `В схлопнутом блоке "Пользователь" должны отображаться данные "8 912 312-31-21, ${newEmail}", а отображаются "${currentInfoContacts}"`);
    });
});
