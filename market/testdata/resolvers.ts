import type {Offer} from 'shared/bcm/indexator/datacamp';

export const createConfirmPriceOfferResult = (offerId: string): Offer => ({
    identifiers: {offerId},
    price: {
        basic: {
            meta: {},
            binaryPrice: {
                price: {low: 1410065408, high: 2, unsigned: true},
                rate: 'CBRF',
                id: 'RUR',
                refId: 'RUR',
            },
        },
        lastValidPrice: {
            meta: {},
        },
    },
});
