import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductReviews} productReviews
 */
export default makeSuite('Список отзывов с количеством элементов больше 10.', {
    story: {
        'По умолчанию': {
            'Отображается 10 отзывов': makeCase({
                id: 'm-touch-2958',
                issue: 'MOBMARKET-13070',
                test() {
                    return this.productReviews.productReviewsCount
                        .should.eventually.to.be.equal(10, 'На странице 10 отзывов');
                },
            }),
            'Отображается кнопка "Показать еще"': makeCase({
                id: 'm-touch-2958',
                issue: 'MOBMARKET-13070',
                test() {
                    return this.productReviews.isLoadMoreButtonShown()
                        .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" видимая');
                },
            }),
        },
        'При клике "Показать еще"': {
            'Загружается больше отзывов': makeCase({
                id: 'm-touch-2958',
                issue: 'MOBMARKET-13070',
                test() {
                    return this.productReviews.productReviewsCount
                        .then(
                            reviewsBefore =>
                                this.browser.yaWaitForChangeValue({
                                    action: () => this.productReviews.loadMoreReviews(),
                                    valueGetter: () => this.productReviews.productReviewsCount,
                                })
                                    .then(() => this.productReviews.productReviewsCount)
                                    .should.eventually.to.be.greaterThan(
                                        reviewsBefore,
                                        'Отзывов на товар на странице стало больше'
                                    )
                        );
                },
            }),
        },
    },
});
