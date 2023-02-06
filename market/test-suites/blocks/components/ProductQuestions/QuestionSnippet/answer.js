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
                    const expectedUrl = await this.browser.yaBuildURL('market:product-question', {
                        productId: 1,
                        productSlug: this.params.productSlug,
                        questionId: 1,
                        questionSlug: this.params.questionSlug,
                    });
                    const actualUrl = await this.questionSnippet.getAnswerLinkUrl();

                    await this.expect(actualUrl)
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'URL «Ответить» ведёт на страницу вопроса');
                },
            }),
        },
    },
});
