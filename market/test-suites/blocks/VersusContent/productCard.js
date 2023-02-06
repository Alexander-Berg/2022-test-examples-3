import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.ProductCard} this.productCard
 *
 */

export default makeSuite('Блок с описанием товара', {
    params: {
        productId: 'Идентификатор продукта',
        slug: 'Слаг продукта',
    },
    story: {
        'Содержит корректную ссылку': {
            'на страницу товара в заголовке': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3779',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });
                    const actualPath = await this.productCard.getProductHref();

                    return this.expect(actualPath).to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
                },
            }),
            'на страницу товара в кнопке "Перейти на товар"': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3780',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });
                    const actualPath = await this.productCard.getButtonLinkHref();

                    return this.expect(actualPath).to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
                },
            }),
            'на страницу отзывов о товаре': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3781',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product-reviews', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });
                    const actualPath = await this.productCard.getReviewsHref();

                    return this.expect(actualPath).to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
                },
            }),
            'на страницу оферов': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3782',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product-offers', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });
                    const actualPath = await this.productCard.getPriceHref();

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
    },
});
