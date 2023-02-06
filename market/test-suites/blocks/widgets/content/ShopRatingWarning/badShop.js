import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок ShopRatingWarning
 * @param {PageObject.ShopRatingWarning} shopRatingWarning
 */
export default makeSuite('Блок с предупреждением о статусе магазина (badShop).', {
    environment: 'kadavr',
    story: {
        'Если ratingType = 0': {
            'отображается блок "Недобросовестный магазин"': makeCase({
                issue: 'MARKETFRONT-12332',
                async test() {
                    await this.shopRatingWarning.isVisible()
                        .should.eventually.to.be.equal(true);

                    return this.shopRatingWarning.isBadShopWarning()
                        .should.eventually.to.be.equal(true);
                },
            }),
        },
    },
});
