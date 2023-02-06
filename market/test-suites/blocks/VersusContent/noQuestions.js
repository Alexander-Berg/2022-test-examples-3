import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.Questions} this.questions
 *
 */

export default makeSuite('Блок вопросов-ответов при отсутствии вопросов.', {
    params: {
        productId: 'Идентификатор продукта',
        productSlug: 'Слаг продукта',
    },
    story: {
        'Содержит корректную ссылку для создания вопроса о товаре': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3772',
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
        'Cодержит кнопку "Спросить"': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3773',
            test() {
                return this.questions
                    .hasAskButton()
                    .should.eventually.be.equal(true, 'Блок содержит кнопку "Спросить"');
            },
        }),
    },
});
