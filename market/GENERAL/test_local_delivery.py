#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
from unittest import skip

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    ExchangeRate,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import ElementCount, NotEmpty

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class T(TestCase):
    """
    Tests that local delivery options is calculated according to
    https://wiki.yandex-team.ru/market/projects/multiregion/feed-update/dev/

    What is tested:
      * <local-delivery-options> section
      * <filters> and <filters-applied> for local delivery
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # TODO: MARKETOUT-47769 вернуть как было. Удалить значения
        cls.settings.default_search_experiment_flags += ['market_hide_long_delivery_offers=0']

        cls.settings.use_delivery_statistics = True
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва', tz_offset=10800),
                    Region(rid=10758, name='Химки', tz_offset=10800),
                ],
            ),
            Region(
                rid=11409,
                name='Приморский край',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=75, name='Владивосток', tz_offset=36000),
                ],
            ),
            Region(
                rid=29349,
                name='Штат Нью-Йорк',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=202, name='Нью-Йорк', tz_offset=-18000),
                ],
            ),
        ]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, name='Московская пепячечная "Доставляем"'),
            Shop(fesh=2, priority_region=75, regions=[213], name='Пепячечная во Владивостоке "Доставляем в Москву"'),
            Shop(fesh=3, priority_region=202, name='Нью-Йорская пепячечная. Еще вчерашний день в полночь по UTC'),
            Shop(fesh=4, priority_region=213, name='Московская пепячечная "Только самовывоз"'),
            Shop(fesh=5, priority_region=213, name='Московская пепячечная "Не доставляем по выходным (см. календарь)"'),
            Shop(
                fesh=6,
                priority_region=213,
                name='Московская пепячечная "Доставляем, но календарь неизвестен"',
                has_delivery_calendar=False,
            ),
            Shop(fesh=8, priority_region=213, name='Московская пепячечная для доставки моделей'),
        ]

        cls.index.outlets += [
            Outlet(point_id=1, fesh=4, region=213, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7001,
                fesh=4,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hyperid=101, title='model-with-free-shipping-and-fast-option'),
            Model(hyperid=102, title='model-with-free-and-fast-shipping'),
            Model(hyperid=103, title='model-with-free-shipping'),
            Model(hyperid=104, title='model-without-free-shipping'),
        ]
        cls.index.offers += [
            Offer(
                fesh=1,
                title='many-options-with-free-delivery-in-moscow',
                hyperid=2,
                delivery_options=[
                    DeliveryOption(price=28000, day_from=1, day_to=2, order_before=23),
                    DeliveryOption(price=500, day_from=1, day_to=2, order_before=23),
                    DeliveryOption(price=0, day_from=2, day_to=6, order_before=24),
                ],
            ),
            Offer(
                fesh=1,
                title='many-options-with-fast-and-free-delivery-in-moscow',
                hyperid=2,
                delivery_options=[
                    DeliveryOption(price=28000, day_from=0, day_to=1, order_before=23),
                    DeliveryOption(price=500, day_from=1, day_to=1, order_before=23),
                    DeliveryOption(price=0, day_from=1, day_to=2, order_before=24),
                ],
            ),
            Offer(fesh=1, title='one-option-no-free-delivery-in-moscow', hyperid=2),
            Offer(
                fesh=1,
                title='one-option-free-delivery-in-moscow',
                hyperid=2,
                delivery_options=[
                    DeliveryOption(price=0, day_from=32, day_to=32, order_before=24),
                ],
            ),
            Offer(
                fesh=1,
                title='two-options-with-free-delivery-in-moscow',
                hyperid=2,
                delivery_options=[
                    DeliveryOption(price=0, day_from=32, day_to=32, order_before=24),
                    DeliveryOption(price=500, day_from=1, day_to=2, order_before=2),
                ],
            ),
            Offer(
                fesh=2,
                title='one-option-free-delivery-in-vladivostok',
                hyperid=2,
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=2, order_before=24),
                ],
            ),
            Offer(
                fesh=2,
                title='one-option-slow-delivery-in-vladivostok',
                hyperid=2,
                delivery_options=[
                    DeliveryOption(price=0, day_from=1, day_to=10, order_before=24),
                ],
            ),
            Offer(fesh=2, title='no-delivery-options-in-vladivostok', has_delivery_options=False),
            Offer(
                fesh=3,
                title='many-delivery-options-in-NY',
                delivery_options=[
                    DeliveryOption(price=28000, day_from=1, day_to=2, order_before=24),
                    DeliveryOption(price=500, day_from=1, day_to=2, order_before=23),
                    DeliveryOption(price=0, day_from=2, day_to=6, order_before=24),
                ],
            ),
            Offer(fesh=4, title='no-delivery-options-in-moscow', has_delivery_options=False, pickup_buckets=[7001]),
            Offer(
                fesh=5,
                title='offer-with-calendar-in-moscow',
                delivery_options=[
                    DeliveryOption(price=500, day_from=0, day_to=2, order_before=24),
                    DeliveryOption(price=300, day_from=2, day_to=5, order_before=23),
                    DeliveryOption(price=0, day_from=32, day_to=32, order_before=24),
                ],
            ),
            Offer(
                fesh=6,
                title='offer-without-calendar-in-moscow',
                delivery_options=[
                    DeliveryOption(price=500, day_from=0, day_to=2, order_before=24),
                ],
            ),
            Offer(
                fesh=1,
                title='checkout-1',
                delivery_options=[
                    DeliveryOption(price=200, day_from=0, day_to=1, order_before=24),
                    DeliveryOption(price=100, day_from=0, day_to=1, order_before=12),
                    DeliveryOption(price=0, day_from=1, day_to=2, order_before=24),
                ],
            ),
            Offer(
                fesh=1,
                title='checkout-2',
                delivery_options=[
                    DeliveryOption(price=100, day_from=0, day_to=1, order_before=23),
                    DeliveryOption(price=200, day_from=0, day_to=0, order_before=2),
                    DeliveryOption(price=0, day_from=1, day_to=2, order_before=24),
                ],
            ),
            Offer(
                fesh=1,
                title='checkout-3',
                delivery_options=[
                    DeliveryOption(price=100, day_from=0, day_to=1, order_before=24),
                    DeliveryOption(price=200, day_from=0, day_to=1, order_before=24),
                    DeliveryOption(price=0, day_from=1, day_to=2, order_before=24),
                ],
            ),
            # Model statistics test offers
            Offer(
                fesh=8,
                title='offer-with-free-shipping-and-fast-option',
                hyperid=101,
                delivery_options=[
                    DeliveryOption(price=500, day_from=1, day_to=1, order_before=23),
                    DeliveryOption(price=0, day_from=2, day_to=5, order_before=24),
                ],
            ),
            Offer(
                fesh=8,
                title='offer-with-free-and-fast-shipping',
                hyperid=102,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=0, order_before=24)],
            ),
            Offer(
                fesh=8,
                title='offer-with-free-shipping',
                hyperid=103,
                delivery_options=[DeliveryOption(price=0, day_from=2, day_to=5, order_before=24)],
            ),
            Offer(
                fesh=8,
                title='offer-without-free-shipping',
                hyperid=104,
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=1, order_before=24)],
            ),
        ]
        cls.index.delivery_calendars += [
            DeliveryCalendar(fesh=5, holidays=[2, 3, 6, 7, 13, 14, 20, 21, 27, 28]),
        ]

        cls.index.currencies = [Currency('BYR', exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=263.36)])]

    def test_delivery_time_present(self):
        response = self.report.request_json(
            'place=prime&rids=213&text=many-options-with-free-delivery-in-moscow&numdoc=48'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "many-options-with-free-delivery-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "dayFrom": 1,
                            "dayTo": 2,
                            "orderBefore": "23",
                        },
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },
                            "isDefault": True,
                        },
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "many-options-with-free-delivery-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "28000",
                            }
                        }
                    ]
                },
            },
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "many-options-with-free-delivery-in-moscow"},
                "delivery": {"inStock": True},
            },
        )

    def test_another_home_rids_timezone(self):
        response = self.report.request_json(
            'place=prime&text=many-options-with-free-delivery-in-moscow&rids=213&home-rids=75&numdoc=48'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "many-options-with-free-delivery-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },
                            "dayFrom": 2,
                            "dayTo": 6,
                            "isDefault": True,
                        },
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "dayFrom": 1,
                            "dayTo": 2,
                            "orderBefore": "6",
                        },
                    ]
                },
            },
        )

    def test_another_home_rids_timezone_yesterday(self):
        response = self.report.request_json('place=prime&text=many-delivery-options-in-NY&rids=202&home-rids=213')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "many-delivery-options-in-NY"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },
                            "dayFrom": 2,
                            "dayTo": 6,
                            "isDefault": True,
                        },
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "dayFrom": 1,
                            "dayTo": 2,
                            "orderBefore": "7",
                        },
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "28000",
                            },
                            "dayFrom": 1,
                            "dayTo": 2,
                        },
                    ]
                },
            },
        )

    def test_delivery_time_absence_in_different_region(self):
        response = self.report.request_json(
            'place=prime&text=one-option-free-delivery-in-vladivostok&rids=213&home-rids=213&numdoc=48'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "one-option-free-delivery-in-vladivostok"},
                "delivery": {"options": ElementCount(0)},
            },
        )

    def test_delivery_currency(self):
        response = self.report.request_json(
            'place=prime&text=two-options-with-free-delivery-in-moscow&rids=213&home-rids=213&currency=BYR&numdoc=48'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "two-options-with-free-delivery-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "BYR",
                                "value": "0",
                            },
                            "isDefault": True,
                        },
                        {
                            "price": {
                                "currency": "BYR",
                                "value": "131680",
                            },
                            "dayFrom": 2,
                            "dayTo": 3,
                        },
                    ],
                    "inStock": False,
                },
            },
        )

    def test_raw_delivery_options(self):
        response = self.report.request_json(
            'place=prime&text=two-options-with-free-delivery-in-moscow&rids=213&home-rids=213&raw_delivery_options=1&numdoc=48'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "two-options-with-free-delivery-in-moscow"},
                "delivery": {"options": [{"rawDayTo": "2", "rawOrderBefore": "2"}]},
            },
        )

    def test_raw_delivery_options_absence(self):
        response = self.report.request_json(
            'place=prime&text=one-option-free-delivery-in-moscow&rids=213&home-rids=213&raw_delivery_options=1&numdoc=48'
        )
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "one-option-free-delivery-in-moscow"}})
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "one-option-free-delivery-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "rawDayTo": NotEmpty(),
                        }
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "one-option-free-delivery-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "rawOrderBefore": NotEmpty(),
                        }
                    ]
                },
            },
        )

    def test_no_delivery_offer(self):
        response = self.report.request_json('place=prime&text=no-delivery-options-in-moscow&rids=213&home-rids=213')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "no-delivery-options-in-moscow"},
                "delivery": {"inStock": True, "options": ElementCount(0)},
            },
        )

    def test_free_delivery_filter(self):
        response = self.report.request_json('place=prime&text=delivery&rids=213&home-rids=213&free_delivery=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "free-delivery", "values": [{"initialFound": 1, "checked": True, "found": 4, "value": "1"}]}
                ]
            },
        )
        self.assertEqual(response.count({"price": {"value": "0"}, "isDefault": True}), 4)

    def test_delivery_interval_filter(self):
        response = self.report.request_json('place=prime&text=delivery&rids=213&home-rids=213&delivery_interval=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "delivery-interval",
                        "values": [
                            {"checked": True, "found": 1, "value": "1"},
                            {"found": 4, "value": "5"},
                        ],
                    }
                ]
            },
        )
        self.assertEqual(response.count({"entity": "offer"}), 1)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "options": [
                        {"dayTo": 1, "isDefault": True},
                    ]
                },
            },
        )

    def test_delivery_interval_filter_another_region(self):
        response = self.report.request_json('place=prime&text=delivery&rids=75&home-rids=213&delivery_interval=5')
        self.assertFragmentIn(
            response,
            {"filters": [{"id": "delivery-interval", "values": [{"checked": True, "found": 1, "value": "5"}]}]},
        )
        self.assertEqual(response.count({"entity": "offer"}), 1)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "options": [
                        {"dayTo": 2, "isDefault": True},
                    ]
                },
            },
        )

    def test_delivery_filter_stats(self):
        response = self.report.request_json('place=prime&text=delivery&rids=213&home-rids=213')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "free-delivery", "values": [{"initialFound": 1, "found": 4, "value": "1"}]},
                    {
                        "id": "delivery-interval",
                        "values": [
                            {"found": 1, "value": "1"},
                            {"found": 4, "value": "5"},
                        ],
                    },
                ]
            },
        )

    def test_delivery_filter_stats_another_region(self):
        response = self.report.request_json('place=prime&text=delivery&rids=75&home-rids=213')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "free-delivery", "values": [{"initialFound": 1, "found": 2, "value": "1"}]},
                    {
                        "id": "delivery-interval",
                        "values": [
                            {"found": 1, "value": "5"},
                        ],
                    },
                ]
            },
        )

    def test_free_delivery_filter_another_region(self):
        response = self.report.request_json('place=prime&text=vladivostok&rids=213&home-rids=213&free_delivery=1')
        self.assertFragmentNotIn(response, {"filters": [{"id": "delivery-interval"}]})
        self.assertEqual(response.count({"entity": "offer"}), 0)

    def test_delivery_interval_filter_another_region2(self):
        response = self.report.request_json('place=prime&text=vladivostok&rids=213&home-rids=213&delivery_interval=1')
        self.assertFragmentIn(
            response,
            {"id": "delivery-interval", "values": [{"found": 0, "checked": True, "value": "1"}]},
            allow_different_len=False,
        )
        self.assertEqual(response.count({"entity": "offer"}), 0)

    @skip('https://st.yandex-team.ru/MARKETOUT-47947')
    def test_delivery_calendars(self):
        response = self.report.request_json(
            'place=prime&rids=213&text=offer-with-calendar-in-moscow' + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer-with-calendar-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "dayFrom": 0,
                            "dayTo": 4,
                        },
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "300",
                            },
                            "dayFrom": 4,
                            "dayTo": 9,
                            "orderBefore": "23",
                        },
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "0",
                            },
                            "isDefault": True,
                        },
                    ],
                    "inStock": True,
                },
            },
        )

    def test_delivery_without_calendar(self):
        response = self.report.request_json('place=prime&rids=213&text=offer-without-calendar-in-moscow&numdoc=48')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer-without-calendar-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "isDefault": True,
                        },
                    ],
                    "inStock": False,
                },
            },
        )

    def test_models_free_delivery_filter(self):
        response = self.report.request_json('place=prime&text=model&rids=213&free_delivery=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "free-delivery", "values": [{"initialFound": 1, "checked": True, "value": "1"}]},
                    {
                        "id": "delivery-interval",
                        "values": [
                            {"found": 1, "value": "0"},
                            {"found": 3, "value": "5"},
                        ],
                    },
                ]
            },
        )
        self.assertEqual(response.count({"type": "model"}), 0)

    def test_models_delivery_interval_filter(self):
        response = self.report.request_json('place=prime&text=model&rids=213&delivery_interval=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "free-delivery", "values": [{"initialFound": 1, "value": "1"}]},
                    {
                        "id": "delivery-interval",
                        "values": [
                            {"found": 1, "value": "0"},
                            {"checked": True, "found": 3, "value": "1"},
                            {"found": 4, "value": "5"},
                        ],
                    },
                ]
            },
        )
        self.assertEqual(response.count({"type": "model"}), 0)

    def test_models_all_delivery_filters(self):
        response = self.report.request_json('place=prime&text=model&rids=213&delivery_interval=1&free_delivery=1')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "free-delivery", "values": [{"checked": True, "initialFound": 1, "value": "1"}]},
                    {
                        "id": "delivery-interval",
                        "values": [
                            {"found": 1, "value": "0"},
                            {"checked": True, "found": 1, "value": "1"},
                            {"found": 3, "value": "5"},
                        ],
                    },
                ]
            },
        )
        self.assertEqual(response.count({"type": "model"}), 0)

    # See MARKETOUT-9170
    @skip('https://st.yandex-team.ru/MARKETOUT-47947')
    def test_delivery_local_options_present_with_regset_1(self):
        response = self.report.request_json(
            'place=prime&rids=213&text=offer-with-calendar-in-moscow&regset=1' + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "offer-with-calendar-in-moscow"},
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "dayFrom": 0,
                            "dayTo": 4,
                        }
                    ]
                },
            },
        )

    @classmethod
    def prepare_delivery_interval_sorting(cls):
        cls.index.shops += [Shop(fesh=101, priority_region=213), Shop(fesh=102, priority_region=213)]
        cls.index.outlets += [
            Outlet(point_id=2, fesh=102, region=213, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=7002,
                fesh=102,
                carriers=[99],
                options=[PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                title="offer goes first",
                fesh=101,
                hyperid=1001,
                delivery_options=[
                    DeliveryOption(price=5000, day_from=1, day_to=2, order_before=23),
                ],
            ),
            Offer(
                title="offer goes second",
                fesh=102,
                hyperid=1001,
                pickup=True,
                has_delivery_options=False,
                pickup_buckets=[7002],
            ),
        ]

    def test_delivery_interval_sorting(self):
        """
        MARKETOUT-12394
        There are two offers: with and without delivery.
        Check that at ranking by delivery intervals offer with delivery goes first
        """
        response = self.report.request_json('place=productoffers&rids=213&hyperid=1001&how=delivery_interval')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "titles": {"raw": "offer goes first"}},
                {"entity": "offer", "titles": {"raw": "offer goes second"}},
            ],
            preserve_order=True,
        )

    @classmethod
    def prepare_order_before_on_weekend(cls):
        """
        Создаем магазины, конфигурация календарей которых
        соответствует пятнице, субботе, воскресенью и понедельнику
        в регионах 213 и 75
        Создаем офферы в этих магазинах с опцией доставки в тот же
        день при заказе до 6 часов
        """
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

        cls.index.offers += [
            Offer(
                fesh=111,
                hyperid=211,
                title='friday_offer_moscow',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=112,
                hyperid=212,
                title='saturday_offer_moscow',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=113,
                hyperid=213,
                title='sunday_offer_moscow',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=114,
                hyperid=214,
                title='monday_offer_moscow',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=115,
                hyperid=211,
                title='friday_offer_vladivostok',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=116,
                hyperid=212,
                title='saturday_offer_vladivostok',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=117,
                hyperid=213,
                title='sunday_offer_vladivostok',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
            Offer(
                fesh=118,
                hyperid=214,
                title='monday_offer_vladivostok',
                delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=6)],
            ),
        ]

    def test_order_before_on_weekend(self):
        """
        Что тестируем: при запросе в выходной день службы доставки магазина
        параметр order-before игнорируется и убирается из выдачи

        Задаем ряд запросов с пятницы по воскресенье и проверяем, что
        сроки доставки отображают доставку в понедельник при заказе
        после 6 часов пятницы
        """

        # Запрос в пятницу до order_before (Москва). Ожидаем, что доставка сегодня
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&rids=213&hyperid=211'.format(place))
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
            response = self.report.request_json('place={}&rids=75&hyperid=211'.format(place))
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
            response = self.report.request_json('place={}&rids=213&hyperid=212'.format(place))
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
            response = self.report.request_json('place={}&rids=75&hyperid=212'.format(place))
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
            response = self.report.request_json('place={}&rids=213&hyperid=213'.format(place))
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
            response = self.report.request_json('place={}&rids=75&hyperid=213'.format(place))
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
            response = self.report.request_json('place={}&rids=213&hyperid=214'.format(place))
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


if __name__ == '__main__':
    main()  # каждую группу тестов можно запускать независимо по файлу
