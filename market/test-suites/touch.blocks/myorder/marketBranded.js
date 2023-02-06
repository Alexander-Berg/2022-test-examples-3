import {
    makeCase,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

import {OrderInfoLine} from '@self/root/src/components/OrderInfo/OrderInfoLine/__pageObject';
import {ORDER_STATUS, ORDER_SUBSTATUS} from '@self/root/src/entities/order';
import {DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import {yandexMarketPickupPoint} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

export default makeSuite('Доставка в брендированный ПВЗ', {
    feature: 'Доставка в брендированный ПВЗ',
    issue: 'MARKETFRONT-53152',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(OrderInfoLine, {
                    root: `${OrderInfoLine.root}[data-auto="deliveryDate"]`,
                    parent: this.orderCard,
                }),
            });

            const order = await this.browser.yaScenario(
                this,
                'checkoutResource.prepareOrder',
                {
                    status: ORDER_STATUS.PROCESSING,
                    substatus: ORDER_SUBSTATUS.SHIPPED,
                    region: this.params.region,
                    orders: [{
                        items: [{skuId: checkoutItemIds.asus.skuId}],
                        deliveryType: 'PICKUP',
                        fulfilment: true,
                        delivery: {
                            type: 'PICKUP',
                            features: [],
                            dates: {
                                fromDate: '23-02-2024',
                                toDate: '23-02-2024',
                                toTime: '12:00',
                            },
                            deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET,
                            outletId: yandexMarketPickupPoint.id,
                        },
                    }],
                }
            );

            if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                const orderId = order?.orders?.[0]?.id;

                return this.browser.yaOpenPage(this.params.pageId, {orderId});
            }

            return this.browser.yaOpenPage(this.params.pageId);
        },

        'Время доставки в брендированный ПВЗ отображается верно': makeCase({
            id: 'bluemarket-4120',
            async test() {
                const expectedOrderStatusText = 'Заказ будет в пункте выдачи в пятницу, 23 февраля к 12:00';
                const expectedDeliveryInfoDateText = 'в пятницу, 23 февраля, к 12:00 доставка Яндекса';

                await this.orderStatus.getDeliveryInfoText()
                    .should.eventually.to.be.equal(expectedOrderStatusText, `Текст должен быть ${expectedOrderStatusText}`);

                await this.deliveryInfo.getContentText()
                    .should.eventually.to.be.equal(
                        expectedDeliveryInfoDateText,
                        `Текст должен быть ${expectedDeliveryInfoDateText}`
                    );
            },
        }),
    },
});
