const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-4.json');

describe('tap-checkout-4: Список товаров. Свайп списка товаров на экране "Ваш заказ"', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    hermione.also.in(['chrome-grid-320', 'chrome-grid-414']);
    it('Должна быть возможность проскролла списка товаров', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);
        await bro.assertView('tap-checkout-4-productListDefault', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 1000);
        await bro.assertView('tap-checkout-4-productListScrollUp', checkoutPage.root);

        await bro.scrollTop(checkoutPage.root, 0);
        await bro.assertView('tap-checkout-4-productListScrollDown', checkoutPage.root);
    });
});
