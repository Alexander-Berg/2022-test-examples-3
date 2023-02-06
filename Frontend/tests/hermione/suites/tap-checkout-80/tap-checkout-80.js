const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-80.json');

describe('tap-checkout-80: Список товаров. Отсутствие кнопки "Все" при условии, что карточки товара помещаются на экран', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна отсутствовать кнопка "Все" при отображении двух карточек товара', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.orders[0].cartItems.pop();

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('products', checkoutPage.root);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна отсутствовать кнопка "Все" при отображении трех карточек товара', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('products', checkoutPage.root);
    });
});
