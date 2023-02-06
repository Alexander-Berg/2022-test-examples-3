import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.Reviews} this.reviews
 *
 */

export default makeSuite('Блок отзывов при наличии только оценок.', {
    params: {
        productId: 'Идентификатор продукта',
        slug: 'Слаг продукта',
    },
    story: {
        'Содержит корректную ссылку для оставления отзыва на товар': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3786',
            async test() {
                const expectedPath = await this.browser.yaBuildURL('market:product-reviews-add', {
                    productId: this.params.productId,
                    slug: this.params.slug,
                });
                const actualPath = await this.reviews.getAddReviewButtonLink();

                return this.expect(actualPath).to.be.link(
                    expectedPath,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    }
                );
            },
        }),
        'Содержит блок оценок': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3787',
            test() {
                return this.reviews
                    .hasGradesList()
                    .should.eventually.be.equal(true, 'Блок содержит блок оценок');
            },
        }),
        'Содержит кнопку "Оставить отзыв"': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3788',
            test() {
                return this.reviews
                    .hasAddReviewButton()
                    .should.eventually.be.equal(true, 'Блок содержит кнопку "Оставить отзыв"');
            },
        }),
    },
});
