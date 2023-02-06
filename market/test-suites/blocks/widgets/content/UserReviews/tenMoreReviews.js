import {makeCase, makeSuite} from 'ginny';

const DEFAULT_VISIBLE_SNIPPETS_COUNT = 10;

/**
 * @param {PageObject.widgets.content.UserReviews} userReviews
 */

export default makeSuite('Если отзывов больше 10.', {
    params: {
        reviewsCount: 'Количество всех отзывов пользователя',
    },
    story: {
        'По-умолчанию': {
            'кнопка "Показать еще" отображается': makeCase({
                id: 'marketfront-3916',
                async test() {
                    return this.userReviews.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Показать еще" отображается');
                },
            }),
            'отображается верное количество отзывов': makeCase({
                id: 'marketfront-3917',
                async test() {
                    return this.userReviews.getUserReviewsCount()
                        .should.eventually.be.equal(DEFAULT_VISIBLE_SNIPPETS_COUNT, 'Отображается ровно 10 сниппетов');
                },
            }),
        },
        'Клик по кнопке "Показать еще"': {
            'скрывает саму кнопку': makeCase({
                id: 'marketfront-3918',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userReviews.clickLoadMoreButton(),
                        valueGetter: () => this.userReviews.isLoadMoreButtonVisible(),
                    });

                    return this.userReviews.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка "Показать еще" скрыта');
                },

            }),
            'загружает оставшиеся сниппеты ответов': makeCase({
                id: 'marketfront-3919',
                async test() {
                    const expectedReviewsCount = this.params.reviewsCount;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userReviews.clickLoadMoreButton(),
                        valueGetter: () => this.userReviews.getUserReviewsCount(),
                    });

                    const visibleReviewsCount = await this.userReviews.getUserReviewsCount();
                    return this.expect(visibleReviewsCount).be.equal(expectedReviewsCount,
                        'Отображается верное количество сниппетов');
                },
            }),
        },
    },
});
