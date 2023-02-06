import {
    mergeSuites,
    prepareSuite,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// Page Objects
import MyOrdersHelper from '@self/root/src/widgets/content/orders/OrderHelper/components/MyOrdersHelper/__pageObject';

// Suites
import OrderInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderInfo';

import {PAYMENT_METHODS_MAP} from '@self/root/src/entities/payment/constants';
import {
    ORDER_ID,
    SHOP_ORDER_ID,
    fullEstimatedOrder,
    estimatedOrderWithoutCancel,
    estimatedOrderReady,
    uniqueOrderReady,
    orderWithEstimateDleivery,
    orderParams,
} from './mock';


module.exports = makeSuite('Заказ с неточной датой доставки', {
    feature: 'estimated',
    environment: 'kadavr',

    defaultParams: {
        trackingId: `${ORDER_ID}/${SHOP_ORDER_ID}`,
        deliveryAddress: 'Адрес доставки:\nМосква, Аэродромная улица, д. 12к2',
        recipient: 'Получатель:\nКулакова Наталья, тел. +7 000 025-08-12',
        deliveryText: 'Стоимость доставки:\nбесплатно',
        paymentMethod: PAYMENT_METHODS_MAP.YANDEX,
        price: '2 999 ₽, оплачено',
        registrationDate: 'Дата оформления:\n1 января 2021, 12:00',
        status: 'В работе у продавца',
        changeDate: false,
    },

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    myOrdersHelper: () => this.createPageObject(MyOrdersHelper),
                });
            },
        },

        prepareSuite(OrderInfoSuite, {
            suiteName: 'Товар под заказ. Отмена доступна. Срок не обговорен.',
            meta: {
                id: 'marketfront-5835',
                issue: 'MARKETFRONT-81068',
            },
            params: {
                receiptDate: 'Дата получения:\nОриентировочно в воскресенье, 3 января\n',
                subStatus: 'Ориентировочная дата доставки — в воскресенье, 3 января. Продавец согласует с вами точную дату и время. Отменить заказ можно до 4 января включительно.',
                cancelOrder: true,
            },
            hooks: {
                async beforeEach() {
                    const checkouterCollections = await this.browser.yaScenario(
                        this,
                        'checkoutResource.createKadavrCheckouterOrdersCollections',
                        {
                            orders: [fullEstimatedOrder],
                        },
                        orderParams
                    );
                    await this.browser.setState('Checkouter.collections', checkouterCollections);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                },
            },
        }),

        prepareSuite(OrderInfoSuite, {
            suiteName: 'Товар под заказ. Отмена не доступна. Срок не обговорен.',
            meta: {
                id: 'marketfront-5851',
                issue: 'MARKETFRONT-81068',
            },
            params: {
                receiptDate: 'Дата получения:\nОриентировочно в воскресенье, 3 января\n',
                subStatus: 'Ориентировочная дата доставки — в воскресенье, 3 января. Продавец согласует с вами точную дату и время.',
                cancelOrder: false,
            },
            hooks: {
                async beforeEach() {
                    const checkouterCollections = await this.browser.yaScenario(
                        this,
                        'checkoutResource.createKadavrCheckouterOrdersCollections',
                        {
                            orders: [estimatedOrderWithoutCancel],
                        },
                        orderParams
                    );
                    await this.browser.setState('Checkouter.collections', checkouterCollections);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                },
            },
        }),

        prepareSuite(OrderInfoSuite, {
            suiteName: 'Товар под заказ. Отмена не доступна. Срок обговорен.',
            meta: {
                id: 'marketfront-5956',
                issue: 'MARKETFRONT-81068',
            },
            params: {
                receiptDate: 'Дата получения:\nв воскресенье, 3 января доставка продавца\n',
                subStatus: 'Заказ будет у вас в воскресенье, 3 января',
                cancelOrder: false,
            },
            hooks: {
                async beforeEach() {
                    const checkouterCollections = await this.browser.yaScenario(
                        this,
                        'checkoutResource.createKadavrCheckouterOrdersCollections',
                        {
                            orders: [estimatedOrderReady],
                        },
                        orderParams
                    );
                    await this.browser.setState('Checkouter.collections', checkouterCollections);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                },
            },
        }),

        prepareSuite(OrderInfoSuite, {
            suiteName: 'Товар под заказ. Отмена доступна. Срок обговорен.',
            params: {
                receiptDate: 'Дата получения:\nв воскресенье, 3 января доставка продавца\n',
                subStatus: 'Заказ будет у вас в воскресенье, 3 января. Отменить заказ можно до 4 января включительно.',
                cancelOrder: true,
            },
            hooks: {
                async beforeEach() {
                    const checkouterCollections = await this.browser.yaScenario(
                        this,
                        'checkoutResource.createKadavrCheckouterOrdersCollections',
                        {
                            orders: [uniqueOrderReady],
                        },
                        orderParams
                    );
                    await this.browser.setState('Checkouter.collections', checkouterCollections);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                },
            },
        }),

        prepareSuite(OrderInfoSuite, {
            suiteName: 'Товар не под заказ. Отмена доступна. Срок не обговорен.',
            meta: {
                id: 'marketfront-5956',
                issue: 'MARKETFRONT-81068',
            },
            params: {
                receiptDate: 'Дата получения:\nОриентировочно в воскресенье, 3 января\n',
                subStatus: 'Ориентировочная дата доставки — в воскресенье, 3 января. Продавец согласует с вами точную дату и время.',
                cancelOrder: true,
                status: 'В сборке у продавца',
            },
            hooks: {
                async beforeEach() {
                    const checkouterCollections = await this.browser.yaScenario(
                        this,
                        'checkoutResource.createKadavrCheckouterOrdersCollections',
                        {
                            orders: [orderWithEstimateDleivery],
                        },
                        orderParams
                    );
                    await this.browser.setState('Checkouter.collections', checkouterCollections);
                    await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
                },
            },
        })
    ),
});
