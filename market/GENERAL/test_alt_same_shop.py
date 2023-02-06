#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BookingAvailability,
    CategoryRestriction,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    GLType,
    GpsCoord,
    HyperCategory,
    Model,
    NewShopRating,
    Offer,
    Outlet,
    OutletDeliveryOption,
    Region,
    RegionalDelivery,
    RegionalRestriction,
    Shop,
    UrlType,
    VCluster,
)
from core.testcase import TestCase, main
from core.matcher import NoKey
from core.types.hypercategory import ADULT_CATEG_ID

from unittest import skip
import copy


class T(TestCase):
    @classmethod
    def prepare_blue_delivery_price(cls):
        '''
        Отключаем цену доставки синих оферов от пользователя, т.к. в этом тесте исторически много завязок на цену в тарифе
        https://st.yandex-team.ru/MARKETOUT-34206
        '''
        cls.settings.blue_delivery_price_enabled = False

    @classmethod
    def prepare(cls):
        # TODO: MARKETOUT-47769 удалить вместе с флагом
        cls.settings.default_search_experiment_flags += ['market_hide_long_delivery_offers=0']
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[111],
                regional_restrictions=[
                    RegionalRestriction(show_offers=False, display_only_matched_offers=False, delivery=False, rids=[0])
                ],
            )
        ]

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
                        ],
                    )
                ],
            )
        ]

        cls.index.outlets += [
            Outlet(fesh=1001, point_id=100, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1001, point_id=200, region=2, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1001, point_id=300, region=216, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1002, point_id=101, region=213, point_type=Outlet.FOR_STORE),
            Outlet(fesh=1004, point_id=405, region=213, point_type=Outlet.FOR_STORE),
            Outlet(
                point_id=102,
                delivery_service_id=103,
                region=213,
                gps_coord=GpsCoord(55.45, 37.7),
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(price=200),
            ),
            Outlet(fesh=1001, point_id=401, region=3, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1002, point_id=402, region=3, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1003, point_id=403, region=3, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1004, point_id=404, region=3, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1008, point_id=808, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(
                fesh=1108,
                point_id=888,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=123),
            ),
            Outlet(
                fesh=1108,
                point_id=809,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=456),
            ),
            Outlet(
                fesh=1108,
                point_id=810,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=789),
            ),
            Outlet(
                fesh=1108,
                point_id=812,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=789),
            ),
            Outlet(
                fesh=1108,
                point_id=811,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(price=1000),
            ),
            Outlet(fesh=1009, point_id=909, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(
                fesh=1010,
                point_id=1808,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=123, price_to=200),
            ),
            Outlet(fesh=1011, point_id=1101, region=157, point_type=Outlet.FOR_STORE),
            Outlet(fesh=1011, point_id=1102, region=157, point_type=Outlet.FOR_PICKUP),
            Outlet(fesh=1012, point_id=1201, region=157, point_type=Outlet.MIXED_TYPE),
            Outlet(fesh=1012, point_id=1202, region=157, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.shops += [
            Shop(
                fesh=1001,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=2.0),
                delivery_service_outlets=[100, 200, 300, 401],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=1002,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=3.0),
                delivery_service_outlets=[402],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=1003,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=4.0),
                delivery_service_outlets=[101, 403],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=1004,
                priority_region=213,
                new_shop_rating=NewShopRating(new_rating_total=5.0),
                delivery_service_outlets=[102, 404, 405],
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=1005, priority_region=157, cpa=Shop.CPA_REAL),
            Shop(fesh=1006, main_fesh=10, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=1007, main_fesh=10, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=1008, priority_region=157, delivery_service_outlets=[808], cpa=Shop.CPA_REAL),
            Shop(fesh=1108, priority_region=157, delivery_service_outlets=[888, 809, 810, 811, 102], cpa=Shop.CPA_REAL),
            Shop(fesh=1009, priority_region=157, delivery_service_outlets=[909], cpa=Shop.CPA_REAL),
            Shop(fesh=1010, priority_region=213, delivery_service_outlets=[1808], cpa=Shop.CPA_REAL),
            Shop(fesh=1011, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=1012, priority_region=213, cpa=Shop.CPA_REAL),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=5555, visual=True),
            HyperCategory(hid=111, name='med'),
            HyperCategory(hid=ADULT_CATEG_ID, children=[HyperCategory(12345)]),
        ]

        cls.index.vclusters += [VCluster(hid=5555, vclusterid=1000000101), VCluster(hid=5555, vclusterid=1000000102)]

        cls.index.models += [
            Model(hid=444, hyperid=7001, group_hyperid=7000),
            Model(hid=444, hyperid=7002, group_hyperid=7000),
            Model(hid=12345, hyperid=92777),
        ]

        cls.index.gltypes += [
            # second kind filters
            GLType(param_id=202, hid=5500, gltype=GLType.ENUM, cluster_filter=True, has_model_filter_index=False),
            GLType(param_id=203, hid=5500, gltype=GLType.BOOL, cluster_filter=True, has_model_filter_index=False),
            GLType(param_id=204, hid=5500, gltype=GLType.NUMERIC, cluster_filter=True, has_model_filter_index=False),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=811, fesh=1003, regional_options=[RegionalDelivery(rid=213, options=[])]),
            DeliveryBucket(
                bucket_id=8111,
                fesh=1001,
                carriers=[12, 99, 9, 11, 7],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=0, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=8121,
                fesh=1002,
                carriers=[12, 99, 9, 11, 7],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=20, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=8122,
                fesh=1002,
                carriers=[12, 99, 9, 11, 7],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=30, day_from=1, day_to=4)])],
            ),
            DeliveryBucket(
                bucket_id=8123,
                fesh=1002,
                carriers=[12, 99, 9, 11, 7],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=40, day_from=2, day_to=3)])],
            ),
            DeliveryBucket(
                bucket_id=8141,
                fesh=1004,
                carriers=[12, 99, 9, 11, 7],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=10, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=8142,
                fesh=1004,
                carriers=[12, 99, 9, 11, 7],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=911,
                fesh=1001,
                carriers=[11],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=19, day_from=0, day_to=3),
                            DeliveryOption(price=16, day_from=4, day_to=4),
                        ],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=922,
                fesh=1001,
                carriers=[22],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=19, day_from=0, day_to=3),
                            DeliveryOption(price=16, day_from=4, day_to=4),
                        ],
                    )
                ],
            ),
        ]

        cls.index.offers += [
            # offer with hyperid=4444* is additional offer to avoid removal of packs with 1 offer
            # fesh 1002
            Offer(cpa=Offer.CPA_REAL, hyperid=8881, fesh=1002, price=110, delivery_buckets=[8121], pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=9991, fesh=1002, price=110, delivery_buckets=[8122], pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=11111, fesh=1002, price=110, delivery_buckets=[8123], pickup=False),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7771,
                fesh=1002,
                price=110,
                delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7772,
                fesh=1002,
                price=110,
                delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
            ),
            Offer(hyperid=55551, fesh=1002, price=1),
            Offer(hyperid=55552, fesh=1002, price=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=44441, fesh=1002, price=1, waremd5='DuE098x_rinQLZn3KKrELw'),
            Offer(cpa=Offer.CPA_REAL, hyperid=3100, fesh=1002, price=5000),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=3100,
                fesh=1002,
                price=3000,
                waremd5='_qQnWXU28-IUghltMZJwNw',
                min_quantity=5,
                step_quantity=3,
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=3100, fesh=1002, price=7000),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1002, price=5),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1002, price=3),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1002, price=7),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1002, price=50),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1002, price=30),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1002, price=70),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41000,
                fesh=1002,
                price=90,
                waremd5='22222222222222gggggggg',
                hid=5500,
                glparams=[GLParam(param_id=202, value=2)],
                delivery_options=[DeliveryOption(price=10, day_from=3, day_to=4)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41000,
                fesh=1002,
                price=110,
                waremd5='09lEaAKkQll1XTgggggggg',
                hid=5500,
                glparams=[GLParam(param_id=202, value=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41000,
                fesh=1001,
                price=120,
                waremd5='11111111111111gggggggg',
                hid=5500,
                glparams=[GLParam(param_id=202, value=2), GLParam(param_id=203, value=0)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=7000, fesh=1002, price=1, waremd5='CdvaDsKX-OH7j6OX79D3Cg'),
            Offer(cpa=Offer.CPA_REAL, hyperid=7001, fesh=1002, price=2),
            Offer(cpa=Offer.CPA_REAL, hyperid=7002, fesh=1002, price=3),
            Offer(cpa=Offer.CPA_REAL, hyperid=92777, fesh=1002),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=92888,
                fesh=1002,
                waremd5='kGEKoOJPY-xtJjfgb5ua8g',
                title="title_1",
                adult=True,
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=92999, fesh=1002, waremd5='otENNVzevIeeT8bsxvY91w', title="title_2"),
            Offer(cpa=Offer.CPA_REAL, hyperid=92555, fesh=1002),
            # fesh 1001
            Offer(cpa=Offer.CPA_REAL, fesh=1001, hid=111, hyperid=111222),
            Offer(cpa=Offer.CPA_REAL, fesh=1001, hid=111, hyperid=111333),
            Offer(
                cpa=Offer.CPA_REAL,
                fesh=1001,
                hyperid=111444,
                waremd5='fzn4MX-9sZiO9MYo66AlkQ',
                delivery_options=[DeliveryOption(price=20, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                fesh=1001,
                hyperid=111555,
                waremd5='RPaDqEFjs1I6_lfC4Ai8jA',
                delivery_options=[DeliveryOption(price=25, day_from=1, day_to=2)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=90444, fesh=1001, adult=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=90555, fesh=1001, adult=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=90666, fesh=1001),
            Offer(cpa=Offer.CPA_REAL, hyperid=91444, fesh=1001, adult=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=91555, fesh=1001, adult=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=91666, fesh=1001),
            Offer(cpa=Offer.CPA_REAL, hyperid=91777, fesh=1001),
            Offer(cpa=Offer.CPA_REAL, hyperid=91888, fesh=1001),
            Offer(cpa=Offer.CPA_REAL, hyperid=80111, fesh=1001, has_delivery_options=True, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=80222, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=80333, fesh=1001, has_delivery_options=False, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=80444, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, price=50, hyperid=980111, fesh=1001, delivery_buckets=[811], pickup=True),
            Offer(cpa=Offer.CPA_REAL, price=50, hyperid=980222, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, price=50, hyperid=980333, fesh=1001, has_delivery_options=False, pickup=False),
            Offer(cpa=Offer.CPA_REAL, price=50, hyperid=980444, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=81111, fesh=1001, has_delivery_options=True, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=81222, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=81333, fesh=1001, has_delivery_options=False, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=81444, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=181111, fesh=1001, has_delivery_options=True, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=181222, fesh=1001, has_delivery_options=True, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=181333, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=181444, fesh=1001, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=228881, fesh=1001, price=110, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=229991, fesh=1001, price=110, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=2211111, fesh=1001, price=110, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=338881, fesh=1001, price=110, has_delivery_options=True, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=339991, fesh=1001, price=110, has_delivery_options=True, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=3311111, fesh=1001, price=110, has_delivery_options=True, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=8881, fesh=1001, price=110, delivery_buckets=[8111]),
            Offer(cpa=Offer.CPA_REAL, hyperid=9991, fesh=1001, price=110, delivery_buckets=[8111]),
            Offer(cpa=Offer.CPA_REAL, hyperid=11111, fesh=1001, price=110, delivery_buckets=[8111]),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7771,
                fesh=1001,
                price=100,
                delivery_options=[DeliveryOption(price=50, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7772,
                fesh=1001,
                price=100,
                delivery_options=[DeliveryOption(price=50, day_from=1, day_to=2)],
            ),
            Offer(hyperid=55551, fesh=1001, price=1),
            Offer(hyperid=55552, fesh=1001, price=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=44441, fesh=1001, price=1, waremd5='1AxLbIEFo2u-f0ayN1RG5g'),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=44442,
                fesh=1001,
                price=1,
                delivery_options=[DeliveryOption(price=10, day_from=0, day_to=0)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=3100, fesh=1001, price=5002),
            Offer(cpa=Offer.CPA_REAL, hyperid=3100, fesh=1001, price=3002, waremd5='FSqiKO1icV4qzU-I7w8qLg'),
            Offer(cpa=Offer.CPA_REAL, hyperid=3100, fesh=1001, price=7002),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1001, price=7),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1001, price=5),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1001, price=9),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1001, price=52),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1001, price=32),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1001, price=72),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41000,
                fesh=1001,
                price=100,
                waremd5='09lEaAKkQll1XTaaaaaaaQ',
                hid=5500,
                glparams=[
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=203, value=0),
                    GLParam(param_id=204, value=500),
                ],
                delivery_options=[DeliveryOption(price=10, day_from=0, day_to=0)],
            ),
            Offer(cpa=Offer.CPA_REAL, fesh=1001, price=150, waremd5='hhhhhhhhhhpppppppppppg'),
            Offer(cpa=Offer.CPA_REAL, fesh=1001, price=200, waremd5='nnnnnnnnnnpppppppppppg'),
            Offer(cpa=Offer.CPA_REAL, hyperid=7000, fesh=1001, price=3),
            Offer(cpa=Offer.CPA_REAL, hyperid=7001, fesh=1001, price=2),
            Offer(cpa=Offer.CPA_REAL, hyperid=7002, fesh=1001, price=1, waremd5='gwgfZ3JflzQelx9tFVgDqQ'),
            # for delivery options
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41111,
                fesh=1001,
                price=100,
                delivery_options=[
                    DeliveryOption(price=10, day_from=0, day_to=3),
                    DeliveryOption(price=5, day_from=5, day_to=5),
                ],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41222,
                fesh=1001,
                price=100,
                delivery_options=[
                    DeliveryOption(price=27, day_from=0, day_to=0),
                    DeliveryOption(price=25, day_from=1, day_to=1),
                    DeliveryOption(price=20, day_from=0, day_to=2),
                    DeliveryOption(price=15, day_from=5, day_to=5),
                    DeliveryOption(
                        price=14, day_from=32, day_to=32
                    ),  # TODO: MARKETOUT-47769 вернуть как было. Удалить значения
                ],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=41333, fesh=1001, price=100, delivery_buckets=[911, 922]),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41555,
                fesh=1001,
                price=100,
                delivery_options=[
                    DeliveryOption(price=26, day_from=0, day_to=0),
                    DeliveryOption(price=25, day_from=0, day_to=1),
                    DeliveryOption(price=20, day_from=0, day_to=2, order_before=11),
                    DeliveryOption(price=15, day_from=5, day_to=5),
                    DeliveryOption(
                        price=14, day_from=32, day_to=32
                    ),  # TODO: MARKETOUT-47769 вернуть как было. Удалить значения
                ],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=41444, fesh=1001, price=100, delivery_buckets=[911]),
            # fesh 1003
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=73111,
                waremd5='lOlF9TzZLwWcibYeKksTIw',
                price=25,
                fesh=1003,
                delivery_options=[DeliveryOption(price=1, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=73222,
                waremd5='qaerG3FLBO_yXOXngxNZAg',
                title="offer-73222",
                price=10,
                fesh=1003,
                delivery_options=[DeliveryOption(price=2, day_from=1, day_to=2)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=90444, fesh=1003, adult=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=90555, fesh=1003),
            Offer(cpa=Offer.CPA_REAL, hyperid=90666, fesh=1003),
            Offer(cpa=Offer.CPA_REAL, hyperid=93555, fesh=1003),
            Offer(cpa=Offer.CPA_REAL, hyperid=93666, fesh=1003),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=103111,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                price=10.25,
                delivery_options=[DeliveryOption(price=1.6, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=103222,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                price=10.5,
                delivery_options=[DeliveryOption(price=1.7, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=103333,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                price=10.6,
                delivery_options=[DeliveryOption(price=1.8, day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80111,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80222,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80333,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=1)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=80444, fesh=1003, has_delivery_options=False, pickup=True),
            Offer(
                cpa=Offer.CPA_REAL,
                price=30,
                hyperid=980111,
                fesh=1003,
                delivery_buckets=[811],
                waremd5='dh78hD_wWrdO01hxPdUcGw',
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                price=100,
                hyperid=980111,
                fesh=1003,
                waremd5='iAT5Tha6W2gOI5pVSgj30g',
                delivery_options=[DeliveryOption(price=1, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                price=100,
                hyperid=980222,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=1, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                price=100,
                hyperid=980333,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=1, day_from=0, day_to=1)],
            ),
            Offer(cpa=Offer.CPA_REAL, price=100, hyperid=980444, fesh=1003, has_delivery_options=False, pickup=True),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=83111,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=83222,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=5)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=83333,
                fesh=1003,
                has_delivery_options=True,
                pickup=False,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=1)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=83444, fesh=1003, has_delivery_options=False, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=8881, fesh=1003, price=110, has_delivery_options=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=9991, fesh=1003, price=110, has_delivery_options=False, pickup=False),
            Offer(cpa=Offer.CPA_REAL, hyperid=11111, fesh=1003, price=110, has_delivery_options=False),
            Offer(hyperid=55551, fesh=1003, price=1),
            Offer(hyperid=55552, fesh=1003, price=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=44441, fesh=1003, price=1, waremd5='8zT9D0cAuDwEtf-xBL280A'),
            Offer(cpa=Offer.CPA_REAL, hyperid=3400, fesh=1003, price=5002),
            Offer(cpa=Offer.CPA_REAL, hyperid=3400, fesh=1003, price=3002),
            Offer(cpa=Offer.CPA_REAL, hyperid=3400, fesh=1003, price=7002),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1003, price=7),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1003, price=5, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=3200, fesh=1003, price=9),
            Offer(cpa=Offer.CPA_REAL, vclusterid=1000000101, fesh=1003, price=500),
            Offer(cpa=Offer.CPA_REAL, vclusterid=1000000101, fesh=1003, price=300, waremd5='fDbQKU6BwzM0vDugM73auA'),
            Offer(cpa=Offer.CPA_REAL, vclusterid=1000000101, fesh=1003, price=700),
            # for delivery options
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41111,
                fesh=1003,
                price=100,
                delivery_options=[DeliveryOption(price=10, day_from=1, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41222,
                fesh=1003,
                price=100,
                delivery_options=[
                    DeliveryOption(price=27, day_from=0, day_to=0),
                    DeliveryOption(price=25, day_from=0, day_to=1),
                ],
            ),
            # fesh 1004
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=74111,
                waremd5='Z3l_Hw8C7FzNXjq9LU5bgQ',
                price=20,
                fesh=1004,
                delivery_options=[DeliveryOption(price=1, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=74222,
                waremd5='nx1WWdWID7Qn9uBK5QD8JQ',
                price=10,
                fesh=1004,
                delivery_options=[DeliveryOption(price=2, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=94111,
                fesh=1004,
                price=5,
                post_term_delivery=True,
                download=True,
                store=True,
                delivery_options=[DeliveryOption(price=0, day_from=1, day_to=2)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=94222,
                fesh=1004,
                price=5,
                post_term_delivery=True,
                download=True,
                store=True,
                delivery_options=[DeliveryOption(price=0, day_from=10, day_to=20)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=94333,
                fesh=1004,
                price=5,
                post_term_delivery=False,
                download=False,
                store=False,
                delivery_options=[DeliveryOption(price=10, day_from=1, day_to=2)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=94444, fesh=1004, price=5, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=94555, fesh=1004, price=5, pickup=True),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=944111,
                fesh=1004,
                price=5,
                post_term_delivery=True,
                download=True,
                store=True,
                delivery_buckets=[8141],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=944222,
                fesh=1004,
                price=5,
                post_term_delivery=True,
                download=True,
                store=True,
                delivery_buckets=[8142],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=944333,
                fesh=1004,
                price=5,
                post_term_delivery=False,
                download=False,
                store=False,
                delivery_buckets=[8142],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80111,
                fesh=1004,
                has_delivery_options=True,
                pickup=True,
                post_term_delivery=True,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80222,
                fesh=1004,
                has_delivery_options=True,
                pickup=True,
                post_term_delivery=True,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80333,
                fesh=1004,
                has_delivery_options=True,
                pickup=False,
                post_term_delivery=False,
                delivery_options=[DeliveryOption(day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=80444,
                fesh=1004,
                has_delivery_options=False,
                pickup=True,
                post_term_delivery=True,
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=84111,
                fesh=1004,
                has_delivery_options=True,
                pickup=True,
                post_term_delivery=True,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=84222,
                fesh=1004,
                has_delivery_options=True,
                pickup=True,
                post_term_delivery=True,
                delivery_options=[DeliveryOption(price=0, day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=84333,
                fesh=1004,
                has_delivery_options=True,
                pickup=False,
                post_term_delivery=False,
                delivery_options=[DeliveryOption(day_from=0, day_to=1)],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=84444,
                fesh=1004,
                has_delivery_options=False,
                pickup=True,
                post_term_delivery=True,
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=84777,
                fesh=1004,
                has_delivery_options=False,
                pickup=True,
                post_term_delivery=False,
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=84888,
                fesh=1004,
                has_delivery_options=False,
                pickup=False,
                post_term_delivery=True,
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=484444,
                fesh=1004,
                has_delivery_options=True,
                pickup=True,
                post_term_delivery=True,
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=484222,
                fesh=1004,
                has_delivery_options=False,
                pickup=True,
                post_term_delivery=True,
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=41000,
                fesh=1004,
                price=110,
                waremd5='000000000001XTgggggggg',
                hid=5500,
                glparams=[GLParam(param_id=202, value=1)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=55551, fesh=1004, price=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=55552, fesh=1004, price=1),
            Offer(cpa=Offer.CPA_REAL, hyperid=44441, fesh=1004, price=1, waremd5='HgFQyiWYdnbRJ8BWPFAK3g'),
            Offer(cpa=Offer.CPA_REAL, title="3400_1", hyperid=3400, fesh=1004, price=5002, post_term_delivery=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=3400, fesh=1004, price=3002),
            Offer(cpa=Offer.CPA_REAL, title="3400_2", hyperid=3400, fesh=1004, price=7002, post_term_delivery=True),
            Offer(cpa=Offer.CPA_REAL, title="3300_1", hyperid=3300, fesh=1004, price=7, post_term_delivery=True),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=3300,
                fesh=1004,
                price=5,
                delivery_options=[DeliveryOption(price=0, day_from=1, day_to=2)],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1004, price=9),
            Offer(
                cpa=Offer.CPA_REAL,
                vclusterid=1000000101,
                fesh=1004,
                price=50,
                delivery_options=[DeliveryOption(price=0, day_from=1, day_to=2)],
            ),
            Offer(cpa=Offer.CPA_REAL, vclusterid=1000000101, fesh=1004, price=30, waremd5='EWjt-tvUywoY9K1ALCjeuA'),
            Offer(cpa=Offer.CPA_REAL, vclusterid=1000000101, fesh=1004, price=70),
            # fesh 1005
            Offer(cpa=Offer.CPA_REAL, hyperid=3400, fesh=1005, price=3002),
            Offer(cpa=Offer.CPA_REAL, hyperid=3300, fesh=1005, price=9),
            # fesh 1006
            Offer(cpa=Offer.CPA_REAL, hyperid=90111, fesh=1006, price=10),
            Offer(cpa=Offer.CPA_REAL, hyperid=90222, fesh=1006, price=10),
            Offer(cpa=Offer.CPA_REAL, hyperid=90333, fesh=1006, price=10),
            # fesh 1007
            Offer(cpa=Offer.CPA_REAL, hyperid=90111, fesh=1007, price=20),
            Offer(cpa=Offer.CPA_REAL, hyperid=90222, fesh=1007, price=20),
            Offer(cpa=Offer.CPA_REAL, hyperid=90333, fesh=1007, price=20),
            # fesh 1008
            Offer(cpa=Offer.CPA_REAL, hyperid=980111, fesh=1008, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=980222, fesh=1008, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=980333, fesh=1008, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=980444, fesh=1008, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=910111, fesh=1008, pickup=True, waremd5='7tT8xKYaTpeaRO68xa2-HA'),
            Offer(cpa=Offer.CPA_REAL, hyperid=910222, fesh=1008, pickup=True, waremd5='Gb3PysNvUYz-3xo2TbAnkw'),
            Offer(cpa=Offer.CPA_REAL, hyperid=910333, fesh=1008, pickup=True, waremd5='07jMPmyWkwYs394aI0-PzA'),
            Offer(cpa=Offer.CPA_REAL, hyperid=910444, fesh=1008, pickup=True, waremd5='8GaN0stIZ5AJ4Oe_0SK3qQ'),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=9880111,
                fesh=1108,
                pickup=True,
                store=True,
                post_term_delivery=True,
                booking_availabilities=[
                    BookingAvailability(809, 213, 3),
                    BookingAvailability(811, 213, 4),
                    BookingAvailability(810, 213, 10),
                ],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=9880222,
                fesh=1108,
                pickup=True,
                store=True,
                post_term_delivery=True,
                booking_availabilities=[
                    BookingAvailability(809, 213, 5),
                    BookingAvailability(811, 213, 6),
                    BookingAvailability(810, 213, 10),
                ],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=9880333,
                fesh=1108,
                pickup=True,
                store=True,
                booking_availabilities=[
                    BookingAvailability(809, 213, 7),
                    BookingAvailability(811, 213, 0),
                    BookingAvailability(810, 213, 10),
                ],
            ),
            Offer(cpa=Offer.CPA_REAL, hyperid=9880444, fesh=1108, pickup=True, store=False),
            # fesh 1009
            Offer(cpa=Offer.CPA_REAL, hyperid=980111, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=980222, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=980333, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=980444, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=910111, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=910222, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=910333, fesh=1009, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=910444, fesh=1009, pickup=True),
            # fesh 1010
            Offer(cpa=Offer.CPA_REAL, hyperid=1080111, fesh=1010, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=1080222, fesh=1010, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=1080333, fesh=1010, pickup=True),
            # fesh 1011
            Offer(cpa=Offer.CPA_REAL, hyperid=1010111, fesh=1011, store=True, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=1010222, fesh=1011, store=True, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=1010333, fesh=1011, store=False, pickup=True),
            # fesh 1012
            Offer(cpa=Offer.CPA_REAL, hyperid=1210111, fesh=1012, store=True, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=1210222, fesh=1012, store=True, pickup=True),
            Offer(cpa=Offer.CPA_REAL, hyperid=1210333, fesh=1012, store=False, pickup=True),
        ]

    def test_output_format(self):
        """Test output format of model, offer and cluster accordingly"""

        response = self.report.request_json(
            'pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1'
        )
        self.assertFragmentIn(
            response,
            {
                "sorts": [],
                "pager": {},
                "results": [
                    {
                        "entity": "offer-pack",
                        "type": "same-shop",
                        "price": {},
                        "shop": {},
                        "delivery": {},
                        "offers": [{"entity": "offer"}, {"entity": "offer"}, {"entity": "offer-alternative"}],
                    },
                    {"entity": "offer-pack"},
                ],
            },
            preserve_order=True,
        )

    def test_Min_price_for_model_in_pack_Sorting(self):
        """Test:"""
        """   1) _Min_price_for_model_in_pack:"""
        """       Offer with minimal price is selected for input model/cluster for particular shop (offer-pack)."""
        """       For input offers the same input offers are selected, instead of offers in same shop with same 2kind params but cheaper."""
        """   2) _Sorting:"""
        """      Packs are sorted by:"""
        """          a) number of found goods"""
        """          b) in groups with same number of goods they are sorted by ascending price"""

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    # these packs contain 3 goods
                    {
                        "entity": "offer-pack",
                        "price": {"value": "3103"},
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 3100}, "prices": {"value": "3002"}},
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTaaaaaaaQ", "prices": {"value": "100"}},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 44441}, "prices": {"value": "1"}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "3111"},
                        "shop": {"id": 1002},
                        "offers": [
                            {"entity": "offer", "model": {"id": 3100}, "prices": {"value": "3000"}},
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTgggggggg",  # input offer is selected instead of cheaper 22222222222222gggggggg
                                "prices": {"value": "110"},
                            },
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 44441}, "prices": {"value": "1"}},
                        ],
                    },
                    # these packs contain 2 goods
                    {
                        "entity": "offer-pack",
                        "price": {"value": "31"},
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer-alternative"
                            },  # offer '000000000001XTgggggggg' is not selected, because value of '202' is '1' but should be '2' for selection
                            {"entity": "offer", "model": {"id": 1000000101}, "prices": {"value": "30"}},
                            {"entity": "offer", "model": {"id": 44441}, "prices": {"value": "1"}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "301"},
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 1000000101}, "prices": {"value": "300"}},
                            {"entity": "offer", "model": {"id": 44441}, "prices": {"value": "1"}},
                        ],
                    },
                ]
            },
            preserve_order=True,
        )

    def test_entity_ids_param(self):
        """Test that offers order in output equal to order in &shopping-list"""

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer", "model": {"id": 3100}},
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTaaaaaaaQ"},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer", "model": {"id": 3100}},
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTgggggggg"},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 1000000101}},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 1000000101}},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                ]
            },
            preserve_order=True,
        )

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3100:1,m:1000000101:1,o:09lEaAKkQll1XTgggggggg:1,m:44441:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer", "model": {"id": 3100}},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTaaaaaaaQ"},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer", "model": {"id": 3100}},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTgggggggg"},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 1000000101}},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 1000000101}},
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 44441}},
                        ],
                    },
                ]
            },
            preserve_order=True,
        )

    def check_pager_last_page(self, response, page_number):
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "pager": {
                    "entity": "pager",
                    "page": page_number,
                    "total": 4,
                    "itemsPerPage": 2,
                },
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

    def test_pager(self):
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1&rids=213&numdoc=2&page={page_num}"
        # page 1
        response = self.report.request_json(base_request.format(page_num="1"))
        self.assertFragmentIn(
            response,
            {
                "total": 4,
                "pager": {
                    "entity": "pager",
                    "page": 1,
                    "total": 4,
                    "itemsPerPage": 2,
                },
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1002},
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        # page 2
        response = self.report.request_json(base_request.format(page_num="2"))
        self.check_pager_last_page(response, 2)

        # page 3 (output page 2, because 2 is last)
        response = self.report.request_json(base_request.format(page_num="3"))
        self.check_pager_last_page(response, 2)

        """Проверяется, что общее количество для показа = total"""
        self.access_log.expect(total_renderable='4').times(3)

    def test_price_filter(self):
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1{price_range}"
        # [mcpricefrom, mcpriceto]
        response = self.report.request_json(base_request.format(price_range="&mcpricefrom=10&mcpriceto=400"))
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",  # this filter is tested, because it is calculated for packs, but not for particular offers
                        "values": [{"max": "400", "min": "10"}, {"max": "301", "min": "31"}],
                    }
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "31"},
                        "shop": {"id": 1004},
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "301"},
                        "shop": {"id": 1003},
                    },
                ],
            },
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(base_request.format(price_range="&mcpricefrom=400&mcpriceto=10"))
        self.assertEqual(0, response.count({"entity": "offer-pack"}))

        # [mcpricefrom, +inf)
        response = self.report.request_json(base_request.format(price_range="&mcpricefrom=3104"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "3111"},
                        "shop": {"id": 1002},
                    }
                ]
            },
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

        # [0 , mcpriceto]
        response = self.report.request_json(base_request.format(price_range="&mcpriceto=31"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "31"},
                        "shop": {"id": 1004},
                    },
                ]
            },
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

    def test_quality_raiting_filter(self):
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1&qrfrom={quality_from}"
        # qrfrom=4
        response = self.report.request_json(base_request.format(quality_from="4"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer-pack", "shop": {"id": 1004, "qualityRating": 5}},
                    {"entity": "offer-pack", "shop": {"id": 1003, "qualityRating": 4}},
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        # qrfrom=5
        response = self.report.request_json(base_request.format(quality_from="5"))
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer-pack", "shop": {"id": 1004, "qualityRating": 5}}]}
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

    def test_free_delivery_filter(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3300:1,m:1000000101:1&free_delivery=1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer", "delivery": {"isFree": True}, "prices": {"value": "5"}},
                            {"entity": "offer", "delivery": {"isFree": True}, "prices": {"value": "50"}},
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_offer_shipping_filter(self):
        base_request = (
            "pp=155&place=alt_same_shop&shopping-list=o:09lEaAKkQll1XTgggggggg:1,m:3300:1&rids={rids}{offer_shipping}"
        )
        # pickup
        response = self.report.request_json(base_request.format(rids="213", offer_shipping="&offer-shipping=pickup"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer",
                                "delivery": {
                                    "hasPickup": True,
                                    "hasLocalStore": False,
                                },
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer",
                                "delivery": {
                                    "hasPickup": True,
                                    "hasLocalStore": False,
                                },
                                "prices": {"value": "32"},
                            },
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

        # postomat
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3400:1,m:3300:1,m:44441:1&rids=213&offer-shipping=postomat"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer", "titles": {"raw": "3400_1"}},
                            {"entity": "offer", "titles": {"raw": "3300_1"}},
                            {"entity": "offer-alternative"},
                        ],
                    }
                ]
            },
        )

        # other
        response = self.report.request_json(base_request.format(rids="2", offer_shipping=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "outlet": {"entity": "outlet", "id": "200", "region": {"id": 2}}},
                            {"entity": "offer", "outlet": {"entity": "outlet", "id": "200", "region": {"id": 2}}},
                        ],
                    }
                ]
            },
        )

        response = self.report.request_json(base_request.format(rids="216", offer_shipping=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "outlet": {"entity": "outlet", "id": "300", "region": {"id": 216}}},
                            {"entity": "offer", "outlet": {"entity": "outlet", "id": "300", "region": {"id": 216}}},
                        ],
                    }
                ]
            },
        )

    def test_delivery_interval_filter(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:22222222222222gggggggg:1,m:44441:1&rids=213&delivery_interval=2"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 0,
                                            "dayTo": 0,
                                        }
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 44441},
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 0,
                                            "dayTo": 2,
                                        }
                                    ]
                                },
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1002},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTgggggggg",  # 09lEaAKkQll1XTgggggggg is selected instead of 22222222222222gggggggg because dayTo for 22222222222222gggggggg equal to 4
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 0,
                                            "dayTo": 2,
                                        }
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 44441},
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 0,
                                            "dayTo": 2,
                                        }
                                    ]
                                },
                            },
                        ],
                    },
                ]
            },
        )
        self.assertEqual(4, response.count({"entity": "offer"}))

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:22222222222222gggggggg:1,m:44442:1&rids=213&delivery_interval=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 0,
                                            "dayTo": 0,
                                        }
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 44442},
                                "delivery": {
                                    "options": [
                                        {
                                            "dayFrom": 0,
                                            "dayTo": 0,
                                        }
                                    ]
                                },
                            },
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

    def test_rids_filter(self):
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:3400:1,m:3300:1{rids}"
        response = self.report.request_json(base_request.format(rids=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer", "model": {"id": 3400}},
                            {"entity": "offer", "model": {"id": 3300}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1005},
                        "offers": [
                            {"entity": "offer", "model": {"id": 3400}},
                            {"entity": "offer", "model": {"id": 3300}},
                        ],
                    },
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(base_request.format(rids="&rids=213"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer", "model": {"id": 3400}},
                            {"entity": "offer", "model": {"id": 3300}},
                        ],
                    }
                ]
            },
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

    def test_offer_without_model(self):
        """Test case when offer has no attached to appropriate model"""
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:hhhhhhhhhhpppppppppppg:1,o:nnnnnnnnnnpppppppppppg:1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "wareId": "hhhhhhhhhhpppppppppppg"},
                            {"entity": "offer", "wareId": "nnnnnnnnnnpppppppppppg"},
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

    def test_multiple_entities(self):
        """Test ability to set count for each input model/cluster/offer"""
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:hhhhhhhhhhpppppppppppg:2,o:nnnnnnnnnnpppppppppppg:3"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "900", "currency": "RUR"},
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "prices": {"value": "150"}},
                            {"entity": "offer", "prices": {"value": "200"}},
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

    def test_skip_single_offer_in_pack(self):
        """Test that packs with single offer are skipped"""
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:44441:1,m:44442:1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},  # other shops are skipped, because contain only offers with hyperid=44441
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 44441},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 44442},
                            },
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

    def test_cpa_offers_only(self):
        """Test that output should contain only CPA offers"""
        """    Only shop with id=1004 contains 2 offers in result. Offers from other shops are filtered out because of non-CPA."""
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:55551:1,m:55552:1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 55551},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 55552},
                            },
                        ],
                    }
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer"}))

    def test_filters(self):
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:55551:1,m:55552:1&rids=213")
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "glprice"},
                    {"id": "qrfrom"},
                    {"id": "free-delivery"},
                    {"id": "offer-shipping"},
                    {"id": "delivery-interval"},
                ]
            },
        )

    def test_position_without_pager(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1&show-urls=encrypted&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "wareId": "FSqiKO1icV4qzU-I7w8qLg"},  # 1
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTaaaaaaaQ"},  # 2
                            {"entity": "offer-alternative"},  # 3   - no position
                            {"entity": "offer", "wareId": "1AxLbIEFo2u-f0ayN1RG5g"},  # 4
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1002},
                        "offers": [
                            {"entity": "offer", "wareId": "_qQnWXU28-IUghltMZJwNw"},  # 5
                            {"entity": "offer", "wareId": "09lEaAKkQll1XTgggggggg"},  # 6
                            {"entity": "offer-alternative"},  # 7   - no position
                            {"entity": "offer", "wareId": "DuE098x_rinQLZn3KKrELw"},  # 8
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer-alternative"},  # 9   - no position
                            {"entity": "offer-alternative"},  # 10  - no position
                            {"entity": "offer", "wareId": "EWjt-tvUywoY9K1ALCjeuA"},  # 11
                            {"entity": "offer", "wareId": "HgFQyiWYdnbRJ8BWPFAK3g"},  # 12
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer-alternative"},  # 13  - no position
                            {"entity": "offer-alternative"},  # 14  - no position
                            {"entity": "offer", "wareId": "fDbQKU6BwzM0vDugM73auA"},  # 15
                            {"entity": "offer", "wareId": "8zT9D0cAuDwEtf-xBL280A"},  # 16
                        ],
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(4, response.count({"entity": "offer-pack"}))

        self.show_log_tskv.expect(ware_md5='FSqiKO1icV4qzU-I7w8qLg', position=1)
        self.show_log_tskv.expect(ware_md5='09lEaAKkQll1XTaaaaaaaQ', position=2)
        self.show_log_tskv.expect(ware_md5='1AxLbIEFo2u-f0ayN1RG5g', position=4)
        self.show_log_tskv.expect(ware_md5='_qQnWXU28-IUghltMZJwNw', position=5)
        self.show_log_tskv.expect(ware_md5='09lEaAKkQll1XTgggggggg', position=6)
        self.show_log_tskv.expect(ware_md5='DuE098x_rinQLZn3KKrELw', position=8)
        self.show_log_tskv.expect(ware_md5='EWjt-tvUywoY9K1ALCjeuA', position=11)
        self.show_log_tskv.expect(ware_md5='HgFQyiWYdnbRJ8BWPFAK3g', position=12)
        self.show_log_tskv.expect(ware_md5='fDbQKU6BwzM0vDugM73auA', position=15)
        self.show_log_tskv.expect(ware_md5='8zT9D0cAuDwEtf-xBL280A', position=16)

    def test_position_with_pager(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1,m:44441:1&show-urls=encrypted&page=2&numdoc=2&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer-alternative"},  # 9   - no position
                            {"entity": "offer-alternative"},  # 10  - no position
                            {"entity": "offer", "wareId": "EWjt-tvUywoY9K1ALCjeuA"},  # 11
                            {"entity": "offer", "wareId": "HgFQyiWYdnbRJ8BWPFAK3g"},  # 12
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer-alternative"},  # 13  - no position
                            {"entity": "offer-alternative"},  # 14  - no position
                            {"entity": "offer", "wareId": "fDbQKU6BwzM0vDugM73auA"},  # 15
                            {"entity": "offer", "wareId": "8zT9D0cAuDwEtf-xBL280A"},  # 16
                        ],
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        self.show_log_tskv.expect(ware_md5='EWjt-tvUywoY9K1ALCjeuA', position=11)
        self.show_log_tskv.expect(ware_md5='HgFQyiWYdnbRJ8BWPFAK3g', position=12)
        self.show_log_tskv.expect(ware_md5='fDbQKU6BwzM0vDugM73auA', position=15)
        self.show_log_tskv.expect(ware_md5='8zT9D0cAuDwEtf-xBL280A', position=16)

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_delivery_included(self):
        # simple 'deliveryincluded'
        request_base = "pp=155&place=alt_same_shop&shopping-list=o:09lEaAKkQll1XTgggggggg:1,m:3300:1,m:3400:1,m:3200:1&rids=213&deliveryincluded=1{additional}"
        response = self.report.request_json(request_base.format(additional=""))
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",
                        "values": [
                            {
                                "max": "5109",  # pack-candidate with price 5109 (pickup pack) from fesh=1004 is not shown but taken into account for filters calculation
                                "min": "237",
                            }
                        ],
                    }
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "237", "currency": "RUR"},
                        "shop": {"id": 1001},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 41000},
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},
                                "delivery": {"price": {"value": "10"}},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3300},
                                "prices": {"value": "32"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 3200},
                                "prices": {"value": "5"},
                                "delivery": {"price": {"value": "100"}},
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "243", "currency": "RUR"},
                        "shop": {"id": 1002},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 41000},
                                "wareId": "09lEaAKkQll1XTgggggggg",
                                "prices": {"value": "110"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3300},
                                "prices": {"value": "30"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 3200},
                                "prices": {"value": "3"},
                                "delivery": {"price": {"value": "100"}},
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "3107", "currency": "RUR"},
                        "shop": {"id": 1004},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 3300},
                                "prices": {"value": "5"},
                                # delivery price is "0"
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3400},
                                "prices": {"value": "3002"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {"entity": "offer-alternative"},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "3107", "currency": "RUR"},
                        "shop": {"id": 1003},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 3400},
                                "prices": {"value": "3002"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3200},
                                "prices": {"value": "5"},
                                "delivery": {"price": {"value": "100"}},
                            },
                        ],
                    },
                ],
            },
        )

        # deliveryincluded with price range
        response = self.report.request_json(request_base.format(additional="&mcpricefrom=237&mcpriceto=250"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "237", "currency": "RUR"},
                        "shop": {"id": 1001},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 41000},
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},
                                "delivery": {"price": {"value": "10"}},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3300},
                                "prices": {"value": "32"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 3200},
                                "prices": {"value": "5"},
                                "delivery": {"price": {"value": "100"}},
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "243", "currency": "RUR"},
                        "shop": {"id": 1002},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 41000},
                                "wareId": "09lEaAKkQll1XTgggggggg",
                                "prices": {"value": "110"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3300},
                                "prices": {"value": "30"},
                                "delivery": {"price": {"value": "100"}},
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 3200},
                                "prices": {"value": "3"},
                                "delivery": {"price": {"value": "100"}},
                            },
                        ],
                    },
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        # deliveryincluded and sort
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:7771:1,m:7772:1&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "200"},
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "prices": {"value": "100"}},
                            {"entity": "offer", "prices": {"value": "100"}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "220"},
                        "shop": {"id": 1002},
                        "offers": [
                            {"entity": "offer", "prices": {"value": "110"}},
                            {"entity": "offer", "prices": {"value": "110"}},
                        ],
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:7771:1,m:7772:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "240"},
                        "shop": {"id": 1002},
                        "offers": [
                            {"entity": "offer", "prices": {"value": "110"}, "delivery": {"price": {"value": "20"}}},
                            {"entity": "offer", "prices": {"value": "110"}, "delivery": {"price": {"value": "20"}}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "250"},
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "prices": {"value": "100"}, "delivery": {"price": {"value": "50"}}},
                            {"entity": "offer", "prices": {"value": "100"}, "delivery": {"price": {"value": "50"}}},
                        ],
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

    def check_delivery_pack_candidate(self, response):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "delivery": {"isAvailable": True, "hasPickup": False},
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "model": {"id": 338881},
                                "delivery": {"isAvailable": True, "hasPickup": False},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 339991},
                                "delivery": {"isAvailable": True, "hasPickup": False},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 3311111},
                                "delivery": {"isAvailable": True, "hasPickup": False},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_delivery_section_merge(self):
        # fullText
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:8881:1,m:9991:1,m:11111:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "370"},
                        "shop": {"id": 1002},
                        "delivery": {
                            "isAvailable": True,
                            "hasPickup": False,
                            "price": {"value": "40"},
                        },
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "430"},
                        "shop": {"id": 1001},
                        "delivery": {
                            "isAvailable": True,
                            "hasPickup": True,
                            "price": {"value": "100"},
                        },
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        self.assertFragmentNotIn(
            response, {"results": [{"entity": "offer-pack", "shop": {"id": 1001}, "delivery": {"shipping"}}]}
        )
        # pickup (principal ability of delivery is absent)
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:8881:1,m:9991:1,m:11111:1&rids=3"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "330"},
                        "shop": {"id": 1001},
                        "delivery": {"isAvailable": False, "hasPickup": True},
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "220"},
                        "shop": {"id": 1003},
                        "delivery": {"isAvailable": False, "hasPickup": True},
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))
        # delivery, no pickup
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:84111:1,m:84222:1,m:84333:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "delivery": {"isAvailable": True, "hasPickup": False},
                        "offers": [
                            {"entity": "offer", "delivery": {"isAvailable": True, "hasPickup": True}},
                            {"entity": "offer", "delivery": {"isAvailable": True, "hasPickup": True}},
                            {"entity": "offer", "delivery": {"isAvailable": True, "hasPickup": False}},
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        # pack-candidates selection
        # delivery_count > pickup_count
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:228881:1,m:229991:1,m:338881:1,m:339991:1,m:3311111:1&rids=213&deliveryincluded=1"
        )
        self.check_delivery_pack_candidate(response)
        # delivery_count == pickup_count
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:228881:1,m:229991:1,m:2211111:1,m:338881:1,m:339991:1,m:3311111:1&rids=213&deliveryincluded=1"
        )
        self.check_delivery_pack_candidate(response)
        # delivery_count < pickup_count
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:228881:1,m:229991:1,m:2211111:1,m:338881:1,m:339991:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "delivery": {"isAvailable": False, "hasPickup": True},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 228881},
                                "delivery": {"isAvailable": False, "hasPickup": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 229991},
                                "delivery": {"isAvailable": False, "hasPickup": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 2211111},
                                "delivery": {"isAvailable": False, "hasPickup": True},
                            },
                            {"entity": "offer-alternative"},
                            {"entity": "offer-alternative"},
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        # delivery services
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:944111:1,m:944222:1,m:944333:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "delivery": {
                            "availableServices": [
                                {"serviceId": 7},
                                {"serviceId": 99},
                                {"serviceId": 9},
                                {"serviceId": 11},
                                {"serviceId": 12},
                            ]
                        },
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 944111},
                                "delivery": {
                                    "availableServices": [
                                        {"serviceId": 103},
                                        {"serviceId": 7},
                                        {"serviceId": 99},
                                        {"serviceId": 9},
                                        {"serviceId": 11},
                                        {"serviceId": 12},
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 944222},
                                "delivery": {
                                    "availableServices": [
                                        {"serviceId": 103},
                                        {"serviceId": 7},
                                        {"serviceId": 99},
                                        {"serviceId": 9},
                                        {"serviceId": 11},
                                        {"serviceId": 12},
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 944333},
                                "delivery": {
                                    "availableServices": [
                                        {"serviceId": 7},
                                        {"serviceId": 99},
                                        {"serviceId": 9},
                                        {"serviceId": 11},
                                        {"serviceId": 12},
                                    ]
                                },
                            },
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "delivery": {"availableServices": [{"serviceId": 103}]},  # no 103
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 944333},
                                "delivery": {"availableServices": [{"serviceId": 103}]},  # no 103
                            }
                        ],
                    }
                ]
            },
        )
        # isDownloadable, hasLocalStore, isFree
        # false
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:94111:1,m:94222:1,m:94333:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "delivery": {"isDownloadable": False, "hasLocalStore": False, "isFree": False},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 94111},
                                "delivery": {"isDownloadable": True, "hasLocalStore": True, "isFree": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 94222},
                                "delivery": {"isDownloadable": True, "hasLocalStore": True, "isFree": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 94333},
                                "delivery": {"isDownloadable": False, "hasLocalStore": False, "isFree": False},
                            },
                        ],
                    }
                ]
            },
        )

        # true
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:94111:1,m:94222:1&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "delivery": {"isDownloadable": True, "hasLocalStore": True, "isFree": True},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 94111},
                                "delivery": {"isDownloadable": True, "hasLocalStore": True, "isFree": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 94222},
                                "delivery": {"isDownloadable": True, "hasLocalStore": True, "isFree": True},
                            },
                        ],
                    }
                ]
            },
        )
        # isPriorityRegion, isCountrywide
        # false
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:94111:1,m:94222:1,m:94444:1&rids=3"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "delivery": {"isPriorityRegion": False, "isCountrywide": False},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 94111},
                                "delivery": {"isPriorityRegion": True, "isCountrywide": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 94222},
                                "delivery": {"isPriorityRegion": True, "isCountrywide": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 94444},
                                "delivery": {"isPriorityRegion": False, "isCountrywide": False},
                            },
                        ],
                    }
                ]
            },
        )
        # true
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:94111:1,m:94222:1&rids=3")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "delivery": {"isPriorityRegion": True, "isCountrywide": True},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 94111},
                                "delivery": {"isPriorityRegion": True, "isCountrywide": True},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 94222},
                                "delivery": {"isPriorityRegion": True, "isCountrywide": True},
                            },
                        ],
                    }
                ]
            },
        )
        # inStock
        # false
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:94222:1,m:94444:1,m:94555:1&rids=3"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "delivery": {"inStock": False},
                        "offers": [
                            {"entity": "offer", "model": {"id": 94222}, "delivery": {"inStock": False}},
                            {"entity": "offer", "model": {"id": 94444}, "delivery": {"inStock": True}},
                            {"entity": "offer", "model": {"id": 94555}, "delivery": {"inStock": True}},
                        ],
                    }
                ]
            },
        )
        # true
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:94444:1,m:94555:1&rids=3")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "delivery": {"inStock": True},
                        "offers": [
                            {"entity": "offer", "model": {"id": 94444}, "delivery": {"inStock": True}},
                            {"entity": "offer", "model": {"id": 94555}, "delivery": {"inStock": True}},
                        ],
                    }
                ]
            },
        )

    def test_multiple_entities_with_same_id(self):
        # models
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:8881:1,m:9991:1,m:11111:1,m:11111:2"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "550"},
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 8881}, "prices": {"value": "110"}},  # * 1 = 110
                            {"entity": "offer", "model": {"id": 9991}, "prices": {"value": "110"}},  # * 1 = 110
                            {"entity": "offer", "model": {"id": 11111}, "prices": {"value": "110"}},  # * 1 = 110
                            {"entity": "offer", "model": {"id": 11111}, "prices": {"value": "110"}},  # * 2 = 220
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        # offers
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:09lEaAKkQll1XTgggggggg:1,o:09lEaAKkQll1XTgggggggg:2,m:8881:1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "410"},
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},  # * 1 = 100
                            },
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},  # * 2 = 200
                            },
                            {"entity": "offer", "model": {"id": 8881}, "prices": {"value": "110"}},  # * 1 = 110
                        ],
                    }
                ]
            },
            preserve_order=True,
        )

    def test_calc_pack(self):
        base_request = "pp=155&place=alt_same_shop&shopping-list=o:1AxLbIEFo2u-f0ayN1RG5g:1,o:FSqiKO1icV4qzU-I7w8qLg:2,e:0:0,o:09lEaAKkQll1XTaaaaaaaQ:1,e:0:0,o:hhhhhhhhhhpppppppppppg:3{add_offers}&calc-pack=1&rids=213{delivery}"  # noqa
        response = self.report.request_json(base_request.format(add_offers="", delivery=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "6555", "currency": "RUR"},
                        "shop": {"id": 1001},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "1AxLbIEFo2u-f0ayN1RG5g",
                                "prices": {"value": "1"},  # * 1 = 1
                            },
                            {
                                "entity": "offer",
                                "wareId": "FSqiKO1icV4qzU-I7w8qLg",
                                "prices": {"value": "3002"},  # * 2 = 6004
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},  # * 1 = 100
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "wareId": "hhhhhhhhhhpppppppppppg",
                                "prices": {"value": "150"},  # * 3 = 450
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(base_request.format(add_offers="", delivery="&deliveryincluded=1"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "6655", "currency": "RUR"},
                        "shop": {"id": 1001},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "1AxLbIEFo2u-f0ayN1RG5g",
                                "prices": {"value": "1"},  # * 1 = 1
                                "delivery": {"price": {"value": "100"}},
                            },
                            {
                                "entity": "offer",
                                "wareId": "FSqiKO1icV4qzU-I7w8qLg",
                                "prices": {"value": "3002"},  # * 2 = 6004
                                "delivery": {"price": {"value": "100"}},
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},  # * 1 = 100
                                "delivery": {"price": {"value": "10"}},
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "wareId": "hhhhhhhhhhpppppppppppg",
                                "prices": {"value": "150"},  # * 3 = 450
                                "delivery": {"price": {"value": "100"}},
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

        # single offer in pack is OK when &calc-pack=1
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:1AxLbIEFo2u-f0ayN1RG5g:1&calc-pack=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "1AxLbIEFo2u-f0ayN1RG5g",
                            }
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))
        self.assertEqual(1, response.count({"entity": "offer"}))

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:1AxLbIEFo2u-f0ayN1RG5g:1,e:0:0&calc-pack=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "1AxLbIEFo2u-f0ayN1RG5g",
                            },
                            {"entity": "offer-alternative"},
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))
        self.assertEqual(1, response.count({"entity": "offer"}))

        # multiple offers with same id
        response = self.report.request_json(
            base_request.format(add_offers=",o:hhhhhhhhhhpppppppppppg:1,o:1AxLbIEFo2u-f0ayN1RG5g:2", delivery="")
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "6707", "currency": "RUR"},
                        "shop": {"id": 1001},
                        "delivery": {"price": {"value": "100"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "1AxLbIEFo2u-f0ayN1RG5g",
                                "prices": {"value": "1"},  # * 1 = 1
                            },
                            {
                                "entity": "offer",
                                "wareId": "FSqiKO1icV4qzU-I7w8qLg",
                                "prices": {"value": "3002"},  # * 2 = 6004
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "wareId": "09lEaAKkQll1XTaaaaaaaQ",
                                "prices": {"value": "100"},  # * 1 = 100
                            },
                            {"entity": "offer-alternative"},
                            {
                                "entity": "offer",
                                "wareId": "hhhhhhhhhhpppppppppppg",
                                "prices": {"value": "150"},  # * 3 = 450
                            },
                            {
                                "entity": "offer",
                                "wareId": "hhhhhhhhhhpppppppppppg",
                                "prices": {"value": "150"},  # * 1 = 150
                            },
                            {
                                "entity": "offer",
                                "wareId": "1AxLbIEFo2u-f0ayN1RG5g",
                                "prices": {"value": "1"},  # * 2 = 2
                            },
                        ],
                    }
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))
        self.assertEqual(6, response.count({"entity": "offer"}))

    def check_group_model_response(self, response):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1002},
                        "offers": [
                            {"entity": "offer", "model": {"id": 7000}, "prices": {"value": "1"}},
                            {"entity": "offer", "model": {"id": 3200}, "prices": {"value": "3"}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 7002}, "prices": {"value": "1"}},
                            {"entity": "offer", "model": {"id": 3200}, "prices": {"value": "5"}},
                        ],
                    },
                ]
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

    def test_group_model(self):
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:7000:1,m:3200:1")
        self.check_group_model_response(response)

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:CdvaDsKX-OH7j6OX79D3Cg:1,m:3200:1"
        )
        self.check_group_model_response(response)

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_section_filters(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:84111:1,m:84222:1,m:84333:1,m:84444:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"found": 1, "value": "delivery"},
                            {"found": 1, "value": "pickup"},
                        ],
                    },
                    {"id": "delivery-interval", "values": [{"found": 1, "value": "1"}]},
                    {
                        "id": "qrfrom",
                        "values": [{"found": 1, "value": "2"}, {"found": 1, "value": "3"}, {"found": 1, "value": "4"}],
                    },
                    {"id": "free-delivery", "values": [{"found": 1, "value": "1"}]},
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer", "model": {"id": 84111}},
                            {"entity": "offer", "model": {"id": 84222}},
                            {"entity": "offer", "model": {"id": 84333}},
                        ],
                    }
                ],
            },
        )
        self.assertEqual(3, response.count({"entity": "offer"}))
        # fesh=1003
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:83111:1,m:83222:1,m:83333:1,m:83444:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "offer-shipping", "values": [{"found": 1, "value": "delivery"}]},
                    {"id": "delivery-interval", "values": [{"found": 1, "value": "5"}]},
                    {
                        "id": "qrfrom",
                        "values": [{"found": 1, "value": "2"}, {"found": 1, "value": "3"}, {"found": 1, "value": "4"}],
                    },
                    {"id": "free-delivery", "values": [{"found": 1, "value": "1"}]},
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer", "model": {"id": 83111}},
                            {"entity": "offer", "model": {"id": 83222}},
                            {"entity": "offer", "model": {"id": 83333}},
                        ],
                    }
                ],
            },
        )
        self.assertEqual(3, response.count({"entity": "offer"}))
        # fesh=1001
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:81111:1,m:81222:1,m:81333:1,m:81444:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"found": 1, "value": "pickup"},
                        ],
                    },
                    {
                        "id": "qrfrom",
                        "values": [{"found": 1, "value": "2"}, {"found": 0, "value": "3"}, {"found": 0, "value": "4"}],
                    },
                    {"id": "free-delivery", "values": [{"found": 0, "value": "1"}]},
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 81111}},
                            {"entity": "offer", "model": {"id": 81222}},
                            {"entity": "offer", "model": {"id": 81444}},
                        ],
                    }
                ],
            },
        )
        self.assertEqual(3, response.count({"entity": "offer"}))
        # all fesh
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:80111:1,m:80222:1,m:80333:1,m:80444:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"found": 2, "value": "delivery"},
                            {"found": 2, "value": "pickup"},
                        ],
                    },
                    {"id": "delivery-interval", "values": [{"found": 1, "value": "1"}, {"found": 2, "value": "5"}]},
                    {
                        "id": "qrfrom",
                        "values": [{"found": 3, "value": "2"}, {"found": 2, "value": "3"}, {"found": 2, "value": "4"}],
                    },
                    {"id": "free-delivery", "values": [{"found": 2, "value": "1"}]},
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 80111}},
                            {"entity": "offer", "model": {"id": 80222}},
                            {"entity": "offer", "model": {"id": 80444}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer", "model": {"id": 80111}},
                            {"entity": "offer", "model": {"id": 80222}},
                            {"entity": "offer", "model": {"id": 80333}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "offers": [
                            {"entity": "offer", "model": {"id": 80111}},
                            {"entity": "offer", "model": {"id": 80222}},
                            {"entity": "offer", "model": {"id": 80333}},
                        ],
                    },
                ],
            },
        )
        self.assertEqual(9, response.count({"entity": "offer"}))
        self.assertFragmentNotIn(
            response, {"results": [{"entity": "regionalDelimiter"}]}  # no regionalDelimiter when no regional packs
        )
        # fitlers from all pack-candidates
        base_request = (
            "pp=155&place=alt_same_shop&shopping-list=m:181111:1,m:181222:1,m:181333:1,m:181444:1&rids=213{shipping}"
        )
        response = self.report.request_json(base_request.format(shipping=""))
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [{"found": 1, "value": "delivery"}, {"found": 1, "value": "pickup"}],
                    },
                ],
                "results": [
                    {  # This is 'delivery' pack and it is only pack in output.
                        # But 'filters' section contains 'delivery' and 'pickup' shippind types because both 'delivery' and 'pickup' pack-candidates are taken into account.
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "delivery": {"isAvailable": True, "hasPickup": False},
                    }
                ],
            },
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(base_request.format(shipping="&offer-shipping=pickup"))
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": "offer-shipping", "values": [{"found": 1, "value": "pickup"}]}],
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "delivery": {"isAvailable": False, "hasPickup": True},
                    }
                ],
            },
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(base_request.format(shipping="&offer-shipping=delivery"))
        self.assertFragmentIn(
            response,
            {
                "filters": [{"id": "offer-shipping", "values": [{"found": 1, "value": "delivery"}]}],
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "delivery": {"isAvailable": True, "hasPickup": False},
                    }
                ],
            },
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

    def test_regional_shop_duplicates_absence(self):
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:90111:1,m:90222:1,m:90333:1")
        self.assertFragmentIn(
            response,
            {"results": [{"entity": "offer-pack", "price": {"value": "30"}, "shop": {"id": 1006}}]},
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))  # fesh=1007 is absent

    def test_round_before_pack_price_calculation(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:103111:1,m:103222:1,m:103333:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "glprice",
                        "values": [
                            {
                                "max": "34",
                                "min": "34",
                            }
                        ],
                    }
                ],
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "34"},
                        "delivery": {"price": {"value": "2"}},
                        "offers": [
                            {"entity": "offer", "model": {"id": 103111}, "prices": {"value": "10"}},
                            {"entity": "offer", "model": {"id": 103222}, "prices": {"value": "11"}},
                            {"entity": "offer", "model": {"id": 103333}, "prices": {"value": "11"}},
                        ],
                    }
                ],
            },
            preserve_order=True,
        )
        self.assertEqual(3, response.count({"entity": "offer"}))

    def check_light_pack(self, response, price1, price2):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": price1},
                        "shop": {"id": 1004},
                        "prepayEnabled": False,
                        "offers": [
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": "73111",
                                "title": "HYPERID-73111",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "offer",
                                "id": "qaerG3FLBO_yXOXngxNZAg",
                                "title": "offer-73222",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "light",
                                "type": "offer",
                                "id": "Z3l_Hw8C7FzNXjq9LU5bgQ",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "light",
                                "type": "offer",
                                "id": "nx1WWdWID7Qn9uBK5QD8JQ",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "model",
                                "id": "556677",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "offer",
                                "id": "nnnnnnnnnnnnnnnnnnnnnn",
                                "title": "",
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": price2},
                        "prepayEnabled": False,
                        "shop": {"id": 1003},
                        "offers": [
                            {
                                "entity": "offer-alternative",
                                "showType": "light",
                                "type": "offer",
                                "id": "lOlF9TzZLwWcibYeKksTIw",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "light",
                                "type": "offer",
                                "id": "qaerG3FLBO_yXOXngxNZAg",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": "74111",
                                "title": "HYPERID-74111",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": "74222",
                                "title": "HYPERID-74222",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "model",
                                "id": "556677",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "offer",
                                "id": "nnnnnnnnnnnnnnnnnnnnnn",
                                "title": "",
                            },
                        ],
                    },
                ]
            },
            preserve_order=True,
        )

    def test_offer_alternative(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:73111:1,o:qaerG3FLBO_yXOXngxNZAg:1,m:74111:1,m:74222:1,m:556677:1,o:nnnnnnnnnnnnnnnnnnnnnn:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "30"},
                        "prepayEnabled": False,
                        "shop": {"id": 1004},
                        "offers": [
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": "73111",
                                "title": "HYPERID-73111",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "offer",
                                "id": "qaerG3FLBO_yXOXngxNZAg",
                                "title": "offer-73222",
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 74111},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 74222},
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "model",
                                "id": "556677",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "offer",
                                "id": "nnnnnnnnnnnnnnnnnnnnnn",
                                "title": "",
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "price": {"value": "35"},
                        "prepayEnabled": False,
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer", "model": {"id": 73111}},
                            {"entity": "offer", "wareId": "qaerG3FLBO_yXOXngxNZAg"},
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": "74111",
                                "title": "HYPERID-74111",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": "74222",
                                "title": "HYPERID-74222",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "model",
                                "id": "556677",
                                "title": "",
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "nonexistent",
                                "type": "offer",
                                "id": "nnnnnnnnnnnnnnnnnnnnnn",
                                "title": "",
                            },
                        ],
                    },
                ]
            },
            preserve_order=True,
        )

        common_request = "pp=155&place=alt_same_shop&shopping-list=m:73111:1,o:qaerG3FLBO_yXOXngxNZAg:1,m:74111:1,m:74222:1,m:556677:1,o:nnnnnnnnnnnnnnnnnnnnnn:1&rids=213&light-pack=1{delivery}"
        response = self.report.request_json(common_request.format(delivery=""))
        self.check_light_pack(response, "30", "35")

        response = self.report.request_json(common_request.format(delivery="&deliveryincluded=1"))
        self.check_light_pack(response, "32", "37")

    def test_show_explicit_content(self):
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:111222:1,m:111333:1,m:111444:1,m:111555:1&rids=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer-alternative", "showType": "nonexistent"},
                            {"entity": "offer-alternative", "showType": "nonexistent"},
                            {"entity": "offer", "model": {"id": 111444}},
                            {"entity": "offer", "model": {"id": 111555}},
                        ],
                    }
                ]
            },
        )

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:111222:1,m:111333:1,m:111444:1,m:111555:1&rids=0&show_explicit_content=medicine"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 111222}},
                            {"entity": "offer", "model": {"id": 111333}},
                            {"entity": "offer", "model": {"id": 111444}},
                            {"entity": "offer", "model": {"id": 111555}},
                        ],
                    }
                ]
            },
        )

    # MARKETOUT-9059
    def test_show_explicit_content_all(self):
        """
        Тот же запрос, что и в `test_show_explicit_content`, но с =all вместо =medicine
        Результат должен быть тем же
        """
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:111222:1,m:111333:1,m:111444:1,m:111555:1&rids=0&show_explicit_content=all"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 111222}},
                            {"entity": "offer", "model": {"id": 111333}},
                            {"entity": "offer", "model": {"id": 111444}},
                            {"entity": "offer", "model": {"id": 111555}},
                        ],
                    }
                ]
            },
        )

    def test_adult(self):
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:90444:1,m:90555:1,m:90666:1")
        self.assertFragmentIn(
            response,
            {
                "adult": True,
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer-alternative"},
                            {"entity": "offer", "model": {"id": 90555}},
                            {"entity": "offer", "model": {"id": 90666}},
                        ],
                    }
                ],
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:90444:1,m:90555:1,m:90666:1&adult=1"
        )
        self.assertFragmentIn(
            response,
            {
                "adult": True,
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer", "model": {"id": 90444}},
                            {"entity": "offer", "model": {"id": 90555}},
                            {"entity": "offer", "model": {"id": 90666}},
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {"entity": "offer", "model": {"id": 90444}},
                            {"entity": "offer", "model": {"id": 90555}},
                            {"entity": "offer", "model": {"id": 90666}},
                        ],
                    },
                ],
            },
            preserve_order=True,
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))

        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:91444:1,m:91555:1,m:91666:1")
        self.assertFragmentIn(response, {"adult": True})
        self.assertEqual(0, response.count({"entity": "offer-pack"}))

        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:93555:1,m:93666:1")
        self.assertFragmentIn(
            response,
            {
                "adult": False,
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "offers": [
                            {"entity": "offer", "model": {"id": 93555}},
                            {"entity": "offer", "model": {"id": 93666}},
                        ],
                    }
                ],
            },
            preserve_order=True,
        )
        self.assertEqual(1, response.count({"entity": "offer-pack"}))

    def test_delivery_price_on_offer_substitution(self):
        """When input entity is offer and the same offer is appeared in result, then it should contain correct delivery price"""
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:fzn4MX-9sZiO9MYo66AlkQ:1,o:RPaDqEFjs1I6_lfC4Ai8jA:1&rids=213"
        )
        self.assertFragmentIn(
            response,
            {
                "adult": False,
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "delivery": {"price": {"value": "25"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": "fzn4MX-9sZiO9MYo66AlkQ",
                                "delivery": {"price": {"value": "20"}},
                            },
                            {
                                "entity": "offer",
                                "wareId": "RPaDqEFjs1I6_lfC4Ai8jA",
                                "delivery": {"price": {"value": "25"}},
                            },
                        ],
                    }
                ],
            },
            preserve_order=True,
        )

    def check_regional_delimiter_with_store(self, model_prefix, fesh):
        base_request = "place=alt_same_shop&shopping-list=m:{prefix}10111:1,m:{prefix}10222:1&rids=157"
        # pack above the regional delimiter because store=True for both offers
        response = self.report.request_json(base_request.format(prefix=model_prefix))
        self.assertFragmentIn(response, {"results": [{"entity": "offer-pack", "shop": {"id": fesh}}]})
        self.assertFragmentNotIn(response, {"results": [{"entity": "regionalDelimiter"}]})

        # pack below the regional delimiter because store=False for {prefix}10333 offer
        base_request = "place=alt_same_shop&shopping-list=m:{prefix}10111:1,m:{prefix}10333:1&rids=157"
        response = self.report.request_json(base_request.format(prefix=model_prefix))
        self.assertFragmentIn(
            response, {"results": [{"entity": "regionalDelimiter"}, {"entity": "offer-pack", "shop": {"id": fesh}}]}
        )

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_regional_delimiter(self):
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:980111:1,m:980222:1,m:980333:1,m:980444:1&rids=213{delivery}{pager}"
        # delivery included
        # no pager
        response = self.report.request_json(base_request.format(delivery="&deliveryincluded=1", pager=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "price": {"value": "301", "isDeliveryIncluded": True},
                        "delivery": {"price": {"value": "1"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 980111},
                                # dh78hD_wWrdO01hxPdUcGw offer has price < price of iAT5Tha6W2gOI5pVSgj30g offer.
                                # However iAT5Tha6W2gOI5pVSgj30g is selected because delivery price is specified for it. dh78hD_wWrdO01hxPdUcGw has no delviery price
                                "wareId": "iAT5Tha6W2gOI5pVSgj30g",
                                "delivery": {"price": {"value": "1"}},
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980222},
                                "delivery": {"price": {"value": "1"}},
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980333},
                                "delivery": {"price": {"value": "1"}},
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer-alternative",
                                "id": "980444",
                            },
                        ],
                    },
                    {
                        # this pack has same offers count and cheaper than pack for shop 1003, but is is located after shop 1003 because has no delivery price
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "price": {"value": "150", "isDeliveryIncluded": False},
                        "delivery": {"price": NoKey("price")},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 980111},
                                "delivery": {
                                    "isAvailable": True,
                                    "price": NoKey("price"),
                                },  # delivery_bucket=811 allows to remove delivery price
                                "prices": {"value": "50"},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980222},
                                "delivery": {"isAvailable": False, "price": NoKey("price")},
                                "prices": {"value": "50"},
                            },
                            {
                                "entity": "offer-alternative",
                                "id": "980333",
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980444},
                                "delivery": {"isAvailable": False, "price": NoKey("price")},
                                "prices": {"value": "50"},
                            },
                        ],
                    },
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer-pack", "shop": {"id": 1009}},
                    {"entity": "offer-pack", "shop": {"id": 1008}},
                ]
            },
            preserve_order=True,
        )
        # page 1 of 2
        response = self.report.request_json(
            base_request.format(delivery="&deliveryincluded=1", pager="&numdoc=2&page=1")
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer-pack", "shop": {"id": 1003}},
                    {"entity": "offer-pack", "shop": {"id": 1001}},
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))
        self.assertFragmentNotIn(response, {"results": [{"entity": "regionalDelimiter"}]})
        # page 2 of 2
        response = self.report.request_json(
            base_request.format(delivery="&deliveryincluded=1", pager="&numdoc=2&page=2")
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer-pack", "shop": {"id": 1009}},
                    {"entity": "offer-pack", "shop": {"id": 1008}},
                ]
            },
        )
        self.assertEqual(2, response.count({"entity": "offer-pack"}))
        # no delivery
        response = self.report.request_json(base_request.format(delivery="", pager=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "price": {"value": "150", "isDeliveryIncluded": False},
                        "delivery": {"price": NoKey("price")},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 980111},
                                "delivery": {
                                    "isAvailable": True,
                                    "price": NoKey("price"),
                                },  # delivery_bucket=811 allows to remove delivery price
                                "prices": {"value": "50"},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980222},
                                "delivery": {"isAvailable": False, "price": NoKey("price")},
                                "prices": {"value": "50"},
                            },
                            {
                                "entity": "offer-alternative",
                                "id": "980333",
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980444},
                                "delivery": {"isAvailable": False, "price": NoKey("price")},
                                "prices": {"value": "50"},
                            },
                        ],
                    },
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "price": {"value": "300", "isDeliveryIncluded": False},
                        "delivery": {"price": {"value": "1"}},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 980111},
                                "wareId": "iAT5Tha6W2gOI5pVSgj30g",
                                "delivery": {"price": {"value": "1"}},
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980222},
                                "delivery": {"price": {"value": "1"}},
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 980333},
                                "delivery": {"price": {"value": "1"}},
                                "prices": {"value": "100"},
                            },
                            {
                                "entity": "offer-alternative",
                                "id": "980444",
                            },
                        ],
                    },
                ]
            },
            preserve_order=True,
        )

        # Output contains only regional packs. RegionalDelimiter should be outputed as first element
        base_request = (
            "pp=155&place=alt_same_shop&shopping-list=m:910111:1,m:910222:1,m:910333:1,m:910444:1&rids=213{pager}"
        )
        response = self.report.request_json(base_request.format(pager=""))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "regionalDelimiter"},
                    {"entity": "offer-pack", "shop": {"id": 1009}},
                    {"entity": "offer-pack", "shop": {"id": 1008}},
                ]
            },
        )
        response = self.report.request_json(base_request.format(pager="&numdoc=1&page=1"))
        self.assertFragmentIn(
            response, {"results": [{"entity": "regionalDelimiter"}, {"entity": "offer-pack", "shop": {"id": 1009}}]}
        )
        response = self.report.request_json(base_request.format(pager="&numdoc=1&page=2"))
        self.assertFragmentIn(response, {"results": [{"entity": "offer-pack", "shop": {"id": 1008}}]})
        self.assertFragmentNotIn(response, {"results": [{"entity": "regionalDelimiter"}]})

        # no regional delimiter in 'calc-pack' mode
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=o:7tT8xKYaTpeaRO68xa2-HA:1,o:Gb3PysNvUYz-3xo2TbAnkw:1,o:07jMPmyWkwYs394aI0-PzA:1,o:8GaN0stIZ5AJ4Oe_0SK3qQ:1&rids=213&calc-pack=1"
        )
        self.assertFragmentIn(response, {"results": [{"entity": "offer-pack", "shop": {"id": 1008}}]})
        self.assertFragmentNotIn(response, {"results": [{"entity": "regionalDelimiter"}]})

        # local store has influence to regional delimiter
        # store
        self.check_regional_delimiter_with_store("10", 1011)
        # mixed
        self.check_regional_delimiter_with_store("12", 1012)

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_click_price(self):
        _ = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:81111:1,m:81222:1&rids=213&show-urls=showPhone,offercard"
        )
        # Бесплатные клики из-за MARKETOUT-22186
        self.show_log_tskv.expect(hyper_id=81222, url_type=UrlType.SHOW_PHONE, click_price=0)
        self.show_log_tskv.expect(hyper_id=81222, url_type=UrlType.OFFERCARD, click_price=0)

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_no_delivery_options_for_pickup_pack(self):
        response = self.report.request_json("pp=155&place=alt_same_shop&shopping-list=m:484222:1,m:484444:1&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1004},
                        "delivery": {"isAvailable": False, "hasPickup": True, "price": NoKey("price")},
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response, {"results": [{"entity": "offer-pack", "shop": {"id": 1004}, "delivery": {"options"}}]}
        )

    def check_shop_counters(self, response, outlets, stores, pickupStores, postomatStores, bookNowStores):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {
                            "id": 1108,
                            "outletsCount": outlets,
                            "storesCount": stores,
                            "pickupStoresCount": pickupStores,
                            "postomatStoresCount": postomatStores,
                            "bookNowStoresCount": bookNowStores,
                        },
                    }
                ]
            },
        )

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_pickup_and_store(self):
        # common pickupOptions
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:9880111:1,m:9880222:1,m:9880333:1,m:9880444:1&rids=213&pickup-options={pickup}"
        response = self.report.request_json(base_request.format(pickup="raw"))
        pickupOptionsNoPostomat = [
            {
                "outlet": {"id": "809", "type": "store"},
            },
            {
                "outlet": {"id": "812", "type": "pickup"},
            },
            {
                "outlet": {"id": "810", "type": "pickup"},
            },
            {
                "outlet": {"id": "811", "type": "store"},
            },
            {
                "outlet": {"id": "888", "type": "pickup"},
            },
        ]
        pickupOptionsWithPostomat = copy.copy(pickupOptionsNoPostomat)
        pickupOptionsWithPostomat.append({"isMarketBranded": True, "outlet": {"id": "102", "type": "pickup"}})
        commonPickupOptions = [
            {
                "outlet": {"id": "812", "type": "pickup"},
            },
            {
                "outlet": {"id": "810", "type": "pickup"},
            },
            {
                "outlet": {"id": "888", "type": "pickup"},
            },
        ]
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1108},
                        "delivery": {"hasPickup": True, "hasLocalStore": False, "pickupOptions": commonPickupOptions},
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 9880111},
                                "delivery": {
                                    "hasPickup": True,
                                    "hasLocalStore": True,
                                    "pickupOptions": pickupOptionsWithPostomat,
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 9880222},
                                "delivery": {
                                    "hasPickup": True,
                                    "hasLocalStore": True,
                                    "pickupOptions": pickupOptionsWithPostomat,
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 9880333},
                                "delivery": {
                                    "hasPickup": True,
                                    "hasLocalStore": True,
                                    "pickupOptions": pickupOptionsNoPostomat,
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 9880444},
                                "delivery": {
                                    "hasPickup": True,
                                    "hasLocalStore": False,
                                    "pickupOptions": commonPickupOptions,
                                },
                            },
                        ],
                    }
                ]
            },
        )
        # grouped pickup options
        response = self.report.request_json(base_request.format(pickup="grouped"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1108},
                        "delivery": {
                            "hasPickup": True,
                            "hasLocalStore": False,
                            "pickupOptions": [
                                {"serviceId": 99, "price": {"value": "123"}, "groupCount": 1},
                                {"serviceId": 99, "price": {"value": "789"}, "groupCount": 2},
                            ],
                        },
                    }
                ]
            },
        )
        # outlet counters
        response = self.report.request_json(base_request.format(pickup="grouped"))
        self.check_shop_counters(response, outlets=3, stores=0, pickupStores=3, postomatStores=0, bookNowStores=0)

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:9880111:1,m:9880222:1,m:9880333:1&rids=213&pickup-options=grouped"
        )
        self.check_shop_counters(response, outlets=5, stores=2, pickupStores=3, postomatStores=0, bookNowStores=1)

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:9880111:1,m:9880222:1&rids=213&pickup-options=grouped"
        )
        self.check_shop_counters(response, outlets=6, stores=2, pickupStores=4, postomatStores=1, bookNowStores=2)
        # pickup price
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:1080111:1,m:1080222:1&rids=213&deliveryincluded=1&pickup-options=raw"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "300", "rawValue": "200"},
                        "shop": {"id": 1010},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "outlet": {"id": "1808"},
                                    "price": {
                                        "value": "123"
                                    },  # pickup price is '123' because pack_price(200) <= outlet_price_to(200)
                                }
                            ]
                        },
                    }
                ]
            },
        )

        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:1080111:1,m:1080222:1,m:1080333:1&rids=213&deliveryincluded=1&pickup-options=raw"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "price": {"value": "400", "rawValue": "300"},
                        "shop": {"id": 1010},
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "outlet": {"id": "1808"},
                                    "price": {
                                        "value": "0"
                                    },  # pickup price is '0' because pack_price(300) > outlet_price_to(200)
                                }
                            ]
                        },
                    }
                ]
            },
        )

    def check_delivery_interval(
        self, response, totalPrice, price, dF, dT, price1, dF1, dT1, price2, dF2, dT2, price3, dF3, dT3
    ):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "price": {"value": totalPrice, "rawValue": "300"},
                        "delivery": {
                            "options": [{"price": {"value": price}, "isDefault": True, "dayFrom": dF, "dayTo": dT}]
                        },
                        "offers": [
                            {
                                "entity": "offer",
                                "model": {"id": 41111},
                                "delivery": {
                                    "options": [
                                        {"price": {"value": price1}, "isDefault": True, "dayFrom": dF1, "dayTo": dT1}
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 41222},
                                "delivery": {
                                    "options": [
                                        {"price": {"value": price2}, "isDefault": True, "dayFrom": dF2, "dayTo": dT2}
                                    ]
                                },
                            },
                            {
                                "entity": "offer",
                                "model": {"id": 41333},
                                "delivery": {
                                    "options": [
                                        {"price": {"value": price3}, "isDefault": True, "dayFrom": dF3, "dayTo": dT3}
                                    ]
                                },
                            },
                        ],
                    }
                ]
            },
        )

    def test_delivery_options(self):
        """Проверка мержа dayFrom, dayTo опций доставки пака в сочетании с &delivery_interval"""
        """Мержатся dayFrom и dayTo только для дефолтовых опций офферов:"""
        """    дефолтовая опция доставки пака будет содержать максимальные значения для dayFrom, dayTo среди всех дефолтовых опций доставки офферов, входящих в пак."""
        """    Если dayFrom и/или dayTo отсутствует, то считается, что соответствующий срок максимален (поэтому соответствующий срок пака будет отсутствовать). На данный момент офферы без любого срока доставки считается CPC, поэтому проверка этого случая выключена."""  # noqa
        """&delivery_interval влияет только на выбор дефолтовой опции доставки для офферов и пака"""
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:41111:1,m:41222:1,m:41333:1&rids=213&deliveryincluded=1{deliv_interval}"

        # response = self.report.request_json(base_request.format(deliv_interval=""))
        # self.check_delivery_interval(response, "316", "16", NoKey("dayFrom"), NoKey("dayTo"), "5", 5, 5, "14", NoKey("dayFrom"), NoKey("dayTo"), "16", 4, 4)

        response = self.report.request_json(base_request.format(deliv_interval="&delivery_interval=3"))
        self.check_delivery_interval(response, "320", "20", 0, 3, "10", 0, 3, "20", 0, 2, "19", 0, 3)
        """   Дополнительно проверяется, что кроме пака со сроком "до 3 дней" есть еще пак со сроком "до 1 дня" """
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1003},
                        "delivery": {
                            "options": [{"price": {"value": "25"}, "isDefault": True, "dayFrom": 1, "dayTo": 1}]
                        },
                    }
                ]
            },
        )

        """Проверяется выбор НЕдефолтовых опций доставки пака:"""
        """В пак отбираются такие НЕдефолтовые опции доставки, которые есть у всех офферов пака"""
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:41222:1,m:41555:1&rids=213&delivery_interval=5&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "price": {"value": "215", "rawValue": "200"},
                        "delivery": {
                            "options": [
                                {"price": {"value": "15"}, "isDefault": True, "dayFrom": 5, "dayTo": 5},
                                {
                                    "price": {"value": "14"},
                                    "isDefault": False,
                                    "dayFrom": NoKey("dayFrom"),
                                    "dayTo": NoKey("dayFrom"),
                                },
                            ]
                        },
                    }
                ]
            },
        )
        """    Продолжение предыдущей проверки: проверяется, что в паке будут использованы НЕдефолтовые опции доставки с serviceId=11 (общий для всех офферов)."""
        """    Опции доставки с serviceId=22 будут откинуты, т.к. они есть не у всех офферов."""
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:41333:1,m:41444:1&rids=213&deliveryincluded=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "price": {"value": "216", "rawValue": "200"},
                        "delivery": {
                            "options": [
                                {
                                    "price": {"value": "16"},
                                    "isDefault": True,
                                    "dayFrom": 4,
                                    "dayTo": 4,
                                    "serviceId": "11",
                                },
                                {
                                    "price": {"value": "19"},
                                    "isDefault": False,
                                    "dayFrom": 0,
                                    "dayTo": 3,
                                    "serviceId": "11",
                                },
                            ]
                        },
                        "offers": [
                            {"entity": "offer", "delivery": {"options": [{"isDefault": False, "serviceId": "22"}]}}
                        ],
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "price": {"value": "216", "rawValue": "200"},
                        "delivery": {"options": [{"isDefault": False, "serviceId": "22"}]},
                    }
                ]
            },
        )

    def __min_quantity_json_response_cpa(self, cpa, min=None, step=None, wareId='_qQnWXU28-IUghltMZJwNw'):
        bundleSettings = (
            {'quantityLimit': {'minimum': min, 'step': step}}
            if min is not None and step is not None
            else NoKey('bundleSettings')
        )

        return {
            'results': [
                {
                    'entity': 'offer-pack',
                    'type': 'same-shop',
                    'offers': [
                        {'entity': 'offer', 'wareId': wareId, 'bundleSettings': bundleSettings, 'cpa': cpa},
                    ],
                }
            ]
        }

    def test_min_quantity(self):
        """
        Тестирование минимального количества.
        Для предложения с wareId="_qQnWXU28-IUghltMZJwNw" установлены минимальное количество и шаг

        Проверяется с различными значениями флага show-min-quantity:
        1. Флаг отстутствует - на выдаче нет ограничений
        2. Флаг равен no - на выдаче нет ограничений
        3. Флаг равен yes - на выдаче присутствует предложение с ограничением
        4. Флаг равен cpa-to-cpc:
            4.1 Для предложения с ограничением поле cpa будет пессимизированно
            4.2 Для предложения без ограничений (wareId='09lEaAKkQll1XTgggggggg', min=1, step=1) cpa сохранится

        """

        response = self.report.request_json(
            'pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1'
        )
        self.assertFragmentIn(response, self.__min_quantity_json_response_cpa('real'))

        response = self.report.request_json(
            'pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1&show-min-quantity=no'
        )
        self.assertFragmentIn(response, self.__min_quantity_json_response_cpa('real'))

        response = self.report.request_json(
            'pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1&show-min-quantity=yes'
        )
        self.assertFragmentIn(response, self.__min_quantity_json_response_cpa('real', 5, 3))

        response = self.report.request_json(
            'pp=155&place=alt_same_shop&shopping-list=m:3100:1,o:09lEaAKkQll1XTgggggggg:1,m:1000000101:1&show-min-quantity=cpa-to-cpc'
        )
        self.assertFragmentIn(response, self.__min_quantity_json_response_cpa(NoKey('cpa'), 5, 3))
        self.assertFragmentIn(response, self.__min_quantity_json_response_cpa('real', 1, 1, '09lEaAKkQll1XTgggggggg'))

    def check_title_and_pictures(self, response, model_id, model_title, offer_id, offer_title, pictures, adult):
        self.assertFragmentIn(
            response,
            {
                "adult": adult,
                "results": [
                    {
                        "entity": "offer-pack",
                        "shop": {"id": 1001},
                        "offers": [
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "model",
                                "id": model_id,
                                "title": model_title,
                                "pictures": pictures,
                            },
                            {
                                "entity": "offer-alternative",
                                "showType": "missing",
                                "type": "offer",
                                "id": offer_id,
                                "title": offer_title,
                                "pictures": pictures,
                            },
                        ],
                    }
                ],
            },
        )

    @skip(
        'С модельным индексом в новом формате не ищется аттрибут ProtoPicInfo, PicInfo, решили врменно отключить тест'
    )
    def test_pictures(self):
        """Тестируется:"""
        """    (1) Вывод картинок для missing офферов и моделей"""
        """    (2) Сокрытие title и pictures для missing офферов и моделей если они adult и adult запрещен"""
        """    (3) Заполнение аггрегата 'adult' в случае попадания в выдачу missing adult сущностей"""
        base_request = "pp=155&place=alt_same_shop&shopping-list=m:91777:1,m:91888:1,m:92777:1,o:kGEKoOJPY-xtJjfgb5ua8g:1&rids=213{adult}"
        pictures = [{"entity": "picture", "thumbnails": []}]
        # (1), (3)
        response = self.report.request_json(base_request.format(adult="&adult=1"))
        self.check_title_and_pictures(
            response, "92777", "HYPERID-92777", "kGEKoOJPY-xtJjfgb5ua8g", "title_1", pictures, True
        )
        # (2), (3)
        response = self.report.request_json(base_request.format(adult=""))
        self.check_title_and_pictures(response, "92777", "", "kGEKoOJPY-xtJjfgb5ua8g", "", NoKey("pictures"), True)

        """Проверка для не-adult модели и оффера"""
        response = self.report.request_json(
            "pp=155&place=alt_same_shop&shopping-list=m:91777:1,m:91888:1,m:92555:1,o:otENNVzevIeeT8bsxvY91w:1&rids=213"
        )
        self.check_title_and_pictures(
            response, "92555", "HYPERID-92555", "otENNVzevIeeT8bsxvY91w", "title_2", pictures, False
        )

    def test_pickup_type(self):
        """Проверяется, что аутлеты с типом самовывоза только depot и только postomat вместе могут сформировать пак для самовывоза"""
        for shipping in ['', '&offer-shipping=pickup']:
            response = self.report.request_json(
                "pp=155&place=alt_same_shop&shopping-list=m:84777:1,m:84888:1" + shipping
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer-pack",
                            "offers": [
                                {"entity": "offer", "model": {"id": 84777}},
                                {"entity": "offer", "model": {"id": 84888}},
                            ],
                        }
                    ]
                },
            )

        """Если применяется фильтр &offer-shipping=depot или &offer-shipping=postomat, то пак не сформировывается, т.к. содержит 1 оффер"""
        for shipping in ['depot', 'postomat']:
            response = self.report.request_json(
                "pp=155&place=alt_same_shop&shopping-list=m:84777:1,m:84888:1&offer-shipping=" + shipping
            )
            self.assertFragmentIn(response, {"total": 0})

    @classmethod
    def prepare_delivery_calc_for_cpa_only(cls):
        cls.index.shops += [
            Shop(fesh=1598901, priority_region=213, cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.outlets += [
            Outlet(
                fesh=1598901,
                point_id=159890101,
                region=1,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(price=200, day_from=30, day_to=60, work_in_holiday=True),
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=822,
                fesh=1598901,
                regional_options=[
                    RegionalDelivery(
                        rid=1,
                        options=[
                            DeliveryOption(price=500, day_from=30, day_to=60),
                        ],
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                cpa=Offer.CPA_REAL, fesh=1598901, waremd5='33333333333333gggggggg', delivery_buckets=[822], pickup=False
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                fesh=1598901,
                waremd5='44444444444444gggggggg',
                has_delivery_options=False,
                pickup=True,
            ),
        ]

    @skip(
        'Тест не перведен на pickup бакеты, поэтому падает. Скипнуть его можно, т.к. place=alt_same_shop больше не используется.'
    )
    def test_delivery_calc_for_cpa_only(self):
        """Что тестируем: сроки доставки (в т.ч. большие) отображаются для cpa-only магазинов в alt_same_shop
        Задаем запросы за офферами с доставкой и самовывозом, проверяем, что на выдаче есть опции доставки
        """
        response = self.report.request_json(
            'place=alt_same_shop&shopping-list=o:33333333333333gggggggg:1&calc-pack=1&rids=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer-pack",
                "delivery": {
                    "options": [
                        {
                            "price": {
                                "currency": "RUR",
                                "value": "500",
                            },
                            "dayFrom": 30,
                            "dayTo": 60,
                        }
                    ]
                },
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=alt_same_shop&shopping-list=o:44444444444444gggggggg:1&calc-pack=1&rids=1'
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer-pack",
                "delivery": {
                    "pickupOptions": [
                        {
                            "outlet": {"id": "159890101", "type": "pickup", "purpose": ["pickup"]},
                            "price": {"currency": "RUR", "value": "200"},
                        }
                    ]
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
