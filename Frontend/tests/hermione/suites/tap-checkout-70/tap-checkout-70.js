const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-70.json');

describe('tap-checkout-70: Список товаров. Отображение разной валюты в карточках карусели товаров', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должен отображаться соответствующая валюта в карточках товаров', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('productCarouselCurrencyPartOne', checkoutPage.root);

        await bro.swipeLeft(checkoutPage.productsCarousel, 300);
        await bro.assertView('productCarouselCurrencyPartTwo', checkoutPage.root);

        await bro.swipeLeft(checkoutPage.productsCarousel, 550);
        await bro.assertView('productCarouselCurrencyPartThree', checkoutPage.root);
    });
});
