import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ProductReviewNew from '@self/platform/spec/page-objects/widgets/parts/ProductReviewNew';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';

/**
 * Тест на формы для отзыва на товар
 * @property {PageObject.ProductReviewNew} productReviewNew
 * @property {PageObject.RatingInput} ratingInput
 */
export default makeSuite('Поздравительный экран. У пользователя есть еще задания', {
    story: {
        'При нажатии на кнопку "Отлично" появляется Промежуточный экран.': mergeSuites(
            {
                async beforeEach() {
                    await this.ratingInput.setAverageGrade();
                    await this.productReviewNew.setCommentTextField('test comment text');
                    await this.productReviewNew.clickNextStepButton();
                    await this.browser.allure.runStep('Ждем появления кнопки', () =>
                        this.browser.waitForVisible(ProductReviewNew.nextStepButton, 5000)
                    );
                    await this.productReviewNew.clickSubmitButton();
                    await this.setPageObjects({
                        gainedExpertise: () => this.createPageObject(GainedExpertise),
                    });
                    await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                        this.browser.waitForVisible(GainedExpertise.root, 5000)
                    );
                    await this.browser.allure.runStep('Ждем появления кнопки "Отлично"', () =>
                        this.browser.waitForVisible(GainedExpertise.closeButton, 5000)
                    );
                    await this.gainedExpertise.close();
                },
                'При нажатии на кнопку К заданиям': {
                    'происходит редирект на Мои задания': makeCase({
                        id: 'm-touch-3627',
                        async test() {
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
                        id: 'm-touch-3626',
                        async test() {
                            await this.browser.allure.runStep('Ждем появления Промежуточного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.exrtaScreenIcon, 5000)
                            );

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.gainedExpertise.clickNotNow(),
                                valueGetter: () => this.gainedExpertise.isVisible(),
                            });

                            await this.expect(
                                await this.gainedExpertise.isVisible()
                            ).to.be.equal(false, 'Промежуточный экран закрылся');
                        },
                    }),
                    'происходит редирект на мои отзывы': makeCase({
                        id: 'm-touch-3628',
                        async test() {
                            await this.browser.allure.runStep('Ждем появления Промежуточного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.exrtaScreenIcon, 5000)
                            );
                            await this.browser.yaWaitForChangeUrl(() => this.gainedExpertise.clickNotNow());

                            const currentUrl = await this.browser.getUrl();
                            const expectedUrl = await this.browser.yaBuildURL('market:my-reviews');

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
                    'экран закрывается происходит редирект на мои отзывы': makeCase({
                        id: 'm-touch-3629',
                        async test() {
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
                            const expectedUrl = await this.browser.yaBuildURL('market:my-reviews');

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
