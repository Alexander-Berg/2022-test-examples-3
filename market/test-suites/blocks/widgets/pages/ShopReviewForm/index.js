import {makeSuite, mergeSuites, makeCase, prepareSuite} from 'ginny';

// components
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import {NO_AVERAGE_GRADE_TEXT} from '@self/platform/components/ShopReviewForm/Grades/constants';

// page objects
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';

// suites
import RatingStarsSuite from '@self/platform/spec/hermione/test-suites/blocks/components/RatingStars';

/**
 * Тесты на виджет ShopReviewForm.
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на магазин.', {
    feature: 'Оставление отзыва',
    story: mergeSuites(
        {
            'Незаполненная форма': {
                'при сохранении': {
                    'должна показывать сообщение об ошибке': makeCase({
                        id: 'marketfront-571',
                        issue: 'MARKETVERSTKA-23660',
                        async test() {
                            await this.reviewForm.submitFirstStep();

                            await this.reviewForm.waitForAverageGradeError()
                                .should.eventually.equal(true, 'Появилось сообщение об ошибке');

                            await this.reviewForm.getAverageGradeErrorText()
                                .should.eventually.equal(NO_AVERAGE_GRADE_TEXT);
                        },
                    }),
                },
            },
        },
        prepareSuite(RatingStarsSuite, {
            params: {
                ratingToSet: 4,
            },
            pageObjects: {
                ratingStars() {
                    return this.createPageObject(RatingStars, {parent: ShopReviewForm.averageGrade});
                },
            },
            hooks: {
                async beforeEach() {
                    await this.ratingStars.waitForVisible();
                },
            },
        })
    ),
});
