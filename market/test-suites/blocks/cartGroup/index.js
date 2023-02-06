import {
    mergeSuites,
    makeSuite,
    makeCase,
} from 'ginny';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';
import {keyBy} from 'ambar';

import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';

import OrderInfoAbstractOrderItem from '@self/root/src/components/TotalDeliveryInfo/Items/AbstractOrderItem/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartTotalInformation from
    '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import {
    RemovedCartItemNotification,
} from '@self/root/src/widgets/content/cart/CartList/components/CartItem/Notification/__pageObject';
import Promocode from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

/**
 * Тесты на блок CartGroup.
 * @param {PageObject.CartGroup} cartGroup
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Карточка магазина.', {
    environment: 'testing',
    feature: 'Саммари',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartItem: () => this.createPageObject(CartItem, {parent: this.cartGroup}),
                    cartGroupOrderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                    orderInfoPromocode: () => this.createPageObject(Promocode, {parent: this.cartGroupOrderInfo}),
                    orderInfoPaymentItem() {
                        return this.createPageObject(
                            OrderInfoAbstractOrderItem,
                            {
                                parent: this.cartGroupOrderInfo,
                                root: `${OrderInfoAbstractOrderItem.root}${OrderInfoAbstractOrderItem.payment}`,
                            }
                        );
                    },
                    orderInfoDeliveryItem() {
                        return this.createPageObject(
                            OrderInfoAbstractOrderItem,
                            {
                                parent: this.cartGroupOrderInfo,
                                root: `${OrderInfoAbstractOrderItem.root}${OrderInfoAbstractOrderItem.delivery}:nth-child(1)`,
                            }
                        );
                    },
                    removedCartItemNotification: () => this.createPageObject(
                        RemovedCartItemNotification,
                        {parent: this.cartItem}
                    ),
                    cartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {parent: this.cartItem}),
                    orderInfoTotal: () => this.createPageObject(OrderTotal),
                    promocode: () => this.createPageObject(Promocode),
                    promocodeSubmitField: () => this.createPageObject(
                        SubmitField,
                        {
                            parent: this.promocode,
                        }
                    ),
                });

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);

                return this.browser.yaScenario(
                    this,
                    'cart.prepareCartPageBySkuId',
                    {
                        items: this.params.items,
                        region: this.params.region,
                    }
                );
            },
        },

        makeSuite('Один оффер в корзине, 1 штука.', {
            defaultParams: {
                items: [{
                    skuId: cartItemsIds.asus.skuId,
                    offerId: cartItemsIds.asus.offerId,
                }],
            },
            story: {
                'По умолчанию': {
                    'отображается информация о заказе': makeCase({
                        id: 'bluemarket-2290',
                        test() {
                            return this.cartGroupOrderInfo.isVisible()
                                .should.eventually.be.equal(true, 'Информация о заказе должна показываться');
                        },
                    }),
                    'отображается общая стоимость товаров без скидки в корзине и она верна': makeCase({
                        id: 'bluemarket-2290',
                        test() {
                            return testTotalItemsPrice.call(this);
                        },
                    }),
                    'отображается поле промокода': makeCase({
                        id: 'bluemarket-2290',
                        issue: 'BLUEMARKET-3635',
                        async test() {
                            await this.orderInfoPromocode.isVisible()
                                .should.eventually.to.be.equal(true, 'Поле промокода должно отображаться');
                        },
                    }),
                },

                'При удалении элемента из магазина': {
                    beforeEach() {
                        return this.cartItemRemoveButton.waitForVisible()
                            .then(() => this.cartItemRemoveButton.click())
                            .then(() => this.cartItem.waitForRemoved());
                    },
                    'скрывается информация о заказе': makeCase({
                        id: 'bluemarket-2290',
                        test() {
                            return this.cartGroupOrderInfo.isVisible()
                                .should.eventually.be.equal(false, 'Информация о заказе не должна показываться');
                        },
                    }),
                },
                'При восстановлении удаленного элемента магазина': {
                    beforeEach() {
                        return this.cartItemRemoveButton.waitForVisible()
                            .then(() => this.cartItemRemoveButton.click())
                            .then(() => this.cartItemRemoveButton.waitForRemoved())
                            .then(() => this.removedCartItemNotification.waitForNotificationVisible())
                            .then(() => this.removedCartItemNotification.cancelClick())
                            .then(() => this.removedCartItemNotification.waitForHidden());
                    },
                    'отображается информация о заказе': makeCase({
                        id: 'bluemarket-761',
                        test() {
                            return this.cartGroupOrderInfo.isVisible()
                                .should.eventually.be.equal(true, 'Информация о заказе должна показываться');
                        },
                    }),
                },
            },
        }),

        makeSuite('Один оффер в корзине, 2 штуки.', {
            defaultParams: {
                items: [{
                    skuId: cartItemsIds.asus.skuId,
                    offerId: cartItemsIds.asus.offerId,
                    count: 2,
                }],
            },
            story: {
                'По умолчанию': {
                    'отображается общая стоимость товаров без скидки в корзине и она верна': makeCase({
                        id: 'bluemarket-2290',
                        test() {
                            // eslint-disable-next-line market/ginny/no-skip
                            return this.skip('MARKETFRONT-49185 Уже были сломаны');
                            // eslint-disable-next-line no-unreachable
                            return testTotalItemsPrice.call(this);
                        },
                    }),
                },
            },
        }),

        makeSuite('Два оффера в корзине, по 2 штуки.', {
            defaultParams: {
                items: [{
                    skuId: cartItemsIds.lensesAirOptix.skuId,
                    offerId: cartItemsIds.lensesAirOptix.offerId,
                    count: 2,
                }, {
                    skuId: cartItemsIds.asusZenFone.skuId,
                    offerId: cartItemsIds.asusZenFone.offerId,
                    count: 2,
                }],
            },
            story: {
                'По умолчанию': {
                    'отображается общая стоимость товаров без скидки в корзине и она верна': makeCase({
                        id: 'bluemarket-2290',
                        test() {
                            return testTotalItemsPrice.call(this);
                        },
                    }),
                },
            },
        })
    ),
});

function testTotalItemsPrice() {
    const itemsBySkuId = keyBy(item => item.skuId, this.params.items);

    const correctOfferPriceWithoutDiscountPromise = this.browser.yaScenario(
        this,
        'deprecatedReportResource.getOffersById',
        this.params.items.map(item => item.offerId),
        this.params.region
    )
        .then(offers => _.map(offers, offer => ({
            price: Number(_.get(offer, 'prices.discount.oldMin', _.get(offer, 'prices.value'))),
            skuId: offer.marketSku,
        })));

    return this.orderInfoTotal.isPriceValueVisible()
        .should.eventually.be.equal(
            true,
            'Информация об общей сумме товаров должна показываться'
        )
        .then(() => Promise.all([
            correctOfferPriceWithoutDiscountPromise,
            this.orderInfoTotal.getPriceValue(),
        ]))
        .then(([reportPrices, totalPrice]) => {
            const itemsReportPrice = _.reduce(reportPrices, (acc, {skuId, price}) => {
                const count = itemsBySkuId[skuId].count;

                return acc + (price * count);
            }, 0);

            return this.expect(totalPrice).to.be.equal(
                itemsReportPrice,
                `Общая стоимость офферов должна быть ${itemsReportPrice}`
            );
        });
}
