import {makeSuite, makeCase} from 'ginny';

import {DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';
import OrderList from '@self/root/src/widgets/content/orders/OrderList/components/View/__pageObject/index.desktop';
import OrderCard from '@self/root/src/widgets/content/orders/OrderCard/components/View/__pageObject/index.desktop';
import OrderHeader from '@self/root/src/components/OrderHeader/__pageObject/index.desktop';
import OrderStatus from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject/index.desktop';
import MyOrderDeliveryInfo from '@self/root/src/components/Orders/DeliveryInfo/__pageObject';
import MoreButton from '@self/root/src/widgets/content/orders/OrderList/components/MoreButton/__pageObject';
import {yandexMarketPickupPoint} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {ORDER_STATUS, ORDER_SUBSTATUS} from '@self/root/src/entities/order';

const ORDER_ID = '0';
const MARKET_BRANDED_ORDER_ID = '1';
const SHOP_ORDER_ID = '829449183';

export default makeSuite('Список заказов.', {
    environment: 'kadavr',
    feature: 'Список заказов.',
    story: {
        async beforeEach() {
            const orders = {};
            orders['0'] = {
                id: ORDER_ID,
                status: ORDER_STATUS.PROCESSING,
                substatus: ORDER_SUBSTATUS.SHIPPED,
                fulfilment: false,
                deliveryType: 'DELIVERY',
                delivery: {
                    type: 'DELIVERY',
                    deliveryPartnerType: 'SHOP',
                    features: [],
                    dates: {
                        fromDate: '23-02-2024',
                        toDate: '24-02-2024',
                    },
                },
                /** Чтобы номер был двойным: с Маркета и магазина */
                shopOrderId: SHOP_ORDER_ID,
            };
            for (let id = 1; id < 21; id++) {
                /** Минимальный набор данных для заказов */
                orders[id] = {
                    id,
                    status: ORDER_STATUS.PROCESSING,
                    substatus: ORDER_SUBSTATUS.SHIPPED,
                    fulfilment: true,
                    deliveryType: 'DELIVERY',
                    delivery: {
                        type: 'DELIVERY',
                        dates: {
                            fromDate: '23-02-2024',
                            toDate: '24-02-2024',
                        },
                        features: [],
                        /** Чтобы номер был обычным: только с Маркета */
                        deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET,
                    },
                };
            }

            orders[MARKET_BRANDED_ORDER_ID] = {
                ...orders[MARKET_BRANDED_ORDER_ID],
                deliveryType: 'PICKUP',
                delivery: {
                    type: 'PICKUP',
                    features: [],
                    dates: {
                        fromDate: '23-02-2024',
                        toDate: '23-02-2024',
                        fromTime: '12:00',
                        toTime: '12:00',
                    },
                    deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET,
                    outletId: yandexMarketPickupPoint.id,
                },
            };

            await this.browser.setState('Checkouter.collections.order', orders);
            await this.setPageObjects({
                ordersList: () => this.createPageObject(OrderList),
                orderCard: () => this.createPageObject(OrderCard, {
                    parent: this.ordersList,
                    root: `${OrderCard.getRootByOrderId(ORDER_ID)}`,
                }),
                orderHeader: () => this.createPageObject(OrderHeader),
                moreButton: () => this.createPageObject(MoreButton),
            });
            await this.browser.yaOpenPage(this.params.pageId);
        },
        'Время доставки в брендированный ПВЗ отображается верно': makeCase({
            issue: 'MARKETPROJECT-53152',
            id: 'bluemarket-4114',
            async test() {
                await this.setPageObjects({
                    marketBrandedOrderCard: () => this.createPageObject(OrderCard, {
                        parent: this.ordersList,
                        root: `${OrderCard.root}[data-order-id="${MARKET_BRANDED_ORDER_ID}"]`,
                    }),
                    marketBrandedOrderStatus: () => this.createPageObject(OrderStatus, {
                        parent: this.marketBrandedOrderCard,
                    }),
                    marketBrandedDeliveryInfo: () => this.createPageObject(MyOrderDeliveryInfo, {
                        parent: this.marketBrandedOrderCard,
                    }),
                });

                const expectedOrderStatusText = 'Заказ будет в пункте выдачи в пятницу, 23 февраля';
                const expectedDeliveryInfoDateText = 'в пятницу, 23 февраля к 12:00 доставка Яндекса';

                await this.marketBrandedOrderStatus.getDeliveryInfoText()
                    .should.eventually.to.be.equal(expectedOrderStatusText, `Текст должен быть ${expectedOrderStatusText}`);

                await this.marketBrandedDeliveryInfo.getOrderDeliveryDateTimeContentText()
                    .should.eventually.to.be.equal(
                        expectedDeliveryInfoDateText,
                        `Текст должен быть ${expectedDeliveryInfoDateText}`
                    );
            },
        }),
        'Заказ Дропшип отображается с номером на Маркете и номером в магазине': makeCase({
            issue: 'MARKETFRONT-21766',
            id: 'marketfront-21766',
            async test() {
                const expectedText = `Заказ № ${ORDER_ID}/${SHOP_ORDER_ID}`;

                await this.orderHeader.getOrderTrackingIdText()
                    .should.eventually.to.contain(expectedText, `Текст должен содержать ${expectedText}`);
            },
        }),
        'Кнопка "Показать ещё" прячется при переходе на последнюю страницу архивных заказов': makeCase({
            issue: 'BLUEMARKET-6508',
            id: 'bluemarket-1455',
            async test() {
                await this.moreButton.click();
                await this.ordersList.waitForPageVisible(2);

                await this.moreButton.click();
                await this.ordersList.waitForPageVisible(3);

                await this.moreButton.click();

                await this.moreButton.waitForVisible(true);
            },
        }),
        'Клик на кнопку "Показать ещё" должен загрузить следующую страницу': makeCase({
            issue: 'BLUEMARKET-6508',
            id: 'bluemarket-1455',
            async test() {
                await this.moreButton.click();

                return this.ordersList.waitForPageVisible(2);
            },
        }),
    },
});
