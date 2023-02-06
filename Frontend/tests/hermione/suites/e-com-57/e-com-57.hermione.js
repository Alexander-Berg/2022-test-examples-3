const assert = require('assert');

const indexPage = require('../../page-objects/index');
const productPage = require('../../page-objects/product');

describe('e-com-57: Главная. Переход на страницу товара из блока "Рекомендуем" с возвратом назад', function() {
    it('Должна открыться страница рекомендованного товара и произойти возврат на главную страницу по нажатию на кнопку назад', async function() {
        const bro = this.browser;
        await openProductPage(bro);

        const productPageSelector = await productPage.searchProductPage(bro, 0);
        await productPage.clickBackButton(bro, productPageSelector.screenHeader);
        await bro.waitForVisible(indexPage.mainPage, 5000);
    });

    it('Должна открыться страница рекомендованного товара и произойти возврат на главную страницу по нажатию на кнопку back браузера', async function() {
        const bro = this.browser;
        await openProductPage(bro);

        await bro.back();
        await bro.waitForVisible(indexPage.mainPage, 5000);
    });

    async function openProductPage(bro) {
        await indexPage.openAndCheckPage(bro);
        await bro.waitForVisible(indexPage.layoutItem(4), 5000);

        await bro.scroll(indexPage.layoutItem(4));
        await indexPage.searchAndWaitProductCardSkeleton(bro, indexPage.layoutItem(4), 0);

        let productRecommendedCart = await indexPage.searchProductCard(bro, indexPage.layoutItem(4), 0);
        let urlProductRecommended = await bro.getAttribute(productRecommendedCart.url, 'href');
        await bro.click(productRecommendedCart.selector);
        await bro.waitForVisible(productPage.productScreen, 10000);

        let urlProductRecommendedCurrent = await bro.getCurrentUrl();
        assert.strictEqual(urlProductRecommendedCurrent, urlProductRecommended, `Должен отображаться урл рекомендованного товара "${urlProductRecommended}", а отображается "${urlProductRecommendedCurrent}"`);
    }
});
