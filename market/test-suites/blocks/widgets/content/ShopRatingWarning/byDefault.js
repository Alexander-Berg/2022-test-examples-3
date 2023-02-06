import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок ShopRatingWarning
 * @param {PageObject.ShopRatingWarning} shopRatingWarning
 */
export default makeSuite('Блок с предупреждением о статусе магазина (по умолчанию).', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'не отображается': makeCase({
                issue: 'MARKETFRONT-12332',
                async test() {
                    return this.shopRatingWarning.isVisible()
                        .should.eventually.to.be.equal(false);
                },
            }),
        },
    },
});
