import {makeCase, makeSuite} from 'ginny';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';
import {DSBS_NEGATIVE_TITLE} from '@self/platform/components/ShopReviewForm/Grades/constants';

/**
 * Тесты на вопросы о DSBS-доставке.
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на магазин. GET-параметр feedbackOrderId. Негативный рейтинг доставки.', {
    id: 'marketfront-4113',
    issue: 'MARKETFRONT-16109',
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    params: {
        expectedNegativeDeliveryQuestions: 'Список вопросов о доставке, если оценка отрицательная',
    },
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ShopReviewForm.averageGrade}),
                deliveryRatingStars: () => this.createPageObject(RatingStars, {parent: ShopReviewForm.deliveryGrade}),
            });
        },
        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },
        'При выборе негативного рейтинга доставки': {
            'должен отображаться корректный список вопросов о доставке': makeCase({
                async test() {
                    const {expectedNegativeDeliveryQuestions} = this.params;
                    await this.ratingStars.waitForVisible();
                    await this.ratingStars.setRating(4);

                    await this.deliveryRatingStars.waitForVisible();
                    await this.deliveryRatingStars.setRating(2);

                    await this.reviewForm.getDsbsDeliveryQuestionsTitle()
                        .should.eventually.be.equal(DSBS_NEGATIVE_TITLE, 'Заголовок вопросов правильный');

                    await this.reviewForm.getDsbsDeliveryQuestionCheckboxTitles()
                        .should.eventually.be.deep.equal(
                            expectedNegativeDeliveryQuestions,
                            'Список вопросов о доставке правильный'
                        );
                },
            }),
            'должен корректно сохранять отзыв': makeCase({
                async test() {
                    await this.ratingStars.waitForVisible();
                    await this.ratingStars.setRating(4);

                    await this.deliveryRatingStars.waitForVisible();
                    await this.deliveryRatingStars.setRating(2);

                    await this.reviewForm.setDsbsDeliveryQuestionCheckboxByIndex(0);
                    await this.reviewForm.setDsbsDeliveryQuestionCheckboxByIndex(1);

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewForm.submitFirstStep(),
                        valueGetter: () => this.reviewForm.isSecondStepVisible(),
                    });

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewForm.submitSecondStep(),
                        valueGetter: () => this.reviewAgitation.isVisible(),
                    });

                    await this.browser.yaWaitForPageReloadedExtended(() => {
                        this.reviewAgitation.skipAgitation();
                    }, 5000);

                    await this.browser.allure.runStep(
                        'После сохранения происходит переход на страницу отзывов пользователя',
                        () => Promise.all([
                            this.browser.getUrl(),
                            this.browser.yaBuildURL('market:my-reviews'),
                        ])
                            .then(([openedUrl, expectedPath]) => this
                                .expect(openedUrl, 'Проверяем что URL изменился')
                                .to.be.link(expectedPath, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                })
                            )
                    );
                },
            }),
        },
    },
});
