import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.Questions} this.questions
 *
 */

export default makeSuite('Блок вопросов-ответов если вопросов больше трех.', {
    params: {
        productId: 'Идентификатор продукта',
        productSlug: 'Слаг продукта',
        questionId: 'Идентификатор вопроса о товаре',
        questionSlug: 'Слаг вопроса о товаре',
    },
    story: {
        'Содержит корректную ссылку': {
            'на страницу всех вопросов о товаре': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3768',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product-questions', {
                        productId: this.params.productId,
                        slug: this.params.productSlug,
                    });
                    const actualPath = await this.questions.getMoreQuestionsLink();

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
                id: 'marketfront-3769',
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
        'При наличии вопросов': {
            'содержит блок вопросов': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3770',
                test() {
                    return this.questions
                        .hasQuestionsList()
                        .should.eventually.be.equal(true, 'Блок содержит блок вопросов');
                },
            }),
            'содержит кнопку "Смотреть все вопросы"': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3771',
                test() {
                    return this.questions
                        .hasMoreButton()
                        .should.eventually.be.equal(true, 'Блок содержит  кнопку "Смотреть все вопросы"');
                },
            }),
        },
    },
});
