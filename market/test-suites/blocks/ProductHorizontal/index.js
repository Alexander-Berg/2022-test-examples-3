import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ProductHorizontal.
 * @param {PageObject.ProductHorizontal} productHorizontal
 */

export default makeSuite('Горизонтальный сниппет с информацией о товаре', {
    story: {
        'по умолчанию': {
            'должен присутствовать.': makeCase({
                id: 'marketfront-4213',
                params: {
                    productId: 'Id товара',
                    slug: 'slug товара',
                },
                test() {
                    return this.productHorizontal.isVisible()
                        .should.eventually.be.equal(true, 'блок присутствует на странице.');
                },
            }),
        },
        'при клике': {
            'должнен сменить страницу на корректную.': makeCase({
                id: 'marketfront-4214',
                params: {
                    productId: 'Id товара',
                    slug: 'Slug товара',
                },
                test() {
                    return this.browser
                        .yaWaitForChangeUrl(() => this.productHorizontal.click())
                        .then(url => this.browser.yaBuildURL('market:product', {
                            productId: this.params.productId,
                            slug: this.params.slug,
                        }).then(buildedUrl => this.expect(url)
                            .to.be.link(buildedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            })
                        ));
                },
            }),
        },
    },
});
