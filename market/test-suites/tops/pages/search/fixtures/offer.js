import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

const offerOptions = {
    cpc: 'testOfferCpc',
    type: 'offer',
    filterState: {
        14871214: ['15266392'],
    },
    orderMinCost: {
        value: 50500,
        currency: 'RUR',
    },
    shop: {
        id: 1,
        slug: 'shop',
        name: 'shop',
    },
    delivery: {
        isAvailable: true,
        isDownloadable: false,
        inStock: false,
        hasPickup: false,
        options: [{
            price: {
                currency: 'RUR',
                value: '0',
                isDeliveryIncluded: false,
                isPickupIncluded: false,
            },
            isDefault: true,
        }],
    },
};

export const offer60days = createOffer(offerOptions, 1);

export default {
    offer60days,
};
