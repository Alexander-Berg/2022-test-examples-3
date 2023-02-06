import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.Questions} this.questions
 *
 */

export default makeSuite('Блок вопросов-ответов если вопросов три или меньше.', {
    params: {
        productId: 'Идентификатор продукта',
        productSlug: 'Слаг продукта',
        questionId: 'Идентификатор вопроса о товаре',
        questionSlug: 'Слаг вопроса о товаре',
    },
    story: {
        'Содержит корректную ссылку': {
            'для создания вопроса о товаре': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3764',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product-questions', {
                        productId: this.params.productId,
                        slug: this.params.productSlug,
                        focus: 'question-form',
                    });

                    const actualPath = await this.questions.getAskQuestionsLink();

                    return this.expect(actualPath).to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
                },
            }),
            'на вопрос о товаре': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3765',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product-question', {
                        productId: this.params.productId,
                        productSlug: this.params.productSlug,
                        questionSlug: this.params.questionSlug,
                        questionId: this.params.questionId,
                    });

                    const actualPath = await this.questions.getQuestionLink();

                    return this.expect(actualPath).to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
                },
            }),
        },
        'Содержит блок вопросов': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3766',
            test() {
                return this.questions
                    .hasQuestionsList()
                    .should.eventually.be.equal(true, 'Блок содержит блок вопросов');
            },
        }),
        'Содержит кнопку "Спросить"': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3767',
            test() {
                return this.questions
                    .hasAskButton()
                    .should.eventually.be.equal(true, 'Блок содержит  кнопку "Спросить"');
            },
        }),
    },
});
