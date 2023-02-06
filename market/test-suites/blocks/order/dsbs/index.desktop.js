import {
    makeCase,
    mergeSuites,
    prepareSuite,
    makeSuite,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_STATUS, ORDER_SUBSTATUS} from '@self/root/src/entities/order';

// Page Objects
import MyOrdersHelper from '@self/root/src/widgets/content/orders/OrderHelper/components/MyOrdersHelper/__pageObject';

// Suites
import OrderInfoSuite from '@self/root/src/spec/hermione/test-suites/blocks/orderInfo';

import {PAYMENT_METHODS_MAP} from '@self/root/src/entities/payment/constants';
import {
    ORDER_ID,
    SHOP_ORDER_ID,
    DELIVERY_SERVICE_ID,
    TRACK_CODE,
    order,
    deliveryServiceInfo,
} from './mock';


module.exports = makeSuite('DSBS. Заказ', {
    feature: 'DSBS',
    environment: 'kadavr',
    id: 'bluemarket-3944',
    issue: 'MARKETFRONT-32913',

    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    myOrdersHelper: () => this.createPageObject(MyOrdersHelper),
                });

                const checkouterCollections = await this.browser.yaScenario(
                    this,
                    'checkoutResource.createKadavrCheckouterOrdersCollections',
                    {
                        orders: [order],
                    },
                    {
                        fulfilment: false,
                        rgb: 'WHITE',
                        paymentType: 'PREPAID',
                        paymentMethod: 'YANDEX',
                        paymentStatus: 'HOLD',
                        status: ORDER_STATUS.PROCESSING,
                        substatus: ORDER_SUBSTATUS.STARTED,
                        creationDate: '01-01-2021 12:00:00',
                        shopOrderId: SHOP_ORDER_ID,
                    },
                    {
                        deliveryServiceInfo: {
                            [DELIVERY_SERVICE_ID]: deliveryServiceInfo,
                        },
                    }
                );
                await this.browser.setState('Checkouter.collections', checkouterCollections);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS);
            },
        },

        prepareSuite(OrderInfoSuite, {
            params: {
                trackingId: `${ORDER_ID}/${SHOP_ORDER_ID}`,
                deliveryAddress: 'Адрес доставки:\nМосква, Аэродромная улица, д. 12к2',
                recipient: 'Получатель:\nКулакова Наталья, тел. +7 000 025-08-12',
                receiptDate: 'Дата получения:\nв воскресенье, 3 января доставка продавца',
                deliveryText: 'Стоимость доставки:\nбесплатно',
                paymentMethod: PAYMENT_METHODS_MAP.YANDEX,
                price: '2 999 ₽, оплачено',
                registrationDate: 'Дата оформления:\n1 января 2021, 12:00',
                status: 'В сборке у продавца',
                trackCode: TRACK_CODE,
                deliveryServiceName: 'Beta_Post_Online',
                deliveryServicePhone: '8 (800) 555-55-55',
                deliveryServiceWebsite: 'localhost:8080',
            },
        }),

        {
            'должен быть виден блок с ответами на вопросы': makeCase({
                async test() {
                    await this.myOrdersHelper
                        .isVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'Блок с ответами на вопросы должен быть на странице'
                        );
                },
            }),
        }
    ),
});
