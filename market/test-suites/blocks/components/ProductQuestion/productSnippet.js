import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на сниппет товара
 * @property {PageObject.ProductSnippet} this.productSnippet
 * @param {string} this.params.expectedProductSlug ожидаемый slug товара
 * @param {string} this.params.expectedProductId ожидаемый id товара
 */
export default makeSuite('Сниппет товара.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'содержит ссылку на вопросы о товаре': makeCase({
                id: 'marketfront-2876',
                issue: 'MARKETVERSTKA-31280',
                feature: 'Переходы',
                async test() {
                    const link = await this.productSnippet.getProductQuestionsLink();

                    const expectedLink = await this.browser.yaBuildURL(
                        'market:product-questions',
                        {
                            slug: this.params.expectedProductSlug,
                            productId: this.params.expectedProductId,
                        }
                    );

                    return this.expect(link, 'Сниппет товара содержит ссылку на КМ')
                        .to.be.link(
                            expectedLink,
                            {
                                skipProtocol: true,
                                skipHostname: true,
                            }
                        );
                },
            }),
        },
    },
});
