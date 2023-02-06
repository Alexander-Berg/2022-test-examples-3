const checkoutPage = require('../../page-objects/checkout');
const cloneDeep = require('../../cloneDeep');

const defaultTestData = require('./tap-checkout-1.json');

describe('tap-checkout-1: Список товаров. Свайп карусели товаров', function() {
    beforeEach(function() {
        this.currentTest.testData = cloneDeep(defaultTestData);
    });

    it('Должен работать свайп карусели товаров', async function() {
        const bro = this.browser;
        const testData = this.currentTest.testData;

        await checkoutPage.open(bro, testData);

        await bro.waitProductBlock();
        await bro.assertView('productCarouselDefault', checkoutPage.root);

        await bro.swipeLeft(checkoutPage.productsCarousel);
        await bro.assertView('productCarouselSwipeLeft', checkoutPage.root);

        await bro.swipeRight(checkoutPage.productsCarousel);
        await bro.assertView('productCarouselSwipeRight', checkoutPage.root);
    });
});
