// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {generateString} from '@self/root/src/utils/string';

// mocks
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as fulfilmentKettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {deliveryPostMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';


export const FREE_DELIVERY_THRESHOLD = 2499;

export function getDsbsCart(params) {
    return getCarts({
        ...params,
        skuMock: dsbs.skuPhoneMock,
        offerMock: dsbs.offerPhoneMock,
        deliveryOptions: [{
            ...deliveryPostMock,
            isExpress: false,
            deliveryPartnerType: 'SHOP',
        }],
    });
}

export function getExpressCart(params) {
    return getCarts({
        ...params,
        skuMock: express.skuExpressMock,
        offerMock: express.offerExpressMock,
        deliveryOptions: [{
            ...deliveryPostMock,
            isExpress: true,
            deliveryPartnerType: 'YANDEX_MARKET',
        }],
    });
}

export function getFulfilmentCart(params) {
    return getCarts({
        ...params,
        skuMock: fulfilmentKettle.skuMock,
        offerMock: fulfilmentKettle.offerMock,
        deliveryOptions: [{
            ...deliveryPostMock,
            isExpress: false,
            deliveryPartnerType: 'YANDEX_MARKET',
        }],
    });
}

function getCarts({
    yaPlus = false,
    more = false,
    number = 1,
    cartIndex = 0,
    skuMock,
    offerMock,
    deliveryOptions,
}) {
    const price = getPriceByOffersNumber(more, number);
    const skuId = generateString();
    const offerId = generateString();
    const newOfferMock = {
        ...offerMock,
        wareId: offerId,
        marketSku: skuId,
        sku: skuId,
        prices: {
            ...offerMock.prices,
            value: price,
            rawValue: price,
        },
    };

    return buildCheckouterBucket({
        cartIndex,
        items: [{
            skuMock: {
                ...skuMock,
                items: [{
                    newOfferMock,
                }],
            },
            offerMock: newOfferMock,
            count: 1,
        }],
        properties: {
            yandexPlusUser: yaPlus,
        },
        deliveryOptions,
    });
}

function getPriceByOffersNumber(more, number) {
    const totalPrice = FREE_DELIVERY_THRESHOLD + (more ? (number * 10) : -(number * 10));

    return Math.floor(totalPrice / number);
}
