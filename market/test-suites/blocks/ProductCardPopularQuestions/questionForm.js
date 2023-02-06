import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionForm} questionForm
 * @param {PageObject.QuestionCard} questionCard
 */
export default makeSuite('Форма оставления вопроса на товар.', {
    story: {
        'при создании вопроса': {
            'вопрос успешно создается': makeCase({
                id: 'marketfront-3713',
                issue: 'MARKETFRONT-362',
                feature: 'Популярные вопросы',
                async test() {
                    const questionToAsk = 'to be or not ot be';
                    await this.questionForm.clickTextField();
                    await this.questionForm.setTextFieldInput(questionToAsk);
                    await this.questionForm.clickActionButton();
                    // ожидание перехода на страницу вопроса и получение текста созданного вопроса
                    await this.questionCard.getQuestionText()
                        .should.eventually.to.be.equal(questionToAsk, 'Вопрос создан правильно');
                },
            }),
        },
    },
});
