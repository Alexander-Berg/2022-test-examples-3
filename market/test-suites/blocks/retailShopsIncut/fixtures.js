import {mergeState, createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import {offerExpressMock} from '@self/root/src/spec/hermione/kadavr-mock/report/express';
import retailShopsIncutFixture from '@self/root/src/spec/hermione/kadavr-mock/report/incuts/retailShopsIncut';

const OFFER_COUNT = 10;

export const createState = () => {
    const offerIds = [];

    for (let offerId = 0; offerId < OFFER_COUNT; offerId++) {
        offerIds.push([`offer-${offerId}`]);
    }

    const offerLists = offerIds.reduce((acc, offerId) =>
        mergeState([acc, createOffer(offerExpressMock, offerId)]), {data: {}, collections: {}});

    return mergeState([
        offerLists,
        {
            data: {
                search: {
                    total: OFFER_COUNT,
                    totalOffers: OFFER_COUNT,
                },
                incuts: {
                    results: [retailShopsIncutFixture],
                },
            },
        },
    ]);
};

export const ROUTE_PARAMS = {
    'local-offers-first': 0,
    nid: 54726,
    hid: 198119,
    slug: 'mobilnye-telefony',
    onstock: 1,
};
