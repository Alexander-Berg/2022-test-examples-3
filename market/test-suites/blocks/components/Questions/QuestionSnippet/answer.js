import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, кнопка «Ответить»', {
    feature: 'Структура страницы',
    story: {
        'По умолчанию': {
            'кнопка «Ответить» содержит ссылку на страницу вопроса': makeCase({
                id: 'marketfront-2710',
                issue: 'MARKETVERSTKA-31042',
                async test() {
                    const actualUrl = await this.questionSnippet.getAnswerLinkUrl();

                    await this.expect(actualUrl)
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'URL «Ответить» ведёт на страницу вопроса');
                },
            }),
        },
    },
});
