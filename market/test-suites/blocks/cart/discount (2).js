import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';

import {discount} from '@self/root/src/spec/hermione/configs/card';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Оффер.', {
    environment: 'testing',
    defaultParams: {
        items: [{
            skuId: discount.skuId,
            offerId: discount.offerId,
            count: 1,
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

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);

                await this.browser.yaScenario(this, prepareCartPageBySkuId, {
                    region: this.params.region,
                    items: this.params.items,
                });
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
                            .should.eventually
                            .to.be.equal(
                                `Скидка ${discountValue} ₽`,
                                `В блоке Саммари должна быть надпись “Скидка ${discountValue} ₽”`
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
