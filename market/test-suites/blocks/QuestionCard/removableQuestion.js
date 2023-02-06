import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionCard} questionCard
 * @param {PageObject.RemovePromptDialog} removePromptDialog
 * @param {PageObject.QuestionList} questionList
 */
export default makeSuite('Блок снипета вопроса, когда вопрос можно удалить', {
    story: {
        'При нажатии на кнопку "Удалить" в диалоге': {
            'вопрос пропадает.': makeCase({
                id: 'm-touch-2283',
                issue: 'MOBMARKET-9117',
                feature: 'Удаление контента',
                async test() {
                    await this.questionCard.clickRemoveButton();
                    await this.removePromptDialog.waitForContentVisible();
                    await this.removePromptDialog.clickSubmitButton();
                    return this.questionList.isQuestionVisible(this.params.questionId)
                        .should.eventually.to.be.equal(false, 'Вопрос больше не отображается');
                },
            }),
        },

        'При нажатии на кнопку "Отменить" в диалоге': {
            'вопрос остаётся.': makeCase({
                id: 'm-touch-2284',
                issue: 'MOBMARKET-9118',
                feature: 'Удаление контента',
                async test() {
                    const urlBefore = await this.browser.getUrl();
                    const questionBefore = await this.questionCard.getQuestionText();
                    await this.questionCard.clickRemoveButton();
                    await this.removePromptDialog.waitForContentVisible();
                    await this.removePromptDialog.clickCloseButton();
                    await this.removePromptDialog.waitForContentHidden();
                    const urlAfter = await this.browser.getUrl();
                    const questionAfter = await this.questionCard.getQuestionText();
                    await this.expect(urlBefore).to.be.equal(
                        urlAfter,
                        'URL не поменялся'
                    );
                    return this.expect(questionBefore).to.be.equal(
                        questionAfter,
                        'Вопрос не изменился'
                    );
                },
            }),
        },

        'При клике вне диалога': {
            'вопрос остаётся.': makeCase({
                id: 'm-touch-2285',
                issue: 'MOBMARKET-9119',
                feature: 'Удаление контента',
                async test() {
                    const urlBefore = await this.browser.getUrl();
                    const questionBefore = await this.questionCard.getQuestionText();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionCard.clickRemoveButton(),
                        valueGetter: () => this.removePromptDialog.isSubmitButtonVisible(),
                    });
                    await this.removePromptDialog.clickOutsideContent();
                    const urlAfter = await this.browser.getUrl();
                    const questionAfter = await this.questionCard.getQuestionText();
                    await this.expect(urlBefore).to.be.equal(
                        urlAfter,
                        'URL не поменялся'
                    );
                    return this.expect(questionBefore).to.be.equal(
                        questionAfter,
                        'Вопрос не изменился'
                    );
                },
            }),
        },
    },
});
