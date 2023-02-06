describe('ProductPage', function() {
    const offersSelector = '.ShopList';
    const offerSelector = '.ShopList-Item';
    const reviewsLabelSelector = '.ShopList .ShopReviews';
    const shopDrawerSelector = '.ShopDrawer';
    const shopDrawerCloseSelector = '.ShopDrawer .ShopDrawer-Close';
    const modalCloseSelector = '.ShopDrawer .Modal-CloseButton';
    const titleSelector = '.ShopCardHeader';
    const reviewsListSelector = '.ReviewsList .ReviewsList-Item';
    const verifiedLinkSelector = '.ShopDrawer .ShopCard-ListItem';
    const verifiedModalSelector = '.VerifiedModal';
    const feedbackSelector = '.ShopCard-Feedback';
    const ReviewExpandButtonSelector = '.Review .TextCut-More';
    const ReviewCollapseButtonSelector = '.Review .TextCut-Collapsed';

    describe('Точки входа в шторку магазина', function() {
        it('Открытие из списка оферов', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';
            const shopDrawerExpFlag = isDesktop ? 'PRODUCTS_enable_reviews_offers_desktop=1' : 'PRODUCTS_enable_reviews_offers=1';

            await browser.yaOpenPageByUrl(`/products/product/868731267/sku/101223279916?exp_flags=sku_offers_ugc_enabled=true;${shopDrawerExpFlag}`);
            await browser.yaWaitForVisible(offersSelector);

            await browser.assertView('plain', offerSelector);

            await browser.click(reviewsLabelSelector);
            await browser.yaWaitForVisible(shopDrawerSelector);
            await browser.yaCheckBaobabEvent({ path: '$page.$main.card.shopList.listControls.shopItem.button-reviews' });
        });

        it('Текстовый вид', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';
            const shopDrawerExpFlag = isDesktop ? 'PRODUCTS_enable_reviews_offers_desktop=1' : 'PRODUCTS_enable_reviews_offers=1';

            await browser.yaOpenPageByUrl(`/products/product/868731267/sku/101223279916?exp_flags=sku_offers_ugc_enabled=true;PRODUCTS_enable_shop_reviews_redesign=text;${shopDrawerExpFlag}`);
            await browser.yaWaitForVisible(offersSelector);
            await browser.assertView('plain', offerSelector);
        });

        it('Кнопочный вид', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';
            const shopDrawerExpFlag = isDesktop ? 'PRODUCTS_enable_reviews_offers_desktop=1' : 'PRODUCTS_enable_reviews_offers=1';

            await browser.yaOpenPageByUrl(`/products/product/868731267/sku/101223279916?exp_flags=sku_offers_ugc_enabled=true;PRODUCTS_enable_shop_reviews_redesign=button;${shopDrawerExpFlag}`);
            await browser.yaWaitForVisible(offersSelector);
            await browser.assertView('plain', offerSelector);
        });
    });

    describe('Шторка магазина', function() {
        beforeEach(async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';
            const shopDrawerExpFlag = isDesktop ? 'PRODUCTS_enable_reviews_offers_desktop=1' : 'PRODUCTS_enable_reviews_offers=1';

            await browser.yaOpenPageByUrl(`/products/product/868731267/sku/101223279916?exp_flags=sku_offers_ugc_enabled=true;${shopDrawerExpFlag}`);
            await browser.yaWaitForVisible(offersSelector, 5000);

            await browser.click(reviewsLabelSelector);
            await browser.yaWaitForVisible(shopDrawerSelector, 5000);
            await browser.yaWaitForVisible(reviewsListSelector, 5000);
        });

        it('Тайтл', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';
            const titleNode = await browser.yaGetBaobabNode({
                path: isDesktop ?
                    '$page.$main.card.shopList.drawer-shop.shop-card.section.section-header.title' :
                    '$page.$main.card.shopList.drawer-shop.title',
                source: 'redir',
            });

            await browser.click(titleSelector);
            // Ждем когда залогируется событие клика
            await browser.waitUntil(async() => {
                const clickEvents = await browser.yaGetBaobabSentEvents('click');
                const titleEvent = titleNode && clickEvents && clickEvents.find(
                    event => event.id === titleNode.id,
                );

                return Boolean(titleEvent);
            }, { timeout: 5000 });
        });

        it('Магазин проверен Яндексом', async function() {
            const { browser } = this;

            const verifiedLinkNode = await browser.yaGetBaobabNode({ path: '$page.$main.card.shopList.drawer-shop.shop-card.section.list-item', source: 'redir' });

            await browser.click(verifiedLinkSelector);
            await browser.yaWaitForVisible(verifiedModalSelector, 5000);
            // Ждем когда залогируется событие клика
            await browser.waitUntil(async() => {
                const clickEvents = await browser.yaGetBaobabSentEvents('click');
                const verifiedLinkEvent = verifiedLinkNode && clickEvents && clickEvents.find(
                    event => event.id === verifiedLinkNode.id,
                );

                return Boolean(verifiedLinkEvent);
            }, { timeout: 5000 });
        });

        it('Пожаловаться', async function() {
            const { browser } = this;

            const feedbackNode = await browser.yaGetBaobabNode({ path: '$page.$main.card.shopList.drawer-shop.shop-card.section.section-footer.feedback', source: 'redir' });

            await browser.click(feedbackSelector);
            // Ждем когда залогируется событие клика
            await browser.waitUntil(async() => {
                const clickEvents = await browser.yaGetBaobabSentEvents('click');
                const feedbackEvent = feedbackNode && clickEvents && clickEvents.find(
                    event => event.id === feedbackNode.id,
                );

                return Boolean(feedbackEvent);
            }, { timeout: 5000 });
        });

        it('Закрытие шторки', async function() {
            const { browser } = this;
            const isDesktop = await browser.getMeta('platform') === 'desktop';

            const closeNode = await browser.yaGetBaobabNode({
                path: isDesktop ?
                    '$page.$main.card.shopList.drawer-shop.close-modal.button' :
                    '$page.$main.card.shopList.drawer-shop.button-close',
                source: 'redir',
            });

            await browser.click(isDesktop ? modalCloseSelector : shopDrawerCloseSelector);
            await browser.yaWaitForHidden(shopDrawerSelector, 5000);
            // Ждем когда залогируется событие клика
            await browser.waitUntil(async() => {
                const clickEvents = await browser.yaGetBaobabSentEvents('click');
                const closeEvent = closeNode && clickEvents && clickEvents.find(
                    event => event.id === closeNode.id,
                );

                return Boolean(closeEvent);
            }, { timeout: 5000 });
        });

        it('Раскрытие отзывов', async function() {
            const { browser } = this;

            const expandNode = await browser.yaGetBaobabNode({ path: '$page.$main.card.shopList.drawer-shop.section.reviews.review.button-expand', source: 'redir' });

            await browser.click(ReviewExpandButtonSelector);
            await browser.yaWaitForVisible(ReviewCollapseButtonSelector, 5000);

            const collapseNode = await browser.yaGetBaobabNode({ path: '$page.$main.card.shopList.drawer-shop.section.reviews.review.button-collapse', source: 'redir' });

            await browser.click(ReviewCollapseButtonSelector);
            await browser.yaWaitForVisible(ReviewExpandButtonSelector, 5000);

            // Ждем когда залогируется событие клика
            await browser.waitUntil(async() => {
                const clickEvents = await browser.yaGetBaobabSentEvents('click');
                const expandEvent = expandNode && clickEvents && clickEvents.find(
                    event => event.id === expandNode.id,
                );
                const collapseEvent = collapseNode && clickEvents && clickEvents.find(
                    event => event.id === collapseNode.id,
                );

                return Boolean(expandEvent && collapseEvent);
            }, { timeout: 5000 });
        });
    });

    it('Рейтинг магазина', async function() {
        const { browser } = this;

        await browser.yaOpenPageByUrl('/products/product/868731267/sku/101223279916?exp_flags=sku_offers_ugc_enabled=true;PRODUCTS_enable_shop_rating=1');
        await browser.yaWaitForVisible(offersSelector);
        await browser.assertView('plain', offerSelector);
    });

    it('Изменение дефолтной сортировки отзывов', async function() {
        const { browser } = this;
        const isDesktop = await browser.getMeta('platform') === 'desktop';
        const shopDrawerExpFlag = isDesktop ? 'PRODUCTS_enable_reviews_offers_desktop=1' : 'PRODUCTS_enable_reviews_offers=1';

        await browser.yaOpenPageByUrl(`/products/product/1414986413/sku/101648804078?no-tests=1&exp_flags=sku_offers_ugc_enabled=true;PRODUCTS_shop_reviews_sorting=by_time;${shopDrawerExpFlag}`);
        await browser.yaWaitForVisible(offersSelector);

        await browser.click(reviewsLabelSelector);
        await browser.yaWaitForVisible(shopDrawerSelector, 5000);
        await browser.yaWaitForVisible(verifiedLinkSelector, 5000);

        await browser.yaFindXHR(
            ({ spy }) => spy.url.startsWith('/ugcpub/digest') && /[&?]ranking=by_time/.test(spy.url),
            { timeoutMsg: 'Не был найден запрос за отзывами' },
        );
    });
});
