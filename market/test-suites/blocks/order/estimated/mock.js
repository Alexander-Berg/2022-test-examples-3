import {checkpoints} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/checkpoints';
import {ORDER_STATUS, ORDER_SUBSTATUS} from '@self/root/src/entities/order';

export const ORDER_ID = 123456789;
export const SHOP_ORDER_ID = 654321;
export const DELIVERY_SERVICE_ID = 1003390;
export const TRACK_CODE = '12345';

export const fullEstimatedOrder = {
    orderId: ORDER_ID,
    items: [{
        buyerPrice: 2999,
        count: 1,
        offerName: 'Некий dsbs товар',
    }],
    deliveryType: 'PENDING',
    delivery: {
        deliveryPartnerType: 'SHOP',
        buyerPrice: 0,
        dates: {
            fromDate: '03-01-2021',
            toDate: '03-01-2021',
        },
        deliveryServiceId: 99,
        parcels: [{
            tracks: [
                {
                    deliveryServiceId: DELIVERY_SERVICE_ID,
                    trackCode: TRACK_CODE,
                    checkpoints,
                },
            ],
        }],
        shipments: [{
            tracks: [
                {
                    deliveryServiceId: DELIVERY_SERVICE_ID,
                    trackCode: '123',
                    checkpoints,
                },
            ],
        }],
        features: [],
        estimated: true,
    },
    address: {
        country: 'Россия',
        city: 'Москва',
        street: 'Аэродромная улица',
        house: '12к2',
        postcode: '125363',
        regionId: 213,
    },
    recipient: {
        phone: '+7 000 025-08-12',
        name: {
            firstName: 'Наталья',
            lastName: 'Кулакова',
        },
    },
    orderCancelPolicy: {
        type: 'time-limit',
        daysForCancel: 3,
        reason: 'unique-order',
        timeUntilExpiration: '2021-01-04',
        notAvailable: false,
    },
    properties: {
        isEstimatedOrder: true,
        isUniqueOrder: true,
    },
};

export const estimatedOrderWithoutCancel = {
    ...fullEstimatedOrder,
    orderCancelPolicy: {
        ...fullEstimatedOrder.orderCancelPolicy,
        notAvailable: true,
    },
};

export const estimatedOrderReady = {
    ...estimatedOrderWithoutCancel,
    delivery: {
        ...estimatedOrderWithoutCancel.delivery,
        estimated: false,
    },
};

export const uniqueOrderReady = {
    ...fullEstimatedOrder,
    delivery: {
        ...fullEstimatedOrder.delivery,
        estimated: false,
    },
    properties: {
        ...fullEstimatedOrder.properties,
        isEstimatedOrder: false,
    },
};

export const orderWithEstimateDleivery = {
    ...fullEstimatedOrder,
    orderCancelPolicy: undefined,
    properties: {},
};

export const orderParams = {
    fulfilment: false,
    rgb: 'WHITE',
    paymentType: 'PREPAID',
    paymentMethod: 'YANDEX',
    paymentStatus: 'HOLD',
    status: ORDER_STATUS.PROCESSING,
    substatus: ORDER_SUBSTATUS.STARTED,
    creationDate: '01-01-2021 12:00:00',
    shopOrderId: SHOP_ORDER_ID,
};
