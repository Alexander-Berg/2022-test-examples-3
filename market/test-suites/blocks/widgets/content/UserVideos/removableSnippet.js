import {makeCase, makeSuite, mergeSuites} from 'ginny';

import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import Notification from '@self/root/src/components/Notification/__pageObject';

/**
 * @param {PageObject.widgets.content.UserVideo} userVideo
 * @param {PageObject.widgets.content.UserVideos} userVideos
 */

export default makeSuite('Единственное видео, которое можно удалить.', {
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                controls: () => this.createPageObject(Controls),
                notification: () => this.createPageObject(Notification),
            });

            await this.browser.yaWaitForChangeValue({
                action: () => this.userVideo.clickControlsButton(),
                valueGetter: () => this.userVideo.isControlsPopupVisible(),
            });

            await this.browser.yaWaitForChangeValue({
                action: () => this.controls.clickDeleteButton(),
                valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
            });
        },
        'Диалог удаления видео.': {
            'При нажатии на кнопку "Удалить"': {
                'видео пропадает со страницы': makeCase({
                    id: 'm-touch-3299',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.userVideo.isVisible(),
                        });

                        return this.userVideo.isVisible()
                            .should.eventually.be.equal(false, 'Сниппет видео не отображается');
                    },
                }),
                'попап удаления закрывается': makeCase({
                    id: 'm-touch-3300',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.userVideo.isControlsPopupVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'отображается zero-стейт страницы': makeCase({
                    id: 'm-touch-3301',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.userVideos.isZeroStateVisible(),
                        });

                        return this.userVideos.isZeroStateVisible()
                            .should.eventually.be.equal(true, 'Zero стейт страницы отображается');
                    },
                }),
                'отображается нотификация об успешном удалении видео': makeCase({
                    id: 'm-touch-3302',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.notification.isTextVisible(),
                        });

                        return this.notification.getText()
                            .should.eventually.be.equal('Видео удалено', 'Текст нотификации верный');
                    },
                }),
            },
            'При нажатии на кнопку "Отменить"': {
                'попап удаления закрыватся': makeCase({
                    id: 'm-touch-3303',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCloseButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет видео остается': makeCase({
                    id: 'm-touch-3304',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCloseButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.userVideo.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет видео отображается');
                    },
                }),
            },
            'При нажатии на паранжу': {
                'попап удаления закрыватся': makeCase({
                    id: 'm-touch-3305',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickOutsideContent(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет видео остается': makeCase({
                    id: 'm-touch-3306',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickOutsideContent(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.userVideo.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет видео отображается');
                    },
                }),
            },
        },
    }),
});
