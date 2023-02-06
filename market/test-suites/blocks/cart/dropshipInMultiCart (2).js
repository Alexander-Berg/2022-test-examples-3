import {
    makeSuite,
    makeCase,
    mergeSuites,
} from 'ginny';

import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {deliveryPickupMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';
import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';

import OrderInfoAbstractOrderItem from '@self/root/src/components/TotalDeliveryInfo/Items/AbstractOrderItem/__pageObject';
import TotalDeliveryInfo from '@self/root/src/components/TotalDeliveryInfo/__pageObject';
import DeliveryRemainder from '@self/root/src/components/DeliveryRemainder/__pageObject';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

const DROPSHIP_COUNT = 2;
const REGULAR_COUNT = 1;
/**
 * Тесты на дропшип с другими товарами на корзине.
 * Тестируем на тестинге, т.к. много интеграции.
 * @param {PageObject.CartOrderInfo} orderInfo
 * @param {PageObject.CartGroup} cartGroup
 */
export default makeSuite('Дропшип + обычные товары.', {
    id: 'bluemarket-2673',
    issue: 'BLUEMARKET-6896',
    feature: 'Дропшип',
    defaultParams: {
        items: [{
            offerId: cartItemsIds.dropship.offerId,
            skuId: cartItemsIds.dropship.skuId,
            count: DROPSHIP_COUNT,
        }, {
            skuId: cartItemsIds.asusZenFone.skuId,
            offerId: cartItemsIds.asusZenFone.offerId,
            count: REGULAR_COUNT,
        }],
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderInfoTotal: () => this.createPageObject(OrderTotal),
                    orderInfoDeliveryItem: () => this.createPageObject(
                        OrderInfoAbstractOrderItem,
                        {
                            parent: this.orderInfo,
                            root: `${OrderInfoAbstractOrderItem.root}${OrderInfoAbstractOrderItem.delivery}`,
                        }
                    ),
                    deliveryRemainder: () => this.createPageObject(DeliveryRemainder),
                    totalDeliveryInfo: () => this.createPageObject(TotalDeliveryInfo),
                });
            },
        },

        makeSuite('В саммари.', {
            environment: 'kadavr',
            defaultParams: {
                carts: [buildCheckouterBucket({
                    cartIndex: 0,
                    items: [{
                        skuMock: farma.skuMock,
                        offerMock: farma.offerMock,
                        count: DROPSHIP_COUNT,
                    }],
                    deliveryOptions: [deliveryPickupMock],
                }), buildCheckouterBucket({
                    cartIndex: 1,
                    shopId: 2,
                    warehouseId: 2,
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: REGULAR_COUNT,
                    }],
                })],
            },
            story: {
                async beforeEach() {
                    const testState = await this.browser.yaScenario(
                        this,
                        prepareMultiCartState,
                        this.params.carts
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

                'Виден пункт "Товары" с общим количеством всех товаров (обычные + дропшип)': makeCase({
                    id: 'bluemarket-2673',
                    issue: 'BLUEMARKET-6896',
                    async test() {
                        const totalCount = DROPSHIP_COUNT + REGULAR_COUNT;

                        return this.orderInfoTotal.getItemsCount()
                            .should.eventually.to.be.equal(
                                totalCount,
                                `Общее количество товаров должно быть ${totalCount}`
                            );
                    },
                }),
            },
        })
    ),
});
