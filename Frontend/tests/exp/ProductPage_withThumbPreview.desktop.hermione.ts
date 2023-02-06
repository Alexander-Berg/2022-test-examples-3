describe('ProductPage / Ракурсы', function() {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=PRODUCTS_desktop_preview_gallery=1');
        await bro.yaWaitForPageLoad();

        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');
        await bro.click('.ProductCardsList-Item:nth-child(1) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.Card-Thumbnail .Thumbnail-Image', 3000, 'Главное изображение товара не появилось');
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs', 3000, 'Блок с ракурсами не появился');
    });

    it('Внешний вид', async function() {
        const bro = this.browser;

        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(1)', 3000, 'Первая картинка в ракурсах не активна');
        await bro.assertView('first-active', '.Card-Thumbnail');
    });

    it('Переключение по наведению', async function() {
        const bro = this.browser;

        const items = await bro.$$('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item');
        assert.isTrue(items.length >= 3, 'Меньше 3х картинок в ракурсах');

        const bigThumb = await bro.$('.Card-Thumbnail .Thumbnail-Image');
        const firstSrc = await bigThumb.getAttribute('src');

        await bro.$('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item:nth-child(2)').moveTo({ xOffset: 5, yOffset: 5 });
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(2)', 3000, 'Вторая картинка в ракурсах не активна');
        const secondSrc = await bigThumb.getAttribute('src');
        assert.notEqual(firstSrc, secondSrc, 'Большая картинка не изменилась');

        await bro.assertView('hovered-second', '.Card-Thumbnail');

        await bro.$('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item:nth-child(3)').moveTo({ xOffset: 5, yOffset: 5 });
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(3)', 3000, 'Третья картинка в ракурсах не активна');
        const thirdSrc = await bigThumb.getAttribute('src');
        assert.notEqual(secondSrc, thirdSrc, 'Большая картинка не переключилась на третью');

        await bigThumb.moveTo({ xOffset: 5, yOffset: 5 });
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(1)', 3000, 'Первая картинка в ракурсах не активна');
        const lastSrc = await bigThumb.getAttribute('src');
        assert.strictEqual(lastSrc, firstSrc, 'Большая картинка не переключилась на первую');
    });

    it('Переключение по клику', async function() {
        const bro = this.browser;

        const items = await bro.$$('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item');
        assert.isTrue(items.length >= 3, 'Меньше 3х картинок в ракурсах');

        const bigThumb = await bro.$('.Card-Thumbnail .Thumbnail-Image');
        const firstSrc = await bigThumb.getAttribute('src');

        const thirdSmallThumb = await bro.$('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item:nth-child(3)');
        await thirdSmallThumb.moveTo({ xOffset: 5, yOffset: 5 });
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(3)', 3000, 'Третья картинка в ракурсах не активна');
        await thirdSmallThumb.click();
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_selected:nth-child(3)', 3000, 'Третья картинка в ракурсах не зафиксировалась');
        const thirdSrc = await bigThumb.getAttribute('src');
        assert.notEqual(firstSrc, thirdSrc, 'Большая картинка не переключилась на третью');

        await bro.$('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item:nth-child(2)').moveTo({ xOffset: 5, yOffset: 5 });
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(2)', 3000, 'Вторая картинка в ракурсах не активна');
        const secondSrc = await bigThumb.getAttribute('src');
        assert.notEqual(thirdSrc, secondSrc, 'Большая картинка не изменилась');

        await bigThumb.moveTo({ xOffset: 5, yOffset: 5 });
        await bro.yaWaitForVisible('.Card-Thumbnail .RadioThumbs .RadioThumbs-Item_active:nth-child(3)', 3000, 'Первая картинка в ракурсах не активна');
        const lastSrc = await bigThumb.getAttribute('src');
        assert.strictEqual(lastSrc, thirdSrc, 'Большая картинка не переключилась на кликнутую третью');
    });
});
