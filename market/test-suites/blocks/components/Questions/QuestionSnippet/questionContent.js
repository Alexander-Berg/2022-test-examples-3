import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, текст вопроса', {
    feature: 'Структура страницы',
    story: {
        'По умолчанию': {
            'содержит ссылку на страницу вопроса': makeCase({
                id: 'marketfront-2711',
                issue: 'MARKETVERSTKA-31043',
                async test() {
                    const actualUrl = await this.questionSnippet.getQuestionContentLinkUrl();

                    await this.expect(actualUrl)
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'Ссылка на тексте вопроса ведёт на страницу вопроса');
                },
            }),
        },
    },
});
