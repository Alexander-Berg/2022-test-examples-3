import {makeCase, makeSuite} from 'ginny';

import CartOfferAvailabilityInfo
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferAvailabilityInfo/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

const START_COUNT = 3;

module.exports = makeSuite('Доступно меньшее количество товаров', {
    environment: 'kadavr',

    params: {
        price: 'Цена товара',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                availableCartOfferAvailabilityInfo: () => this.createPageObject(CartOfferAvailabilityInfo),
                orderTotal: () => this.createPageObject(OrderTotal),
            });
        },
        [`Надпись "Осталось ${START_COUNT - 1} штуки" отображается на оффере`]: makeCase({
            async test() {
                const offerNotification = `Осталось ${START_COUNT - 1} штуки`;

                await this.availableCartOfferAvailabilityInfo.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Надпись должна отображаться'
                    );

                return this.availableCartOfferAvailabilityInfo.getStatusText()
                    .should.eventually.to.be.include(
                        offerNotification,
                        `Надпись должна содержать текст ${offerNotification}`
                    );
            },
        }),

        'Сумма и количество товаров в саммари отображается с учетом доступного количества': makeCase({
            async test() {
                const count = START_COUNT - 1;
                await this.orderTotal.getItemsCount()
                    .should.eventually.to.be.equal(
                        count,
                        `Количество товаров в саммари должно быть равно ${count}`
                    );

                const price = count * this.params.price;
                return this.orderTotal.getItemsValue()
                    .should.eventually.to.be.equal(
                        price,
                        `Цена товаров равна ${price}`
                    );
            },
        }),
    },
});
