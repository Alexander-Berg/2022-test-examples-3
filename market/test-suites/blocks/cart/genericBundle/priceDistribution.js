import {makeSuite, makeCase} from 'ginny';
import assert from 'assert';

export default makeSuite('Размазывание', {
    feature: 'Акция товар + подарок',
    id: 'bluemarket-3126',
    story: {
        'По умолчанию': {
            'нотификация с размазыванием присутствует': makeCase({
                async test() {
                    await this.priceDistributionNotification.isVisible()
                        .should.eventually.be.equal(true, 'Плашка о размазывании цены должна отображаться');

                    await this.bundleNewNotification.isVisible()
                        .should.eventually.be.equal(false, 'Плашка об образовании комплекта должна отсутствовать');
                },
            }),
            'цены основного товара и подарка размазаны': makeCase({
                async test() {
                    const {
                        primary,
                        gift,
                        count,
                    } = this.yaTestData.bundles;

                    assert(primary.originalPrice === primary.newPrice + gift.newPrice,
                        'Цены в моках должны быть размазаны');

                    await this.primaryDiscountPrice.getPriceValue()
                        .should.eventually.be.equal(primary.originalPrice * count,
                            'Должна отображаться правильная общая цена товара + подарка');
                },
            }),
            'цена в саммари корректная': makeCase({
                async test() {
                    const {gift, count} = this.yaTestData.bundles;
                    const expectedDiscount = gift.originalPrice * count;

                    await this.orderTotal.getDiscount().should.eventually.be.equal(
                        expectedDiscount,
                        `Размер скидки в саммари должен быть равен суммарной стоимости подарков (${expectedDiscount})`
                    );
                },
            }),
        },
    },
});
