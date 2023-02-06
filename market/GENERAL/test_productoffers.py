#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Autostrategy,
    AutostrategyType,
    AutostrategyWithDatasourceId,
    BlueOffer,
    ClickType,
    CpaCategory,
    CpaCategoryType,
    CreditGlobalRestrictions,
    CreditPlan,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    DynamicSkuOffer,
    ExchangeRate,
    GLParam,
    GLType,
    GLValue,
    HybridAuctionParam,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MinBidsCategory,
    MnPlace,
    Model,
    NavCategory,
    NewShopRating,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Picture,
    PictureMbo,
    PictureParam,
    RegionalClicks,
    RegionalDelivery,
    Shop,
    Tax,
    VCluster,
    Vendor,
    VendorLogo,
    VirtualModel,
)
from core.types.picture import thumbnails_config
from core.types.fast_mappings import MappedOfferInfo
from core.testcase import TestCase, main
from core.matcher import Wildcard, Absent, NotEmpty, NoKey, Contains, Capture
from core.cpc import Cpc


TOTAL_OFFERS_COUNT = 8
TOTAL_OFFERS_COUNT_FOR_PP6 = min(TOTAL_OFFERS_COUNT, 6)
THRESHOLDED_OFFERS_COUNT_FOR_PP6 = 3
FEE_MULTIPLIER = 10000.0


GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(
    min_price=2000, max_price=300000, category_blacklist=[654456, 321123], category_whitelist=[456654]
)

CREDIT_PLANS = [
    CreditPlan(
        plan_id='AD51BF786AA86B36BA57B8002FB4B474',
        bank="Сбербанк",
        term=12,
        rate=12.3,
        initial_payment_percent=0,
        min_price=3500,
        max_price=30000,
        category_blacklist=[123456, 654321],
        category_whitelist=[],
    ),
    CreditPlan(
        plan_id='C0AE65435E1D9065A64F1335B51C54AB',
        bank="Альфа-банк",
        term=6,
        rate=10.5,
        initial_payment_percent=0,
        min_price=2000,
        category_blacklist=[123321],
    ),
    CreditPlan(
        plan_id='0E966DEBAA73ABD8379FA316F8326B8D',
        bank="Райффайзен банк",
        term=24,
        rate=13,
        initial_payment_percent=0,
        max_price=40000,
    ),
]

CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True) for plan in CREDIT_PLANS]


def create_randx(raw_randx):
    return raw_randx * 2


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        # TODO: MARKETOUT-47769 убрать фместе с флагом
        cls.settings.default_search_experiment_flags += ['market_hide_long_delivery_offers=0']
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.index.fixed_index_generation = '19700101_0300'

        cls.settings.is_archive_new_format = True
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        cls.index.currencies += [
            Currency(
                name=Currency.UE,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.0333333),
                ],
            )
        ]

        cls.index.navtree += [NavCategory(nid=100, hid=10)]

        cls.index.hypertree += [
            HyperCategory(hid=200, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=300, output_type=HyperCategoryType.GURU),
            HyperCategory(hid=400, output_type=HyperCategoryType.GURU),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=200, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=300, regions=[213], cpa_type=CpaCategoryType.CPA_WITH_CPC_PESSIMIZATION),
            CpaCategory(hid=400, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213),
            Shop(fesh=1002, priority_region=213),
            Shop(fesh=1003, priority_region=213),
            Shop(fesh=1004, priority_region=213),
            Shop(fesh=1005, priority_region=213),
            Shop(fesh=1006, priority_region=213),
            Shop(fesh=1007, priority_region=213),
            Shop(fesh=1008, priority_region=213),
            Shop(fesh=1009, priority_region=213),
            Shop(fesh=1010, priority_region=213),
            Shop(fesh=1011, priority_region=213),
            Shop(fesh=1012, priority_region=213),
            Shop(fesh=1013, priority_region=213),
            Shop(fesh=1014, priority_region=213),
            Shop(fesh=1015, priority_region=213),
            Shop(fesh=2001, priority_region=213),
            Shop(fesh=2002, priority_region=213),
            Shop(fesh=3000, priority_region=213),
            Shop(fesh=3002, priority_region=2, main_fesh=3000),
            Shop(fesh=3038, priority_region=38, main_fesh=3000),
            Shop(fesh=4000, priority_region=213),
            Shop(fesh=627953, priority_region=213),
        ]

        cls.index.hypertree += [HyperCategory(hid=5555, visual=True)]

        cls.index.vclusters += [VCluster(hid=5555, vclusterid=1000000101), VCluster(hid=5555, vclusterid=1000000102)]

        cls.index.gltypes += [
            GLType(param_id=202, hid=5500, gltype=GLType.ENUM, cluster_filter=True),
            GLType(param_id=203, hid=5500, gltype=GLType.BOOL, cluster_filter=True),
            GLType(param_id=204, hid=5500, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(param_id=205, hid=5501, gltype=GLType.ENUM),
            GLType(param_id=206, hid=5501, gltype=GLType.BOOL),
            GLType(param_id=207, hid=5501, gltype=GLType.NUMERIC),
            GLType(param_id=208, hid=5555, gltype=GLType.ENUM, cluster_filter=True),
            GLType(param_id=209, hid=5555, gltype=GLType.BOOL, cluster_filter=True),
            GLType(param_id=210, hid=5555, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(param_id=211, hid=5556, gltype=GLType.ENUM, cluster_filter=True),
        ]

        cls.index.offers += [
            Offer(title='ABCD', fesh=1, vclusterid=1000000007, hid=10555),
            Offer(title='ABCD', fesh=1, hyperid=1000000008, hid=10556),
            Offer(hyperid=3001, fesh=3000, price=400, delivery_options=[DeliveryOption(price=100)]),
            Offer(hyperid=3001, fesh=3002, price=500, delivery_options=[DeliveryOption(price=100)]),
            Offer(hyperid=3001, fesh=4000, price=410, delivery_options=[DeliveryOption(price=30)]),
            Offer(hyperid=5001, hid=5500),
            Offer(
                hyperid=5002,
                hid=5500,
                glparams=[
                    GLParam(param_id=202, value=2),
                    GLParam(param_id=203, value=0),
                    GLParam(param_id=204, value=500),
                ],
            ),
            Offer(
                hyperid=5002,
                hid=5500,
                glparams=[
                    GLParam(param_id=202, value=3),
                    GLParam(param_id=203, value=1),
                    GLParam(param_id=204, value=100),
                ],
            ),
            Offer(hyperid=5002, hid=5500),
            Offer(hyperid=5004, hid=5501),
            Offer(vclusterid=1000000101, hid=5555),
            Offer(
                vclusterid=1000000102,
                hid=5555,
                glparams=[
                    GLParam(param_id=208, value=2),
                    GLParam(param_id=209, value=0),
                    GLParam(param_id=210, value=500),
                ],
            ),
            Offer(vclusterid=1000000102, hid=5555),
            Offer(hyperid=1101, fesh=1001, price=1),
            Offer(hyperid=1101, fesh=1002, price=1000),
            Offer(hyperid=1101, fesh=1003, price=2000),
            Offer(hyperid=1101, fesh=1004, price=3000),
            Offer(hyperid=1101, fesh=1005, price=4000),
            Offer(hyperid=1101, fesh=1006, price=10000),
            # default
            Offer(
                hyperid=2101,
                fesh=2001,
                price=9000,
                hid=200,
                randx=create_randx(501),
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
            ),
            # fast delivery
            Offer(
                hyperid=2101,
                fesh=2001,
                price=10000,
                hid=200,
                randx=create_randx(502),
                delivery_options=[DeliveryOption(price=100, day_from=1, day_to=1, order_before=23)],
            ),
            # with discount
            Offer(
                hyperid=2101,
                fesh=2001,
                price=9500,
                hid=200,
                discount=5,
                randx=create_randx(503),
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=5, order_before=23)],
            ),
            # vendor recommended
            Offer(
                hyperid=2101,
                fesh=2001,
                price=11001,
                hid=200,
                is_recommended=True,
                randx=create_randx(700),
                ts=4,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=5, order_before=23)],
            ),
            # vendor recommended with discount
            Offer(
                hyperid=2101,
                fesh=2001,
                price=11002,
                hid=200,
                discount=5,
                is_recommended=True,
                randx=create_randx(100),
                ts=5,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=5, order_before=23)],
            ),
            # prepay enabled
            Offer(
                hyperid=2101,
                fesh=2002,
                price=11003,
                hid=200,
                randx=create_randx(100),
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=5, order_before=23)],
            ),
            # not prepay enabled because not cpa
            Offer(
                hyperid=2101,
                fesh=2002,
                price=11003,
                hid=200,
                randx=create_randx(100),
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=5, order_before=23)],
            ),
            # free delivery
            Offer(
                hyperid=2101,
                fesh=2001,
                price=11004,
                hid=200,
                randx=create_randx(503),
                delivery_options=[DeliveryOption(price=0, day_from=3, day_to=5, order_before=23)],
            ),
            # not cpa offer to check intents fallback to offers output
            Offer(hyperid=3101, fesh=1001, price=10000, hid=300),
            # cpa and not cpa offers to check that cpc is not shown. 1 CPA offer
            Offer(hyperid=3201, fesh=1001, price=10000, hid=300),
            Offer(hyperid=3201, fesh=1002, price=10000, hid=300),
            # cpa and not cpa offers to check that cpc is not shown. 2 CPA offers, with intents
            Offer(hyperid=3301, fesh=1001, price=5000, hid=300, bid=100, title="not cpa, bid=100"),
            Offer(hyperid=3301, fesh=1002, price=6000, hid=300, bid=200, title="not cpa, bid=200"),
            Offer(hyperid=3301, fesh=1003, price=7000, hid=300, title="cpa, fee=300"),
            Offer(hyperid=3301, fesh=1004, price=8000, hid=300, title="cpa, fee=400"),
            Offer(hyperid=3301, fesh=1005, price=9000, hid=300, is_recommended=True, title="cpa, fee=500"),
            # cpa and not cpa offers to check random
            Offer(hyperid=3302, fesh=1001, price=1000, hid=300),
            Offer(hyperid=3302, fesh=1002, price=2000, hid=300),
            Offer(hyperid=3302, fesh=1003, price=3000, hid=300),
            Offer(hyperid=3302, fesh=1004, price=4000, hid=300),
            Offer(hyperid=3302, fesh=1005, price=5000, hid=300),
            Offer(hyperid=3302, fesh=1006, price=6000, hid=300),
            Offer(hyperid=3302, fesh=1007, price=7000, hid=300),
            Offer(hyperid=3302, fesh=1008, price=8000, hid=300),
            Offer(hyperid=3302, fesh=1009, price=9000, hid=300),
            Offer(hyperid=3302, fesh=1010, price=10000, hid=300),
            Offer(hyperid=3302, fesh=1011, price=11000, hid=300),
            Offer(hyperid=3302, fesh=1012, price=12000, hid=300),
            Offer(hyperid=3302, fesh=1013, price=13000, hid=300),
            Offer(hyperid=3302, fesh=1014, price=14000, hid=300),
            Offer(hyperid=3302, fesh=1015, price=15000, hid=300),
            # cpc only offers, check auction for default offer
            Offer(hyperid=3401, fesh=1001, price=10000, hid=300, bid=130),
            Offer(hyperid=3401, fesh=1002, price=10000, hid=300, bid=140),
            Offer(hyperid=3401, fesh=1003, price=10000, hid=300, bid=150),
            Offer(hyperid=3401, fesh=1004, price=10000, hid=300, bid=160),
            # cpc and cpa category
            Offer(hyperid=3501, fesh=1001, price=11000, hid=400, bid=30, title="cpa offer"),
            Offer(hyperid=3501, fesh=1002, price=12000, hid=400, bid=40, title="not cpa offer"),
            # rotation in benefits independent on price
            Offer(
                hyperid=3701,
                fesh=1001,
                price=1000,
                hid=200,
                is_recommended=True,
                ts=1,
                randx=create_randx(10),
                title="offer 1",
            ),
            Offer(
                hyperid=3701,
                fesh=1002,
                price=2000,
                hid=200,
                is_recommended=True,
                ts=2,
                randx=create_randx(12),
                title="offer 2",
            ),
            Offer(
                hyperid=3701,
                fesh=1003,
                price=3000,
                hid=200,
                is_recommended=True,
                ts=3,
                randx=create_randx(13),
                title="offer 3",
            ),
            # Default offer is CPA in not CPA ranking MARKETOUT-9882
            Offer(hyperid=4101, fesh=1001, price=3000, hid=400, bid=400),
            Offer(hyperid=4101, fesh=1002, price=3000, hid=400, bid=300),
            Offer(hyperid=4101, fesh=1003, price=3000, hid=400, bid=200),
            Offer(hyperid=4101, fesh=1004, price=3000, hid=400, bid=100),
            # Default offer is CPA in CPA with CPC pessimization category
            Offer(
                hyperid=4201,
                fesh=1001,
                price=3000,
                hid=200,
                bid=400,
                ts=420101,
                randx=create_randx(1),
                title="hyper 4201 offer 1",
            ),
            Offer(
                hyperid=4201,
                fesh=1002,
                price=3000,
                hid=200,
                bid=300,
                ts=420102,
                randx=create_randx(2),
                title="hyper 4201 offer 2",
            ),
            Offer(
                hyperid=4201,
                fesh=1003,
                price=3000,
                hid=200,
                bid=200,
                ts=420103,
                randx=create_randx(3),
                title="hyper 4201 offer 3",
            ),
            Offer(
                hyperid=4201,
                fesh=1004,
                price=3000,
                hid=200,
                bid=100,
                ts=420104,
                randx=create_randx(4),
                title="hyper 4201 offer 4",
            ),
            # Contents API and Adviser pp for CPA with CPC pessimization category
            Offer(hyperid=4301, fesh=1001, price=3000, hid=200, bid=400),
            Offer(hyperid=4301, fesh=1001, price=3000, hid=200, bid=300),
            Offer(hyperid=4301, fesh=1001, price=3000, hid=200, bid=200),
            Offer(hyperid=4301, fesh=1001, price=3000, hid=200, bid=100),
            # CPC-only model in CPA with CPC pessimization category
            Offer(hyperid=4401, fesh=1001, hid=200),
            Offer(hyperid=4401, fesh=1002, hid=200),
            # CPC-only model in CPA and CPC category
            Offer(hyperid=4501, fesh=1001, hid=400),
            Offer(hyperid=4501, fesh=1002, hid=400),
        ]

        cls.index.models += [
            Model(hyperid=3601, hid=400, title='cpc and cpa model'),
            Model(hyperid=1005000, hid=20, title='test nid filter'),
        ]

        # MARKETOUT-9014 - models
        cls.index.models += [Model(hyperid=1201, title='Nokian Nordman')]

        cls.index.offers += [
            Offer(hyperid=1201, fesh=2201, title='Nokian Nordman R15'),
            Offer(hyperid=1201, fesh=2201, title='Nokian Nordman R16'),
            Offer(hyperid=1201, fesh=2202, title='Nokian Nordman R15 in shop 2202'),
            Offer(hyperid=1201, fesh=2202, title='Nokian Nordman R16 in shop 2202'),
            Offer(hyperid=1201, fesh=2203, title='Nokian Nordman R16 in shop 2203'),
            Offer(hyperid=1005000, fesh=2201, title='Nid filter test offer', hid=20),
        ]

        # MARKETOUT-9014 - vclusters
        cls.index.gltypes += [
            GLType(
                param_id=201,
                hid=13,
                gltype=GLType.ENUM,
                values=[1, 2, 3, 4],
                unit_name='Size',
                cluster_filter=True,
                model_filter_index=1,
            )
        ]

        cls.index.vclusters += [VCluster(vclusterid=1200000001, hid=13, title='Shtuka')]

        cls.index.offers += [
            Offer(vclusterid=1200000001, glparams=[GLParam(param_id=201, value=1)], hid=13, price=100, fesh=2202),
            Offer(vclusterid=1200000001, glparams=[GLParam(param_id=201, value=2)], hid=13, price=200, fesh=2202),
            Offer(vclusterid=1200000001, glparams=[GLParam(param_id=201, value=3)], hid=13, price=300, fesh=2202),
            Offer(vclusterid=1200000001, glparams=[GLParam(param_id=201, value=4)], hid=13, price=400, fesh=2202),
        ]

        # Для тестирования груповой модели
        cls.index.models += [
            Model(hid=101, hyperid=1001, group_hyperid=1000),
            Model(hid=101, hyperid=1002, group_hyperid=1000),
        ]
        cls.index.offers += [
            Offer(hid=101, hyperid=1000),
            Offer(hid=101, hyperid=1001),
            Offer(hid=101, hyperid=1002),
        ]

        # MARKETOUT-9254: сортировка по времени доставки
        cls.index.shops += [
            Shop(fesh=2011, priority_region=213),
            Shop(fesh=2012, priority_region=213),
            Shop(fesh=2013, priority_region=213),
            Shop(fesh=2014, priority_region=213),
            Shop(fesh=2015, priority_region=213),
            Shop(fesh=2016, priority_region=213),
            Shop(fesh=2017, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=3.0)),
            Shop(fesh=2018, priority_region=213),
            Shop(fesh=2019, priority_region=213, new_shop_rating=NewShopRating(new_rating_total=5.0)),
            Shop(fesh=2020, priority_region=2, pickup_buckets=[5001]),
        ]

        cls.index.outlets += [Outlet(fesh=2020, region=213, point_type=Outlet.FOR_PICKUP, point_id=10001)]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=2020,
                carriers=[99],
                options=[PickupOption(outlet_id=10001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        cls.index.offers += [
            Offer(
                hyperid=2103,
                fesh=2011,
                delivery_options=[DeliveryOption(price=200, day_from=1, day_to=1, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2012,
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=3, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2013,
                delivery_options=[DeliveryOption(price=50, day_from=3, day_to=4, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2014,
                delivery_options=[DeliveryOption(price=10, day_from=3, day_to=5, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2015,
                delivery_options=[DeliveryOption(price=20, day_from=4, day_to=6, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2016,
                delivery_options=[DeliveryOption(price=200, day_from=0, day_to=0, order_before=23)],
            ),
            # TODO: MARKETOUT-47769 вернуть как было. Удалить значения
            Offer(
                hyperid=2103,
                fesh=2017,
                price=1000,
                delivery_options=[DeliveryOption(price=100, order_before=23, day_from=32, day_to=32)],
            ),
            Offer(
                hyperid=2103,
                fesh=2018,
                delivery_options=[DeliveryOption(price=50, day_from=31, day_to=31, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2019,
                price=100,
                delivery_options=[DeliveryOption(price=10, day_from=32, day_to=32, order_before=23)],
            ),
            Offer(
                hyperid=2103,
                fesh=2020,
                delivery_options=[DeliveryOption(price=200, day_from=1, day_to=1, order_before=23)],
            ),
        ]

        # MARKETOUT-9633 price sort monetization
        cls.index.offers += [
            Offer(hyperid=4001, fesh=1401, price=700, bid=10),
            Offer(hyperid=4001, fesh=1402, price=800, bid=100),
            Offer(hyperid=4001, fesh=1403, price=900, bid=20),
        ]

        cls.index.offers += [
            Offer(hyperid=4002, manufacturer_warranty=True, title="with_manufacturer_warranty"),
            Offer(hyperid=4002, manufacturer_warranty=False, title="without_manufacturer_warranty"),
        ]

        cls.index.offers += [Offer(hyperid=4003, title='with_bad_picture', picture='pic1||pic2|50x50:50x50')]

        # MARKETOUT-10890
        cls.index.offers += [
            Offer(fesh=1001, hyperid=17001, bid=900, price=50000),
            Offer(fesh=1001, hyperid=17001, bid=100, price=10000),
            Offer(fesh=1002, hyperid=17001, bid=700, price=10000),
            Offer(fesh=1003, hyperid=17001, bid=600, price=10000),
            Offer(fesh=1004, hyperid=17001, bid=500, price=10000),
        ]

        # MARKETOUT-10127
        cls.index.offers += [Offer(hyperid=17002, hid=5556)]

        cls.index.credit_plans_container.global_restrictions = GLOBAL_RESTRICTIONS
        cls.index.credit_plans_container.credit_plans = CREDIT_PLANS

    @classmethod
    def prepare_preorder_dates_independence_from_rgb(cls):
        shop_id = 10000
        shop_feed_id = 1
        cls.index.shops += [Shop(fesh=shop_id, tax_system=Tax.OSN, blue=Shop.BLUE_REAL, cpa=Shop.CPA_REAL)]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                hid=11567,
                waremd5='MarketSku4-IiLVm1goleg',
                blue_offers=[
                    BlueOffer(feedid=shop_feed_id, offerid='preorder_dates_independence_from_rgb', is_preorder=True)
                ],
            ),
        ]

    def test_prepare_preorder_date_independence_from_rgb(self):
        """
        Проверяем, что даты по предзаказу получаем независимо от параметра rgb
        """
        request_without_rgb = 'place=productoffers' '&hyperid=1' '&rids=213' '&show-preorder=1' '&market-sku=1'
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=10000, sku='preorder_dates_independence_from_rgb')]

        dayToCapture = Capture()

        expected_fragment = {
            "options": [
                {
                    "dayFrom": NotEmpty(),
                    "dayTo": NotEmpty(capture=dayToCapture),
                }
            ]
        }

        response = self.report.request_json(request_without_rgb + '&rgb=blue')
        self.assertFragmentIn(response, expected_fragment)
        self.show_log.expect(courier_day_to=int(dayToCapture.value))
        self.show_log.expect(courier_day_to=int(dayToCapture.value))

        response = self.report.request_json(request_without_rgb + '&rgb=green')
        self.assertFragmentIn(response, expected_fragment)
        self.show_log.expect(courier_day_to=int(dayToCapture.value))
        self.show_log.expect(courier_day_to=int(dayToCapture.value))

        response = self.report.request_json(request_without_rgb)
        self.assertFragmentIn(response, expected_fragment)
        self.show_log.expect(courier_day_to=int(dayToCapture.value))
        self.show_log.expect(courier_day_to=int(dayToCapture.value))

    @classmethod
    def prepare_test_output_format(cls):
        cls.index.vendors += [
            Vendor(
                vendor_id=501,
                name='samsung',
                website='www.samsung.com',
                webpage_recommended_shops='http://www.samsung.com/ru/brandshops/',
                description='VendorDescription',
                logos=[VendorLogo(url='//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png')],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=301, vendor_id=501, title='samsung galaxy s8'),
        ]

    def test_credit_plans(self):
        response = self.report.request_json('place=productoffers&hyperid=301')
        self.assertFragmentIn(response, {'creditOptions': CREDIT_PLANS_FRAGMENT}, allow_different_len=False)

    def test_output_format(self):
        '''
        tests output fields in place.
        '''
        response = self.report.request_json('place=productoffers&hyperid=301')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'vendor': {
                    'name': 'samsung',
                    'website': 'www.samsung.com',
                    "description": "VendorDescription",
                    "logo": {
                        "entity": "picture",
                        "url": "//mdata.yandex.net/i?path=b0726220734_img_id5949902134120952009.png",
                    },
                    "webpageRecommendedShops": "http://www.samsung.com/ru/brandshops/",
                },
                "delivery": {"deliveryPartnerTypes": ["SHOP"]},
            },
        )

    def test_mcprice_no_filter(self):
        # without mcprice filter
        response = self.report.request_json('place=productoffers&hyperid=1101')
        self.assertEqual(6, response.count({"entity": "offer", "model": {"id": 1101}}))

    def test_mcprice_to0(self):
        response = self.report.request_json('place=productoffers&hyperid=1101&mcpriceto=0.01')
        self.assertEqual(0, response.count({"entity": "offer", "model": {"id": 1101}}))

    def test_mcprice_to1(self):
        response = self.report.request_json('place=productoffers&hyperid=1101&mcpriceto=1000')
        self.assertEqual(2, response.count({"entity": "offer", "model": {"id": 1101}}))
        self.assertFragmentIn(response, {"entity": "offer", "prices": {"value": "1000"}})
        self.assertFragmentNotIn(response, {"entity": "offer", "prices": {"value": "2000"}})

    def test_mcprice_from(self):
        response = self.report.request_json('place=productoffers&hyperid=1101&mcpricefrom=1500')
        self.assertEqual(4, response.count({"entity": "offer", "model": {"id": 1101}}))
        self.assertFragmentIn(response, {"entity": "offer", "prices": {"value": "2000"}})
        self.assertFragmentNotIn(response, {"entity": "offer", "prices": {"value": "1000"}})

    def test_mcprice_from_to(self):
        response = self.report.request_json('place=productoffers&hyperid=1101&mcpricefrom=1500&mcpriceto=2500')
        self.assertEqual(1, response.count({"entity": "offer", "model": {"id": 1101}}))
        self.assertFragmentIn(response, {"entity": "offer", "prices": {"value": "2000"}})
        self.assertFragmentNotIn(response, {"entity": "offer", "prices": {"value": "1000"}})
        self.assertFragmentNotIn(response, {"entity": "offer", "prices": {"value": "3000"}})

    def test_url_show_empty_text(self):
        # without mcprice filter
        response = self.report.request_json('place=productoffers&hyperid=1101&show-urls=offercard')
        self.assertFragmentNotIn(response, {"U_DIRECT_OFFER_CARD_URL": Contains("&text=")})

        response = self.report.request_json(
            'place=productoffers&hyperid=1101&show-urls=offercard&rearr-factors=url_show_empty_text=1'
        )
        self.assertFragmentIn(response, {"U_DIRECT_OFFER_CARD_URL": Contains("&text=")})

    def test_bundle_count_for_model(self):
        # У модели 1201 есть два оффера в магазине 2201, 2 в магазине 2202, и 1 в магазине 2203
        response = self.report.request_json('place=productoffers&hyperid=1201&grhow=shop&offers-set=list,default')
        shop2count = {2201: 2, 2202: 2, 2203: 1}
        for shop, count in shop2count.items():
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "model": {"id": 1201},
                    "shop": {"id": shop},
                    "bundleCount": count,
                    "bundled": {"modelId": 1201, "count": count},
                },
            )

        # В плейсе productoffers ищется топ офферов, и в конце добавляется ДО.
        # При небольшом количестве офферов будет дублирование. Но в этом тесте проверяем, что bundleCount есть и в ДО
        default_offer = next(
            obj for obj in response.root['search']['results'] if obj.get('benefit', {}).get('type') == 'default'
        )
        shop = default_offer["shop"]["id"]
        self.assertEqual(default_offer["bundleCount"], shop2count[shop])
        self.assertEqual(default_offer["bundled"]["count"], shop2count[shop])

    def test_bundle_count_for_vcluster(self):
        response = self.report.request_json('place=productoffers&hyperid=1200000001&grhow=shop')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "model": {"id": 1200000001},
                "bundleCount": 4,
                "bundled": {"modelId": 1200000001, "count": 4},
            },
        )

    def test_groupby(self):
        response = self.report.request_json('place=productoffers&hyperid=1200000001&grhow=shop')
        self.assertFragmentIn(response, {"groupBy": "shop"})
        response = self.report.request_json('place=productoffers&hyperid=1200000001&grhow=offer')
        self.assertFragmentIn(response, {"groupBy": "offer"})
        # group by offer by default:
        response = self.report.request_json('place=productoffers&hyperid=1200000001')
        self.assertFragmentIn(response, {"groupBy": "offer"})

    def test_group_model_expansion(self):
        response = self.report.request_json('place=productoffers&hyperid=1000')
        # "total": 5 ?
        self.assertEqual(3, response.count({"entity": "offer"}))
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1000}})
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1001}})
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1002}})

    def test_group_model_expansion_not_a_group_model(self):
        response = self.report.request_json('place=productoffers&hyperid=1001')
        self.assertEqual(1, response.count({"entity": "offer"}))
        self.assertFragmentIn(response, {"entity": "offer", "model": {"id": 1001}})

    def test_list_result_sorting_mixed_split_price_sort_monetization_aprice(self):
        # cpa and cpc offers in result
        response = self.report.request_json(
            'place=productoffers&hyperid=3301&rids=213&hid=300&offers-set=list&show-urls=encrypted,cpa&pp=21&how=aprice'
        )
        self.assertFragmentIn(
            response,
            [
                {"prices": {"value": "5000"}},
                {"prices": {"value": "6000"}},
                {"prices": {"value": "7000"}},
                {"prices": {"value": "8000"}},
                {"prices": {"value": "9000"}},
            ],
            preserve_order=True,
        )

    def test_list_result_sorting_mixed_split_price_sort_monetization_dprice(self):
        # cpa and cpc offers in result
        response = self.report.request_json(
            'place=productoffers&hyperid=3301&rids=213&hid=300&offers-set=list&show-urls=encrypted,cpa&pp=21&how=dprice'
        )
        self.assertFragmentIn(
            response,
            [
                {"prices": {"value": "9000"}},
                {"prices": {"value": "8000"}},
                {"prices": {"value": "7000"}},
                {"prices": {"value": "6000"}},
                {"prices": {"value": "5000"}},
            ],
            preserve_order=True,
        )

    def test_list_result_not_random_sorting_desktop_first_10(self):
        # cpa and cpc offers in result
        response = self.report.request_json(
            'place=productoffers&hyperid=3302&rids=213&hid=300&offers-set=list&show-urls=encrypted,cpa&numdoc=100'
        )
        self.assertFragmentIn(
            response,
            [
                {"prices": {"value": "10000"}},
                {"prices": {"value": "7000"}},
                {"prices": {"value": "12000"}},
                {"prices": {"value": "11000"}},
                {"prices": {"value": "6000"}},
                {"prices": {"value": "14000"}},
                {"prices": {"value": "13000"}},
                {"prices": {"value": "15000"}},
                {"prices": {"value": "8000"}},
                {"prices": {"value": "9000"}},
            ],
            preserve_order=False,
        )

    def test_list_result_random_sorting_uid_1_desktop_first_10(self):
        # cpa and cpc offers in result
        response = self.report.request_json(
            'place=productoffers&hyperid=3302&rids=213&hid=300&offers-set=list&show-urls=encrypted,cpa&numdoc=100'
            '&rearr-factors=market_ranged_offers_by_random=1'
            '&yandexuid=1'
        )
        self.assertFragmentIn(
            response,
            [
                {"prices": {"value": "10000"}},
                {"prices": {"value": "7000"}},
                {"prices": {"value": "12000"}},
                {"prices": {"value": "11000"}},
                {"prices": {"value": "6000"}},
                {"prices": {"value": "14000"}},
                {"prices": {"value": "13000"}},
                {"prices": {"value": "15000"}},
                {"prices": {"value": "8000"}},
                {"prices": {"value": "9000"}},
            ],
            preserve_order=False,
        )

    def test_list_result_random_sorting_uid_2_desktop_first_10(self):
        # cpa and cpc offers in result
        response = self.report.request_json(
            'place=productoffers&hyperid=3302&rids=213&hid=300&offers-set=list&show-urls=encrypted,cpa&numdoc=100'
            '&rearr-factors=market_ranged_offers_by_random=1'
            '&yandexuid=2'
        )
        self.assertFragmentIn(
            response,
            [
                {"prices": {"value": "13000"}},
                {"prices": {"value": "7000"}},
                {"prices": {"value": "10000"}},
                {"prices": {"value": "9000"}},
                {"prices": {"value": "11000"}},
                {"prices": {"value": "6000"}},
                {"prices": {"value": "8000"}},
                {"prices": {"value": "14000"}},
                {"prices": {"value": "12000"}},
                {"prices": {"value": "15000"}},
            ],
            preserve_order=False,
        )

    def test_list_result_random_sorting_uid_2_desktop_page_2(self):
        # cpa and cpc offers in result
        response = self.report.request_json(
            'place=productoffers&hyperid=3302&rids=213&hid=300&offers-set=list&show-urls=encrypted,cpa&page=2'
            '&rearr-factors=market_ranged_offers_by_random=1&numdoc=100'
            '&yandexuid=2'
        )
        self.assertFragmentIn(
            response,
            [
                {"prices": {"value": "3000"}},
                {"prices": {"value": "2000"}},
                {"prices": {"value": "5000"}},
                {"prices": {"value": "4000"}},
                {"prices": {"value": "1000"}},
            ],
            preserve_order=False,
        )

    def test_no_search_results(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213&nosearchresults=1')
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
            },
        )
        self.assertFragmentIn(
            response,
            {
                "total": TOTAL_OFFERS_COUNT,
            },
        )
        self.assertTrue(len(response.root['sorts']) > 0)

    # MARKETOUT-9254
    def test_how_delivery_interval(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&&rids=213&how=delivery_interval')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "shop": {"id": 2016}},
                    {"entity": "offer", "shop": {"id": 2011}},
                    {"entity": "offer", "shop": {"id": 2012}},
                    {"entity": "offer", "shop": {"id": 2013}},
                    {"entity": "offer", "shop": {"id": 2014}},
                    {"entity": "offer", "shop": {"id": 2015}},
                    {"entity": "offer", "shop": {"id": 2018}},
                    {"entity": "offer", "shop": {"id": 2019}},
                    {"entity": "offer", "shop": {"id": 2017}},
                    {"entity": "offer", "shop": {"id": 2020}},
                ]
            },
            preserve_order=True,
        )

    # MARKETOUT-9258 + MARKETOUT-9409 (without discount)
    def test_sorts_in_productoffers_no_sort_without_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213')
        self.assertFragmentIn(
            response, {"sorts": [{"options": [{"id": "aprice"}, {"id": "dprice"}]}, {"options": [{"id": "rorp"}]}]}
        )

    def test_sorts_in_productoffers_sort_by_aprice_without_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice", "isActive": True}, {"id": "dprice"}]},
                    {"options": [{"id": "rorp"}]},
                ]
            },
        )

    def test_sorts_in_productoffers_sort_by_dprice_without_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213&how=dprice')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice"}, {"id": "dprice", "isActive": True}]},
                    {"options": [{"id": "rorp"}]},
                ]
            },
        )

    def test_sorts_in_productoffers_sort_by_rorp_without_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213&how=rorp')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice"}, {"id": "dprice"}]},
                    {"options": [{"id": "rorp", "isActive": True}]},
                ]
            },
        )

    # MARKETOUT-9409 with discount
    def test_sorts_in_productoffers_no_sort_with_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice"}, {"id": "dprice"}]},
                    {"options": [{"id": "rorp"}]},
                    {
                        "options": [
                            {
                                "id": "discount_p",
                            }
                        ]
                    },
                ]
            },
        )

    def test_sorts_in_productoffers_sort_by_aprice_with_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213&how=aprice')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice", "isActive": True}, {"id": "dprice"}]},
                    {"options": [{"id": "rorp"}]},
                    {
                        "options": [
                            {
                                "id": "discount_p",
                            }
                        ]
                    },
                ]
            },
        )

    def test_sorts_in_productoffers_sort_by_dprice_with_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213&how=dprice')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice"}, {"id": "dprice", "isActive": True}]},
                    {"options": [{"id": "rorp"}]},
                    {
                        "options": [
                            {
                                "id": "discount_p",
                            }
                        ]
                    },
                ]
            },
        )

    def test_sorts_in_productoffers_sort_by_rorp_with_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213&how=rorp')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice"}, {"id": "dprice"}]},
                    {"options": [{"id": "rorp", "isActive": True}]},
                    {
                        "options": [
                            {
                                "id": "discount_p",
                            }
                        ]
                    },
                ]
            },
        )

    def test_sorts_in_productoffers_sort_by_discount_p_with_discount(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213&how=discount_p')
        self.assertFragmentIn(
            response,
            {
                "sorts": [
                    {"options": [{"id": "aprice"}, {"id": "dprice"}]},
                    {
                        "options": [
                            {
                                "id": "rorp",
                            }
                        ]
                    },
                    {"options": [{"id": "discount_p", "isActive": True}]},
                ]
            },
        )

    # MARKETOUT-9338
    def test_isdeliveryincluded_not_specified(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": False}})
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "model": {"id": 2103}, "prices": {"isDeliveryIncluded": False}}]}
        )

    def test_isdeliveryincluded_specified_and_true(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213&deliveryincluded=1')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": True}})
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "model": {"id": 2103}, "prices": {"isDeliveryIncluded": True}}]}
        )

    def test_isdeliveryincluded_specified_and_false(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213&deliveryincluded=0')
        self.assertFragmentIn(response, {"search": {"isDeliveryIncluded": False}})
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "model": {"id": 2103}, "prices": {"isDeliveryIncluded": False}}]}
        )

    # MARKETOUT-9391: убираем фильтры с одним значением
    # MARKETOUT-9514: НЕ убираем фильтры с одним значением
    # MARKETOUT-9515: возвращаем убирание фильтров enum с одним значением + убирание фильтров boolean, где в случае установки found будет равно 0
    @classmethod
    def prepare_single_valued_enum_filters(cls):
        cls.index.models += [
            Model(
                hyperid=103,
                title="Grabli",
                vendor_id=100501,
                glparams=[GLParam(param_id=201, vendor=True, value=100501)],
            )
        ]

        cls.index.offers += [
            Offer(hyperid=103, fesh=1001, price=200, glparams=[GLParam(param_id=201, vendor=True, value=100501)])
        ]

        cls.index.offers += [
            Offer(hyperid=104, fesh=1001, price=300, glparams=[GLParam(param_id=201, vendor=True, value=100501)])
        ]

        cls.index.offers += [
            Offer(hyperid=104, fesh=1002, price=300, glparams=[GLParam(param_id=201, vendor=True, value=100501)])
        ]

        cls.index.offers += [Offer(hyperid=200, fesh=1002, price=300, discount=50)]

    # MARKETOUT-9515
    def test_single_value_enum_filters_are_not_here(self):
        response = self.report.request_json('place=productoffers&hyperid=103&rids=213')
        self.assertFragmentNotIn(
            response,
            {
                "id": "home_region",
                "type": "enum",
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "id": "fesh",
                "type": "enum",
            },
        )
        self.assertFragmentNotIn(
            response, {"id": "show-book-now-only", "type": "boolean", "values": [{"found": 0, "value": "1"}]}
        )

    # MARKETOUT-9515
    def test_single_value_enum_filters_inside_search_results_are_still_here(self):
        response = self.report.request_json('place=productoffers&hyperid=103&rids=213')
        self.assertFragmentIn(
            response, {"search": {"results": [{"entity": "offer", "filters": [{"id": "201", "type": "enum"}]}]}}
        )

    # MARKETOUT-9391
    # MARKETOUT-9515
    def test_two_value_enum_are_here(self):
        response = self.report.request_json('place=productoffers&hyperid=104&rids=213')
        self.assertFragmentIn(response, {"id": "fesh", "type": "enum", "values": [{"id": "1001"}, {"id": "1002"}]})

    def test_vendor_recommended_filter(self):
        response = self.report.request_json('place=productoffers&hyperid=2101&rids=213')
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": "filter-vendor-recommended"}]})

    def test_invalid_glfilter_log_message(self):
        self.report.request_json('place=productoffers&hyperid=1101&glfilter=123:456')
        self.error_log.expect('Error in glfilters syntax:').once()

    # MARKETOUT-9652
    @classmethod
    def prepare_free_delivery(cls):
        cls.index.offers += [
            Offer(
                hyperid=105,
                fesh=1001,
                price=300,
                delivery_options=[DeliveryOption(price=0, day_from=1, day_to=2, order_before=23)],
            )
        ]

        cls.index.offers += [
            Offer(
                hyperid=105,
                fesh=1002,
                price=300,
                delivery_options=[DeliveryOption(price=111, day_from=1, day_to=2, order_before=23)],
            )
        ]

    def test_free_delivery_filter_in_product_offers(self):
        response = self.report.request_json('place=productoffers&hyperid=105&rids=213')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "results": [
                        {
                            "entity": "offer",
                            "model": {"id": 105},
                            "delivery": {"options": [{"price": {"value": "111"}}]},
                        },
                        {"entity": "offer", "model": {"id": 105}, "delivery": {"options": [{"price": {"value": "0"}}]}},
                    ],
                }
            },
        )

    def test_free_delivery_filter_in_product_offers_filter_on(self):
        response = self.report.request_json('place=productoffers&hyperid=105&rids=213&free_delivery=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "offer", "model": {"id": 105}, "delivery": {"options": [{"price": {"value": "0"}}]}},
                    ],
                }
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=105&rids=213&free-delivery=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "offer", "model": {"id": 105}, "delivery": {"options": [{"price": {"value": "0"}}]}},
                    ],
                }
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=105&rids=213&free_delivery=0&free-delivery=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "offer", "model": {"id": 105}, "delivery": {"options": [{"price": {"value": "0"}}]}},
                    ],
                }
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=105&rids=213&free_delivery=1&free-delivery=0')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "offer", "model": {"id": 105}, "delivery": {"options": [{"price": {"value": "0"}}]}},
                    ],
                }
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=105&rids=213&free_delivery=1&free-delivery=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"entity": "offer", "model": {"id": 105}, "delivery": {"options": [{"price": {"value": "0"}}]}},
                    ],
                }
            },
        )

        self.assertFragmentNotIn(
            response, {"entity": "offer", "model": {"id": 105}, "delivery": {"price": {"value": "111"}}}
        )

    # MARKETOUT-9421
    @classmethod
    def prepare_for_test_cpa_filter(cls):
        cls.index.offers += [
            Offer(hyperid=501, fesh=1001, price=200),
            Offer(hyperid=502, fesh=1001, price=200),
            Offer(hyperid=502, fesh=1002, price=200),
            Offer(hyperid=503, fesh=1001, price=200),
        ]

    def test_regional_delimiter(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&rids=213')
        self.assertFragmentIn(response, {"entity": "regionalDelimiter"})

        response = self.report.request_json('place=productoffers&hyperid=2103')
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})

    def test_no_redirect(self):
        rspNoRedir = {"redirect": {}}

        response = self.report.request_json('place=productoffers&hyperid=1000000007&hid=10555')
        self.assertFragmentIn(response, {"search": {"total": 1, "cpaCount": 0}})
        self.assertFragmentNotIn(response, rspNoRedir)

        rspNull = {"search": {"total": 0, "cpaCount": 0}}

        response = self.report.request_json('place=productoffers&hyperid=1000000007&hid=10555&cpa=real')
        self.assertFragmentIn(response, rspNull)
        self.assertFragmentNotIn(response, rspNoRedir)

        response = self.report.request_json('place=productoffers&hyperid=1000000008&hid=10556&cpa=real')
        self.assertFragmentIn(response, rspNull)
        self.assertFragmentNotIn(response, rspNoRedir)

        response = self.report.request_json('place=productoffers&hyperid=1000000008&hid=10556&cpa=real&cvredirect=2')
        self.assertFragmentIn(response, rspNull)
        self.assertFragmentNotIn(response, rspNoRedir)

    def test_no_fesh_filter(self):
        response = self.report.request_json('place=productoffers&hyperid=1101')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 6},
                "filters": [
                    {
                        "id": "fesh",
                        "values": [
                            {"value": "SHOP-1001"},
                            {"value": "SHOP-1002"},
                            {"value": "SHOP-1003"},
                            {"value": "SHOP-1004"},
                            {"value": "SHOP-1005"},
                            {"value": "SHOP-1006"},
                        ],
                    }
                ],
            },
        )

    def test_total_renderable(self):
        """Проверяется, что общее количество для показа = total"""
        request = 'place=productoffers&hyperid=1101'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"search": {"total": 6}})
        self.assertEqual(6, response.count({"entity": "offer"}))
        response = self.report.request_json(request + '&numdoc=3')
        self.assertFragmentIn(response, {"search": {"total": 6}})
        self.assertEqual(3, response.count({"entity": "offer"}))
        self.access_log.expect(total_renderable='6').times(2)

    def test_fesh_filter(self):
        response = self.report.request_json('place=productoffers&hyperid=1101&fesh=1001')
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 1},
                "filters": [
                    {
                        "id": "fesh",
                        "values": [
                            {"value": "SHOP-1001"},
                            {"value": "SHOP-1002"},
                            {"value": "SHOP-1003"},
                            {"value": "SHOP-1004"},
                            {"value": "SHOP-1005"},
                            {"value": "SHOP-1006"},
                        ],
                    }
                ],
            },
        )

    def test_negative_fesh_filter(self):
        """При указании отрицательного fesh офферы магазина должны отфильтровываться

        https://st.yandex-team.ru/MARKETOUT-12054
        """

        def shop_result(shop_id):
            return {"search": {"results": [{"entity": "offer", "shop": {"id": shop_id}}]}}

        # Делаем запрос без фильтра по магазину
        response = self.report.request_json('place=productoffers&hyperid=1101')
        # Проверяем, что офферы магазинов с id=1001 и id=1002 есть
        self.assertFragmentIn(response, shop_result(1001))
        self.assertFragmentIn(response, shop_result(1002))

        # Делаем запрос с отрицательным фильтром по магазину с id=1002
        response = self.report.request_json('place=productoffers&hyperid=1101&fesh=-1002')
        # Проверяем, что оффер магазина с id=1001 есть, а с id=1002 нет
        self.assertFragmentIn(response, shop_result(1001))
        self.assertFragmentNotIn(response, shop_result(1002))

        # Делаем запрос с отрицательным фильтром по магазинам с id=1001 и id=1002
        response = self.report.request_json('place=productoffers&hyperid=1101&fesh=-1001&fesh=-1002')
        # Проверяем, что офферов обоих магазинов нет
        self.assertFragmentNotIn(response, shop_result(1001))
        self.assertFragmentNotIn(response, shop_result(1002))

    def test_max_price_with_delivery_included(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=3001&rids=213&mcpriceto=420&deliveryincluded=1'
        )
        self.assertFragmentIn(response, {"total": 0})

    def test_max_price_with_no_delivery_included(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=3001&rids=213&mcpriceto=420&deliveryincluded=0'
        )
        self.assertFragmentIn(response, {"total": 2})

    def test_price_sorting(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=3001&rids=213&mcpriceto=550&how=rorp&deliveryincluded=0'
        )
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"prices": {"value": "400"}}, {"prices": {"value": "410"}}]}},
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=3001&rids=213&mcpriceto=550&how=rorp&deliveryincluded=1'
        )
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"prices": {"value": "440"}}, {"prices": {"value": "500"}}]}},
            preserve_order=True,
        )

    def core_no_empty_filter_5002(self, total, place_name):
        response = self.report.request_json('place={pn}&hyperid=5002&hid=5500'.format(pn=place_name))
        self.assertFragmentIn(response, {"search": {"total": total}})
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "202", "values": [{"initialFound": 1}]},
                    {"id": "203", "values": [{"initialFound": 2}]},
                    {"id": "204", "values": [{"id": "500~500"}] if place_name == 'productoffers' else [{'max': '500'}]},
                ]
            },
        )

    def core_no_empty_filter(self, place_name):
        response = self.report.request_json('place={pn}&hyperid=5001&hid=5500'.format(pn=place_name))
        self.assertFragmentIn(response, {"search": {"total": 1}})
        self.assertFragmentNotIn(response, {"filters": [{"id": "202"}, {"id": "203"}, {"id": "204"}]})

        self.core_no_empty_filter_5002(total=3, place_name=place_name)

    def core_no_empty_filter2(self, total1, total2, place_name):
        response = self.report.request_json('place={pn}&hyperid=5004&hid=5501'.format(pn=place_name))
        self.assertFragmentIn(response, {"search": {"total": total1}})
        self.assertFragmentNotIn(response, {"filters": [{"id": "205"}, {"id": "207"}]})

        self.core_no_empty_filter_5002(total=total2, place_name=place_name)

    def core_no_empty_filter3(self, search_results, place_name):
        response = self.report.request_xml('place={pn}&hyperid=1000000101&hid=5555'.format(pn=place_name))
        sr1 = search_results
        sr1 += ' total="1">'
        self.assertFragmentIn(
            response,
            '''
            {sr}
            </search_results>
        '''.format(
                sr=sr1
            ),
        )
        self.assertFragmentNotIn(
            response,
            '''
            <gl_filters></gl_filters>
        ''',
        )

        sr1 = search_results
        sr1 += ' total="2">'
        response = self.report.request_xml('place={pn}&hyperid=1000000102&hid=5555'.format(pn=place_name))
        self.assertFragmentIn(
            response,
            '''
            {sr}
                <gl_filters>
                    <filter id="208" name="GLPARAM-208" noffers="1" position="1" sub_type="" type="enum"><value found="1" id="2" initial-found="1" value="VALUE-2"/></filter>
                    <filter id="209" name="GLPARAM-209" noffers="1" position="1" sub_type="" type="boolean"><value found="2" id="0" initial-found="2" value="0"/><value found="0" id="1" initial-found="0" value="1"/></filter>  # noqa
                    <filter id="210" name="GLPARAM-210" noffers="1" position="1" precision="0" sub_type="" type="number"><value id="found" initial-max="500" initial-min="500" max="500" min="500"/></filter>  # noqa
                </gl_filters>
            </search_results>
        '''.format(
                sr=sr1
            ),
        )

    def test_no_empty_filter(self):
        self.core_no_empty_filter('productoffers')
        self.core_no_empty_filter2(total1=1, total2=3, place_name='prime')

    def test_price_monetization(self):
        '''
        После перехода на новый расчёт мин бидов (MARKETOUT-14481), на сортировках на карточуе модели,
        в качестве стоимости клика используется мин бид
        '''

        self.report.request_json("place=productoffers&hyperid=4001&show-urls=external")
        self.report.request_json("place=productoffers&hyperid=4001&show-urls=external&how=aprice")
        self.report.request_json("place=productoffers&hyperid=4001&show-urls=external&how=dprice")
        self.report.request_json("place=productoffers&hyperid=4001&show-urls=external&how=rorp")

        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=21, min_bid=1).times(1)
        self.click_log.expect(ClickType.EXTERNAL, cb=20, cp=11, min_bid=1).times(1)
        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=1, min_bid=1).times(3)
        self.click_log.expect(ClickType.EXTERNAL, cb=20, cp=1, min_bid=1).times(3)
        self.click_log.expect(ClickType.EXTERNAL, cb=10, cp=1, min_bid=1).times(4)

    def test_no_onstock_filter(self):
        response = self.report.request_json('place=productoffers&hyperid=3001&rids=213')
        self.assertFragmentIn(response, {"search": {"total": 2}})

    def test_missing_pp(self):
        response = self.report.request_json('place=productoffers&hyperid=1101&ip=127.0.0.1', add_defaults=False)
        self.error_log.expect('Some client has not set PP value').once()
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI", "message": "No pp parameter is set"}})

        self.assertFragmentNotIn(response, {"filters": [{"id": 'onstock'}]})
        self.error_log.expect(code=3043)

    def test_manufacturer_warranty(self):
        response = self.report.request_json('place=productoffers&hyperid=4002')

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "with_manufacturer_warranty"},
                        "manufacturer": {"entity": "manufacturer", "warranty": True},
                    },
                    {
                        "titles": {"raw": "without_manufacturer_warranty"},
                        "manufacturer": {"entity": "manufacturer", "warranty": False},
                    },
                ]
            },
        )

        response = self.report.request_json('place=productoffers&hyperid=4002&manufacturer_warranty=1')
        self.assertFragmentNotIn(response, {"results": [{"titles": {"raw": "without_manufacturer_warranty"}}]})

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "with_manufacturer_warranty"},
                        "manufacturer": {"entity": "manufacturer", "warranty": True},
                    }
                ]
            },
        )

    def test_delivery_interval_filter(self):
        response = self.report.request_json('place=productoffers&hyperid=2103&delivery_interval=3&rids=213')
        self.assertFragmentIn(response, {"entity": "offer", "shop": {"id": 2011}})
        self.assertFragmentIn(response, {"entity": "offer", "shop": {"id": 2012}})
        response = self.report.request_json('place=productoffers&hyperid=2103&delivery_interval=1&rids=213')
        self.assertFragmentIn(response, {"entity": "offer", "shop": {"id": 2011}})
        self.assertFragmentNotIn(response, {"entity": "offer", "shop": {"id": 2012}})

    def test_bad_picture(self):
        response = self.report.request_json('place=productoffers&hyperid=4003')
        self.assertFragmentIn(response, {'entity': 'offer', 'titles': {'raw': 'with_bad_picture'}})

    @classmethod
    def prepare_home_region(cls):
        cls.index.offers += [
            Offer(hyperid=4701, fesh=2301, hid=200),
            Offer(hyperid=4701, fesh=2302, hid=200),
            Offer(hyperid=4701, fesh=2303, hid=200),
            Offer(hyperid=4701, fesh=2304, hid=200),
        ]
        cls.index.shops += [
            Shop(fesh=2301, priority_region=213, home_region=10),
            Shop(fesh=2302, priority_region=213, home_region=10),
            Shop(fesh=2303, priority_region=213, home_region=11),
            Shop(fesh=2304, priority_region=213, home_region=12),
        ]

    def test_home_region(self):
        '''Проверяем работу фильтра home_region_filter на примере из трех стран
        Проверяем, что в позапросной статистике по фильтру всегда указываются
        все три страны с правильным количеством офферов, т.к. прочих
        пользовательских фильтров нет
        '''
        response = self.report.request_json('place=productoffers&hyperid=4701&hid=200&rids=213')
        self.assertFragmentIn(response, {'total': 4})

        response = self.report.request_json('place=productoffers&hyperid=4701&hid=200&rids=213&home_region_filter=10')
        self.assertFragmentIn(response, {'total': 2})
        self.assertFragmentIn(
            response,
            [
                {'shop': {'id': 2301}},
                {'shop': {'id': 2302}},
            ],
        )

        response = self.report.request_json('place=productoffers&hyperid=4701&hid=200&rids=213&home_region_filter=11')
        self.assertFragmentIn(response, {'total': 1})
        self.assertFragmentIn(response, {'shop': {'id': 2303}})

        response = self.report.request_json('place=productoffers&hyperid=4701&hid=200&rids=213&home_region_filter=12')
        self.assertFragmentIn(response, {'total': 1})
        self.assertFragmentIn(response, {'shop': {'id': 2304}})

        response = self.report.request_json('place=productoffers&hyperid=4701&hid=200&rids=213&home_region_filter=213')
        self.assertFragmentIn(response, {'total': 0})

        response = self.report.request_json(
            'place=productoffers&hyperid=4701&hid=200&rids=213&home_region_filter=10,11'
        )
        self.assertFragmentIn(response, {'total': 3})
        self.assertFragmentIn(
            response,
            [
                {'shop': {'id': 2301}},
                {'shop': {'id': 2302}},
                {'shop': {'id': 2303}},
            ],
        )

    @classmethod
    def prepare_no_home_region(cls):
        cls.index.offers += [
            Offer(hyperid=4702, fesh=2305, hid=201),
            Offer(hyperid=4702, fesh=2306, hid=201),
        ]
        cls.index.shops += [
            Shop(fesh=2305, priority_region=213, home_region=10),
            Shop(fesh=2306, priority_region=213, home_region=10),
        ]

    def test_no_home_region(self):
        '''Делаем запросы за офферами модели 4702 с разными
        вариантами home_region_filter
        Ожидаем, что на выдаче офферы соответствуют заданному
        фильтру по стране продавца, а позапросной статистики
        по home_region нет, т.к. есть только один вариант
        значения (10)
        '''
        # Запрос без home_region_filter
        response = self.report.request_json('place=productoffers&hyperid=4702&hid=201&rids=213')
        self.assertFragmentIn(response, {'total': 2})
        self.assertFragmentIn(response, {'shop': {'id': 2305}})
        self.assertFragmentIn(response, {'shop': {'id': 2306}})
        self.assertFragmentNotIn(response, {'id': 'home_region'})

        # Запрос с правильным home_region_filter
        response = self.report.request_json('place=productoffers&hyperid=4702&hid=201&rids=213&home_region_filter=10')
        self.assertFragmentIn(response, {'total': 2})
        self.assertFragmentIn(response, {'shop': {'id': 2305}})
        self.assertFragmentIn(response, {'shop': {'id': 2306}})
        self.assertFragmentNotIn(response, {'id': 'home_region'})

        # Запрос с неправильным home_region_filter
        response = self.report.request_json('place=productoffers&hyperid=4702&hid=201&rids=213&home_region_filter=213')
        self.assertFragmentIn(response, {'total': 0})
        self.assertFragmentNotIn(response, {'id': 'home_region'})

    def test_hyperid_type_in_access_log(self):
        _ = self.report.request_json('place=productoffers&hyperid=1101')
        self.access_log.expect(product_type='MODEL')

        _ = self.report.request_json('place=productoffers&hyperid=1000000007')
        self.access_log.expect(product_type='VCLUSTER')

        _ = self.report.request_json('place=prime&hyperid=1101')
        self.access_log.expect(product_type='NONE')

    @classmethod
    def prepare_qrfrom_filters(cls):
        """Создадим несколько магазинов с разными рейтингами"""
        cls.index.shops += [
            Shop(fesh=7000),
            Shop(
                fesh=7001,
                new_shop_rating=NewShopRating(
                    new_rating=1.2,
                    rec_and_nonrec_pub_count=234,
                ),
            ),
            Shop(
                fesh=7002,
                new_shop_rating=NewShopRating(
                    new_rating_total=2.0,
                    rec_and_nonrec_pub_count=345,
                ),
            ),
            Shop(
                fesh=7003,
                new_shop_rating=NewShopRating(
                    new_rating_total=3.4,
                    rec_and_nonrec_pub_count=666,
                ),
            ),
        ]

        """Модель которую будем запрашивать"""
        cls.index.models += [
            Model(hyperid=4000, title='Model'),
        ]

        """У этой модели есть офферы"""
        cls.index.offers += [
            Offer(fesh=7000, title="Offer 0", hyperid=4000),
            Offer(fesh=7001, title="Offer 1", hyperid=4000),
            Offer(fesh=7002, title="Offer 2", hyperid=4000),
            Offer(fesh=7003, title="Offer 3", hyperid=4000),
        ]

    def test_qfrom_filters_no_limit(self, aux=''):
        """Проверим, что без лимитов в выдаче будут все магазины"""

        response = self.report.request_json('place=productoffers&hyperid=4000' + aux)

        self.assertFragmentIn(
            response,
            [
                {
                    "shop": {
                        "qualityRating": 0,
                        "overallGradesCount": 0,
                        "ratingToShow": 0,
                        "ratingType": 6,
                    }
                },
                {
                    "shop": {
                        "qualityRating": 1,
                        "overallGradesCount": 234,
                        "ratingToShow": 1.2,
                        "ratingType": 3,
                    }
                },
                {
                    "shop": {
                        "qualityRating": 2,
                        "overallGradesCount": 345,
                        "ratingToShow": 2,
                        "ratingType": 2,
                    }
                },
                {
                    "shop": {
                        "qualityRating": 3,
                        "overallGradesCount": 666,
                        "ratingToShow": 3.4,
                        "ratingType": 2,
                    }
                },
            ],
            allow_different_len=False,
        )

    def test_qfrom_filters_0(self, aux=''):
        """&qrfrom=0 - тоже самое, что нет лимитов"""
        self.test_qfrom_filters_no_limit("&qrfrom=0" + aux)

    def test_qfrom_filters_1(self, aux=''):
        """
        qrfrom=1 означает, что нужно показать все магазины с рейтингов >=1
        """
        response = self.report.request_json('place=productoffers&hyperid=4000&qrfrom=1' + aux)

        self.assertFragmentIn(
            response,
            [
                {
                    "shop": {
                        "qualityRating": 1,
                    }
                },
                {
                    "shop": {
                        "qualityRating": 2,
                    }
                },
                {
                    "shop": {
                        "qualityRating": 3,
                    }
                },
            ],
            allow_different_len=False,
        )

    def test_qfrom_filters_2(self, aux=''):
        """
        Здесь и далее увеличиваем лимит и проверяем что магазины
        с рейтингом >= указанного числа есть в выдаче,
        а < указанного числа - нет
        """
        response = self.report.request_json('place=productoffers&hyperid=4000&qrfrom=2' + aux)

        self.assertFragmentIn(
            response,
            [
                {
                    "shop": {
                        "qualityRating": 2,
                    }
                },
                {
                    "shop": {
                        "qualityRating": 3,
                    }
                },
            ],
            allow_different_len=False,
        )

    def test_qfrom_filters_3(self, aux=''):
        response = self.report.request_json('place=productoffers&hyperid=4000&qrfrom=3' + aux)

        self.assertFragmentIn(
            response,
            [
                {
                    "shop": {
                        "qualityRating": 3,
                    }
                }
            ],
            allow_different_len=False,
        )

    def test_qfrom_filters_4(self, aux=''):
        response = self.report.request_json('place=productoffers&hyperid=4000&qrfrom=4' + aux)

        self.assertFragmentIn(response, {'results': []})

    def test_qfrom_filters_no_limit__gr_shop(self):
        return self.test_qfrom_filters_no_limit("&grhow=shop")

    def test_qfrom_filters_0__gr_shop(self):
        return self.test_qfrom_filters_0("&grhow=shop")

    def test_qfrom_filters_1__gr_shop(self):
        return self.test_qfrom_filters_1("&grhow=shop")

    def test_qfrom_filters_2__gr_shop(self):
        return self.test_qfrom_filters_2("&grhow=shop")

    def test_qfrom_filters_3__gr_shop(self, aux=''):
        return self.test_qfrom_filters_3("&grhow=shop")

    def test_qfrom_filters_4__gr_shop(self, aux=''):
        return self.test_qfrom_filters_4("&grhow=shop")

    def test_price_sort_monetization_click_prices(self):
        """
        See MARKETOUT-10890
        Check that price sort monetization does not take more money
        than specified for the offer.
        Checks case when there are two offers for the same shop:
        one with low bids, low price and with high bids and high price.
        In the case offer with hich bids is shown at pp=6 and with low at pp=13

        После введения нового расчёта мин бидов (MARKETOUT-14481) на сортировках на карточке модели
        в качестве цены клика всегда будет использоваться мин бид, а тикет MARKETOUT-15644 включает использование min fee
        """
        _ = self.report.request_json(
            'place=productoffers&hyperid=17001&rids=213&show-urls=encrypted,cpa&how=aprice&pp=13&grhow=shop'
        )
        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=13, shop_id=1001)

    @classmethod
    def prepare_shop_created_at(cls):
        '''
        Подготовка данных для теста даты добавления магазина.
        https://st.yandex-team.ru/MARKETOUT-10484
        '''
        cls.index.models += [
            Model(title="ball", hid=2221, hyperid=222309),
            Model(title="dall", hid=2222, hyperid=222310),
        ]

        cls.index.shops += [
            Shop(fesh=222500, priority_region=213, created_at='2016-12-01'),
            Shop(fesh=222501, priority_region=214),
        ]

        cls.index.offers += [
            Offer(title="red ball with white line", hyperid=222309, hid=2221, fesh=222500),
            Offer(title="green dall", hyperid=222310, hid=2222, fesh=222501),
        ]

    def test_shop_created_at(self):
        '''
        Проверка поля даты добавления магазина.
        https://st.yandex-team.ru/MARKETOUT-10484
        Для магазина id=222500 было добавлено поле created_at, которое пробросилось на вывод как поле "createdAt" в изначальном формате ("YYYY-MM-DD" для нас это просто строка)
        '''
        response = self.report.request_json('place=productoffers&hyperid=222309&rids=213')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "shop": {
                            "id": 222500,
                            "createdAt": "2016-12-01",
                        },
                    }
                ]
            },
        )

    def test_shop_created_at_missed(self):
        '''
        Проверка отсутствия поля даты добавления магазина.
        https://st.yandex-team.ru/MARKETOUT-10484
        Для магазина id=222501 этого поля нет и на выдаче created_at будет отсутствовать.
        '''
        response = self.report.request_json('place=productoffers&hyperid=222310&rids=214')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "shop": {
                            "id": 222501,
                            "createdAt": NoKey("createdAt"),
                        },
                    }
                ]
            },
        )

    # place=productoffers&hyperid=301
    def test__no_postomat_in_offer_shipping(self):
        '''
        Проверка того, что в фильтре "offer-shipping" отсутсвует значение "postomat"
        MARKETOUT-11205
        '''
        response = self.report.request_json('place=productoffers&hyperid=301')
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "offer-shipping",
                        "values": [
                            {"value": "postomat"},
                        ],
                    }
                ]
            },
        )

    @classmethod
    def prepare_discount_doesnt_depend_on_delivery_cost(cls):
        """
        Создаем оффер со скидкой, который доставляется в 2 региона, в один из регионов достовка платная
        """
        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[225], name='shop 10'),
        ]

        cls.index.models += [
            Model(hyperid=3010, hid=400, title='model'),
        ]

        cls.index.offers += [
            Offer(
                fesh=10,
                hyperid=3010,
                title='has delivery and discount',
                price=1000,
                discount=50,
                delivery_options=[DeliveryOption(price=500, day_from=1, day_to=2, order_before=6)],
            ),
        ]

    def test_discount_doesnt_depend_on_delivery_cost(self):
        """
        Проверяем, что стоимость доставки не влияет на размер скидки оффера
        """
        response = self.report.request_json('place=productoffers&hyperid=3010&rids=213&deliveryincluded=1')
        self.assertFragmentIn(
            response,
            {
                "prices": {
                    "value": "1500",
                    "rawValue": "1000",
                    "discount": {
                        "oldMin": "2500",
                        "percent": 50,
                    },
                },
            },
        )

    def test_no_required_params_filter(self):
        '''
        Make sure we don't filter out offers by "required" (cluster_filter) params.
        See MARKETOUT-10127
        '''
        response = self.report.request_json('place=productoffers&hyperid=17002')
        self.assertFragmentIn(response, {"results": [NotEmpty()]}, allow_different_len=False)

    @classmethod
    def prepare_gl_filters_price_min(cls):
        cls.index.offers += [
            Offer(
                hyperid=6002,
                hid=5500,
                glparams=[GLParam(param_id=202, value=2), GLParam(param_id=203, value=0)],
                price=1000,
            ),
            Offer(
                hyperid=6002,
                hid=5500,
                glparams=[GLParam(param_id=202, value=1), GLParam(param_id=203, value=1)],
                price=2500,
            ),
            Offer(
                hyperid=6002,
                hid=5500,
                glparams=[GLParam(param_id=202, value=2), GLParam(param_id=203, value=1)],
                price=500,
            ),
        ]

    def test_gl_filters_price_min(self):
        """
        проверяем, что в атрибуте priceMin установлена минимальная цена для указанного параметра
        для логических и перечисляемых значений.
        Для остальных - не требовалось
        """

        response = self.report.request_json('place=productoffers&hyperid=6002&hid=5500')
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "202",
                        "values": [
                            {"id": "1", "priceMin": {"value": "2500"}},
                            {"id": "2", "priceMin": {"value": "500"}},
                        ],
                    },
                    {
                        "id": "203",
                        "values": [
                            {"id": "0", "priceMin": {"value": "1000"}},
                            {"id": "1", "priceMin": {"value": "500"}},
                        ],
                    },
                ]
            },
        )

    @classmethod
    def prepare_hybrid_auction(cls):
        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(
                category=90401,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.0016,
                market_conv_for_cpa_msk=0.035,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
            ),
            HybridAuctionParam(
                category=90563,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.0016,
                market_conv_for_cpa_msk=0.035,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
                avg_items_count=10.0,
            ),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=90563,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90563,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90401,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
        ]

        """
        Generic auction
        """
        for i in range(10):
            cls.index.shops += [
                Shop(fesh=21030 + i, priority_region=213),
            ]

        cls.index.offers += [
            Offer(hyperid=21030, fesh=21031, title="ha bid=90", price=9000, hid=200, bid=90, randx=create_randx(1)),
            Offer(hyperid=21030, fesh=21032, title="ha bid=85", price=9000, hid=200, bid=85, randx=create_randx(2)),
            Offer(hyperid=21030, fesh=21033, title="ha bid=87", price=9000, hid=200, bid=87, randx=create_randx(3)),
            Offer(hyperid=21030, fesh=21034, title="ha bid=45", price=9000, hid=200, bid=45, randx=create_randx(4)),
            Offer(hyperid=21030, fesh=21035, title="ha bid=43", price=9000, hid=200, bid=43, randx=create_randx(5)),
            Offer(hyperid=21030, fesh=21036, title="ha bid=41", price=9000, hid=200, bid=41, randx=create_randx(6)),
            Offer(hyperid=21030, fesh=21037, title="ha bid=40", price=9000, hid=200, bid=40, randx=create_randx(7)),
        ]

        for i in range(1, 10):
            cls.index.offers += [
                Offer(
                    hyperid=21031,
                    fesh=21030 + i,
                    title="ha threshold passed by 4 of 9",
                    price=9000,
                    hid=200,
                    cbid=40 + 5 * i,
                    randx=create_randx(10 + i),
                )
            ]

        for i in range(6, 10):
            cls.index.offers += [
                Offer(
                    hyperid=21033,
                    fesh=21030 + i,
                    title="ha threshold passed by 4 of 4",
                    price=9000,
                    hid=200,
                    cbid=1000 + i,
                    randx=create_randx(10 + i),
                )
            ]

        cls.index.offers += [
            Offer(hyperid=21032, fesh=21031, title="test alpha", price=9000, hid=200, bid=150),
            Offer(hyperid=21032, fesh=21032, title="test alpha", price=9000, hid=200, bid=40),
        ]

        cls.index.offers += [
            Offer(hyperid=21034, fesh=21031, title="test cpc mult", price=9000, hid=200, bid=40),
            Offer(hyperid=21034, fesh=21032, title="test cpc mult", price=9000, hid=200, bid=20),
            Offer(hyperid=21034, fesh=21033, title="test cpc mult", price=9000, hid=200, bid=100),
            Offer(hyperid=21034, fesh=21034, title="test cpc mult", price=9000, hid=200, bid=50),
        ]

    def test_hybrid_auction_pp21(self):
        """
        Check hybrid auction with not-threshold auto broker
        All offers are shown at pp=21 and no threshold applied to CPM

        После включения нового расчёта мин бидов (MARKETOUT-14481), на сортировках на карточке модели
        в качестве цены клика используется мин бид, а тикет MARKETOUT-15644 включает использование min fee

        """
        for how in ["", "&how=aprice", "&how=dprice", "&how=rorp"]:
            response = self.report.request_json(
                '&place=productoffers&pp=21&hyperid=21031&hid=200&rids=213&grhow=shop&offers-set=listCpa&debug=1&show-urls=external,cpa'
                + how
            )

            offer_count = response.count(
                {
                    'entity': 'offer',
                }
            )
            self.assertEqual(offer_count, 9)

            if how == '':
                self.assertFragmentIn(response, 'AutoBroker: Mode: CardHybridCpmPolicy AfterSort: NoAfterSort')
            else:
                self.assertFragmentIn(response, 'AutoBroker: Mode: MinBidMinFee AfterSort: NoAfterSort')

        for pos, cp in [
            [1, 81],
            [2, 76],
            [3, 71],
            [4, 66],
            [5, 61],
            [6, 56],
            [7, 51],
            [8, 46],
        ]:
            self.click_log.expect(ClickType.EXTERNAL, cp=cp, position=pos, min_bid=12).times(1)
            self.click_log.expect(ClickType.EXTERNAL, cp=12, position=pos, min_bid=12).times(3)

        self.click_log.expect(ClickType.EXTERNAL, cp=12, position=9, min_bid=12).times(4)

    @classmethod
    def prepare_hybrid_auction_offers_different_types_ranking(cls):
        cls.index.shops += [
            Shop(fesh=5101, priority_region=213),
            Shop(fesh=5102, priority_region=213),
            Shop(fesh=5103, priority_region=213, cpc=Shop.CPC_NO),
            Shop(fesh=5104, priority_region=213, cpc=Shop.CPC_NO),
            Shop(fesh=5105, priority_region=213, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(title="cpc only", hyperid=55101, hid=90574, fesh=5101, price=10000, bid=50, ts=1),
            Offer(title="cpc and cpa", hyperid=55101, hid=90574, fesh=5102, price=10000, bid=50, ts=2),
            Offer(title="cpa only 500", hyperid=55101, hid=90574, fesh=5103, price=10000, bid=50, ts=3),
            Offer(
                title="cpa only 400",
                hyperid=55101,
                hid=90574,
                fesh=5104,
                price=10000,
                bid=100,
                ts=4,
                randx=create_randx(100),
            ),
            Offer(
                title="cpa only 400",
                hyperid=55101,
                hid=90574,
                fesh=5105,
                price=10000,
                bid=100,
                ts=5,
                randx=create_randx(10),
            ),
        ]
        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(
                category=90574,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.0016,
                market_conv_for_cpa_msk=0.035,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
            ),
        ]

        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=90574,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.1,
            ),
            MinBidsCategory(
                category_id=90574,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.1,
            ),
        ]

    @classmethod
    def prepare_hybrid_auction_auto_broker_rounding(cls):
        cls.index.shops += [
            Shop(fesh=5501, priority_region=213),
            Shop(fesh=5502, priority_region=213),
            Shop(fesh=5503, priority_region=213),
            Shop(fesh=5504, priority_region=213),
        ]

        cls.index.offers += [
            Offer(title="bid=150", hyperid=55201, hid=501, fesh=5501, price=10000, bid=150, ts=1),
            Offer(title="bid=120", hyperid=55201, hid=501, fesh=5502, price=10000, bid=120, ts=2),
            Offer(title="bid=60", hyperid=55201, hid=501, fesh=5503, price=10000, bid=60, ts=3),
            Offer(title="bid=30", hyperid=55201, hid=501, fesh=5504, price=10000, bid=30, ts=4),
        ]

    def test_hybrid_auction_auto_broker_rounding(self):
        _ = self.report.request_json(
            '&place=productoffers&pp=21&hyperid=55201&rids=213&grhow=shop&offers-set=listCpa&rearr-factors=market_ha_cpa_cpm_mult=3.3&show-urls=external,cpa'
        )

        self.click_log.expect(ClickType.EXTERNAL, cb=150, cp=121, shop_id=5501, position=1)

        self.click_log.expect(ClickType.EXTERNAL, cb=120, cp=61, shop_id=5502, position=2)

        self.click_log.expect(ClickType.EXTERNAL, cb=60, cp=31, shop_id=5503, position=3)

    @classmethod
    def prepare_best_deal(cls):
        cls.index.shops += [
            Shop(fesh=5000, priority_region=213),
        ]

        cls.index.models += [
            Model(hyperid=16000, hid=6000),
            Model(hyperid=16001, hid=6000),
            Model(hyperid=16002, hid=6000),
            Model(hyperid=16003, hid=6000),
        ]

        """
        Создаём модель с двумя офферами, первый из которых имеет скидку,
        которая обеспечивает минимальную цену среди всех доступных предложений
        """
        cls.index.offers += [
            Offer(
                hyperid=16000,
                fesh=5000,
                price=3000,
                discount=60,
                bid=100,
                title="16000_1",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                ts=160005000,
            ),
            Offer(
                hyperid=16000,
                fesh=5000,
                price=4000,
                discount=5,
                bid=10,
                title="16000_2",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 160005000).respond(0.002)

        """
        Создаём модель с двумя офферами, первый из которых имеет скидку,
        но она НЕ обеспечивает минимальную цену среди всех доступных предложений
        """
        cls.index.offers += [
            Offer(
                hyperid=16001,
                fesh=5000,
                price=3000,
                discount=10,
                title="16001_1",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
            ),
            Offer(
                hyperid=16001,
                fesh=5000,
                price=2000,
                title="16001_2",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                discount=5,
            ),
        ]

        """
        Создаём модель с двумя офферами без скидки
        """
        cls.index.offers += [
            Offer(
                hyperid=16002,
                fesh=5000,
                price=3000,
                title="16002_1",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                discount=5,
            ),
            Offer(
                hyperid=16002,
                fesh=5000,
                price=2000,
                title="16002_2",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                discount=5,
            ),
        ]

        """
        Создаём модель с тремя офферами, один из которых относится к другому региону. Последний
        должен иметь минимальную цену, чтобы проверить, что статистика модели учитывает регион.
        """
        cls.index.shops += [
            Shop(fesh=5001, priority_region=300),
        ]

        cls.index.offers += [
            Offer(
                hyperid=16003,
                fesh=5001,
                price=1000,
                title="16002_1",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                discount=5,
            ),
            Offer(
                hyperid=16003,
                fesh=5000,
                price=3000,
                title="16002_2",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                discount=5,
            ),
            Offer(
                hyperid=16003,
                fesh=5000,
                price=2000,
                title="16002_3",
                delivery_options=[DeliveryOption(price=100, day_from=2, day_to=2, order_before=23)],
                discount=5,
            ),
        ]

    def test_best_deal(self):
        """
        Делаем запрос к productoffers выбрав модель 16000

        Проверяем, что на выдаче есть все два оффера и один ДО, и что метки isBestDeal расставлены
        правильно
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=16000&numdoc=6&hid=6000&rids=213&offers-set=default,listCpa&pp=6'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "16000_1"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16000_2"},
                    "prices": {"discount": {"isBestDeal": False}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16000_1"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": {"type": "default"},
                },
            ],
        )

    def test_best_deal_discount_is_too_small(self):
        """
        Делаем запрос к productoffers выбрав модель 16001

        Проверяем, что на выдаче есть все два оффера и один ДО, и что метки isBestDeal расставлены
        правильно
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=16001&numdoc=6&hid=6000&rids=213&offers-set=default,listCpa&pp=6'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "16001_1"},
                    "prices": {"discount": {"isBestDeal": False}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16001_2"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16001_2"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": {"type": "default"},
                },
            ],
        )

    def test_best_deal_no_discount(self):
        """
        Делаем запрос к productoffers выбрав модель 16002

        Проверяем, что на выдаче есть все два оффера и один ДО, и что метки isBestDeal расставлены
        правильно
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=16002&numdoc=6&hid=6000&rids=213&offers-set=default,listCpa&pp=6'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "16002_1"},
                    "prices": {"discount": {"isBestDeal": False}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16002_2"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16002_2"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": {"type": "default"},
                },
            ],
        )

    def test_best_deal_region1(self):
        """
        Делаем запрос к productoffers выбрав модель 16003

        Проверяем, что у оффера с минимальной в регионе ценой стоит метка isBestDeal
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=16003&numdoc=6&hid=6000&rids=213&offers-set=default,listCpa&pp=6'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "16002_3"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": Absent(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "16002_3"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": {"type": "default"},
                },
            ],
        )

    def test_best_deal_region2(self):
        """
        Делаем запрос к productoffers выбрав модель 16003

        Проверяем, что у оффера с минимальной в регионе ценой стоит метка isBestDeal
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=16003&numdoc=6&hid=6000&rids=300&offers-set=default,listCpa&pp=6'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": "16002_1"},
                    "prices": {"discount": {"isBestDeal": True}},
                    "benefit": {"type": "default"},
                },
            ],
        )

    @classmethod
    def prepare_reference_price(cls):
        """Добавляются офферы с ценой меньше и больше reference-price=100"""
        cls.index.shops += [Shop(fesh=5002, priority_region=213)]

        cls.index.offers += [
            Offer(hyperid=1111, fesh=5002, price=90, waremd5='22222222222222gggggggg'),
            Offer(hyperid=1111, fesh=5002, price=95, waremd5='11111111111111gggggggg'),
            Offer(hyperid=1111, fesh=5002, price=104, waremd5='09lEaAKkQll1XTaaaaaaaQ'),
            Offer(hyperid=1111, fesh=5002, price=105, waremd5='DuE098x_rinQLZn3KKrELw'),
            Offer(hyperid=1111, fesh=5002, price=107, waremd5='_qQnWXU28-IUghltMZJwNw'),
        ]

    def test_reference_price(self):
        """Проверяется, что только офферы, цена которых <= reference-price * 1.05, присутствуют в выдаче"""
        response = self.report.request_json('place=productoffers&hyperid=1111&reference-price=100&how=aprice')
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "wareId": "22222222222222gggggggg", "prices": {"value": "90"}},
                {"entity": "offer", "wareId": "11111111111111gggggggg", "prices": {"value": "95"}},
                {"entity": "offer", "wareId": "09lEaAKkQll1XTaaaaaaaQ", "prices": {"value": "104"}},
                {"entity": "offer", "wareId": "DuE098x_rinQLZn3KKrELw", "prices": {"value": "105"}},
            ],
            preserve_order=True,
        )

        self.assertFragmentNotIn(response, [{"entity": "offer", "wareId": "_qQnWXU28-IUghltMZJwNw"}])

    @classmethod
    def prepare_delivery_without_SiS_in_hybrid_auction(cls):
        """
        Добавляем локальный оффер с низкой ставкой и оффер из региона (без СиС) с большой ставкой.
        """
        cls.index.shops += [
            Shop(fesh=6001, priority_region=213),
            Shop(fesh=6002, priority_region=214, regions=[225]),
        ]

        cls.index.offers += [
            Offer(hyperid=6101, hid=6201, fesh=6001, bid=10, title='local'),
            Offer(hyperid=6101, hid=6201, fesh=6002, bid=1000, title='nonlocal'),
        ]

    def test_delivery_without_SiS_in_hybrid_auction(self):
        """
        Региональный оффер без СиС должен быть пессимизирован.
        """
        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=6101&hid=6201&rids=213&offers-set=listCpa&local-offers-first=0'
        )
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'local'}},
                {'titles': {'raw': 'nonlocal'}},
            ],
        )

    @classmethod
    def prepare_missed_hidd(cls):
        """
        Добавляем офер, у которого нет поля hidd
        """
        cls.index.shops += [
            Shop(fesh=6011, priority_region=213),
        ]

        cls.index.offers += [
            Offer(
                hyperid=6111,
                fesh=6011,
                waremd5='DPBf1fM7GEYfSiYSJaWtIQ',
                dont_save_fields=['hidd'],
                feedid=11000,
                offerid="OfFeR1",
                title='NoHidd',
                glparams=[GLParam(param_id=202, value=2)],
            )
        ]

    def test_missed_hidd(self):
        """
        Офер, у которого нет параметра hidd будет пропущен, а в логи быдет записана ошибка.
        """
        response = self.report.request_json('place=productoffers&pp=6&hyperid=6111&rids=213')
        self.assertFragmentNotIn(
            response,
            [
                {'titles': {'raw': 'NoHidd'}},
            ],
        )

        self.base_logs_storage.error_log.expect('No HIDD attributes in document')

    @classmethod
    def prepare_hybrid_auction_mn_formula_experiment(cls):
        """
        See MARKETOUT-14787
        Check offess ranking in hybrid auction with CTR prediction for CPC and CPA parts
        """
        cls.index.hybrid_auction_settings += [
            HybridAuctionParam(
                category=90564,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                shop_conv_for_cpc=0.1,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.0016,
                market_conv_for_cpa_msk=0.055,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
                shop_conv_for_cpc_msk=0.1,
            ),
            HybridAuctionParam(
                category=90565,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                shop_conv_for_cpc=0.1,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.007,
                market_conv_for_cpa_msk=0.055,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
                shop_conv_for_cpc_msk=0.1,
            ),
            HybridAuctionParam(
                category=90566,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                shop_conv_for_cpc=0.1,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.02,
                market_conv_for_cpa_msk=0.055,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
                shop_conv_for_cpc_msk=0.1,
            ),
            HybridAuctionParam(
                category=90567,
                cpa_ctr_for_cpa=0.0035,
                cpc_ctr_for_cpa=0.008,
                market_conv_for_cpa=0.02,
                shop_conv_for_cpa=0.2,
                cpc_ctr_for_cpc=0.013,
                shop_conv_for_cpc=0.1,
                cpa_ctr_for_cpa_msk=0.0026,
                cpc_ctr_for_cpa_msk=0.0016,
                market_conv_for_cpa_msk=0.055,
                shop_conv_for_cpa_msk=0.2,
                cpc_ctr_for_cpc_msk=0.008,
                shop_conv_for_cpc_msk=0.1,
            ),
        ]
        cls.index.min_bids_category_stats += [
            MinBidsCategory(
                category_id=90564,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90564,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90565,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.1,
            ),
            MinBidsCategory(
                category_id=90565,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.1,
            ),
            MinBidsCategory(
                category_id=90566,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90566,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.2,
            ),
            MinBidsCategory(
                category_id=90567,
                geo_group_id=0,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.1,
            ),
            MinBidsCategory(
                category_id=90567,
                geo_group_id=1,
                price_group_id=0,
                drr=0.0095,
                search_conversion=0.04,
                card_conversion=0.04,
                full_card_conversion=0.1,
            ),
        ]

        for i in range(1, 6):
            cls.index.shops += [Shop(fesh=9000 + i, priority_region=213, regions=[225])]
        cls.index.shops += [Shop(fesh=9020, priority_region=213, regions=[225], cpc=Shop.CPC_NO)]

        cls.index.models += [
            Model(hyperid=8011, hid=90564),
        ]
        cls.index.offers += [
            Offer(hyperid=8011, fesh=9001, price=10000, bid=100, ts=8021),
            Offer(hyperid=8011, fesh=9002, price=10000, bid=100, ts=8022),
            Offer(hyperid=8011, fesh=9020, price=10000, bid=0, ts=8023),
            Offer(hyperid=8011, fesh=9003, price=10000, bid=300, ts=8024),
            Offer(hyperid=8011, fesh=9004, price=10000, bid=30, ts=8025),
            Offer(hyperid=8011, fesh=9005, price=10000, bid=900, ts=8026),
        ]

        cls.index.models += [
            Model(hyperid=8012, hid=90565),
        ]
        cls.index.offers += [
            Offer(hyperid=8012, fesh=9001, price=10000, bid=100, ts=8031),
        ]

        cls.index.models += [
            Model(hyperid=8013, hid=90566),
        ]
        cls.index.offers += [
            Offer(hyperid=8013, fesh=9001, price=10000, bid=100, ts=8041),
        ]

        cls.index.models += [
            Model(hyperid=8024, hid=90567),
        ]
        cls.index.offers += [
            Offer(hyperid=8024, fesh=9001, price=10000, bid=100, ts=8021),
            Offer(hyperid=8024, fesh=9002, price=10000, bid=100, ts=8022),
            Offer(hyperid=8024, fesh=9020, price=10000, bid=0, ts=8023),
            Offer(hyperid=8024, fesh=9003, price=10000, bid=300, ts=8024),
            Offer(hyperid=8024, fesh=9004, price=10000, bid=30, ts=8025),
            Offer(hyperid=8024, fesh=9005, price=10000, bid=900, ts=8026),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8021).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8022).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8023).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8024).respond(0.012)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8025).respond(0.014)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8026).respond(0.016)

    def test_hybrid_auction_mn_formula_experiment_cpm_value_for_cpc(self):
        """
        Проверяем, что CPM для офферов считается правильно.
        Оффер CPC
        Формулы тут: MARKETOUT-14787
        """
        # Проверяем CPM без дополнительных коэффициентов
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpa_ctr_mult=1;'
            '&debug=1'
        )
        # CPM = (CpcCpm) * 100000
        # c1 = 1
        # CpcCtr = c1 * MN = 0.01
        # CpcCpm = CpcCtr * bid = 0.01 * 100 = 1
        # CPM = 1 * 100000 = 100000
        self.assertFragmentIn(
            response,
            {
                'properties': {'TS': '8026', 'CPM': '1440000'},
            },
        )

        # Проверяем CPM с коэффициентами к CTR
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpc_ctr_mult_for_cpc=5;market_ranging_cpa_by_ue_in_top_addition_constant_d=0;'
            '&debug=1'
        )
        # CPM = (CpcCpm) * 100000
        # c1 = 5
        # CpcCtr = c1 * MN = 0.05
        # CpcCpm = CpcCtr * bid = 0.01 * 100 = 5
        # CPM = 5 * 100000 = 500000 ~ 499999 (due to float errors)
        self.assertFragmentIn(
            response,
            {
                'properties': {'TS': '8022', 'CPM': '499999'},
            },
        )

        # Проверяем CPM с коэффициентами к CTR и степенью
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpc_ctr_mult_for_cpc=5;market_ha_ctr_pow=0.7;market_ranging_cpa_by_ue_in_top_addition_constant_d=0;'
            '&debug=1'
        )
        # CPM = (CpcCpm) * 100000
        # c1 = 5
        # p1 = 0.7
        # CpcCtrRaw = c1 * MN = 0.05
        # CpcCtrCategory = 0.008
        # CpcCtr = (CpcCtrRaw) ^ p1 * (CpcCtrCategory ^ (1 - p1)) = 0.12282 * 0.23492 = 0.02885
        # CpcCpm = CpcCtr * bid = 0.02885 * 100 = 2.885
        # CPM = 2.885 * 100000 ~ 288539 (due to float errors)
        self.assertFragmentIn(
            response,
            {
                'properties': {'TS': '8022', 'CPM': '288539'},
            },
        )

    def test_hybrid_auction_mn_formula_experiment_mn_values(self):
        """
        Проверяем, что все нужные формулы вычисляются.
        Подробности тут: MARKETOUT-14787
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa' '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'fullFormulaInfo': [
                    {
                        'tag': 'CpcClick',
                        'name': 'MNA_HybridAuctionCpcCtr2430',
                    },
                    {
                        'tag': 'CpaBuy',
                        'name': 'MNA_P_Purchase_log_loss_full_factors_6w_20210311',
                    },
                ],
            },
        )

    def test_hybrid_auction_mn_formula_experiment_mn_values_custom(self):
        """
        Проверяем, что все нужные формулы вычисляются.
        Подробности тут: MARKETOUT-14787
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpa_formula=CustomCPA;market_ha_cpc_formula=CustomCpc'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'fullFormulaInfo': [
                    {
                        'tag': 'CpcClick',
                        'name': 'CustomCpc',
                    },
                    {
                        'tag': 'CpaBuy',
                        'name': 'CustomCPA',
                    },
                ],
            },
        )

    def test_top6_logloss_formula_presents(self):
        """
        Проверяем, что все формула для top6 logloss вычисляется.
        Подробности тут: MADV-1240
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpa_formula=MNA_top6_formula_logloss_220718'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'fullFormulaInfo': [
                    {
                        'tag': 'CpaBuy',
                        'name': 'MNA_top6_formula_logloss_220718',
                    },
                ],
            },
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpa_formula=MNA_top6_formula_logloss_220725'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'fullFormulaInfo': [
                    {
                        'tag': 'CpaBuy',
                        'name': 'MNA_top6_formula_logloss_220725',
                    },
                ],
            },
        )

    def test_top6_softmax_formula_presents(self):
        """
        Проверяем, что все формула для top6 logloss вычисляется.
        Подробности тут: MADV-1240
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpa_formula=MNA_top6_formula_softmax_220718'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'fullFormulaInfo': [
                    {
                        'tag': 'CpaBuy',
                        'name': 'MNA_top6_formula_softmax_220718',
                    },
                ],
            },
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpa_formula=MNA_top6_formula_softmax_220725'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'fullFormulaInfo': [
                    {
                        'tag': 'CpaBuy',
                        'name': 'MNA_top6_formula_softmax_220725',
                    },
                ],
            },
        )

    def test_hybrid_auction_mn_formula_experiment_autobroker_with_coefficients(self):
        """
        Проверяем, что автоброкер работает правильно, понижая ставки пропорционально CPM.
        Формулы тут: MARKETOUT-14787
        Проверяем коэффициентами к CTR-ам
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=8011&rids=213&grhow=shop&offers-set=list&show-urls=external,cpa'
            '&rearr-factors=market_ha_cpc_ctr_mult_for_cpa=5;market_ha_cpa_ctr_mult=2;market_ha_cpc_ctr_mult_for_cpc=10;market_ranging_cpa_by_ue_in_top_addition_constant_d=0;market_ranging_cpa_by_ue_in_top_cpa_multiplier=1'  # noqa
            '&debug=1'
        )

        # Расчёт CPM проверяется в отдельном тесте, здесь значения CPM взяты из выдачи.
        def check_cpm(ts, cpm):
            self.assertFragmentIn(
                response,
                {
                    'properties': {'TS': str(ts), 'CPM': str(cpm)},
                },
            )

        for ts, cpm in [
            (8026, 14400002),
            (8024, 3600000),
            (8022, 999999),
            (8021, 999999),
            (8025, 419999),
        ]:
            check_cpm(ts, cpm)
        # 1st/2nd = 1.63494
        # 2nd/3rd = 1.04097
        # 3rd/4th = 1.0375
        # 4th/5th = 1.36054
        # 5th/6th = 9.79993
        # Амнистированные bid и fee должны быть уменьшены от исходных в такое количество раз
        # После этого они могут быть немного увеличенны, что бы с итогами округления итоговый CPM не получился ниже подпирающего.

        # 30 / 1.63494 ~ 18 -> 19
        # 2500 / 1.63494 ~ 1529 -> 1530
        self.click_log.expect(ClickType.EXTERNAL, cb=900, cp=225, position=1)

        # 900 / 1.04097 ~ 865
        self.click_log.expect(ClickType.EXTERNAL, cb=300, cp=84, position=2)

        # 100 / 1.0375 ~ 96
        # 2000 / 1.0375 ~ 1928
        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=100, position=3)

        # 300 / 9.79993 ~ 31
        # 1000 / 9.79993 ~ 102
        self.click_log.expect(ClickType.EXTERNAL, cb=100, cp=43, position=4)
        # to min bid
        self.click_log.expect(ClickType.EXTERNAL, cb=30, cp=13, min_bid=13, position=5)

    @classmethod
    def prepare_gl_filters_from_default_offer(cls):
        """Создаем gurulight типы - базовый цвет(221), уникальный цвет(222),
        "просто" numeric(223) и "просто" enum(224)
        Создаем модель и офферы к ней со значениями gl-параметров 1 и 2
        (попарно, 221-222 и 223-224)
        """
        cls.index.gltypes += [
            GLType(
                param_id=221, hid=5700, gltype=GLType.ENUM, cluster_filter=True, unique_color_id=222, values=[1, 2, 3]
            ),
            GLType(
                param_id=222,
                hid=5700,
                gltype=GLType.ENUM,
                cluster_filter=True,
                subtype='image_picker',
                values=[1, 2, 3],
            ),
            GLType(param_id=223, hid=5700, gltype=GLType.NUMERIC, cluster_filter=True),
            GLType(param_id=224, hid=5700, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2, 3]),
            GLType(
                param_id=225,
                hid=5700,
                gltype=GLType.ENUM,
                subtype='size',
                values=[
                    GLValue(1, text="M", unit_value_id=1),
                    GLValue(2, text="L", unit_value_id=1),
                    GLValue(3, text="44", unit_value_id=2),  # Also M
                    GLValue(4, text="52", unit_value_id=2),  # Also L
                ],
                unit_name='Size',
                cluster_filter=True,
                unit_param_id=510,
            ),
            GLType(
                param_id=510,
                hid=5700,
                gltype=GLType.ENUM,
                name='size_units',
                position=None,
                values=[GLValue(value_id=1, text='American', default=True), GLValue(value_id=2, text='Russian')],
            ),
            GLType(param_id=511, hid=5700, gltype=GLType.BOOL, cluster_filter=True),
            GLType(param_id=512, hid=5700, gltype=GLType.STRING, cluster_filter=True),
            GLType(param_id=513, hid=5700, gltype=GLType.ENUM, cluster_filter=False, values=[1, 2, 3]),
        ]

        cls.index.models += [
            Model(
                hyperid=8014,
                hid=5700,
                glparams=[GLParam(param_id=513, value=2)],
                proto_add_pictures=[
                    PictureMbo(
                        params=[
                            PictureParam(param_id=224, type=GLType.ENUM, value=1),
                            PictureParam(param_id=225, type=GLType.ENUM, value=1),
                            PictureParam(param_id=222, type=GLType.ENUM, value=1),
                            PictureParam(param_id=511, type=GLType.BOOL, value=True),
                            PictureParam(param_id=221, type=GLType.ENUM, value=1),
                            PictureParam(param_id=223, type=GLType.NUMERIC, value='1'),  # Invalid, should be ignored
                        ]
                    ),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                title="1-1-1-1",
                randx=create_randx(4444),
                hyperid=8014,
                glparams=[
                    GLParam(param_id=221, value=1),
                    GLParam(param_id=222, value=1),
                    GLParam(param_id=223, value=1),
                    GLParam(param_id=224, value=1),
                    GLParam(param_id=225, value=1),
                    GLParam(param_id=225, value=3),
                    GLParam(param_id=511, value=1),
                    GLParam(param_id=512, string_value="Super"),
                ],
            ),
            Offer(
                title="1-1-2-2",
                hyperid=8014,
                randx=create_randx(3333),
                glparams=[
                    GLParam(param_id=221, value=1),
                    GLParam(param_id=222, value=1),
                    GLParam(param_id=223, value=2),
                    GLParam(param_id=224, value=2),
                    GLParam(param_id=225, value=2),
                    GLParam(param_id=225, value=4),
                    GLParam(param_id=511, value=1),
                    GLParam(param_id=512, string_value="Great"),
                ],
            ),
            Offer(
                title="2-2-1-1",
                hyperid=8014,
                randx=create_randx(2222),
                glparams=[
                    GLParam(param_id=221, value=2),
                    GLParam(param_id=222, value=2),
                    GLParam(param_id=223, value=1),
                    GLParam(param_id=224, value=1),
                    GLParam(param_id=225, value=1),
                    GLParam(param_id=225, value=3),
                    GLParam(param_id=511, value=0),
                    GLParam(param_id=512, string_value="Average"),
                ],
            ),
            Offer(
                title="2-2-2-2",
                hyperid=8014,
                randx=create_randx(1111),
                glparams=[
                    GLParam(param_id=221, value=2),
                    GLParam(param_id=222, value=2),
                    GLParam(param_id=223, value=1),
                    GLParam(param_id=224, value=1),
                    GLParam(param_id=225, value=2),
                    GLParam(param_id=225, value=4),
                    GLParam(param_id=511, value=0),
                    GLParam(param_id=512, string_value="Low"),
                ],
            ),
        ]

    def test_gl_filters_from_default_offer_unique_color(self):
        """Что тестируем: при запросе КМ значения фильтра "базовый цвет" заменяются
        на одно значение уникального цвета из дефолтного оффера
        """
        # Запрашиваем все возможные значения фильтра по цвету сразу
        response = self.report.request_json(
            'place=productoffers&hyperid=8014&rids=0&hid=5700&offers-set=list,default&glfilter=221:1,2'
        )

        # Проверяем, что на выдаче есть только офферы, у которых фильтр 222 имеет значение "1" (2шт.)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "1-1-1-1"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "1-1-2-2"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "1-1-1-1"},
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        # Проверяем, что у базового цвета нет checked-значений, у уникального цвета выбрано значение "1",
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "221",
                        "values": [{"id": "1", "checked": Absent()}, {"id": "2", "checked": Absent()}],
                    },
                    {
                        "id": "222",
                        "values": [{"id": "1", "checked": True}, {"id": "2", "checked": Absent()}],
                    },
                ]
            },
        )

    def test_size_filters_from_default_offer(self):
        """
        Проверяем, что размер чекается в дефолтной сетке по значению из дефолтного оффера
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=8014&rids=0&hid=5700&offers-set=list,default&glfilter=225:1,2'
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "225",
                        "units": [
                            {
                                "values": [{"id": "2", "checked": Absent()}, {"id": "1", "checked": True}],
                            }
                        ],
                    }
                ]
            },
        )

    def test_enum_filters_from_default_offer(self):
        '''
        Проверяем, что в enum-фильтрах устанавливается 1 значение из дефолтного оффера всегда
        в cgi если передано несколько значений
        '''
        # Запрашиваем два значения для enum-фильтра 224
        response = self.report.request_json(
            'place=productoffers&hyperid=8014&rids=0&hid=5700&offers-set=list,default&glfilter=224:1,2&glfilter=223:1,2&grhow=shop'
        )

        # Проверяем, что фильтр 224:1,2 преобразовался в 224:1 и на выдаче 3 оффера с этим значением
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer", "titles": {"raw": "1-1-1-1"}, "bundleCount": 1},
                        {
                            "entity": "offer",
                            "titles": {"raw": "2-2-1-1"},
                        },
                        {
                            "entity": "offer",
                            "titles": {"raw": "2-2-2-2"},
                        },
                        {"entity": "offer", "titles": {"raw": "1-1-1-1"}, "bundleCount": 1},
                    ]
                }
            },
            allow_different_len=False,
        )

        # проверяем, что checked - выбрано только 1 значение для фильтра 224
        # 223 - без изменений
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "224",
                        "values": [{"id": "1", "checked": True}, {"id": "2", "checked": Absent()}],
                    },
                    {"values": [{"id": "1~1", "checked": True}, {"id": "2~2", "checked": Absent()}], "id": "223"},
                ]
            },
        )

    def test_missing_gl_filters_from_default_offer(self):
        """Что тестируем: если Деффолтный Оффер не найден с переданными гл фильтрами,
        то они все сбрасываются, и возвращаются все офферы
        """
        # Запрашиваем значение 3 у фильтра 221 (таких офферов нет) и значения от 1 до 2 для фильтров
        # 222 и 223
        response = self.report.request_json(
            'place=productoffers&hyperid=8014&rids=0&hid=5700&offers-set=list,default&glfilter=221:3&glfilter=223:1,2&glfilter=224:1,3'
        )

        # Проверяем, что фильтр 221:3 и 224:1,3 сбросились и на выдаче все 4 оффера
        self.assertFragmentIn(response, {"search": {"total": 4}})

        # Проверяем, что все фильтры сбросились
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "221",
                        "values": [{"id": "1", "checked": Absent()}, {"id": "2", "checked": Absent()}],
                    },
                    {
                        "id": "222",
                        "values": [{"id": "1", "checked": Absent()}, {"id": "2", "checked": Absent()}],
                    },
                    {
                        "id": "223",
                        "values": [{"id": "1~1", "checked": Absent()}, {"id": "2~2", "checked": Absent()}],
                    },
                    {"id": "224", "values": [{"id": "1", "checked": Absent()}, {"id": "2", "checked": Absent()}]},
                ]
            },
        )

    def test_force_gl_filters(self):
        """Что тестируем: если Деффолтный Оффер не найден с переданными гл фильтрами,
        и включенным флагом &force-gl-filters=1
        то они НЕ сбрасываются, и возвращается пустой ответ
        """
        # Запрашиваем значение 3 у фильтра 221 (таких офферов нет)
        response = self.report.request_json(
            'place=productoffers&hyperid=8014&rids=0&hid=5700&offers-set=list,default&glfilter=221:3&force-gl-filters=1'
        )

        self.assertFragmentIn(response, {"search": {"total": 0}})

        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
            },
        )

    @classmethod
    def prepare_no_picture(cls):
        cls.index.models += [
            Model(hyperid=906090, hid=6090),
        ]
        cls.index.offers += [
            Offer(
                hyperid=906090,
                picture=Picture(
                    picture_id='iyC4nHslqLtqZJLygVAHeA',
                    width=200,
                    height=200,
                    thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
                ),
            )
        ]

    def test_no_picture(self):
        '''
        проверяем что с флагом market_no_pictures=1 картинки пропадают
        '''
        response = self.report.request_json('place=productoffers&hyperid=906090')

        self.assertFragmentIn(response, {"pictures": []})

        response = self.report.request_json('place=productoffers&hyperid=906090&rearr-factors=market_no_pictures=1')

        self.assertFragmentNotIn(response, {"pictures": []})

    @classmethod
    def prepare_offer_counters(cls):
        cls.index.shops += [Shop(fesh=10101, priority_region=213), Shop(fesh=10102, priority_region=213)]

        cls.index.gltypes += [
            GLType(param_id=1001, hid=9900, gltype=GLType.ENUM, cluster_filter=True, values=[1, 2]),
        ]

        cls.index.models += [
            Model(hyperid=9201, hid=9900),
        ]

        cls.index.offers += [
            Offer(
                hyperid=9201,
                fesh=10101,
                title='o1',
                manufacturer_warranty=0,
                glparams=[GLParam(param_id=1001, value=2)],
            ),
            Offer(
                hyperid=9201,
                fesh=10101,
                title='o2',
                manufacturer_warranty=0,
                glparams=[GLParam(param_id=1001, value=1)],
            ),
            Offer(
                hyperid=9201,
                fesh=10102,
                title='o3',
                manufacturer_warranty=0,
                glparams=[GLParam(param_id=1001, value=1)],
            ),
            Offer(
                hyperid=9201,
                fesh=10102,
                title='o4',
                manufacturer_warranty=1,
                glparams=[GLParam(param_id=1001, value=1)],
            ),
        ]

    def test_offer_counters(self):
        '''
        Проверяем, что с разными фильтрами
        не меняется количество до фильтров: totalOffersBeforeFilters и totalShopsBeforeFilters -
        всегда одинаковые
        Проверяем разные фильтры - что они на результат не влияют
        '''
        for query in [
            'place=productoffers&hyperid=9201&hid=9900&rids=213',
            'place=productoffers&hyperid=9201&hid=9900&rids=213&fesh=10101',
            'place=productoffers&hyperid=9201&hid=9900&rids=213&manufacturer_warranty=1&filter-promo-or-discount=1&filter-goods-of-the-week=1',
            'place=productoffers&hyperid=9201&hid=9900&rids=213&glfilter=1001:2',
        ]:
            response = self.report.request_json(query)
            self.assertFragmentIn(response, {'totalOffersBeforeFilters': 4, 'totalShopsBeforeFilters': 2})

    @classmethod
    def prepare_avg_items_count(cls):
        cls.index.models += [
            Model(hyperid=9301, hid=90563),
        ]

        cls.index.offers += [
            Offer(hyperid=9301, fesh=1001, price=1000, bid=70),
            Offer(hyperid=9301, fesh=1002, price=1000, bid=10),
        ]

    def test_avg_items_count(self):
        """
        Проверяем влияние учёта среднего количества позиций в заказе на CPA часть ранжирвоания офферов в гибридном аукционе
        """
        response = self.report.request_json('place=productoffers&hyperid=9301&rids=213')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'shop': {'id': 1001},
                },
                {
                    'entity': 'offer',
                    'shop': {'id': 1002},
                },
            ],
            preserve_order=True,
        )

    @classmethod
    def prepare_top_size_experiment(cls):
        cls.index.models += [
            Model(hyperid=10301),
        ]

        cls.index.offers += [
            Offer(hyperid=10301, fesh=1001, price=10000, bid=60, ts=1030101),
            Offer(hyperid=10301, fesh=1002, price=10000, bid=55, ts=1030102),
            Offer(hyperid=10301, fesh=1003, price=10000, bid=50, ts=1030103),
            Offer(hyperid=10301, fesh=1004, price=10000, bid=45, ts=1030104),
            # Threshold is here, bid = 43
            Offer(hyperid=10301, fesh=1005, price=10000, bid=40, ts=1030105),
            Offer(hyperid=10301, fesh=1006, price=10000, bid=35, ts=1030106),
            Offer(hyperid=10301, fesh=1007, price=10000, bid=30, ts=1030107),
            Offer(hyperid=10301, fesh=1008, price=10000, bid=25, ts=1030108),
        ]

        for ts in range(1030101, 1030109):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, ts).respond(0.008)

    def test_top_size_experiment_small(self):
        """
        Проверяем работу флага market_model_card_top_size.
        Он задаёт количество офферов на морде карточки и отключает порог по CPM.
        market_model_card_top_size = 3
        Заданное значение меньше исходного количества офферов на морде карточки
        Порог не работает, прошли 3 оффера, последний(3ий) подперт следующим оффером
        """
        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=10301&rids=213&grhow=shop&offers-set=listCpa&show-urls=external,cpa&numdoc=6'
            '&rearr-factors=market_model_card_top_size=3'
        )
        offer_count = response.count(
            {
                'entity': 'offer',
            }
        )
        self.assertEqual(offer_count, 3)
        self.click_log.expect(ClickType.EXTERNAL, cp=46, position=3)

    def test_top_size_experiment_equal(self):
        """
        Проверяем работу флага market_model_card_top_size.
        Он задаёт количество офферов на морде карточки и отключает порог по CPM.
        market_model_card_top_size = 4
        Заданное значение равно исходному количеству офферов на морде карточки
        Порог не работает, прошли 4 оффера, последний(4ый) подперт следующим оффером
        """
        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=10301&rids=213&grhow=shop&offers-set=listCpa&show-urls=external,cpa&numdoc=6'
            '&rearr-factors=market_model_card_top_size=4'
        )
        offer_count = response.count(
            {
                'entity': 'offer',
            }
        )
        self.assertEqual(offer_count, 4)
        self.click_log.expect(ClickType.EXTERNAL, cp=41, position=4)

    def test_top_size_experiment_big(self):
        """
        Проверяем работу флага market_model_card_top_size.
        Он задаёт количество офферов на морде карточки и отключает порог по CPM.
        market_model_card_top_size = 7
        Заданное значение больше исходного количества офферов на морде карточки
        и больше переданного numdoc
        Порог не работает, прошли 7 офферов, последний(7ой) подперт следующим оффером
        """
        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=10301&rids=213&grhow=shop&offers-set=listCpa&show-urls=external,cpa&numdoc=6'
            '&rearr-factors=market_model_card_top_size=7'
        )
        offer_count = response.count(
            {
                'entity': 'offer',
            }
        )
        self.assertEqual(offer_count, 7)
        self.click_log.expect(ClickType.EXTERNAL, cp=26, position=7)

    @classmethod
    def prepare_multivalue_united_glfilters(cls):
        """Создаем размерные сетки, где M - это 44-48 размер, а L - это 50-й
        Создаем офферы размеров M и L
        """
        cls.index.gltypes += [
            GLType(
                param_id=1577401,
                hid=1577400,
                gltype=GLType.ENUM,
                name='size_units',
                position=None,
                values=[
                    GLValue(value_id=157740101, text='American'),
                    GLValue(value_id=157740102, text='Russian', default=True),
                ],
            ),
            GLType(
                param_id=1577402,
                hid=1577400,
                gltype=GLType.ENUM,
                subtype='size',
                unit_name='Size',
                cluster_filter=True,
                unit_param_id=1577401,
                values=[
                    GLValue(1, text="M", unit_value_id=157740101),
                    GLValue(12, text="M-L", unit_value_id=157740101),
                    GLValue(2, text="L", unit_value_id=157740101),
                    GLValue(3, text="44", unit_value_id=157740102),  # Also M
                    GLValue(4, text="46", unit_value_id=157740102),  # Also M
                    GLValue(5, text="48", unit_value_id=157740102),  # Also M
                    GLValue(6, text="50", unit_value_id=157740102),  # Also L
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="куртка размера M",
                hyperid=1577410,
                hid=1577400,
                glparams=[
                    GLParam(param_id=1577402, value=1),
                    GLParam(param_id=1577402, value=3),
                    GLParam(param_id=1577402, value=4),
                    GLParam(param_id=1577402, value=5),
                ],
            ),
            Offer(
                title="куртка размера M-L",
                hyperid=1577410,
                hid=1577400,
                glparams=[
                    GLParam(param_id=1577402, value=1),
                    GLParam(param_id=1577402, value=12),
                    GLParam(param_id=1577402, value=4),
                    GLParam(param_id=1577402, value=5),
                ],
            ),
            Offer(
                title="куртка размера L",
                hyperid=1577410,
                hid=1577400,
                glparams=[
                    GLParam(param_id=1577402, value=2),
                    GLParam(param_id=1577402, value=6),
                ],
            ),
        ]

    def check_multivalue_united_glfilters_from_default_offer(self, rearr):
        """
        Проверяем, что чекается именно выбранный пользователем размер, даже если
        в ДО есть несколько вариантов
        """
        for filter_values in ['4', '4,5']:
            response = self.report.request_json(
                'place=productoffers&hyperid=1577410&rids=0&hid=1577400&offers-set=list,default&glfilter=1577402:{}&debug=1'.format(
                    filter_values
                )
                + rearr
            )
            self.assertFragmentIn(
                response,
                {
                    "filters": [
                        {
                            "id": "1577402",
                            "units": [
                                {
                                    "unitId": "American",
                                    "values": [
                                        {"id": "2", "checked": Absent()},
                                        {"id": "1", "checked": Absent()},
                                        {"id": "12", "checked": Absent()},
                                    ],
                                },
                                {
                                    "unitId": "Russian",
                                    "values": [
                                        {"id": "3", "checked": Absent()},
                                        {"id": "4", "checked": True},
                                        {"id": "5", "checked": Absent()},
                                        {"id": "6", "checked": Absent()},
                                    ],
                                },
                            ],
                        }
                    ]
                },
            )
            self.assertTrue("New glfilters from default offer: 1577402:4\"" in str(response))

        # Проверяем, что чекается размерная сетка из запроса
        response = self.report.request_json(
            'place=productoffers&hyperid=1577410&rids=0&hid=1577400&offers-set=list,default&glfilter=1577402:1,12&debug=1'
            + rearr
        )
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "1577402",
                        "units": [
                            {
                                "unitId": "American",
                                "values": [
                                    {"id": "2", "checked": Absent()},
                                ],
                            },
                            {
                                "unitId": "Russian",
                                "values": [
                                    {"id": "3", "checked": Absent()},
                                    {"id": "4", "checked": Absent()},
                                    {"id": "5", "checked": Absent()},
                                    {"id": "6", "checked": Absent()},
                                ],
                            },
                        ],
                    }
                ]
            },
        )
        # Чекается только одно из двух значений из запроса, хотя к ДО подходят оба
        in1 = "New glfilters from default offer: 1577402:1\""
        in2 = "New glfilters from default offer: 1577402:12\""
        self.assertTrue(in1 in str(response) or in2 in str(response))

        contains1 = response.contains([{"id": "1", "checked": True}, {"id": "12", "checked": Absent()}])
        contains2 = response.contains([{"id": "1", "checked": Absent()}, {"id": "12", "checked": True}])
        self.assertTrue(contains1 or contains2)

    def test_multivalue_united_glfilters_from_default_offer(self):
        self.check_multivalue_united_glfilters_from_default_offer("")

    @classmethod
    def prepare_no_glfilters(cls):
        cls.index.gltypes += [
            GLType(
                param_id=1631501, hid=1631500, gltype=GLType.ENUM, subtype='size', model_filter_index=-1, values=[1, 2]
            ),
        ]

        cls.index.models += [
            Model(
                title="куртка неизвестного размера",
                hyperid=1631510,
                hid=1631500,
                glparams=[
                    GLParam(param_id=1631501, value=1),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(title="куртка неизвестного размера", hyperid=1631510, hid=1631500),
        ]

    def test_no_glfilters_in_default_offer(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=1631510&rids=0&hid=1631500&offers-set=list,default&glfilter=1631501:1'
        )
        self.assertFragmentIn(response, {"results": [{"entity": "offer", "benefit": {"type": "default"}}]})

    @classmethod
    def prepare_order_min_cost(cls):
        cls.index.currencies = [
            Currency(
                'BYN',
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=1.0 / 25),
                ],
            ),
        ]
        cls.index.shops += [Shop(fesh=14001, order_min_cost=1000, currency='BYN')]
        cls.index.offers += [
            Offer(fesh=14001, price=24000, hyperid=888999),
            Offer(fesh=14001, price=25000, hyperid=888990),
        ]

    def test_order_min_cost(self):
        '''
        Проверяем, что в ответе есть order_min_cost, если цена оффера ниже минимальной цены заказа
        '''
        response = self.report.request_json('place=productoffers&hyperid=888999')
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"id": 14001}, "orderMinCost": {"currency": "BYN", "value": "1000"}}
        )

        response = self.report.request_json('place=productoffers&hyperid=888990')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {"id": 14001},
                "orderMinCost": NoKey('orderMinCost'),
            },
        )

    @classmethod
    def prepare_slug(cls):
        cls.index.shops += [Shop(fesh=14002, name='Магазин')]
        cls.index.offers += [Offer(fesh=14002, price=200, hyperid=8889990, title="Оффер")]

    def test_slug(self):
        """В информации о магазине должен присутствовать slug"""
        response = self.report.request_json('place=productoffers&hyperid=8889990')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 14002,
                    "name": "Магазин",
                    "slug": "magazin",
                },
            },
        )

    @classmethod
    def prepare_shop_logo(cls):
        cls.index.shops += [
            Shop(
                fesh=73731,
                shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/orig',
                shop_logo_info='14:30:PNG',
                shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small',
            ),
            Shop(
                fesh=73732,
                shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small',
                shop_logo_info='14:30:PNG',
            ),
            Shop(
                fesh=73733,
                shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small',
                shop_logo_info='14:30:SVG',
            ),
            Shop(
                fesh=73734,
                shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small',
                shop_logo_retina_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/orig',
            ),  # no logo_info
            Shop(
                fesh=73735,
                shop_logo_url='http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small',
                shop_logo_info='14:30',  # incorrect info
            ),
            Shop(
                fesh=73736,
            ),
        ]

        cls.index.offers += [
            Offer(fesh=73731, price=500, hyperid=88899911),
            Offer(fesh=73732, price=400, hyperid=88899911),
            Offer(fesh=73733, price=100, hyperid=88899911),
            Offer(fesh=73734, price=200, hyperid=88899911),
            Offer(fesh=73735, price=200, hyperid=88899911),
            Offer(fesh=73736, price=200, hyperid=88899911),
        ]

    def test_shop_logo(self):
        '''
        Проверяем, что когда у магазина указаны два поля логотипа,
        то у оффера возвращаются логотипы в нужном формате (PNG тип)
        '''
        response = self.report.request_json('place=productoffers&hyperid=88899911')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 73731,
                    "logo": {
                        "entity": "picture",
                        "width": 30,
                        "height": 14,
                        "url": "http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small",
                        "extension": "PNG",
                        "thumbnails": [
                            {
                                "entity": "thumbnail",
                                "id": "30x14",
                                "containerWidth": 30,
                                "containerHeight": 14,
                                "width": 30,
                                "height": 14,
                                "densities": [
                                    {
                                        "entity": "density",
                                        "id": "1",
                                        "url": "http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small",
                                    },
                                    {
                                        "entity": "density",
                                        "id": "2",
                                        "url": "http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/orig",
                                    },
                                ],
                            }
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

        # Случай, когда у магазина нет retina логотипа, но есть обычный
        # Возвращается только обычный логотип (PNG тип)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 73732,
                    "logo": {
                        "entity": "picture",
                        "width": 30,
                        "height": 14,
                        "url": "http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small",
                        "extension": "PNG",
                        "thumbnails": [
                            {
                                "entity": "thumbnail",
                                "id": "30x14",
                                "containerWidth": 30,
                                "containerHeight": 14,
                                "width": 30,
                                "height": 14,
                                "densities": [
                                    {
                                        "entity": "density",
                                        "id": "1",
                                        "url": "http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small",
                                    }
                                ],
                            }
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

        # Картинка в SVG формате, тогда не возвращаем урезанные картинки (thumbnails)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 73733,
                    "logo": {
                        "entity": "picture",
                        "width": 30,
                        "height": 14,
                        "url": "http://avatars.mdst.yandex.net:80/get-market-shop-logo/1395289/2a00000166aa90931a90f16f243ed902eddc/small",
                        "extension": "SVG",
                        "thumbnails": NoKey("thumbnails"),
                    },
                },
            },
            allow_different_len=False,
        )

        # В поле info некорректное число параметров (нет расширения)
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"id": 73734, "logo": NoKey("logo")}}, allow_different_len=False
        )

        # Когда не хватает info, то не возвращаем логотип
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"id": 73735, "logo": NoKey("logo")}}, allow_different_len=False
        )

        # Когда у магазина нет информации о логотипе, то вообще нет этого поля
        self.assertFragmentIn(
            response, {"entity": "offer", "shop": {"id": 73736, "logo": NoKey("logo")}}, allow_different_len=False
        )

    def test_shops_logo_disabled_by_dynamic(self):
        '''
        Проверяем правильность обработки динамического фильтра
        '''
        self.dynamic.market_dynamic.disabled_logo_shops.clear()
        # Нет данных в файле, логотип возвращается
        response = self.report.request_json('place=productoffers&hyperid=88899911')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 73731,
                    "logo": NotEmpty(),
                },
            },
        )

        # Добавляем магазин в файл, логотип не возвращается
        self.dynamic.market_dynamic.disabled_logo_shops += [DynamicShop(73731)]

        response = self.report.request_json('place=productoffers&hyperid=88899911')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 73731,
                    "logo": NoKey("logo"),
                },
            },
        )

        # Убрали магазин из файла, снова возвращается
        self.dynamic.market_dynamic.disabled_logo_shops.clear()

        response = self.report.request_json('place=productoffers&hyperid=88899911')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "shop": {
                    "id": 73731,
                    "logo": NotEmpty(),
                },
            },
        )

    @classmethod
    def prepare_group_by_shop(cls):
        cls.index.shops += [
            Shop(fesh=22233000),
            Shop(fesh=22233001),
            Shop(fesh=22233002),
        ]

        cls.index.models += [
            Model(hyperid=22233000),
        ]

        cls.index.offers += [
            Offer(fesh=22233000, price=500, hyperid=22233000),
            Offer(fesh=22233000, price=500, hyperid=22233000),
            Offer(fesh=22233001, price=500, hyperid=22233000),
            Offer(fesh=22233001, price=500, hyperid=22233000),
            Offer(fesh=22233002, price=500, hyperid=22233000),
            Offer(fesh=22233002, price=500, hyperid=22233000),
        ]

    def test_group_by_shop(self):
        self.assertFragmentIn(
            self.report.request_json('place=productoffers&grhow=shop&hyperid=22233000'), {"search": {"total": 3}}
        )
        self.assertFragmentIn(
            self.report.request_json('place=productoffers&grhow=shop&hyperid=22233000&fesh=22233000'),
            {"search": {"total": 1}},
        )
        self.assertFragmentIn(
            self.report.request_json('place=productoffers&grhow=shop&hyperid=22233000&fesh=22233000,22233001'),
            {"search": {"total": 2}},
        )

    @classmethod
    def prepare_franchise(cls):
        cls.index.hypertree += [
            HyperCategory(hid=104, output_type=HyperCategoryType.GURU),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=14020987,
                xslname='hero_global',
                hid=104,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='Angry Birds'),
                    GLValue(value_id=2, text='Тransformers'),
                    GLValue(value_id=3, text='Star Wars'),
                ],
            ),
        ]

        cls.index.models += [
            Model(hyperid=44455, hid=104),
            Model(hyperid=66677, hid=104),
        ]

        cls.index.offers += [
            Offer(hid=104, fesh=22233000, price=500, hyperid=44455, glparams=[GLParam(param_id=14020987, value=1)]),
            Offer(
                hid=104,
                fesh=22233000,
                price=500,
                hyperid=66677,
                glparams=[GLParam(param_id=14020987, value=2), GLParam(param_id=14020987, value=3)],
            ),
            Offer(hid=104, fesh=22233000, price=500, hyperid=88899),
        ]

    def test_franchise(self):
        """Проверяем вывод id франшиз оффера (если есть) и если задан соответствущий флажок"""
        self.assertFragmentIn(
            self.report.request_json(
                'place=productoffers&hyperid=44455&rearr-factors=market_show_franchises_for_offer=1'
            ),
            {"franchises": [{"id": "1"}]},
        )
        self.assertFragmentIn(
            self.report.request_json(
                'place=productoffers&grhow=shop&hyperid=66677&rearr-factors=market_show_franchises_for_offer=1'
            ),
            {"franchises": [{"id": "2"}, {"id": "3"}]},
        )
        self.assertFragmentNotIn(
            self.report.request_json('place=productoffers&grhow=shop&hyperid=66677'), {"franchises": NotEmpty()}
        )
        self.assertFragmentNotIn(
            self.report.request_json(
                'place=productoffers&grhow=shop&hyperid=88899&rearr-factors=market_show_franchises_for_offer=1'
            ),
            {"franchises": NotEmpty()},
        )

    @classmethod
    def prepare_stock_store_count(cls):
        cls.index.offers += [
            Offer(hid=2386500, hyperid=2386501, title='only one', stock_store_count=1),
            Offer(hid=2386500, hyperid=2386501, title='many', stock_store_count=10),
        ]

    def test_stock_store_count(self):
        """Проверяем вывод id франшиз оффера (если есть) и если задан соответствущий флажок"""
        response = self.report.request_json('place=productoffers&hyperid=2386501')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'only one'}, 'stockStoreCount': 1},
                    {'titles': {'raw': 'many'}, 'stockStoreCount': 10},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_shop_recommend(cls):
        cls.index.hypertree += [
            HyperCategory(hid=3535, output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hyperid=3535, hid=3535),
        ]

        cls.index.shops += [
            Shop(fesh=1234567, priority_region=213),
            Shop(fesh=234, priority_region=213),
        ]

        cls.index.offers += [
            Offer(fesh=1234567, hyperid=3535, title='Offer1'),
            Offer(fesh=234, hyperid=3535, title='Offer2'),
            Offer(fesh=234, hyperid=3535, title='Offer3'),
            Offer(fesh=234, hyperid=3535, title='Offer4'),
        ]

        cls.index.regional_clicks += [
            RegionalClicks(shop_id=1234567, geo_id=213, window=window, count=150) for window in range(48)
        ]

    def test_shop_recommend(self):
        """
        Проверяем наличие причин купить у магазина оффера под флагом эксперимента
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=3535&rids=213&rearr-factors=market_return_shop_recommend=1'
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "id": 234,
                    "reasonsToBuy": [
                        {"factorId": 4, "factorName": "Удобный самовывоз", "value": 4.7},
                        {"factorId": 2, "factorName": "Вежливое общение", "value": 4.6},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=3535&rids=213&rearr-factors=market_return_shop_recommend=2'
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "id": 234,
                    "reasonsToBuy": [{"factorId": 4, "factorName": "Удобный самовывоз", "value": 4.6}],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "id": 1234567,
                    "reasonsToBuy": [{"factorId": 5, "factorName": "Высокая популярность", "value": 150}],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=3535&rids=213&rearr-factors=market_return_shop_recommend=3'
        )
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "id": 234,
                    "reasonsToBuy": [
                        {"factorId": 4, "factorName": "Удобный самовывоз", "value": 4.7},
                        {"factorId": 1, "factorName": "Быстрая доставка", "value": 4.3},
                    ],
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        self.assertFragmentIn(response, [{"shop": {"id": 1234567, "reasonsToBuy": NoKey("reasonsToBuy")}}])

        response = self.report.request_json('place=productoffers&hyperid=3535&rids=213')
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 234, "reasonsToBuy": NoKey("reasonsToBuy")}},
                {"shop": {"id": 1234567, "reasonsToBuy": NoKey("reasonsToBuy")}},
            ],
        )

    def test_search_in_clones(self):
        def gen_req(shop_id, region, search_in_clones):
            req = 'place=productoffers&hyperid=3001&rids={}&fesh={}'.format(region, shop_id)
            return req + '&search-in-clones=1' if search_in_clones else req

        # Оффер есть в магазине 3000 в регионе 213 и в магазине 3002 в регионе 2
        # При search-in-clones=1 магазин должен находиться и если в запросе не он сам, а любой его клон
        for region in (213, 2):
            for clone in (3000, 3002, 3038):
                response = self.report.request_json(gen_req(clone, region, True))
                self.assertFragmentIn(
                    response, {"shop": {"id": 3000 if region == 213 else 3002, "requestShopId": clone}}
                )

        # Без search-in-clones=1 оффер должен находиться только в самом магазине с оффером
        for shop_id, region in ((3000, 213), (3002, 2)):
            response = self.report.request_json(gen_req(shop_id, region, False))
            self.assertFragmentIn(response, {"shop": {"id": shop_id, "requestShopId": Absent()}})

        for region in (213, 2):
            for clone in (3000, 3002, 3038):
                if region == 213 and clone == 3000 or region == 2 and clone == 3002:
                    continue
                response = self.report.request_json(gen_req(clone, region, False))
                self.assertFragmentIn(response, {"shop": Absent()})

        # проверка, что ничего не падает у магазинов без клонов
        response = self.report.request_json(gen_req(1001, 213, True))
        self.assertFragmentIn(response, {"shop": Absent()})
        response = self.report.request_json(gen_req(1001, 213, False))
        self.assertFragmentIn(response, {"shop": Absent()})

    @classmethod
    def prepare_pp_for_product_overview(cls):
        cls.index.models += [Model(hyperid=373737)]

        cls.index.shops += [
            Shop(fesh=3737371),
            Shop(fesh=3737372),
        ]

        cls.index.offers += [
            Offer(fesh=3737371, hyperid=373737),
            Offer(fesh=3737372, hyperid=373737),
        ]

    def test_pp_for_product_overview(self):
        response = self.report.request_json(
            "place=productoffers&pp=603&offers-set=default,list&show-urls=encrypted&hyperid=373737"
        )
        self.assertFragmentIn(
            response,
            {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(647))}, "benefit": {"type": "default"}},
        )

        self.assertFragmentIn(
            response,
            {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(603))}, "benefit": NoKey("benefit")},
        )

    @classmethod
    def prepare_offline_prices(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=198119,
                name="Electronics",
                children=[
                    HyperCategory(hid=91491, name="Mobile telephones"),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=4949491, priority_region=213, regions=[49, 213]),
            Shop(fesh=1672, priority_region=213, regions=[213]),
            Shop(fesh=184977, priority_region=213, regions=[213]),
        ]

        cls.index.models += [
            Model(hyperid=12649875, hid=91491),
        ]

        cls.index.offers += [
            Offer(fesh=4949491, hyperid=12649875),
            Offer(fesh=1672, hyperid=12649875),
            Offer(fesh=184977, hyperid=12649875),
        ]

    def test_offline_prices(self):
        '''
        Проверяем, что под флагом рядом с search отдается блок offlinePrices с ценами в оффлайн магазинах
        '''
        rearr = '&rearr-factors=market_return_offline_prices=1'
        response = self.report.request_json(
            'place=productoffers&hyperid=12649875&rids=213&{}&show-cutprice=1&hid=91491&return-online-offers=1'.format(
                rearr
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"entity": "offer"},
                        {"entity": "offer"},
                        {"entity": "offer"},
                    ]
                },
                "offlinePrices": {
                    "shopsInfo": [
                        {"shopDomain": "euroset.ru", "price": 9690},
                        {"shopDomain": "technopoint.ru", "price": 9499},
                        {"shopDomain": "www.dns-shop.ru", "price": 9999},
                        {"shopDomain": "www.eldorado.ru", "price": 9790},
                    ],
                    "updateTime": '2019-10-25 11:30',
                },
                "onlineOffers": [
                    {
                        "entity": "offer",
                        "shop": {"id": 1672},
                    },
                    {
                        "entity": "offer",
                        "shop": {"id": 184977},
                    },
                ],
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=productoffers&hyperid=12649875&rids=49&{}&show-cutprice=1&hid=91491&return-online-offers=1'.format(
                rearr
            )
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"results": [{"entity": "regionalDelimiter"}, {"entity": "offer"}]},
                "offlinePrices": {
                    "shopsInfo": [
                        {"shopDomain": "www.eldorado.ru", "price": 9990},
                        {"shopDomain": "www.dns-shop.ru", "price": 9999},
                    ],
                    "updateTime": '2019-10-25 11:30',
                },
                "onlineOffers": [],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_hidden_duplicates(cls):
        cls.index.offers += [
            Offer(hyperid=5500, fesh=8000, hid=200, price=100, title='red model 5500 from shop 8000'),
            Offer(hyperid=5500, fesh=8000, hid=200, price=200, title='black model 5500 from shop 8000'),
            Offer(hyperid=5500, fesh=8001, hid=200, price=300, title='model 5500 from shop 8001'),
        ]

        cls.index.shops += [
            # похожие магазины, а не региональные клоны
            Shop(fesh=8000, priority_region=213, main_fesh=8000),
            Shop(fesh=8001, priority_region=213, main_fesh=8000),
        ]

    def _test_hidden_duplicates(self, additional_params):
        # У модели есть 2 оффера в одном магазине, и 1 в "похожем" магазине в том же регионе
        req = 'place=productoffers&hyperid=5500&rids=213' + additional_params

        # Без grhow=shop показываются все оффера из всех магазинов
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'total': 3,
                'totalOffers': 3,
                'duplicatesHidden': 0,
                'results': [
                    {'titles': {'raw': 'red model 5500 from shop 8000'}, 'bundleCount': Absent(), 'shop': {'id': 8000}},
                    {
                        'titles': {'raw': 'black model 5500 from shop 8000'},
                        'bundleCount': Absent(),
                        'shop': {'id': 8000},
                    },
                    {'titles': {'raw': 'model 5500 from shop 8001'}, 'bundleCount': Absent(), 'shop': {'id': 8001}},
                ],
            },
            allow_different_len=False,
        )

        # С grhow=shop из всех офферов будет показываться только один.
        # При how=aprice будет выбран оффер из магазина 8000, т.к он самый дешевый
        response = self.report.request_json(req + '&grhow=shop&how=aprice')
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'totalOffers': 3,
                'duplicatesHidden': 1,  # скрыт оффер из магазина 8001
                'results': [
                    {
                        'titles': {'raw': 'red model 5500 from shop 8000'},
                        'bundleCount': 2,  # из этого магазина нашлось два оффера
                        'shop': {'id': 8000},
                    },
                ],
            },
            allow_different_len=False,
        )

        # Оффер из магазина 8001 самый дорогой, поэтому при how=dpice будет выбран он,
        # а оффера из магазина 8000 будут скрыты
        response = self.report.request_json(req + '&grhow=shop&how=dprice')
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'totalOffers': 3,
                'duplicatesHidden': 1,  # скрыта группа офферов из магазина 8000
                'results': [
                    {'titles': {'raw': 'model 5500 from shop 8001'}, 'bundleCount': 1, 'shop': {'id': 8001}},
                ],
            },
            allow_different_len=False,
        )

    def test_hidden_duplicates(self):
        self._test_hidden_duplicates('')
        # При show-cutprice=1 идем по другой ветке кода, проверим и ее
        self._test_hidden_duplicates('&show-cutprice=1')

    @classmethod
    def prepare_virtualmodel_offer_id(cls):
        '''prepare offers without parent model'''

        cls.index.shops += [
            Shop(fesh=2708, priority_region=213),
            Shop(fesh=2709, priority_region=213),
            Shop(fesh=2710, datafeed_id=4240, priority_region=213, regions=[213], client_id=11, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                fesh=2708,
                price=150,
                waremd5='red_axe_from_mvideo_md',
                title='offer without model',
                virtual_model_id=100500,
            ),
            Offer(
                fesh=2709,
                price=175,
                waremd5='blue_sword_from_dns_md',
                title='offer without model',
                virtual_model_id=100501,
            ),
            Offer(
                fesh=2710,
                waremd5='WhiteCpaWithMSKUvmid1g',
                hid=4244,
                price=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
                virtual_model_id=100502,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                fesh=2710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_virtualmodel_offer_id(self):
        '''test offers without parent model flag'''

        for fast_cards in [';use_fast_cards=0', ';use_fast_cards=1']:
            flags = (
                '&rearr-factors=market_skip_nid_filter_in_product_offers=1;market_cards_everywhere_product_offers=1;market_cards_everywhere_range=100000:200000'
                + fast_cards
            )

            response = self.report.request_json('place=productoffers&hyperid=100500' + flags)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'red_axe_from_mvideo_mQ',
                            'prices': {'value': '150'},
                            "model": {
                                "id": 100500,
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

            # Если в запросе передали виртуальный msku, которого нет, мы не должны ломаться
            response = self.report.request_json('place=productoffers&hyperid=100500&market-sku=100500' + flags)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'red_axe_from_mvideo_mQ',
                            'prices': {'value': '150'},
                            "model": {
                                "id": 100500,
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )
            # Даже если не передали hyperid
            response = self.report.request_json('place=productoffers&market-sku=100500' + flags)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'red_axe_from_mvideo_mQ',
                            'prices': {'value': '150'},
                            "model": {
                                "id": 100500,
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

            response = self.report.request_json('place=productoffers&hyperid=100501' + flags)
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'blue_sword_from_dns_mQ',
                            'prices': {'value': '175'},
                            "model": {
                                "id": 100501,
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

            response = self.report.request_json(
                'place=productoffers&hyperid=110511' + flags
            )  # unknown hyperid in range
            self.assertFragmentIn(response, {'search': {'totalOffers': 0, 'results': []}}, allow_different_len=False)

            response = self.report.request_json(
                'place=productoffers&hyperid=200501' + flags
            )  # unknown hyperid out of range
            self.assertFragmentIn(response, {'search': {'totalOffers': 0, 'results': []}}, allow_different_len=False)

            # проверяем что с если фронт просит ДО то мы отдаем оффер с benefit
            for offer_set, btype in [
                ('list,default', "default"),
                ('defaultList,listCpa', "cheapest" if fast_cards == ';use_fast_cards=1' else "default"),
            ]:
                response = self.report.request_json(
                    'place=productoffers&hyperid=100500' + flags + '&offers-set=' + offer_set
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': 'red_axe_from_mvideo_mQ',
                                'prices': {'value': '150'},
                                "benefit": {"type": btype},
                                "model": {
                                    "id": 100500,
                                },
                            },
                        ]
                    },
                )

                response = self.report.request_json(
                    'place=productoffers&hyperid=100502' + flags + '&offers-set=' + offer_set
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': 'WhiteCpaWithMSKUvmid1g',
                                'prices': {'value': '150'},
                                "benefit": {
                                    "type": btype,
                                    "nestedTypes": [btype],
                                },
                                "model": {
                                    "id": 100502,
                                },
                            },
                        ]
                    },
                )

            response = self.report.request_json(
                'place=productoffers&hyperid=100502&cpa=real' + flags + '&offers-set=' + offer_set
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'WhiteCpaWithMSKUvmid1g',
                            'prices': {'value': '150'},
                            "benefit": {
                                "type": 'cheapest' if fast_cards == ';use_fast_cards=1' else 'cpa',
                                "nestedTypes": ['cheapest' if fast_cards == ';use_fast_cards=1' else 'cpa'],
                            },
                            "model": {
                                "id": 100502,
                            },
                        },
                    ]
                },
            )

    def test_do_virtual_model_bids_from_cpc_param(self):
        """
        Проверяем, что cpc влияет на ДО
        """
        hyper_id = 100502
        shop_fee = 750
        brokered_fee = 300
        waremd5 = 'WhiteCpaWithMSKUvmid1g'

        cpc = Cpc.create_for_offer(
            click_price=71, offer_id=waremd5, bid=80, shop_id=21, shop_fee=shop_fee, fee=brokered_fee, minimal_fee=111
        )

        for fast_cards in ['', 'use_fast_cards=1']:
            response = self.report.request_json(
                "place=productoffers&hyperid=%s&offers-set=defaultList,listCpa&pp=6&cpc=%s&debug=da"
                "&rearr-factors=use_offer_type_priority_as_main_factor_in_do=1;market_ranging_blue_offer_priority_eq_dsbs=1;"
                "market_cards_everywhere_product_offers=1;market_cards_everywhere_range=100000:200000;%s"
                % (hyper_id, cpc, fast_cards)
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        "fee": format(brokered_fee / FEE_MULTIPLIER, '.4f'),
                        "feeShowPlain": Wildcard("fee: \"" + format(brokered_fee / FEE_MULTIPLIER, '.4f') + "\"*"),
                        "debug": {"wareId": waremd5, "sale": {"shopFee": shop_fee, "brokeredFee": brokered_fee}},
                    }
                ],
                preserve_order=True,
            )

    @classmethod
    def prepare_fast_mappings(cls):
        cls.index.shops += [
            Shop(fesh=27081, priority_region=213, datafeed_id=100200),
            Shop(fesh=27091, priority_region=213, datafeed_id=100201),
            Shop(fesh=27101, priority_region=213, datafeed_id=100202, regions=[213], client_id=1100, cpa=Shop.CPA_REAL),
            Shop(fesh=27102, priority_region=213, datafeed_id=100206, regions=[213], client_id=1101, cpa=Shop.CPA_REAL),
            Shop(fesh=27103, priority_region=213, datafeed_id=100207, regions=[213], client_id=1101, cpa=Shop.CPA_REAL),
        ]

        cls.index.models += [
            Model(hyperid=1242, title='Model 1242'),
            Model(hyperid=12420, title='Model 12420'),
            Model(hyperid=12421, title='Model 12421'),
            Model(hyperid=12422, title='Model 12422'),
            Model(hyperid=12423, title='Model 12423'),
            Model(hyperid=1243, title='Model 1243'),
            Model(hyperid=1555, title='Model 1555 - no fast mappings'),
            Model(hyperid=1556, title='Model 1556'),
            Model(hyperid=100439187587, title='Model 100439187587'),
        ]

        cls.index.virtual_models += [
            VirtualModel(virtual_model_id=300400),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1243,
                sku=12430,
                fesh=27101,
                blue_offers=[
                    BlueOffer(
                        feedid=100202,
                        offerid='offer_blue_12430_1',
                        price=200,
                        waremd5='offer_blue_12430_1__mQ',
                        title='offer sku 12430 1',
                    ),
                    BlueOffer(
                        feedid=100203,
                        offerid='offer_blue_12430_2',
                        price=125,
                        waremd5='offer_blue_12430_2__mQ',
                        title='offer sku 12430 2',
                    ),
                ],
            ),
            MarketSku(
                hyperid=1244,
                sku=12440,
                fesh=27101,
                blue_offers=[
                    BlueOffer(
                        feedid=100204,
                        offerid='offer_blue_12440_1',
                        price=150,
                        waremd5='offer_blue_12440_1__mQ',
                        title='offer sku 12440 1',
                    ),
                    BlueOffer(
                        feedid=100205,
                        offerid='offer_blue_12440_2',
                        price=120,
                        waremd5='offer_blue_12440_2__mQ',
                        title='offer sku 12440 2',
                    ),
                ],
            ),
            MarketSku(
                hyperid=1556,
                sku=15560,
                fesh=27101,
                blue_offers=[
                    BlueOffer(
                        feedid=100208,
                        offerid='offer_blue_15560_1',
                        price=150,
                        waremd5='offer_blue_15560_1__mQ',
                        title='offer sku 15560 1',
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                offerid='offer_model_1242_1',
                waremd5='offer_model_1242_1__mQ',
                title='offer model 1242 1',
                hyperid=1242,
                fesh=27081,
                price=150,
            ),
            Offer(
                offerid='offer_no_model___1',
                waremd5='offer_no_model___1__mQ',
                title='offer model 1242 2',
                fesh=27091,
                price=125,
            ),
            Offer(
                offerid='offer_model_1242_3',
                waremd5='offer_model_1242_3__mQ',
                title='offer model 1242 3',
                hyperid=1242,
                fesh=27101,
                price=175,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[42401],
            ),
            Offer(
                offerid='offer_white_12430_3',
                waremd5='offer_white_12430_3_mQ',
                title='offer sku 12430 3',
                hyperid=1243,
                sku=12430,
                fesh=27102,
                price=175,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[42401],
            ),
            Offer(
                offerid='offer_white_no_sku',
                waremd5='offer_white_no_sku__mQ',
                title='offer no sku',
                hyperid=1243,
                fesh=27103,
                price=100,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[42401],
            ),
            Offer(
                offerid='offer_no_fast_mapp',
                waremd5='offer_no_fast_mapp__mQ',
                title='offer no fast mappings',
                hyperid=1555,
                fesh=27103,
                price=100,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[42401],
            ),
            Offer(
                offerid='offer_virtual_card',
                waremd5='offer_virtual_card__mQ',
                title='offer for virtual card',
                virtual_model_id=300400,
                fesh=27103,
                price=100,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[42401],
            ),
            Offer(
                offerid='пюрешка',
                waremd5='offer_pure_kotleta__mQ',
                title='пюрешка',
                fesh=27103,
                price=150,
            ),
            Offer(
                offerid='offer_model_100439187587',
                waremd5='offer_model_huge____mQ',
                title='offer model 100439187587',
                fesh=27103,
                price=150,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=42401,
                fesh=27101,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

        # Добавляем быстрые маппинги на модели
        cls.fast_mappings.add_fast_model_mapping(1242, MappedOfferInfo(feed_id=100200, offer_id='offer_model_1242_1'))
        cls.fast_mappings.add_fast_model_mapping(1242, MappedOfferInfo(feed_id=100201, offer_id='offer_no_model___1'))
        cls.fast_mappings.add_fast_model_mapping(12420, MappedOfferInfo(feed_id=100200, offer_id='offer_model_1242_1'))
        cls.fast_mappings.add_fast_model_mapping(12421, MappedOfferInfo(feed_id=100201, offer_id='offer_no_model___1'))
        cls.fast_mappings.add_fast_model_mapping(12422, MappedOfferInfo(feed_id=100202, offer_id='offer_model_1242_3'))
        cls.fast_mappings.add_fast_model_mapping(12423, MappedOfferInfo(feed_id=100207, offer_id='пюрешка'))
        cls.fast_mappings.add_fast_model_mapping(
            100439187587, MappedOfferInfo(feed_id=100207, offer_id='offer_model_100439187587')
        )

        cls.fast_mappings.add_fast_model_mapping(100500, MappedOfferInfo(feed_id=100200, offer_id='offer_model_1242_1'))
        cls.fast_mappings.add_fast_model_mapping(100500, MappedOfferInfo(feed_id=100204, offer_id='offer_blue_12440_1'))
        cls.fast_mappings.add_fast_model_mapping(100500, MappedOfferInfo(feed_id=100202, offer_id='offer_blue_12430_1'))
        cls.fast_mappings.add_fast_model_mapping(100500, MappedOfferInfo(feed_id=100207, offer_id='offer_white_no_sku'))
        cls.fast_mappings.add_fast_model_mapping(100500, MappedOfferInfo(feed_id=100203, offer_id='offer_blue_12430_2'))

        cls.fast_mappings.add_fast_model_mapping(100501, MappedOfferInfo(feed_id=100202, offer_id='offer_blue_12430_1'))
        cls.fast_mappings.add_fast_model_mapping(100502, MappedOfferInfo(feed_id=100203, offer_id='offer_blue_12430_2'))
        cls.fast_mappings.add_fast_model_mapping(100503, MappedOfferInfo(feed_id=100204, offer_id='offer_blue_12440_1'))
        cls.fast_mappings.add_fast_model_mapping(100504, MappedOfferInfo(feed_id=100205, offer_id='offer_blue_12440_2'))
        cls.fast_mappings.add_fast_model_mapping(
            100505, MappedOfferInfo(feed_id=100206, offer_id='offer_white_12430_3')
        )
        cls.fast_mappings.add_fast_model_mapping(100506, MappedOfferInfo(feed_id=100207, offer_id='offer_white_no_sku'))

        # Добавляем быстрые маппинги на скю
        cls.fast_mappings.add_fast_sku_mapping(12430, MappedOfferInfo(feed_id=100202, offer_id='offer_blue_12430_1'))
        cls.fast_mappings.add_fast_sku_mapping(12430, MappedOfferInfo(feed_id=100203, offer_id='offer_blue_12430_2'))

        cls.fast_mappings.add_fast_sku_mapping(12431, MappedOfferInfo(feed_id=100207, offer_id='offer_white_no_sku'))
        cls.fast_mappings.add_fast_sku_mapping(12431, MappedOfferInfo(feed_id=100203, offer_id='offer_blue_12430_2'))

        cls.fast_mappings.add_fast_sku_mapping(12440, MappedOfferInfo(feed_id=100204, offer_id='offer_blue_12440_1'))
        cls.fast_mappings.add_fast_sku_mapping(12440, MappedOfferInfo(feed_id=100202, offer_id='offer_blue_12430_1'))

        cls.fast_mappings.add_fast_sku_mapping(12432, MappedOfferInfo(feed_id=100204, offer_id='offer_blue_12440_1'))
        cls.fast_mappings.add_fast_sku_mapping(12432, MappedOfferInfo(feed_id=100202, offer_id='offer_blue_12430_1'))
        cls.fast_mappings.add_fast_sku_mapping(12432, MappedOfferInfo(feed_id=100207, offer_id='offer_white_no_sku'))

        cls.fast_mappings.add_fast_sku_mapping(1005010, MappedOfferInfo(feed_id=100202, offer_id='offer_blue_12430_1'))
        cls.fast_mappings.add_fast_sku_mapping(1005020, MappedOfferInfo(feed_id=100203, offer_id='offer_blue_12430_2'))
        cls.fast_mappings.add_fast_sku_mapping(1005030, MappedOfferInfo(feed_id=100204, offer_id='offer_blue_12440_1'))
        cls.fast_mappings.add_fast_sku_mapping(1005040, MappedOfferInfo(feed_id=100205, offer_id='offer_blue_12440_2'))
        cls.fast_mappings.add_fast_sku_mapping(1005050, MappedOfferInfo(feed_id=100206, offer_id='offer_white_12430_3'))
        cls.fast_mappings.add_fast_sku_mapping(1005060, MappedOfferInfo(feed_id=100207, offer_id='offer_white_no_sku'))

    def test_fast_mappings_model(self):
        '''
        Теперь будет способ быстро запросить актуальные маппинги кокретных моделей и мскю
        Нужно, чтобы в productoffers они применялись для офферов
        Подробно тут и в родительском тикете: https://st.yandex-team.ru/MARKETOUT-46637
        '''

        # Изначально у модели всего 2 оффера
        response = self.report.request_json('place=productoffers&hyperid=1242&offers-set=list')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_1__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_3__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # Если простро включить флаг, то ничего не поменяется
        flags = '&rearr-factors=market_fast_mappings=100'
        response = self.report.request_json('place=productoffers&hyperid=1242&offers-set=list' + flags)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_1__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_3__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # Если добавить только верный pp (карточки), то тоже ничего не меняется
        use_fast_mapping = '&pp=6'
        response = self.report.request_json('place=productoffers&hyperid=1242&offers-set=list' + use_fast_mapping)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_1__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_3__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # Если модель не проходит фильтр (1242 % 100 > 40) то тоже ничего не меняется
        flags = '&rearr-factors=market_fast_mappings=40'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&hyperid=1242&offers-set=default,list' + use_fast_mapping + flags
        )
        response = self.report.request_json('place=productoffers&hyperid=1242&offers-set=list' + flags)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_1__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_3__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        # Теперь запрашиваем быстрый маппинг, там будут оффера offer_model_1242_1__mQ и offer_no_model___1__mQ
        # Тк offer_no_model___1__mQ дешевле, он будет ДО
        flags = '&rearr-factors=market_fast_mappings=100'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&hyperid=1242&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_model_1242_1__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_no_model___1__mQ',
                        "model": {
                            "id": 1242,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_no_model___1__mQ',
                        "model": {
                            "id": 1242,
                        },
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Попробуем с каждым оффером (офферам соответствует моделька)
        flags = '&rearr-factors=market_fast_mappings=100'
        models = [12420, 12421, 12422, 12423, 100439187587]

        wares = [
            'offer_model_1242_1__mQ',
            'offer_no_model___1__mQ',
            'offer_model_1242_3__mQ',
            'offer_pure_kotleta__mQ',
            'offer_model_huge____mQ',
        ]
        for model_id, ware_md5 in zip(models, wares):
            use_fast_mapping = '&pp=6'
            response = self.report.request_json(
                'place=productoffers&hyperid={}&offers-set=default,list'.format(model_id) + use_fast_mapping + flags
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': ware_md5,
                            "model": {
                                "id": model_id,
                            },
                            "benefit": {"type": "default"},
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_fast_mappings_sku(self):
        # Сначала идут запросы только с sku
        # Простой запрос
        response = self.report.request_json('place=productoffers&market-sku=12430&offers-set=default,list')
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_1__mQ', "sku": "12430"},
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_2__mQ', "sku": "12430"},
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_12430_3_mQ',
                        "sku": "12430",
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_12430_3_mQ',
                        "sku": "12430",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Сначала проверяем что скю фильтруется флагом (12430 % 100 > 20)
        flags = '&rearr-factors=market_fast_mappings=20'
        use_fast_mapping = '&pp=6&debug=1'
        response = self.report.request_json(
            'place=productoffers&market-sku=12430&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_1__mQ', "sku": "12430"},
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_2__mQ', "sku": "12430"},
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_12430_3_mQ',
                        "sku": "12430",
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_12430_3_mQ',
                        "sku": "12430",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # В быстрых маппингах у скю 12430 только оффера offer_blue_12430_1 и offer_blue_12430_2__mQ
        flags = '&rearr-factors=market_fast_mappings=100'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&market-sku=12430&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_1__mQ', "sku": "12430"},
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_2__mQ', "sku": "12430"},
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_12430_2__mQ',
                        "sku": "12430",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # проверяеем что pp=21 (OFFERS_WITH_RELEVANCE) тоже работает
        flags = '&rearr-factors=market_fast_mappings=100'
        use_fast_mapping = '&pp=21'
        response = self.report.request_json(
            'place=productoffers&market-sku=12430&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_1__mQ', "sku": "12430"},
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_2__mQ', "sku": "12430"},
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_12430_2__mQ',
                        "sku": "12430",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # У скю 12431 оффера offer_white_no_sku и offer_blue_12430_2
        flags = '&rearr-factors=market_fast_mappings=100'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&market-sku=12431&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': 'offer_white_no_sku__mQ', "sku": "12431"},
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_2__mQ', "sku": "12431"},
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_no_sku__mQ',
                        "sku": "12431",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # У скю 12440 в быстрых маппингах оффера offer_blue_12440_1, offer_blue_12430_1
        flags = '&rearr-factors=market_fast_mappings=100'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&market-sku=12440&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': 'offer_blue_12440_1__mQ', "sku": "12440"},
                    {'entity': 'offer', 'wareId': 'offer_blue_12430_1__mQ', "sku": "12440"},
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_12440_1__mQ',
                        "sku": "12440",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # У скю 12432 в быстрых маппингах оффера offer_blue_12440_1, offer_blue_12430_1 и offer_white_no_sku
        # Так же важно, что у модельки 100500 офферов в быстрых маппингах больше, но они не будут искаться
        flags = '&rearr-factors=market_fast_mappings=100'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&hyperid=100500&market-sku=12432&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_12440_1__mQ',
                        "model": {
                            "id": 100500,
                        },
                        "sku": "12432",
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_12430_1__mQ',
                        "model": {
                            "id": 100500,
                        },
                        "sku": "12432",
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_no_sku__mQ',
                        "model": {
                            "id": 100500,
                        },
                        "sku": "12432",
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_white_no_sku__mQ',
                        "model": {
                            "id": 100500,
                        },
                        "sku": "12432",
                        "benefit": {"type": "default"},
                    },
                ]
            },
            allow_different_len=False,
        )

        # Пройдемся по всем офферам
        flags = '&rearr-factors=market_fast_mappings=100'
        models = [
            100501,
            100502,
            100503,
            100504,
            100505,
            100506,
        ]
        skus = [
            1005010,
            1005020,
            1005030,
            1005040,
            1005050,
            1005060,
        ]
        wares = [
            'offer_blue_12430_1__mQ',
            'offer_blue_12430_2__mQ',
            'offer_blue_12440_1__mQ',
            'offer_blue_12440_2__mQ',
            'offer_white_12430_3_mQ',
            'offer_white_no_sku__mQ',
        ]
        for model_id, sku_id, ware_md5 in zip(models, skus, wares):
            use_fast_mapping = '&pp=6'
            response = self.report.request_json(
                'place=productoffers&hyperid={}&market-sku={}&offers-set=default,list'.format(model_id, sku_id)
                + use_fast_mapping
                + flags
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': ware_md5,
                            "model": {
                                "id": model_id,
                            },
                            "sku": str(sku_id),
                            "benefit": {"type": "default"},
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_fast_mappings_with_simple_mappings(self):
        '''
        Может быть такое, что у сервиса быстрых маппингов пустые маппинги для карточки - это ок (например, все маппинги удалили)
        Но запрошенный айдишник может быть виртуальным (такие мы не храним в сервисе)
        Для таких карточек все должно работать без патча быстрых маппингов
        '''

        # Одиночная карточка модели без быстрых маппингов
        flags = '&rearr-factors=market_fast_mappings=100;market_cards_everywhere_range=300100:300900;use_fast_cards=1'
        use_fast_mapping = '&pp=6'
        response = self.report.request_json(
            'place=productoffers&hyperid=1555&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {'results': []},
            allow_different_len=False,
        )

        # Одиночная карточка скю без быстрых маппингов
        response = self.report.request_json(
            'place=productoffers&hyperid=1556&market-sku=15560&offers-set=default,list' + use_fast_mapping + flags
        )
        self.assertFragmentIn(
            response,
            {'results': []},
            allow_different_len=False,
        )

        # Одиночная виртуальная карточка
        for req_body in ['&hyperid=300400', '&hyperid=300400&market-sku=300400', '&market-sku=300400']:
            response = self.report.request_json(
                'place=productoffers{}&offers-set=default,list'.format(req_body) + use_fast_mapping + flags
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'offer_virtual_card__mQ',
                            "benefit": {"type": "default"},
                        },
                    ]
                },
                allow_different_len=False,
            )

        # Карточка с быстрыми маппингами, обычная без быстрых, виртуальная
        response = self.report.request_json(
            'place=productoffers&hyperid=1242,300400,1555&offers-set=default,list&use_multiple_hyperid=1'
            + use_fast_mapping
            + flags
        )
        self.assertFragmentIn(
            response,
            {
                "modelIdWithOffers": [
                    {
                        "model_id": 1242,
                        "offers": [
                            {
                                'entity': 'offer',
                                'wareId': 'offer_model_1242_1__mQ',
                                "model": {
                                    "id": 1242,
                                },
                            },
                            {
                                'entity': 'offer',
                                'wareId': 'offer_no_model___1__mQ',
                                "model": {
                                    "id": 1242,
                                },
                            },
                            {
                                'entity': 'offer',
                                'wareId': 'offer_no_model___1__mQ',
                                "model": {
                                    "id": 1242,
                                },
                                "benefit": {"type": "default"},
                            },
                        ],
                    },
                    {
                        "model_id": 300400,
                        "offers": [
                            {
                                'entity': 'offer',
                                'wareId': 'offer_virtual_card__mQ',
                                "benefit": {"type": "default"},
                                "model": {
                                    "id": 300400,
                                },
                            },
                        ],
                    },
                    {
                        "model_id": 1555,
                        "offers": NoKey("offers"),
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_fast_cards(cls):
        cls.index.shops += [
            Shop(fesh=12708, priority_region=213),
            Shop(fesh=12709, priority_region=213),
            Shop(fesh=12710, datafeed_id=14240, priority_region=213, regions=[213], client_id=12, cpa=Shop.CPA_REAL),
            Shop(
                fesh=12711,
                datafeed_id=14241,
                priority_region=213,
                regions=[213],
                client_id=13,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(fesh=12712, datafeed_id=14242, priority_region=213, regions=[213], client_id=14, cpa=Shop.CPA_REAL),
        ]

        cls.index.offers += [
            Offer(
                fesh=12708,
                price=165,
                waremd5='offer_cpc_vmid_fc0__mQ',
                title='Оффер быстрокарточки 1580 cpc - 0',
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12709,
                price=175,
                waremd5='offer_cpc_vmid_fc1__mQ',
                title='Оффер быстрокарточки 1580 cpc - 1',
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12710,
                waremd5='offer_cpa_vmid_fc0__mQ',
                title='Оффер быстрокарточки 1580 cpa - 0',
                price=150,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14240],
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12712,
                waremd5='offer_cpa_vmid_fc1__mQ',
                title='Оффер быстрокарточки 1580 cpa - 1',
                price=155,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[14242],
                sku=1580,
                virtual_model_id=1580,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12711,
                waremd5='offer_blue_vmid_fc0_mQ',
                title='Оффер быстрокарточки 1580 blue - 0',
                price=250,
                delivery_buckets=[14241],
                sku=1580,
                virtual_model_id=1580,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
            Offer(
                fesh=12711,
                waremd5='offer_blue_vmid_fc1_mQ',
                title='Оффер быстрокарточки 1580 blue - 1',
                price=350,
                delivery_buckets=[14241],
                sku=1580,
                virtual_model_id=1580,
                blue_without_real_sku=True,
                vmid_is_literal=False,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=14240,
                fesh=12710,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14241,
                fesh=12711,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=14242,
                fesh=12712,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]

    def test_fast_cards_product_offers(self):
        '''
        Появились новые типы вуртуальных карточек - Быстрые карточки
        Их айдишник неотлечим от sku и лежит в нем же
        Но у офферов с скюшкой быстрых карточек в extraData лежит VirtualModelId == sku
        А литерала vmid нет

        Проверям их работу для списка офферов и расчета дефолтного оффера
        Под флагом: use_fast_cards

        TODO: кажется, нужно добавить тесты на несколько айдишников в запросе
        '''

        # Делаем простой запрос в productoffers за всеми офферами
        flags = '&rearr-factors=use_fast_cards=1'
        response = self.report.request_json('place=productoffers&hyperid=1580&offers-set=list' + flags)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'offer_cpc_vmid_fc0__mQ',
                        "model": {
                            "id": 1580,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_cpc_vmid_fc1__mQ',
                        "model": {
                            "id": 1580,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_cpa_vmid_fc0__mQ',
                        "model": {
                            "id": 1580,
                        },
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_cpa_vmid_fc1__mQ',
                        "model": {
                            "id": 1580,
                        },
                        "benefit": Absent(),
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_vmid_fc0_mQ',
                        "model": {
                            "id": 1580,
                        },
                        "offerColor": "blue",
                    },
                    {
                        'entity': 'offer',
                        'wareId': 'offer_blue_vmid_fc1_mQ',
                        "model": {
                            "id": 1580,
                        },
                        "offerColor": "blue",
                    },
                ]
            },
            allow_different_len=False,
        )

        # Добавляем к запросу расчет ДО
        offers_1580_with_do = [
            {
                'entity': 'offer',
                'wareId': 'offer_cpc_vmid_fc0__mQ',
                "model": {
                    "id": 1580,
                },
            },
            {
                'entity': 'offer',
                'wareId': 'offer_cpc_vmid_fc1__mQ',
                "model": {
                    "id": 1580,
                },
            },
            {
                'entity': 'offer',
                'wareId': 'offer_cpa_vmid_fc0__mQ',
                "model": {
                    "id": 1580,
                },
                "benefit": {"type": "default"},
            },
            {
                'entity': 'offer',
                'wareId': 'offer_cpa_vmid_fc1__mQ',
                "model": {
                    "id": 1580,
                },
            },
            {
                'entity': 'offer',
                'wareId': 'offer_blue_vmid_fc0_mQ',
                "model": {
                    "id": 1580,
                },
                "offerColor": "blue",
            },
            {
                'entity': 'offer',
                'wareId': 'offer_blue_vmid_fc1_mQ',
                "model": {
                    "id": 1580,
                },
                "offerColor": "blue",
            },
        ]

        response = self.report.request_json('place=productoffers&hyperid=1580&offers-set=default,list' + flags)
        self.assertFragmentIn(
            response,
            {'results': offers_1580_with_do},
        )

        # Запрос c айдишником быстрой карточки вместо мску
        response = self.report.request_json('place=productoffers&market-sku=1580&offers-set=default,list' + flags)
        self.assertFragmentIn(
            response,
            {'results': offers_1580_with_do},
        )

        # Запрос hyperid и msku равным vmid
        response = self.report.request_json(
            'place=productoffers&hyperid=1580&market-sku=1580&offers-set=default,list' + flags
        )
        self.assertFragmentIn(
            response,
            {'results': offers_1580_with_do},
        )

        # без флага - пустая выдача
        response = self.report.request_json(
            'place=productoffers&hyperid=1580&offers-set=list&rearr-factors=use_fast_cards=0'
        )
        self.assertFragmentIn(
            response,
            {'results': []},
            allow_different_len=False,
        )

    @classmethod
    def prepare_cpa_auction_with_vendor(cls):
        cls.index.shops += [
            Shop(fesh=10001, priority_region=213, regions=[255], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=10002, priority_region=213, regions=[255], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=10003, priority_region=213, regions=[255], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.offers += [
            Offer(fesh=10001, price=15000, hyperid=10501, cpa=Offer.CPA_REAL, fee=1000),
            Offer(fesh=10002, price=15000, hyperid=10501, cpa=Offer.CPA_REAL, fee=500),
            Offer(
                fesh=10003,
                price=15000,
                hyperid=10501,
                cpa=Offer.CPA_REAL,
                fee=200,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=111,
                    datasource_id=1,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=100),
                ),
            ),
        ]

    def test_cpa_auction_with_vendor_baseline(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=10501&rearr-factors=market_money_use_vendor_cpa_bid_on_model_card=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 10001}},
                {"shop": {"id": 10002}},
                {"shop": {"id": 10003}},
            ],
            preserve_order=True,
        )

        self.click_log.expect(ClickType.CPA, position=1, shop_fee=1000, shop_fee_ab=501)
        self.click_log.expect(ClickType.CPA, position=2, shop_fee=500, shop_fee_ab=201)
        self.click_log.expect(ClickType.CPA, position=3, shop_fee=200, shop_fee_ab=0)

    def test_cpa_auction_with_vendor(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=10501&rearr-factors=market_money_use_vendor_cpa_bid_on_model_card=1;market_money_vendor_cpc_to_cpa_conversion_prices=0.05'
        )
        self.assertFragmentIn(
            response,
            [
                {"shop": {"id": 10001}},
                {"shop": {"id": 10003}},
                {"shop": {"id": 10002}},
            ],
            preserve_order=True,
        )

        self.click_log.expect(ClickType.CPA, position=1, shop_fee=1000, shop_fee_ab=601)
        self.click_log.expect(ClickType.CPA, position=2, shop_fee=200, shop_fee_ab=101)
        self.click_log.expect(ClickType.CPA, position=3, shop_fee=500, shop_fee_ab=0)

    def test_do_not_fail_for_not_exists_sku(self):
        """Взяла просто запрос из прода со скухой которой нет в индексе
        кроме &market-sku в запросе отсутствуют &hyperid и &hid
        и поскольку скухи нет то их не удастся восстановить,
        а также невозможно использовать статистики и пр.
        Проверяем что ничто не падает
        """

        response = self.report.request_json(
            'place=productoffers&hid_by_hyper_id=1&hid-by-market-sku=1&combinator=1&cpa=real&viewtype=list&offer-set=default,defaultList,listCpa'
            '&show-credits=1&rids=213&with-rebuilt-model=1&enableMultiOffers=true&market-sku=101250910179&do-waremd5=YSH4gEonHmmF1FJzeU4tXw'
            '&how=dpop&numdoc=1&page=1&show-urls=cpa&enable_multioffer=1&debug=da'
        )

        # в частности была проблема с модельными статистиками
        self.assertFragmentIn(response, "InitRequestedModels(): RequestModels: []")
        self.assertFragmentIn(response, "RequestModels.empty(): 1 Cgi.DisableDynamicModelStats().Get(): 0")
        self.assertFragmentNotIn(response, {'reqwizardText': Contains("hyper_id:\"0\"")})

        # запрашиваем реальную существующую скуху
        response = self.report.request_json(
            'place=productoffers&hid_by_hyper_id=1&hid-by-market-sku=1&combinator=1&cpa=real&viewtype=list&offer-set=default,defaultList,listCpa'
            '&show-credits=1&rids=213&with-rebuilt-model=1&enableMultiOffers=true&market-sku=1&do-waremd5=MarketSku4-IiLVm1goleg'
            '&how=dpop&numdoc=1&page=1&show-urls=cpa&enable_multioffer=1&debug=da'
        )

        self.assertFragmentIn(response, "InitRequestedModels(): RequestModels: [1]")
        self.assertFragmentIn(response, "RequestModels.empty(): 0 Cgi.DisableDynamicModelStats().Get(): 0")
        self.assertFragmentIn(response, {'reqwizardText': Contains("hyper_id:\"1\"")})
        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Contains('ActualizeHyperId(): Actualize &hyperid if need'),
                    Contains('AddHyperAndHid(): Add &hyperid and &hid if not exists'),
                    Contains('AddHyperAndHid(): For market-sku=1 we need to add &hyperid (1) or &hid (1)'),
                    Contains('AddHyperAndHid(): Found msku by market-sku=1'),
                    Contains('Add hyperid=1 for market-sku=1'),
                    Contains('Add hid=11567 for market-sku=1'),
                ]
            },
        )

        self.assertFragmentIn(response, {'search': {'total': 1}})

    '''
    Проверяем, что при расхождении в хидах указание nid в запросе не приведет к скрытию оффера
    '''

    def test_nid_filter(self):
        request = 'place=productoffers&hyperid=1005000&nid=100'
        rearr_value = '&rearr-factors=market_skip_nid_filter_in_product_offers={}'
        skip_nid_flag_values = [0, 1]
        # Ожидаемый ответ, оффер не должен отфильтровываться
        target_response = {
            "search": {
                "total": 1,
                "totalOffers": 1,
                "totalPassedAllGlFilters": 1,
                "results": [{"entity": "offer", "titles": {"raw": "Nid filter test offer"}}],
            }
        }
        # Выключенный флаг возвращает старую логику, оффер будет отфильтровываться
        empty_response = {"search": {"total": 0, "totalOffers": 0}}
        # Проверяем, что флаг по умолчанию включен, оффер виден
        response = self.report.request_json(request)
        self.assertFragmentIn(response, target_response)
        # Проверяем, что оффер виден с включенным флагом и скрывается при выключенном флаге
        request = request + rearr_value
        for skip_nid_flag in skip_nid_flag_values:
            current_request = request.format(skip_nid_flag)
            expected_response = target_response if skip_nid_flag else empty_response
            response = self.report.request_json(current_request)
            self.assertFragmentIn(response, expected_response)
        # проверяем, что в других плейсах фильтр работает
        prime_request = 'place=prime&hyperid=10050000&nid=100'
        self.assertFragmentIn(self.report.request_json(prime_request), empty_response)
        # проверяем, что в таком же запросе, но без nid, оффер найден
        prime_request_non_empty = 'place=prime&hyperid=1005000'
        self.assertFragmentIn(self.report.request_json(prime_request_non_empty), target_response)


if __name__ == '__main__':
    main()
