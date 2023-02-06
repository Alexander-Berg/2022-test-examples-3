import {makeCase, makeSuite, mergeSuites} from 'ginny';

import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import Notification from '@self/root/src/components/Notification/__pageObject';

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
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                controls: () => this.createPageObject(Controls),
                notification: () => this.createPageObject(Notification),
            });

            await this.browser.yaWaitForChangeValue({
                action: () => this.userReview.clickControlsButton(),
                valueGetter: () => this.userReview.isControlsPopupVisible(),
            });

            await this.browser.yaWaitForChangeValue({
                action: () => this.controls.clickDeleteButton(),
                valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
            });
        },
        'Диалог удаления отзыва.': {
            'При нажатии на кнопку "Удалить"': {
                'отзыв пропадает со страницы': makeCase({
                    id: 'm-touch-3185',
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
                    id: 'm-touch-3186',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'отображается zero-стейт страницы': makeCase({
                    id: 'm-touch-3187',
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
                    id: 'm-touch-3188',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.notification.isTextVisible(),
                        });

                        return this.notification.waitForText('Отзыв удалён');
                    },
                }),
            },
            'При нажатии на кнопку "Отменить"': {
                'попап удаления закрыватся': makeCase({
                    id: 'm-touch-3189',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCloseButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет отзыва остается': makeCase({
                    id: 'm-touch-3190',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCloseButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.userReview.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет отзыва отображается');
                    },
                }),
            },
            'При нажатии на паранжу': {
                'попап удаления закрыватся': makeCase({
                    id: 'm-touch-3191',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickOutsideContent(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет отзыва остается': makeCase({
                    id: 'm-touch-3192',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickOutsideContent(),
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
