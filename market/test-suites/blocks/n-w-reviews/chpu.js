import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки блоков нерекомендованных отзывов и фильтров по оценке.
 * @param {PageObject.ShopReviewsHidden} shopReviewsHidden
 * @param {PageObject.ShopRatingStat} shopRatingStat
 */
export default makeSuite('Блок рекомендованных отзывов.', {
    story: {
        'Ссылка на отзывы с оценкой.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2970',
                    issue: 'MARKETVERSTKA-31995',
                    async test() {
                        const url = await this.shopRatingStat.getRatingReviewsUrl();

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
