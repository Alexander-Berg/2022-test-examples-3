var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    request_text: 'такси',
    type: 'snippet',
    data_stub: {
        num: 0,
        doctitle: 'taxi',
        favicon_domain: 'm.taxi.yandex.ru',
        green_url: 'm.\u0007[taxi\u0007].yandex.ru/city-tariff/?city=ÐÐ¾ÑÐºÐ²Ð°',
        snippets: {
            full: {
                applicable: 1,
                counter_prefix: '/snippet/taxi/',
                data: {
                    br: [
                        38.324771,
                        55.313084
                    ],
                    city: 'Москва',
                    class: [
                        'econom',
                        'business',
                        'comfortplus',
                        'vip',
                        'minivan'
                    ],
                    for_wizard: true,
                    geo_id: 213,
                    last_updated: 1482062922,
                    max_tariffs_url: 'https://m.taxi.yandex.ru/city-tariff/?city=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0',
                    payment_options: {
                        applepay: true,
                        corp: true,
                        coupon: true,
                        creditcard: true
                    },
                    price: {
                        free_route: [
                            {
                                class: {
                                    class: 'econom',
                                    id: 'econom',
                                    name: 'Эконом',
                                    service_levels: [
                                        50
                                    ]
                                },
                                currency: 'руб.',
                                id: 'taximeter.min_price_included_distance_and_time',
                                name: 'Минимальная стоимость (включено 5 мин и 2 км)',
                                price: 99,
                                visual_group: 'main'
                            },
                            {
                                class: {
                                    class: 'business',
                                    id: 'business',
                                    name: 'Комфорт',
                                    service_levels: [
                                        70
                                    ]
                                },
                                currency: 'руб.',
                                id: 'taximeter.min_price_included_distance_and_time',
                                name: 'Минимальная стоимость (включено 5 мин и 2 км)',
                                price: 199,
                                visual_group: 'main'
                            },
                            {
                                class: {
                                    class: 'comfortplus',
                                    id: 'comfortplus',
                                    name: 'Комфорт+',
                                    service_levels: [
                                        80
                                    ]
                                },
                                currency: 'руб.',
                                id: 'taximeter.min_price_included_distance_and_time',
                                name: 'Минимальная стоимость (включено 5 мин и 0 км)',
                                price: 199,
                                visual_group: 'main'
                            },
                            {
                                class: {
                                    class: 'minivan',
                                    id: 'minivan',
                                    name: 'Минивэн',
                                    service_levels: [
                                        95
                                    ]
                                },
                                currency: 'руб.',
                                id: 'taximeter.min_price_included_distance_and_time',
                                name: 'Минимальная стоимость (включено 5 мин и 0 км)',
                                price: 199,
                                visual_group: 'main'
                            },
                            {
                                class: {
                                    class: 'vip',
                                    id: 'vip',
                                    name: 'Бизнес',
                                    service_levels: [
                                        90
                                    ]
                                },
                                currency: 'руб.',
                                id: 'taximeter.min_price_included_distance_and_time',
                                name: 'Минимальная стоимость (включено 5 мин и 0 км)',
                                price: 299,
                                visual_group: 'main'
                            }
                        ],
                        to_airport: [
                            {
                                class: {
                                    class: 'econom',
                                    id: 'econom',
                                    name: 'Эконом',
                                    service_levels: [
                                        50
                                    ]
                                },
                                currency: 'руб.',
                                id: 'fixed_route',
                                name: 'Аэропорт Шереметьево — Аэропорт Шереметьево',
                                price: 850,
                                visual_group: 'other'
                            },
                            {
                                class: {
                                    class: 'business',
                                    id: 'business',
                                    name: 'Комфорт',
                                    service_levels: [
                                        70
                                    ]
                                },
                                currency: 'руб.',
                                id: 'fixed_route',
                                name: 'ЮЗАО — Аэропорт Внуково',
                                price: 1050,
                                visual_group: 'other'
                            },
                            {
                                class: {
                                    class: 'minivan',
                                    id: 'minivan',
                                    name: 'Минивэн',
                                    service_levels: [
                                        95
                                    ]
                                },
                                currency: 'руб.',
                                id: 'fixed_route',
                                name: 'ЮЗАО — Аэропорт Внуково',
                                price: 1050,
                                visual_group: 'other'
                            },
                            {
                                class: {
                                    class: 'comfortplus',
                                    id: 'comfortplus',
                                    name: 'Комфорт+',
                                    service_levels: [
                                        80
                                    ]
                                },
                                currency: 'руб.',
                                id: 'fixed_route',
                                name: 'ЮЗАО — Аэропорт Внуково',
                                price: 1250,
                                visual_group: 'other'
                            },
                            {
                                class: {
                                    class: 'vip',
                                    id: 'vip',
                                    name: 'Бизнес',
                                    service_levels: [
                                        90
                                    ]
                                },
                                currency: 'руб.',
                                id: 'fixed_route',
                                name: 'Аэропорт Шереметьево — Аэропорт Шереметьево',
                                price: 1300,
                                visual_group: 'other'
                            }
                        ]
                    },
                    req_destination_rules: {
                        min_timedelta: 1500
                    },
                    requirements: {
                        animaltransport: true,
                        applepay: true,
                        bicycle: true,
                        check: true,
                        childchair_moscow: true,
                        conditioner: true,
                        corp: true,
                        coupon: true,
                        creditcard: true,
                        nosmoking: true,
                        yellowcarnumber: true
                    },
                    service_levels: [
                        {
                            can_be_default: true,
                            cars: [
                                'Hyundai Solaris',
                                'Kia Rio',
                                'Ford Focus',
                                'Renault Fluence'
                            ],
                            name: 'Эконом',
                            only_for_soon_orders: false,
                            service_level: 50
                        },
                        {
                            can_be_default: true,
                            cars: [
                                'Skoda Octavia',
                                'Ford Galaxy',
                                'Hyundai i40'
                            ],
                            name: 'Комфорт',
                            only_for_soon_orders: false,
                            service_level: 70
                        },
                        {
                            can_be_default: true,
                            cars: [
                                'Nissan Teana',
                                'Toyota Camry',
                                'Lexus ES'
                            ],
                            name: 'Комфорт+',
                            only_for_soon_orders: false,
                            service_level: 80
                        },
                        {
                            can_be_default: true,
                            cars: [
                                'Mercedes-Benz E-Class',
                                'Hyundai Equus'
                            ],
                            name: 'Бизнес',
                            only_for_soon_orders: false,
                            service_level: 90
                        },
                        {
                            can_be_default: false,
                            cars: [
                                '6 мест. Например',
                                'Ford Galaxy',
                                'Volkswagen Caddy.'
                            ],
                            name: 'Минивэн',
                            only_for_soon_orders: false,
                            service_level: 95
                        }
                    ],
                    support_phone: '+74957058888',
                    supported_feedback_choices: {
                        cancelled_reason: [
                            {
                                label: 'Заказал по ошибке',
                                name: 'usererror'
                            },
                            {
                                label: 'Водитель попросил отменить',
                                name: 'driverrequest'
                            },
                            {
                                label: 'Слишком долго ждать',
                                name: 'longwait'
                            },
                            {
                                label: 'Уехал на другом такси',
                                name: 'othertaxi'
                            },
                            {
                                label: 'Водитель ехал в другую сторону',
                                name: 'droveaway'
                            }
                        ]
                    },
                    supported_requirements: [
                        {
                            label: 'Такси с жёлтыми номерами',
                            name: 'yellowcarnumber',
                            persistent: false,
                            type: 'boolean'
                        },
                        {
                            label: 'Некурящий водитель',
                            name: 'nosmoking',
                            persistent: false,
                            type: 'boolean'
                        },
                        {
                            driver_name: 'childchair',
                            label: 'Детское кресло',
                            name: 'childchair_moscow',
                            persistent: false,
                            select: {
                                caption: 'Выберите тип кресла:',
                                options: [
                                    {
                                        label: '9-18 кг, от 9 месяцев до 4 лет',
                                        name: 'infant',
                                        value: 1
                                    },
                                    {
                                        label: '15-25 кг, от 3 до 7 лет',
                                        name: 'chair',
                                        value: 3
                                    },
                                    {
                                        label: '22-36 кг, от 6 до 12 лет',
                                        name: 'booster',
                                        value: 7
                                    }
                                ],
                                type: 'number'
                            },
                            type: 'select'
                        },
                        {
                            label: 'Перевозка велосипеда',
                            name: 'bicycle',
                            persistent: false,
                            type: 'boolean'
                        },
                        {
                            label: 'Кондиционер',
                            name: 'conditioner',
                            persistent: false,
                            type: 'boolean'
                        },
                        {
                            label: 'Перевозка животного',
                            name: 'animaltransport',
                            persistent: false,
                            type: 'boolean'
                        },
                        {
                            label: 'Квитанция об оплате',
                            name: 'check',
                            persistent: false,
                            type: 'boolean'
                        }
                    ],
                    tariff_calc: true,
                    taxicount: {
                        loyal: false,
                        online: 10319,
                        parks: 311
                    },
                    tl: [
                        36.902971,
                        56.072737
                    ],
                    tz: '+0300'
                },
                slot: 'full',
                slot_rank: 0,
                template: 'taxi',
                type: 'taxi',
                types: {
                    all: [
                        'snippets',
                        'taxi'
                    ],
                    kind: 'wizard',
                    main: 'taxi'
                }
            }
        }
    }
};
