import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ConfirmReviewRemoveDialog from '@self/platform/spec/page-objects/components/ProductReview/Footer/RemoveReviewConfirmDialog';
import Controls from '@self/platform/components/ManageControls/__pageObject';

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
                removePromptDialog: () => this.createPageObject(ConfirmReviewRemoveDialog),
                controls: () => this.createPageObject(Controls),
            });
        },
        'Попап управления отзывом.': {
            'Клик по кнопке "Изменить"': {
                'ведет на редактирования отзыва': makeCase({
                    id: 'marketfront-3931',
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
                    id: 'marketfront-3932',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userReview.clickControlsButton(),
                            valueGetter: () => this.userReview.isControlsPopupVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.controls.clickDeleteButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(true, 'Попап удаления отзыва отображается');
                    },
                }),
            },
        },
    }),
});
