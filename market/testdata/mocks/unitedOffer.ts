import {assocPath, clone, dissocPath} from 'ramda';

import type {GetUnitedOffersResponse} from 'shared/bcm/indexator/datacamp/types';
import {CARD_STATUS} from 'shared/constants/datacamp';

const COMMON_OFFER_MOCK: GetUnitedOffersResponse = {
    offers: [
        {
            basic: {
                identifiers: {
                    offerId: '123',
                    extra: {
                        shopSku: '123',
                        wareMd5: 'wareMd5',
                    },
                    businessId: 10441467,
                },
                meta: {
                    tsCreated: {
                        nanos: 335000000,
                        seconds: {low: 1613046624, high: 0, unsigned: false},
                    },
                    scope: 1,
                },
                status: {
                    incompleteWizard: {
                        meta: {
                            source: 5,
                            timestamp: {seconds: {low: 1623254143, high: 0, unsigned: false}, nanos: 601503000},
                        },
                        flag: false,
                    },
                    consistency: {mbocConsistency: true},
                    version: {
                        offerVersion: {counter: {low: 1691, high: 0, unsigned: true}},
                        ucDataVersion: {counter: {low: 278, high: 0, unsigned: true}},
                        actualContentVersion: {counter: {low: 770, high: 0, unsigned: true}},
                    },
                    fieldsPlacementVersion: {
                        meta: {
                            source: 5,
                            timestamp: {seconds: {low: 1623254143, high: 0, unsigned: false}, nanos: 601503000},
                        },
                        value: 1,
                    },
                },
                content: {
                    partner: {
                        original: {
                            name: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1623254143, high: 0, unsigned: false},
                                        nanos: 601503000,
                                    },
                                },
                                value: 'тест ав 42',
                            },
                            description: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            typePrefix: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            vendor: {
                                meta: {
                                    source: 13,
                                    timestamp: {
                                        seconds: {low: 1613046814, high: 0, unsigned: false},
                                        nanos: 743000000,
                                    },
                                },
                                value: '-',
                            },
                            model: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            vendorCode: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            barcode: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1619605358, high: 0, unsigned: false},
                                        nanos: 141000000,
                                    },
                                },
                                value: ['7455345460003'],
                            },
                            offerParams: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            groupId: {
                                meta: {
                                    source: 13,
                                    timestamp: {
                                        seconds: {low: 1613046814, high: 0, unsigned: false},
                                        nanos: 743000000,
                                    },
                                },
                                value: 0,
                            },
                            downloadable: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                flag: false,
                            },
                            adult: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                flag: false,
                            },
                            age: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            url: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                value:
                                    'https://partner-front--marketpartner-21980-unitted-test.demofslb.market.yandex.ru/shop/1001055071/assortment/offer-card?offerId=test-av-42&wizardStep=TERMS_OF_PLACEMENT',
                            },
                            expiry: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                validityPeriod: {},
                            },
                            countryOfOrigin: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1619605358, high: 0, unsigned: false},
                                        nanos: 141000000,
                                    },
                                },
                                value: ['3'],
                            },
                            weight: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1619605358, high: 0, unsigned: false},
                                        nanos: 141000000,
                                    },
                                },
                                grams: {low: 3000, high: 0, unsigned: true},
                            },
                            dimensions: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1619605358, high: 0, unsigned: false},
                                        nanos: 141000000,
                                    },
                                },
                                lengthMkm: {low: 30000, high: 0, unsigned: true},
                                widthMkm: {low: 30000, high: 0, unsigned: true},
                                heightMkm: {low: 30000, high: 0, unsigned: true},
                            },
                            category: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                id: {low: 1910377173, high: 0, unsigned: true},
                                name:
                                    'Все товары/Бытовая техника/Климатическая техника/Кондиционеры/Mitsubishi Electric',
                                parentId: {low: 0, high: 0, unsigned: true},
                                pathCategoryIds: '1910377173',
                                pathCategoryNames:
                                    'Все товары/Бытовая техника/Климатическая техника/Кондиционеры/Mitsubishi Electric',
                                businessId: 10441467,
                            },
                            groupName: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            lifespan: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                serviceLifePeriod: {},
                            },
                            tnVedCode: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                            manufacturer: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                        },
                        originalTerms: {
                            sellerWarranty: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                warrantyPeriod: {},
                            },
                            boxCount: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                            },
                        },
                        marketSpecificContent: {
                            parameterValues: {
                                meta: {
                                    source: 13,
                                    timestamp: {seconds: {low: 1613046814, high: 0, unsigned: false}, nanos: 743000000},
                                },
                                parameterValues: [
                                    {
                                        valueSource: 1,
                                        paramId: {low: 13887626, high: 0, unsigned: false},
                                        value: {valueType: 2, optionId: 13887686, strValue: 'белый'},
                                    },
                                    {
                                        valueSource: 1,
                                        paramId: {low: 14871214, high: 0, unsigned: false},
                                        value: {valueType: 2, optionId: 14899397, strValue: 'белый'},
                                    },
                                ],
                            },
                            rating: {
                                currentRating: 15,
                                expectedRating: 30,
                            },
                        },
                    },
                    market: {
                        enrichedOffer: {
                            categoryId: 90463,
                            modelId: 0,
                            classificationTypeValue: 0,
                            clusterCreatedTimestamp: {low: 0, high: 0, unsigned: false},
                            duplicateOfferGroupId: {low: 0, high: 0, unsigned: false},
                            matchedTypeValue: 2,
                            marketSkuId: {low: -1, high: -1, unsigned: false},
                        },
                        marketCategory: 'Автомобильные компрессоры',
                        // @ts-expect-error(TS2322) неверная типизация proto
                        categoryId: 90463,
                        productName: '',
                        realUcVersion: {counter: {low: 278, high: 0, unsigned: true}},
                    },
                    binding: {
                        partner: {
                            meta: {
                                source: 13,
                                timestamp: {seconds: {low: 1613046814, high: 0, unsigned: false}, nanos: 743000000},
                            },
                            marketCategoryId: 90463,
                        },
                        approved: {
                            meta: {
                                source: 7,
                                timestamp: {seconds: {low: 1624106873, high: 0, unsigned: false}, nanos: 152470000},
                            },
                            marketCategoryId: 90463,
                            marketCategoryName: 'Автомобильные компрессоры',
                        },
                        ucMapping: {
                            meta: {timestamp: {seconds: {low: 1620782982, high: 0, unsigned: false}, nanos: 0}},
                            marketCategoryId: 90463,
                            marketModelId: {low: 0, high: 0, unsigned: false},
                            marketSkuId: {low: -1, high: -1, unsigned: false},
                            marketCategoryName: 'Автомобильные компрессоры',
                            marketModelName: '',
                            marketSkuName: '',
                        },
                    },
                    status: {
                        result: {
                            meta: {
                                source: 8,
                                timestamp: {seconds: {low: 1622738700, high: 0, unsigned: false}, nanos: 0},
                            },
                            cardStatus: 7,
                        },
                        contentSystemStatus: {
                            meta: {
                                source: 7,
                                timestamp: {seconds: {low: 1624106873, high: 0, unsigned: false}, nanos: 152470000},
                            },
                            cpcState: 6,
                            cpaState: 9,
                            allowCategorySelection: false,
                            allowModelSelection: true,
                            allowModelCreateUpdate: true,
                            modelBarcodeRequired: false,
                            activeError: [
                                {
                                    code: 'mboc.error.barcode-required',
                                    level: 3,
                                    params: [
                                        {
                                            name: 'offerId',
                                            value: '123',
                                        },
                                    ],
                                    text:
                                        'Добавьте штрихкод производителя — это обязательный параметр. Допустимые форматы: EAN-13, EAN-8, UPC-A, UPC-E, Code 128. Если штрихкодов несколько, укажите их через запятую.',
                                },
                            ],
                            statusContentVersion: {counter: {low: 769, high: 0, unsigned: true}},
                            partnerMappingStatus: {
                                timestamp: {seconds: {low: 1613046814, high: 0, unsigned: false}, nanos: 743000000},
                                skuMappingState: 0,
                                modelMappingState: 0,
                                categoryMappingState: 2,
                            },
                        },
                    },
                },
                pictures: {
                    partner: {
                        original: {
                            meta: {
                                source: 13,
                                timestamp: {seconds: {low: 1613046814, high: 0, unsigned: false}, nanos: 743000000},
                            },
                            source: [
                                {
                                    url: 'datacamp.market.yandex.ru/pictures/10747577/QgFtwS9HK5K-Oe5_qY5U4Q',
                                    source: 1,
                                },
                            ],
                        },
                        actual: {
                            'datacamp.market.yandex.ru/pictures/10747577/QgFtwS9HK5K-Oe5_qY5U4Q': {
                                id: '65aEfal_Cbn7aK7xQN9rMA',
                                original: {
                                    url:
                                        '//avatars.mds.yandex.net/get-marketpictesting/1368321/market_65aEfal_Cbn7aK7xQN9rMA/orig',
                                    width: 3648,
                                    height: 3648,
                                    containerWidth: 3648,
                                    containerHeight: 3648,
                                },
                                thumbnails: [],
                                meta: {timestamp: {seconds: {low: 1614844284, high: 0, unsigned: false}, nanos: 0}},
                                namespace: 'marketpictesting',
                                status: 1,
                            },
                        },
                    },
                },
            },
            service: {
                10441453: {
                    identifiers: {
                        shopId: 10441453,
                        offerId: '123',
                        extra: {shopSku: '123', wareMd5: 'wareMd5'},
                        businessId: 10441467,
                    },
                    meta: {
                        rgb: 2,
                        tsCreated: {
                            seconds: {low: 1619605358, high: 0, unsigned: false},
                            nanos: 178000000,
                        },
                        scope: 2,
                    },
                    status: {
                        disabled: [
                            {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1623254143, high: 0, unsigned: false},
                                        nanos: 601503000,
                                    },
                                },
                                flag: false,
                            },
                            {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1623254143, high: 0, unsigned: false},
                                        nanos: 601503000,
                                    },
                                },
                                flag: true,
                            },
                        ],
                        publishByPartner: 1,
                        result: 8,
                        version: {
                            offerVersion: {counter: {low: 718, high: 0, unsigned: true}},
                            ucDataVersion: {counter: {low: 1, high: 0, unsigned: true}},
                            actualContentVersion: {counter: {low: 2, high: 0, unsigned: true}},
                        },
                        originalCpa: {
                            meta: {
                                source: 5,
                                timestamp: {seconds: {low: 1623254143, high: 0, unsigned: false}, nanos: 601503000},
                            },
                            flag: true,
                        },
                        unitedCatalog: {
                            meta: {
                                source: 4,
                                timestamp: {seconds: {low: 1619605358, high: 0, unsigned: false}, nanos: 178000000},
                            },
                            flag: true,
                        },
                    },
                    content: {
                        partner: {
                            original: {
                                url: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                    value:
                                        'https://partner-front--marketpartner-21980-unitted-test.demofslb.market.yandex.ru/shop/1001055071/assortment/offer-card?offerId=test-av-42&wizardStep=TERMS_OF_PLACEMENT',
                                },
                            },
                            actual: {
                                url: {
                                    meta: {
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                    value:
                                        'https://partner-front--marketpartner-21980-unitted-test.demofslb.market.yandex.ru/shop/1001055071/assortment/offer-card?offerId=test-av-42&wizardStep=TERMS_OF_PLACEMENT',
                                },
                            },
                            originalTerms: {
                                supplyQuantity: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                },
                                supplyPlan: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                    value: 2,
                                },
                                transportUnitSize: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                },
                                supplyWeekdays: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                },
                                partnerDeliveryTime: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                },
                            },
                        },
                    },
                    price: {
                        basic: {
                            meta: {
                                source: 5,
                                timestamp: {
                                    seconds: {low: 1623254143, high: 0, unsigned: false},
                                    nanos: 601503000,
                                },
                            },
                            binaryPrice: {price: {low: -884901888, high: 2, unsigned: true}},
                            vat: 7,
                        },
                        enableAutoDiscounts: {
                            meta: {
                                source: 5,
                                timestamp: {
                                    seconds: {low: 1623254143, high: 0, unsigned: false},
                                    nanos: 601503000,
                                },
                            },
                            flag: false,
                        },
                        originalPriceFields: {
                            vat: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1623254143, high: 0, unsigned: false},
                                        nanos: 601503000,
                                    },
                                },
                                value: 7,
                            },
                        },
                    },
                },
            },
        },
    ],
};

export const UNITED_OFFER_MOCK: GetUnitedOffersResponse = clone(COMMON_OFFER_MOCK);

const shopCategory = assocPath(
    ['offers', 0, 'basic', 'content', 'partner', 'original', 'category', 'id'],
    1039,
    UNITED_OFFER_MOCK,
);

const hasCardMarketStatus = assocPath(
    ['offers', 0, 'basic', 'content', 'status', 'result', 'cardStatus'],
    CARD_STATUS.HAS_CARD_MARKET,
    shopCategory,
);

export const UNITED_OFFER_NOT_EDITABLE_MOCK: GetUnitedOffersResponse = hasCardMarketStatus;

const hasCardPartnerStatus = assocPath(
    ['offers', 0, 'basic', 'content', 'status', 'result', 'cardStatus'],
    CARD_STATUS.HAS_CARD_PARTNER,
    shopCategory,
);

const hasCardPartnerStatusWithoutCurrentRating = dissocPath<GetUnitedOffersResponse>(
    ['offers', 0, 'basic', 'content', 'partner', 'marketSpecificContent', 'rating', 'currentRating'],
    hasCardPartnerStatus,
);

export const UNITED_OFFER_EDITABLE_MOCK: GetUnitedOffersResponse = hasCardPartnerStatusWithoutCurrentRating;

export const UNITED_OFFER_EDITABLE_MOCK_LOW: GetUnitedOffersResponse = assocPath(
    ['offers', 0, 'basic', 'content', 'partner', 'marketSpecificContent', 'rating', 'currentRating'],
    10,
    UNITED_OFFER_EDITABLE_MOCK,
);

export const UNITED_OFFER_EDITABLE_MOCK_MID: GetUnitedOffersResponse = assocPath(
    ['offers', 0, 'basic', 'content', 'partner', 'marketSpecificContent', 'rating', 'currentRating'],
    33,
    UNITED_OFFER_EDITABLE_MOCK_LOW,
);

export const UNITED_OFFER_EDITABLE_MOCK_HIGH: GetUnitedOffersResponse = assocPath(
    ['offers', 0, 'basic', 'content', 'partner', 'marketSpecificContent', 'rating', 'currentRating'],
    80,
    UNITED_OFFER_EDITABLE_MOCK_MID,
);
