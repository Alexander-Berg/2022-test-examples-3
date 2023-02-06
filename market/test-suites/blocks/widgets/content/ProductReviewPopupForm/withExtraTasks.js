import {makeCase, makeSuite, mergeSuites} from 'ginny';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import Radio from '@self/platform/spec/page-objects/levitan-gui/Radio';
import ReviewPhotoGallery from '@self/platform/components/ProductReview/ReviewPhotoGallery/__pageObject';
import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import Description from '@self/platform/components/ProductReview/Description/__pageObject';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';

/**
 * Тест на формы для отзыва на товар
 * @param {PageObject.ProductReviewForm} reviewForm
 * @property {PageObject.RatingInput} ratingInput
 */
export default makeSuite('Поздравительный экран. У пользователя есть еще задания', {
    story: {
        'При нажатии на кнопку "Отлично" появляется Промежуточный экран.': mergeSuites(
            {
                async beforeEach() {
                    await this.setPageObjects({
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
                    const {reviewForm, browser} = this;

                    return reviewForm
                        .isVisible()
                        .should.eventually.equal(true, 'Форма отзыва видна')
                        .then(() => browser.yaScenario(this, 'productReviews.fillForm.firstStep'))
                        .then(() => reviewForm.submitFirstStep())
                        .then(() => reviewForm.waitForSecondStep())
                        .then(() => reviewForm.submitSecondStep())
                        .then(() => reviewForm.waitForInvisible());
                },
                'При нажатии на кнопку К заданиям': {
                    'происходит редирект на Мои задания': makeCase({
                        id: 'marketfront-4803',
                        async test() {
                            await this.setPageObjects({
                                gainedExpertise: () => this.createPageObject(GainedExpertise),
                            });
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );
                            await this.gainedExpertise.close();
                            await this.browser.allure.runStep('Ждем появления Промежуточного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.exrtaScreenIcon, 5000)
                            );
                            await this.browser.yaWaitForChangeUrl(() => this.gainedExpertise.goToTasks());

                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL('market:my-tasks');

                            await this.browser.allure.runStep(
                                'Проверяем URL открытой страницы',
                                () => this.expect(currentUrl).to.be.link(
                                    expectedUrl,
                                    {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }
                                )
                            );
                        },
                    }),
                },
                'При нажатии на кнопку Не сейчас': {
                    'экран закрывается': makeCase({
                        id: 'marketfront-4804',
                        async test() {
                            await this.setPageObjects({
                                gainedExpertise: () => this.createPageObject(GainedExpertise),
                            });
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );
                            await this.gainedExpertise.close();

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.gainedExpertise.clickNotNow(),
                                valueGetter: () => this.gainedExpertise.isVisible(),
                            });

                            await this.expect(
                                await this.gainedExpertise.isVisible()
                            ).to.be.equal(false, 'Промежуточный экран закрылся');
                        },
                    }),
                    'остаемся на текущей странице': makeCase({
                        id: 'marketfront-4805',
                        async test() {
                            await this.setPageObjects({
                                gainedExpertise: () => this.createPageObject(GainedExpertise),
                            });
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );
                            await this.gainedExpertise.close();
                            await this.browser.allure.runStep('Ждем появления Промежуточного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.exrtaScreenIcon, 5000)
                            );
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.gainedExpertise.clickNotNow(),
                                valueGetter: () => this.gainedExpertise.isVisible(),
                            });
                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL('market:index');

                            await this.browser.allure.runStep(
                                'Проверяем URL открытой страницы',
                                () => this.expect(currentUrl).to.be.link(
                                    expectedUrl,
                                    {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }
                                )
                            );
                        },
                    }),
                },
                'При нажатии на паранжу': {
                    'экран закрывается, остаемся на текущей странице': makeCase({
                        id: 'marketfront-4806',
                        async test() {
                            await this.setPageObjects({
                                gainedExpertise: () => this.createPageObject(GainedExpertise),
                            });
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );
                            await this.gainedExpertise.close();
                            await this.browser.allure.runStep('Ждем появления Промежуточного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.exrtaScreenIcon, 5000)
                            );

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.gainedExpertise.paranjaClick(),
                                valueGetter: () => this.gainedExpertise.isVisible(),
                            });

                            await this.expect(
                                await this.gainedExpertise.isVisible()
                            ).to.be.equal(false, 'Промежуточный экран закрылся');

                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL('market:index');

                            await this.browser.allure.runStep(
                                'Проверяем URL открытой страницы',
                                () => this.expect(currentUrl).to.be.link(
                                    expectedUrl,
                                    {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    }
                                )
                            );
                        },
                    }),
                },
            }
        ),
    },
});
