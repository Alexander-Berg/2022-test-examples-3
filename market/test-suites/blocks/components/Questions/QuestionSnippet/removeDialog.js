import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 * @param {PageObject.Dialog} dialog
 */
export default makeSuite('Сниппет вопроса, диалог удаления', {
    feature: 'Структура страницы',
    story: {
        'По умолчанию': {
            'кнопка «Отменить» диалога удаления закрывает диалог, не удаляя вопрос': makeCase({
                id: 'marketfront-2855',
                issue: 'MARKETVERSTKA-31061',
                async test() {
                    await this.questionSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickCancelButton();
                    await this.dialog.waitForContentHidden();
                    const isQuestionSnippetExists = await this.questionSnippet.isExisting();
                    await this.expect(isQuestionSnippetExists).to.be.equal(true, 'Вопрос не удалён');
                },
            }),
            '«крестик» окна диалога удаления закрывает диалог, не удаляя вопрос': makeCase({
                id: 'marketfront-2858',
                issue: 'MARKETVERSTKA-31062',
                async test() {
                    await this.questionSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickCloseButton();
                    await this.dialog.waitForContentHidden();
                    const isQuestionSnippetExists = await this.questionSnippet.isExisting();
                    await this.expect(isQuestionSnippetExists).to.be.equal(true, 'Вопрос не удалён');
                },
            }),
            'клик вне окна диалога удаления закрывает диалог, не удаляя вопрос': makeCase({
                id: 'marketfront-2859',
                issue: 'MARKETVERSTKA-31063',
                async test() {
                    await this.questionSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.dialog.clickOutsideContent();
                    await this.dialog.waitForContentHidden();
                    const isQuestionSnippetExists = await this.questionSnippet.isExisting();
                    await this.expect(isQuestionSnippetExists).to.be.equal(true, 'Вопрос не удалён');
                },
            }),
        },
    },
});
