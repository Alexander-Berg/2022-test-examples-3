import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на точку входа на вопросы к товару в заголовке КМ
 * @property {PageObject.ProductTitle} this.productTitle
 */

export default makeSuite('Точка входа на вопросы к товару в заголовке КМ', {
    story: {
        'Если есть вопросы о товаре': {
            'ссылка отображается': makeCase({
                async test() {
                    const {productId, slug} = this.params;
                    const expectedUrl = await this.browser.yaBuildURL(
                        'market:product-questions', {
                            productId,
                            slug,
                        });

                    await this.expect(
                        this.productTitle.isQaEntrypointVisible()
                    ).to.equal(true, 'Ссылка видна');
                    const url = await this.productTitle.getQaEntrypointUrl();

                    return this.expect(url).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
