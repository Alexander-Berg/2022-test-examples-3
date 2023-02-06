import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import {productWithPicture, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import {PRICE} from '../kadavrMocks';

const PRODUCT_ROUTE = phoneProductRoute;

const SHOP_INFO = {
    id: 123,
    qualityRating: 4,
    overallGradesCount: 111,
    name: 'test shop',
    slug: 'test-shop',
};
const SECOND_SHOP_INFO = {
    id: 1,
    qualityRating: 4,
    overallGradesCount: 111,
    name: 'test shop',
    slug: 'test-shop',
};

const picture = {
    'entity': 'picture',
    'thumbnails': [{
        'containerWidth': 50,
        'containerHeight': 50,
        'url': '//avatars.mds.yandex.net/get-marketpictesting/1044912/market_NzxnwHmEQUXv6y1nm0pHCA/50x50',
        'width': 50,
        'height': 50,
    }],
};

const buildProductOffersResultsState = () => {
    const offers = [];

    offers.push(createOffer({
        shop: SHOP_INFO,
        prices: PRICE,

        urls: {
            encrypted: '/redir/',
        },
        pictures: [
            picture,
            picture,
        ],

        bundleCount: 3,
    }));
    offers.push(createOffer({
        shop: SECOND_SHOP_INFO,
        prices: PRICE,

        urls: {
            encrypted: '/redir/',
        },
        pictures: [
            picture,
            picture,
        ],

        bundleCount: 3,
    }));

    return mergeReportState([
        productWithPicture,
        ...offers,
        {
            data: {
                search: {
                    total: 1,
                    totalOffers: 1,
                    totalOffersBeforeFilters: 1,
                    totalModels: 0,
                },
            },
        },
    ]);
};

export {
    buildProductOffersResultsState,
    PRODUCT_ROUTE,
    SHOP_INFO,
};
