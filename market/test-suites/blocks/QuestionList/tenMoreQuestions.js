import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionList} questionList
 */
export default makeSuite('Если вопросов больше 10.', {
    story: {
        'Отображается кнопка "Показать еще"': makeCase({
            id: 'm-touch-2180',
            issue: 'MOBMARKET-8781',
            test() {
                return this.questionList.isLoadMoreButtonShown()
                    .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" видимая');
            },
        }),
        'При загрузке': {
            'Отображается 10 вопросов': makeCase({
                id: 'm-touch-2208',
                issue: 'MOBMARKET-8807',
                test() {
                    return this.questionList.productQuestionCount
                        .should.eventually.to.be.equal(10, 'На странице 10 вопросов');
                },
            }),
        },
        'При клике "Показать еще"': {
            'Загружается дополнительная порция вопросов': makeCase({
                id: 'm-touch-2209',
                issue: 'MOBMARKET-8842',
                test() {
                    return this.questionList.productQuestionCount
                        .then(
                            questionsBefore =>
                                this.browser.yaWaitForChangeValue({
                                    action: () => this.questionList.loadMoreQuestions(),
                                    valueGetter: () => this.questionList.productQuestionCount,
                                })
                                    .then(() => this.questionList.productQuestionCount)
                                    .should.eventually.to.be.greaterThan(
                                        questionsBefore,
                                        'Вопросов на товар на странице стало больше'
                                    )
                        );
                },
            }),
        },
    },
});
