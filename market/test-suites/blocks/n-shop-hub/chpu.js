import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на ЧПУ ссылки на n-shop-hub.
 * @param {PageObject.shopHub} shopHub
 */
export default makeSuite('ЧПУ ссылки на хабе магазина.', {
    story: {
        'Ссылка "Читать все отзывы".': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2966',
                    issue: 'MARKETVERSTKA-31995',
                    async test() {
                        const url = await this.shopHub.getReadAllButtonUrl();

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
        'Кнопка "Показать все отзывы" в попапе.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2967',
                    issue: 'MARKETVERSTKA-31995',
                    async test() {
                        await this.shopHub.hoverReviews();
                        await this.popup2.waitForReviewsPopup();
                        const url = await this.popup2.getDistributionTooltipButtonUrl();

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
        'Ссылка на все отзывы.': {
            'По умолчанию': {
                'содержит slug и является ЧПУ': makeCase({
                    id: 'marketfront-2968',
                    issue: 'MARKETVERSTKA-31995',
                    async test() {
                        const url = await this.ratingContribution.getAllReviewsUrl();

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
