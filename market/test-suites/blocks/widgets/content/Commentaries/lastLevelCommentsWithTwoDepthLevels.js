import {makeCase, makeSuite} from 'ginny';

import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';

/**
 * @param {PageObject.Toolbar} firstLevelCommentToolbar
 * @param {PageObject.Comment.Snippet} firstLevelCommentSnippet
 * @param {PageObject.Toolbar} secondLevelCommentToolbar
 * @param {PageObject.Comment.Snippet} secondLevelCommentSnippet
 * @param {PageObject.CommentList} firstLevelCommentList
 * @param {PageObject.SmallForm} firstSmallForm
 */
export default makeSuite('Список комментариев, последнего уровня глубиной 2', {
    feature: 'Комментарии',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                controls: () => this.createPageObject(Controls),
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
            });
        },
        'При вводе ответа на комментарий 1го уровня': {
            'появляется кнопка отправить': makeCase({
                id: 'm-touch-2712',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.firstSmallForm.isVisible(),
                    });
                    await this.firstSmallForm.setTextFieldText('Check please!');
                    const secondLevelSendButtonState = await this.firstSmallForm.isSendButtonVisible();
                    return this.expect(secondLevelSendButtonState)
                        .to.be.equal(true, 'В форме отправки комментария 2го уровня кнопка "отправить" отображается');
                },
            }),
        },
        'При удалении комментария 1го уровня': {
            'он заменяется на плейсхолдер': makeCase({
                id: 'm-touch-2722',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelCommentSnippet.moreActionsClick(),
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
                    const deletePlaceholder = await this.firstLevelCommentSnippet.isDeletePlaceholderVisible();
                    return this.expect(deletePlaceholder)
                        .to.equal(true, 'Плейсхолдер удаленного комментария отображается');
                },
            }),
        },
        'При удалении комментария 2го уровня': {
            'комментарий исчезает': makeCase({
                id: 'm-touch-2720',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentList.isVisible(),
                    });
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
                    await this.secondLevelCommentList.waitForListHide();
                },
            }),
        },
        'При удалении комментария 1го уровня и потомков': {
            'ветка с комментариями исчезает': makeCase({
                id: 'm-touch-2721',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelCommentSnippet.moreActionsClick(),
                        valueGetter: () => this.controls.isVisible(),
                    });

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.controls.clickDeleteButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });

                    await this.removePromptDialog.clickSubmitButton();
                    await this.removePromptDialog.waitForContentHidden();
                    await this.firstLevelCommentSnippet.isDeletePlaceholderVisible();

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentList.isVisible(),
                    });

                    await this.secondLevelCommentSnippet.isMoreActionsExist();

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
                    await this.firstLevelCommentList.waitForListHide();
                },
            }),
        },
    },
});
