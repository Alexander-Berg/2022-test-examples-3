const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-13.json');

describe('tap-checkout-13: Получатель. Отображение элементов схлопнутого блока и экрана редактирования при повторном оформлении заказа при разной комбинации полей', function() {
    beforeEach(async function() {
        this.defaultData = cloneDeep(defaultTestData);
        await precondition(this);
    });

    it('При запросе полных данных пользвователя данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        await openCheckoutAndOpenContactsScreen(this);
    });

    it('При запросе "name" и "phone" данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        const testData = this.defaultData;
        delete testData.payerDetails[2].type;

        await openCheckoutAndOpenContactsScreen(this);
    });

    it('При запросе "name" и "email" данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        const testData = this.defaultData;
        delete testData.payerDetails[1].type;

        await openCheckoutAndOpenContactsScreen(this);
    });

    it('При запросе "phone" и "email" данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        const testData = this.defaultData;
        delete testData.payerDetails[0].type;

        await openCheckoutAndOpenContactsScreen(this);
    });

    it('При запросе "name" данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        const testData = this.defaultData;
        delete testData.payerDetails[1].type;
        delete testData.payerDetails[2].type;

        await openCheckoutAndOpenContactsScreen(this);
    });

    it('При запросе "phone" данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        const testData = this.defaultData;
        delete testData.payerDetails[0].type;
        delete testData.payerDetails[2].type;

        await openCheckoutAndOpenContactsScreen(this);
    });

    it('При запросе "email" данные в схлопнутом блоке и на экране редактирования должны совпадать', async function() {
        const testData = this.defaultData;
        delete testData.payerDetails[0].type;
        delete testData.payerDetails[1].type;

        await openCheckoutAndOpenContactsScreen(this);
    });

    async function precondition(ctx) {
        const bro = ctx.browser;
        const testData = ctx.defaultData;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);
        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await checkoutPage.searchAndFillInput(bro, 'email', 'tap-checkout-54@example.com');
        await bro.submitOrderAndCheckResult();
    }

    async function openCheckoutAndOpenContactsScreen(ctx) {
        const bro = ctx.browser;
        const testData = ctx.defaultData;

        await checkoutPage.open(bro, testData);
        await bro.handleCheckoutEvent('restoreState', {});
        await bro.waitForVisible(checkoutPage.pageLoader, 5000, true);

        await bro.waitIconsLoad();
        await bro.assertView('compactContacts', checkoutPage.root);

        await checkoutPage.clickCompactContactsButton(bro);
        await bro.assertView('contactsScreen', checkoutPage.root);
    }
});
