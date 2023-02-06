const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-71.json');

describe('tap-checkout-71: Список товаров. Отображение разной валюты в карточках товаров и в блоке с доп.инф о корзине', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должна отображаться соответствующая валюта на экране "Ваш заказ"', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);
        await bro.waitForVisible(checkoutPage.productAllButton, 5000);

        await checkoutPage.clickProductAllButton(bro);
        await bro.assertView('productListCurrencyPartOne', checkoutPage.root);

        await bro.scrollTo(checkoutPage.productSummary);
        await bro.hideElement(checkoutPage.stickyHeader);

        await bro.assertView('productListCurrencyPartTwo', checkoutPage.root);
    });
});
