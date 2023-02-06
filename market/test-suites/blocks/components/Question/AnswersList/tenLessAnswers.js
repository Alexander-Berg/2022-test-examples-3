import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.AnswersList} answersList
 * @param {PageObject.Paginator} paginator
 */
export default makeSuite('Блок списка ответов. Если на вопрос 5 ответов', {
    story: {
        'По умолчанию': {
            'отображается 5 ответов': makeCase({
                id: 'marketfront-2881',
                issue: 'MARKETVERSTKA-31260',
                feature: 'Структура страницы',
                async test() {
                    const answersCount = await this.answersList.answersCount;
                    await this.expect(answersCount)
                        .to.be.equal(5);
                },
            }),
            'пагинация не отображается': makeCase({
                id: 'marketfront-2879',
                issue: 'MARKETVERSTKA-31269',
                feature: 'Структура страницы',
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
