import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок snippet-card
 * @param {PageObject.SnippetCard} snippetCard
 */
export default makeSuite('Ссылка Отзывы на сниппете товара.', {
    story: {
        'При нажатии': {
            'должна открыться страница отзывов на товар': makeCase({
                environment: 'kadavr',
                params: {
                    productId: 'ID продукта, который отображен в сниппете',
                    slug: 'Slug продукта',
                    reviewLinkTrack: 'Параметр track для ссылки на отзывы',
                },
                defaultParams: {
                    reviewLinkTrack: 'srchlink',
                },
                async test() {
                    await this.snippetCard.reviewLink.isExisting();
                    const url = await this.browser.allure.runStep(
                        'Нажимаем на ссылку "Отзывы"',
                        () => this.browser.yaWaitForChangeUrl(() => this.snippetCard.reviewLink.click(), 10000)
                    );

                    const expectedUrl = await this.browser.yaBuildURL('market:product-reviews', {
                        productId: this.params.productId,
                        slug: this.params.slug,
                        track: this.params.reviewLinkTrack,
                    });

                    return this.browser.allure.runStep('Открылась страница отзывов на указанный товар',
                        () => this.expect(url).to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        })
                    );
                },
            }),
        },
    },
});
