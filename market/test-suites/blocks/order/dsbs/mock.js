import {checkpoints} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/checkpoints';

export const ORDER_ID = 123456789;
export const SHOP_ORDER_ID = 654321;
export const DELIVERY_SERVICE_ID = 1003390;
export const TRACK_CODE = '12345';

export const order = {
    orderId: ORDER_ID,
    items: [{
        buyerPrice: 2999,
        count: 1,
        offerName: 'Некий dsbs товар',
    }],
    deliveryType: 'DELIVERY',
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
};

export const deliveryServiceInfo = {
    name: 'Beta_Post_Online',
    phones: ['8 (800) 555-55-55'],
    trackOrderSite: 'localhost:8080',
    trackCodeSource: 'DS_TRACK_CODE',
};
