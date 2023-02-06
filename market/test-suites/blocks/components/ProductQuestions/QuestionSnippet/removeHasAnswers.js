import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, кнопка «Удалить», есть ответы', {
    feature: 'Структура страницы',
    story: {
        'Если на вопрос есть ответы': {
            'кнопка «Удалить» отсутствует': makeCase({
                id: 'marketfront-2826',
                issue: 'MARKETVERSTKA-31046',
                async test() {
                    await this.questionSnippet
                        .isRemoveVisible()
                        .should.eventually.be.equal(
                            false,
                            'Кнопка «Удалить» не видна'
                        );
                },
            }),
        },
    },
});
