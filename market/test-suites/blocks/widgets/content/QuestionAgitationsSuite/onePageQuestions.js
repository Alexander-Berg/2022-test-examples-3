import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.QuestionAgitations} questionAgitations
 */
export default makeSuite('Виджет агитации оставления ответов.' +
    ' Если количество вопросов помещается на одну страницу.', {
    params: {
        questionsCount: 'Число вопросов на которые предложено ответить пользователю',
    },
    story: mergeSuites({
        'По умолчанию': {
            'Отображается виджет с вопросами других пользователей': makeCase({
                id: 'marketfront-3975',
                test() {
                    return this.questionAgitations.isVisible()
                        .should.eventually.be.equal(true, 'Блок с вопросами других пользователей отображается');
                },
            }),
            'Кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-3976',
                test() {
                    return this.questionAgitations.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка «Показать еще» не отображается');
                },
            }),
            'Отображается правильное количество вопросов': makeCase({
                id: 'marketfront-3977',
                test() {
                    const expectedQuestionsCount = this.params.questionsCount;
                    return this.questionAgitations.getQuestionAgitationsCount()
                        .should.eventually.be.equal(expectedQuestionsCount,
                            'Отображается правильное количество вопросов');
                },
            }),
        },
    }),
});
