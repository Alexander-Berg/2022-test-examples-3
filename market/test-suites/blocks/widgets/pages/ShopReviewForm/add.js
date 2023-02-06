import {makeSuite, makeCase} from 'ginny';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';
import Description from '@self/platform/components/ProductReview/Description/__pageObject';

/**
 * Тесты на блок ShopReviewForm.
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 * @param {PageObject.ReviewAgitation} reviewAgitation
 */
export default makeSuite('Форма отзыва на магазин. Добавление нового отзыва.', {
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ShopReviewForm.averageGrade}),
                reviewItemDescription: () => this.createPageObject(Description, {parent: this.reviewItem}),
            });
        },

        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        // TODO: Учесть замечания https://github.yandex-team.ru/market/MarketNode/pull/4551#discussion_r1038224
        'Заполненная форма': {
            'должна сохранять данные в отзыв': makeCase({
                id: 'marketfront-573',
                issue: 'MARKETVERSTKA-23662',
                async test() {
                    // eslint-disable-next-line no-unused-vars
                    const {reviewForm, reviewAgitation, browser, allure, reviewItemDescription} = this;

                    await reviewForm.isVisible()
                        .should.eventually.equal(true, 'Форма отзыва видна');
                    await browser.yaScenario(this, 'shopReviewsAdd.fillForm.firstStep');
                    await browser.yaWaitForChangeValue({
                        action: () => reviewForm.submitFirstStep(),
                        valueGetter: () => reviewForm.isSecondStepVisible(),
                    });

                    await browser.yaWaitForChangeValue({
                        action: () => reviewForm.submitSecondStep(),
                        valueGetter: () => reviewAgitation.isVisible(),
                    });

                    await browser.yaWaitForPageReloadedExtended(() => {
                        reviewAgitation.skipAgitation();
                    }, 5000);

                    await allure.runStep(
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

                    await allure.runStep(
                        'Проверяем наличие сохраненного текста отзыва',
                        () => reviewItemDescription.getProText().should.eventually.be.equal(
                            'Automated tests generated content. Pro.',
                            'Отображается сохраненный текст отзыва'
                        )
                    );
                },
            }),

            'должна переводить на страницу моих отзывов с параметром gradeId': makeCase({
                issue: 'MARKETVERSTKA-33006',
                id: 'marketfront-3301',
                async test() {
                    const {reviewForm, reviewAgitation, browser} = this;

                    await browser.yaScenario(this, 'shopReviewsAdd.fillForm.firstStep');
                    await browser.yaWaitForChangeValue({
                        action: () => reviewForm.submitFirstStep(),
                        valueGetter: () => reviewForm.isSecondStepVisible(),
                    });

                    await browser.yaWaitForChangeValue({
                        action: () => reviewForm.submitSecondStep(),
                        valueGetter: () => reviewAgitation.isVisible(),
                    });

                    await browser.yaWaitForPageReloadedExtended(() => {
                        reviewAgitation.skipAgitation();
                    }, 5000);

                    return this.allure.runStep(
                        'После сохранения происходит переход на страницу отзывов пользователя',
                        () => this.browser.getUrl()
                            .then(openedUrl => this
                                .expect(openedUrl, 'Проверяем что URL изменился')
                                .to.be.link({
                                    pathname: '/my/reviews',
                                    query: {
                                        gradeId: /\d+/,
                                    },
                                }, {
                                    mode: 'match',
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
