import {makeCase, makeSuite} from 'ginny';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';

/**
 * Тесты на виджет ProductReviewForm.
 *
 * @param {PageObject.ProductReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на товар. GET-параметр averageGrade.', {
    id: 'marketfront-4014',
    issue: 'MARKETFRONT-8714',
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    params: {
        expectedAverageGrade: 'значение параметра averageGrade',
    },
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ProductReviewForm.rating}),
            });
        },
        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        'Значение параметра от 1 до 5': {
            'должно устанавливать значение Общей оценки': makeCase({
                test() {
                    const {expectedAverageGrade} = this.params;
                    return this.ratingStars.getRating()
                        .should.eventually.equal(expectedAverageGrade, `значение общей оценки ${expectedAverageGrade}`);
                },
            }),
        },
    },
});
