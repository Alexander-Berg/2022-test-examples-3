import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.QuestionAgitations} questionAgitations
 */
export default makeSuite('Виджет агитации оставления ответов. Если количество вопросов помещается на 3 страницы.', {
    params: {
        questionsCount: 'Число вопросов на которые предложено ответить пользователю',
    },
    story: mergeSuites({
        'При клике по кнопке «Показать еще».': {
            'Кнопка «Показать еще» отображается.': makeCase({
                id: 'marketfront-3978',
                async test() {
                    await this.questionAgitations.clickLoadMoreAndWait();
                    return this.questionAgitations.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка «Показать еще» отображается');
                },
            }),
            'Отображается правильное количество вопросов': makeCase({
                id: 'marketfront-3979',
                async test() {
                    const expectedQuestionsCount = 6;
                    await this.questionAgitations.clickLoadMoreAndWait();
                    return this.questionAgitations.getQuestionAgitationsCount()
                        .should.eventually.be.equal(expectedQuestionsCount,
                            'Отображается правильное количество вопросов');
                },
            }),
        },
        'При клике по кнопке «Показать еще» дважды.': {
            'Подгружается все вопросы.': makeCase({
                id: 'marketfront-3980',
                async test() {
                    const expectedQuestionsCount = this.params.questionsCount;
                    await this.questionAgitations.clickLoadMoreAndWait();
                    await this.questionAgitations.clickLoadMoreAndWait();
                    return this.questionAgitations.getQuestionAgitationsCount()
                        .should.eventually.be.equal(expectedQuestionsCount,
                            'Отображается правильное количество вопросов');
                },
            }),
            'Кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-3981',
                async test() {
                    await this.questionAgitations.clickLoadMoreAndWait();
                    await this.questionAgitations.clickLoadMoreAndWait();
                    return this.questionAgitations.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка «Показать еще» не отображается');
                },
            }),
        },
    }),
});
