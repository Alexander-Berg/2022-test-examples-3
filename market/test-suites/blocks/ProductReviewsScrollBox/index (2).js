import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок ProductReviewScrollBox
 * @param {PageObject.ProductReviewScrollBox} productReviewScrollBox
 */

export default makeSuite('Скроллбокс отзывов.',
    {
        environment: 'kadavr',
        story: {
            'Кнопка "Смотреть все"': {
                'При нажатии': {
                    'ведет на страницу отзывов': makeCase({
                        params: {
                            productId: 'Идентификатор товара',
                            slug: 'Slug товара',
                        },
                        id: 'm-touch-3571',
                        issue: 'MARKETFRONT-36998',
                        async test() {
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.productReviewsScrollBox.reviewsLinkClick(),
                                valueGetter: () => this.browser.getUrl(),
                            });

                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL(
                                'touch:product-reviews',
                                {
                                    productId: String(this.params.productId),
                                    slug: this.params.slug,
                                }
                            );
                            return this.expect(currentUrl).to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                        },
                    }),
                },
            },
            'Кнопка "Все отзывы"': {
                'При нажатии': {
                    'ведет на страницу отзывов': makeCase({
                        params: {
                            productId: 'Идентификатор товара',
                            slug: 'Slug товара',
                        },
                        id: 'm-touch-3572',
                        issue: 'MARKETFRONT-36998',
                        async test() {
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.productReviewsScrollBox.moreCardClick(),
                                valueGetter: () => this.browser.getUrl(),
                            });

                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL(
                                'touch:product-reviews',
                                {
                                    productId: String(this.params.productId),
                                    slug: this.params.slug,
                                }
                            );
                            return this.expect(currentUrl).to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                        },
                    }),
                },
            },
            'Отзыв': {
                'При нажатии': {
                    'ведет на страницу отзывов': makeCase({
                        params: {
                            productId: 'Идентификатор товара',
                            slug: 'Slug товара',
                            reviewId: 'Идентификатор отзыва',
                        },
                        id: 'm-touch-3573',
                        issue: 'MARKETFRONT-36998',
                        async test() {
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.productReviewsScrollBox.reviewClick(),
                                valueGetter: () => this.browser.getUrl(),
                            });

                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL(
                                'touch:product-reviews',
                                {
                                    productId: String(this.params.productId),
                                    slug: this.params.slug,
                                    firstReviewId: String(this.params.reviewId),
                                }
                            );
                            return this.expect(currentUrl).to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                        },
                    }),
                },
            },
        },
    }
);
