const productPage = require('../../page-objects/product');

describe('e-com-10: Товар. Выбор размера товара', function() {
    it('Должен измениться цвет кнопки выбранного размера', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '1bylZU1F9OqBisp9qgeBEn');
        await bro.waitForVisible(productPage.sizesCarousel, 5000);
        await bro.scroll(productPage.sizesCarousel, 0, -100);

        await bro.assertView('e-com-10-sizesCarouselDefault', productPage.sizesCarousel);

        await productPage.selectAndCheckSizeLabelActive(bro, 0);
        await productPage.selectAndCheckSizeLabelActive(bro, 2);
        await productPage.selectAndCheckSizeLabelActive(bro, 4);
        await productPage.selectAndCheckSizeLabelActive(bro, 6);
        await bro.assertView('e-com-10-sizesLabelActive', productPage.sizesCarousel);

        await productPage.selectAndCheckSizeLabelDefault(bro, 0);
        await productPage.selectAndCheckSizeLabelDefault(bro, 2);
        await productPage.selectAndCheckSizeLabelDefault(bro, 4);
        await productPage.selectAndCheckSizeLabelDefault(bro, 6);
        await bro.assertView('e-com-10-sizesLabelAfterDoubleClick', productPage.sizesCarousel);
    });
});
