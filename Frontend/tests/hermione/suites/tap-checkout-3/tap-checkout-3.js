const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-3.json');

describe('tap-checkout-3: Список товаров. Переход на экран "Ваш заказ" с возвратом назад', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна быть возможность вернуться на основной экран с экрана "Ваш заказ"', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);
        await bro.click(checkoutPage.headerBackButton);
        await bro.waitForVisible(checkoutPage.headerBackButton, 5000, true);
        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);
        await bro.click(checkoutPage.screenButton);
        await bro.waitForVisible(checkoutPage.screenButton, 5000, true);
        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);
        await bro.back();
        await bro.waitForVisible(checkoutPage.productList, 5000, true);

        await bro.waitForVisible(checkoutPage.productAllButton, 5000);
    });
});
