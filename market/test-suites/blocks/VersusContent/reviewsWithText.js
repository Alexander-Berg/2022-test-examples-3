import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.Reviews} this.reviews
 *
 */

export default makeSuite('Блок отзывов при наличии отзывов с текстом', {
    params: {
        productId: 'Идентификатор продукта',
        slug: 'Слаг продукта',
    },
    story: {
        'Содержит корректную ссылку на страницу всех отзывов': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3783',
            async test() {
                const expectedPath = await this.browser.yaBuildURL('market:product-reviews', {
                    productId: this.params.productId,
                    slug: this.params.slug,
                });
                const actualPath = await this.reviews.getMoreReviewsButtonLink();

                return this.expect(actualPath).to.be.link(
                    expectedPath,
                    {
                        skipProtocol: true,
                        skipHostname: true,
                    }
                );
            },
        }),
        'Содержит сниппет отзыва': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3784',
            test() {
                return this.reviews
                    .hasReviewItem()
                    .should.eventually.be.equal(true, 'Блок содержит отзыв с текстом');
            },
        }),
        'Содержит кнопку "Смотреть все отзывы"': makeCase({
            issue: 'MARKETFRONT-5153',
            id: 'marketfront-3785',
            test() {
                return this.reviews
                    .hasMoreReviewsButton()
                    .should.eventually.be.equal(true, 'Блок содержит  кнопку "Смотреть все отзывы"');
            },
        }),
    },
});
