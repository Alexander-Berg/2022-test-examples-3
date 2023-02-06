#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryOption,
    ExchangeRate,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
    OfferDimensions,
)
from core.types.delivery import (
    BlueDeliveryTariff,
)
from core.testcase import TestCase, main
from core.matcher import NoKey

TOTAL_OFFERS_COUNT = 8
TOTAL_OFFERS_COUNT_FOR_PP6 = min(TOTAL_OFFERS_COUNT, 6)
THRESHOLDED_OFFERS_COUNT_FOR_PP6 = 3

OFFER_WITH_FREE_DELIVERY_IN_FEED = Offer.generate_waremd5('free_delivery_feed')


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=700000, regions=[213], priority_region=213),
            Shop(
                fesh=700001,
                regions=[213],
                priority_region=213,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=7000, region=213),
            Outlet(point_id=7001, region=213),
            Outlet(point_id=7002, region=213),
            Outlet(point_id=7003, region=213),
            Outlet(point_id=7004, region=213),
            Outlet(point_id=7005, region=213),
            Outlet(point_id=7006, region=75),
            Outlet(point_id=7007, region=213),
            Outlet(point_id=7008, region=213),
            Outlet(point_id=7009, region=213),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=70001,
                fesh=700000,
                options=[
                    PickupOption(outlet_id=7000, day_from=2, day_to=3, price=500),
                    PickupOption(outlet_id=7001, day_from=1, day_to=4, price=400),
                    PickupOption(outlet_id=7002, day_from=2, day_to=2, price=400),
                    PickupOption(outlet_id=7006, day_from=1, day_to=1, price=100),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=70002,
                fesh=700000,
                options=[
                    PickupOption(outlet_id=7003, day_from=2, day_to=3, price=600),
                    PickupOption(outlet_id=7004, day_from=1, day_to=4, price=200),
                    PickupOption(outlet_id=7005, day_from=2, day_to=2, price=200),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=70003,
                fesh=700000,
                options=[
                    PickupOption(outlet_id=7007, day_from=2, day_to=3, price=0),
                    PickupOption(outlet_id=7008, day_from=1, day_to=4, price=200),
                    PickupOption(outlet_id=7009, day_from=2, day_to=2, price=100),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=80001,
                fesh=700001,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=80002,
                fesh=700001,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.offers += [
            Offer(fesh=700000, price=1000, title='offer with default pickup price 200', pickup_buckets=[70001, 70002]),
            Offer(fesh=700000, price=1000, title='offer with default pickup price 400', pickup_buckets=[70001]),
            Offer(
                fesh=700000,
                price=1000,
                title='offer with default local pickup price 500',
                pickup_option=DeliveryOption(price=500, day_from=1, day_to=7, order_before=10),
                pickup_buckets=[70001, 70003],
            ),
            Offer(fesh=700000, price=1000, title='offer without pickup'),
            Offer(
                fesh=700001,
                price=1000,
                title='offer with free pickup only',
                pickup_buckets=[70003],
                delivery_buckets=[80002],
            ),
            Offer(
                fesh=700001,
                price=1000,
                title='offer without free pickup and delivery',
                pickup_buckets=[70001],
                delivery_buckets=[80002],
            ),
            Offer(fesh=700001, price=1000, title='offer with free delivery only', delivery_buckets=[80001, 80002]),
            Offer(
                fesh=700001,
                price=1000,
                title='offer with free delivery in feed',
                waremd5=OFFER_WITH_FREE_DELIVERY_IN_FEED,
                blue_weight=50,
                weight=50,
                dimensions=OfferDimensions(length=10, width=20, height=30),
                cpa=Offer.CPA_REAL,
                delivery_options=[DeliveryOption(price=0, day_from=1, day_to=1)],
                delivery_buckets=[8888],
            ),
        ]

        cls.index.currencies = [
            Currency(
                name=Currency.BYN,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=0.5),
                ],
            ),
        ]

    def test_pickup_included(self):
        '''
        Проверяем, что на десктопе при фильтре included-in-price=pickup
        минимальная стоимость самовывоза прибавляется к цене
        '''
        hide_off_flags = '&rearr-factors=market_hide_included_in_price_filter=0'
        pickupIncluded = 'included-in-price=pickup'
        response = self.report.request_json(
            'place=prime&fesh=700000&rids=213&platform=desktop&{}'.format(pickupIncluded) + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 200"},
                    "prices": {
                        "value": "1200",
                        "isPickupIncluded": True,
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 400"},
                    "prices": {
                        "value": "1400",
                        "isPickupIncluded": True,
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer without pickup"},
                    "prices": {"value": "1000", "isPickupIncluded": True},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'offer with default local pickup price 500'},
                    "prices": {"value": "1500", "isPickupIncluded": True},
                },
            ],
            allow_different_len=False,
        )

        self.assertFragmentNotIn(response, {"id": "cost-of-delivery"})

        self.assertFragmentIn(
            response,
            {
                "id": "included-in-price",
                "values": [
                    {"value": "pickup", "found": 1, "checked": True},
                    {"value": "delivery", "found": 1, "checked": NoKey("checked")},
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&fesh=700000&rids=75&platform=desktop&{}'.format(pickupIncluded) + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 200"},
                    "prices": {
                        "value": "1100",
                        "isPickupIncluded": True,
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 400"},
                    "prices": {
                        "value": "1100",
                        "isPickupIncluded": True,
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'offer with default local pickup price 500'},
                    "prices": {"value": "1100", "isPickupIncluded": True},
                },
            ],
        )

        '''
        Проверяем, что при другой валюте пользователя стоимость самовывоза также конвертируется
        '''

        response = self.report.request_json(
            'place=prime&fesh=700000&rids=75&platform=desktop&{}&currency=BYN'.format(pickupIncluded) + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 200"},
                    "prices": {"value": "2200", "isPickupIncluded": True, "currency": "BYN"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 400"},
                    "prices": {"value": "2200", "isPickupIncluded": True, "currency": "BYN"},
                },
            ],
        )

        '''
        Проверяем, что без параметра самовывоз не прибавляется
        '''
        response = self.report.request_json('place=prime&fesh=700000&rids=213&platform=desktop' + hide_off_flags)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 200"},
                    "prices": {"value": "1000", "isPickupIncluded": False},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 400"},
                    "prices": {"value": "1000", "isPickupIncluded": False},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer without pickup"},
                    "prices": {"value": "1000", "isPickupIncluded": False},
                },
            ],
        )

        self.assertFragmentNotIn(
            response,
            {
                "id": "cost-of-delivery",
            },
        )

        self.assertFragmentIn(
            response,
            {
                "id": "included-in-price",
                "values": [
                    {"value": "pickup", "found": 1, "checked": NoKey("checked")},
                    {"value": "delivery", "found": 1, "checked": NoKey("checked")},
                ],
            },
            allow_different_len=False,
        )

        '''
        Проверяем, что не на десктопе, но с параметром самовывоз не прибавляется
        '''

        response = self.report.request_json(
            'place=prime&fesh=700000&rids=213&{}'.format(pickupIncluded) + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 200"},
                    "prices": {"value": "1000", "isPickupIncluded": False},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with default pickup price 400"},
                    "prices": {"value": "1000", "isPickupIncluded": False},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "offer without pickup"},
                    "prices": {"value": "1000", "isPickupIncluded": False},
                },
            ],
        )

        '''
        Проверяем, что нет фильтра included-in-price и cost-of-delivery
        '''
        self.assertFragmentNotIn(response, {"id": "cost-of-delivery"})

        self.assertFragmentNotIn(response, {"id": "included-in-price"})

    def test_free_filter_in_local_region(self):
        """
        Проверяем, что оффер, у которого в бакетах бесплатно, а в фиде - нет,
        в локальном регионе фильтруется по "бесплатная доставка"
        """
        response = self.report.request_json('place=prime&fesh=700000&rids=213&platform=desktop&cost-of-delivery=free')
        self.assertFragmentIn(response, {"results": []}, allow_different_len=False)

    @classmethod
    def prepare_free_filter_unified_tariffs(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=8888,
                dc_bucket_id=8888,
                fesh=700001,
                regional_options=[
                    RegionalDelivery(
                        rid=2,
                        options=[DeliveryOption(price=0, day_from=0, day_to=2, shop_delivery_price=0)],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
        ]
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(
                    user_price=549,
                    weight_threshold=30,
                    dsbs_payment=550,
                )
            ],
            regions=[213],
        )

        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(
                    user_price=100,
                ),
            ],
        )

    def test_free_filter_unified_tariffs(self):
        """
        Проверяем, что фильтр по бесплатной доставке корректно работает с едиными тарифами
        У оффера в опции из фида (в локальный 213 регион) и в бакете (в регион 2) бесплатная доставка,
        но по тарифам - платная - он не должен попадать в фильтр при включенных тарифах
        """
        for region in (2, 213):
            response = self.report.request_json(
                'place=prime&offerid={}&rids={}&cost-of-delivery=free&platform=desktop&debug=1'.format(
                    OFFER_WITH_FREE_DELIVERY_IN_FEED, region
                )
            )
            self.assertFragmentIn(response, {"results": []}, allow_different_len=False)
            self.assertFragmentIn(
                response,
                {
                    "filters": {"FILTER_FREE_DELIVERY_OR_PICKUP": 1},
                },
                allow_different_len=False,
            )

    def test_free_pickup_only(self):
        '''
        Проверяем, что при фильтре cost-of-delivery=free фильтруются оффера без бесплатного самовывоза и без бесплатной доставки
        '''
        freeDeliveryOrPickup = 'cost-of-delivery=free'
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'

        # Без фильтра - есть все оффера
        response = self.report.request_json('place=prime&fesh=700001&rids=213&platform=desktop' + unified_off_flags)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with free pickup only"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'offer without free pickup and delivery'},
                },
                {"entity": "offer", "titles": {"raw": 'offer with free delivery only'}},
                {"entity": "offer", "titles": {"raw": 'offer with free delivery in feed'}},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "name": "Стоимость доставки",
                "values": [
                    {"found": 3, "value": "Бесплатно", "id": "free"},
                ],
            },
            allow_different_len=False,
        )

        # С фильтром
        response = self.report.request_json(
            'place=prime&fesh=700001&rids=213&platform=desktop&{}'.format(freeDeliveryOrPickup) + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with free pickup only"},
                },
                {"entity": "offer", "titles": {"raw": 'offer with free delivery only'}},
                {"entity": "offer", "titles": {"raw": 'offer with free delivery in feed'}},
            ],
            allow_different_len=False,
        )

        # На не десктопе ничего не фильтруется
        response = self.report.request_json(
            'place=prime&fesh=700001&rids=213&{}'.format(freeDeliveryOrPickup) + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "offer with free pickup only"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'offer without free pickup and delivery'},
                },
                {"entity": "offer", "titles": {"raw": 'offer with free delivery only'}},
                {"entity": "offer", "titles": {"raw": 'offer with free delivery in feed'}},
            ],
            allow_different_len=False,
        )

    @classmethod
    def prepare_filters_by_split(cls):
        cls.index.shops += [
            Shop(fesh=800),
            Shop(fesh=801),
            Shop(fesh=802),
        ]

        cls.index.outlets += [
            Outlet(point_id=8000, region=213),
            Outlet(point_id=8001, region=213),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=801,
                fesh=800,
                options=[PickupOption(outlet_id=8000, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=802,
                fesh=800,
                options=[PickupOption(outlet_id=8000, day_from=1, day_to=2, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=803,
                fesh=801,
                options=[PickupOption(outlet_id=8001, day_from=1, day_to=2, price=0)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=804,
                fesh=801,
                options=[PickupOption(outlet_id=8001, day_from=1, day_to=2, price=200)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=801,
                fesh=800,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=200, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=802,
                fesh=800,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=803,
                fesh=801,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=804,
                fesh=801,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=300, day_from=2, day_to=3)])],
            ),
        ]

        cls.index.models += [Model(hyperid=100)]

        cls.index.offers += [
            Offer(fesh=800, price=1000, title='100 pickup, no delivery', pickup_buckets=[801]),
            Offer(fesh=800, price=1000, title='100 pickup, 200 delivery', pickup_buckets=[801], delivery_buckets=[801]),
            Offer(fesh=800, price=1000, title='100 pickup, 0 delivery', pickup_buckets=[801], delivery_buckets=[802]),
            Offer(
                fesh=800,
                price=1000,
                title='0 pickup, 200 delivery',
                pickup_buckets=[802],
                delivery_buckets=[801],
                hyperid=100,
            ),
            Offer(fesh=800, price=1000, title='0 pickup, 0 delivery', pickup_buckets=[802], delivery_buckets=[802]),
            Offer(fesh=800, price=1000, title='0 pickup, no delivery', pickup_buckets=[802]),
            Offer(fesh=800, price=1000, title='no pickup, 200 delivery', delivery_buckets=[801]),
            Offer(fesh=800, price=1000, title='no pickup, 0 delivery', delivery_buckets=[802]),
            Offer(fesh=800, price=1000, title='no pickup, no delivery'),
            Offer(
                fesh=801,
                price=1000,
                title='free pickup, not free delivery',
                pickup_buckets=[803],
                delivery_buckets=[804],
            ),
            Offer(
                fesh=802,
                price=1000,
                title='not free pickup, free delivery',
                pickup_buckets=[804],
                delivery_buckets=[803],
            ),
        ]

    def test_first_split_filters(self):
        rearr = 'platform=desktop&rearr-factors=market_pickup_included_first_split=1;market_dsbs_tariffs=0;market_unified_tariffs=0'

        response = self.report.request_json('place=prime&fesh=800&rids=213&{}'.format(rearr))
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, no delivery"}},
                {"titles": {"raw": "100 pickup, 200 delivery"}},
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
                {"titles": {"raw": "no pickup, 200 delivery"}},
                {"titles": {"raw": "no pickup, 0 delivery"}},
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {
                        "found": 5,
                        "value": "Бесплатно",
                        "id": "free",
                    },
                    {
                        "found": 0,
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                    },
                ],
            },
        )

        # Проверяем, что показывается галочка про включение стоимости доставки в цену при фильтре по наличию доставки/самовывоза
        response = self.report.request_json('place=prime&fesh=800&rids=213&{}&offer-shipping=delivery'.format(rearr))
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, 200 delivery"}},
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "no pickup, 200 delivery"}},
                {"titles": {"raw": "no pickup, 0 delivery"}},
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {"initialFound": 3, "found": 3, "value": "Бесплатно", "id": "free", "checked": NoKey("checked")},
                    {
                        "found": 1,
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                        "checked": NoKey("checked"),
                    },
                ],
            },
        )

        # Проверяем наличие галочки при фильтрации по наличию самовывоза
        response = self.report.request_json('place=prime&fesh=800&rids=213&{}&offer-shipping=pickup'.format(rearr))
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, no delivery"}},
                {"titles": {"raw": "100 pickup, 200 delivery"}},
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {"initialFound": 3, "found": 3, "value": "Бесплатно", "id": "free", "checked": NoKey("free")},
                    {
                        "found": 1,
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                        "checked": NoKey("checked"),
                    },
                ],
            },
        )

        # Проверяем, что с cost-of-delivery=included, но без фильтра по способу доставки (offer-shipping), фильтра included не чекнут
        response = self.report.request_json('place=prime&fesh=800&rids=213&{}&cost-of-delivery=included'.format(rearr))
        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {"initialFound": 5, "found": 5, "value": "Бесплатно", "id": "free", "checked": NoKey("free")},
                    {
                        "found": 0,
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                        "checked": NoKey("checked"),
                    },
                ],
            },
        )

        # стоимость самовывоза/доставки не прибавляется:
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "100 pickup, 200 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": False},
                },
            ],
        )

        # Проверяем, что прибавилась стоимость доставки
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&{}&offer-shipping=delivery&cost-of-delivery=included'.format(rearr)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "100 pickup, 200 delivery"},
                    "prices": {"value": "1200", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "100 pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "0 pickup, 200 delivery"},
                    "prices": {"value": "1200", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "0 pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "no pickup, 200 delivery"},
                    "prices": {"value": "1200", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "no pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {"initialFound": 3, "found": 3, "value": "Бесплатно", "id": "free", "checked": NoKey("checked")},
                    {
                        "found": 1,
                        "value": "Показывать цену с учётом доставки",
                        "checked": True,
                        "id": "included",
                    },
                ],
            },
        )

        # Проверяем, что прибавилась стоимость самовывоза
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&{}&offer-shipping=pickup&cost-of-delivery=included'.format(rearr)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "100 pickup, 200 delivery"},
                    "prices": {"value": "1100", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "100 pickup, 0 delivery"},
                    "prices": {"value": "1100", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "0 pickup, 200 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "0 pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "100 pickup, no delivery"},
                    "prices": {"value": "1100", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "0 pickup, no delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {"initialFound": 3, "found": 3, "value": "Бесплатно", "id": "free", "checked": NoKey("checked")},
                    {
                        "found": 1,
                        "value": "Показывать цену с учётом доставки",
                        "checked": True,
                        "id": "included",
                    },
                ],
            },
        )

        # Проверяем статистики для фильтра по бесплатной доставке/самовывозу
        response = self.report.request_json('place=prime&fesh=801&rids=213&{}'.format(rearr))

        self.assertFragmentIn(
            response, {"id": "cost-of-delivery", "values": [{"found": 1, "id": "free"}, {"id": "included", "found": 0}]}
        )

        response = self.report.request_json('place=prime&fesh=801&rids=213&{}&offer-shipping=pickup'.format(rearr))

        self.assertFragmentIn(
            response, {"id": "cost-of-delivery", "values": [{"found": 1, "id": "free"}, {"id": "included", "found": 1}]}
        )

        response = self.report.request_json('place=prime&fesh=802&rids=213&{}'.format(rearr))
        self.assertFragmentIn(
            response, {"id": "cost-of-delivery", "values": [{"found": 1, "id": "free"}, {"id": "included", "found": 0}]}
        )

        response = self.report.request_json('place=prime&fesh=802&rids=213&{}&offer-shipping=delivery'.format(rearr))
        self.assertFragmentIn(
            response, {"id": "cost-of-delivery", "values": [{"found": 1, "id": "free"}, {"id": "included", "found": 1}]}
        )

    def test_first_split_filters_free_delivery(self):
        rearr = 'platform=desktop&rearr-factors=market_pickup_included_first_split=1;market_dsbs_tariffs=0;market_unified_tariffs=0'

        # Проверяем фильтрацию по бесплатной доставке или самовывозу
        response = self.report.request_json('place=prime&fesh=800&rids=213&{}&cost-of-delivery=free'.format(rearr))

        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
                {"titles": {"raw": "no pickup, 0 delivery"}},
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {
                        "found": 5,
                        "value": "Бесплатно",
                        "checked": True,
                        "id": "free",
                    },
                    {
                        "found": 0,  # == это пункт выделен серым, так как не отфильтровано по способу доставки
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                    },
                ],
            },
        )

        # Проверяем фильтрацию по бесплатной доставке
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&{}&cost-of-delivery=free&offer-shipping=delivery'.format(rearr)
        )

        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "no pickup, 0 delivery"}},
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {
                        "found": 3,
                        "value": "Бесплатно",
                        "checked": True,
                        "id": "free",
                    },
                    {
                        "found": 1,
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                    },
                ],
            },
        )

        # Проверяем фильтрацию по бесплатному самовывозу
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&{}&cost-of-delivery=free&offer-shipping=pickup'.format(rearr)
        )

        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {
                        "found": 3,
                        "value": "Бесплатно",
                        "checked": True,
                        "id": "free",
                    },
                    {
                        "found": 1,
                        "value": "Показывать цену с учётом доставки",
                        "id": "included",
                    },
                ],
            },
        )

    def test_second_split_filters(self):
        hide_off_flags = '&rearr-factors=market_hide_included_in_price_filter=0'
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&platform=desktop' + unified_off_flags + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, no delivery"}},
                {"titles": {"raw": "100 pickup, 200 delivery"}},
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
                {"titles": {"raw": "no pickup, 200 delivery"}},
                {"titles": {"raw": "no pickup, 0 delivery"}},
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "type": "enum",
                "values": [
                    {
                        "found": 5,
                        "value": "Бесплатно",
                        "id": "free",
                    },
                ],
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "included-in-price",
                "type": "boolean",
                "hasBoolNo": True,
                "values": [{"value": "delivery", "found": 1}, {"value": "pickup", "found": 1}],
            },
        )

        # Проверяем, что есть фильтр cost-of-delivery на КМ
        response = self.report.request_json(
            'place=productoffers&hyperid=100&rids=213&platform=desktop' + unified_off_flags
        )
        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
            },
        )

        # Проверяем, что стоимость доставки прибавилась к цене
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&platform=desktop&included-in-price=delivery'
            + unified_off_flags
            + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "100 pickup, 200 delivery"},
                    "prices": {"value": "1200", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "100 pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "0 pickup, 200 delivery"},
                    "prices": {"value": "1200", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "0 pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "no pickup, 200 delivery"},
                    "prices": {"value": "1200", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "no pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "100 pickup, no delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {
                    "titles": {"raw": "0 pickup, no delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": True, "isPickupIncluded": False},
                },
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        # Проверяем, что прибавилась стоимость самовывоза
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&platform=desktop&included-in-price=pickup'
            + unified_off_flags
            + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "100 pickup, 200 delivery"},
                    "prices": {"value": "1100", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "100 pickup, 0 delivery"},
                    "prices": {"value": "1100", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "0 pickup, 200 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "0 pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "100 pickup, no delivery"},
                    "prices": {"value": "1100", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "0 pickup, no delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "no pickup, 200 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {
                    "titles": {"raw": "no pickup, 0 delivery"},
                    "prices": {"value": "1000", "isDeliveryIncluded": False, "isPickupIncluded": True},
                },
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        # Проверяем фильтрацию по бесплатному самовывозу
        response = self.report.request_json(
            'place=prime&fesh=800&rids=213&platform=desktop&cost-of-delivery=free&offer-shipping=pickup'
            + unified_off_flags
        )

        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "id": "cost-of-delivery",
                "values": [
                    {
                        "found": 3,
                        "value": "Бесплатно",
                        "checked": True,
                        "id": "free",
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_filters_not_in_desktop(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&fesh=800&rids=213&{}' + unified_off_flags)
        self.assertFragmentNotIn(
            response,
            {
                "id": "cost-of-delivery",
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "id": "included-in-price",
            },
        )

        self.assertFragmentIn(response, {"id": "free-delivery"})

    def test_hiding_of_included_in_price_filters(self):
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        response = self.report.request_json('place=prime&fesh=800&rids=213&platform=desktop' + unified_off_flags)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "100 pickup, no delivery"}},
                {"titles": {"raw": "100 pickup, 200 delivery"}},
                {"titles": {"raw": "100 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, 200 delivery"}},
                {"titles": {"raw": "0 pickup, 0 delivery"}},
                {"titles": {"raw": "0 pickup, no delivery"}},
                {"titles": {"raw": "no pickup, 200 delivery"}},
                {"titles": {"raw": "no pickup, 0 delivery"}},
                {"entity": "regionalDelimiter"},
            ],
            allow_different_len=False,
        )

        self.assertFragmentNotIn(response, {"id": "included-in-price"})

    @classmethod
    def prepare_relevance(cls):
        cls.index.shops += [Shop(fesh=900), Shop(fesh=901, priority_region=213)]

        cls.index.outlets += [
            Outlet(point_id=9000, region=213),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=901,
                fesh=900,
                options=[PickupOption(outlet_id=9000, day_from=1, day_to=2, price=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=901,
                fesh=900,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=200, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(bucket_id=902, fesh=900, regional_options=[RegionalDelivery(rid=213, unknown=True)]),
            DeliveryBucket(bucket_id=903, fesh=901, regional_options=[RegionalDelivery(rid=213, unknown=True)]),
        ]

        cls.index.offers += [
            Offer(
                fesh=900,
                price=1000,
                title='with pickup and delivery price',
                pickup_buckets=[901],
                delivery_buckets=[901],
            ),
            Offer(fesh=900, price=900, title='with pickup price only', pickup_buckets=[901]),
            Offer(fesh=900, price=800, title='with delivery price only', delivery_buckets=[901]),
            Offer(fesh=900, price=700, title='with delivery unknown only', delivery_buckets=[902]),
            Offer(fesh=901, price=1000, title='with delivery from local region'),
        ]

    def test_relevance(self):
        '''
        Проверяем, что при включеннии стоимости доставки/самовывоза в цену,
        пессимизируются оффера без доставки/самовывоза
        Порядок офферов:
        - Из локального региона пользователя, с ценой доставки
        - Из локального региона пользователя, без цены доставки
        - Из нелокального региона пользователя, с ценой доставки
        - Из нелокального региона пользователя, без цены доставки
        '''

        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        hide_off_flags = '&rearr-factors=market_hide_included_in_price_filter=0'
        response = self.report.request_json(
            'place=prime&fesh=900&rids=213&platform=desktop&included-in-price=delivery&how=aprice&debug=da'
            + unified_off_flags
            + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "with pickup and delivery price"},
                    "prices": {"value": "1200"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "3"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "1"},
                        ]
                    },
                },
                {
                    "titles": {"raw": "with pickup price only"},
                    "prices": {"value": "900"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "3"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "0"},
                        ]
                    },
                },
                {
                    "entity": "regionalDelimiter",
                },
                {
                    "titles": {"raw": "with delivery price only"},
                    "prices": {"value": "1000"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "2"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "1"},
                        ]
                    },
                },
                {
                    "titles": {"raw": "with delivery unknown only"},
                    "prices": {"value": "700"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "2"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "0"},
                        ]
                    },
                },
            ],
            preserve_order=True,
        )

        # Проверяем включение самовывоза в цену

        response = self.report.request_json(
            'place=prime&fesh=900&fesh=901&rids=213&platform=desktop&included-in-price=pickup&how=aprice&debug=da'
            + unified_off_flags
            + hide_off_flags
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "with pickup price only"},
                    "prices": {"value": "1000"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "3"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "1"},
                        ]
                    },
                },
                {
                    "titles": {"raw": "with pickup and delivery price"},
                    "prices": {"value": "1100"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "3"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "1"},
                        ]
                    },
                },
                {
                    "titles": {"raw": "with delivery from local region"},
                    "prices": {"value": "1000"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "3"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "0"},
                        ]
                    },
                },
                {
                    "entity": "regionalDelimiter",
                },
                {
                    "titles": {"raw": "with delivery unknown only"},
                    "prices": {"value": "700"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "2"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "0"},
                        ]
                    },
                },
                {
                    "titles": {"raw": "with delivery price only"},
                    "prices": {"value": "800"},
                    "debug": {
                        "rank": [
                            {"name": "DELIVERY_TYPE", "value": "2"},
                            {"name": "INCLUDED_DELIVERY_OR_PICKUP", "value": "0"},
                        ]
                    },
                },
            ],
            preserve_order=True,
        )

        # Проверяем, что нет INCLUDED_DELIVERY_OR_PICKUP без фильтра
        response = self.report.request_json(
            'place=prime&fesh=900&fesh=901&rids=213&platform=desktop&how=aprice&debug=da'
            + unified_off_flags
            + hide_off_flags
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "debug": {"rank": {"name": "INCLUDED_DELIVERY_OR_PICKUP"}},
                },
            ],
        )


if __name__ == '__main__':
    main()
