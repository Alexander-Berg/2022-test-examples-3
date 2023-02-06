import {makeSuite, makeCase} from 'ginny';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';

/**
 * @param {PageObject.Toolbar} secondLevelCommentToolbar
 * @param {PageObject.Toolbar} thirdLevelCommentToolbar
 * @param {PageObject.CommentList} secondLevelCommentList
 * @param {PageObject.Comment.Snippet} secondLevelCommentSnippet
 * @param {PageObject.SmallForm} secondSmallForm
 */
export default makeSuite('Список комментариев, глубиной 3 уровня', {
    feature: 'Комментарии к статьям',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                controls: () => this.createPageObject(Controls),
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
            });
        },
        'При вводе ответа на комментарий 2го уровня': {
            'появляется кнопка отправить': makeCase({
                id: 'm-touch-2712',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondSmallForm.isVisible(),
                    });
                    await this.secondSmallForm.setTextFieldText('Check please!');
                    const thirdLevelSendButtonState = await this.secondSmallForm.isSendButtonVisible();
                    return this.expect(thirdLevelSendButtonState)
                        .to.be.equal(true, 'В форме отправки комментария 3го уровня кнопка "отправить" отображается');
                },
            }),
        },
        'В блоке раскрывания на втором уровне': {
            'по умолчанию': {
                'отображается корректное число потомков': makeCase({
                    id: 'm-touch-2710',
                    issue: 'MOBMARKET-11795',
                    async test() {
                        const expandText = await this.secondLevelCommentToolbar.getExpandChildrenText();
                        return this.expect(expandText)
                            .to.equal('1 комментарий', 'Отображается верный текст у кнопки раскрытия потомков');
                    },
                }),
            },
        },
        'При удалении комментария 2го уровня': {
            'он заменяется на плейсхолдер': makeCase({
                id: 'm-touch-2722',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.removePromptDialog.clickSubmitButton(),
                        valueGetter: () => this.removePromptDialog.isVisible(),
                    });
                    const deletePlaceholder = await this.secondLevelCommentSnippet.isDeletePlaceholderVisible();
                    return this.expect(deletePlaceholder)
                        .to.equal(true, 'Плейсхолдер удаленного комментария отображается');
                },
            }),
        },
        'При удаленном комментарии 2го уровня': {
            'форма отправки комментария отображается': makeCase({
                id: 'm-touch-2719',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.removePromptDialog.clickSubmitButton(),
                        valueGetter: () => this.removePromptDialog.isVisible().catch(() => null),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondSmallForm.isVisible(),
                    });

                    const thirdLevelFormState = await this.secondSmallForm.isVisible();
                    return this.expect(thirdLevelFormState)
                        .to.equal(true, 'Форма третьего уровня отображается');
                },
            }),
            'форма отправки комментария не содержит обращения': makeCase({
                id: 'm-touch-2777',
                issue: 'MOBMARKET-11924',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.removePromptDialog.clickSubmitButton(),
                        valueGetter: () => this.removePromptDialog.isVisible().catch(() => null),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondSmallForm.isVisible(),
                    });

                    const text = await this.secondSmallForm.getText();
                    return this.expect(text)
                        .to.equal('', 'Форма не содержит обращения');
                },
            }),
        },
        'При отправке ответа на комментарий 2го уровня': {
            'он отображается в списке': makeCase({
                id: 'm-touch-2711',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondSmallForm.isVisible(),
                    });
                    await this.secondSmallForm.setTextFieldText('check please!');
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondSmallForm.clickSendButton(),
                        valueGetter: () => this.thirdLevelCommentList.getCommentSnippets(),
                    });

                    const [, newComment] = await this.thirdLevelCommentList.getCommentsText();

                    return this.expect(newComment)
                        .to.equal('check please!', 'Новый комментарий отобразился');
                },
            }),
        },
        'При отправке ответа на комментарий с обращением 2го уровня': {
            'он отображается в списке с обращением': makeCase({
                id: 'm-touch-2774',
                issue: 'MOBMARKET-11924',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondSmallForm.isVisible(),
                    });
                    const name = await this.secondSmallForm.getText();
                    const fullMessage = `${name}check please!`;
                    await this.secondSmallForm.setTextFieldText(fullMessage);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondSmallForm.clickSendButton(),
                        valueGetter: () => this.thirdLevelCommentList.getCommentSnippets(),
                    });

                    const [, newComment] = await this.thirdLevelCommentList.getCommentsText();

                    return this.expect(newComment)
                        .to.equal(fullMessage, 'Новый комментарий отобразился');
                },
            }),
        },
        'При удалении комментария 3го уровня': {
            'комментарий исчезает': makeCase({
                id: 'm-touch-2720',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.thirdLevelCommentList.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.thirdLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });
                    await this.removePromptDialog.clickSubmitButton();
                    await this.removePromptDialog.waitForContentHidden();
                    await this.thirdLevelCommentList.waitForListHide();
                },
            }),
        },
        'При удалении комментария 2го уровня и потомков': {
            'ветка с комментариями исчезает': makeCase({
                id: 'm-touch-2721',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });

                    await this.removePromptDialog.clickSubmitButton();
                    await this.removePromptDialog.waitForContentHidden();
                    await this.secondLevelCommentSnippet.isDeletePlaceholderVisible();

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.thirdLevelCommentList.isVisible(),
                    });

                    await this.thirdLevelCommentSnippet.isMoreActionsExist();

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.thirdLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });
                    await this.removePromptDialog.clickSubmitButton();
                    await this.removePromptDialog.waitForContentHidden();
                    await this.secondLevelCommentList.waitForListHide();
                },
            }),
        },
    },
});
