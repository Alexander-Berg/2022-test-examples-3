import type {GetUnitedOffersResponse, Mapping} from 'shared/bcm/indexator/datacamp/types';

export const approvedWithoutId: Mapping = {
    meta: {
        source: 7,
        timestamp: {seconds: {low: 1624106873, high: 0, unsigned: false}, nanos: 152470000},
    },
    marketCategoryId: 91491,
    marketCategoryName: 'Мобильные телефоны',
    marketModelName: 'Apple iPhone Xr 256GB',
};

export const ucMappingWithoutId: Mapping = {
    meta: {timestamp: {seconds: {low: 1620782982, high: 0, unsigned: false}, nanos: 0}},
    marketCategoryId: 91491,
    marketCategoryName: 'Мобильные телефоны',
    marketModelName: 'Apple iPhone Xr 256GB',
};

export const UNITED_OFFER_FOR_VARIANTS_MOCK: GetUnitedOffersResponse = {
    offers: [
        {
            basic: {
                identifiers: {
                    offerId: 'autotest-offer-variant-3',
                    extra: {
                        shopSku: 'autotest-offer-variant-3',
                        wareMd5: 'wareMd5',
                    },
                    businessId: 10956736,
                },
                meta: {
                    tsCreated: {
                        nanos: 527000000,
                        seconds: {low: 1619438013, high: 0, unsigned: false},
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
                        offerVersion: {counter: {low: 1555, high: 0, unsigned: true}},
                        ucDataVersion: {counter: {low: 9, high: 0, unsigned: true}},
                        actualContentVersion: {counter: {low: 1551, high: 0, unsigned: true}},
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
                                value: 'iPhone Xr 256GB',
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
                                value: 'Apple',
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
                                value: ['190198775283'],
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
                            },
                            dimensions: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1619605358, high: 0, unsigned: false},
                                        nanos: 141000000,
                                    },
                                },
                            },
                            category: {
                                meta: {
                                    source: 5,
                                    timestamp: {
                                        seconds: {low: 1613046727, high: 0, unsigned: false},
                                        nanos: 191000000,
                                    },
                                },
                                id: {low: 871967196, high: 0, unsigned: true},
                                name: 'Мобильные телефоны',
                                parentId: {low: 0, high: 0, unsigned: true},
                                pathCategoryIds: '871967196',
                                pathCategoryNames: 'Мобильные телефоны',
                                businessId: 10956736,
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
                                        paramId: {low: 14871214, high: 0, unsigned: false},
                                        paramName: 'Цвет товара для карточки',
                                        value: {valueType: 2, optionId: 14898056, strValue: 'красный'},
                                    },
                                    {
                                        valueSource: 1,
                                        paramId: {low: 13887626, high: 0, unsigned: false},
                                        paramName: 'Цвет товара для фильтра',
                                        value: {valueType: 2, optionId: 13891866, strValue: 'красный'},
                                    },
                                ],
                            },
                        },
                    },
                    market: {
                        enrichedOffer: {
                            categoryId: 91491,
                            modelId: 217318117,
                            classificationTypeValue: 0,
                            clusterCreatedTimestamp: {low: 0, high: 0, unsigned: false},
                            duplicateOfferGroupId: {low: 0, high: 0, unsigned: false},
                            matchedTypeValue: 23,
                            marketSkuId: {low: 100439187564, high: 0, unsigned: false},
                        },
                        marketCategory: 'Мобильные телефоны',
                        // @ts-expect-error(TS2322) неверная типизация proto
                        categoryId: 91491,
                        productName: 'Apple iPhone Xr 256GB',
                        realUcVersion: {counter: {low: 9, high: 0, unsigned: true}},
                    },
                    binding: {
                        approved: {
                            meta: {
                                source: 7,
                                timestamp: {seconds: {low: 1624106873, high: 0, unsigned: false}, nanos: 152470000},
                            },
                            marketCategoryId: 91491,
                            marketCategoryName: 'Мобильные телефоны',
                            marketModelId: {low: 217318117, high: 0, unsigned: false},
                            marketModelName: 'Apple iPhone Xr 256GB',
                            marketSkuId: {low: 1654939756, high: 23, unsigned: false},
                        },
                        ucMapping: {
                            meta: {timestamp: {seconds: {low: 1620782982, high: 0, unsigned: false}, nanos: 0}},
                            marketCategoryId: 91491,
                            marketModelId: {low: 217318117, high: 0, unsigned: false},
                            marketSkuId: {low: 1654939756, high: 23, unsigned: false},
                            marketCategoryName: 'Мобильные телефоны',
                            marketModelName: 'Apple iPhone Xr 256GB',
                            marketSkuName: 'Смартфон Apple iPhone Xr 256GB, красный',
                        },
                    },
                    status: {
                        result: {
                            meta: {
                                source: 8,
                                timestamp: {seconds: {low: 1622738700, high: 0, unsigned: false}, nanos: 0},
                            },
                            cardStatus: 1,
                        },
                        contentSystemStatus: {
                            meta: {
                                source: 7,
                                timestamp: {seconds: {low: 1624106873, high: 0, unsigned: false}, nanos: 152470000},
                            },
                            cpcState: 1,
                            cpaState: 0,
                            allowCategorySelection: false,
                            allowModelSelection: false,
                            allowModelCreateUpdate: false,
                            modelBarcodeRequired: false,
                            skuMappingConfidence: 1,
                            statusContentVersion: {counter: {low: 1551, high: 0, unsigned: true}},
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
                                    url: 'datacamp.market.yandex.ru/pictures/10956735/dOFG8hADU3EzyTlSIhosqw',
                                    source: 1,
                                },
                            ],
                        },
                        actual: {
                            'datacamp.market.yandex.ru/pictures/10956735/dOFG8hADU3EzyTlSIhosqw': {
                                id: 'AbHXqQFyqfhp_Yb9AY50Iw',
                                original: {
                                    url:
                                        '//avatars.mds.yandex.net/get-marketpictesting/4791546/market_AbHXqQFyqfhp_Yb9AY50Iw/orig',
                                    width: 1500,
                                    height: 1500,
                                    containerWidth: 1500,
                                    containerHeight: 1500,
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
                10956735: {
                    identifiers: {
                        shopId: 10956735,
                        offerId: 'autotest-offer-variant-3',
                        extra: {shopSku: 'autotest-offer-variant-3', wareMd5: 'wareMd5'},
                        businessId: 10956736,
                    },
                    meta: {
                        rgb: 1,
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
                                    source: 4,
                                    timestamp: {
                                        seconds: {low: 1623254143, high: 0, unsigned: false},
                                        nanos: 601503000,
                                    },
                                },
                                flag: false,
                            },
                            {
                                meta: {
                                    source: 7,
                                    timestamp: {
                                        seconds: {low: 1623254143, high: 0, unsigned: false},
                                        nanos: 601503000,
                                    },
                                },
                                flag: false,
                            },
                        ],
                        publishByPartner: 1,
                        result: 8,
                        version: {
                            offerVersion: {counter: {low: 1488, high: 0, unsigned: true}},
                            ucDataVersion: {counter: {low: 4, high: 0, unsigned: true}},
                            actualContentVersion: {counter: {low: 5, high: 0, unsigned: true}},
                        },
                        originalCpa: {
                            meta: {
                                source: 5,
                                timestamp: {seconds: {low: 1623254143, high: 0, unsigned: false}, nanos: 601503000},
                            },
                            flag: false,
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
                                    value: 'https://test.ru',
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
                                    value: 'https://test.ru',
                                },
                            },
                            originalTerms: {
                                quantity: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                },
                                salesNotes: {
                                    meta: {
                                        source: 4,
                                        timestamp: {
                                            seconds: {low: 1619605358, high: 0, unsigned: false},
                                            nanos: 178000000,
                                        },
                                    },
                                    value: '',
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
                            binaryPrice: {
                                price: {low: -1295421440, high: 139, unsigned: true},
                                rate: 'CBRF',
                                id: 'RUR',
                                refId: 'RUR',
                            },
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
                    },
                },
            },
        },
    ],
};
