const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-73.json');

describe('tap-checkout-73: Общее. Отображение разной валюты в итоговой стоимости заказа', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна отображаться итоговая стоимость в формате RUB', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате RUR', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'RUR';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате EUR', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'EUR';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате USD', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'USD';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате BYN', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'BYN';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате BYR', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'BYR';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате UAH', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'UAH';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате KZT', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'KZT';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });

    it('Должна отображаться итоговая стоимость в формате GBP', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.total.amount.currency = 'GBP';

        await checkoutPage.open(bro, testData);
        await bro.assertView(`orderSummary-${testData.total.amount.currency}`, checkoutPage.root);
    });
});
