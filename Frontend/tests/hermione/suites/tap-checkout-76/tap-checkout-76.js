const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-76.json');

describe('tap-checkout-76: Список товаров. Отображение количества товара на экране с одной карточкой товара', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должно отображаться более одной позиции товара с коротким названием', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('productShortTitle', checkoutPage.root);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должно отображаться более одной позиции товара с длинным названием', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        testData.orders[0].cartItems[0].title = 'Тестовый товар с очень длинным названием тестового товара';

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('productLongTitle', checkoutPage.root);
    });
});
