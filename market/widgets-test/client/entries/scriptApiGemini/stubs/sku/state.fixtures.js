/**
 * @file Фикстуры для стейта в виджете карточки товара.
 */

import {compose, range} from 'ramda';

import {CURRENCY_CODES} from 'src/main/common/constants/currency';
import {
    createCategoryStub,
    createCityStub,
    createModelFeatureStub,
    createModelSpecificationItemStub,
    createModelStub,
    createOfferStub,
    createSearchResultItemStub,
    createSearchResultStub,
    createSkuStub,
    createVendorStub,
} from 'src/main/common/entities/spec/stubs';
import {
    createFakeDeliveryInfo,
    createFakeDeliveryOption,
    createFakeShop,
    createPrice,
    limitPriceEntityValues,
} from 'src/main/common/specHelpers/entities';
import {createFakePartnerData, createSkuWidget, createState} from 'src/widgets-main/common/specHelpers/state';
import * as PRODUCT_PHOTOS from 'src/widgets-test/client/constants/productPhotos';

import {createEntityLongName, createWarnings} from '../helpers';

const productPhotos = [PRODUCT_PHOTOS.VERTICAL, PRODUCT_PHOTOS.HORIZONTAL, PRODUCT_PHOTOS.SQUARE, null];

const createPriceLimitedMillion = compose(
    createPrice,
    limitPriceEntityValues(1e7),
);

const category = createCategoryStub();

const vendor = createVendorStub();

const cityRegion = createCityStub({id: 1});

export const createInitialState = ({warningCode, warningText, outOfStock = false}) => () => {
    const modelSpecification = createModelSpecificationItemStub({
        features: range(1, 11).map(x =>
            createModelFeatureStub({
                name: `feature_name_${x}`,
                value: `feature_${x}`,
            }),
        ),
    });

    const model = createModelStub({
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

    const sku = createSkuStub({
        id: '1',
        photo: PRODUCT_PHOTOS.SQUARE,
        modelId: model.id,
        warnings: createWarnings({warningCode, warningText}),
    });

    let itemsCount = 1;

    if (outOfStock) {
        itemsCount = 0;
    }

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
        createOfferStub({
            id: String(i + 1),
            sku: sku.id,
            shop: shops[i],
            model,
            name: createEntityLongName(i + 1, `offer_${i + 1}`, '.'),
            price: createPriceLimitedMillion({
                // Для проверки разрядности.
                value: Math.round(9.5 ** (x + 2)),
                // Для проверки скидок.
                base: (x + 1) % 2 && 10 ** (x + 2),
            }),
            // Разные виды/отсутствие фотографии.
            photo: productPhotos[x % 4],
            deliveryInfo: createFakeDeliveryInfo({
                shopRegion: cityRegion,
                userRegion: cityRegion,
                downloadable: false,
                pickup: true,
                carried: true,
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
        }),
    );

    let skusSearchResult;

    if (outOfStock) {
        skusSearchResult = createSearchResultStub({
            items: [createSearchResultItemStub(sku)],
        });
    } else {
        skusSearchResult = createSearchResultStub({
            items: [
                createSearchResultItemStub(sku, {
                    offers: offers.map(offer => offer.id),
                }),
            ],
        });
    }

    const widget = createSkuWidget({
        themeId: 1,
        searchResult: skusSearchResult,
        region: cityRegion,
        currencyCode: CURRENCY_CODES.RUB,
        partner: createFakePartnerData(),
        params: {},
        displayMode: 'desktop',
        themeShowTotalInfoLinks: true,
    });

    let state;

    // Для виджета "Не в продаже".
    if (outOfStock) {
        state = createState({
            widget,
            entities: [category, skusSearchResult, sku, model, vendor],
        });
    } else {
        state = createState({
            widget,
            entities: [category, model, sku, offers, skusSearchResult, shops, vendor],
        });
    }

    return state;
};
