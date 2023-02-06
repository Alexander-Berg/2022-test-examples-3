import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на ЧПУ в блоке w-product-offers.
 * @param {PageObject.ShopName} shopName
 * @param {PageObject.OfferSnippet} offerSnippet
 */
export default makeSuite('Блок с офферами на КМ.', {
    story: {
        'Ссылка на отзывы магазина в дефолтном оффере.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2414',
                    issue: 'MOBMARKET-10021',
                    async test() {
                        const url = await this.shopName.getShopReviewsUrl();

                        return this.expect(url).to.be.link({
                            pathname: `shop--${this.params.slug}/${this.params.shopId}/reviews`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
        'Ссылка на отзывы магазина в оффере из топ-6.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2414',
                    issue: 'MOBMARKET-10021',
                    async test() {
                        const url = await this.offerSnippet.getShopReviewsUrl();

                        return this.expect(url).to.be.link({
                            pathname: `shop--${this.params.slug}/${this.params.shopId}/reviews`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});
