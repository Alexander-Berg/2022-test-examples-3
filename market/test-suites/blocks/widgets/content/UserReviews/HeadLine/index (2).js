import {makeCase, makeSuite, mergeSuites} from 'ginny';

import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';

/**
 * @param {components.UserReview.ShopHeadline | components.UserReview.ProductHeadline} headline
 * @param {PageObject.widgets.content.UserReview} userReview
 */

export default makeSuite('Шапка сниппета отзыва.', {
    params: {
        expectedModifyReviewLink: 'Ожидаемая ссылка на изменение отзыва',
        expectedReviewTypeLink: 'Ссылка на товар или магазин, на который оставлен отзыв',
    },
    story: mergeSuites({
        async beforeEach() {
            this.setPageObjects({
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                controls: () => this.createPageObject(Controls),
            });
        },
        'Кнопка управления отзывом (троеточие).': {
            'Клик по кнопке': {
                'открывает попап управления отзывом': makeCase({
                    id: 'm-touch-3197',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userReview.clickControlsButton(),
                            valueGetter: () => this.userReview.isControlsPopupVisible(),
                        });

                        return this.userReview.isControlsPopupVisible()
                            .should.eventually.be.equal(true, 'Попап управления отзывом отображается');
                    },

                }),
            },
        },
        'Попап управления отзывом.': {
            'Клик по кнопке "Изменить"': {
                'ведет на редактирования отзыва': makeCase({
                    id: 'm-touch-3198',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userReview.clickControlsButton(),
                            valueGetter: () => this.userReview.isControlsPopupVisible(),
                        });

                        await this.controls.clickEditButton();

                        const expectedUrl = this.params.expectedModifyReviewLink;
                        const actualUrl = await this.browser.getUrl();

                        return this.expect(actualUrl, 'Ссылка на изменение отзыва корректная корректная')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
            'Клик по кнопке "Удалить"': {
                'открывает попап удаления отзыва': makeCase({
                    id: 'm-touch-3199',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userReview.clickControlsButton(),
                            valueGetter: () => this.userReview.isControlsPopupVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.controls.clickDeleteButton(),
                            valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                        });

                        return this.removePromptDialog.isSubmitButtonVisible()
                            .should.eventually.be.equal(true, 'Попап удаления отзыва отображается');
                    },
                }),
            },
        },
    }),
});
