/**
 * @file Фикстуры для стейта в модельном виджете.
 */

import {compose, head, range} from 'ramda';

import {CURRENCY_CODES} from 'src/main/common/constants/currency';
import {getOfferModelId, getSearchResultOfferItems} from 'src/main/common/entities';
import {
    createCityStub,
    createSearchResultItemStub,
    createSearchResultStub,
    createVendorStub,
} from 'src/main/common/entities/spec/stubs';
import {
    createFakeCategory,
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
import {createFakePartnerData, createModelWidget, createState} from 'src/widgets-main/common/specHelpers/state';
import * as PRODUCT_PHOTOS from 'src/widgets-test/client/constants/productPhotos';

import {createEntityLongName, createWarnings} from '../helpers';

const productPhotos = [PRODUCT_PHOTOS.VERTICAL, PRODUCT_PHOTOS.HORIZONTAL, PRODUCT_PHOTOS.SQUARE, null];

const createPriceLimitedMillion = compose(
    createPrice,
    limitPriceEntityValues(1e7),
);

const cityRegion = createCityStub({id: 1});

const vendor = createVendorStub();

const createItems = ({models, offers, offersSearchResults, itemsCount}) => {
    const items = models.map(model =>
        createSearchResultItemStub(model, {
            modelOffers: offersSearchResults[model.id],
            defaultOffer: head(getSearchResultOfferItems(offersSearchResults[model.id])),
        }),
    );

    for (let i = models.length; i < itemsCount; i++) {
        items.push(createSearchResultItemStub(offers[i]));
    }

    return items;
};

export const createInitialState = ({themeRows, warningCode, warningText, offersCount = 0, modelsCount = 10}) => () => {
    const itemsCount = offersCount + modelsCount;

    const categories = range(0, 3).map(x => createFakeCategory({id: x}));

    const modelSpecification = createFakeModelSpecification({
        features: range(1, 11).map(x =>
            createFakeModelFeature({
                value: `feature_${x}`,
            }),
        ),
    });

    const models = range(0, modelsCount).map((x, i, xs) =>
        createFakeModel({
            id: i + 1,
            // Разные виды/отсутствие фотографии.
            photo: productPhotos[x % 4],
            specification: [modelSpecification],
            offersCount: 123,
            // Для проверки звёздочек.
            rating: x % xs.length,
            // Для проверки плюрализации.
            opinionsCount: x * 100 + (x % xs.length),
            category: categories[i % 3],
            name: createEntityLongName(i + 1, `model_${i + 1}`, '/'),
            warnings: x % 2 ? [] : createWarnings({warningCode, warningText}),
            vendor,
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
            name: createEntityLongName(i + 1, `offer_${i + 1}`, '.'),
            price: createPriceLimitedMillion({
                // Для проверки разрядности.
                value: Math.round(9.5 ** (x + 2)),
                // Для проверки скидок.
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
            category: categories[i % 3],
            model: models[i] || null,
            warnings: x % 2 ? [] : createWarnings({warningCode, warningText}),
            vendor,
        }),
    );

    const offersSearchResults = models.reduce((results, model) => {
        const modelOffers = offers.filter(offer => getOfferModelId(offer) === model.id);

        results[model.id] = createSearchResultStub({
            items: modelOffers.map(offer => createSearchResultItemStub(offer)),
            offersUrl: createFakeUrl(),
        });

        return results;
    }, {});

    const modelsSearchResult = createSearchResultStub({
        items: createItems({models, offers, offersSearchResults, itemsCount}),
        redirectUrl: createFakeUrl(),
    });

    const widget = createModelWidget({
        themeId: 1,
        themeRows,
        searchResult: modelsSearchResult,
        params: {
            text: 'model',
        },
        searchResultPagination: {
            productsSlice: [0, itemsCount],
            loadedProducts: itemsCount + 1,
        },
        region: cityRegion,
        currencyCode: CURRENCY_CODES.RUB,
        partner: createFakePartnerData(),
        displayMode: 'desktop',
        themeShowTotalInfoLinks: true,
        themeShowPromo: true,
    });

    return createState({
        widget,
        entities: [Object.values(offersSearchResults), modelsSearchResult, categories, models, offers, shops, vendor],
    });
};
