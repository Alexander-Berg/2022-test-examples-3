import {makeSuite, makeCase} from 'ginny';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import CommentSnippet from '@self/platform/spec/page-objects/components/Comment/Snippet';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import EditForm from '@self/platform/spec/page-objects/components/Comment/EditForm';

/**
 * Тесты на компонент widgets/content/Commentaries/form
 *
 * @param {PageObject.BigForm} bigForm
 * @param {PageObject.CommentList} commentList
 */
export default makeSuite('Форма оставления комментария. Для авторизованного пользователя', {
    feature: 'Комментарии к статьям',
    story: {
        'При вводе первого символа': {
            'появляется кнопка "отправить"': makeCase({
                id: 'm-touch-2497',
                issue: 'MOBMARKET-10698',
                async test() {
                    await this.bigForm.fillTextField('a');
                    return this.expect(this.bigForm.isSendButtonVisible())
                        .to.equal(true, 'Кнопка "отправить" отображается');
                },
            }),
        },
        'При превышении лимита символов': {
            'появляется дисклеймер "слишком длинный текст"': makeCase({
                id: 'm-touch-2502',
                issue: 'MOBMARKET-10703',
                async test() {
                    await this.bigForm.fillTextField('a'.repeat(2001));
                    await this.bigForm.waitForError('Слишком длинный комментарий', 'Текст дисклеймера верный');
                },
            }),
        },
        'При вводе 1000 символов': {
            'появляется счетчик символов': makeCase({
                id: 'm-touch-2498',
                issue: 'MOBMARKET-10699',
                async test() {
                    await this.bigForm.fillTextField('a'.repeat(1000));
                    return this.expect(this.bigForm.isSymbolCounterVisible())
                        .to.equal(true, 'Счетчик символов появился');
                },
            }),
        },
        'При клике на крестик': {
            'форма очищается': makeCase({
                id: 'm-touch-2501',
                issue: 'MOBMARKET-10702',
                async test() {
                    await this.bigForm.fillTextField('a'.repeat(1002));
                    await this.bigForm.clickClean();
                    return this.expect(await this.bigForm.getFormText())
                        .to.equal('', 'Форма ввода пустая');
                },
            }),
        },
        'При создании комментария': {
            'форма очищается': makeCase({
                id: 'm-touch-2503',
                issue: 'MOBMARKET-10704',
                async test() {
                    await this.bigForm.fillTextField(Math.random().toString(2));
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.bigForm.clickSendButton(),
                        valueGetter: () => this.bigForm.getFormText(),
                    });
                    return this.expect(await this.bigForm.getFormText())
                        .to.equal('', 'Форма ввода пустая');
                },
            }),
            'новый комментарий появляется в списке': makeCase({
                id: 'm-touch-2504',
                issue: 'MOBMARKET-10705',
                async test() {
                    await this.bigForm.fillTextField(Math.random().toString(2));
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.bigForm.clickSendButton(),
                        valueGetter: () => this.commentList.getCommentSnippets(),
                    });
                    return this.expect(this.commentList.getCommentSnippets())
                        .to.equal(1, 'Комментарий появился в ленте');
                },
            }),
            'и редактирования нового комментария':
                makeSuite('комментарий удаляется', {
                    story: {
                        async beforeEach() {
                            await this.setPageObjects({
                                commentSnippet: () => this.createPageObject(CommentSnippet, {parent: this.commentList}),
                                controls: () => this.createPageObject(Controls),
                                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
                                editForm: () => this.createPageObject(EditForm),
                            });
                            await this.bigForm.fillTextField(Math.random().toString(2));
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.bigForm.clickSendButton(),
                                valueGetter: () => this.commentList.isVisible(),
                            });
                            await this.expect(this.commentList.getCommentSnippets())
                                .to.equal(1, 'Комментарий появился в ленте');
                            await this.commentSnippet.isMoreActionsExist();
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.commentSnippet.moreActionsClick(),
                                valueGetter: () => this.controls.isVisible(),
                            });
                            await this.controls.clickEditButton();
                            await this.editForm.isVisible()
                                .should.eventually.to.be.equal(true, 'Форма редактирования комментария появилась');
                        },
                        'при подтверждении сохранения пустого текста': makeCase({
                            id: 'm-touch-2941',
                            issue: 'MOBMARKET-12937',
                            async test() {
                                await this.editForm.isClearTextFieldButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Кнопка очистки формы редактирования отображается'
                                    );
                                await this.editForm.clickClearTextFieldButton();
                                await this.editForm.getText().should.eventually.to.be.equal('', 'Форма очистилась');
                                await this.editForm.clickSendButton();
                                await this.removePromptDialog.waitForContentVisible();
                                await this.removePromptDialog.clickSubmitButton();
                                await this.removePromptDialog.waitForContentHidden();
                                await this.notification.waitForText('Комментарий удалён');
                                await this.commentList.isVisible()
                                    .should.eventually.to.be.equal(false, 'Список комментариев больше не отображается');
                            },
                        }),
                        'при сохранении и последущем удалении комментария': makeCase({
                            id: 'm-touch-2942',
                            issue: 'MOBMARKET-12937',
                            async test() {
                                await this.editForm.setText('aaaaaaaaaaa');
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.editForm.clickSendButton(),
                                    valueGetter: () => this.commentSnippet.isVisible(),
                                });
                                await this.commentSnippet.isMoreActionsExist();
                                await this.browser.yaWaitForChangeValue({
                                    action: () => this.commentSnippet.moreActionsClick(),
                                    valueGetter: () => this.controls.isVisible(),
                                });
                                await this.controls.clickDeleteButton();
                                await this.removePromptDialog.waitForContentVisible();
                                await this.removePromptDialog.clickSubmitButton();
                                await this.removePromptDialog.waitForContentHidden();
                                await this.notification.waitForText('Комментарий удалён');
                                await this.commentList.isVisible()
                                    .should.eventually.to.be.equal(false, 'Список комментариев больше не отображается');
                            },
                        }),
                    },
                }),
        },
        'При создании 2го комментария подряд c одинаковым текстом': {
            'появляется ошибка': makeCase({
                id: 'm-touch-2505',
                issue: 'MOBMARKET-10706',
                async test() {
                    await this.bigForm.fillTextField('lol');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.bigForm.clickSendButton(),
                        valueGetter: () => this.commentList.getCommentSnippets(),
                    });
                    await this.bigForm.fillTextField('lol');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.bigForm.clickSendButton(),
                        valueGetter: () => this.notification.getText(),
                    });
                    await this.notification.getText()
                        .should.eventually.be.equal('Ваш комментарий уже принят');
                },
            }),
        },
    },
});
