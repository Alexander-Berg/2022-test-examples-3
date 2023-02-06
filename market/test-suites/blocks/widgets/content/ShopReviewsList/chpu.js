import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки на ShopReviewsList.
 * @param {PageObject.shopReviewsList} shopReviewsList
 */
export default makeSuite('ЧПУ ссылки на карусели с отзывами.', {
    story: {
        'Ссылка "Все отзывы".': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'm-touch-2638',
                    issue: 'MOBMARKET-10021',
                    async test() {
                        const url = await this.shopReviewsList.getReadAllReviewsUrl();

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
        'Первый отзыв в карусели.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'm-touch-2533',
                    issue: 'MOBMARKET-10021',
                    async test() {
                        const url = await this.shopReviewsList.getReviewUrlByIndex();

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
