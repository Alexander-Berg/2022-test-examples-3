import {makeCase, makeSuite, mergeSuites} from 'ginny';

import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';

export default makeSuite('Оффер.', {
    environment: 'kadavr',
    defaultParams: {
        items: [{
            skuId: kettle.skuMock.id,
            offerId: kettle.offerMock.offerId,
        }],
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    orderTotal: () => this.createPageObject(OrderTotal),
                    cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                    cartItem: () => this.createPageObject(CartItem),
                    discountPrice: () => this.createPageObject(DiscountPrice, {parent: this.cartItem}),
                });

                const cart = buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                        buyerPrice: 600,
                        buyerPriceNominal: 600,
                        buyerPriceBeforeDiscount: 1000,
                    }],

                    itemsTotal: 600,
                    buyerTotalBeforeDiscount: 1000,
                    buyerItemsTotalDiscount: 400,
                    buyerItemsTotalBeforeDiscount: 1000,
                    buyerTotalDiscount: 400,
                });
                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    [cart]
                );

                const {cart: cartState} = testState.checkouterState;
                await this.browser.setState('Checkouter.collections.cart',
                    {
                        ...cartState,
                        [cart.label]: {
                            ...cartState[cart.label],
                            promos: [
                                {
                                    type: 'MARKET_BLUE',
                                    marketPromoId: '',
                                    buyerItemsDiscount: 400,
                                    deliveryDiscount: 0,
                                },
                            ],
                        },
                    }
                );

                return this.browser.yaScenario(
                    this,
                    prepareCartPageBySkuId,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        region: this.params.region,
                    }
                );
            },
            'Товар со скидкой. В списке офферов': {
                'у товара отображается старая цена': makeCase({
                    feature: 'Оффер',
                    id: 'bluemarket-2723',
                    issue: 'BLUEMARKET-6355',
                    async test() {
                        return this.discountPrice.oldPrice.isVisible()
                            .should.eventually.to.be.equal(true, 'У оффера должна отображаться старая цена');
                    },
                }),
                'у товара отображается новая цена': makeCase({
                    feature: 'Оффер',
                    id: 'bluemarket-2723',
                    issue: 'BLUEMARKET-6355',
                    async test() {
                        return this.discountPrice.currentPrice.isVisible()
                            .should.eventually.to.be.equal(true, 'У оффера должна отображаться новая цена');
                    },
                }),
                'старая цена больше новой': makeCase({
                    feature: 'Оффер',
                    id: 'bluemarket-2723',
                    issue: 'BLUEMARKET-6355',
                    async test() {
                        const oldPrice = await this.discountPrice.getOldPriceValue();
                        const currentPrice = await this.discountPrice.getPriceValue();
                        return this.expect(oldPrice).to.be.above(
                            currentPrice,
                            'Старая цена должна быть больше новой цены'
                        );
                    },
                }),
            },
            'Товар со скидкой. В саммари': {
                'правильные даные': makeCase({
                    feature: 'Саммари',
                    id: 'bluemarket-2723',
                    issue: 'BLUEMARKET-6355',
                    async test() {
                        const oldPrice = await this.discountPrice.getOldPriceValue();
                        const currentPrice = await this.discountPrice.getPriceValue();
                        const discountValue = oldPrice - currentPrice;
                        await this.orderTotal.getDiscountText()
                            .should.eventually.to.be.equal(
                                `Скидки -\u202F${discountValue}\u202F₽`,
                                `В блоке Саммари должна быть надпись “Скидки - ${discountValue} ₽”`
                            );

                        const discountTotal = await this.orderTotal.getDiscount();
                        const total = await this.orderTotal.getItemsValue();
                        return this.expect(currentPrice).to.equal(
                            total - discountTotal,
                            'Значение новой цены товара должно быть равно цене из Саммари за вычетом скидки на товары'
                        );
                    },
                }),
            },
        }
    ),
});
