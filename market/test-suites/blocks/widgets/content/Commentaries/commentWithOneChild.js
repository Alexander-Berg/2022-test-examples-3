import {makeSuite, makeCase} from 'ginny';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';

/**
 * @param {PageObject.Toolbar} firstLevelToolbar
 * @param {PageObject.CommentList} secondLevelCommentList
 * @param {PageObject.SmallForm} smallForm
 * @param {PageObject.Toolbar} secondLevelCommentToolbar
 */
export default makeSuite('Список комментариев с 1 потомком', {
    feature: 'Комментарии к статьям',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                controls: () => this.createPageObject(Controls),
                removePromptDialog: () => this.createPageObject(RemovePromptDialog),
            });
        },
        'Блок раскрывания': {
            'по умолчанию': {
                'отображает верное количество потомков': makeCase({
                    id: 'm-touch-2729',
                    issue: 'MOBMARKET-11795',
                    async test() {
                        const expandText = await this.firstLevelToolbar.getExpandChildrenText();
                        return this.expect(expandText)
                            .to
                            .equal('1 комментарий', 'Отображается верный текст у кнопки раскрытия потомков');
                    },
                }),
            },
            'при раскрытии': {
                'отображается верное количество сниппетов': makeCase({
                    id: 'm-touch-2728',
                    issue: 'MOBMARKET-11795',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.firstLevelToolbar.clickExpandChildren(),
                            valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                        });
                        const snippetCount = await this.secondLevelCommentList.getCommentSnippets();
                        return this.expect(snippetCount)
                            .to
                            .equal(1, 'Отобразилось верное количество снипетов комментариев');
                    },
                }),
                'отображается форма оставления комментария второго уровня': makeCase({
                    id: 'm-touch-2725',
                    issue: 'MOBMARKET-11795',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.firstLevelToolbar.clickExpandChildren(),
                            valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                        });
                        const secondLevelForm = await this.smallForm.isVisible();
                        return this.expect(secondLevelForm)
                            .to
                            .equal(true, 'Форма оставления комментария 2го уровня отображается');
                    },
                }),
                'форма оставления комментария второго уровня содержит обращение': makeCase({
                    id: 'm-touch-2780',
                    issue: 'MOBMARKET-11924',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.firstLevelToolbar.clickExpandChildren(),
                            valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                        });
                        const text = await this.smallForm.getText();
                        return this.expect(text)
                            .to
                            .equal('Vasya P., ', 'Форма оставления комментария 2го уровня содержит обращение');
                    },
                }),
            },
            'при скрытии после раскрытия': {
                'скрывает потомков': makeCase({
                    id: 'm-touch-2727',
                    issue: 'MOBMARKET-11795',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.firstLevelToolbar.clickExpandChildren(),
                            valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                        });
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.firstLevelToolbar.clickExpandChildren(),
                            valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                        });
                        const snippetCount = await this.secondLevelCommentList.getCommentSnippets();
                        await this.expect(snippetCount)
                            .to
                            .equal(0, 'потомки не отображаются');
                        const expandText = await this.firstLevelToolbar.getExpandChildrenText();
                        return this.expect(expandText)
                            .to.equal('1 комментарий', 'Отображается верный текст у кнопки раскрытия потомков');
                    },
                }),
            },
        },
        'При удалении комментария 1го уровня': {
            'комментарий 1го уровня заменяется на плейсхолдер': makeCase({
                id: 'm-touch-2716',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.moreActionsClick(),
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
            'комментарий 2го уровня исчезает': makeCase({
                id: 'm-touch-2723',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                    });
                    await this.secondLevelCommentToolbar.moreActionsClick();
                    await this.controls.clickDeleteButton();
                    await this.removePromptDialog.waitForContentVisible();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.removePromptDialog.clickSubmitButton(),
                        valueGetter: () => this.removePromptDialog.isVisible(),
                    });
                    const secondLevelSnippetsCount = await this.secondLevelCommentList.getCommentSnippets();
                    return this.expect(secondLevelSnippetsCount)
                        .to.be.equal(0, 'Комментарий второго уровня не отображается');
                },
            }),
        },
        'При удалении комментария 1го уровня, а потом потомка': {
            'оба комментария исчезают': makeCase({
                id: 'm-touch-2715',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.moreActionsClick(),
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
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.moreActionsClick(),
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
                    const firstLevelSnippet = this.firstLevelCommentSnippet.isVisible();
                    return this.expect(firstLevelSnippet)
                        .to.be.equal(false, 'Сниппет больше не отображается');
                },
            }),
        },
        'При вводе ответа на комментарий 1го уровня': {
            'появляется кнопка отправить': makeCase({
                id: 'm-touch-2714',
                issue: 'MOBMARKET-11795',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentList.getCommentSnippets(),
                    });
                    await this.smallForm.setTextFieldText('check please!');
                    const sendButtonState = await this.smallForm.isSendButtonVisible();
                    return this.expect(sendButtonState)
                        .to.equal(true, 'Кнопка отправки отображается');
                },
            }),
        },
    },
});
