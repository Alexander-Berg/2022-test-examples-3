import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.SmallQuestionSnippet} SmallQuestionSnippet
 */
export default makeSuite('Ссылка количества ответов на вопрос, если ответов нет.', {
    environment: 'kadavr',
    id: 'marketfront-3740',
    issue: 'MARKETFRONT-362',
    params: {
        expectedQuestionUrl: 'Ожидаемая ссылка на вопрос',
    },
    story: {
        'По умолчанию': {
            'ведет на конкретный вопрос.': makeCase({
                async test() {
                    const currentHref = await this.smallQuestionSnippet.answersHref;

                    await this.expect(currentHref, 'ссылка корректная')
                        .to.be.link({pathname: this.params.expectedQuestionUrl}, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),

            'отображается и содержит текст "Ответов нет".': makeCase({
                async test() {
                    await this.smallQuestionSnippet.answersLink.isVisible()
                        .should.eventually.to.be.equal(true, 'Ссылка количества ответов отображается');
                    await this.smallQuestionSnippet.getAnswersLinkText()
                        .should.eventually.be.equal(
                            'Нет ответов',
                            'Правильный текст ссылки'
                        );
                },
            }),
        },
    },
});
