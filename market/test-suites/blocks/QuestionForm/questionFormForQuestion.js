import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionForm} questionForm
 * @param {PageObject.QuestionCard} questionCard
 */
export default makeSuite('Блок формы вопроса', {
    story: {
        'при создании вопроса': {
            'успешно создается вопрос': makeCase({
                id: 'm-touch-2214',
                issue: 'MOBMARKET-8932',
                feature: 'Добавление контента',
                async test() {
                    await this.questionForm.clickTextField();
                    await this.questionForm.setTextFieldInput('lol');
                    // ждём перехода на новую страницу и проверяем текст вопроса
                    await this.browser.yaWaitForChangeUrl(
                        () => this.questionForm.clickActionButtonWrapper()
                    );
                    await this.questionCard.getQuestionText()
                        .should.eventually.to.be.equal('lol', 'Вопрос создан правильно');
                },
            }),
        },
    },
});
