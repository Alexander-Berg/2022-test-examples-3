import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.AnswersList} answersList
 * @param {PageObject.Paginator} paginator
 */
export default makeSuite('Блок списка ответов. Если ответов на вопрос больше 10', {
    story: {
        'Если ответов на вопрос больше 10': {
            'отображается 10 ответов в блоке': makeCase({
                id: 'marketfront-2880',
                issue: 'MARKETVERSTKA-31259',
                feature: 'Структура страницы',
                async test() {
                    const answersCount = await this.answersList.answersCount;
                    await this.expect(answersCount)
                        .to.be.equal(10);
                },
            }),
            'пагинация отображается': makeCase({
                id: 'marketfront-2878',
                issue: 'MARKETVERSTKA-31268',
                feature: 'Структура страницы',
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
