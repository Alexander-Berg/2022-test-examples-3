const assert = require('assert');

const productPage = require('../../page-objects/product');

describe('e-com-13: Товар. Переход на страницу товара из блока "Другие цвета"', function() {
    it('Должна открыться страница товара и произойти возврат по нажатию на кнопку back браузера', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, 'LIVdi3O6Kd1RgUrGLnu0z');
        await bro.waitForVisible(productPage.productScreen, 5000);

        const urlProduct = await bro.getCurrentUrl();
        await checkOpenOtherColorsProduct(bro);

        await bro.back();
        await productPage.checkBackProductPage(bro, urlProduct);
    });

    it('Должна открыться страница товара и произойти возврат по нажатию на кнопку назад', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, 'LIVdi3O6Kd1RgUrGLnu0z');
        await bro.waitForVisible(productPage.productScreen, 5000);

        const urlProduct = await bro.getCurrentUrl();
        await checkOpenOtherColorsProduct(bro);

        const productPageSelector = await productPage.searchProductPage(bro, 1);
        await productPage.clickBackButton(bro, productPageSelector.screenHeader);
        await bro.waitForVisible(productPageSelector.screenHeader, 5000, true);

        await productPage.checkBackProductPage(bro, urlProduct);
    });

    async function checkOpenOtherColorsProduct(bro) {
        await bro.scroll(productPage.productCarouselFirstCard);

        const carousel = await productPage.searchProductCarousel(bro, 0);
        await productPage.searchAndWaitProductCardSkeleton(bro, carousel.selector, 0);

        const productCarouselCard = await productPage.searchProductCarouselCard(bro, 0, 0);
        const urlProductCarouselCard = await bro.getAttribute(productCarouselCard.cardUrl, 'href');
        await bro.click(productCarouselCard.card);
        await bro.waitForVisible(productCarouselCard.card, 5000, true);

        const urlProductCarouselCardCurrent = await bro.getCurrentUrl();
        assert.strictEqual(urlProductCarouselCardCurrent, urlProductCarouselCard, `Должен отображаться урл товара "${urlProductCarouselCard}", а отображается "${urlProductCarouselCardCurrent}"`);
    }
});
