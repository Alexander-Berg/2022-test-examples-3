import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.QuestionAgitations} questionAgitations
 */
export default makeSuite('Виджет агитации оставления ответов. Если количество вопросов помещается на 2 страницы.', {
    params: {
        questionsCount: 'Число вопросов на которые предложено ответить пользователю',
    },
    story: mergeSuites({
        'По умолчанию.': {
            'Кнопка «Показать еще» отображается.': makeCase({
                id: 'marketfront-3982',
                test() {
                    return this.questionAgitations.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка «Показать еще» отображается');
                },
            }),
            'Отображается правильное количество вопросов': makeCase({
                id: 'marketfront-3983',
                test() {
                    const expectedQuestionsCount = 3;
                    return this.questionAgitations.getQuestionAgitationsCount()
                        .should.eventually.be.equal(expectedQuestionsCount,
                            'Отображается правильное количество вопросов');
                },
            }),
        },
        'При клике по кнопке «Показать еще».': {
            'Подгружается еще одна страница.': makeCase({
                id: 'marketfront-3984',
                async test() {
                    const expectedQuestionsCount = this.params.questionsCount;
                    await this.questionAgitations.clickLoadMoreAndWait();
                    return this.questionAgitations.getQuestionAgitationsCount()
                        .should.eventually.be.equal(expectedQuestionsCount,
                            'Отображается правильное количество вопросов');
                },
            }),
            'Кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-3985',
                async test() {
                    await this.questionAgitations.clickLoadMoreAndWait();
                    return this.questionAgitations.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка «Показать еще» не отображается');
                },
            }),
        },
    }),
});
