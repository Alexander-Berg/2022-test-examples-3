module.exports = function () {
    return {
        num: 0,
        snippets: {
            full: {
                slot: 'full',
                type: 'companies',
                template: 'companies',
                counter_prefix: '/snippet/companies/map/',
                serp_info: {
                    subtype: 'map',
                    type: 'companies'
                },
                types: {
                    kind: 'wizard',
                    main: 'companies',
                    all: [
                        'snippets',
                        'companies'
                    ]
                },
                data: {
                    GeoMetaSearchData: {
                        properties: {
                            nameForRubrics: 'Аптека в Москве',
                            name: 'Аптеки в Москве - фото, телефоны, адреса',
                            titleGeoPart: 'в Москве',
                            titleGeoPartHighlight: [
                                [
                                    2,
                                    8
                                ]
                            ],
                            nameHighlightForRubrics: [
                                [
                                    0,
                                    6
                                ],
                                [
                                    9,
                                    15
                                ]
                            ],
                            nameHighlight: [
                                [
                                    0,
                                    6
                                ],
                                [
                                    9,
                                    15
                                ]
                            ],
                            ResponseMetaData: {
                                SearchRequest: {
                                    request: 'аптеки москва'
                                },
                                SearchResponse: {
                                    boundedBy: [
                                        [
                                            37.23284189,
                                            55.49113064
                                        ],
                                        [
                                            38.06108858,
                                            55.95756479
                                        ]
                                    ],
                                    Point: {
                                        type: 'Point',
                                        coordinates: [
                                            37.64696524,
                                            55.72504714
                                        ]
                                    },
                                    InternalResponseInfo: {
                                        context: 'CAAAAAIAqtTsgVZgSEAZcmw9Q+BLQGhefbf1ALU/WW/+ZQqkpz8CAAAAAQIBAAAAAAAAAAFhBDn+IAEOVirbAQABAACAPwAAAAAAAAAA'
                                    },
                                    SourceMetaDataList: {
                                        SourceMetaData: {
                                            ResponseMetaData: {
                                                BusinessSearchResponse: {
                                                    Filters: [
                                                        {
                                                            type: 'Boolean',
                                                            name: 'круглосуточно',
                                                            id: 'open_24h'
                                                        },
                                                        {
                                                            type: 'Boolean',
                                                            name: 'работает сейчас',
                                                            id: 'open_now'
                                                        },
                                                        {
                                                            type: 'Boolean',
                                                            name: 'оплата картой',
                                                            id: 'payment_by_credit_card'
                                                        },
                                                        {
                                                            type: 'Boolean',
                                                            name: 'есть сайт',
                                                            id: 'has_url'
                                                        },
                                                        {
                                                            type: 'Boolean',
                                                            name: 'с фото',
                                                            id: 'has_photo'
                                                        },
                                                        {
                                                            enum: [
                                                                {
                                                                    name: 'ГорЗдрав',
                                                                    id: '367179142'
                                                                },
                                                                {
                                                                    name: 'Аптеки Столички',
                                                                    id: '41608180100'
                                                                },
                                                                {
                                                                    name: 'Столичные аптеки',
                                                                    id: '805550706'
                                                                },
                                                                {
                                                                    name: 'Аптека 36,6',
                                                                    id: '6003312'
                                                                },
                                                                {
                                                                    name: 'Аптеки столицы',
                                                                    id: '43219044071'
                                                                }
                                                            ],
                                                            type: 'EnumFeature',
                                                            name: 'сети',
                                                            id: 'chain_id'
                                                        },
                                                        {
                                                            enum: [
                                                                {
                                                                    name: 'Аптека',
                                                                    id: '184105932'
                                                                },
                                                                {
                                                                    name: 'Фитопродукция, БАДы',
                                                                    id: '184105930'
                                                                },
                                                                {
                                                                    name: 'Интернет-магазин',
                                                                    id: '184105742'
                                                                }
                                                            ],
                                                            type: 'EnumFeature',
                                                            name: 'категории',
                                                            id: 'category_id'
                                                        },
                                                        {
                                                            disabled: '1',
                                                            type: 'Boolean',
                                                            name: 'гостевая парковка',
                                                            id: 'car_park'
                                                        },
                                                        {
                                                            disabled: '1',
                                                            type: 'Boolean',
                                                            name: 'бесплатный Wi-Fi',
                                                            id: 'wi_fi'
                                                        }
                                                    ]
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        features: [
                            {
                                geometry: {
                                    coordinates: [
                                        37.438987,
                                        55.849423
                                    ]
                                },
                                properties: {
                                    name: 'Планета здоровья',
                                    BusinessRating: {
                                        reviews: '0',
                                        ratings: '5',
                                        score: '7.9'
                                    },
                                    titleHighlight: [
                                        [
                                            19,
                                            25
                                        ]
                                    ],
                                    CompanyMetaData: {
                                        Features: [
                                            {
                                                type: 'bool',
                                                name: 'оплата картой',
                                                value: 1,
                                                id: 'payment_by_credit_card'
                                            }
                                        ],
                                        Distance: {
                                            text: '710 км',
                                            value: '710000'
                                        },
                                        url: 'http://www.planetazdorovo.ru',
                                        Phones: [
                                            {
                                                country: '7',
                                                prefix: '495',
                                                type: 'phone',
                                                number: '3693300',
                                                formatted: '+7 (495) 369-33-00'
                                            },
                                            {
                                                country: '7',
                                                prefix: '495',
                                                type: 'phone',
                                                number: '7833638',
                                                formatted: '+7 (495) 783-36-38'
                                            }
                                        ],
                                        address: 'Россия, Москва, Сходненская улица, 37',
                                        InternalCompanyInfo: {
                                            seoname: 'planeta_zdorovya',
                                            ids: [
                                                '1205101830'
                                            ],
                                            geoid: 213
                                        },
                                        Hours: {
                                            text: 'ежедневно, круглосуточно',
                                            Availabilities: [
                                                {
                                                    TwentyFourHours: 1,
                                                    Everyday: 1
                                                }
                                            ],
                                            tzOffset: '10800'
                                        },
                                        Snippet: {
                                            FeatureSet: [
                                                'payment_by_credit_card'
                                            ]
                                        },
                                        names: [
                                            'Планета здоровья'
                                        ],
                                        urls: [
                                            'http://www.planetazdorovo.ru'
                                        ],
                                        Address: {
                                            country_code: 'RU',
                                            formatted: 'Россия, Москва, Сходненская улица, 37',
                                            Components: [
                                                {
                                                    kind: 'country',
                                                    name: 'Россия'
                                                },
                                                {
                                                    kind: 'province',
                                                    name: 'Москва'
                                                },
                                                {
                                                    kind: 'locality',
                                                    name: 'Москва'
                                                },
                                                {
                                                    kind: 'street',
                                                    name: 'Сходненская улица'
                                                },
                                                {
                                                    kind: 'house',
                                                    name: '37'
                                                }
                                            ]
                                        },
                                        Geo: {
                                            precision: 'exact'
                                        },
                                        Chains: {
                                            Chain: {
                                                name: 'Планета здоровья',
                                                id: '3046001143'
                                            }
                                        },
                                        id: '1205101830',
                                        Categories: [
                                            {
                                                internal: null,
                                                InternalCategoryInfo: {
                                                    AppleData: {
                                                        acid: 'yandex_184105932',
                                                        level: '2'
                                                    },
                                                    seoname: 'pharmacy'
                                                },
                                                name: 'Аптека',
                                                nameHighlight: [
                                                    [
                                                        0,
                                                        6
                                                    ]
                                                ],
                                                class: 'drugstores'
                                            }
                                        ],
                                        name: 'Планета здоровья'
                                    },
                                    Stops: {
                                        items: [
                                            {
                                                Distance: {
                                                    text: '130 м',
                                                    value: '131.45'
                                                },
                                                Style: {
                                                    color: '#92007b'
                                                },
                                                name: 'Сходненская',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.43979235,
                                                        55.85051324
                                                    ]
                                                }
                                            },
                                            {
                                                Distance: {
                                                    text: '1,4 км',
                                                    value: '1372.45'
                                                },
                                                Style: {
                                                    color: '#92007b'
                                                },
                                                name: 'Планерная',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.43613759,
                                                        55.86164512
                                                    ]
                                                }
                                            },
                                            {
                                                Distance: {
                                                    text: '2,4 км',
                                                    value: '2410.89'
                                                },
                                                Style: {
                                                    color: '#0a6f20'
                                                },
                                                name: 'Речной вокзал',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.47623287,
                                                        55.85489121
                                                    ]
                                                }
                                            }
                                        ]
                                    }
                                }
                            },
                            {
                                geometry: {
                                    coordinates: [
                                        37.623609,
                                        55.73034
                                    ]
                                },
                                properties: {
                                    name: 'Столичные аптеки',
                                    BusinessRating: {
                                        reviews: '0',
                                        ratings: '0'
                                    },
                                    titleHighlight: [
                                        [
                                            10,
                                            16
                                        ],
                                        [
                                            19,
                                            25
                                        ]
                                    ],
                                    CompanyMetaData: {
                                        Hours: {
                                            text: 'пн-вс 00:00–00:00',
                                            Availabilities: [
                                                {
                                                    Monday: 1,
                                                    Tuesday: 1,
                                                    Friday: 1,
                                                    Wednesday: 1,
                                                    Thursday: 1,
                                                    Saturday: 1,
                                                    Sunday: 1,
                                                    Intervals: [
                                                        {
                                                            to: '00:00:01',
                                                            from: '00:00:00'
                                                        }
                                                    ]
                                                }
                                            ],
                                            tzOffset: '10800'
                                        },
                                        Features: [
                                            {
                                                type: 'bool',
                                                name: 'изготовление лекарств',
                                                value: 0,
                                                id: 'production_pharmacy'
                                            },
                                            {
                                                type: 'bool',
                                                name: 'оплата картой',
                                                value: 0,
                                                id: 'payment_by_credit_card'
                                            }
                                        ],
                                        Distance: {
                                            text: '700 км',
                                            value: '700000'
                                        },
                                        url: 'http://www.citypharm.ru',
                                        Phones: [
                                            {
                                                country: '7',
                                                prefix: '499',
                                                type: 'phone',
                                                number: '2372961',
                                                formatted: '+7 (499) 237-29-61'
                                            },
                                            {
                                                country: '7',
                                                prefix: '499',
                                                type: 'phone',
                                                number: '2367172',
                                                formatted: '+7 (499) 236-71-72'
                                            },
                                            {
                                                country: '7',
                                                prefix: '499',
                                                type: 'phone',
                                                number: '2372852',
                                                formatted: '+7 (499) 237-28-52'
                                            },
                                            {
                                                country: '7',
                                                prefix: '499',
                                                type: 'phone',
                                                number: '2374242',
                                                formatted: '+7 (499) 237-42-42'
                                            }
                                        ],
                                        address: 'Москва, ул. Большая Полянка, 65/74',
                                        InternalCompanyInfo: {
                                            seoname: 'stolichnyye_apteki',
                                            ids: [
                                                '1068590181',
                                                '1017563611',
                                                '1016776021',
                                                '1087917292'
                                            ],
                                            geoid: 213
                                        },
                                        Snippet: {
                                            FeatureSet: [
                                                'payment_by_credit_card',
                                                'production_pharmacy'
                                            ]
                                        },
                                        names: [
                                            'Столичные аптеки'
                                        ],
                                        urls: [
                                            'http://www.citypharm.ru',
                                            'http://www.citypharm.ru/apteki/7.html'
                                        ],
                                        Address: {
                                            country_code: 'RU',
                                            formatted: 'Москва, ул. Большая Полянка, 65/74',
                                            Components: [
                                                {
                                                    kind: 'country',
                                                    name: 'Россия'
                                                },
                                                {
                                                    kind: 'province',
                                                    name: 'Москва'
                                                },
                                                {
                                                    kind: 'locality',
                                                    name: 'Москва'
                                                },
                                                {
                                                    kind: 'street',
                                                    name: 'улица Большая Полянка'
                                                },
                                                {
                                                    kind: 'house',
                                                    name: '65/74с3'
                                                }
                                            ]
                                        },
                                        Geo: {
                                            precision: 'exact'
                                        },
                                        Chains: {
                                            Chain: {
                                                name: 'Столичные аптеки',
                                                id: '805550706'
                                            }
                                        },
                                        id: '1068590181',
                                        Categories: [
                                            {
                                                internal: null,
                                                InternalCategoryInfo: {
                                                    AppleData: {
                                                        acid: 'yandex_184105932',
                                                        level: '2'
                                                    },
                                                    seoname: 'pharmacy'
                                                },
                                                name: 'Аптека',
                                                nameHighlight: [
                                                    [
                                                        0,
                                                        6
                                                    ]
                                                ],
                                                class: 'drugstores'
                                            }
                                        ],
                                        name: 'Столичные аптеки'
                                    },
                                    Stops: {
                                        items: [
                                            {
                                                Distance: {
                                                    text: '160 м',
                                                    value: '163.131'
                                                },
                                                Style: {
                                                    color: '#7f0000'
                                                },
                                                name: 'Добрынинская',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.6225212,
                                                        55.72900958
                                                    ]
                                                }
                                            },
                                            {
                                                Distance: {
                                                    text: '400 м',
                                                    value: '395.409'
                                                },
                                                Style: {
                                                    color: '#a2a5b4'
                                                },
                                                name: 'Серпуховская',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.6250009,
                                                        55.72687648
                                                    ]
                                                }
                                            },
                                            {
                                                Distance: {
                                                    text: '690 м',
                                                    value: '688.558'
                                                },
                                                Style: {
                                                    color: '#ff7f00'
                                                },
                                                name: 'Октябрьская',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.61277122,
                                                        55.73126017
                                                    ]
                                                }
                                            }
                                        ]
                                    }
                                }
                            },
                            {
                                geometry: {
                                    coordinates: [
                                        37.639446,
                                        55.729843
                                    ]
                                },
                                properties: {
                                    name: 'Аптека 36,6',
                                    BusinessRating: {
                                        reviews: '0',
                                        ratings: '0'
                                    },
                                    titleHighlight: [
                                        [
                                            0,
                                            6
                                        ],
                                        [
                                            14,
                                            20
                                        ]
                                    ],
                                    CompanyMetaData: {
                                        Distance: {
                                            text: '700 км',
                                            value: '695000'
                                        },
                                        name: 'Аптека 36,6',
                                        url: 'http://www.366.ru',
                                        Phones: [
                                            {
                                                country: '7',
                                                prefix: '495',
                                                type: 'phone',
                                                number: '7976366',
                                                formatted: '+7 (495) 797-63-66'
                                            },
                                            {
                                                country: '7',
                                                prefix: '495',
                                                type: 'phone',
                                                number: '7978686',
                                                formatted: '+7 (495) 797-86-86'
                                            }
                                        ],
                                        address: 'Москва, Павелецкая пл., 1а, стр. 1, Аптека №1135 (цокольный этаж)',
                                        InternalCompanyInfo: {
                                            seoname: 'apteka_36_6',
                                            geoid: 213,
                                            email: 'info@366.ru',
                                            ids: [
                                                '1672273030',
                                                '1345454098',
                                                '1648815244'
                                            ]
                                        },
                                        Hours: {
                                            text: 'ежедневно, 9:00–22:00',
                                            Availabilities: [
                                                {
                                                    Intervals: [
                                                        {
                                                            to: '22:00:00',
                                                            from: '09:00:00'
                                                        }
                                                    ],
                                                    Everyday: 1
                                                }
                                            ],
                                            tzOffset: '10800'
                                        },
                                        names: [
                                            'Аптека 36,6'
                                        ],
                                        urls: [
                                            'http://www.366.ru'
                                        ],
                                        Address: {
                                            country_code: 'RU',
                                            formatted: 'Москва, Павелецкая пл., 1а, стр. 1, Аптека №1135 (цокольный этаж)',
                                            Components: [
                                                {
                                                    kind: 'country',
                                                    name: 'Россия'
                                                },
                                                {
                                                    kind: 'province',
                                                    name: 'Москва'
                                                },
                                                {
                                                    kind: 'locality',
                                                    name: 'Москва'
                                                },
                                                {
                                                    kind: 'street',
                                                    name: 'Павелецкая площадь'
                                                },
                                                {
                                                    kind: 'house',
                                                    name: '1А'
                                                }
                                            ]
                                        },
                                        Geo: {
                                            precision: 'exact'
                                        },
                                        Chains: {
                                            Chain: {
                                                name: 'Аптека 36,6',
                                                id: '6003312'
                                            }
                                        },
                                        id: '1672273030',
                                        Categories: [
                                            {
                                                internal: null,
                                                InternalCategoryInfo: {
                                                    AppleData: {
                                                        acid: 'yandex_184105932',
                                                        level: '2'
                                                    },
                                                    seoname: 'pharmacy'
                                                },
                                                name: 'Аптека',
                                                nameHighlight: [
                                                    [
                                                        0,
                                                        6
                                                    ]
                                                ],
                                                class: 'drugstores'
                                            }
                                        ]
                                    },
                                    Stops: {
                                        items: [
                                            {
                                                Distance: {
                                                    text: '31 м',
                                                    value: '31.1126'
                                                },
                                                Style: {
                                                    color: '#0a6f20'
                                                },
                                                name: 'Павелецкая',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.63895732,
                                                        55.72979779
                                                    ]
                                                }
                                            },
                                            {
                                                Distance: {
                                                    text: '270 м',
                                                    value: '271.783'
                                                },
                                                Style: {
                                                    color: '#7f0000'
                                                },
                                                name: 'Павелецкая',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.63633128,
                                                        55.73153703
                                                    ]
                                                }
                                            },
                                            {
                                                Distance: {
                                                    text: '970 м',
                                                    value: '965.813'
                                                },
                                                Style: {
                                                    color: '#a2a5b4'
                                                },
                                                name: 'Серпуховская',
                                                Point: {
                                                    type: 'Point',
                                                    coordinates: [
                                                        37.6250009,
                                                        55.72687648
                                                    ]
                                                }
                                            }
                                        ]
                                    }
                                }
                            }
                        ]
                    }
                }
            }
        }
    };
};
