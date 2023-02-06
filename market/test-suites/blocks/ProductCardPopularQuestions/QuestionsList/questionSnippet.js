import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.SmallQuestionSnippet} SmallQuestionSnippet
 */
export default makeSuite('Сниппет популярных вопросов.', {
    environment: 'kadavr',
    params: {
        expectedQuestionUrl: 'Ожидаемая ссылка на вопрос',
    },
    story: {
        'Текст вопроса': {
            'по умолчанию': {
                'содержит ссылку на вопрос.': makeCase({
                    id: 'marketfront-3715',
                    issue: 'MARKETFRONT-362',
                    async test() {
                        const currentHref = await this.smallQuestionSnippet.textHref;

                        await this.expect(currentHref, 'ссылка корректная')
                            .to.be.link({pathname: this.params.expectedQuestionUrl}, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
        'Ссылка количества ответов на вопрос': {
            'по умолчанию': {
                'отображается и ведет на конкретный вопрос.': makeCase({
                    id: 'marketfront-3739',
                    issue: 'MARKETFRONT-362',
                    async test() {
                        const currentHref = await this.smallQuestionSnippet.answersHref;

                        await this.smallQuestionSnippet.answersLink.isVisible()
                            .should.eventually.to.be.equal(true, 'Ссылка количества ответов отображается');
                        await this.expect(currentHref, 'ссылка корректная')
                            .to.be.link({pathname: this.params.expectedQuestionUrl}, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
