let stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = function(photoWidth, photoHeight, patternSize) {
    photoWidth = photoWidth || 130;
    photoHeight = photoHeight || 130;
    patternSize = patternSize || 65;

    let photo = stubs.imageUrlStub(photoWidth, photoHeight, { patternSize: patternSize });

    return {
        num: '9',
        snippets: {
            full: {
                applicable: 1,
                counter_prefix: '/snippet/companies/company/',
                data: {
                    GeoMetaSearchData: {
                        features: [
                            {
                                geometry: {
                                    coordinates: [
                                        37.499713,
                                        55.699522,
                                    ],
                                    type: 'Point',
                                },
                                properties: {
                                    Photos: {
                                        Photos: [
                                            {
                                                urlTemplate: photo,
                                            },
                                        ],
                                    },
                                    BusinessRating: {
                                        ratings: '0',
                                        reviews: '42',
                                        score: 4.2,
                                    },
                                    CompanyMetaData: {
                                        Address: {
                                            Components: [
                                                {
                                                    kind: 'country',
                                                    name: 'Россия',
                                                },
                                                {
                                                    kind: 'province',
                                                    name: 'Москва',
                                                },
                                                {
                                                    kind: 'locality',
                                                    name: 'Москва',
                                                },
                                                {
                                                    kind: 'street',
                                                    name: 'Мичуринский проспект',
                                                },
                                                {
                                                    kind: 'house',
                                                    name: '30А',
                                                },
                                            ],
                                            country_code: 'RU',
                                            formatted: 'Москва, Мичуринский просп., 30б',
                                        },
                                        Categories: [
                                            {
                                                InternalCategoryInfo: {
                                                    AppleData: {
                                                        acid: 'auto',
                                                        level: '1',
                                                    },
                                                    seoname: 'car_accessories',
                                                },
                                                internal: null,
                                                name: 'Автоаксессуары',
                                            },
                                        ],
                                        Features: [
                                            {
                                                id: 'cargo_transportation',
                                                name: 'грузоперевозки',
                                                type: 'bool',
                                                value: 0,
                                            },
                                            {
                                                id: 'sober_driver',
                                                name: 'трезвый водитель',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'price_taxi',
                                                name: 'по городу',
                                                type: 'text',
                                                value: '350–1100 руб',
                                            },
                                            {
                                                id: 'car_rental',
                                                name: 'прокат авто',
                                                type: 'bool',
                                                value: 0,
                                            },
                                            {
                                                id: 'animal_transportation',
                                                name: 'перевозка животных',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'vip_taxi',
                                                name: 'vip-такси',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'corporate_taxi',
                                                name: 'корпоративное такси',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'child_seat',
                                                name: 'детское кресло',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'courier_delivery',
                                                name: 'курьерская доставка',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'ceremonies',
                                                name: 'обслуживание торжеств',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'airport_and_railway_transfers',
                                                name: 'поездка в аэропорты и на вокзалы',
                                                type: 'bool',
                                                value: 1,
                                            },
                                            {
                                                id: 'transfer',
                                                name: 'трансфер',
                                                type: 'bool',
                                                value: 1,
                                            },
                                        ],
                                        Geo: {
                                            precision: 'number',
                                        },
                                        Hours: {
                                            Availabilities: [
                                                {
                                                    Everyday: 1,
                                                    Intervals: [
                                                        {
                                                            from: '08:00:00',
                                                            to: '17:00:00',
                                                        },
                                                    ],
                                                },
                                            ],
                                            text: 'ежедневно, 8:00–17:00',
                                            tzOffset: '10800',
                                        },
                                        InternalCompanyInfo: {
                                            geoid: 213,
                                            ids: [
                                                '1367146754',
                                            ],
                                            seoname: 'avto_elegant',
                                        },
                                        Phones: [
                                            {
                                                country: '7',
                                                formatted: '+7 (916) 500-10-61',
                                                number: '5001061',
                                                prefix: '916',
                                                type: 'phone',
                                            },
                                        ],
                                        address: 'Москва, Мичуринский просп., 30б',
                                        closed: 'permanent',
                                        id: '1367146754',
                                        internal: null,
                                        name: 'Авто Элегант',
                                        names: [
                                            'Авто Элегант',
                                        ],
                                        unreliable: null,
                                        url: 'http://yandex.ru/search/touch?text=Loremipsumdolorsitametconsecteturadipiscingelit',
                                        urls: null,
                                    },
                                    Panoramas: {
                                        items: [
                                            {
                                                Point: {
                                                    coordinates: [
                                                        37.49945469,
                                                        55.69944158,
                                                    ],
                                                    type: 'Point',
                                                },
                                                direction: [
                                                    61.1,
                                                    10,
                                                ],
                                                id: '1297434783_673798918_23_1430999248',
                                                span: [
                                                    90,
                                                    45,
                                                ],
                                            },
                                        ],
                                    },
                                    Router: {
                                        types: [
                                            'mt',
                                            'auto',
                                        ],
                                    },
                                    Stops: {
                                        items: [
                                            {
                                                Distance: {
                                                    text: '210 м',
                                                    value: '212.881',
                                                },
                                                Point: {
                                                    coordinates: [
                                                        37.49873098,
                                                        55.69769214,
                                                    ],
                                                    type: 'Point',
                                                },
                                                Style: {
                                                    color: '#ffdd03',
                                                },
                                                name: 'Раменки',
                                            },
                                            {
                                                Distance: {
                                                    text: '1,3 км',
                                                    value: '1327.32',
                                                },
                                                Point: {
                                                    coordinates: [
                                                        37.51606709,
                                                        55.70706142,
                                                    ],
                                                    type: 'Point',
                                                },
                                                Style: {
                                                    color: '#ffdd03',
                                                },
                                                name: 'Ломоносовский проспект',
                                            },
                                            {
                                                Distance: {
                                                    text: '2,3 км',
                                                    value: '2327.16',
                                                },
                                                Point: {
                                                    coordinates: [
                                                        37.53453263,
                                                        55.6924403,
                                                    ],
                                                    type: 'Point',
                                                },
                                                Style: {
                                                    color: '#cc0000',
                                                },
                                                name: 'Университет',
                                            },
                                        ],
                                    },
                                    URIMetaData: {
                                        URI: {
                                            uri: 'ymapsbm1://org?oid=1367146754',
                                        },
                                    },
                                    boundedBy: [
                                        [
                                            37.4956075,
                                            55.6972035,
                                        ],
                                        [
                                            37.5038185,
                                            55.7018405,
                                        ],
                                    ],
                                    name: 'Авто Элегант',
                                    title: 'Авто Элегант в Москве, Химки',
                                    titleHighlight: [
                                        [
                                            0,
                                            4,
                                        ],
                                        [
                                            5,
                                            12,
                                        ],
                                        [
                                            23,
                                            28,
                                        ],
                                    ],
                                },
                                type: 'Feature',
                            },
                        ],
                        properties: {
                            ResponseMetaData: {
                                SearchResponse: {
                                    InternalResponseInfo: {
                                        context: 'CAAAAAIAa9WuCWnPQkD/PuPCgeBLQJP8iF+xhuQ/aw2l9iLa3T8CAAAAAQIBAAAAAAAAAAHzUD6Ku2P7pNUAAAABAACAPwAAAAAAAAAA',
                                        display: 'multiple',
                                        drag_context: 'CAAAAAIAa9WuCWnPQkD/PuPCgeBLQJP8iF+xhuQ/aw2l9iLa3T8CAAAAAQIBAAAAAAAAAAHzUD6Ku2P7pNUAAAABAACAPwAAAAAAAAAA',
                                        link_from_serp: 'CAAAAAIDa9WuCWnPQkD/PuPCgeBLQJP8iF+xhuQ/aw2l9iLa3T8CAAAAAQIBAAAAAAAAAAHzUD6Ku2P7pNUAAAABAACAPwAAAAAAAAAA',
                                        reqid: '1492168891822771-736283776025132611104178-ws33-124-TCH',
                                        serpid: '1492168891822771-736283776025132611104178-ws33-124-TCH',
                                    },
                                    Point: {
                                        coordinates: [
                                            37.39014588,
                                            55.81831081,
                                        ],
                                        type: 'Point',
                                    },
                                    Sort: null,
                                    SourceMetaDataList: {
                                        SourceMetaData: {
                                            ResponseMetaData: {
                                                BusinessSearchRequest: {
                                                    boundedBy: [
                                                        [
                                                            37.228925,
                                                            55.824582,
                                                        ],
                                                        [
                                                            37.507133,
                                                            56.026005,
                                                        ],
                                                    ],
                                                    internal: null,
                                                    request: 'элегант авто',
                                                    results: 10,
                                                },
                                                BusinessSearchResponse: {
                                                    Filters: [
                                                        {
                                                            id: 'open_24h',
                                                            name: 'круглосуточно',
                                                            type: 'Boolean',
                                                        },
                                                        {
                                                            id: 'open_now',
                                                            name: 'работает сейчас',
                                                            type: 'Boolean',
                                                        },
                                                        {
                                                            disabled: '1',
                                                            id: 'has_url',
                                                            name: 'есть сайт',
                                                            type: 'Boolean',
                                                        },
                                                        {
                                                            disabled: '1',
                                                            id: 'has_photo',
                                                            name: 'с фото',
                                                            type: 'Boolean',
                                                        },
                                                        {
                                                            disabled: '1',
                                                            id: 'car_park',
                                                            name: 'парковка',
                                                            type: 'Boolean',
                                                        },
                                                        {
                                                            id: 'wi_fi',
                                                            name: 'Wi-Fi',
                                                            type: 'Boolean',
                                                        },
                                                        {
                                                            disabled: '1',
                                                            id: 'payment_by_credit_card',
                                                            name: 'оплата картой',
                                                            type: 'Boolean',
                                                        },
                                                    ],
                                                    Point: {
                                                        coordinates: [
                                                            37.544755,
                                                            55.763781,
                                                        ],
                                                        type: 'Point',
                                                    },
                                                    boundedBy: [
                                                        [
                                                            37.509837,
                                                            55.661445,
                                                        ],
                                                        [
                                                            37.579672,
                                                            55.865849,
                                                        ],
                                                    ],
                                                    found: 3,
                                                    internal: null,
                                                    sort: 'rank',
                                                },
                                            },
                                        },
                                    },
                                    boundedBy: [
                                        [
                                            37.20295778,
                                            55.609499,
                                        ],
                                        [
                                            37.57732522,
                                            56.0260052,
                                        ],
                                    ],
                                    found: 5,
                                },
                            },
                            name: 'Авто Элегант в Москве, Химки',
                            nameForRubrics: 'Автоателье в Москве, Химки',
                            nameHighlight: [
                                [
                                    0,
                                    4,
                                ],
                                [
                                    5,
                                    12,
                                ],
                                [
                                    23,
                                    28,
                                ],
                            ],
                            nameHighlightForRubrics: [
                                [
                                    21,
                                    26,
                                ],
                            ],
                            titleGeoPart: 'в Москве, Химки',
                            titleGeoPartHighlight: [
                                [
                                    10,
                                    15,
                                ],
                            ],
                        },
                        type: 'FeatureCollection',
                    },
                    GeoobjectTop3MeanRelev: 0.3923827112,
                    displayMode: 'new_map/single/free_without_url/sure_company',
                    geoKind: 'locality',
                    language: 'ru_RU',
                    singleOrgMx: '0.334378',
                    singleOrgMxType: 'SureSingleOrg',
                    subtype: 'company',
                },
                serp_info: {
                    format: 'json',
                    org_one_bna: 0,
                    subtype: 'company',
                    type: 'companies',
                },
                slot: 'full',
                slot_rank: 0,
                subtype: 'company',
                template: 'companies',
                type: 'companies',
                types: {
                    all: [
                        'snippets',
                        'companies',
                    ],
                    kind: 'wizard',
                    main: 'companies',
                },
            },
        },
    };
};
