import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, кнопка «Удалить», старый вопрос', {
    feature: 'Структура страницы',
    story: {
        'Если вопрос создан более чем сутки назад': {
            'кнопка «Удалить» отсутствует': makeCase({
                id: 'marketfront-2825',
                issue: 'MARKETVERSTKA-31045',
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
