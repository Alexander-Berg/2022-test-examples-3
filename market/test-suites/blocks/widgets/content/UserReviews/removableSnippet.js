import {makeCase, makeSuite, mergeSuites} from 'ginny';

import ConfirmReviewRemoveDialog from '@self/platform/spec/page-objects/components/ProductReview/Footer/RemoveReviewConfirmDialog';
import Controls from '@self/platform/components/ManageControls/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';
import ModalFloat from '@self/platform/spec/page-objects/components/ModalFloat';

/**
 * @param {PageObject.widgets.content.UserReview} userReview
 * @param {PageObject.widgets.content.UserReviews} userReviews
 */

export default makeSuite('Единственный отзыв, который можно удалить.', {
    params: {
        reviewsCount: 'Количество отображаемых отзывов пользователя',
    },
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                removePromptDialog: () => this.createPageObject(ConfirmReviewRemoveDialog),
                modalFloat: () => this.createPageObject(ModalFloat),
                controls: () => this.createPageObject(Controls),
                notification: () => this.createPageObject(Notification),
            });

            await this.browser.yaWaitForChangeValue({
                action: () => this.userReview.clickControlsButton(),
                valueGetter: () => this.userReview.isControlsPopupVisible(),
            });

            await this.browser.yaWaitForChangeValue({
                action: () => this.controls.clickDeleteButton(),
                valueGetter: () => this.removePromptDialog.isVisible(),
            });
        },
        'Диалог удаления отзыва.': {
            'При нажатии на кнопку "Удалить"': {
                'отзыв пропадает со страницы': makeCase({
                    id: 'marketfront-3920',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.userReview.isVisible(),
                        });

                        return this.userReview.isVisible()
                            .should.eventually.be.equal(false, 'Сниппет отзыва не отображается');
                    },
                }),
                'попап удаления закрывается': makeCase({
                    id: 'marketfront-3921',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.userReview.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'отображается zero-стейт страницы': makeCase({
                    id: 'marketfront-3922',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.userReviews.isZeroStateVisible(),
                        });

                        return this.userReviews.isZeroStateVisible()
                            .should.eventually.be.equal(true, 'Zero стейт страницы отображается');
                    },
                }),
                'отображается нотификация об успешном удалении отзыва': makeCase({
                    id: 'marketfront-3923',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.userReview.isVisible(),
                        });

                        return this.notification.waitForText('Отзыв удалён');
                    },
                }),
            },
            'При нажатии на кнопку "Отменить"': {
                'попап удаления закрыватся': makeCase({
                    id: 'marketfront-3924',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCancelButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет отзыва остается': makeCase({
                    id: 'marketfront-3925',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCancelButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.userReview.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет отзыва отображается');
                    },
                }),
            },
            'При нажатии на паранжу': {
                'попап удаления закрыватся': makeCase({
                    id: 'marketfront-3926',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.modalFloat.closeOnParanja(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет отзыва остается': makeCase({
                    id: 'marketfront-3927',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.modalFloat.closeOnParanja(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.userReview.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет отзыва отображается');
                    },
                }),
            },
        },
    }),
});
