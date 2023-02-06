import {makeCase, makeSuite, prepareSuite, mergeSuites} from 'ginny';

import PhotoAttachedSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/PhotoList';
import ProductReviewNew from '@self/platform/spec/page-objects/widgets/parts/ProductReviewNew';
import GainedExpertise from '@self/project/src/widgets/content/GainedExpertise/__pageObject';
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';
import RadioBox from '@self/platform/components/RadioBox/__pageObject';
import CloseConfirm from '@self/platform/components/ReviewForm/CloseConfirm/__pageObject';

/**
 * Тест на формы для отзыва на товар
 * @property {PageObject.ProductReviewNew} productReviewNew
 * @property {PageObject.RatingInput} ratingInput
 */
export default makeSuite('Форма отзыва на товар', {
    params: {
        productId: 'Id товара',
        slug: 'Слаг товара',
    },
    story: {
        'Первый экран.': mergeSuites(
            prepareSuite(PhotoAttachedSuite, {
                hooks: {
                    async beforeEach() {
                        await this.ratingInput.isVisible();
                        await this.ratingInput.setAverageGrade();
                    },
                },
            }),
            {
                'При выставлении средней оценки': {
                    'появляются текстовые поля': makeCase({
                        id: 'm-touch-3359',
                        async test() {
                            await this.ratingInput.isVisible();
                            let isFieldsVisible = await this.productReviewNew.isTextFieldsVisible();
                            await this.expect(isFieldsVisible)
                                .to.be.equal(false, 'Текстовые поля скрыты');
                            await this.ratingInput.setAverageGrade();
                            isFieldsVisible = await this.productReviewNew.isTextFieldsVisible();
                            await this.expect(isFieldsVisible)
                                .to.be.equal(true, 'Текстовые поля видны');
                        },
                    }),
                    'появляется форма для фото': makeCase({
                        id: 'm-touch-3360',
                        async test() {
                            await this.ratingInput.isVisible();
                            let isPhotoListVisible = await this.productReviewNew.isPhotoListVisible();
                            await this.expect(isPhotoListVisible)
                                .to.be.equal(false, 'Текстовые поля скрыты');
                            await this.ratingInput.setAverageGrade();
                            isPhotoListVisible = await this.productReviewNew.isPhotoListVisible();
                            await this.expect(isPhotoListVisible)
                                .to.be.equal(true, 'Форма для добавления фото поля видна');
                        },
                    }),
                },
            },
            {
                'при превышении максимального количества символов в форме': {
                    'кнопка "Дальше" отключается': makeCase({
                        id: 'm-touch-2981',
                        async test() {
                            const text = 'z'.repeat(2001);
                            await this.ratingInput.setAverageGrade();

                            await this.expect(await this.productReviewNew.isTextFieldsVisible())
                                .to.be.equal(true, 'Текстовые поля видны');
                            await this.productReviewNew.setProTextField(text);
                            await this.expect(await this.productReviewNew.isNextStepButtonDisabled())
                                .to.be.equal(true, 'Кнопка "Дальше" заблокирована');
                            await this.productReviewNew.clearProTextField();
                            await this.expect(await this.productReviewNew.isNextStepButtonDisabled())
                                .to.be.equal(false, 'Кнопка "Дальше" разблокирована');
                            await this.productReviewNew.setContraTextField(text);
                            await this.expect(await this.productReviewNew.isNextStepButtonDisabled())
                                .to.be.equal(true, 'Кнопка "Дальше" заблокирована');
                            await this.productReviewNew.clearContraTextField();
                            await this.expect(await this.productReviewNew.isNextStepButtonDisabled())
                                .to.be.equal(false, 'Кнопка "Дальше" разблокирована');
                            await this.productReviewNew.setCommentTextField(text);
                            await this.expect(await this.productReviewNew.isNextStepButtonDisabled())
                                .to.be.equal(true, 'Кнопка "Дальше" заблокирована');
                            await this.productReviewNew.clearCommentTextField();
                            await this.expect(await this.productReviewNew.isNextStepButtonDisabled())
                                .to.be.equal(false, 'Кнопка "Дальше" разблокирована');
                        },
                    }),
                },
            }
        ),
        'Второй экран.': mergeSuites(
            {
                async beforeEach() {
                    await this.ratingInput.setAverageGrade();
                    this.setPageObjects({
                        factorRating: () => this.createPageObject(RatingInput, {
                            parent: `${ProductReviewNew.factorScreen}>div:nth-child(2)`,
                        }),
                    });
                },
                'По умолчанию': {
                    'кнопка "Отправить" активна': makeCase({
                        id: 'm-touch-3361',
                        async test() {
                            await this.productReviewNew.clickNextStepButton();
                            await this.expect(await this.productReviewNew.isSubmitButtonActive())
                                .to.be.equal(true, 'Кнопка "Дальше" разблокирована');
                        },
                    }),
                    'факторная оценка кликабельна': makeCase({
                        id: 'm-touch-3362',
                        async test() {
                            await this.productReviewNew.clickNextStepButton();
                            await this.browser.allure.runStep('Ждем появления поля со звездами', () =>
                                this.browser.waitForVisible(RatingInput.root, 5000)
                            );
                            await this.factorRating.setAverageGrade();
                            const startsCount = await this.factorRating.getSelectedStarsCountFromData();
                            await this.expect(startsCount).to.be.equal(1, 'Факторы кликабельны');
                        },
                    }),
                },
            }
        ),
        'Кнопка Назад в шапке.': mergeSuites(
            {
                async beforeEach() {
                    await this.ratingInput.setAverageGrade();
                    this.setPageObjects({
                        factorRating: () => this.createPageObject(RatingInput, {
                            parent: `${ProductReviewNew.factorScreen}>div:nth-child(2)`,
                        }),
                        radiobox: () => this.createPageObject(RadioBox, {
                            parent: `${ProductReviewNew.factorScreen}`,
                        }),
                    });
                },
                'При возвращении со второго экрана на первый': {
                    'написанный текст остался в форме': makeCase({
                        id: 'm-touch-3363',
                        async test() {
                            await this.expect(await this.productReviewNew.isTextFieldsVisible())
                                .to.be.equal(true, 'Текстовые поля видны');
                            await this.productReviewNew.setProTextField('test pro text');
                            await this.productReviewNew.setContraTextField('test contra text');
                            await this.productReviewNew.setCommentTextField('test comment text');
                            await this.productReviewNew.clickNextStepButton();
                            await this.productReviewNew.clickBackButton();
                            await this.expect(await this.productReviewNew.getProTextField())
                                .to.be.equal('test pro text', 'Текст в форме Достоинства остался');
                            await this.expect(await this.productReviewNew.getContraTextField())
                                .to.be.equal('test contra text', 'Текст в форме Недостатки остался');
                            await this.expect(await this.productReviewNew.getCommentTextField())
                                .to.be.equal('test comment text', 'Текст в форме Комментарии остался');
                        },
                    }),
                },
                'При возвращении со второго экрана на первый и обратно': {
                    'выбранные факторы и радио-кнопки остаются': makeCase({
                        id: 'm-touch-3364',
                        async test() {
                            await this.expect(await this.productReviewNew.isTextFieldsVisible())
                                .to.be.equal(true, 'Текстовые поля видны');
                            await this.productReviewNew.clickNextStepButton();

                            await this.factorRating.setAverageGrade();
                            await this.radiobox.checkRadioButton();

                            await this.productReviewNew.clickBackButton();
                            await this.productReviewNew.clickNextStepButton();
                            await this.expect(
                                await this.factorRating.getSelectedStarsCountFromData()
                            ).to.be.equal(1, 'Факторы сохранились в форме');
                            await this.expect(
                                await this.radiobox.getCheckedRadioValue()
                            ).to.be.equal(0, 'Выбор радио-кнопки сохранился в форме');
                        },
                    }),
                },
            }
        ),
        'Крестик в шапке.': mergeSuites(
            {
                beforeEach() {
                    this.setPageObjects({
                        closeConfirm: () => this.createPageObject(CloseConfirm),
                        factorRating: () => this.createPageObject(RatingInput, {
                            parent: `${ProductReviewNew.factorScreen}>div:nth-child(2)`,
                        }),
                    });
                },
                'При выставлении только оценки и попытке закрыть форму': {
                    'не появляется диалоговое окно': makeCase({
                        id: 'm-touch-3365',
                        async test() {
                            await this.ratingInput.setAverageGrade();
                            await this.productReviewNew.clickCloseButton();
                            await this.expect(
                                await this.closeConfirm.isVisible()
                            ).to.be.equal(false, 'Диалоговое окно не появилось');
                        },
                    }),
                },
                'При выставлении оценки, переходе на второй шаг и попытке закрыть форму': {
                    'выходим из формы': makeCase({
                        id: 'm-touch-3366',
                        async test() {
                            await this.ratingInput.setAverageGrade();
                            await this.expect(await this.productReviewNew.isTextFieldsVisible())
                                .to.be.equal(true, 'Текстовые поля видны');
                            await this.productReviewNew.clickNextStepButton();
                            await this.productReviewNew.clickCloseButton();
                            await this.expect(
                                await this.closeConfirm.isVisible()
                            ).to.be.equal(false, 'Диалоговое окно не появилось');
                        },
                    }),
                },
                'При переходе на второй шаг, выборе факторов и попытке закрыть форму': {
                    'появляется диалоговое окно': makeCase({
                        id: 'm-touch-3367',
                        async test() {
                            await this.ratingInput.setAverageGrade();
                            await this.productReviewNew.clickNextStepButton();
                            await this.browser.allure.runStep('Ждем появления поля со звездами', () =>
                                this.browser.waitForVisible(RatingInput.root, 5000)
                            );
                            await this.factorRating.setAverageGrade();
                            await this.productReviewNew.clickCloseButton();

                            await this.expect(
                                await this.closeConfirm.isVisible()
                            ).to.be.equal(true, 'Диалоговое окно появилось');
                        },
                    }),
                },
                'При переходе на второй шаг, возврате на первый, заполнении текста и попытке выхода': {
                    'появляется диалоговое окно': makeCase({
                        id: 'm-touch-3368',
                        async test() {
                            await this.ratingInput.setAverageGrade();
                            await this.productReviewNew.clickNextStepButton();
                            await this.productReviewNew.clickBackButton();
                            await this.productReviewNew.setCommentTextField('test comment text');
                            await this.productReviewNew.clickCloseButton();

                            await this.expect(
                                await this.closeConfirm.isVisible()
                            ).to.be.equal(true, 'Диалоговое окно появилось');
                        },
                    }),
                },
            }
        ),
        'Диалоговое окно.': mergeSuites(
            {
                async beforeEach() {
                    await this.ratingInput.setAverageGrade();
                    await this.expect(await this.productReviewNew.isTextFieldsVisible())
                        .to.be.equal(true, 'Текстовые поля видны');
                    await this.productReviewNew.clickCloseButton();

                    await this.setPageObjects({
                        closeConfirm: () => this.createPageObject(CloseConfirm),
                        gainedExpertise: () => this.createPageObject(GainedExpertise),
                    });
                },
                'При выборе "Сохранить и уйти"': {
                    'появляется поздравительный экран': makeCase({
                        id: 'm-touch-3369',
                        async test() {
                            await this.gainedExpertise.waitForModalVisible(5000);
                            await this.expect(
                                await this.gainedExpertise.isVisible()
                            ).to.be.equal(true, 'Поздравительный экран появился');
                        },
                    }),
                },
                'При выборе "Не сохранять и уйти" после возврата на первый экран': {
                    'показываем поздравительный экран': makeCase({
                        id: 'm-touch-3370',
                        async test() {
                            await this.gainedExpertise.waitForModalVisible(5000);
                            await this.expect(
                                await this.gainedExpertise.isVisible()
                            ).to.be.equal(true, 'Поздравительный экран появился');
                        },
                    }),
                },
            }
        ),
        'Поздравительный экран. У пользователя больше нет заданий.': mergeSuites(
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
                },
                'При нажатии на кнопку "Отлично"': {
                    'экран закрывается': makeCase({
                        id: 'm-touch-3371',
                        async test() {
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.closeButton, 5000)
                            );

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.gainedExpertise.close(),
                                valueGetter: () => this.gainedExpertise.isVisible(),
                            });

                            await this.expect(
                                await this.gainedExpertise.isVisible()
                            ).to.be.equal(false, 'Поздравительный экран закрылся');
                        },
                    }),
                    'происходит редирект на мои отзывы': makeCase({
                        id: 'm-touch-3372',
                        async test() {
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.closeButton, 5000)
                            );
                            await this.browser.yaWaitForChangeUrl(() => this.gainedExpertise.close());

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
                'По умолчанию': {
                    'содержит корректную ссылку "Что означают уровни"': makeCase({
                        id: 'm-touch-3373',
                        async test() {
                            await this.browser.allure.runStep('Ждем появления поздравительного экрана', () =>
                                this.browser.waitForVisible(GainedExpertise.root, 5000)
                            );

                            const actualUrl = await this.gainedExpertise.getAboutHref();
                            const expectedUrl = await this.browser.yaBuildURL('market:my-expertise-info');

                            await this.browser.allure.runStep(
                                'Проверяем корректность ссылки',
                                () => this.expect(actualUrl).to.be.link(
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
