import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * @param {PageObject.OfferSummaryShopName} shopInfo
 */
export default makeSuite('Рейтинг и отзывы о магазине', {
    params: {
        shopId: 'Id магазина',
        slug: 'Слаг',
    },
    story: mergeSuites(
        {
            'По умолчанию': {
                'содержит ссылку на отзывы о магазине': makeCase({
                    id: 'm-touch-3647',
                    async test() {
                        const actualUrl = await this.shopInfo.getShopReviewsLink();

                        const expectedUrl = await this.browser.yaBuildURL('touch:shop-reviews', {
                            shopId: this.params.shopId,
                            slug: this.params.slug,
                        });

                        return this.expect(actualUrl)
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        }
    ),
});
