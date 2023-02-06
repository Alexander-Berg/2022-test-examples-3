// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {generateString} from '@self/root/src/utils/string';

// mocks
import * as express from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';


export const FREE_DELIVERY_THRESHOLD = 2499;

export function getExpressCart() {
    return getCarts({
        skuMock: express.skuExpressMock,
        offerMock: express.offerExpressMock,
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            isExpress: true,
            deliveryPartnerType: 'YANDEX_MARKET',
        }],
    });
}

function getCarts({
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
        deliveryOptions,
    });
}

function getPriceByOffersNumber(more, number) {
    const totalPrice = FREE_DELIVERY_THRESHOLD + (more ? (number * 10) : -(number * 10));

    return Math.floor(totalPrice / number);
}
