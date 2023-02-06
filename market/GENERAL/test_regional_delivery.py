#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    ExchangeRate,
    GLParam,
    GLType,
    NewShopRating,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
)

from core.testcase import TestCase, main
from core.matcher import Absent

DELIVERY_INTERVALS = [0, 1, 5, 32]


class T(TestCase):
    @classmethod
    def prepare_basic(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # TODO: MARKETOUT-47769 Удалить флаг
        cls.settings.default_search_experiment_flags += ['market_hide_long_delivery_offers=0']

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                tz_offset=10800,
                children=[
                    Region(
                        rid=213,
                        name='Москва',
                        tz_offset=10800,
                        children=[
                            Region(rid=216, name='Зеленоград', tz_offset=10800),
                            Region(
                                rid=114619,
                                name='Новомосковский административный округ',
                                region_type=Region.FEDERAL_DISTRICT,
                                children=[
                                    Region(rid=10720, name='Внуково', region_type=Region.VILLAGE),
                                    Region(rid=21624, name='Щербинка', region_type=Region.CITY),
                                ],
                            ),
                        ],
                    ),
                    Region(rid=10758, name='Химки', tz_offset=10800),
                ],
            ),
            Region(
                rid=84,
                name='США',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=202, name='Нью-Йорк', tz_offset=-18000),
                ],
            ),
            Region(
                rid=159,
                name='Казахстан',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=163, name='Астана', tz_offset=14400),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, name='Московская пепячечная "Доставляем"'),
            Shop(fesh=2, priority_region=213, name='Московская пепячечная "Доставляем, кроме выходных и праздников"'),
            Shop(
                fesh=3,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                name='Для теста фильтров, офферы с бесплатной или платной доставкой',
            ),
            Shop(fesh=4, priority_region=213, name='Для теста фильтров, офферы с бесплатной доставкой'),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=211,
                fesh=1,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=28000, day_from=1, day_to=3, order_before=23),
                            DeliveryOption(price=500, day_from=0, day_to=0, order_before=23),
                        ],
                    ),
                    RegionalDelivery(
                        rid=1,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=4, order_before=23),
                        ],
                    ),
                    RegionalDelivery(
                        rid=163, options=[DeliveryOption(price=500, day_from=6, day_to=7, order_before=23)]
                    ),
                    RegionalDelivery(rid=202, unknown=True),
                    RegionalDelivery(rid=10758, forbidden=True),
                ],
            ),
            DeliveryBucket(
                bucket_id=212,
                fesh=1,
                carriers=[2],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=700, day_from=1, day_to=3, order_before=23),
                            DeliveryOption(price=500, day_from=31, day_to=31, order_before=24),
                            DeliveryOption(price=0, day_from=2, day_to=32, order_before=24),
                        ],
                    ),
                    RegionalDelivery(
                        rid=1,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=5, order_before=23),
                        ],
                    ),
                    RegionalDelivery(rid=163, unknown=True),
                    RegionalDelivery(rid=202, forbidden=True),
                    RegionalDelivery(rid=10758, forbidden=True),
                ],
            ),
            DeliveryBucket(
                bucket_id=214,
                fesh=1,
                carriers=[3],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=32, order_before=24)]
                    ),
                    RegionalDelivery(
                        rid=163, options=[DeliveryOption(price=500, day_from=6, day_to=7, order_before=23)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=221,
                fesh=2,
                carriers=[1],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=0, day_to=2, order_before=24),
                            DeliveryOption(price=300, day_from=2, day_to=5, order_before=23),
                            DeliveryOption(price=0, day_from=32, day_to=32, order_before=24),
                        ],
                    ),
                    RegionalDelivery(
                        rid=1,
                        options=[
                            DeliveryOption(price=700, day_from=0, day_to=2, order_before=24),
                            DeliveryOption(price=500, day_from=2, day_to=5, order_before=23),
                            DeliveryOption(price=200, day_from=32, day_to=32, order_before=24),
                        ],
                    ),
                    RegionalDelivery(
                        rid=202,
                        options=[
                            DeliveryOption(price=700, day_from=0, day_to=5, order_before=24),
                            DeliveryOption(price=500, day_from=2, day_to=9, order_before=23),
                            DeliveryOption(price=200, day_from=31, day_to=31, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=222,
                fesh=2,
                carriers=[1],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                    RegionalDelivery(
                        rid=202,
                        options=[
                            DeliveryOption(price=500, day_from=0, day_to=1, order_before=24),
                        ],
                    ),
                ],
            ),
        ]

        for interval_idx, interval in enumerate(DELIVERY_INTERVALS):
            cls.index.delivery_buckets += [
                DeliveryBucket(
                    bucket_id=230 + interval_idx,
                    fesh=3,
                    carriers=[1],
                    regional_options=[
                        RegionalDelivery(
                            rid=213,
                            options=[
                                DeliveryOption(price=100, day_from=0, day_to=2, order_before=24),
                            ],
                        ),
                        RegionalDelivery(
                            rid=163,
                            options=[
                                DeliveryOption(price=100, day_from=0, day_to=interval, order_before=24),
                            ],
                        ),
                        RegionalDelivery(
                            rid=202,
                            options=[
                                DeliveryOption(price=14 - interval_idx * 2, day_from=0, day_to=2, order_before=24),
                            ],
                        ),
                    ],
                ),
                DeliveryBucket(
                    bucket_id=235 + interval_idx,
                    fesh=3,
                    carriers=[1],
                    regional_options=[
                        RegionalDelivery(
                            rid=163,
                            options=[
                                DeliveryOption(price=0, day_from=0, day_to=interval + 1, order_before=24),
                            ],
                        ),
                    ],
                ),
                DeliveryBucket(
                    bucket_id=240 + interval_idx,
                    fesh=4,
                    carriers=[1],
                    regional_options=[
                        RegionalDelivery(
                            rid=213,
                            options=[
                                DeliveryOption(price=100, day_from=0, day_to=2, order_before=24),
                            ],
                        ),
                        RegionalDelivery(
                            rid=163,
                            options=[
                                DeliveryOption(price=0, day_from=0, day_to=interval, order_before=24),
                            ],
                        ),
                        RegionalDelivery(
                            rid=202,
                            options=[
                                DeliveryOption(price=6 - interval_idx * 2, day_from=0, day_to=2, order_before=24),
                            ],
                        ),
                    ],
                ),
            ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=2, title='moscow-offer', delivery_buckets=[211, 212]),
            Offer(fesh=2, hyperid=3, title='moscow-calendar-offer', delivery_buckets=[221]),
            Offer(fesh=2, hyperid=4, title='moscow-calendar-no-onstock-offer', delivery_buckets=[222]),
            Offer(fesh=1, hyperid=6, title='test-cpm', delivery_buckets=[214]),
        ]

        for interval_idx, interval in enumerate(DELIVERY_INTERVALS):
            cls.index.offers += [
                Offer(
                    fesh=3,
                    hyperid=5,
                    price=interval_idx + 1,
                    bid=500,
                    title='test-filter-non-free-' + str(interval),
                    delivery_buckets=[230 + interval_idx, 235 + interval_idx],
                    delivery_options=[
                        DeliveryOption(price=200, day_from=32, day_to=32),
                    ],
                ),
                Offer(
                    fesh=4,
                    hyperid=5,
                    price=interval_idx + 5,
                    bid=100,
                    title='test-filter-free-' + str(interval),
                    delivery_buckets=[240 + interval_idx],
                    delivery_options=[
                        DeliveryOption(price=200, day_from=32, day_to=32),
                    ],
                ),
            ]

        cls.index.currencies = [
            Currency('KZT', exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=0.2)]),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=2, holidays=[2, 3, 6, 7, 13, 14, 20, 21, 27, 28]),
        ]

    def test_delivery_options_format(self):
        """Что тестируем: формат выдачи опций, скрытие заведомо более невыгодных опций,
        граничные значения day_from и day_to
        Проверяем, что в опциях бакета с несколькими carrier-id отображается
        один serviceId (первый из списка)
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=2'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "region": {
                            "entity": "region",
                            "id": 213,
                            "name": "Москва",
                            "lingua": {"name": {"genitive": "Москва", "preposition": " ", "prepositional": "Москва"}},
                        },
                        "options": [
                            {"price": {"currency": "RUR", "value": "0"}, "serviceId": "2", "isDefault": True},
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 0,
                                "dayTo": 0,
                                "isDefault": False,
                                "serviceId": "1",
                            },
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 31,
                                "dayTo": 31,
                                "isDefault": False,
                                "serviceId": "2",
                            },
                            {
                                "price": {"currency": "RUR", "value": "700"},
                                "dayFrom": 1,
                                "dayTo": 3,
                                "orderBefore": "23",
                                "isDefault": False,
                                "serviceId": "2",
                            },
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_delivery_options_another_region(self):
        # Что тестируем: опции доставки в регион, родительский по отношению к приоритетному
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=1&hyperid=2'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "region": {
                            "entity": "region",
                            "id": 1,
                            "name": "Московская область",
                            "lingua": {
                                "name": {
                                    "genitive": "Московская область",
                                    "preposition": " ",
                                    "prepositional": "Московская область",
                                }
                            },
                        },
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 1,
                                "dayTo": 4,
                                "orderBefore": "23",
                                "isDefault": True,
                                "serviceId": "1",
                            },
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 1,
                                "dayTo": 5,
                                "orderBefore": "23",
                                "isDefault": False,
                                "serviceId": "2",
                            },
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_delivery_options_another_timezone(self):
        # Что тестируем: опции доставки в регион в другом часовом поясе
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=163&hyperid=2'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 6,
                                "dayTo": 7,
                                "orderBefore": "0",
                                "isDefault": True,
                                "serviceId": "1",
                            }
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_delivery_options_unknown(self):
        # Что тестируем: поддержку "неопределенных" опций для замены regions в shops.dat
        # Также тестируем случай, когда в одном бакете доставка запрещена (forbidden),
        # а в другом разрешена (unknown). Оффер должен считаться доставляемым
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=202&hyperid=2'.format(place))
            self.assertFragmentIn(response, {"delivery": {"options": []}}, preserve_order=True)

    def test_delivery_options_forbidden(self):
        # Что тестируем: опции типа forbidden делают оффер недоставляемым
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=10758&hyperid=2'.format(place))
            self.assertFragmentNotIn(response, {"delivery": {"options": []}}, preserve_order=True)

    def test_delivery_options_another_currency(self):
        # Что тестируем: опции доставки в другой валюте (курс 1 KZT = 5 RUR)
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=2&currency=KZT'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "KZT", "value": "100"},
                                "dayFrom": 6,
                                "dayTo": 7,
                                "orderBefore": "0",
                                "isDefault": True,
                                "serviceId": "1",
                            }
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_delivery_options_calendar(self):
        # Что тестируем: опции доставки с учетом календаря в локальном регионе магазина
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=3'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "region": {
                            "entity": "region",
                            "id": 213,
                            "name": "Москва",
                            "lingua": {"name": {"genitive": "Москва", "preposition": " ", "prepositional": "Москва"}},
                        },
                        "options": [
                            {"price": {"currency": "RUR", "value": "0"}, "serviceId": "1", "isDefault": True},
                            {
                                "price": {"currency": "RUR", "value": "300"},
                                "dayFrom": 4,
                                "dayTo": 9,
                                "orderBefore": "23",
                                "isDefault": False,
                                "serviceId": "1",
                            },
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 0,
                                "dayTo": 4,
                                "isDefault": False,
                                "serviceId": "1",
                            },
                        ],
                    }
                },
                preserve_order=True,
            )

    def test_delivery_options_another_timezone_yesterday(self):
        # Что тестируем: опции доставки в регион в другом часовом поясе, где текущее время - вчера (с учетом календарей)
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=202&hyperid=3'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {"price": {"currency": "RUR", "value": "200"}, "serviceId": "1", "isDefault": True},
                            {
                                "price": {"currency": "RUR", "value": "500"},
                                "dayFrom": 4,
                                "dayTo": 15,
                                "orderBefore": "15",
                                "isDefault": False,
                                "serviceId": "1",
                            },
                            {
                                "price": {"currency": "RUR", "value": "700"},
                                "dayFrom": 0,
                                "dayTo": 9,
                                "isDefault": False,
                                "serviceId": "1",
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_onstock_local(self):
        # Что тестируем: наличие/отсутствие onstock в локальном регионе в зависимости от сроков доставки
        response = self.report.request_json('place=prime&rids=213&hyperid=3')
        self.assertFragmentIn(response, {'total': 1}, preserve_order=False)
        self.assertFragmentIn(response, {'inStock': True}, preserve_order=False)

        response = self.report.request_json('place=prime&rids=213&hyperid=4')
        self.assertFragmentIn(response, {'total': 1}, preserve_order=False)
        self.assertFragmentIn(response, {'inStock': False}, preserve_order=False)

    def test_onstock_non_local(self):
        # Что тестируем: наличие/отсутствие inStock в нелокальном регионе в зависимости от
        # сроков доставки в этот регион
        response = self.report.request_json('place=productoffers&rids=202&hyperid=3')
        self.assertFragmentIn(response, {'total': 1}, preserve_order=False)
        self.assertFragmentIn(response, {'inStock': False}, preserve_order=False)

        response = self.report.request_json('place=productoffers&rids=202&hyperid=4')
        self.assertFragmentIn(response, {'total': 1}, preserve_order=False)
        self.assertFragmentIn(response, {'inStock': True}, preserve_order=False)

        response = self.report.request_json('place=prime&rids=202&hyperid=3')
        self.assertFragmentIn(response, {'total': 1}, preserve_order=False)
        self.assertFragmentIn(response, {'inStock': False}, preserve_order=False)

        response = self.report.request_json('place=prime&rids=202&hyperid=4')
        self.assertFragmentIn(response, {'total': 1}, preserve_order=False)
        self.assertFragmentIn(response, {'inStock': True}, preserve_order=False)

    @classmethod
    def prepare_onstock_unknown_non_local(cls):
        '''Создаем бакет для региона 163 с неизвестными опциями доставки и два бакета
        для региона 213 с быстрой и медленной доставкой
        Создаем офферы с бакетом для региона 163 и разными варинатами доставки
        в регион 213 (локальный для магазина)
        '''
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=270,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=163, unknown=True),
                ],
            ),
            DeliveryBucket(
                bucket_id=271,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=2),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=272,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=300, day_from=3, day_to=5),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=9, hyperid=60, title='delivery-buckets-fast', delivery_buckets=[270, 271]),
            Offer(fesh=9, hyperid=61, title='delivery-buckets-slow', delivery_buckets=[270, 272]),
            Offer(
                fesh=9,
                hyperid=62,
                title='delivery-yml-fast',
                delivery_buckets=[270],
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=2),
                ],
            ),
            Offer(
                fesh=9,
                hyperid=63,
                title='delivery-yml-slow',
                delivery_buckets=[270],
                delivery_options=[
                    DeliveryOption(price=500, day_from=3, day_to=4),
                ],
            ),
            Offer(
                fesh=9,
                hyperid=64,
                title='delivery-yml-fast',
                delivery_buckets=[270, 272],
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=2),
                ],
            ),
        ]

    def test_onstock_unknown_non_local(self):
        '''Что тестируем: при неизвестных опциях доставки в регион пользователя
        флаг inStock в оффере вычисляется по опциям доставки в локальный
        регион магазина
        Тестируем 5 вариантов:
        1. Быстрая доставка по информации в бакетах - inStock=1, т.к.
           есть опция доставки за два рабочих дня максимум
        2. Медленная доставка по информации в бакетах - inStock=0, т.к.
           доставка от 3-х до 5 рабочих дней
        3. Быстрая доставка по информации в фиде - inStock=1, т.к.
           есть опция доставки за два рабочих дня максимум
        4. Медленная доставка по информации в бакетах - inStock=0, т.к.
           доставка от 3-х до 4-х рабочих дней
        5. Медленная доставка по информации в бакетах и быстрая в фиде - inStock=1,
           т.к. есть есть опция доставки за два рабочих дня максимум в фиде, и
           опция доставки от 3-х до 5 рабочих дней в бакете при этом не
           принимается во внимание

        Проверяем, что для плейсов 'prime' и 'productoffers' на выдаче
        правильный inStock
        '''
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=163&hyperid=60'.format(place))
            self.assertFragmentIn(
                response, {"search": {"total": 1, "results": [{"delivery": {"inStock": True}}]}}, preserve_order=True
            )

            response = self.report.request_json('place={}&rids=163&hyperid=61'.format(place))
            self.assertFragmentIn(
                response, {"search": {"total": 1, "results": [{"delivery": {"inStock": False}}]}}, preserve_order=True
            )

            response = self.report.request_json('place={}&rids=163&hyperid=62'.format(place))
            self.assertFragmentIn(
                response, {"search": {"total": 1, "results": [{"delivery": {"inStock": True}}]}}, preserve_order=True
            )

            response = self.report.request_json('place={}&rids=163&hyperid=63'.format(place))
            self.assertFragmentIn(
                response, {"search": {"total": 1, "results": [{"delivery": {"inStock": False}}]}}, preserve_order=True
            )

            response = self.report.request_json('place={}&rids=163&hyperid=64'.format(place))
            self.assertFragmentIn(
                response, {"search": {"total": 1, "results": [{"delivery": {"inStock": True}}]}}, preserve_order=True
            )

    def test_interval_filters_0(self):
        # Что тестируем: фильтрацию офферов с доставкой сегодня (бесплатной и платной)
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=0'.format(place) + unified_off_flags
            )

            # Должны остаться только два оффера с доставкой "сегодня"
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}},
                        ],
                    }
                },
                preserve_order=False,
            )

            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&free_delivery=1&delivery_interval=0'.format(place) + unified_off_flags
            )

            # Должен остаться только один оффер, т.к. в test-filter-non-free-0 есть
            # бесплатная доставка, но она на день дольше
            self.assertFragmentIn(
                response,
                {"search": {"total": 1, "results": [{"entity": "offer", "titles": {"raw": "test-filter-free-0"}}]}},
                preserve_order=False,
            )

    def test_interval_filters_1(self):
        # Что тестируем: фильтрацию офферов с доставкой завтра (бесплатной и платной)
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=1'.format(place) + unified_off_flags
            )

            # Должны остаться только четыре оффера с доставкой "сегодня" и "завтра"
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 4,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-1"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-1"}},
                        ],
                    }
                },
                preserve_order=False,
            )

            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&free_delivery=1&delivery_interval=1'.format(place) + unified_off_flags
            )

            # Должны остаться только три оффера, в test-filter-non-free-1 бесплатная
            # доставка на день дольше
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 3,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-1"}},
                        ],
                    }
                },
                preserve_order=False,
            )

    def test_interval_filters_5(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=5'.format(place) + unified_off_flags
            )

            # Должны остаться только шесть офферов с доставкой "сегодня", "завтра" и "до 5 дней"
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 6,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-1"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-1"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-5"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-5"}},
                        ],
                    }
                },
                preserve_order=False,
            )

            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&free_delivery=1&delivery_interval=5'.format(place) + unified_off_flags
            )

            # Должны остаться только пять офферов, в test-filter-non-free-5 бесплатная
            # доставка на день дольше
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 5,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-1"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-1"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-5"}},
                        ],
                    }
                },
                preserve_order=False,
            )

    def test_free_delivery_only(self):
        # Что тестируем: фильтрацию офферов только фильтром бесплатной доставки
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&rids=213&fesh=2&free_delivery=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "offer", "titles": {"raw": "moscow-calendar-offer"}},
                    ],
                }
            },
            preserve_order=False,
        )

        response = self.report.request_json('place=productoffers&rids=213&hyperid=4&free_delivery=1')
        self.assertFragmentIn(response, {"search": {"total": 0}}, preserve_order=False)

    def test_filter_stats_5(self):
        # Что тестируем: Позапросные статистики с фильтром на доставку до 5 дней
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=5'.format(place) + unified_off_flags
            )
            # В статистике учитываются только офферы, подходящие под delivery_interval=5
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "delivery-interval",
                            "type": "boolean",
                            "name": "Срок доставки",
                            "subType": "",
                            "kind": 2,
                            "hasBoolNo": True,
                            "values": [
                                {"found": 2, "value": "0"},
                                {"found": 4, "value": "1"},
                                {"checked": True, "found": 6, "value": "5"},
                            ],
                        },
                        {
                            "id": "free-delivery",
                            "type": "boolean",
                            "name": "Бесплатная доставка курьером",
                            "subType": "",
                            "kind": 2,
                            "values": [{"initialFound": 1, "found": 5, "value": "1"}],
                        },
                    ]
                },
                preserve_order=True,
            )

            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=5&free_delivery=1'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "delivery-interval",
                            "values": [
                                {"found": 1, "value": "0"},
                                {"found": 3, "value": "1"},
                                {"checked": True, "found": 5, "value": "5"},
                            ],
                        },
                        {
                            "id": "free-delivery",
                            "values": [{"initialFound": 1, "found": 5, "checked": True, "value": "1"}],
                        },
                    ]
                },
                preserve_order=True,
            )

    def test_filter_stats_0(self):
        # Что тестируем: Позапросные статистики с фильтром на доставку сегодня
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=0'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "delivery-interval",
                            "values": [
                                {"checked": True, "found": 2, "value": "0"},
                                {"found": 4, "value": "1"},
                                {"found": 6, "value": "5"},
                            ],
                        },
                        {"id": "free-delivery", "values": [{"initialFound": 1, "found": 1, "value": "1"}]},
                    ]
                },
                preserve_order=True,
            )

            response = self.report.request_json(
                'place={}&rids=163&hyperid=5&delivery_interval=0&free_delivery=1'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "delivery-interval",
                            "values": [
                                {"checked": True, "found": 1, "value": "0"},
                                {"found": 3, "value": "1"},
                                {"found": 5, "value": "5"},
                            ],
                        },
                        {
                            "id": "free-delivery",
                            "values": [{"initialFound": 1, "found": 1, "checked": True, "value": "1"}],
                        },
                    ]
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_delivery_interval_filter_stats2(cls):
        cls.index.gltypes = [
            GLType(param_id=203, hid=888, gltype=GLType.ENUM, values=[1, 2], cluster_filter=True),
        ]

        cls.index.offers += [
            Offer(
                title='sales',
                price=42,
                hyperid=999,
                hid=888,
                glparams=[GLParam(param_id=203, value=2)],
                delivery_buckets=[211],
                manufacturer_warranty=True,
                fesh=2,
            ),
            Offer(
                fesh=4,
                title='has_delivery',
                price=100,
                hyperid=999,
                hid=888,
                glparams=[GLParam(param_id=203, value=1)],
                delivery_buckets=[213, 222],
                manufacturer_warranty=False,
            ),
        ]

    def test_delivery_interval_filter_stats2(self):
        """
        проверяем, что при фильтрации по магазину, по гл параметру и гарантии производителя правильно
        учитывается(либо не учитывается)
        found для срока доставки курьером.
        """
        # проверяем, что при фильтрации по delivery_interval и без нее статистики по фильтру одинаковые
        for query in ['place=prime&hyperid=999&rids=213', 'place=prime&hyperid=999&delivery-interval=0&rids=213']:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {"id": "delivery-interval", "values": [{"found": 1, "value": "0"}, {"found": 2, "value": "5"}]},
            )

        # проверяем фильтрацию по гарантии производителя
        for query in ['place=prime&hyperid=999&hid=888&rids=213&manufacturer_warranty=1']:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {"id": "delivery-interval", "values": [{"found": 1, "value": "0"}, {"found": 1, "value": "5"}]},
                allow_different_len=False,
            )

        # проверяем фильтрацию по гл-параметру, магазину
        for query in [
            'place=prime&hyperid=999&hid=888&rids=213&glfilter=203:2',
            'place=prime&hyperid=999&hid=888&rids=213&fesh=2',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "delivery-interval",
                    "values": [
                        # really 1 document found but it was duplicated in glfilters
                        {"found": 2, "value": "0"},
                        {"found": 2, "value": "5"},
                    ],
                },
                allow_different_len=False,
            )

        for query in [
            'place=prime&hyperid=999&hid=888&rids=213&glfilter=203:1',
            'place=prime&hyperid=999&hid=888&rids=213&fesh=4',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(
                response,
                {
                    "id": "delivery-interval",
                    "values": [
                        # really 1 document found but it was duplicated in glfilters
                        {"found": 0, "value": "0"},
                        {"found": 2, "value": "5"},
                    ],
                },
                allow_different_len=False,
            )

    def test_price_sorting_with_delivery(self):
        # Что тестируем: Сортировку "по цене с учетом доставки"
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=202&hyperid=5&how=aprice&deliveryincluded=1'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-free-32"}, "prices": {"value": "8"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-5"}, "prices": {"value": "9"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-1"}, "prices": {"value": "10"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}, "prices": {"value": "11"}},
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-non-free-32"},
                                "prices": {"value": "12"},
                            },
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-5"}, "prices": {"value": "13"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-1"}, "prices": {"value": "14"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}, "prices": {"value": "15"}},
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_desc_price_sorting_with_delivery(self):
        # Что тестируем: Сортировку "по цене с учетом доставки" по убыванию
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=202&hyperid=5&how=dprice&deliveryincluded=1'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-0"}, "prices": {"value": "15"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-1"}, "prices": {"value": "14"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-non-free-5"}, "prices": {"value": "13"}},
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-non-free-32"},
                                "prices": {"value": "12"},
                            },
                            {"entity": "offer", "titles": {"raw": "test-filter-free-0"}, "prices": {"value": "11"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-1"}, "prices": {"value": "10"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-5"}, "prices": {"value": "9"}},
                            {"entity": "offer", "titles": {"raw": "test-filter-free-32"}, "prices": {"value": "8"}},
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_delivery_interval_sorting(self):
        # Что тестируем: Сортировку "по сроку доставки"
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=163&hyperid=5&how=delivery_interval'.format(place))

            # Офферы упорядочены по сроку доставки, офферы с одинаковым сроком между
            # собой ранжируются по quality_rating магазина
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-non-free-0"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-free-0"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-non-free-1"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-free-1"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-non-free-5"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-free-5"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-non-free-32"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "test-filter-free-32"},
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_local_delivery_price(self):
        """Что тестируем: Вывод тегов с ценой локальной доставки"""
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&rids=213&hyperid=4&debug=1' + unified_off_flags)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "delivery": {"price": {"currency": "RUR", "value": "500"}},
                        "debug": {"factors": {"DELIVERY_LOCAL": "1"}},
                    }
                ]
            },
        )

    # MARKETOUT-10308
    @classmethod
    def prepare_local_delivery_addition(cls):
        """Бакеты с идентификатором ГП 99 (собственная курьерская служба магазина)
        или без идентификатора ГП
        Офферы с опциями локальной доставки из фида
        """
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=215,
                fesh=1,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=0, day_to=0, order_before=24),
                            DeliveryOption(price=0, day_from=2, day_to=32, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=216,
                fesh=1,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=0, day_from=2, day_to=32, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=217,
                fesh=1,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=32, day_to=32, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=218,
                fesh=1,
                regional_options=[
                    RegionalDelivery(
                        rid=163,
                        options=[
                            DeliveryOption(price=200, day_from=32, day_to=32, order_before=24),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=1,
                hyperid=7,
                delivery_buckets=[211, 212],
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=3, order_before=23),
                ],
            ),
            Offer(
                fesh=1,
                hyperid=8,
                delivery_buckets=[215],
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=1, order_before=23),
                ],
            ),
            Offer(
                fesh=1,
                hyperid=10,
                delivery_buckets=[218],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=1, order_before=23),
                ],
            ),
            Offer(
                fesh=1,
                hyperid=11,
                delivery_buckets=[218],
                delivery_options=[
                    DeliveryOption(price=300, day_from=1, day_to=3, order_before=18),
                ],
            ),
            Offer(fesh=1, hyperid=12, has_delivery_options=False),
            # Бесплатная доставка из бакета, быстрая и бесплатная из фида
            Offer(
                fesh=1,
                hyperid=9,
                title='free-and-fast-same-option',
                delivery_buckets=[216],
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=1, order_before=23),
                ],
            ),
            # Бесплатная доставка из бакета, быстрая из фида
            Offer(
                fesh=1,
                hyperid=9,
                title='free-and-fast',
                delivery_buckets=[216],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=1, order_before=23),
                ],
            ),
            # Бесплатная доставка из бакета
            Offer(
                fesh=1,
                hyperid=9,
                title='free',
                delivery_buckets=[216],
                delivery_options=[
                    DeliveryOption(price=100, day_from=32, day_to=32, order_before=23),
                ],
            ),
            # Быстрая доставка из фида
            Offer(
                fesh=1,
                hyperid=9,
                title='fast',
                delivery_buckets=[217],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=1, order_before=23),
                ],
            ),
            # Нет ни быстрой, ни бесплатной доставки
            Offer(
                fesh=1,
                hyperid=9,
                title='non-free-and-slow',
                delivery_buckets=[217],
                delivery_options=[
                    DeliveryOption(price=100, day_from=32, day_to=32, order_before=23),
                ],
            ),
        ]

    def test_local_delivery_addition_non_default(self):
        """Что тестируем: локальные опции доставки из фида добавляются
        в случае локальной доставки

        Проверяем, что опция из фида появилась в выдаче, что она не стала
        дефолтной и не"схлопнула" опцию от Калькулятора, т.к. у них разные
        service-id
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=7'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "0", "isDeliveryIncluded": False},
                                "isDefault": True,
                                "serviceId": "2",
                            },
                            {
                                "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                "dayFrom": 0,
                                "dayTo": 0,
                                "orderBefore": "23",
                                "isDefault": False,
                                "serviceId": "1",
                            },
                            {
                                "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                "dayFrom": 31,
                                "dayTo": 31,
                                "isDefault": False,
                                "serviceId": "2",
                            },
                            {
                                "price": {"currency": "RUR", "value": "700", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 3,
                                "orderBefore": "23",
                                "isDefault": False,
                                "serviceId": "2",
                            },
                            {
                                "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 3,
                                "orderBefore": "23",
                                "isDefault": False,
                                "serviceId": "99",
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_local_delivery_addition_default(self):
        """Что тестируем: локальные опции доставки из фида добавляются в случае
        локальной доставки и могут становиться дефолтными

        Проверяем, что опция из фида появилась в выдаче, стала дефолтной и
        "схлопнула" опцию от Калькулятора, т.к. у них один и тот же service-id
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=8'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "0", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 1,
                                "orderBefore": "23",
                                "isDefault": True,
                                "serviceId": "99",
                            },
                            {
                                "price": {"currency": "RUR", "value": "100", "isDeliveryIncluded": False},
                                "dayFrom": 0,
                                "dayTo": 0,
                                "isDefault": False,
                                "serviceId": "99",
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_local_delivery_addition_another_region(self):
        """Что тестируем: локальные опции доставки из фида не добавляются в
        случае региональной доставки
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=163&hyperid=7'.format(place))
            self.assertFragmentIn(response, {"delivery": {"options": []}}, preserve_order=True)
            self.assertFragmentNotIn(response, {"delivery": {"options": [{"serviceId": "99"}]}}, preserve_order=False)

    def test_free_delivery_filter_with_local_addition(self):
        """Что тестируем: локальные опции учитываются при фильтрации по
        бесплатной доставке

        Проверяем, что на выдаче только офферы с бесплатной доставкой
        Проверяем статистики
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=9&free_delivery=1'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "free-and-fast-same-option"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "free-and-fast"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "free"},
                            },
                        ]
                    }
                },
                preserve_order=False,
            )

            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {"id": "delivery-interval", "values": [{"found": 1, "value": "1"}]},
                        {"id": "free-delivery", "values": [{"found": 3, "value": "1"}]},
                    ]
                },
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "0"}]}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "5"}]}]})

    def test_delivery_interval_filter_with_local_addition(self):
        """Что тестируем: локальные опции учитываются при фильтрации по
        интервалу доставки

        Проверяем, что на выдаче только офферы с доставкой "завтра"
        Проверяем статистики
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=9&delivery_interval=1'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "free-and-fast-same-option"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "free-and-fast"},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast"},
                            },
                        ]
                    }
                },
                preserve_order=False,
            )

            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {"id": "delivery-interval", "values": [{"found": 3, "value": "1"}]},
                        {"id": "free-delivery", "values": [{"found": 1, "value": "1"}]},
                    ]
                },
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "0"}]}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "5"}]}]})

    def test_free_delivery_tommorow_filter_with_local_addition(self):
        """Что тестируем: локальные опции правильно учитываются при
        фильтрации бесплатной доставке завтра

        Проверяем, что на выдаче только офферы с бесплтной доставкой
        "завтра" _в одной опции_
        Оффер с бесплатной доставкой в одной опции и быстрой в другой
        должен отфильтроваться

        Проверяем статистики
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=9&free_delivery=1&delivery_interval=1'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "free-and-fast-same-option"},
                            },
                        ]
                    }
                },
                preserve_order=False,
            )

            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {"id": "delivery-interval", "values": [{"found": 1, "value": "1"}]},
                        {"id": "free-delivery", "values": [{"found": 1, "value": "1"}]},
                    ]
                },
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "0"}]}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "5"}]}]})

    def test_priority_local_options_only(self):
        """Что тестируем: оффер с опциями для приоритетного региона только в фиде
        отображается в нем с приоритетным типом доставки

        Проверяем, что на выдаче есть оффер с заданной опцией доставки
        Проверяем, что в опции доставки есть идентификатор ГП 99, хотя в бакете он не был
        явно задан
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=10'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 1,
                                "orderBefore": "23",
                                "isDefault": True,
                                "serviceId": "99",
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_non_priority_local_options_only(self):
        """Что тестируем: оффер с опциями для приоритетного региона только в фиде
        отображается в неприоритетном регионе

        Проверяем, что на выдаче есть оффер с заданной опцией доставки
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=163&hyperid=10'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "200", "isDeliveryIncluded": False},
                                "isDefault": True,
                                "serviceId": "99",
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_local_options_only(self):
        """Что тестируем: оффер с опциями для приоритетного региона только в фиде
        и без бакетов отображается в приоритетном регионе

        Проверяем, что на выдаче есть оффер с заданной опцией доставки
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=11'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "300", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 3,
                                "orderBefore": "18",
                                "isDefault": True,
                                "serviceId": "99",
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_no_local_options_no_buckets(self):
        """Что тестируем: оффер без опций и без бакетов не отображается в приоритетном регионе

        Проверяем, что на выдаче нет оффера с заданной опцией доставки
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=12'.format(place))
            self.assertFragmentNotIn(response, {"entity": "offer"}, preserve_order=True)

    # MARKETOUT-10308
    @classmethod
    def prepare_local_delivery_addition_flags_processing(cls):
        """Бакеты с тремя типами опций (явная опция, unknown и forbidden)
        Наборы офферов для каждого из бакетов с быстрой локальной доставкой,
        медленной и без локальной доставки
        """
        cls.index.shops += [
            Shop(fesh=5, priority_region=213, name='Для теста расчета флагов оффера'),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=223,
                fesh=5,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=163,
                        options=[
                            DeliveryOption(price=100, day_from=1, day_to=4, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=224, fesh=5, carriers=[99], regional_options=[RegionalDelivery(rid=163, unknown=True)]
            ),
            DeliveryBucket(
                bucket_id=225, fesh=5, carriers=[99], regional_options=[RegionalDelivery(rid=163, forbidden=True)]
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=5,
                hyperid=21,
                price=9,
                title='slow-local-and-regional-delivery',
                delivery_buckets=[223],
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=3, order_before=23),
                ],
            ),
            Offer(
                fesh=5,
                hyperid=22,
                price=8,
                title='fast-local-and-regional-delivery',
                delivery_buckets=[223],
                delivery_options=[
                    DeliveryOption(price=400, day_from=1, day_to=1, order_before=23),
                ],
            ),
            Offer(
                fesh=5,
                hyperid=23,
                price=7,
                title='only-regional-delivery',
                delivery_buckets=[223],
                has_delivery_options=False,
            ),
            Offer(
                fesh=5,
                hyperid=21,
                price=6,
                title='slow-local-and-unknown-regional-delivery',
                delivery_buckets=[224],
                delivery_options=[
                    DeliveryOption(price=300, day_from=1, day_to=3, order_before=23),
                ],
            ),
            Offer(
                fesh=5,
                hyperid=22,
                price=5,
                title='fast-local-and-unknown-regional-delivery',
                delivery_buckets=[224],
                delivery_options=[
                    DeliveryOption(price=200, day_from=1, day_to=1, order_before=23),
                ],
            ),
            Offer(
                fesh=5,
                hyperid=23,
                price=4,
                title='only-unknown-regional-delivery',
                delivery_buckets=[224],
                has_delivery_options=False,
            ),
            Offer(
                fesh=5,
                hyperid=21,
                price=3,
                title='slow-local-and-forbidden-regional-delivery',
                delivery_buckets=[225],
                delivery_options=[
                    DeliveryOption(price=100, day_from=1, day_to=3, order_before=23),
                ],
            ),
            Offer(
                fesh=5,
                hyperid=22,
                price=2,
                title='fast-local-and-forbidden-regional-delivery',
                delivery_buckets=[225],
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=1, order_before=23),
                ],
            ),
            Offer(
                fesh=5,
                hyperid=23,
                price=1,
                title='only-forbidden-regional-delivery',
                delivery_buckets=[225],
                has_delivery_options=False,
            ),
        ]

    def test_local_delivery_addition_flags_priority(self):
        """Что тестируем: расчет флагов доставки в приоритетном регионе

        Проверяем, что на выдаче есть офферы, для которых в фиде заданы опции доставки
        в приоритетный регион с правильными значениями onstock
        Проверяем, что на выдаче всего 6 офферов, т.е. нет офферов, для которых
        опции в фиде не заданы
        Опции доставки в неприоритетный регион (163) не должны влиять на выдачу
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=21&hyperid=22&hyperid=23&how=dprice&deliveryincluded=1'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-unknown-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-unknown-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-forbidden-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-forbidden-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_local_delivery_addition_flags_priority_child(self):
        """Что тестируем: расчет флагов доставки в регионе, дочернем к приоритетному

        Проверяем, что на выдаче есть офферы, для которых в фиде заданы опции доставки
        в приоритетный регион с правильными значениями onstock
        Проверяем, что на выдаче всего 6 офферов, т.е. нет офферов, для которых
        опции в фиде не заданы
        Опции доставки в неприоритетный регион (163) не должны влиять на выдачу
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=216&hyperid=21&hyperid=22&hyperid=23&how=dprice&deliveryincluded=1'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-unknown-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-unknown-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-forbidden-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-forbidden-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    def test_local_delivery_addition_flags_non_priority(self):
        """Что тестируем: расчет флагов доставки в неприоритетном регионе

        Проверяем, что на выдаче есть офферы, для которых в бакете заданы опции доставки
        (явно или через unknown) в неприоритетный регион с правильными значениями onstock,
        в т.ч. и те офферы, для которых опции в фиде не заданы
        Проверяем, что на выдаче всего 6 офферов, т.е. нет офферов, для которых
        опции заданы как forbidden
        Проверяем, что для офферов с unknown-опциями значение onstock зависит от опций
        в фиде, а если в фиде нет опций, то берется значение из оффера (1)
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=21&hyperid=22&hyperid=23&how=dprice&deliveryincluded=1'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "only-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "slow-local-and-unknown-regional-delivery"},
                                "delivery": {"inStock": False},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "fast-local-and-unknown-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "only-unknown-regional-delivery"},
                                "delivery": {"inStock": True},
                            },
                        ]
                    }
                },
                preserve_order=True,
            )

    # MARKETOUT-10497
    @classmethod
    def prepare_hide_non_available_delivery_options(cls):
        """Бакеты с опциями доставки в два региона
        Наборы офферов для каждого из бакетов с опциями локальной доставки
        """
        cls.index.shops += [
            Shop(fesh=6, priority_region=213, name='Для теста влияния available на сроки доставки'),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=281,
                fesh=6,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=1, day_to=4, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=282,
                fesh=6,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=163,
                        options=[
                            DeliveryOption(price=200, day_from=5, day_to=9, order_before=24),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=6,
                hyperid=31,
                title='available-offer',
                delivery_buckets=[281, 282],
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=3, order_before=23),
                ],
            ),
            Offer(
                fesh=6,
                hyperid=31,
                title='non-available-offer',
                available=False,
                delivery_buckets=[281, 282],
                delivery_options=[],
            ),
            Offer(
                fesh=6,
                hyperid=31,
                title='available-without-buckets',
                delivery_options=[
                    DeliveryOption(price=300, day_from=1, day_to=3, order_before=23),
                ],
            ),
        ]

    def test_non_available_offer_priority(self):
        """Что тестируем: сокрытие опций доставки в случае available=False

        Проверяем, что у офферов с available=True на выдаче есть сроки доставки,
        а у оффера с available=False сроки скрылись и опции схлопнулись, т.е.
        опции с ценой 400 на выдаче нет совсем
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=31&how=aprice'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 3,
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "available-without-buckets"},
                                "delivery": {
                                    "isAvailable": True,
                                    "options": [
                                        {
                                            "price": {"currency": "RUR", "value": "300"},
                                            "serviceId": "99",
                                            "isDefault": True,
                                            "dayFrom": 1,
                                            "dayTo": 3,
                                            "orderBefore": "23",
                                        }
                                    ],
                                },
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "non-available-offer"},
                                "delivery": {
                                    "isAvailable": True,
                                    "options": [
                                        {
                                            "price": {"currency": "RUR", "value": "100"},
                                            "serviceId": "99",
                                            "isDefault": True,
                                        }
                                    ],
                                },
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "available-offer"},
                                "delivery": {
                                    "isAvailable": True,
                                    "options": [
                                        {
                                            "price": {"currency": "RUR", "value": "100"},
                                            "serviceId": "99",
                                            "dayFrom": 1,
                                            "dayTo": 4,
                                            "isDefault": True,
                                        },
                                        {
                                            "price": {"currency": "RUR", "value": "500"},
                                            "serviceId": "99",
                                            "dayFrom": 1,
                                            "dayTo": 3,
                                            "orderBefore": "23",
                                        },
                                    ],
                                },
                            },
                        ],
                    }
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    "options": [
                        {
                            "price": {"currency": "RUR", "value": "400"},
                        }
                    ]
                },
                preserve_order=False,
            )

    def test_non_available_offer_non_priority(self):
        """Что тестируем: сокрытие опций доставки в случае available=False в неприоритетном регионе

        Проверяем, что у офферов с available=True на выдаче есть сроки доставки,
        а у оффера с available=False их нет
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=163&hyperid=31&how=aprice'.format(place) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {
                                "entity": "offer",
                                "titles": {"raw": "non-available-offer"},
                                "delivery": {
                                    "isAvailable": True,
                                    "options": [
                                        {
                                            "price": {"currency": "RUR", "value": "200"},
                                            "serviceId": "99",
                                            "dayFrom": Absent(),
                                            "dayTo": Absent(),
                                            "isDefault": True,
                                        }
                                    ],
                                },
                            },
                            {
                                "entity": "offer",
                                "titles": {"raw": "available-offer"},
                                "delivery": {
                                    "isAvailable": True,
                                    "options": [
                                        {
                                            "price": {"currency": "RUR", "value": "200"},
                                            "serviceId": "99",
                                            "dayFrom": 5,
                                            "dayTo": 9,
                                            "isDefault": True,
                                        }
                                    ],
                                },
                            },
                        ],
                    }
                },
            )

    def test_non_available_offer_delivery_stats(self):
        """Что тестируем: учет офферов с available=False в статистиках фильтров

        Проверяем, что у оффер с available=False не попадает в статистику
         по срокам доставки
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=31&how=aprice'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "delivery-interval",
                            "type": "boolean",
                            "name": "Срок доставки",
                            "subType": "",
                            "kind": 2,
                            "hasBoolNo": True,
                            "values": [{"found": 2, "value": "5"}],
                        }
                    ]
                },
                preserve_order=True,
            )
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "1"}]}]})
            self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval", "values": [{"value": "0"}]}]})

    def test_non_available_offer_delivery_filter(self):
        """Что тестируем: офферы с available=False отфильтровываются фильтром по
        сроку доставки

        Проверяем, что у оффер с available=False не попадает в статистику
         по срокам доставки
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=31&how=aprice&delivery_interval=5'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "results": [
                            {"entity": "offer", "titles": {"raw": "available-without-buckets"}},
                            {"entity": "offer", "titles": {"raw": "available-offer"}},
                        ],
                    }
                },
            )

    @classmethod
    def prepare_default_local_delivery_option(cls):
        cls.index.shops += [
            Shop(fesh=8, priority_region=213),
            Shop(fesh=9, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                fesh=8,
                hyperid=32,
                price=50,
                title='cheap-delivery-local-offer',
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=1),
                    DeliveryOption(price=100, day_from=1, day_to=4),
                ],
            ),
            Offer(
                fesh=9,
                hyperid=32,
                price=10,
                title='expensive-delivery-local-offer',
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=1),
                    DeliveryOption(price=200, day_from=1, day_to=32),
                ],
            ),
        ]

    def test_local_delivery_yml_default_option(self):
        """Что тестируем: алгоритм выбора опции по умолчанию среди опций из YML

        Проверяем, что среди быстрых и дешевых опций доставки в YML
        выбирается наиболее дешевая, вне зависимости от сроков
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=32&how=aprice'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "delivery": {
                                "options": [
                                    {
                                        "price": {"currency": "RUR", "value": "200", "isDeliveryIncluded": False},
                                        "dayFrom": Absent(),
                                        "dayTo": Absent(),
                                        "isDefault": True,
                                        "serviceId": "99",
                                    },
                                    {
                                        "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                        "dayFrom": 1,
                                        "dayTo": 1,
                                        "isDefault": False,
                                        "serviceId": "99",
                                    },
                                ]
                            },
                        },
                        {
                            "entity": "offer",
                            "delivery": {
                                "options": [
                                    {
                                        "price": {"currency": "RUR", "value": "100", "isDeliveryIncluded": False},
                                        "dayFrom": 1,
                                        "dayTo": 4,
                                        "isDefault": True,
                                        "serviceId": "99",
                                    },
                                    {
                                        "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                        "dayFrom": 1,
                                        "dayTo": 1,
                                        "isDefault": False,
                                        "serviceId": "99",
                                    },
                                ]
                            },
                        },
                    ]
                },
                preserve_order=True,
            )

    def test_local_delivery_yml_sorts(self):
        """Что тестируем: сортировка по цене с учетом доставки учитывает
        дефолтную опцию (самую дешевую)

        Проверяем, офферы отсортированы в порядке увеличения цены доставки,
        а не цены оффера
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json(
                'place={}&rids=213&hyperid=32&how=aprice&deliveryincluded=1'.format(place)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "cheap-delivery-local-offer"},
                            "delivery": {
                                "options": [
                                    {
                                        "price": {"currency": "RUR", "value": "100", "isDeliveryIncluded": False},
                                        "dayFrom": 1,
                                        "dayTo": 4,
                                        "isDefault": True,
                                        "serviceId": "99",
                                    },
                                    {
                                        "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                        "dayFrom": 1,
                                        "dayTo": 1,
                                        "isDefault": False,
                                        "serviceId": "99",
                                    },
                                ]
                            },
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "expensive-delivery-local-offer"},
                            "delivery": {
                                "options": [
                                    {
                                        "price": {"currency": "RUR", "value": "200", "isDeliveryIncluded": False},
                                        "dayFrom": Absent(),
                                        "dayTo": Absent(),
                                        "isDefault": True,
                                        "serviceId": "99",
                                    },
                                    {
                                        "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                        "dayFrom": 1,
                                        "dayTo": 1,
                                        "isDefault": False,
                                        "serviceId": "99",
                                    },
                                ]
                            },
                        },
                    ]
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_delivery_methods(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=260,
                fesh=1,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(rid=213),
                ],
            ),
            # Неправильный регион
            DeliveryBucket(bucket_id=262, fesh=1, carriers=[4], regional_options=[RegionalDelivery(rid=84)]),
            # Доставка в нужный регион запрещена
            DeliveryBucket(
                bucket_id=263,
                fesh=1,
                carriers=[5],
                regional_options=[
                    RegionalDelivery(rid=213, forbidden=True),
                ],
            ),
            # То же что и выше, но для флажка unknown
            DeliveryBucket(
                bucket_id=264,
                fesh=1,
                carriers=[6],
                regional_options=[
                    RegionalDelivery(rid=213, unknown=True),
                ],
            ),
            # Перевозчик 1 уже есть в другом бакете
            DeliveryBucket(
                bucket_id=265,
                fesh=1,
                carriers=[1],
                regional_options=[
                    RegionalDelivery(rid=213),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1, hyperid=40, title='', delivery_buckets=[260, 261, 262, 263, 264]),
        ]

    def test_delivery_methods(self):
        """
        Проверяем, что список перевозчиков формируется на основе списков
        перевозчиков из regional_delivery.mmap
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=40'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "availableServices": [
                        {"serviceId": 1},
                        {"serviceId": 3},
                        {"serviceId": 6},
                    ]
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_equal_options(cls):
        '''Создаем бакет с одинаковыми опциями доставки и оффес с одинаковыми
        локальными опции
        '''
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=250,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=163,
                        options=[
                            DeliveryOption(price=500, day_from=1, day_to=4, order_before=21),
                            DeliveryOption(price=500, day_from=1, day_to=4, order_before=21),
                            DeliveryOption(price=500, day_from=1, day_to=4, order_before=21),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=9,
                hyperid=50,
                title='same-delivery-options',
                delivery_buckets=[250],
                delivery_options=[
                    DeliveryOption(price=300, day_from=1, day_to=4, order_before=21),
                    DeliveryOption(price=300, day_from=1, day_to=4, order_before=21),
                    DeliveryOption(price=300, day_from=1, day_to=4, order_before=21),
                ],
            ),
        ]

    def test_same_options_local(self):
        """Что тестируем: одинаковые опции доставки не перезатирают "дефолтную" опцию доставки
        в приоритетном регионе магазина

        Проверяем, что на выдаче есть ровно одна доставки с признаком 'default'
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=50'.format(place))
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "300", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 4,
                                "orderBefore": "21",
                                "isDefault": True,
                            }
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    def test_same_options_non_local(self):
        """Что тестируем: одинаковые опции доставки не перезатирают "дефолтную" опцию доставки
        в неприоритетном регионе магазина

        Проверяем, что на выдаче есть ровно одна доставки с признаком 'default'
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=163&hyperid=50'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "500", "isDeliveryIncluded": False},
                                "dayFrom": 1,
                                "dayTo": 4,
                                "orderBefore": "22",
                                "isDefault": True,
                            }
                        ]
                    }
                },
                preserve_order=True,
                allow_different_len=False,
            )

    @classmethod
    def prepare_delivery_region_section(cls):
        '''Создаем бакет для региона 1 с неизвестными опциями доставки
        Создаем оффер с опциями доставки в фиде и этим бакетом
        '''
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=280,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=1, unknown=True),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=9,
                hyperid=80,
                title='yml-delivery-options',
                delivery_buckets=[280],
                delivery_options=[
                    DeliveryOption(price=300, day_from=1, day_to=4),
                ],
            ),
        ]

    def test_delivery_region_section(self):
        """Что тестируем: в локальном регионе при наличии опций и в yml и в бакетах
        в теге <delivery> и секции delivery: { region : {} } выводится приоритетный
        регион магазина, а не регион определенный в бакете

        Проверяем, что на выдаче правильный регион (213), а не регион из бакета (1)
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=80'.format(place))
            self.assertFragmentIn(response, {"search": {"total": 1}}, preserve_order=True)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "region": {
                            "entity": "region",
                            "id": 213,
                            "name": "Москва",
                            "lingua": {"name": {"genitive": "Москва", "preposition": " ", "prepositional": "Москва"}},
                        },
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_priority_delivery_unknown(cls):
        '''Создаем бакет для региона 1 с неизвестными опциями доставки
        Создаем оффер с этим бакетом и без опций доставки в фиде
        '''

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=290,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=1, unknown=True),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=9, hyperid=90, title='no-delivery-options', delivery_buckets=[290], has_delivery_options=False),
        ]

    def test_priority_delivery_unknown(self):
        """Что тестируем: в локальном регионе при отсуствии явно заданных опций
        (в yml или бакетах) оффер считается не доставляемым курьером

        Проверяем, что при запросе в приоритетном регионе (213) оффер
        не показывается на выдаче и отфильтровывается по признаку доставки,
        а в неприоритетном регионе (1) выводится без опций доставки
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=90&debug=1&debug-doc-count=1'.format(place))
            self.assertFragmentIn(response, {"search": {"total": 0}}, preserve_order=True)
            self.assertFragmentIn(response, {"DROP_REASON": "DELIVERY"})

            response = self.report.request_json('place={}&rids=1&hyperid=90&debug=1&debug-doc-count=1'.format(place))
            self.assertFragmentIn(response, {"search": {"total": 1}}, preserve_order=True)
            self.assertFragmentNotIn(response, {"delivery": {"options": [{"price": {}}]}}, preserve_order=True)

    @classmethod
    def prepare_priority_delivery_forbidden_yml(cls):
        '''Создаем бакет для региона 213 с запрещенными опциями доставки
        Создаем оффер с этим бакетом и опциями доставки в фиде
        '''

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=291,
                fesh=9,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=213, forbidden=True),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=9,
                hyperid=91,
                title='forbidden_with_yml',
                delivery_buckets=[291],
                delivery_options=[
                    DeliveryOption(price=300, day_from=1, day_to=2),
                ],
            ),
        ]

    def test_priority_delivery_forbidden_yml(self):
        """Что тестируем: в локальном регионе при запрещенной доставке
        в бакетах и наличии опций в yml оффер считается доставляемым

        Проверяем, что при запросе в приоритетном регионе (213) оффер
        показывается на выдаче и приоритетная доставка есть
        """
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=91&'.format(place))
            self.assertFragmentIn(response, {"search": {"total": 1}}, preserve_order=True)
            self.assertFragmentNotIn(response, {"delivery": {"isPrority": True}}, preserve_order=True)

    @classmethod
    def prepare_delivery_in_villages(cls):
        cls.index.shops += [
            Shop(fesh=10, priority_region=213, name='Для теста фильтров, офферы с доставкой во сёла'),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=401,
                fesh=10,
                carriers=[1],
                regional_options=[
                    # Москва
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=100, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                    # Новомосковский округ
                    RegionalDelivery(
                        rid=114619,
                        options=[
                            DeliveryOption(price=200, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                    # Щербинка
                    RegionalDelivery(
                        rid=21624,
                        options=[
                            DeliveryOption(price=300, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=402,
                fesh=10,
                carriers=[1],
                regional_options=[
                    # Москва
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=400, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                    # Новомосковский округ
                    RegionalDelivery(
                        rid=114619,
                        options=[
                            DeliveryOption(price=500, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                    # Внуково
                    RegionalDelivery(
                        rid=10720,
                        options=[
                            DeliveryOption(price=600, day_from=0, day_to=3, order_before=24),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                fesh=10,
                hyperid=101,
                title='Village test: new mosow',
                delivery_buckets=[401],
                has_delivery_options=False,
            ),
            Offer(
                fesh=10,
                hyperid=102,
                title='Village test: new mosow village',
                delivery_buckets=[402],
                has_delivery_options=False,
            ),
        ]

    def test_delivery_in_villages(self):
        '''
        Что тестируем: Доставка с субъекты меньше чем город
            Дерево регионов:
            Москва(город)
                -> Новомосковский округ
                    -> Щербинка(город)
                    -> Внуково(деревня)

            0. Указаны опции для Москвы, проверяем что именно они и мспользуются
            1. Работает для региона меньше чем город (деревня в округ), стоимость указана для округа
               Для пользователя во Внуково берём опции для Новомосковского округа
            1. Работает для город а в округе города (город в округе), стоимость указана для города в округе
               Для пользователя в Щербинке берём опции для Щербинки
            3. Работает для региона меньше чем город (деревня в округе), стоимость указана для округа и деревни в округе
               Для пользователя во Внуково берём опции для Внуково
            4. Работает для города в округе города (город округ), стоимость указана для округа
               Для пользователя в Щербинке берём опции для Новомосковского округа
        '''

        expects = [
            {"hyperid": 101, "rids": 213, "price": 100, "delivery": 213},  # Москва
            {
                "hyperid": 101,
                "rids": 10720,
                "price": 200,
                "delivery": 114619,
            },  # Внуково, т.к. берётся из Новомосковского округ
            {"hyperid": 101, "rids": 21624, "price": 300, "delivery": 21624},  # Щербинка
            {"hyperid": 102, "rids": 10720, "price": 600, "delivery": 10720},  # Внуково
            {
                "hyperid": 102,
                "rids": 21624,
                "price": 500,
                "delivery": 114619,
            },  # Щербинка, т.к. берётся из Новомосковского округ
        ]
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for expect in expects:
            response = self.report.request_json(
                'place=productoffers&rids={}&hyperid={}'.format(expect["rids"], expect["hyperid"]) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "region": {"id": expect["delivery"]},
                        "options": [{"price": {"value": "{}".format(expect["price"])}}],
                    }
                },
                preserve_order=True,
            )

    @classmethod
    def prepare_regional_order_before_on_weekend(cls):
        """
        Создаем магазины, конфигурация календарей которых
        соответствует пятнице, субботе, воскресенью и понедельнику
        в регионах 213 и 75
        Создаем офферы в этих магазинах с опцией доставки в тот же
        день при заказе до 6 часов
        """
        cls.index.regiontree += [
            Region(
                rid=11409,
                name='Приморский край',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=75, name='Владивосток', tz_offset=36000),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=111, priority_region=213),
            Shop(fesh=112, priority_region=213),
            Shop(fesh=113, priority_region=213),
            Shop(fesh=114, priority_region=213),
            Shop(fesh=115, priority_region=75),
            Shop(fesh=116, priority_region=75),
            Shop(fesh=117, priority_region=75),
            Shop(fesh=118, priority_region=75),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=111, holidays=[1, 2]),
            DeliveryCalendar(fesh=112, holidays=[0, 1]),
            DeliveryCalendar(fesh=113, holidays=[0]),
            DeliveryCalendar(fesh=115, holidays=[1, 2]),
            DeliveryCalendar(fesh=116, holidays=[0, 1]),
            DeliveryCalendar(fesh=117, holidays=[0]),
        ]

        delivery_option = DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=411,
                fesh=111,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=213, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=412,
                fesh=112,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=213, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=413,
                fesh=113,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=213, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=414,
                fesh=114,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=213, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=415,
                fesh=115,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=75, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=416,
                fesh=116,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=75, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=417,
                fesh=117,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=75, options=[delivery_option]),
                ],
            ),
            DeliveryBucket(
                bucket_id=418,
                fesh=118,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=75, options=[delivery_option]),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=111, hyperid=211, title='friday_offer_moscow', delivery_buckets=[411]),
            Offer(fesh=112, hyperid=212, title='saturday_offer_moscow', delivery_buckets=[412]),
            Offer(fesh=113, hyperid=213, title='sunday_offer_moscow', delivery_buckets=[413]),
            Offer(fesh=114, hyperid=214, title='monday_offer_moscow', delivery_buckets=[414]),
            Offer(fesh=115, hyperid=211, title='friday_offer_vladivostok', delivery_buckets=[415]),
            Offer(fesh=116, hyperid=212, title='saturday_offer_vladivostok', delivery_buckets=[416]),
            Offer(fesh=117, hyperid=213, title='sunday_offer_vladivostok', delivery_buckets=[417]),
            Offer(fesh=118, hyperid=214, title='monday_offer_vladivostok', delivery_buckets=[418]),
        ]

    def test_regional_order_before_on_weekend(self):
        """
        Что тестируем: при запросе в выходной день службы доставки магазина
        параметр order-before игнорируется и убирается из выдачи

        Задаем ряд запросов с пятницы по воскресенье и проверяем, что
        сроки доставки отображают доставку в понедельник при заказе
        после 6 часов пятницы
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        # Запрос в пятницу до order_before (Москва). Ожидаем, что доставка сегодня
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=211'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 0,
                                "dayTo": 0,
                                "orderBefore": "6",
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в пятницу после order_before (Владивосток)
        # Ожидаем, что доставка в понедельник (через 3 дня)
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=75&hyperid=211'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 3,
                                "dayTo": 3,
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в субботу до order_before (Москва)
        # Ожидаем, что доставка в понедельник (через 2 дня)
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=212'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 2,
                                "dayTo": 2,
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в субботу после order_before (Владивосток)
        # Ожидаем, что доставка в понедельник (через 2 дня)
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=75&hyperid=212'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 2,
                                "dayTo": 2,
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в воскресенье до order_before (Москва)
        # Ожидаем, что доставка в понедельник (через 1 день)
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=213'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 1,
                                "dayTo": 1,
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в воскресенье после order_before (Владивосток)
        # Ожидаем, что доставка в понедельник (через 1 день)
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=75&hyperid=213'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 1,
                                "dayTo": 1,
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в понедельник до order_before (Москва). Ожидаем, что доставка сегодня
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=214'.format(place) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "options": [
                            {
                                "price": {"currency": "RUR", "value": "100"},
                                "dayFrom": 0,
                                "dayTo": 0,
                                "orderBefore": "6",
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

    # MARKETOUT-13060
    @classmethod
    def prepare_non_integer_delivery(cls):
        cls.index.shops += [
            Shop(fesh=1306001, priority_region=213),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=1306001, holidays=[0]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=13061,
                fesh=1306001,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=123.45, day_from=0, day_to=0, order_before=6),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1306001, hyperid=213, title='noninteger_offer', delivery_buckets=[13061]),
        ]

    def test_non_integer_delivery(self):
        """
        Делаем запрос, проверяем, что пришла округлённая цена доставки
        """
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&rids=213&text=noninteger' + unified_off_flags)
        self.assertFragmentIn(
            response, {"entity": "offer", "delivery": {"options": [{"price": {"value": "123", "currency": "RUR"}}]}}
        )

    @classmethod
    def prepare_longer_delivery_intervals(cls):
        """Создаем магазин с тремя офферами, с разными бакетами - из
        локального и нелокального региона с длительными сроками
        """
        cls.index.shops += [
            Shop(fesh=1563101, priority_region=213),
        ]

        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=1563101, holidays=[0, 57, 58]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=431,
                fesh=1563101,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=150, day_from=30, day_to=31, order_before=6),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=432,
                fesh=1563101,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=200, day_from=50, day_to=59, order_before=6),
                        ],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=433,
                fesh=1563101,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=250, day_from=35, day_to=57, order_before=6),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=1563101, hyperid=156311101, hid=1563111, delivery_buckets=[431]),
            Offer(fesh=1563101, hyperid=156311201, hid=1563112, delivery_buckets=[432]),
            Offer(fesh=1563101, hyperid=156311301, hid=1563113, delivery_buckets=[433]),
        ]

    def test_longer_delivery_intervals(self):
        """Что тестируем: вывод длительных сроков доставки
        в приоритетном и неприоритетном регионах

        Проверяем, что в приоритетном регионе сроки больше 31 дня
        не приходят на выдачу ("на заказ" на фронте).

        Проверяем, что в неприоритетном регионе сроки больше 60 дней
        не приходят на выдачу

        Проверяем, что в неприоритетном регионе срок в 60 дней
        приходит на выдачу
        """
        # Запрос в приоритетном регионе, 31 день в бакете возрастает до 32 дней
        # за счет календаря
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for place_params in ['prime', 'productoffers&hyperid=156311101']:
            response = self.report.request_json(
                'place={}&rids=213&hid=1563111'.format(place_params) + unified_off_flags
            )
            self.assertFragmentIn(
                response,
                {
                    'delivery': {
                        'options': [
                            {
                                "price": {"currency": "RUR", "value": "150"},
                                "dayFrom": Absent(),
                                "dayTo": Absent(),
                                "orderBefore": Absent(),
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в неприоритетном регионе, 59 дней в бакете возрастают до 62 дней
        # за счет календаря и поэтому скрываются
        for place_params in ['prime', 'productoffers&hyperid=156311201']:
            response = self.report.request_json('place={}&rids=2&hid=1563112'.format(place_params) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    'delivery': {
                        'options': [
                            {
                                "price": {"currency": "RUR", "value": "200"},
                                "dayFrom": Absent(),
                                "dayTo": Absent(),
                                "orderBefore": Absent(),
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

        # Запрос в неприоритетном регионе, 58 дней в бакете возрастают до 60 дней
        # за счет календаря
        for place_params in ['prime', 'productoffers&hyperid=156311301']:
            response = self.report.request_json('place={}&rids=2&hid=1563113'.format(place_params) + unified_off_flags)
            self.assertFragmentIn(
                response,
                {
                    'delivery': {
                        'options': [
                            {
                                "price": {"currency": "RUR", "value": "250"},
                                "dayFrom": 36,
                                "dayTo": 60,
                                "orderBefore": Absent(),
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

    def test_long_local_delivery_enabled(self):
        # Запрос в приоритетном регионе, 31 день в бакете возрастает до 32 дней
        # за счет календаря
        open_option = {
            "dayFrom": 31,
            "isEstimated": True,
            "dayTo": 32,
        }
        closed_option = {
            "dayFrom": Absent(),
            "dayTo": Absent(),
            "isEstimated": Absent(),
        }
        for flag, option in [
            (None, closed_option),
            (0, closed_option),
            (1, open_option),
        ]:
            rearr_flag = ''
            if flag is not None:
                rearr_flag += '&rearr-factors=market_long_local_delivery={}'.format(flag)

            for place_params in ['prime', 'productoffers&hyperid=156311101']:
                response = self.report.request_json('place={}&rids=213&hid=1563111'.format(place_params) + rearr_flag)
                self.assertFragmentIn(
                    response,
                    {'delivery': {'options': [option]}},
                    allow_different_len=False,
                )


if __name__ == '__main__':
    main()  # каждую группу тестов можно запускать независимо по файлу
