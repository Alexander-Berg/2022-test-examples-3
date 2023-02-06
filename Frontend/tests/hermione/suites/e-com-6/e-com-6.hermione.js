const assert = require('assert');

const indexPage = require('../../page-objects/index');
const productPage = require('../../page-objects/product');

describe('e-com-6: Главная. Переход на страницу товара с возвратом назад', function() {
    it('Должна открыться страница товара и произойти возврат на главную страницу по нажатию на кнопку назад', async function() {
        const bro = this.browser;
        await openProductPage(bro);

        const productPageSelector = await productPage.searchProductPage(bro, 0);
        await productPage.clickBackButton(bro, productPageSelector.screenHeader);
        await bro.waitForVisible(indexPage.mainPage, 5000);
    });

    it('Должна открыться страница товара и произойти возврат на главную страницу по нажатию на кнопку back браузера', async function() {
        const bro = this.browser;

        await openProductPage(bro);
        await bro.back();
        await bro.waitForVisible(indexPage.mainPage, 5000);
    });

    async function openProductPage(bro) {
        await indexPage.openAndCheckPage(bro);
        await indexPage.searchAndWaitProductCardSkeleton(bro, indexPage.layoutItem(4), 0);

        let productRecommendedCart = await indexPage.searchProductCard(bro, indexPage.layoutItem(1), 0);
        let productTitle = await bro.getText(productRecommendedCart.title);

        await bro.click(indexPage.productCarouselFirstCard);
        await bro.waitForVisible(productPage.productScreen, 10000);
        await bro.waitForVisible(productPage.productInfoTitle);

        let currentProductTitle = await bro.getText(productPage.productInfoTitle);
        assert.strictEqual(currentProductTitle, productTitle, `Должно отображаться название товара "${productTitle}", а отображается "${currentProductTitle}"`);
    }
});
