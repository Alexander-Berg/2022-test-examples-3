import {makeCase, makeSuite} from 'ginny';

import ShopReviewNew from '@self/platform/spec/page-objects/widgets/parts/ShopReviewNew';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';

/**
 * Тесты на вопросы о DSBS-доставке.
 *
 * @param {PageObject.ShopReviewFormPage} shopReviewNew
 */
export default makeSuite('Форма отзыва на магазин. GET-параметр feedbackOrderId. Негативный рейтинг доставки.', {
    id: 'm-touch-3390',
    issue: 'MARKETFRONT-16114',
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    params: {
        expectedNegativeDeliveryQuestions: 'Список вопросов о доставке, если оценка отрицательная',
    },
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingInput, {parent: ShopReviewNew.rating}),
                deliveryRatingStars: () => this.createPageObject(RatingInput, {parent: ShopReviewNew.delivery}),
            });
        },
        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },
        'При выборе негативного рейтинга доставки': {
            'должен отображаться корректный список вопросов о доставке': makeCase({
                async test() {
                    const {expectedNegativeDeliveryQuestions} = this.params;
                    await this.shopReviewNew.waitForFormLoaded();
                    await this.ratingStars.setRating(4);
                    await this.deliveryRatingStars.setRating(2);

                    await this.shopReviewNew.getDeliveryTitle()
                        .should.eventually.be.equal('Что пошло не так?', 'Заголовок вопросов правильный');

                    await this.shopReviewNew.getDeliveryQuestionCheckboxTitles()
                        .should.eventually.be.deep.equal(
                            expectedNegativeDeliveryQuestions,
                            'Список вопросов о доставке правильный'
                        );
                },
            }),
        },
        'должен корректно сохранять отзыв': makeCase({
            async test() {
                await this.shopReviewNew.waitForFormLoaded();
                await this.ratingStars.setRating(4);
                await this.deliveryRatingStars.setRating(2);

                await this.shopReviewNew.setDeliveryQuestionCheckboxByIndex(0);
                await this.shopReviewNew.setDeliveryQuestionCheckboxByIndex(1);

                await this.shopReviewNew.submitFirstStep();
                await this.shopReviewNew.submitForm();

                await this.browser.yaWaitForPageReloadedExtended(() => {
                    this.shopReviewNew.closeForm();
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
});
