import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на виджет ProductOffersStaticList.
 *
 * @property {PageObject.ProductOffersStaticList} this.productOffersStaticList
 */
export default makeSuite('Ссылка на все доступные предложения', {
    params: {
        productId: 'Идентификатор модели',
        slug: 'Слаг модели',
    },
    story: {
        'По умолчанию': {
            'ведёт на карточку модели': makeCase({
                id: 'm-touch-2884',
                issue: 'MOBMARKET-12583',

                async test() {
                    const url = await this.productOffersStaticList.getAllOffersLinkUrl();

                    await this.expect(url).to.be.link({
                        pathname: `/product--${this.params.slug}/${this.params.productId}/offers`,
                    }, {
                        skipProtocol: true,
                        skipHostname: true,
                        skipQuery: true,
                    });
                },
            }),
        },
    },
});
