import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на страницу автосравнений
 * @property {PageObject.VersusContent.Header} this.header
 *
 */

export default makeSuite('Шапка.', {
    params: {
        productId: 'Идентификатор продукта',
        slug: 'Слаг продукта',
    },
    story: {
        'По умолчанию': {
            'Содержит корректную ссылку на страницу товара': makeCase({
                issue: 'MARKETFRONT-5153',
                id: 'marketfront-3763',
                async test() {
                    const expectedPath = await this.browser.yaBuildURL('market:product', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                    });
                    const actualPath = await this.header.getProductHref();

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
