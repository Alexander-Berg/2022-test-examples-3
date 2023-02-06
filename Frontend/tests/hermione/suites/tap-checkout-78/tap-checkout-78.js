const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-78.json');

describe('tap-checkout-78: Список товаров. Отображение количества товаров в карточках товара на экране "Ваш заказ"', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должно отображаться количество каждого товара в списке', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);

        await bro.assertView('productList', checkoutPage.root);
    });
});
