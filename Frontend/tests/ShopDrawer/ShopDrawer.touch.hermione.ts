describe('ProductPage', function() {
    const offersSelector = '.ShopList';
    const reviewsLabelSelector = '.ShopList-Item:nth-child(3) .ShopReviews';
    const shopDrawerSelector = '.ShopDrawer';
    const reviewsListSelector = '.ReviewsList .ReviewsList-Item';
    const shareSelector = '.ShopDrawer .ShareNative.Link';

    describe('Шторка магазина', function() {
        it('Шаринг', async function() {
            const { browser } = this;

            await browser.yaOpenPageByUrl('/products/product/1722785411/sku/100244187891?exp_flags=PRODUCTS_enable_reviews_offers=1;sku_offers_ugc_enabled=1;shop_info_page=1;');
            await browser.yaWaitForVisible(offersSelector, 5000);

            await browser.click(reviewsLabelSelector);
            await browser.yaWaitForVisible(shopDrawerSelector, 5000);
            await browser.yaWaitForVisible(reviewsListSelector, 5000);

            const shareNode = await browser.yaGetBaobabNode({ path: '$page.$main.card.shopList.drawer-shop.share', source: 'redir' });

            await browser.click(shareSelector);
            // Ждем когда залогируется событие клика
            await browser.waitUntil(async() => {
                const clickEvents = await browser.yaGetBaobabSentEvents('click');
                const shareEvent = shareNode && clickEvents && clickEvents.find(
                    event => event.id === shareNode.id,
                );

                return Boolean(shareEvent);
            }, { timeout: 5000 });
        });
    });
});
