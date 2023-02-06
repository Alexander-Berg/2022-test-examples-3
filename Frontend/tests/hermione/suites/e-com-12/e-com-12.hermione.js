const productPage = require('../../page-objects/product');

describe('e-com-12:  Товар. Отображение шильдика скидки и стоимость товара со скидкой', function() {
    it('На странице с галереей шильдик скидки и цена со скидкой должны корректно отображаться', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '3rb5dGKvPJgjfWARNrMtxX');
        await bro.waitForVisible(productPage.galleryImageLoaded, 5000);
        await bro.assertView('e-com-12-productHeader', productPage.productHeader);
    });

    it('На странице без галереи шильдик скидки и цена со скидкой должны корректно отображаться', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '3R3a3CDzcOwWpnf79FpVZu');
        await bro.waitForVisible(productPage.productProductNoImage, 5000);
        await bro.assertView('e-com-12-productHeaderNoImage', productPage.productHeader);
    });

    it('На странице с длинным названием шильдик скидки и цена со скидкой должны корректно отображаться', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '34khLEPiu58ISBFE29Ctco');
        await bro.waitForVisible(productPage.galleryImageLoaded, 5000);
        await bro.assertView('e-com-12-productHeader', productPage.productHeader);
    });
});
