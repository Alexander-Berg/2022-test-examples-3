import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.widgets.content.UserAnswers.components.CabinetAnswerSnippet} cabinetAnswerSnippet
 * @param {PageObject.widgets.components.PersonalCabinetZeroState} zeroState
 * @param {PageObject.components.RemovePromptDialog} removePromptDialog
 * @param {PageObject.components.Notification} notification
 */

export default makeSuite('Единственный ответ, который можно удалить.', {
    story: {
        'По умолчанию': {
            'крестик для удаления ответа отображается': makeCase({
                id: 'm-touch-3137',
                async test() {
                    return this.cabinetAnswerSnippet.isRemoveButtonVisible()
                        .should.eventually.be.equal(true, 'Крестик для удаления ответа отображается');
                },
            }),
        },
        'При клике на крестик': {
            'попап удаления ответа появляется': makeCase({
                id: 'm-touch-3138',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                        valueGetter: () => this.removePromptDialog.isVisible(),
                    });

                    return this.removePromptDialog.isVisible()
                        .should.eventually.be.equal(true, 'Попап удаления ответа отображается');
                },
            }),
        },
        'Диалог удаления ответа.': {
            'При нажатии на кнопку "Удалить"': {
                'ответ пропадает со страницы': makeCase({
                    id: 'm-touch-3139',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.cabinetAnswerSnippet.isVisible(),
                        });

                        return this.cabinetAnswerSnippet.isVisible()
                            .should.eventually.be.equal(false, 'Сниппет ответа не отображается');
                    },
                }),
                'попап удаления закрывается': makeCase({
                    id: 'm-touch-3140',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'отображается zero-стейт страницы': makeCase({
                    id: 'm-touch-3141',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.zeroState.isVisible(),
                        });

                        return this.zeroState.isVisible()
                            .should.eventually.be.equal(true, 'Zero стейт страницы отображается');
                    },
                }),
                'отображается нотификация об успешном удалении ответа': makeCase({
                    id: 'm-touch-3142',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickSubmitButton(),
                            valueGetter: () => this.notification.isTextVisible(),
                        });

                        return this.notification.waitForText('Ответ удалён');
                    },
                }),
            },
            'При нажатии на кнопку "Отменить"': {
                'попап удаления закрыватся': makeCase({
                    id: 'm-touch-3143',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCloseButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет ответа остается': makeCase({
                    id: 'm-touch-3144',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickCloseButton(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.cabinetAnswerSnippet.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет ответа отображается');
                    },
                }),
            },
            'При нажатии на паранжу': {
                'попап удаления закрыватся': makeCase({
                    id: 'm-touch-3145',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickOutsideContent(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.removePromptDialog.isVisible()
                            .should.eventually.be.equal(false, 'Попап удаления закрылся');
                    },
                }),
                'сниппет ответа остается': makeCase({
                    id: 'm-touch-3146',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.cabinetAnswerSnippet.removeAnswerButtonClick(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.removePromptDialog.clickOutsideContent(),
                            valueGetter: () => this.removePromptDialog.isVisible(),
                        });

                        return this.cabinetAnswerSnippet.isVisible()
                            .should.eventually.be.equal(true, 'Сниппет ответа отображается');
                    },
                }),
            },
        },
    },
});
