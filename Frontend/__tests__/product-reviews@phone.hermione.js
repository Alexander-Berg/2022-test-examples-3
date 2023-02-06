describe('Ecom-tap', () => {
    describe('Отзывы на товары', () => {
        hermione.only.notIn('iphone', 'Страница отзывов не работает на iphone < 12');
        describe('turbo-app-product-reviews', () => {
            it('На странице товара есть рейтинг и отзывы', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: '192',
                        pcgi: 'rnd%3Dw76c1jt',
                        patch: 'setProductReviews',
                        productReviews: '100,4.5',
                    },
                    expFlags: {
                        'turbo-app-product-reviews': '1',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.yaShouldBeVisible('.ProductScreen-Image .Rating');
                await browser.yaShouldBeVisible('.ProductReviewsLink');
                await browser.yaShouldBeVisible('.ProductReviewsLink .Rating');
            });

            it('На странице товара есть отзывы, но нет рейтинга, если он меньше значения флага', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: '192',
                        pcgi: 'rnd%3Dw76c1jt',
                        patch: 'setProductReviews',
                        productReviews: '100,3.5',
                    },
                    expFlags: {
                        'turbo-app-product-reviews': '42',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.yaShouldNotBeVisible('.ProductScreen-Image .Rating');
                await browser.yaShouldBeVisible('.ProductReviewsLink');
                await browser.yaShouldNotBeVisible('.ProductReviewsLink .Rating');
            });
        });

        describe('turbo-app-listing-reviews', () => {
            it('На странице каталога есть рейтинг товаров', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    query: {
                        patch: 'setProductReviews',
                        productReviews: '100,3.5',
                    },
                    expFlags: {
                        'turbo-app-listing-reviews': '1',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.yaScrollPage('.ProductList', 0.3);
                const ratings = await browser.getText('.ProductItem .Rating');
                assert.isTrue(Boolean(ratings.length), 'На странице нет товаров с рейтингом');
            });

            it('На странице каталога нет рейтингов товара ниже значения, переданного в флаг', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    query: {
                        patch: 'setProductReviews',
                        productReviews: '100,3.5',
                    },
                    expFlags: {
                        'turbo-app-listing-reviews': '42',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.yaScrollPage('.ProductList', 0.3);
                await browser.yaShouldNotBeVisible('.ProductItem .Rating');
            });
        });

        hermione.only.notIn('iphone', 'Страница отзывов не работает на iphone < 12');
        describe('turbo-app-read-only-reviews', () => {
            it('На странице товара есть кнопка читать отзывы', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: '192',
                        pcgi: 'rnd%3Dw76c1jt',
                        patch: 'setProductReviews',
                        productReviews: '100,3.5',
                    },
                    expFlags: {
                        'turbo-app-product-reviews': '1',
                        'turbo-app-read-only-reviews': '1',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.yaShouldBeVisible('.ProductReviewsLink');
            });

            it('На странице товара нет кнопки оставить отзыв', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: '192',
                        pcgi: 'rnd%3Dw76c1jt',
                        patch: 'setProductReviews',
                        productReviews: '0,3.5',
                    },
                    expFlags: {
                        'turbo-app-product-reviews': '42',
                        'turbo-app-read-only-reviews': '1',
                    },
                });

                await browser.yaWaitForVisible('.ScreenContent');
                await browser.yaShouldNotBeVisible('.ProductReviewsLink');
            });
        });
    });
});
