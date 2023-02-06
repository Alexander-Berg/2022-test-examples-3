import {makeCase, makeSuite} from 'ginny';

import CartTotalInformation from
    '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

/**
 * Проверяет саммари в корзине
 */
module.exports = makeSuite('Саммари корзины', {
    environment: 'kadavr',

    params: {
        shouldShowSummary: 'Должно ли показываться саммари корзины',
        expectedPrice: 'Цена товаров',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                cartOrderInfo: () => this.createPageObject(CartTotalInformation),
                orderTotal: () => this.createPageObject(OrderTotal),
            });
        },

        'Блок отображается как положено с суммой и количеством товаров': makeCase({
            async test() {
                await this.cartOrderInfo.isExisting()
                    .should.eventually.to.be.equal(
                        this.params.shouldShowSummary,
                        `Саммари корзины ${this.params.shouldShowSummary ? '' : 'не'} должно отображаться`
                    );
                if (this.params.shouldShowSummary) {
                    await this.orderTotal.getItemsCount()
                        .should.eventually.to.be.equal(
                            1,
                            'Количество товаров в саммари должно быть равно 1'
                        );

                    return this.orderTotal.getItemsValue()
                        .should.eventually.to.be.equal(
                            this.params.expectedPrice,
                            `Цена товаров равна ${this.params.expectedPrice}`
                        );
                }
            },
        }),
    },
});
