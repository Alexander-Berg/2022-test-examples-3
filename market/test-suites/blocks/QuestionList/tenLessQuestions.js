import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionList} questionList
 */
export default makeSuite('Если вопросов меньше 10.', {
    params: {
        questionCount: 'Количество вопросов на странице',
    },
    story: {
        'То отображается верное количество вопросов': makeCase({
            id: 'm-touch-2207',
            issue: 'MOBMARKET-9005',
            test() {
                return this.questionList.productQuestionCount
                    .should.eventually.to.be.equal(
                        this.params.questionCount,
                        'Отображается верное количество вопросов'
                    );
            },
        }),
        'Кнопка "Показать еще" не отображается': makeCase({
            id: 'm-touch-2181',
            issue: 'MOBMARKET-8787',
            test() {
                return this.questionList.isLoadMoreButtonShown()
                    .should.eventually.to.be.equal(false, 'Кнопка "Показать еще" невидимая');
            },
        }),
    },
});
