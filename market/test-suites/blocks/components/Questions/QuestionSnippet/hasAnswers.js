import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, есть ответы', {
    feature: 'Структура страницы',
    story: {
        'Если есть 5 ответов': {
            'отображается текст «Ещё 4 ответа» и его ссылка ведёт на страницу вопроса': makeCase({
                id: 'marketfront-2708',
                issue: 'MARKETVERSTKA-31040',
                async test() {
                    await this.questionSnippet
                        .getAnswersCount()
                        .should.eventually.be.equal(
                            'Ещё 4 ответа',
                            'Отображается текст «Ещё 4 ответа»'
                        );

                    const actualUrl = await this.questionSnippet.getAnswersCountLinkUrl();

                    await this.expect(actualUrl)
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'URL «Ещё 4 ответа» ведёт на страницу вопроса');
                },
            }),
        },
    },
});
