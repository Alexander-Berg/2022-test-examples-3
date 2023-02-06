import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на ЧПУ в блоке ProductOffers.
 * @param {PageObject.ShopName} shopName
 * @param {PageObject.ProductOffers} productOffer
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
    },
});
