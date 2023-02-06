import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.ProductQuestions.List | PageObject.QuestionsLayout.QuestionList.List} list
 * @param {PageObject.Paginator} paginator
 */
export default makeSuite('Список вопросов на нескольких страницах', {
    feature: 'Структура страницы',
    story: {
        'По умолчанию': {
            'количество вопросов в списке равно 10': makeCase({
                id: 'marketfront-2819',
                issue: 'MARKETVERSTKA-31038',
                async test() {
                    await this.list
                        .getQuestionsCount()
                        .should.eventually.be.equal(
                            10,
                            'Количество вопросов равно 10'
                        );
                },
            }),
            'пагинация отображается': makeCase({
                id: 'marketfront-2964',
                issue: 'MARKETVERSTKA-31661',
                async test() {
                    await this.paginator
                        .isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Пагинатор должен быть'
                        );
                },
            }),
        },
    },
});
