const productPage = require('../../page-objects/product');

describe('e-com-21:  Товар. Свайп карусели размеров товара', function() {
    it('Должна свайпаться карусель размеров товара', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '2XUQKaJ3tHEArF75fkrfN3');
        await bro.waitForVisible(productPage.sizesCarousel, 5000);
        await bro.scroll(productPage.sizesCarousel, 0, -100);

        await bro.swipeRight(productPage.sizesCarousel);
        await bro.assertView('app-editor-21-sizesCarouselDefault', productPage.sizesCarousel);

        await bro.swipeLeft(productPage.sizesCarousel);
        await bro.assertView('app-editor-21-sizesCarouselSwipeLeft', productPage.sizesCarousel);

        await bro.swipeRight(productPage.sizesCarousel);
        await bro.assertView('app-editor-21-sizesCarouselSwipeRight', productPage.sizesCarousel);
    });
});
