const productPage = require('../../page-objects/product');

const itemPosition = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];

describe('e-com-11: Товар. Выбор всех доступных размеров товара', function() {
    it('Должна быть возможность выбора всех доступных размеров товара', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '2XUQKaJ3tHEArF75fkrfN3');
        await bro.waitForVisible(productPage.sizesCarousel, 5000);
        await bro.scroll(productPage.sizesCarousel, 0, -100);

        for (let item of itemPosition) {
            await productPage.selectAndCheckSizeLabelActive(bro, item);
        }
        await bro.swipeLeft(productPage.sizesCarousel);
        await bro.assertView('e-com-11-sizesLabelActive', productPage.sizesCarousel);

        for (let item of itemPosition) {
            await productPage.selectAndCheckSizeLabelDefault(bro, item);
        }

        await bro.assertView('e-com-11-sizesLabelDefault', productPage.sizesCarousel);
    });
});
