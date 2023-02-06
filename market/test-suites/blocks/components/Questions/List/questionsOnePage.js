import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ProductQuestions.List | PageObject.QuestionsLayout.QuestionList.List} list
 * @param {PageObject.Paginator} paginator
 * @param {Number} params.questionsCount
 */
export default makeSuite('Список вопросов на одной странице', {
    feature: 'Структура страницы',
    story: {
        'По умолчанию': {
            'количество вопросов в списке равно количеству вопросов, полученных с бекенда': makeCase({
                id: 'marketfront-2820',
                issue: 'MARKETVERSTKA-31039',
                async test() {
                    await this.list
                        .getQuestionsCount()
                        .should.eventually.be.equal(
                            this.params.questionsCount,
                            'Количество вопросов равно количеству, полученному с бекенда'
                        );
                },
            }),
            'пагинация не отображается': makeCase({
                id: 'marketfront-2818',
                issue: 'MARKETVERSTKA-31037',
                async test() {
                    await this.paginator
                        .isVisible()
                        .should.eventually.be.equal(
                            false,
                            'Пагинатора быть не должно'
                        );
                },
            }),
        },
    },
});
