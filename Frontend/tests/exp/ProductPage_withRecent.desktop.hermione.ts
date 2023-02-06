describe('ProductPage / Просмотренные товары в карточке', function() {
    it('Внешний вид', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone&exp_flags=PRODUCTS_recent_items=1');
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

        const target = await bro.getAttribute('.RecentItems .ProductCardsList-Item:nth-child(1) .ProductCard', 'target');
        assert.strictEqual(target, '_blank');

        await bro.yaCheckNewTabOpen({
            async action() {
                await bro.click('.RecentItems .ProductCardsList-Item:nth-child(1) .ProductCard');
            },
            async checkTab() {
                await bro.yaWaitForPageLoad();
                await bro.yaWaitForVisible('.RecentItems', 'Просмотренные товары не появилась');
                await bro.$('.RecentItems').scrollIntoView();
                await bro.assertView('second', '.RecentItems');
            },
        });

        await bro.yaLocalStorageClear();
    });

    it('Скролл в блоке', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=футболка&exp_flags=PRODUCTS_recent_items=1');
        await bro.yaWaitForSearchPage();

        const { length: lengthBefore } = await bro.$$('.ProductCardsList-Item');
        assert.isTrue(lengthBefore >= 6, 'На странице меньше 6 карточек');

        await bro.click('.ProductCardsList-Item:nth-child(1) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.ProductEntityModal-CloseButton');
        await bro.click('.ProductEntityModal-CloseButton');
        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');
        await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'Паранжа не скрылась');

        await bro.click('.ProductCardsList-Item:nth-child(2) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.ProductEntityModal-CloseButton');
        await bro.click('.ProductEntityModal-CloseButton');
        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');
        await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'Паранжа не скрылась');

        await bro.click('.ProductCardsList-Item:nth-child(3) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.ProductEntityModal-CloseButton');
        await bro.click('.ProductEntityModal-CloseButton');
        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');
        await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'Паранжа не скрылась');

        await bro.click('.ProductCardsList-Item:nth-child(4) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.ProductEntityModal-CloseButton');
        await bro.click('.ProductEntityModal-CloseButton');
        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');
        await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'Паранжа не скрылась');

        await bro.click('.ProductCardsList-Item:nth-child(5) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.ProductEntityModal-CloseButton');
        await bro.click('.ProductEntityModal-CloseButton');
        await bro.yaWaitForVisible('.SearchPage-Products .ProductCardsList', 3000, 'На странице нет списка товаров');
        await bro.yaWaitForHidden('.ProductCardsList-Shadow', 3000, 'Паранжа не скрылась');

        await bro.click('.ProductCardsList-Item:nth-child(6) .ProductCard');
        await bro.yaWaitForVisible('.Card', 3000, 'Карточка товара не появилась');
        await bro.yaWaitForVisible('.RecentItems', 'Просмотренные товары не появилась');

        await bro.yaWaitForVisible('.RecentItems .Scroller .Scroller-ArrowButton_direction_right', 3000, 'Стрелка на право не появилась');
        await bro.click('.RecentItems .Scroller .Scroller-ArrowButton_direction_right');
        await bro.yaWaitForVisible('.RecentItems .Scroller .Scroller-ArrowButton_direction_left', 3000, 'Стрелка на лево не появилась');

        await bro.$('.RecentItems').scrollIntoView();
        await bro.assertView('scrolled', '.RecentItems');

        await bro.yaLocalStorageClear();
    });
});
