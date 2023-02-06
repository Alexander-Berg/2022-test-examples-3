const productPage = require('../../page-objects/product');

describe('e-com-14: Товар. Свайп карусели карточек товара в блоке "Другие цвета"', function() {
    it('Должен работать свайп карусели блока "Другие цвета"', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, 'LIVdi3O6Kd1RgUrGLnu0z');
        await bro.waitForVisible(productPage.productCarousel, 15000);

        await bro.scroll(productPage.productCarousel);

        const productCarouselCardFirst = await productPage.searchProductCarouselCard(bro, 0, 0);
        const productCarouselCardTwo = await productPage.searchProductCarouselCard(bro, 0, 1);
        const productCarouselCardThree = await productPage.searchProductCarouselCard(bro, 0, 2);

        await bro.hideElement(productPage.productHeaderFixed);
        await bro.hideElement(productCarouselCardFirst.image);
        await bro.hideElement(productCarouselCardTwo.image);
        await bro.hideElement(productCarouselCardThree.image);
        await bro.assertView('e-com-14-productCarouselDefault', productPage.productCarousel);

        await bro.swipeLeft(productPage.productCarousel);
        await bro.assertView('e-com-14-productCarouselSwipeLeft', productPage.productCarousel);

        await bro.swipeRight(productPage.productCarousel);
        await bro.assertView('e-com-14-productCarouselSwipeRight', productPage.productCarousel);
    });
});
