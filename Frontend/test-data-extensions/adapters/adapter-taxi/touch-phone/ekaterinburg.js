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
                        60.941589,
                        56.586256
                    ],
                    city: 'Екатеринбург',
                    class: [
                        'econom',
                        'business',
                        'vip'
                    ],
                    for_wizard: true,
                    geo_id: 54,
                    last_updated: 1482064619,
                    max_tariffs_url: 'https://m.taxi.yandex.ru/city-tariff/?city=%D0%95%D0%BA%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%BD%D0%B1%D1%83%D1%80%D0%B3',
                    payment_options: {
                        applepay: true,
                        corp: true,
                        coupon: true,
                        creditcard: true
                    },
                    precalc_cost: true,
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
                                id: 'taximeter.once_price',
                                name: 'Посадка в авто',
                                price: 49,
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
                                price: 149,
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
                                name: 'Минимальная стоимость (включено 10 мин и 3 км)',
                                price: 299,
                                visual_group: 'main'
                            }
                        ]
                    },
                    requirements: {
                        applepay: true,
                        corp: true,
                        coupon: true,
                        creditcard: true
                    },
                    service_levels: [
                        {
                            can_be_default: true,
                            cars: [
                                'Renault Logan',
                                'Daewoo Nexia',
                                'Nissan Almera',
                                'Kia Rio'
                            ],
                            name: 'Эконом',
                            only_for_soon_orders: false,
                            service_level: 50
                        },
                        {
                            can_be_default: true,
                            cars: [
                                'Škoda Rapid',
                                'Hyundai Solaris',
                                'Peugeot 408',
                                'Chevrolet Cruze'
                            ],
                            name: 'Комфорт',
                            only_for_soon_orders: false,
                            service_level: 70
                        },
                        {
                            can_be_default: true,
                            cars: [
                                'Toyota Camry',
                                'Nissan Teana',
                                'Audi A6',
                                'BMW 5 series'
                            ],
                            name: 'Бизнес',
                            only_for_soon_orders: false,
                            service_level: 90
                        }
                    ],
                    support_phone: '+73432260616',
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
                    supported_requirements: [],
                    taxicount: {
                        loyal: false,
                        online: 926,
                        parks: 73
                    },
                    tl: [
                        60.272697,
                        56.965551
                    ],
                    tz: '+0500'
                },
                serp_info: {
                    flat: false,
                    format: 'json',
                    slot: 'full',
                    type: 'taxi'
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
