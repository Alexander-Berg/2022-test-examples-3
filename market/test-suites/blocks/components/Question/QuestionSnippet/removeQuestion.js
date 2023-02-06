import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 * @param {PageObject.Dialog} dialog
 * @param {PageObject.NoQuestions} noQuestions
 */
export default makeSuite('Сниппет вопроса на странице вопроса, диалог удаления', {
    feature: 'Структура страницы',
    story: {
        'По умолчанию': {
            'кнопка «Отменить» диалога удаления закрывает диалог, не удаляя вопрос': makeCase({
                id: 'marketfront-2911',
                issue: 'MARKETVERSTKA-31279',
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
                id: 'marketfront-2912',
                issue: 'MARKETVERSTKA-31276',
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
                id: 'marketfront-2913',
                issue: 'MARKETVERSTKA-31277',
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
        'При подтверждении удаления': {
            'происходит редирект на страницу вопросов': makeCase({
                id: 'marketfront-2910',
                issue: 'MARKETVERSTKA-31278',
                params: {
                    expectedTargetUrl: 'Ожидаемый адрес после удаления вопроса',
                },
                async test() {
                    await this.questionSnippet.clickRemove();
                    await this.dialog.waitForContentVisible();
                    await this.browser.yaWaitForChangeUrl(() => this.dialog.clickSubmitButton());

                    const url = await this.browser.getUrl();
                    await this.expect(url, 'Произошел переход на страница списка вопросов')
                        .to.be.link(this.params.expectedTargetUrl, {
                            skipHostname: true,
                            skipProtocol: true,
                        });

                    return this.noQuestions.waitForVisible();
                },
            }),
        },
    },
});
