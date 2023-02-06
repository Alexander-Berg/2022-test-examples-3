import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, нет ответов', {
    feature: 'Структура страницы',
    story: {
        'Если ответов нет': {
            'кнопка «Ещё N ответов» не отображается': makeCase({
                id: 'marketfront-2709',
                issue: 'MARKETVERSTKA-31041',
                async test() {
                    await this.questionSnippet
                        .isMoreAnswerVisible()
                        .should.eventually.be.equal(
                            false,
                            'Кнопка «Ещё N ответов» не отображается'
                        );
                },
            }),
        },
    },
});
