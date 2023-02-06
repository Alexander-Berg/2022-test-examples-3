const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-77.json');

describe('tap-checkout-77: Список товаров. Отображение количества товара на экране с каруселью товаров', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('В карточках товара должно отображаться количество товара', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('productCarousel', checkoutPage.root);
    });
});
