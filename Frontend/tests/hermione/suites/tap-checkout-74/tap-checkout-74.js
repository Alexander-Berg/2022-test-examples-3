const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-74.json');

describe('tap-checkout-74: Общее. Отображение разной валюты в блоке с детализацией заказа', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна отображаться соответствующая валюта в блоке с детализацией заказа', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.assertView('orderSummaryCurrencyInfo', checkoutPage.root);
    });
});
