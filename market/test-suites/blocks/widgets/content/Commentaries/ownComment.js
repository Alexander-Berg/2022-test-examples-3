import {makeCase, makeSuite, mergeSuites} from 'ginny';
import EditForm from '@self/platform/spec/page-objects/components/Comment/EditForm';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Notification from '@self/root/src/components/Notification/__pageObject';

/**
 * @param {PageObject.Comment.Snippet} commentSnippet
 */
export default makeSuite('Блок комментария на статью, просматриваемый автором коммента.', {
    story: mergeSuites(
        makeSuite('Диалог удаления', {
            feature: 'Удаление комментария',
            story: {
                async beforeEach() {
                    await this.setPageObjects({
                        controls: () => this.createPageObject(Controls),
                        removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                        notification: () => this.createPageObject(Notification),
                    });
                    await this.controls.isVisible();
                    await this.controls.clickDeleteButton();
                    await this.removePromptDialog.isSubmitButtonVisible();
                },
                'по тапу вне попапа': {
                    'закрывается': makeCase({
                        id: 'm-touch-2301',
                        issue: 'MOBMARKET-10816',
                        async test() {
                            await this.removePromptDialog.clickOutsideContent();
                            await this.removePromptDialog.waitForContentHidden();
                            await this.commentSnippet.isMoreActionsExist()
                                .should.eventually.to.be.equal(true, 'Троеточие отображается на сниппете.');
                        },
                    }),
                },
                'при нажатии кнопки «Отменить»': {
                    'закрывается': makeCase({
                        id: 'm-touch-2300',
                        issue: 'MOBMARKET-10815',
                        async test() {
                            await this.removePromptDialog.clickCloseButton();
                            await this.removePromptDialog.waitForContentHidden();
                            await this.commentSnippet.isMoreActionsExist()
                                .should.eventually.to.be.equal(true, 'Троеточие отображается на сниппете.');
                        },
                    }),
                },
                'при нажатии на кнопку «Удалить»': {
                    'успешно удаляется': makeCase({
                        id: 'm-touch-2299',
                        issue: 'MOBMARKET-10814',
                        async test() {
                            await this.removePromptDialog.clickSubmitButton();
                            await this.removePromptDialog.waitForContentHidden();
                            await this.notification
                                .getText()
                                .should.eventually.be.equal('Комментарий удалён');
                        },
                    }),
                },
            },
        }),
        makeSuite('Редактирование', {
            feature: 'Редактирование комментария без обращения',
            story: {
                async beforeEach() {
                    await this.setPageObjects({
                        controls: () => this.createPageObject(Controls),
                        editForm: () => this.createPageObject(EditForm),
                        notification: () => this.createPageObject(Notification),
                        removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                    });
                    await this.controls.isVisible();
                    await this.controls.clickEditButton();
                    await this.commentSnippet.isVisible()
                        .should.eventually.to.be.equal(false, 'Сниппет больше не отображается');
                    await this.editForm.isVisible()
                        .should.eventually.to.be.equal(true, 'Форма редактирования отображается');
                },
                'Кнопка Изменить.': {
                    'При клике': {
                        'заменяет сниппет на форму редактирования': makeCase({
                            id: 'm-touch-2910',
                            issue: 'MOBMARKET-11926',
                            feature: 'Редактирования контента',
                            async test() {
                                await this.editForm.isCancelButtonVisible()
                                    .should.eventually.to.be.equal(true, 'Кнопка отмены редактирования отображается');
                                await this.editForm.isSendButtonVisible()
                                    .should.eventually.to.be.equal(true, 'Кнопка сохранения комментария отображается');
                            },
                        }),
                    },
                },
                'Кнопка «Х» формы редактирования': {
                    'При клике': {
                        'очищается форма': makeCase({
                            id: 'm-touch-2908',
                            issue: 'MOBMARKET-11926',
                            feature: 'Структура страницы',
                            async test() {
                                await this.editForm.isClearTextFieldButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Кнопка очистки формы редактирования отображается'
                                    );
                                await this.editForm.clickClearTextFieldButton();
                                await this.editForm.getText()
                                    .should.eventually.to.be.equal('', 'форма очистилась');
                            },
                        }),
                    },
                },
                'Кнопка Отменить формы редактирования.': {
                    'При клике': {
                        'заменяет форму редактирования на сниппет': makeCase({
                            id: 'm-touch-2904',
                            issue: 'MOBMARKET-11926',
                            feature: 'Редактирования контента',
                            async test() {
                                const text = await this.editForm.getText();
                                await this.editForm.editText('!');
                                await this.editForm.clickCancelButton();
                                await this.commentSnippet.isVisible()
                                    .should.eventually.to.be.equal(true, 'Сниппет отображается');
                                await this.commentSnippet.getVisibleSnippetText()
                                    .should.eventually.to.equal(text, 'Текст комментария корректный');
                            },
                        }),
                    },
                },
                'Кнопка Отправить формы редактирования.': {
                    'При клике': {
                        'сохраняет комментарий, появляется дата редактирования': makeCase({
                            id: 'm-touch-2907',
                            issue: 'MOBMARKET-11926',
                            feature: 'Редактирования контента',
                            async test() {
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.editForm.clickSendButton(),
                                    valueGetter: () => this.commentSnippet.isVisible(),
                                });
                                await this.commentSnippet.getDateText()
                                    .should.eventually.to.equal(
                                        'Изменён только что',
                                        'Появилась дата изменения комментария'
                                    );
                            },
                        }),
                        'при полностью удаленном тексте из формы, вызывает диалог удаления': {
                            'при подтверждении удаляет комментрий': makeCase({
                                id: 'm-touch-2900',
                                issue: 'MOBMARKET-11926',
                                feature: 'Редактирования контента',
                                async test() {
                                    await this.editForm.clickClearTextFieldButton();
                                    await this.editForm.clickSendButton();
                                    await this.removePromptDialog.waitForContentVisible();
                                    await this.removePromptDialog.clickSubmitButton();
                                    await this.removePromptDialog.waitForContentHidden();
                                    await this.notification
                                        .getText()
                                        .should.eventually.be.equal('Комментарий удалён');
                                    await this.commentSnippet.isVisible()
                                        .should.eventually.to.be.equal(false, 'Сниппет больше не отображается');
                                },
                            }),
                            'при отмене удаления возвращается к форме редактирования комментария': makeCase({
                                id: 'm-touch-2899',
                                issue: 'MOBMARKET-11926',
                                feature: 'Редактирования контента',
                                async test() {
                                    await this.editForm.clickClearTextFieldButton();
                                    await this.editForm.clickSendButton();
                                    await this.removePromptDialog.waitForContentVisible();
                                    await this.removePromptDialog.clickCloseButton();
                                    await this.removePromptDialog.waitForContentHidden();
                                    await this.editForm.isVisible()
                                        .should.eventually.to.be.equal(true, 'Форма редактирования отображается');
                                },
                            }),
                        },
                    },
                },
            },
        })
    ),
});
