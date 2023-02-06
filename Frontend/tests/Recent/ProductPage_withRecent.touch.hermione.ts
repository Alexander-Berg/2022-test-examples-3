describe('ProductPage / Просмотренные товары в карточке', function() {
    it('Внешний вид', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone');
        await bro.yaWaitForSearchPage();

        const { length: lengthBefore } = await bro.$$('.ProductCardsList-Item');
        assert.isTrue(lengthBefore >= 2, 'На странице меньше 2 карточек');

        await bro.click('.ProductCardsList-Item:nth-child(1) .ProductCard');
        await bro.yaWaitForPageLoad();

        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');

        const closeButtonSelector = await bro.getMeta('platform') === 'desktop' ? '.ProductEntityModal-CloseButton' : '.Card-CloseButton';
        await bro.yaWaitForVisible(closeButtonSelector);
        await bro.click(closeButtonSelector);
        await bro.yaWaitForPageLoad();

        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');

        const { length: lengthAfter } = await bro.$$('.ProductCardsList-Item');
        assert.isTrue(lengthAfter >= 2);

        await bro.click('.ProductCardsList-Item:nth-child(2) .ProductCard');
        await bro.yaWaitForPageLoad();

        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.RecentItems', 'Просмотренные товары не появилась');
        await bro.$('.RecentItems').scrollIntoView();
        await bro.assertView('first', '.RecentItems');

        await bro.click('.RecentItems .ProductCardsList-Item:nth-child(1) .ProductCard');
        await bro.yaWaitForPageLoad();

        await bro.yaWaitForVisible('.RecentItems', 'Просмотренные товары не появилась');
        await bro.$('.RecentItems').scrollIntoView();
        await bro.assertView('second', '.RecentItems');

        await bro.yaLocalStorageClear();
    });
});
