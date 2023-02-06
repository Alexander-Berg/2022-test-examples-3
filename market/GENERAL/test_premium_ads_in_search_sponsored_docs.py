#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Model,
    UngroupedModel,
    MnPlace,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.matcher import Contains, Absent, NotEmpty, Capture, ElementCount
from core.blender_bundles import get_supported_incuts_cgi, create_blender_bundles


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class BlenderConstCpaShopIncut:
    BUNDLE = '''
{
    "incut_places": ["Search"],
    "incut_positions": [1],
    "incut_viewtypes": ["Gallery", "PremiumRichSnippet"],
    "incut_ids": ["default", "cpa_shop_incut_rich_snippet"],
    "result_scores": [
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "Gallery",
            "incut_id": "default",
            "score": 0.75
        },
        {
            "incut_place": "Search",
            "row_position": 1,
            "incut_viewtype": "PremiumRichSnippet",
            "incut_id": "cpa_shop_incut_rich_snippet",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_PREMIUM_ADS" : {
        "search_type == text && client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_premium_ads.json"
        },
        "search_type == textless && client == frontend && platform == desktop" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == ANDROID" : {
            "bundle_name": "const_premium_ads.json"
        },
        "client == IOS" : {
            "bundle_name": "const_premium_ads.json"
        }
    }
}
"""


class T(TestCase):
    @classmethod
    def prepare_blender_bundles_config(cls):
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {
                "const_premium_ads.json": BlenderConstCpaShopIncut.BUNDLE,
            },
        )

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=1, home_region=213),
            DynamicWarehouseInfo(id=2, home_region=213),
            DynamicWarehouseInfo(id=3, home_region=213),
            DynamicWarehouseInfo(id=4, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=2,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=3,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=4,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[1, 2, 3, 4]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=5678,
                carriers=[157],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=6)]
                    ),
                ],
            ),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(1, 0),
                    WarehouseWithPriority(2, 1),
                    WarehouseWithPriority(3, 1),
                    WarehouseWithPriority(4, 0),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=3100 + i,
                datafeed_id=3100 + i,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=i % 4 + 1,
            )
            for i in range(1, 11)
        ] + [
            Shop(
                fesh=3100 + i,
                priority_region=213,
                cpa=Shop.CPA_REAL,
            )
            for i in range(21, 31)
        ]

        cls.index.models += [
            Model(
                hyperid=94301,
                hid=91013,
                title="Исходная модель 1",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=11,
                        title="Расхлопнутая модель 1.1",
                        key='94301_1',
                    ),
                    UngroupedModel(
                        group_id=12,
                        title="Расхлопнутая модель 1.2",
                        key='94301_2',
                    ),
                ],
            ),
            Model(
                hyperid=94302,
                hid=91013,
                title="Исходная модель 2",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=21,
                        title="Расхлопнутая модель 2.1",
                        key='94302_1',
                    ),
                    UngroupedModel(
                        group_id=22,
                        title="Расхлопнутая модель 2.2",
                        key='94302_2',
                    ),
                ],
            ),
            Model(
                hyperid=94303,
                hid=91013,
                title="Исходная модель 3",
            ),
            Model(
                hyperid=94304,
                hid=91013,
                title="Исходная модель 4",
            ),
            Model(
                hyperid=94305,
                hid=91013,
                title="Исходная модель 5",
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=1,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7501,
                        price=2100,
                        feedid=3101,
                        waremd5='OFF1_2100_SKU1_SUP01_Q',
                    ),
                    BlueOffer(
                        ts=7502,
                        price=2150,
                        fee=50,
                        feedid=3102,
                        waremd5='OFF2_2150_SKU1_SUP02_Q',
                    ),
                ],
                ungrouped_model_blue=11,
            ),
            MarketSku(
                hyperid=94301,
                delivery_buckets=[1234],
                sku=2,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7503,
                        price=2200,
                        fee=100,
                        feedid=3103,
                        waremd5='OFF3_2200_SKU2_SUP03_Q',
                    ),
                    BlueOffer(
                        ts=7504,
                        price=2300,
                        fee=200,
                        feedid=3104,
                        waremd5='OFF4_2300_SKU2_SUP04_Q',
                    ),
                ],
                ungrouped_model_blue=12,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=3,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7505,
                        price=2200,
                        fee=200,
                        feedid=3105,
                        waremd5='OFF5_2200_SKU3_SUP05_Q',
                    ),
                    BlueOffer(
                        ts=7506,
                        price=2300,
                        fee=0,
                        feedid=3106,
                        waremd5='OFF6_2300_SKU3_SUP06_Q',
                    ),
                ],
                ungrouped_model_blue=21,
            ),
            MarketSku(
                hyperid=94302,
                delivery_buckets=[1234],
                sku=4,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7507,
                        price=2100,
                        feedid=3107,
                        waremd5='OFF7_2100_SKU4_SUP07_Q',
                    ),
                ],
                ungrouped_model_blue=22,
            ),
            MarketSku(
                hyperid=94303,
                delivery_buckets=[1234],
                sku=5,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7508,
                        price=2100,
                        feedid=3108,
                        waremd5='OFF8_2100_SKU5_SUP08_Q',
                    ),
                ],
            ),
            MarketSku(
                hyperid=94304,
                delivery_buckets=[1234],
                sku=6,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7509,
                        price=2100,
                        feedid=3109,
                        waremd5='OFF9_2100_SKU6_SUP09_Q',
                    ),
                ],
            ),
            MarketSku(
                hyperid=94305,
                delivery_buckets=[1234],
                sku=7,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7510,
                        price=2100,
                        fee=275,
                        feedid=3110,
                        waremd5='OFF10_2100_SKU7_SUP10_',
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=94303,
                ts=7520,
                price=2100,
                fee=150,
                sku=5,
                title='dsbs offer 1',
                fesh=3121,
                cpa=Offer.CPA_REAL,
                waremd5='eGfWVXuC7TtdYeZZGiov0w',
            ),
            Offer(
                hid=91013,
                ts=7521,
                price=2100,
                fee=500,
                title='dsbs offer 2',
                fesh=3122,
                cpa=Offer.CPA_REAL,
                waremd5='KXGI8T3GP_pqjgdd7HfoHQ',
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 7501).respond(0.9)
            cls.matrixnet.on_place(place, 7502).respond(0.89)
            cls.matrixnet.on_place(place, 7503).respond(0.88)
            cls.matrixnet.on_place(place, 7504).respond(0.87)
            cls.matrixnet.on_place(place, 7505).respond(0.86)
            cls.matrixnet.on_place(place, 7506).respond(0.85)
            cls.matrixnet.on_place(place, 7507).respond(0.84)
            cls.matrixnet.on_place(place, 7508).respond(0.83)
            cls.matrixnet.on_place(place, 7509).respond(0.82)
            cls.matrixnet.on_place(place, 7510).respond(0.81)
            cls.matrixnet.on_place(place, 7520).respond(0.80)
            cls.matrixnet.on_place(place, 7521).respond(0.79)

    def test_docs_order_and_docs_count(self):
        # Проверяем порядок и кол-во документов в выдаче прайм
        # В выдаче prime присутствуют дубликаты между спонсорскими документами и поисковыми это нормально
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str) + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 12,  # 12 = 7(скю) + 1(оффер) + 4(спонсорских товара)
                    'totalModels': 7,
                    'totalOffers': 1,
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF10_2100_SKU7_SUP10w",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF4_2300_SKU2_SUP04_Q",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2200_SKU2_SUP03_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF9_2100_SKU6_SUP09_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF10_2100_SKU7_SUP10w",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "KXGI8T3GP_pqjgdd7HfoHQ",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_docs_order_and_docs_count_prohibit_duplicates(self):
        # Проверяем порядок и кол-во документов в выдаче прайм без дублей
        # В выдаче prime присутствуют дубликаты между спонсорскими документами и поисковыми это нормально
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str) + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 9,
                    'totalModels': 7,
                    'totalOffers': 1,
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF1_2100_SKU1_SUP01_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF10_2100_SKU7_SUP10w",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF4_2300_SKU2_SUP04_Q",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF3_2200_SKU2_SUP03_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF7_2100_SKU4_SUP07_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF9_2100_SKU6_SUP09_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "KXGI8T3GP_pqjgdd7HfoHQ",
                                        'urls': {
                                            'cpa': Contains('/shop_fee_ab=0/'),
                                        },
                                        'sponsored': Absent(),
                                    },
                                ]
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_equality_prime_sponsored_docs_and_cpa_shop_incut(self):
        # Проверяем что оффера из cpa_shop_incut попадают в спонсорские позиции поисковой выдачи
        # Запрашиваем выдачу cpa_shop_incut и сравниваем ее с документами с полем sponsored в выдаче prime
        # Должен совпадать порядок офферов и полность совпадать сами оффера кроме pp и position
        # Категорийный поиск
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        sponsored_doc_fee_1 = Capture()
        sponsored_doc_fee_2 = Capture()
        sponsored_doc_fee_3 = Capture()
        sponsored_doc_fee_4 = Capture()

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF10_2100_SKU7_SUP10w",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_1),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=275/', '/pp=231/', '/position=2/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF4_2300_SKU2_SUP04_Q",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_2),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=200/', '/pp=231/', '/position=3/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_3),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=200/', '/pp=231/', '/position=6/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_4),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=150/', '/pp=231/', '/position=7/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                    ]
                }
            },
        )

        # market_premium_ads_gallery_default_min_num_doc нужен чтобы проходить порог по минимуму документов в выдаче

        response = self.report.request_json(
            'place=cpa_shop_incut&hid=91013&pp=18&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': "OFF10_2100_SKU7_SUP10w",
                        'fee': sponsored_doc_fee_1.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=275/'),
                        },
                    },
                    {
                        'wareId': "OFF4_2300_SKU2_SUP04_Q",
                        'fee': sponsored_doc_fee_2.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=200/'),
                        },
                    },
                    {
                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                        'fee': sponsored_doc_fee_3.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=200/'),
                        },
                    },
                    {
                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                        'fee': sponsored_doc_fee_4.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=150/'),
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_equality_prime_sponsored_docs_and_cpa_shop_incut_prohibit_duplicates(self):
        # Проверяем что оффера из cpa_shop_incut попадают в спонсорские позиции поисковой выдачи
        # Запрашиваем выдачу cpa_shop_incut и сравниваем ее с документами с полем sponsored в выдаче prime
        # Должен совпадать порядок офферов и полность совпадать сами оффера кроме pp и position
        # Категорийный поиск
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        sponsored_doc_fee_1 = Capture()
        sponsored_doc_fee_2 = Capture()
        sponsored_doc_fee_3 = Capture()

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF10_2100_SKU7_SUP10w",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_1),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=275/', '/pp=231/', '/position=2/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "OFF4_2300_SKU2_SUP04_Q",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_2),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=200/', '/pp=231/', '/position=3/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                                        'fee': NotEmpty(capture=sponsored_doc_fee_3),
                                        'urls': {
                                            'cpa': Contains('/shop_fee=150/', '/pp=231/', '/position=6/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                    ]
                }
            },
        )

        # market_premium_ads_gallery_default_min_num_doc нужен чтобы проходить порог по минимуму документов в выдаче

        response = self.report.request_json(
            'place=cpa_shop_incut&hid=91013&pp=18&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': "OFF10_2100_SKU7_SUP10w",
                        'fee': sponsored_doc_fee_1.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=275/'),
                        },
                    },
                    {
                        'wareId': "OFF4_2300_SKU2_SUP04_Q",
                        'fee': sponsored_doc_fee_2.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=200/'),
                        },
                    },
                    {
                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                        'urls': {
                            'cpa': Contains('/shop_fee=200/'),
                        },
                    },
                    {
                        'wareId': "eGfWVXuC7TtdYeZZGiov0w",
                        'fee': sponsored_doc_fee_3.value,
                        'urls': {
                            'cpa': Contains('/shop_fee=150/'),
                        },
                    },
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_creation_sponsored_doc_in_prime(self):
        # Проверяем правильно ли запросилась модель для оффера из cpa_shop_incut и как заполнены ее поля
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=91013&place=prime&blender=1&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': "product",
                            'slug': "iskhodnaia-model-2",
                            'type': "model",
                            'id': 94302,
                            'modelCreator': "market",
                            'skuOffersCount': 2,
                            'skuPrices': {
                                'min': "2200",
                                'max': "2300",
                            },
                            'sponsored': True,
                            'prices': {
                                'min': "2100",
                                'max': "2200",
                            },
                            'offers': {
                                'items': [
                                    {
                                        'entity': "offer",
                                        'marketSkuCreator': "market",
                                        'wareId': "OFF5_2200_SKU3_SUP05_Q",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=200/', '/pp=231/', '/position=6/'),
                                        },
                                        'feeShowPlain': Contains("pp: 231"),
                                        'prices': {
                                            'rawValue': "2200",
                                        },
                                        'sku': "3",
                                        'benefit': {
                                            'type': "default",
                                            'description': "Первый офер, схлопнутый до модели",
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_many_white_offers(cls):
        cls.index.shops += [
            Shop(
                fesh=3150 + i,
                priority_region=213,
                cpa=Shop.CPA_REAL,
            )
            for i in range(1, 41)
        ]

        cls.index.models += [
            Model(
                hyperid=94350 + i,
                hid=91015,
                title="model_{}".format(94350 + i),
            )
            for i in range(1, 41)
        ]

        cls.index.offers += [
            Offer(
                hyperid=94350 + i,
                hid=91015,
                fesh=3150 + i,
                ts=7550 + i,
                price=1000,
                title='Dishwasher Product#{}'.format(7550 + i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 11)
        ] + [
            Offer(
                hyperid=94350 + i,
                hid=91015,
                fesh=3150 + i,
                ts=7550 + i,
                price=1000,
                fee=i * 10,
                title='Dishwasher Product#{}'.format(7550 + i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(11, 41)
        ]

        for i in range(1, 41):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7550 + i).respond(0.5)

    def test_premium_ads_and_prime_together(self):
        # Проверяем что первые топN документов из cpa_shop_incut идут в трафареты в prime, а оставшиеся во врезку
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 3,  # спонсорские позиции: { 2, 3, 6, 7, 16, 17, 26, 27, 36, 37, 47, 48 }
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_blender_bundles_for_inclid": "2:const_premium_ads.json",
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&text=dishwasher&place=prime&blender=1&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&client=frontend&platform=desktop'
            '&supported-incuts={}&rearr-factors={}'.format(get_supported_incuts_cgi(), rearr_flags_str)
        )

        # сначала из офферов cpa_shop_incut набираются трафареты
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'slug': "dishwasher-product-7590",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=400/', '/pp=231/', '/position=2/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'slug': "dishwasher-product-7589",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=390/', '/pp=231/', '/position=3/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        # ...
                        {
                            'offers': {
                                'items': [
                                    {
                                        'slug': "dishwasher-product-7580",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=300/', '/pp=231/', '/position=47/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                        {
                            'offers': {
                                'items': [
                                    {
                                        'slug': "dishwasher-product-7579",
                                        'urls': {
                                            'cpa': Contains('/shop_fee=290/', '/pp=231/', '/position=48/'),
                                        },
                                        'sponsored': True,
                                    },
                                ]
                            },
                        },
                    ]
                }
            },
            preserve_order=True,
        )

        # только после того как набраны трафареты оставшиеся оффера из cpa_shop_incut идут в премиальную врезку
        self.assertFragmentIn(
            response,
            {
                'incuts': {
                    'results': [
                        {
                            'entity': 'searchIncut',
                            'incutId': 'default',
                            'inClid': 2,
                            'items': [
                                {
                                    'slug': "dishwasher-product-7578",
                                },
                                {
                                    'slug': "dishwasher-product-7577",
                                },
                                {
                                    'slug': "dishwasher-product-7576",
                                },
                                # ...
                            ],
                        },
                    ],
                },
            },
            preserve_order=True,
        )

    def test_request_appears_in_debug(self):
        # Проверяем, что в дебаге легко найти подзапрос в place=cpa_shop_incut
        rearr_flags_dict = {
            # TODO: почистить флаги, когда выкатятся в прод
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_blender_bundles_for_inclid": "2:const_premium_ads.json",
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&text=dishwasher&place=prime&blender=1&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&client=frontend&platform=desktop'
            '&supported-incuts={}&rearr-factors={}'.format(get_supported_incuts_cgi(), rearr_flags_str)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(response, "Perform additional request to place=NMarketReport::TCPAShopIncut:")

    @classmethod
    def prepare_sponsored_docs_with_regional_delimeter(cls):
        cls.index.shops += [
            Shop(
                fesh=3250 + i,
                priority_region=300,
                cpa=Shop.CPA_REAL,
            )
            for i in range(1, 4)
        ] + [Shop(fesh=3250 + i, priority_region=400, cpa=Shop.CPA_REAL, regions=[300]) for i in range(4, 7)]

        cls.index.models += [
            Model(
                hyperid=94450 + i,
                hid=91025,
                title="model_{}".format(94450 + i),
            )
            for i in range(1, 7)
        ]

        cls.index.offers += [
            Offer(
                hyperid=94450 + i,
                hid=91025,
                fesh=3250 + i,
                ts=7650 + i,
                price=1000,
                title='nokia#{}'.format(7650 + i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 4)
        ] + [
            Offer(
                hyperid=94450 + i,
                hid=91025,
                fesh=3250 + i,
                ts=7650 + i,
                price=1000,
                fee=i * 100,
                title='nokia#{}'.format(7650 + i),
                cpa=Offer.CPA_REAL,
            )
            for i in range(4, 7)
        ]

        for i in range(1, 11):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7650 + i).respond(0.5 - i * 0.01)

    def test_premium_sponsored_docs_with_regional_delimeter(self):
        # Проверяем как работает региональный разделитель для спонсорских доков набранных из премиальной
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 2,  # спонсорские позиции: [2, 3, 6, 7, 11, 12, ...]
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 0,
        }

        request = (
            'place=prime&text=nokia&rids=300&pp=7&show-urls=external,decrypted,direct%2Ccpa&bsformat=2'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rgb=green_with_blue&blender=1'
        )

        # случай когда региональный разделитель учитывает спонсорские оффера в выдаче
        rearr_flags_dict["market_regional_delimiter_ignore_sponsored_docs"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(request + '&rearr-factors={}'.format(rearr_flags_str))

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"offers": {"items": [{"titles": {"raw": "nokia#7651"}}]}},
                    {"entity": "regionalDelimiter"},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7656"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7655"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7652"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7653"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7654"}, "sponsored": True}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # случай когда региональный разделитель не учитывает спонсорские оффера в выдаче
        rearr_flags_dict["market_regional_delimiter_ignore_sponsored_docs"] = 1
        rearr_flags_dict["market_premium_ads_in_search_sponsored_places_allow_duplicates"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(request + '&rearr-factors={}'.format(rearr_flags_str))

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"offers": {"items": [{"titles": {"raw": "nokia#7651"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7656"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7655"}, "sponsored": True}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7652"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7653"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7654"}, "sponsored": True}]}},
                    {"entity": "regionalDelimiter"},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7654"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7655"}}]}},
                    {"offers": {"items": [{"titles": {"raw": "nokia#7656"}}]}},
                ]
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_duplicates_resolve_with_demux(self):
        """
        Проверяем, что можно запустить демукс для разрешения дублей между трафаретами и премиальной врезкой
        При этом в трафареты попадает топ документов из cpa_shop_incut, а в премиальную часть документов
        из топа дублируется, а часть берётся из хвоста
        """
        rearr_flags_dict = {
            "market_report_mimicry_in_serp_pattern": 3,  # спонсорские позиции: { 2, 3, 6, 7, 16, 17, 26, 27, 36, 37, 47, 48 }
            "market_buybox_auction_search_sponsored_places_web": 0,
            "market_premium_ads_incut_get_docs_through_prime": 1,
            "market_premium_ads_in_search_sponsored_places_web": 1,
            "market_white_search_auction_cpa_fee": 1,
            "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
            "market_blender_cpa_shop_incut_enabled": 1,
            "market_blender_use_bundles_config": 1,
            "market_blender_bundles_for_inclid": "2:const_premium_ads.json",
            # Включаем демукс и функциональность для использования с трафаретами
            "market_premium_ads_reuse_search_sponsored_offers_in_demux": 1,
            "market_cpa_shop_incut_premium_ads_use_demux": 1,
            # кол-во первых позиций трафаретов, которые нельзя дублировать
            "cpa_shop_incut_demux_banned_offers_cnt_from_mimic": 3,
            # левая и правая границы для интерполяции иниц. вероятностей в демуксе
            "cpa_shop_incut_demux_init_prob_left": 0.3,
            "cpa_shop_incut_demux_init_prob_right": 1.0,
            # для воспроизводимости
            "market_cpa_shop_incut_premium_ads_demux_random_seed": 123,
            "market_premium_ads_in_search_sponsored_places_allow_duplicates": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&text=dishwasher&place=prime&blender=1&rgb=green_with_blue&rids=213'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&viewtype=list'
            '&use-default-offers=1&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&client=frontend&platform=desktop'
            '&supported-incuts={}&rearr-factors={}'.format(get_supported_incuts_cgi(), rearr_flags_str)
        )

        response = self.report.request_json(request)

        # Готовимся парсить выдачу
        search_sponsored_cnt = 12  # Всего в выдаче 12 трафаретов
        incut_offers_cnt = 10  # Во врезке 10 офферов
        search_sponsored_offers = [Capture() for _ in range(search_sponsored_cnt)]
        incut_offers = [Capture() for _ in range(incut_offers_cnt)]

        # Парсим выдачу
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": NotEmpty(capture=capture),
                                        "sponsored": True,
                                    },
                                ],
                            },
                        }
                        for capture in search_sponsored_offers
                    ],
                },
                "incuts": {
                    "results": [
                        {
                            "incutId": "default",
                            "items": [
                                {
                                    "wareId": NotEmpty(capture=capture),
                                }
                                for capture in incut_offers
                            ],
                        },
                    ],
                },
            },
        )

        # Проверяем кол-во офферов в премиальной врезке (то, что их не больше 10)
        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            "incutId": "default",
                            "items": ElementCount(10),
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

        # Превращаем capture-list в str-list
        search_sponsored_offers = [capture.value for capture in search_sponsored_offers]
        incut_offers = [capture.value for capture in incut_offers]

        # Проверяем, что демукс отработал правильно
        # cpa_shop_incut_demux_banned_offers_cnt_from_mimic=3 - первые 3 трафарета в премиальную не попали
        for offer in search_sponsored_offers[:3]:
            self.assertTrue(offer not in incut_offers)

        # Проверяем, что дубли между трафаретами и премиальной врезкой всё же есть
        has_duplicate = False
        for offer in incut_offers:
            if offer in search_sponsored_offers:
                has_duplicate = True
                break
        self.assertTrue(has_duplicate)

        # Проверяем, что вероятностный механизм работает - после 3-го трафарета не все трафареты дублируются
        # И это же означает, что в премиальную попадают какие-то офферы из хвоста
        has_hole = False
        for offer in incut_offers:
            if offer not in search_sponsored_offers:
                has_hole = True
                break
        self.assertTrue(has_hole)

        # Проверяем краевое условие на интерполяцию вероятностей
        # cpa_shop_incut_demux_init_prob_right=1 - значит, последний из трафаретов есть в премиальной
        self.assertTrue(search_sponsored_offers[-1] in incut_offers)


if __name__ == '__main__':
    main()
