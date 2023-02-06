import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок ProductReviewFormMicro
 * @param {PageObject.RatingInput} productReviewFormMicro
 */

export default makeSuite('Звезды рейтинга', {
    environment: 'kadavr',
    story: {
        'При нажатии': {
            'ведут на страницу оставления отзыва': makeCase({
                params: {
                    productId: 'Идентификатор товара',
                    slug: 'Slug товара',
                },
                id: 'm-touch-3576',
                issue: 'MARKETFRONT-36998',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        // метод RatingInput.setRating багует и не даёт выставить конкретную оценку
                        action: () => this.ratingInput.setRating(5),
                        valueGetter: () => this.browser.getUrl(),
                    });

                    const currentUrl = await this.browser.getUrl();
                    const expectedUrl = await this.browser.yaBuildURL(
                        'market:product-reviews-add',
                        {
                            productId: String(this.params.productId),
                            slug: this.params.slug,
                            // метод RatingInput.setRating багует и попадает всегда в 5-ю звезду.
                            // Поэтому проверять выставление query-параметра averageGrade здесь мы не будем.
                            // Для проверки этого написан testament-тест
                            // averageGrade: this.params.rating,
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
});
