const productPage = require('../../page-objects/product');

describe('e-com-9: Товар. Просмотр длинного описания товара', function() {
    it('По нажатию на кнопку "Подробнее" должно отображаться полное описание товара', async function() {
        const bro = this.browser;

        await bro.openPage(productPage, '1bylZU1F9OqBisp9qgeBEn');
        await bro.waitForVisible(productPage.descriptionExpand, 5000);
        await bro.assertView('e-com-9-productDescription', productPage.description);

        await bro.click(productPage.descriptionExpand);
        await bro.waitForVisible(productPage.descriptionCollapsed, 5000, true);
        await bro.assertHidden(productPage.descriptionExpand);

        await bro.scroll(productPage.description, 0, -100);
        await bro.assertView('e-com-9-productDescriptionCollapsed', productPage.description);
    });
});
