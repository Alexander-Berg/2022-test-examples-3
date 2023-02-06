import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.UserQuestions} UserQuestions
 */
export default makeSuite('Список с количеством элементов меньше 10.', {
    params: {
        questionCount: 'Количество вопросов на странице',
    },
    story: {
        'По умолчанию': {
            'Отображается меньше 10 вопросов': makeCase({
                id: 'm-touch-3110',
                issue: 'MARKETFRONT-6439',
                test() {
                    return this.userQuestions.userQuestionsCount
                        .should.eventually.to.be.equal(
                            this.params.questionCount,
                            'Отображается верное количество вопросов'
                        );
                },
            }),
            'Кнопка "Показать еще" скрыта': makeCase({
                id: 'm-touch-3111',
                issue: 'MARKETFRONT-6439',
                test() {
                    return this.userQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(false, 'Кнопка "Показать еще" не видна');
                },
            }),
        },
    },
});
