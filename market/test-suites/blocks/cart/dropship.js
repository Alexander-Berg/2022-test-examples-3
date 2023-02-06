import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';

import promocodes from '@self/root/src/spec/hermione/configs/checkout/promocodes';
import * as alcohol from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';

import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartOfferAvailabilityInfo
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferAvailabilityInfo/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import OrderInfoAbstractOrderItem from '@self/root/src/components/TotalDeliveryInfo/Items/AbstractOrderItem/__pageObject';
import Promocode from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import DeliveryRemainder from '@self/root/src/components/DeliveryRemainder/__pageObject';
import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';

import {deliveryPickupMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

/**
 * Тесты на дропшип на корзине.
 * @param {PageObject.CartOrderInfo} orderInfo
 * @param {PageObject.CartGroup} cartGroup
 */
export default makeSuite('Дропшип.', {
    feature: 'Дропшип',
    environment: 'kadavr',
    defaultParams: {
        items: [{
            skuMock: alcohol.skuMock,
            offerMock: alcohol.offerMock,
        }],
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                    cartItem: () => this.createPageObject(CartItem,
                        {
                            parent: this.cartGroup,
                            root: this.cartItems.firstItem,
                        }
                    ),
                    cartOffer: () => this.createPageObject(CartOffer, {parent: this.cartItem}),
                    cartOfferAvailabilityInfo: () => this.createPageObject(
                        CartOfferAvailabilityInfo,
                        {parent: this.cartOffer}
                    ),
                    orderInfoTotal: () => this.createPageObject(OrderTotal),
                    orderInfoPromocode: () => this.createPageObject(Promocode, {parent: this.orderInfo}),
                    orderInfoDeliveryItem: () => this.createPageObject(
                        OrderInfoAbstractOrderItem,
                        {
                            parent: this.orderInfo,
                            root: `${OrderInfoAbstractOrderItem.root}${OrderInfoAbstractOrderItem.delivery}`,
                        }
                    ),
                    promocode: () => this.createPageObject(Promocode),
                    promocodeSubmitField: () => this.createPageObject(
                        SubmitField,
                        {
                            parent: this.promocode,
                        }
                    ),
                    deliveryRemainder: () => this.createPageObject(DeliveryRemainder),
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    [buildCheckouterBucket({
                        items: this.params.items,
                        deliveryOptions: [deliveryPickupMock],
                    })]
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
        },

        {
            'Промокод не применяется, в саммари пункт промокода отсутствует': makeCase({
                id: 'bluemarket-645',
                issue: 'BLUEMARKET-3900',
                async test() {
                    const validPromocode = promocodes.find(promocode => promocode.status === 'ACTIVE');
                    await this.promocodeSubmitField.applyValue(validPromocode.code, 'промокод');
                    await this.browser.yaSafeAction(this.orderInfoPreloader.waitForHidden(3000), true);

                    await this.orderInfoTotal.isPromocodeVisible()
                        .should.eventually.to.be.equal(false, 'Скидка по промокоду не должна отображаться');
                },
            }),

            'Нет блока с прогрессом до бесплатной доставки': makeCase({
                id: 'bluemarket-2672',
                issue: 'BLUEMARKET-6896',
                async test() {
                    return this.deliveryRemainder.isExisting()
                        .should.eventually.be.equal(false, 'Блока с прогрессом быть не должно');
                },
            }),
        }
    ),
});
