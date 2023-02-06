import {makeSuite, makeCase} from 'ginny';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import Radio from '@self/platform/spec/page-objects/levitan-gui/Radio';
import ReviewPhotoGallery from '@self/platform/components/ProductReview/ReviewPhotoGallery/__pageObject';
import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import Description from '@self/platform/components/ProductReview/Description/__pageObject';

/**
 * Тесты на виджет ProductReviewForm.
 *
 * @param {PageObject.ProductReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на товар. Добавление нового отзыва.', {
    issue: 'MARKETFRONT-33503',
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

            'должна закрываться по завершению': makeCase({
                id: 'marketfront-4577',
                async test() {
                    const {reviewForm, browser} = this;

                    return reviewForm
                        .isVisible()
                        .should.eventually.equal(true, 'Форма отзыва видна')
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.firstStep'))
                        .then(() => reviewForm.submitFirstStep())
                        .then(() => reviewForm.waitForSecondStep())
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.secondStep'))
                        .then(() => reviewForm.submitSecondStep())
                        .then(() => reviewForm.waitForInvisible());
                },
            }),

            'должна сохранять данные в отзыв': makeCase({
                id: 'marketfront-4578',
                async test() {
                    const {reviewForm, browser, allure, reviewItemDescription} = this;

                    return reviewForm
                        .isVisible()
                        .should.eventually.equal(true, 'Форма отзыва видна')
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.firstStep'))
                        .then(() => reviewForm.submitFirstStep())
                        .then(() => reviewForm.waitForSecondStep())
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.secondStep'))
                        .then(() => this.browser.yaOpenPage('market:my-reviews'))

                        .then(() => allure.runStep(
                            'Проверяем наличие сохраненного текста отзыва',
                            () => reviewItemDescription.getProText().should.eventually.include(
                                'Automated tests generated content. Pro.',
                                'Отображается сохраненный текст отзыва'
                            )
                        ));
                },
            }),

            'должна сохранять фотографию в отзыв': makeCase({
                id: 'marketfront-4579',
                test() {
                    const {browser, allure, reviewItemPhotos, reviewForm} = this;

                    return browser.yaScenario(this, 'productReviews.fillForm.reviewWithPhoto')
                        .then(() => reviewForm.submitSecondStep())
                        .then(() => this.browser.yaOpenPage('market:my-reviews'))
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
