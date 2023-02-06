import {makeSuite, makeCase} from 'ginny';

/**
 * @property {PageObject.ProductCardHeader} productCardHeader
 */
export default makeSuite('Точка входа на вопросы к товару в заголовке КМ', {
    story: {
        'Если есть вопросы о товаре': {
            'ссылка отображается':
                makeCase({
                    id: 'm-touch-2983',
                    issue: 'MOBMARKET-13319',
                    feature: 'Структура страницы',
                    async test() {
                        const {productId, slug} = this.params;
                        const expectedUrl = await this.browser.yaBuildURL(
                            'touch:product-questions', {
                                productId,
                                slug,
                            });

                        await this.expect(
                            this.productCardHeader.isQaEntrypointVisible()
                        ).to.equal(true, 'Ссылка видна');
                        const url = await this.productCardHeader.getQaEntrypointUrl();

                        return this.expect(url).to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
        },
    },
});
