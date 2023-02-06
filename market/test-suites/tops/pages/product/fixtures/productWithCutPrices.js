import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    FILTER_ID,
    cutPriceFilterWithoutNoMatterValue,
    cutPriceFilterValueNewChecked,
    cutPriceFilterValueUsedChecked,
    cutPriceFilterValueUsed,
} from '@self/platform/spec/hermione/fixtures/cutprice';

import {productWithPicture, productWithCutPrice, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import {PRICE} from '../kadavrMocks';


const CUT_PRICE_CONDITION_REASON = 'Помялась коробка';

const PRODUCT_ROUTE = phoneProductRoute;

const SHOP_INFO = {
    id: 123,
    qualityRating: 4,
    overallGradesCount: 111,
    name: 'test shop',
    slug: 'test-shop',
};

const SHOP_INFO2 = {
    id: 124,
    qualityRating: 3,
    overallGradesCount: 53,
    name: 'second test shop',
    slug: 'second-test-shop',
};

const picture = {
    'entity': 'picture',
    'thumbnails':
        [
            {
                'containerWidth': 50,
                'containerHeight': 50,
                'url': '//avatars.mds.yandex.net/get-marketpictesting/1044912/market_NzxnwHmEQUXv6y1nm0pHCA/50x50',
                'width': 50,
                'height': 50,
            },
        ],
};

const buildProductOffersResultsState = (offersCount = 2, filterValue = 'new') => {
    const offers = [];

    for (let i = 0; i < offersCount; i++) {
        offers.push(createOffer({
            shop: i % 2 ? SHOP_INFO2 : SHOP_INFO,
            prices: PRICE,
            isCutPrice: true,
            condition: {
                type: 'like-new',
                reason: CUT_PRICE_CONDITION_REASON,
            },

            urls: {
                encrypted: '/redir/',
            },
            pictures: [
                picture,
                picture,
            ],
        }));
    }

    let cutPriceFilter;

    if (filterValue === 'new') {
        cutPriceFilter = mergeState([
            cutPriceFilterWithoutNoMatterValue,
            cutPriceFilterValueNewChecked,
            cutPriceFilterValueUsed,
        ]);
    } else {
        cutPriceFilter = mergeState([
            cutPriceFilterWithoutNoMatterValue,
            cutPriceFilterValueUsedChecked,
            cutPriceFilterValueUsed,
        ]);
    }

    return mergeReportState([
        productWithPicture,
        ...offers,

        cutPriceFilter,

        {
            data: {
                search: {
                    total: offersCount,
                    totalOffers: offersCount,
                    totalOffersBeforeFilters: offersCount,
                    totalModels: 0,
                },
            },
        },
    ]);
};

const buildProductWithDefaultOffer = () => {
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };

    return mergeState([
        productWithCutPrice,
        dataMixin,
    ]);
};

export {
    buildProductOffersResultsState,
    buildProductWithDefaultOffer,
    CUT_PRICE_CONDITION_REASON,
    PRICE,
    SHOP_INFO,
    PRODUCT_ROUTE,
    FILTER_ID,
};
