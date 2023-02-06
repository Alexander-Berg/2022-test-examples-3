import {makeCase, makeSuite} from 'ginny';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';

/**
 * Тесты на виджет ShopReviewForm.
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на магазин. GET-параметр averageGrade.', {
    id: 'marketfront-4015',
    issue: 'MARKETFRONT-8714',
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    params: {
        expectedAverageGrade: 'значение параметра averageGrade',
    },
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ShopReviewForm.averageGrade}),
            });
        },
        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        'Значение параметра от 1 до 5': {
            'должно устанавливать значение Общей оценки': makeCase({
                test() {
                    const {expectedAverageGrade} = this.params;
                    return this.ratingStars.waitForVisible()
                        .then(() => this.ratingStars.getRating())
                        .should.eventually.equal(expectedAverageGrade, `значение общей оценки ${expectedAverageGrade}`);
                },
            }),
        },
    },
});
