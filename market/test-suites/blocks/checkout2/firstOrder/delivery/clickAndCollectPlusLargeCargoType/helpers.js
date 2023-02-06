import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    offerMock as largeCargoTypeOfferMock,
    skuMock as largeCargoTypeSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {
    offerMock as farmaOfferMock,
    outletMock as farmaOutletMock,
    skuMock as farmaSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';

export const LARGE_CARGO_TYPE_DELIVERY_PRICE = 549;

export const carts = [
    buildCheckouterBucket({
        cartIndex: 0,
        items: [{
            skuMock: largeCargoTypeSkuMock,
            offerMock: largeCargoTypeOfferMock,
            cargoTypes: largeCargoTypeOfferMock.cargoTypes,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            buyerPrice: LARGE_CARGO_TYPE_DELIVERY_PRICE,
        }],
    }),
    buildCheckouterBucket({
        cartIndex: 1,
        items: [{
            skuMock: farmaSkuMock,
            offerMock: farmaOfferMock,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryPickupMock,
            paymentOptions: [
                paymentOptions.cashOnDelivery,
            ],
            outlets: [
                {id: x5outletMock.id, regionId: 0},
                {id: farmaOutletMock.id, regionId: 0},
            ],
        }],
        outlets: [
            x5outletMock,
            farmaOutletMock,
        ],
    }),
];
