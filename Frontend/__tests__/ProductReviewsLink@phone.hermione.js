const __ym = {
    doc_ui: 'touch-phone',
    turbo_page: 1,
};

hermione.only.notIn('iphone', 'Страница отзывов не работает на iphone < 12');
describe('ProductReviewsLink', function() {
    it('Без рейтинга и отзывов', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/rnd/w76c1jt',
            query: { product_id: 192 },
            expFlags: {
                'turbo-app-product-reviews': '1',
                'analytics-disabled': '0',
            },
        });

        await browser.yaWaitForVisible('.EcomScreen_type_product');

        await browser.yaMockImages();
        await browser.assertView('image', ['.turbo-card-slider']);

        await browser.yaWaitForVisible('.ProductReviewsLink');
        await browser.assertView('link', ['.ProductReviewsLink']);

        await browser.click('.ProductReviewsLink');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'leave-product-reviews-click',
            params: { ecom_spa: 1, __ym },
        });

        const url = await browser.getUrl();
        const { searchParams } = new URL(url);

        assert.strictEqual(searchParams.get('page_type'), 'reviews', 'не произошёл переход на страницу отзывов');
    });

    it('Без рейтинга и отзывов, под описанием', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/rnd/w76c1jt',
            query: { product_id: 192 },
            expFlags: {
                'turbo-app-product-reviews': '1',
                'analytics-disabled': '0',
                'turbo-app-product-reviews-position-bottom': '1',
            },
        });

        await browser.yaWaitForVisible('.ProductReviewsLink');
        await browser.yaScrollPage('.ProductReviewsLink');
        const className = await browser.getAttribute('.ProductScreen-Info > div:last-child', 'class');
        assert.isTrue(className.indexOf('ProductReviewsLink') !== -1);
    });

    it('C рейтингом и отзывами', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            url: '/turbo/spideradio.github.io/s/rnd/w76c1jt',
            query: {
                product_id: 192,
                patch: 'setProductReviews',
                productReviews: '100,4.5',
            },
            expFlags: {
                'turbo-app-product-reviews': '1',
                'analytics-disabled': '0',
            },
        });

        await browser.yaWaitForVisible('.EcomScreen_type_product');

        await browser.yaMockImages();
        await browser.assertView('image', ['.turbo-card-slider']);

        await browser.yaWaitForVisible('.ProductReviewsLink');
        await browser.assertView('link', ['.ProductReviewsLink']);

        await browser.click('.ProductReviewsLink');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'read-product-reviews-click',
            params: { ecom_spa: 1, __ym },
        });
    });

    [5, 3.5, 2, 1].forEach(rating => {
        hermione.only.in(['chrome-phone'], 'Ускоряем тесты - проверяем только цвет рейтинга');
        it(`Внешний вид с рейтингом ${rating}`, async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/rnd/w76c1jt',
                query: {
                    product_id: 192,
                    patch: 'setProductReviews',
                    productReviews: `100,${rating}`,
                },
                expFlags: {
                    'turbo-app-product-reviews': '1',
                    'analytics-disabled': '0',
                },
            });

            await browser.yaWaitForVisible('.EcomScreen_type_product');

            await browser.yaWaitForVisible('.ProductReviewsLink');
            await browser.assertView('link', ['.ProductReviewsLink']);
        });
    });
});
