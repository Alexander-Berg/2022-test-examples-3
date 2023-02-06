import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на кнопку n-reviews-add-button
 *
 * @param {PageObject.Widgets.Content.ShopReviews} shopReviews
 */
export default makeSuite('Тулбар отзывов. Кнопка "Написать отзыв"', {
    feature: 'Создание отзыва',
    params: {
        shopId: 'Идентификатор магазина',
        slug: 'Слаг магазина',
        path: 'Имя роута',
    },
    environment: 'testing',
    story: {
        beforeEach() {
            return this.browser.yaOpenPage(this.params.path, {
                shopId: this.params.shopId,
                slug: this.params.slug,
            });
        },

        'содержит корректную ссылку на страницу создания отзыва': makeCase({
            id: 'marketfront-1140',
            issue: 'MARKETVERSTKA-25235',

            async test() {
                const retpath = await this.browser.getUrl();
                const expectedPath = await this.browser.yaBuildURL('market:shop-reviews-add', {
                    shopId: this.params.shopId,
                    slug: this.params.slug,
                    retpath: encodeURIComponent(retpath),
                    track: 'rev_sh',
                });

                const actualPath = await this.shopReviews.getAddButtonHref();

                return this.expect(actualPath)
                    .to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
            },
        }),
    },
});
