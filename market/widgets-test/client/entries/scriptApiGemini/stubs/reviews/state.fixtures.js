/**
 * @file Фикстуры для стейта в виджете с отзывами.
 */

import {compose, range} from 'ramda';

import {CURRENCY_CODES} from 'src/main/common/constants/currency';
import {OpinionAuthorVisibility, OpinionUsageTime} from 'src/main/common/entities';
import {
    createCityStub,
    createOpinionAuthorStub,
    createOpinionStub,
    createSearchResultItemStub,
    createSearchResultStub,
    createVendorStub,
} from 'src/main/common/entities/spec/stubs';
import {
    createFakeCategory,
    createFakeColorFilter,
    createFakeColorFilterValue,
    createFakeDeliveryInfo,
    createFakeDeliveryOption,
    createFakeModel,
    createFakeModelFeature,
    createFakeModelSpecification,
    createFakeOffer,
    createFakeShop,
    createPrice,
    limitPriceEntityValues,
} from 'src/main/common/specHelpers/entities';
import {createFakeUrl} from 'src/main/common/specHelpers/faker';
import {createFakePartnerData, createReviewsWidget, createState} from 'src/widgets-main/common/specHelpers/state';
import * as PRODUCT_PHOTOS from 'src/widgets-test/client/constants/productPhotos';

import {createEntityLongName, createWarnings} from '../helpers';

const productPhotos = [PRODUCT_PHOTOS.VERTICAL, PRODUCT_PHOTOS.HORIZONTAL, PRODUCT_PHOTOS.SQUARE, null];
const usageTime = Object.values(OpinionUsageTime);

const createPriceLimitedMillion = compose(
    createPrice,
    limitPriceEntityValues(1e7),
);

const cityRegion = createCityStub({id: 1});

const vendor = createVendorStub();

export const createInitialState = ({warningCode, warningText, opinionsCount = 2}) => () => {
    const itemsCount = 6;

    const categories = range(0, 3).map(x => createFakeCategory({id: x}));

    const modelColorFilter = createFakeColorFilter({
        values: [
            createFakeColorFilterValue({color: '#42aaff'}),
            createFakeColorFilterValue({color: '#068e5b'}),
            createFakeColorFilterValue({color: '#b509aa'}),
        ],
    });

    const modelSpecification = range(0, 3).map(x =>
        createFakeModelSpecification({
            name: `common_feature_${x + 1}`,
            features: range(1, itemsCount).map(j =>
                createFakeModelFeature({
                    name: `feature_name_${j}`,
                    value: createEntityLongName(j + 1, `feature_${x + 1}_${j}`, ' '),
                }),
            ),
        }),
    );

    const model = createFakeModel({
        id: 1,
        photo: PRODUCT_PHOTOS.VERTICAL,
        specification: modelSpecification,
        offersCount: 123,
        opinionsCount: 456,
        rating: 4.5,
        filters: [modelColorFilter],
        category: categories[0],
        vendor,
        specificationUrl: createFakeUrl(),
        warnings: createWarnings({warningCode, warningText}),
    });

    const opinions = range(0, opinionsCount).map((x, i) =>
        createOpinionStub({
            id: i + 1,
            modelId: model.id,
            grade: (i % 5) + 1,
            pros: {
                text: createEntityLongName((x + 1) * ((x + 1) % 2) * 3, `opinion_pros_text_${x + 1} `, ' '),
            },
            cons: {
                text: createEntityLongName((x + 1) * ((x + 1) % 3) * 3, `opinion_cons_text_${x + 1} `, ''),
                url: x * (x % 3) === 0 ? createFakeUrl() : null,
            },
            text: {
                text: createEntityLongName(x * (x % 3) * 3, `opinion_text_${x + 1} `, ' '),
                url: createFakeUrl(),
            },
            author: createOpinionAuthorStub({
                visibility: OpinionAuthorVisibility.Name,
                name: `Test Name ${x + 1}`,
            }),
            region: cityRegion,
            usageTime: usageTime[x % 4],
            date:
                Date.now() -
                1000 * 60 * 60 * 24 * x -
                1000 * 60 * 60 * 24 * 30 * x -
                1000 * 60 * 60 * 24 * 30 * 12 * (x % 2),
        }),
    );

    const shops = range(0, itemsCount).map((x, i, xs) =>
        createFakeShop({
            id: i + 1,
            // Для проверки звёздочек.
            rating: x % xs.length,
            // Для проверки плюрализации.
            reviewsCount: x * 100 + (x % xs.length),
            name: createEntityLongName(i + 1, `shop_${i + 1}`, '.'),
        }),
    );

    const offers = range(0, itemsCount).map((x, i, xs) =>
        createFakeOffer({
            id: String(i + 10),
            shop: shops[i],
            price: createPriceLimitedMillion({
                // Для проверки разрядности.
                value: Math.round(9.5 ** (x + 2)),
                // Для проверки скидок
                base: x % 2 && 10 ** (x + 2),
            }),
            photo: productPhotos[x % 4],
            deliveryInfo: createFakeDeliveryInfo({
                shopRegion: cityRegion,
                userRegion: cityRegion,
                downloadable: x === 0,
                pickup: Boolean(x % 2),
                carried: Boolean(x % 3),
                options: [
                    createFakeDeliveryOption({
                        price: createPriceLimitedMillion({
                            // Для проверки разрядности.
                            value: 10 ** (x + 2),
                        }),
                        daysFrom: xs.length - x,
                        daysTo: xs.length + 1 - x,
                    }),
                ],
            }),
            category: [categories[i % 3]],
            model,
            warnings: x % 2 ? [] : createWarnings({warningCode, warningText}),
        }),
    );

    const offersSearchResult = createSearchResultStub({
        items: offers.map(offer => createSearchResultItemStub(offer)),
        offersUrl: createFakeUrl(),
    });

    const opinionsSearchResult = createSearchResultStub({
        items: opinions.map(opinion => createSearchResultItemStub(opinion)),
        opinionAddUrl: createFakeUrl(),
    });

    const modelsSearchResult = createSearchResultStub({
        items: [
            createSearchResultItemStub(model, {
                modelOffers: offersSearchResult,
                modelOpinions: opinionsSearchResult,
            }),
        ],
    });

    const widget = createReviewsWidget({
        themeId: 1,
        searchResult: modelsSearchResult,
        params: {},
        region: cityRegion,
        currencyCode: CURRENCY_CODES.RUB,
        partner: createFakePartnerData(),
        themeShowTotalInfoLinks: true,
        themeShowPromo: true,
    });

    return createState({
        widget,
        entities: [
            categories,
            model,
            modelsSearchResult,
            offersSearchResult,
            opinionsSearchResult,
            offers,
            opinions,
            shops,
            vendor,
        ],
    });
};
