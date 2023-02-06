const productPage = require('../../page-objects/product');

describe('e-com-8:  Товар. Свайп галереи изображений', function() {
    it('Должны свайпаться изображения в галереи товара', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '3xHytCzWthwEpQU0PDrBlf');
        await bro.waitForVisible(productPage.galleryImageLoaded, 5000);
        await bro.assertView('e-com-8-headerProductPageFirstPointActive', productPage.gallery, { ignoreElements: productPage.galleryImageLoaded });

        await bro.swipeRight(productPage.gallerySlider);
        await bro.waitForVisible(productPage.galleryPointActive(1), 2000);

        await bro.swipeLeft(productPage.gallerySlider);
        await bro.waitForVisible(productPage.galleryPointActive(2), 2000);

        const galleryPoints = await bro.elements(productPage.galleryPoint);
        for (let i = 3; i <= galleryPoints.value.length; i++) {
            await bro.swipeLeft(productPage.gallerySlider);
            await bro.waitForVisible(productPage.galleryPointActive(i), 2000);
        }

        await bro.assertView('e-com-8-headerProductPageLastPointActive', productPage.gallery, { ignoreElements: productPage.galleryItem });

        await bro.swipeLeft(productPage.gallerySlider);
        await bro.waitForVisible(productPage.galleryPointActive(galleryPoints.value.length));
    });
});
