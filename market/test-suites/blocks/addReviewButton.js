import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на кнопку добавить отзыв
 * @property {PageObject.Button2} this.addReviewButton
 *
 */

export default makeSuite('Кнопка "Написать отзыв"', {
    params: {
        productId: 'Идентификатор продукта',
    },
    story: {
        'Содержит корректную ссылку на страницу создания отзыва': makeCase({
            issue: 'MARKETVERSTKA-25227',
            async test() {
                const expectedPath = await this.browser.yaBuildURL('market:product-reviews-add', {
                    productId: this.params.productId,
                    retpath: await this.browser.getUrl(),
                });

                const actualPath = await this.addReviewButton.getHref();

                return this.expect(actualPath)
                    .to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
            },
        }),
    },
});
