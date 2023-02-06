

import {makeCase, makeSuite, mergeSuites} from 'ginny';
import {path} from 'ambar';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import assert from 'assert';

module.exports = makeSuite('Статус заказа', {
    issue: 'BLUEMARKET-10197',
    story: mergeSuites(
        {
            async beforeEach() {
                await createOrder.call(this, {
                    status: this.params.status,
                    substatus: this.params.substatus,
                    deliveryType: this.params.deliveryType,
                    skuId: this.params.skuId,
                    isDropship: this.params.isDropship,
                });
            },
        },
        makeSuite('Заказ типа доставки PICKUP', {
            id: 'bluemarket-3336',
            environment: 'kadavr',
            defaultParams: {
                deliveryType: 'PICKUP',
                skuId: checkoutItemIds.asus.skuId,
                isDropship: false,
            },
            story: makeSuite('Статус заказа PROCESSING.', {
                defaultParams: {
                    status: 'PROCESSING',
                },
                story: mergeSuites({
                    'Блока со статусом заказа нет': makeCase({
                        async test() {
                            assert(this.myOrder, 'PageObject.myOrder must be defined');
                            assert(this.orderStatusNotifier, 'PageObject.orderStatusNotifier must be defined');

                            const exists = await this.myOrder.isExisting(this.orderStatusNotifier.root);
                            return this.expect(exists)
                                .to.be.equal(false, 'Блока со статусом вообще не должно быть');
                        },
                    }),
                }),
            }),
        })
    ),
});

async function createOrder({status, substatus, deliveryType, skuId, isDropship} = {}) {
    const {browser} = this;
    const order = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status,
            substatus,
            fulfilment: !isDropship,
            region: this.params.region,
            orders: [{
                items: [{skuId}],
                deliveryType,
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
        }
    );

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        const orderId = path(['orders', '0', 'id'], order);
        return this.browser.yaOpenPage(this.params.pageId, {orderId});
    }

    return this.browser.yaOpenPage(this.params.pageId);
}
