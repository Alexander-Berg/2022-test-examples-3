const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-6.json');

describe('tap-checkout-6: Получатель. Отображение элементов в блоке для неавторизованного пользователя', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('В блоке "Пользователь" должны отображаться соответствующие элементы', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('contacts-block', checkoutPage.root);
    });
});
