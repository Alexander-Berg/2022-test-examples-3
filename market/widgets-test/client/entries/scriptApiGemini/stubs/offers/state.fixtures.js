/**
 * @file Фикстуры для стейта в офферном виджете.
 */

import {compose, range} from 'ramda';

import {CURRENCY_CODES} from 'src/main/common/constants/currency';
import {SortType} from 'src/main/common/constants/sort';
import {getOfferModelId} from 'src/main/common/entities';
import {
    createCityStub,
    createSearchResultItemStub,
    createSearchResultStub,
    createVendorStub,
} from 'src/main/common/entities/spec/stubs';
import {
    createFakeCategory,
    createFakeColorFilterValue,
    createFakeDeliveryInfo,
    createFakeDeliveryOption,
    createFakeModel,
    createFakeModelFeature,
    createFakeModelSpecification,
    createFakeOffer,
    createFakePhotoPickerFilter,
    createFakePhotoPickerFilterValue,
    createFakeShop,
    createPrice,
    limitPriceEntityValues,
} from 'src/main/common/specHelpers/entities';
import {createFakeUrl} from 'src/main/common/specHelpers/faker';
import {createFakePartnerData, createOffersWidget, createState} from 'src/widgets-main/common/specHelpers/state';
import * as PRODUCT_PHOTOS from 'src/widgets-test/client/constants/productPhotos';

import {createEntityLongName, createWarnings} from '../helpers';

const productPhotos = [PRODUCT_PHOTOS.VERTICAL, PRODUCT_PHOTOS.HORIZONTAL, PRODUCT_PHOTOS.SQUARE, null];

const createPriceLimitedMillion = compose(
    createPrice,
    limitPriceEntityValues(1e7),
);

const category = createFakeCategory({id: 1});

const vendor = createVendorStub();

const cityRegion = createCityStub({id: 1});

export const createInitialState = ({warningCode, warningText, outOfStock = false}) => () => {
    let itemsCount = 6;

    let colorFilter = createFakePhotoPickerFilter({
        values: [
            createFakePhotoPickerFilterValue({color: '#42aaff', photo: PRODUCT_PHOTOS.HORIZONTAL.url}),
            createFakePhotoPickerFilterValue({color: '#068e5b', photo: PRODUCT_PHOTOS.VERTICAL.url}),
            createFakePhotoPickerFilterValue({color: '#b509aa', photo: PRODUCT_PHOTOS.SQUARE.url}),
        ],
    });
    const offerColorFilter = createFakePhotoPickerFilter({
        values: [createFakeColorFilterValue({color: '#ff231e'})],
    });
    const offerColorFilter2 = createFakePhotoPickerFilter({
        values: [createFakeColorFilterValue({color: '#ffbe00'})],
    });

    if (outOfStock) {
        itemsCount = 0;
        colorFilter = [];
    }

    const params = {
        sort: SortType.Popular,
    };

    const modelSpecification = createFakeModelSpecification({
        features: range(1, 11).map(x =>
            createFakeModelFeature({
                value: `feature_${x}`,
            }),
        ),
    });

    const model = createFakeModel({
        id: 1,
        photo: PRODUCT_PHOTOS.VERTICAL,
        specification: [modelSpecification],
        offersCount: 123,
        opinionsCount: 456,
        rating: 4.5,
        category,
        vendor,
        warnings: createWarnings({warningCode, warningText}),
    });

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
            id: String(i + 1),
            shop: shops[i],
            model,
            name: createEntityLongName(i + 1, `offer_${i + 1}`, '.'),
            price: createPriceLimitedMillion({
                // Для проверки разрядности.
                value: Math.round(9.5 ** (x + 2)),
                // Для проверки скидок.
                base: x % 2 && 10 ** (x + 2),
            }),
            // Разные виды/отсутствие фотографии.
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
                pickupOptions: [
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
            category,
            vendor,
            warnings: x % 2 ? [] : createWarnings({warningCode, warningText}),
            activeFilters: x % 3 === 1 ? [] : [[offerColorFilter, offerColorFilter2][x % 2]],
        }),
    );

    const offersSearchResult = createSearchResultStub({
        items: offers.map(offer => createSearchResultItemStub(offer)),
        offersUrl: createFakeUrl(),
        filters: [colorFilter],
    });

    const modelsSearchResult = createSearchResultStub({
        items: [
            createSearchResultItemStub(model, {
                modelOffers: offersSearchResult,
            }),
        ],
    });

    let widget = createOffersWidget({
        themeId: 1,
        params,
        searchResult: modelsSearchResult,
        region: cityRegion,
        currencyCode: CURRENCY_CODES.RUB,
        partner: createFakePartnerData(),
        displayMode: 'desktop',
        themeShowTotalInfoLinks: true,
        themeShowPromo: true,
    });

    let state = createState({
        widget,
        entities: [category, model, modelsSearchResult, offersSearchResult, offers, shops, vendor],
    });

    // Для виджета "Не в продаже".
    if (outOfStock) {
        itemsCount = 6;
        const alternateModels = range(0, itemsCount).map(x =>
            createFakeModel({
                id: x + 101,
                name: createEntityLongName(x + 1, `alternate_model_${x + 101}`, '.'),
                photo: productPhotos[x % 4],
                specification: [modelSpecification],
                offersCount: Math.round(9.5 ** x),
                opinionsCount: Math.round(9 ** x),
                rating: x % itemsCount,
                category,
                vendor,
                warnings: x % 2 ? [] : createWarnings({warningCode, warningText}),
            }),
        );

        const alternateShops = range(0, itemsCount).map((x, i, xs) =>
            createFakeShop({
                id: i + 1,
                // Для проверки звёздочек.
                rating: x % xs.length,
                // Для проверки плюрализации.
                reviewsCount: x * 100 + (x % xs.length),
                name: createEntityLongName(i + 1, `shop_${i + 1}`, '.'),
            }),
        );

        const alternateOffers = range(0, itemsCount).map((x, i, xs) =>
            createFakeOffer({
                id: String(i + 11),
                shop: alternateShops[i],
                model: alternateModels[i],
                name: createEntityLongName(i + 1, `alternate_offer_${i + 11}`, '.'),
                price: createPriceLimitedMillion({
                    // Для проверки разрядности.
                    value: Math.round(9.5 ** (x + 2)),
                    // Для проверки скидок.
                    base: x % 2 && 10 ** (x + 2),
                }),
                // Разные виды/отсутствие фотографии.
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
                category,
                vendor,
                warnings: x % 2 ? [] : createWarnings({warningCode, warningText}),
            }),
        );

        const alternateOffersSearchResults = alternateModels.reduce((alternateResults, alternateModel) => {
            const alternateModelOffers = alternateOffers.filter(
                alternateOffer => getOfferModelId(alternateOffer) === alternateModel.id,
            );

            alternateResults[alternateModel.id] = createSearchResultStub({
                items: alternateModelOffers.map(offer => createSearchResultItemStub(offer)),
                offersUrl: createFakeUrl(),
            });

            return alternateResults;
        }, {});

        const alternateModelsSearchResult = createSearchResultStub({
            items: alternateModels.map(alternateModel =>
                createSearchResultItemStub(alternateModel, {
                    modelOffers: alternateOffersSearchResults[alternateModel.id],
                }),
            ),
            searchType: 'also_viewed',
        });

        widget = createOffersWidget({
            themeId: 1,
            params,
            alternateSearchResult: alternateModelsSearchResult,
            searchResult: modelsSearchResult,
            searchResultPagination: {
                productsSlice: [0, itemsCount],
                loadedProducts: itemsCount + 1,
            },
            region: cityRegion,
            currencyCode: CURRENCY_CODES.RUB,
            partner: createFakePartnerData(),
            themeShowPromo: true,
        });

        state = createState({
            widget,
            entities: [
                alternateModels,
                alternateOffers,
                alternateShops,
                alternateModelsSearchResult,
                Object.values(alternateOffersSearchResults),
                category,
                modelsSearchResult,
                model,
                offersSearchResult,
                vendor,
            ],
        });
    }

    return state;
};
