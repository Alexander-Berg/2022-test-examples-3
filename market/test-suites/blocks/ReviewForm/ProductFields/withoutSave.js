import {makeCase, makeSuite, mergeSuites} from 'ginny';

import CloseConfirm from '@self/platform/components/ReviewForm/CloseConfirm/__pageObject';

/**
 * Тест на формы для отзыва на товар
 * @property {PageObject.ProductReviewNew} productReviewNew
 * @property {PageObject.RatingInput} ratingInput
 */
export default makeSuite('Форма отзыва на товар без сохранения', {
    params: {
        productId: 'Id товара',
        slug: 'Слаг товара',
    },
    story: {
        'Диалоговое окно.': mergeSuites(
            {
                async beforeEach() {
                    await this.ratingInput.setAverageGrade();
                    await this.expect(await this.productReviewNew.isTextFieldsVisible())
                        .to.be.equal(true, 'Текстовые поля видны');
                    await this.productReviewNew.setProTextField('Perfect');
                    await this.productReviewNew.clickCloseButton();

                    await this.setPageObjects({
                        closeConfirm: () => this.createPageObject(CloseConfirm),
                    });
                },
                'При выборе "Не сохранять и уйти"': {
                    'проиcходит редирект на страницу отзывов': makeCase({
                        id: 'm-touch-3370',
                        async test() {
                            await this.expect(
                                await this.closeConfirm.isVisible()
                            ).to.be.equal(true, 'Диалоговое окно появилось');

                            await this.browser.yaWaitForChangeUrl(() => this.closeConfirm.clickLeaveButton());

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
