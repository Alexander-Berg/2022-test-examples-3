const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-107.json');

describe('tap-checkout-107: Получатель. Отображение разной комбинации полей при первом и повторном оформлении заказа', function() {
    beforeEach(async function() {
        this.defaultData = cloneDeep(defaultTestData);
    });

    it('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе всех данных', async function() {
        const bro = this.browser;
        const testData = this.defaultData;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await checkoutPage.searchAndFillInput(bro, 'email', 'tap-checkout-54@example.com');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    // eslint-disable-next-line mocha/no-skipped-tests
    it.skip('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе всех данных, кроме почты', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        delete testData.payerDetails[2].type;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    it('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе всех данных, кроме номера телефона', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        delete testData.payerDetails[1].type;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await checkoutPage.searchAndFillInput(bro, 'email', 'tap-checkout-54@example.com');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    it('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе всех данных, кроме имени и фамилии', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        delete testData.payerDetails[0].type;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await checkoutPage.searchAndFillInput(bro, 'email', 'tap-checkout-54@example.com');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    it('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе только имени и фамилии', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        delete testData.payerDetails[1].type;
        delete testData.payerDetails[2].type;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'name', 'Name and Surname');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    it('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе только номера телефона', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        delete testData.payerDetails[0].type;
        delete testData.payerDetails[2].type;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'phone', '89123123121');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    it('Данные должны совпадать в блоке "Получатель" и на экране "Получатель" при запросе только почты', async function() {
        const bro = this.browser;
        const testData = this.defaultData;
        delete testData.payerDetails[0].type;
        delete testData.payerDetails[1].type;

        await bro.auth('tap-checkout-54');
        await checkoutPage.open(bro, testData);

        await bro.waitIconsLoad();
        await bro.assertView('default', checkoutPage.root);

        await checkoutPage.searchAndFillInput(bro, 'email', 'tap-checkout-54@example.com');
        await bro.submitOrderAndCheckResult();
        await reopenPageAndClickCompactContactsButton(bro, testData);
    });

    async function reopenPageAndClickCompactContactsButton(bro, testData) {
        await checkoutPage.open(bro, testData);
        await bro.handleCheckoutEvent('restoreState', {});

        await bro.waitIconsLoad();
        await bro.assertView('compactContacts', checkoutPage.root);

        await bro.click(checkoutPage.compactContactsButton);
        await bro.waitForVisible(checkoutPage.contactsScreen, 5000);
        await bro.waitForVisible(checkoutPage.mainScreen, 1000, true);
        await bro.assertView('contactsScreen', checkoutPage.root);
    }
});
