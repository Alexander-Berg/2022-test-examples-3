import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки на ShopCard.
 * @param {PageObject.shopCard} shopCard
 */
export default makeSuite('ЧПУ ссылки на все отзывы.', {
    story: {
        'Ссылка "N отзывов".': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'm-touch-2419',
                    issue: 'MOBMARKET-10021',
                    async test() {
                        const url = await this.shopCard.getReadAllReviewsUrl();

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
