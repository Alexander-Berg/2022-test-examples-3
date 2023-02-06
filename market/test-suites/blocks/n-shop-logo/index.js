import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-shop-logo
 *
 * @property {PageObject.ShopLogo} shopLogo
 */
export default makeSuite('Логотип магазина.', {
    feature: 'Логотип магазина',
    story: {
        'По умолчанию': {
            'изображение логотипа присутствует': makeCase({
                environment: 'kadavr',
                async test() {
                    return this.shopLogo.isImageVisible().should.eventually.to.be.equal(
                        true,
                        'Изображение логотипа магазина присутствует.'
                    );
                },
            }),
        },
    },
});
