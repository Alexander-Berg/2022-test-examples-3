import {makeSuite, makeCase} from 'ginny';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import Radio from '@self/platform/spec/page-objects/levitan-gui/Radio';
import ReviewPhotoGallery from '@self/platform/components/ProductReview/ReviewPhotoGallery/__pageObject';
import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import Description from '@self/platform/components/ProductReview/Description/__pageObject';

/**
 * Тесты на виджет ProductReviewForm.
 *
 * @param {PageObject.ReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на товар. Добавление нового отзыва.', {
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ProductReviewForm.rating}),
                experience: () => this.createPageObject(Radio, {root: ProductReviewForm.experience}),
                recommendation: () =>
                    this.createPageObject(Radio, {root: ProductReviewForm.recommendation}),
                reviewItemPhotos: () => this.createPageObject(ReviewPhotoGallery, {parent: this.reviewItem}),
                reviewItemDescription: () => this.createPageObject(Description, {parent: this.reviewItem}),
                factors: () => new Array(5)
                    .fill(0)
                    .map((__, i) => this.createPageObject(RatingStars, {
                        root: `${ProductReviewForm.stepTwo} div:nth-child(${i + 3})`,
                    })),
            });
        },

        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        'Заполненная форма': {
            beforeEach() {
                return this.browser.setState('schema', {
                    mdsPictures: [{
                        namespace: 'market-ugc',
                        groupId: 3723,
                        imageName: '2a000001654282aec0648192ce44a1708325',
                    }],
                });
            },

            'должна сохранять данные в отзыв': makeCase({
                issue: 'MARKETVERSTKA-24089',
                id: 'marketfront-724',
                async test() {
                    // eslint-disable-next-line no-unused-vars
                    const {reviewForm, browser, allure, reviewItemDescription} = this;

                    return reviewForm
                        .isVisible()
                        .should.eventually.equal(true, 'Форма отзыва видна')
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.firstStep'))
                        .then(() => reviewForm.submitFirstStep())
                        .then(() => reviewForm.waitForSecondStep())
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.secondStep'))
                        .then(() => browser.yaWaitForPageReloadedExtended(() => {
                            reviewForm.submitSecondStep();
                        }, 5000))

                        .then(() => allure.runStep(
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
                        ))

                        .then(() => allure.runStep(
                            'Проверяем наличие сохраненного текста отзыва',
                            () => reviewItemDescription.getProText().should.eventually.be.equal(
                                'Automated tests generated content. Pro.',
                                'Отображается сохраненный текст отзыва'
                            )
                        ))
                    ;
                },
            }),

            'должна переводить на страницу моих отзывов с параметром gradeId': makeCase({
                issue: 'MARKETVERSTKA-33006',
                id: 'marketfront-3273',
                async test() {
                    await this.browser.yaScenario(this, 'productReviews.fillForm.firstStep');
                    await this.reviewForm.submitFirstStep();
                    await this.reviewForm.waitForSecondStep();
                    await this.browser.yaScenario(this, 'productReviews.fillForm.secondStep');
                    await this.browser.yaWaitForPageReloadedExtended(() => {
                        this.reviewForm.submitSecondStep();
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

            'должна сохранять фотографию в отзыв': makeCase({
                issue: 'MARKETVERSTKA-27656',
                id: 'marketfront-2322',
                test() {
                    const {browser, allure, reviewItemPhotos, reviewForm} = this;

                    return browser.yaScenario(this, 'productReviews.fillForm.reviewWithPhoto')
                        .then(() => browser.yaWaitForPageReloadedExtended(() => {
                            reviewForm.submitSecondStep();
                        }, 5000))
                        .then(() => allure.runStep(
                            'Проверяем наличие сохраненной фотографии у отзыва',
                            () => reviewItemPhotos.isVisible().should.eventually.be.equal(
                                true,
                                'Сохраненное фото отображается'
                            )
                        ));
                },
            }),
        },
    },
});
