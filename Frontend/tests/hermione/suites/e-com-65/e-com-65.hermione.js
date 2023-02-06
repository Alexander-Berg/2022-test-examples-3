const assert = require('assert');

const productPage = require('../../page-objects/product');

describe('e-com-65: Товар. Переход на страницу товара из блока "Рекомендуем" с возвратом назад', function() {
    it('Должна открыться страница товара и произойти возврат по нажатию на кнопку back браузера', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '2OOLw5ur855BqGrZtoM3F0');
        await bro.waitForVisible(productPage.productRecommended, 5000);

        const urlProduct = await bro.getCurrentUrl();
        await checkOpenProductRecommendedPage(bro);

        await bro.back();
        await productPage.checkBackProductPage(bro, urlProduct);
    });

    it('Должна открыться страница товара и произойти возврат по нажатию на кнопку назад', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '2OOLw5ur855BqGrZtoM3F0');
        await bro.waitForVisible(productPage.productRecommended, 5000);

        const urlProduct = await bro.getCurrentUrl();
        await checkOpenProductRecommendedPage(bro);

        const productPageSelector = await productPage.searchProductPage(bro, 1);
        await productPage.clickBackButton(bro, productPageSelector.screenHeader);
        await bro.waitForVisible(productPageSelector.screenHeader, 5000, true);

        await productPage.checkBackProductPage(bro, urlProduct);
    });

    async function checkOpenProductRecommendedPage(bro) {
        await bro.scroll(productPage.productRecommended);
        await productPage.searchAndWaitProductCardSkeleton(bro, productPage.productRecommended, 0);

        const productRecommendedCart = await productPage.searchProductRecommendedCard(bro, 0);
        const urlProductRecommended = await bro.getAttribute(productRecommendedCart.url, 'href');
        await bro.click(productRecommendedCart.selector);
        await bro.waitForVisible(productRecommendedCart.selector, 5000, true);

        const urlProductRecommendedCurrent = await bro.getCurrentUrl();
        assert.strictEqual(urlProductRecommendedCurrent, urlProductRecommended, `Должен отображаться урл рекомендованного товара "${urlProductRecommended}", а отображается "${urlProductRecommendedCurrent}"`);
    }
});
