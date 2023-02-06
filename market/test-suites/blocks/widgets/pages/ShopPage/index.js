import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок n-shop-hub.
 * @param {PageObject.ShopPage} shopPage
 * @param {Number} params.shopId id магазина - обязателен.
 */
export default makeSuite('Блок хаба магазина.', {
    feature: 'Хаб магазина',
    environment: 'kadavr',
    story: {
        'Карусель популярных товаров.': {
            'При нажатии на ссылку "Смотреть все"': {
                'должна открыться страница поиска товаров текущего магазина': makeCase({
                    id: 'marketfront-2160',
                    issue: 'MARKETVERSTKA-27043',
                    params: {
                        shopId: 'Id магазина',
                        slug: 'slug магазина',
                    },
                    test() {
                        return this.browser.yaWaitForChangeUrl(() => this.shopPage.clickSeeAllPopular(), 10000)
                            .then(url =>
                                this.browser.yaBuildURL('market:search', {
                                    'fesh': this.params.shopId,
                                    'slug': this.params.slug,
                                })
                                    .then(buildedUrl => this.expect(url, 'Проверяем что URL является поиском магазина')
                                        .to.be.link(buildedUrl, {
                                            skipProtocol: true,
                                            skipHostname: true,
                                        })
                                    ));
                    },
                }),
            },
        },
    },
});
