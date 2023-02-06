const openPageWithReviwsButton = async(browser, reviewsCount = 0, rating = 0) => {
    await browser.yaOpenEcomSpa({
        service: 'spideradio.github.io',
        pageType: 'about',
        query: {
            patch: 'setShopReviewsData',
            shopReviews: `${reviewsCount},${rating}`,
        },
    });

    await browser.yaWaitForVisible('.ShopReviewsLink');
};

describe('Ecom-tap', function() {
    hermione.only.notIn('iphone', 'Страница рейтинга не работает на ios < 12');
    describe('ShopReviewsLink', function() {
        it('Внешний вид с отзывами, без рейтинга', async function() {
            const { browser } = this;
            await openPageWithReviwsButton(browser, 421);
            await browser.assertView('button', '.ShopReviewsLink');
        });

        it('Внешний вид с рейтингом, без отзывов', async function() {
            const { browser } = this;
            await openPageWithReviwsButton(browser, 0, 4.5);
            await browser.assertView('button', '.ShopReviewsLink');
        });

        it('Внешний вид с рейтингом и отзывами', async function() {
            const { browser } = this;
            await openPageWithReviwsButton(browser, 123, 2.1);
            await browser.assertView('button', '.ShopReviewsLink');
        });
    });
});
