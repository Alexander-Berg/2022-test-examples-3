#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CategoryRestriction,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    Disclaimer,
    DynamicDeliveryServiceInfo,
    DynamicMarketSku,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    MnPlace,
    Model,
    ModelDescriptionTemplates,
    Offer,
    OfferDimensions,
    Opinion,
    Picture,
    Region,
    RegionalDelivery,
    RegionalModel,
    RegionalRestriction,
    Shop,
    Tax,
    UngroupedModel,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.matcher import Contains, Absent, NotEmpty, EqualToOneOf, Greater, Regex, EmptyDict
from core.types.picture import thumbnails_config
from core.types.relevance_tweaker_data import RelevanceTweakRecord
import datetime
import calendar


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            "market_money_disable_bids=0",
            'market_new_cpm_iterator=0',
            'market_filter_offers_with_model_without_sku=0',
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.index.shops += [Shop(fesh=100, priority_region=213, regions=[213])]

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

    @classmethod
    def prepare_fesh_literal(cls):
        cls.index.shops += [
            Shop(fesh=1007, priority_region=213, regions=[213]),
            Shop(fesh=1008, priority_region=213, regions=[213]),
        ]

        cls.index.mskus += [
            MarketSku(sku=1408666, fesh=1007, hid=1999, title='this sku', waremd5='1000000000111000000001'),
            MarketSku(sku=1108556, fesh=1007, hid=1999, title='that sku', waremd5='1000000000222000000001'),
            MarketSku(sku=1005001, fesh=1008, hid=1999, title='wrong sku', waremd5='1000000003330000000002'),
        ]

        cls.index.offers += [
            Offer(sku=1408666, fesh=1007, hid=1999, title='this sku offer', waremd5='1011100000000000000003'),
            Offer(sku=1108556, hid=1999, title='that sku offer', waremd5='1022200000000000000003'),
            Offer(sku=1005001, fesh=1008, hid=1999, title='wrong offer', waremd5='1033300000000000000004'),
        ]

    def titles_raw_frag(self, title):
        return {
            'results': [
                {'titles': {'raw': Contains(title)}},
            ]
        }

    def test_fesh_literal(self):
        request = 'place=prime&hid=1999&debug=da&fesh=1007'
        request += '&rearr-factors=market_metadoc_search=skus'
        request += '&rearr-factors=enable_sku_literals=1'

        literal_frag = {
            'debug': {'report': {'context': {'collections': {'SHOP': {'text': [Contains('yx_ds_id:\"1007\"')]}}}}},
        }

        response = self.report.request_json(request)

        self.assertFragmentIn(response, self.titles_raw_frag('this'), allow_different_len=False)
        self.assertFragmentNotIn(response, self.titles_raw_frag('that'))
        self.assertFragmentNotIn(response, self.titles_raw_frag('wrong'))
        self.assertFragmentIn(response, literal_frag, allow_different_len=True)

    @classmethod
    def prepare_offers_by_sku(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=1,
                regions=[213],
                datafeed_id=1,
            ),
            Shop(
                fesh=2,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=2,
                regions=[213],
                datafeed_id=2,
            ),
            Shop(
                fesh=3,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=3,
                regions=[213],
                datafeed_id=3,
            ),
            Shop(
                fesh=4,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=4,
                regions=[213],
                datafeed_id=4,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=11,
                hid=1,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="Расхлопнутая модель 11.0", key='11_0'),
                    UngroupedModel(group_id=2, title="Расхлопнутая модель 11.1", key='11_1'),
                    UngroupedModel(group_id=3, title="Расхлопнутая модель 11.2", key='11_2'),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                sku=1,
                title='скучная горилла 1',
                hid=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                hyperid=11,
                delivery_buckets=[1234],
                glparams=[GLParam(param_id=101, value=1)],
                ungrouped_model_blue=1,
                blue_offers=[
                    BlueOffer(
                        price=4,
                        waremd5='0BpxoGSFbboWM0nz7BHD8Q',
                        ts=102,
                        title='скучная горилла синий оффер 1',
                        feedid=1,
                        fesh=1,
                        download=True,
                    ),
                    BlueOffer(
                        price=3,
                        waremd5='jtz2CzErBm3oY90EJVeFSg',
                        title='скучная горилла синий оффер 2',
                        feedid=2,
                        fesh=2,
                        download=True,
                    ),
                    BlueOffer(
                        price=2,
                        waremd5='Evvwyq6asEysEt-QSbBPxg',
                        title='скучная горилла синий оффер 3',
                        feedid=3,
                        fesh=3,
                        download=True,
                    ),
                    BlueOffer(
                        price=1,
                        waremd5='gwgfZ3JflzQelx9tFVgDqQ',
                        title='скучная горилла синий оффер 4',
                        feedid=4,
                        fesh=4,
                        has_gone=True,
                        ts=101,
                        download=True,
                    ),
                ],
            ),
            MarketSku(
                sku=2,
                title='скучная горилла 2',
                hid=1,
                fesh=100,
                glparams=[GLParam(param_id=101, value=1)],
                ungrouped_model_blue=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                hyperid=11,
            ),
            MarketSku(
                sku=3,
                title='скучная горилла 3',
                hid=1,
                fesh=100,
                glparams=[GLParam(param_id=101, value=2)],
                ungrouped_model_blue=3,
                waremd5='Sku3-wdDXWsIiLVm1goleg',
                hyperid=11,
            ),
        ]

        cls.index.offers += [
            # Also sku=1, but from cpc-shard
            Offer(
                sku=1,
                price=5,
                title='скучная горилла оффер 11',
                glparams=[GLParam(param_id=101, value=1)],
                ts=100,
                waremd5='22222222222222gggggggg',
                hid=1,
                fesh=100,
                hyperid=11,
                ungrouped_model_blue=1,
            ),
            Offer(
                sku=1,
                price=6,
                title='скучная горилла оффер 12',
                glparams=[GLParam(param_id=101, value=1)],
                waremd5='09lEaAKkQll1XTgggggggg',
                hid=1,
                fesh=100,
                hyperid=11,
                ungrouped_model_blue=1,
            ),
            Offer(
                sku=3,
                price=6,
                title='скучная горилла оффер 31',
                glparams=[GLParam(param_id=101, value=2)],
                hyperid=11,
                waremd5='DuE098x_rinQLZn3KKrELw',
                hid=1,
                fesh=100,
                ungrouped_model_blue=3,
            ),
            Offer(
                sku=3,
                price=5,
                title='скучная горилла оффер 32',
                glparams=[GLParam(param_id=101, value=2)],
                hyperid=11,
                waremd5='_qQnWXU28-IUghltMZJwNw',
                hid=1,
                fesh=100,
                ungrouped_model_blue=3,
            ),
            Offer(
                sku=3,
                price=4,
                title='скучная горилла оффер 33',
                glparams=[GLParam(param_id=101, value=2)],
                hyperid=11,
                waremd5='RPaDqEFjs1I6_lfC4Ai8jA',
                hid=1,
                fesh=100,
                ungrouped_model_blue=3,
            ),
            Offer(
                sku=3,
                price=3,
                title='скучная горилла оффер 34',
                glparams=[GLParam(param_id=101, value=2)],
                hyperid=11,
                waremd5='22222222222222gggg401g',
                hid=1,
                fesh=100,
                ungrouped_model_blue=3,
            ),
            Offer(
                sku=3,
                price=2,
                title='скучная горилла оффер 35',
                glparams=[GLParam(param_id=101, value=2)],
                hyperid=11,
                ts=10,
                waremd5='22222222222222gggg501g',
                hid=1,
                fesh=100,
                ungrouped_model_blue=3,
            ),
            Offer(
                sku=3,
                price=1,
                offerid='skuchnaya_gorilla_36',
                title='скучная горилла оффер 36',
                glparams=[GLParam(param_id=101, value=2)],
                hyperid=11,
                waremd5='22222222222222gggg404g',
                hid=1,
                fesh=100,
                ungrouped_model_blue=3,
            ),
            # no sku
            Offer(
                price=7,
                title='не скучная горилла оффер 01',
                glparams=[GLParam(param_id=101, value=1)],
                waremd5='8GaN0stIZ5AJ4Oe_0SK3qQ',
                hid=1,
                fesh=100,
            ),
            Offer(
                price=8,
                title='не скучная горилла оффер 02',
                glparams=[GLParam(param_id=101, value=2)],
                waremd5='FRNxd7-S67VQQyexo4gQqA',
                hid=1,
                fesh=100,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 102).respond(0.9)

    def test_offers_by_sku(self):
        """
        Проверяем, что работает нагребание офферов по найденным ску
        """
        q_no_filters = (
            'place=prime&text=скучная+горилла&debug=da&rids=213&'
            'rearr-factors=market_metadoc_search=offers;'
            'market_blue_buybox_max_price_rel_add_diff=0;'
            'market_relevance_formula_threshold=0.2;'
            'market_metadoc_effective_pruncount=10&debug-doc-count=100&'
            'allow-collapsing=1&use-default-offers=1&local-offers-first=1'
        )

        white_shard_flag = '&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_search_in_white_offer_shard_all_cpa_docs={}'

        for white_shard_flag_value in (
            '',
            white_shard_flag.format('0'),
            white_shard_flag.format('1'),
        ):
            q = q_no_filters + white_shard_flag_value
            response = self.report.request_json(q)

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'не скучная горилла оффер 01'},
                            'debug': {'metadoc': Absent()},
                        },
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'не скучная горилла оффер 02'},
                            'debug': {'metadoc': Absent()},
                        },
                        {
                            'entity': 'product',
                            'id': 11,
                            'offers': {'items': [{'sku': '1'}]},
                            # 'скучная горилла синий оффер 4' is gone
                            # 3 and 2 have lower warehouse priority
                            # so we pick 'скучная горилла синий оффер 1' as buybox
                            # it has wareId = 0BpxoGSFbboWM0nz7BHD8Q
                            'debug': {
                                'wareId': '0BpxoGSFbboWM0nz7BHD8Q',
                                'metadoc': {
                                    'sku': '1',
                                    'childWareId': '0BpxoGSFbboWM0nz7BHD8Q',
                                },
                            },
                        },
                        {
                            'entity': 'product',
                            'id': 11,
                            'offers': {'items': [{'sku': '3'}]},
                            # 'скучная горилла оффер 36' has the lowest price
                            # it has wareId = 22222222222222gggg404g
                            'debug': {
                                'wareId': '22222222222222gggg404g',
                                'metadoc': {
                                    'sku': '3',
                                    'childWareId': '22222222222222gggg404g',
                                },
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

        q = q_no_filters + white_shard_flag.format('1')

        # with &ignore-has-gone offer 4 from sku 1 will be found
        # it has wareId = gwgfZ3JflzQelx9tFVgDqQ

        response = self.report.request_json(q + '&ignore-has-gone=1')

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 11,
                'offers': {'items': [{'sku': '1'}]},
                'debug': {
                    'wareId': 'gwgfZ3JflzQelx9tFVgDqQ',
                    'metadoc': {
                        'sku': '1',
                        'childWareId': 'gwgfZ3JflzQelx9tFVgDqQ',
                    },
                },
            },
        )

        response = self.report.request_json(q + '&hid=1&glfilter=101:1')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'не скучная горилла оффер 01'},
                        'debug': {'metadoc': Absent()},
                    },
                    {
                        'entity': 'product',
                        'id': 11,
                        'offers': {'items': [{'sku': '1'}]},
                        'debug': {
                            'wareId': '0BpxoGSFbboWM0nz7BHD8Q',
                            'metadoc': {
                                'sku': '1',
                                'childWareId': '0BpxoGSFbboWM0nz7BHD8Q',
                            },
                        },
                    },
                ]
            },
            allow_different_len=False,
        )

        self.dynamic.market_dynamic.disabled_market_sku += [
            DynamicMarketSku(supplier_id=100, shop_sku='skuchnaya_gorilla_36'),
        ]

        response = self.report.request_json(q)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'не скучная горилла оффер 01'},
                        'debug': {'metadoc': Absent()},
                    },
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'не скучная горилла оффер 02'},
                        'debug': {'metadoc': Absent()},
                    },
                    {
                        'entity': 'product',
                        'id': 11,
                        'offers': {'items': [{'sku': '1'}]},
                        'debug': {
                            'wareId': '0BpxoGSFbboWM0nz7BHD8Q',
                            'metadoc': {
                                'sku': '1',
                                'childWareId': '0BpxoGSFbboWM0nz7BHD8Q',
                            },
                        },
                    },
                    # {
                    # 'скучная горилла оффер 36' is filtered by dynamic
                    # 'скучная горилла оффер 35' is selected.
                    # But it is filtered later by relevance threshold;
                    # this filter doesn't work at selecton, so sku=3
                    # is filtered completely
                    # },
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'brief': {
                        'filters': {
                            'RELEVANCE_THRESHOLD': 1,
                            'CHILDLESS_METADOC': Greater(0),
                        }
                    },
                    'basesearch': {
                        'documents': [
                            {
                                'properties': {
                                    'METADOC_SKU': '3',
                                    'CHILD_WAREMD5': '22222222222222gggg501g',
                                    'METADOC_FILTERING': 'FILTERED_BY_UNIFIED_HIDE_RULE_SERVICE: 1',
                                    'DROP_REASON': 'RELEVANCE_THRESHOLD',
                                }
                            },
                            {
                                'properties': {
                                    'METADOC_SKU': '2',
                                    'DROP_REASON': 'CHILDLESS_METADOC',
                                }
                            },
                        ]
                    },
                },
            },
        )

    @classmethod
    def prepare_literals(cls):
        cls.index.models += [
            Model(hyperid=100, hid=2),
            Model(hyperid=101, hid=2),
            Model(hyperid=102, hid=2),
        ]

        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[213]),
            Shop(fesh=11, priority_region=213, regions=[213]),
            Shop(
                fesh=12,
                priority_region=213,
                regions=[213],
                warehouse_id=1,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                datafeed_id=12,
            ),
            Shop(
                fesh=13,
                priority_region=213,
                regions=[213],
                warehouse_id=1,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                datafeed_id=13,
            ),
        ]

        cls.index.mskus += [
            MarketSku(sku=10, title='скучающий сурикат', hid=2, hyperid=100),
            MarketSku(
                sku=11,
                title='скучающая синица',
                hid=2,
                blue_offers=[
                    BlueOffer(
                        price=1, title='скучающая синица 1', feedid=12, waremd5='sku-11-blue-1-poq57hRg', hyperid=101
                    ),
                    BlueOffer(
                        price=2, title='скучающая синица 2', feedid=13, waremd5='sku-11-blue-2-poq57hRg', hyperid=101
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(
                title='скучающий сурикат 0',
                hid=2,
                fesh=10,
                sku=10,
                price=1,
                waremd5='sku-10-white-0-oq57hRg',
                hyperid=100,
            ),
            Offer(
                title='скучающий сурикат 1',
                hid=2,
                fesh=11,
                sku=10,
                price=2,
                waremd5='sku-10-white-1-oq57hRg',
                hyperid=100,
            ),
            Offer(title='весёлый сурикат 0', hid=2, fesh=10, waremd5='no-sku-white-0-oq57hRg'),
            Offer(title='весёлый сурикат 1', hid=2, fesh=11, waremd5='no-sku-white-1-oq57hRg', hyperid=102),
        ]

    def test_literals(self):
        """
        Проверяем, что работают литералы уровней ску и офферов на тексте
        и бестексте
        Проверяем, что для некоторых особых фильтров метадоковый поиск
        отключается
        """

        q_no_filters = (
            'place=prime&text=сурикат&debug=da&rids=213&'
            'rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_metadoc_search=offers&'
            'allow-collapsing=1&use-default-offers=1'
        )

        response = self.report.request_json(q_no_filters)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'sku-10-white-0-oq57hRg'},
                        'offers': {'items': [{'sku': '10'}]},
                    },
                    {'titles': {'raw': 'весёлый сурикат 0'}},
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'no-sku-white-1-oq57hRg'},
                        'offers': {'items': [{'sku': Absent()}]},
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(q_no_filters + '&fesh=11')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'sku-10-white-1-oq57hRg'},
                        'offers': {'items': [{'sku': '10'}]},
                    },
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'no-sku-white-1-oq57hRg'},
                        'offers': {'items': [{'sku': Absent()}]},
                    },
                ]
            },
            allow_different_len=False,
        )

        q_blue_no_filters = (
            'place=prime&text=синица&debug=da&rids=213&'
            'rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_metadoc_search=offers&'
            'allow-collapsing=1&use-default-offers=1'
        )

        response = self.report.request_json(q_blue_no_filters)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'sku-11-blue-1-poq57hRg'},
                        'offers': {'items': [{'sku': '11'}]},
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(q_blue_no_filters + '&supplier_type=1')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'sku-11-blue-2-poq57hRg'},
                        'offers': {'items': [{'sku': '11'}]},
                    },
                ]
            },
            allow_different_len=False,
        )

        q_textless_no_filters = (
            'place=prime&hid=2&debug=da&rids=213&'
            'rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_metadoc_search=offers&'
            'entities=offer&'  # There are also tests with model search
            'allow-collapsing=1&use-default-offers=1'
        )

        response = self.report.request_json(q_textless_no_filters)

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'sku-10-white-0-oq57hRg'},
                        'offers': {'items': [{'sku': '10'}]},
                    },
                    {'titles': {'raw': 'весёлый сурикат 0'}},
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'no-sku-white-1-oq57hRg'},
                        'offers': {'items': [{'sku': Absent()}]},
                    },
                    {
                        'entity': 'product',
                        'debug': {'wareId': 'sku-11-blue-1-poq57hRg'},
                        'offers': {'items': [{'sku': '11'}]},
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=сурикат&debug=da&rids=213&'
            'rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_metadoc_search=offers&'
            'offerid=sku-10-white-0-oq57hRg'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': 'sku-10-white-0-oq57hRg',
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_offers_models_output(cls):
        cls.index.shops += [
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name='Один 1P поставщик',
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=99, datafeed_id=999, priority_region=213, regions=[213]),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1001),
            Model(hyperid=2, hid=1001, title='айфон модель 2'),
            Model(hyperid=3, hid=1002),
        ]

        cls.index.offers += [
            # model#1 default offer
            Offer(hyperid=1, fesh=99, waremd5='otENNVzevIeeT8bsxvY91w', ts=1),
            # model#1 collapsed offer
            Offer(hyperid=1, title='айфон оффер 101', fesh=99, waremd5='jooaMuw3dToRIrcA3DG32A', ts=2),
            # without sku
            Offer(hyperid=2, fesh=99, waremd5='xzFUFhFuAvI1sVcwDnxXPQ', ts=3),
            # with sku
            Offer(hyperid=3, price=9000, fesh=99, waremd5='V5Y7eJkIdDh0sMeCecijqw', sku=901, ts=4),
            Offer(hyperid=3, price=8000, fesh=99, waremd5='BH8EPLtKmdLQhLUasgaOnA', sku=901, ts=5),
            Offer(
                hyperid=3, price=7000, title='айфон оффер901', fesh=99, waremd5='uqb4K8RseBZosGXlOs8MVw', sku=901, ts=6
            ),
            # unmatched offer
            Offer(title='несматченный айфон оффер 102', fesh=99, waremd5='R444Pv6gPSRso7ok6xDEAw'),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 4).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 5).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 6).respond(0.9)

        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=1002,
                friendlymodel=['model friendly {sku_filter}'],
                model=[('Основное', {'model full': '{sku_filter}'})],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                hid=1002,
                param_id=701,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]

        picture1 = Picture(
            picture_id='IyC4nHslqLtqZJLygVAHe1',
            width=200,
            height=200,
            thumb_mask=thumbnails_config.get_mask_by_names(['200x200']),
        )
        cls.index.mskus += [
            MarketSku(
                hyperid=3,
                sku=901,
                picture=picture1,
                descr='ай да айфон',
                title='айфон sku901',
                waremd5='yRgmzyBD4j8r4rkCby6Iuw',
                glparams=[GLParam(param_id=701, value=2)],
            ),
        ]

    def test_offers_models_output(self):
        """модели с ДО со skuId + модели, найденные поиском + непоматченные офферы, почти как сейчас в проде"""

        response = self.report.request_json(
            'place=prime&text=айфон&rids=213&use-default-offers=1'
            '&allow-collapsing=1&show-models-specs=msku-friendly,msku-full'
            '&rearr-factors=market_metadoc_search=offers'
            '&numdoc=20'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # unmatched offers
                    {
                        'entity': 'offer',
                        'titles': {'raw': 'несматченный айфон оффер 102'},
                        'wareId': 'R444Pv6gPSRso7ok6xDEAw',
                    },
                    # found models
                    {
                        'entity': 'product',
                        'id': 2,
                        'titles': {'raw': 'айфон модель 2'},
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'xzFUFhFuAvI1sVcwDnxXPQ',  # default offer
                                }
                            ]
                        },
                    },
                    # sku data in offer
                    {
                        'id': 3,
                        'offers': {
                            'items': [
                                {
                                    'skuAwareTitles': {
                                        'raw': 'айфон sku901',
                                        'highlighted': [
                                            {'value': 'айфон', 'highlight': True},
                                            {'value': ' sku901'},
                                        ],
                                    },
                                    'skuAwarePictures': [{'original': {'width': 200}}],
                                    'skuAwareSpecs': {
                                        'friendly': ['model friendly value2'],
                                        'full': [
                                            {
                                                'groupName': 'Основное',
                                                'groupSpecs': [{'name': 'model full', 'value': 'value2'}],
                                            }
                                        ],
                                    },
                                    'wareId': 'uqb4K8RseBZosGXlOs8MVw',  # min price child offer
                                    'prices': {'value': '7000'},
                                }
                            ]
                        },
                    },
                    # collapsed to models
                    {
                        'entity': 'product',
                        'id': 1,
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'otENNVzevIeeT8bsxvY91w',
                                }
                            ]
                        },
                    },
                ]
            },
            preserve_order=False,
        )

        self.show_log.expect(ware_md5='R444Pv6gPSRso7ok6xDEAw')
        self.show_log.expect(hyper_id=1, original_ware_md5='jooaMuw3dToRIrcA3DG32A')
        self.show_log.expect(hyper_id=2)
        self.show_log.expect(ware_md5='uqb4K8RseBZosGXlOs8MVw')
        self.feature_log.expect(ware_md5='R444Pv6gPSRso7ok6xDEAw')
        self.feature_log.expect(ware_md5='uqb4K8RseBZosGXlOs8MVw')

    def test_skus_output(self):
        """скушки + непоскутченные офферы + найденные поиском (НЕ схлопнутые) модели"""

        request = (
            'place=prime&text=айфон&rids=213&use-default-offers=1'
            '&allow-collapsing=1'
            '&show-models-specs=msku-friendly,msku-full'
            '&rearr-factors=market_metadoc_search=skus'
            '&numdoc=20&blender=1'
        )

        response = self.report.request_json(request)

        descr = 'ай да айфон'
        _ = 'айфон оффер901'
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # skus
                    {
                        'entity': 'sku',
                        'titles': {'raw': 'айфон sku901'},
                        'slug': 'aifon-sku901',
                        'categories': [
                            {
                                'entity': 'category',
                                'id': 1002,
                            }
                        ],
                        'description': descr,
                        'formattedDescription': {
                            'fullHtml': descr,
                            'fullPlain': descr,
                            'shortHtml': descr,
                            'shortPlain': descr,
                        },
                        'id': '901',
                        'isAdult': False,
                        'specs': {
                            'friendly': ['model friendly value2'],
                            'full': [
                                {'groupName': 'Основное', 'groupSpecs': [{'name': 'model full', 'value': 'value2'}]}
                            ],
                        },
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'uqb4K8RseBZosGXlOs8MVw',  # min price child offer
                                    'prices': {'value': '7000'},
                                }
                            ]
                        },
                        'showUid': Regex("[0-9]{26}"),
                    },
                    # offers without sku
                    {
                        'entity': 'offer',
                        'wareId': 'R444Pv6gPSRso7ok6xDEAw',
                    },
                    # found models
                    {
                        'entity': 'product',
                        'id': 2,
                        'titles': {'raw': 'айфон модель 2'},
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'wareId': 'xzFUFhFuAvI1sVcwDnxXPQ',
                                }
                            ]
                        },
                    },
                    # collapsed to models, no skus
                    {
                        'entity': 'product',
                        'id': 1,
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'otENNVzevIeeT8bsxvY91w',
                                }
                            ]
                        },
                    },
                ]
            },
            allow_different_len=True,
            preserve_order=False,
        )

        self.show_log.expect(ware_md5='R444Pv6gPSRso7ok6xDEAw')
        self.show_log.expect(hyper_id=1, original_ware_md5='jooaMuw3dToRIrcA3DG32A')
        self.show_log.expect(hyper_id=2)
        self.show_log.expect(ware_md5='uqb4K8RseBZosGXlOs8MVw')
        self.feature_log.expect(ware_md5='R444Pv6gPSRso7ok6xDEAw')
        self.feature_log.expect(ware_md5='uqb4K8RseBZosGXlOs8MVw')

        response = self.report.request_json(
            request
            + (
                '&rearr-factors=market_report_blender_pattern_min_results_for_incuts=1'
                ';market_vendor_incut_hide_undeliverable_models=0'
                ';market_vendor_incut_enable_banners=1'
                ';market_report_blender_pattern_factors=,,5,,1'
                ';market_report_blender_pattern=0x0'
            )
        )

        self.assertFragmentIn(
            response,
            {
                'renderMap': {
                    'list': [
                        Regex('[0-9]{26}'),
                        Regex('[0-9]{26}'),
                        Regex('[0-9]{26}'),
                        Regex('[0-9]{26}'),
                    ]
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_metadoc_id_replacement(cls):
        cls.index.offers += [
            Offer(waremd5='7h3hnmks9hqpcnz5hn3mGw', sku=78431),
        ]
        cls.index.mskus += [
            MarketSku(
                hid=7843,
                sku=78431,
                # Hyper is necessary. https://st.yandex-team.ru/MARKETOUT-41842#6141f398bd29ea364553c7cb
                hyperid=78432,
                title='avocado',
                waremd5='y6djfurud7878adhfhe29w',  # must differ from offer's ware md5
            ),
        ]

    def test_metadoc_id_replacement(self):
        """
        Check that doc origin changes without metadoc id replacement.
        """
        request = (
            'hid=7843&text=avocado&place=prime&debug=da'
            '&rearr-factors=market_metadoc_search=skus;market_replace_metadoc_id_with_child_id={replace_doc_id}'
        )

        response_with_child_doc_id = self.report.request_json(request.format(replace_doc_id=1))
        self.assertFragmentIn(
            response_with_child_doc_id,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '78431',
                            'debug': {
                                'properties': {
                                    'METADOC_DOCID': NotEmpty(),
                                    'CHILD_DOCID': NotEmpty(),
                                },
                                'metadoc': {
                                    'sku': '78431',
                                    'childWareId': '7h3hnmks9hqpcnz5hn3mGw',
                                },
                                'tech': {
                                    'originId': NotEmpty(),
                                },
                            },
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

        sku_debug_info = response_with_child_doc_id['search']['results'][0]['debug']
        metadoc_docid = sku_debug_info['properties']['METADOC_DOCID']
        child_docid = sku_debug_info['properties']['CHILD_DOCID']

        # Check debug docids with metadocid replacement
        # With replacement origin doc must be child doc
        self.assertNotEqual(metadoc_docid, child_docid)
        self.assertFragmentIn(
            response_with_child_doc_id,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '78431',
                            'debug': {
                                'tech': {
                                    'originId': r'^\d+-{}$'.format(child_docid),
                                },
                            },
                        }
                    ]
                }
            },
            allow_different_len=False,
            use_regex=True,
        )

        # Without replacement origin doc must be metadoc
        response_with_metadoc_doc_id = self.report.request_json(request.format(replace_doc_id=0))
        self.assertFragmentIn(
            response_with_metadoc_doc_id,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '78431',
                            'debug': {
                                'properties': {
                                    'METADOC_DOCID': metadoc_docid,
                                    'CHILD_DOCID': child_docid,
                                },
                                'metadoc': {
                                    'sku': '78431',
                                    'childWareId': '7h3hnmks9hqpcnz5hn3mGw',
                                },
                                'tech': {
                                    'originId': r'^\d+-{}$'.format(metadoc_docid),
                                },
                            },
                        }
                    ]
                }
            },
            allow_different_len=False,
            use_regex=True,
        )

    @classmethod
    def prepare_metadoc_factors(cls):
        cls.index.offers += [
            Offer(
                price=7000,
                title='cucumber offer',
                waremd5='pPpxddR1fkXqxxqEh3UfYw',
                sku=565602,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=5656,
                hyperid=565601,
                sku=565602,
                title='cucumber sku',
                waremd5='uivb5hsjuehakxSSjakuaw',  # must differ from offer's ware md5
            ),
        ]

    def test_metadoc_factors(self):
        """
        Check that factors don't change without metadoc id replacement.
        """
        request = (
            'text=cucumber&hid=5656&place=prime&debug=da'
            '&rearr-factors=market_metadoc_search=skus;market_replace_metadoc_id_with_child_id={replace_doc_id}'
        )

        # Check that we calculate factors. Especially model/offer/dssm/ctr factors.
        response_with_metadoc_doc_id = self.report.request_json(request.format(replace_doc_id=0))
        factors_to_check = [
            'SHOP_ID',
            'OFFER_PRICE',
            'DOC_EMBEDDING_HARD2_0',
            'DOCUMENT_QUERY_CTR',
            'DSSM_BERT',
        ]
        self.assertFragmentIn(
            response_with_metadoc_doc_id,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '565602',
                            'debug': {
                                'factors': {factor: NotEmpty() for factor in factors_to_check},
                                'metadoc': {
                                    'sku': '565602',
                                    'childWareId': 'pPpxddR1fkXqxxqEh3UfYw',
                                },
                            },
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

        sku_factors = response_with_metadoc_doc_id['search']['results'][0]['debug']['factors']

        # Check that factors are the same with flag turned on.
        # NOTE: sku_factors are not the only factors in this repsponse.
        #       There are also meta factors but they are out of the scope of this test
        response_with_child_doc_id = self.report.request_json(request.format(replace_doc_id=1))
        self.assertFragmentIn(
            response_with_child_doc_id,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'sku',
                            'id': '565602',
                            'debug': {
                                'factors': sku_factors,
                                'metadoc': {
                                    'sku': '565602',
                                    'childWareId': 'pPpxddR1fkXqxxqEh3UfYw',
                                },
                            },
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    sale_date = (datetime.datetime.now() - datetime.timedelta(days=20)).date()

    @classmethod
    def prepare_models_output(cls):
        cls.index.shops += [
            Shop(fesh=85, priority_region=213, regions=[213]),
        ]

        cls.index.models += [
            Model(hyperid=85, hid=85, title='прицел msku'),
            Model(hyperid=86, hid=86, title='прицел без msku'),
        ]

        cls.index.offers += [
            # with sku
            Offer(hyperid=85, price=123, fesh=85, waremd5='fOSGhqNHNtNDAkpbybrbYR', sku=85, ts=1),
            # without sku
            Offer(hyperid=86, fesh=85, waremd5='OaVLOaXLous88wcMX5y705', ts=2),
        ]

        cls.index.mskus += [
            MarketSku(hyperid=85, sku=85),
        ]

    def test_models_without_sku_offers(self):
        """Под флагом находить только модели без ску офферов"""

        # with flag
        response = self.report.request_json(
            'place=prime&text=прицел&rids=213'
            '&rearr-factors=market_metadoc_search=offers;market_metadoc_search_models_without_sku_offers=1'
            '&numdoc=20'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 86,
                        'titles': {'raw': 'прицел без msku'},
                    },
                ]
            },
            preserve_order=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 85,
                        'titles': {'raw': 'прицел msku'},
                    },
                ]
            },
            preserve_order=False,
        )

        # without flag
        response = self.report.request_json(
            'place=prime&text=прицел&rids=213'
            '&rearr-factors=market_metadoc_search=offers;market_metadoc_search_models_without_sku_offers=0'
            '&numdoc=20'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'product',
                        'id': 86,
                        'titles': {'raw': 'прицел без msku'},
                    },
                    {
                        'entity': 'product',
                        'id': 85,
                        'titles': {'raw': 'прицел msku'},
                    },
                ]
            },
            preserve_order=False,
        )

    @classmethod
    def prepare_inherited_fields(cls):
        # TODO: add non-enum filters aggregation
        cls.index.gltypes += [
            GLType(
                param_id=1,
                hid=666,
                position=1,
                xslname='colorType',
                gltype=GLType.ENUM,
                subtype='color',
                cluster_filter=True,
            ),
            GLType(
                param_id=2,
                hid=666,
                position=2,
                xslname='packageType',
                gltype=GLType.NUMERIC,
                subtype='package',
                cluster_filter=True,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=666,
                hyperid=666666,
                title='SKU666',
                hid=666,
                ungrouped_model_blue=1,
                delivery_buckets=[1234],
            ),
            MarketSku(
                sku=777,
                hyperid=666666,
                title='SKU777',
                hid=666,
                ungrouped_model_blue=2,
                delivery_buckets=[1234],
            ),
            MarketSku(
                sku=888,
                hyperid=777777,
                title='SKU888',
                hid=666,
                delivery_buckets=[1234],
            ),
        ]
        sale_timestamp = calendar.timegm(cls.sale_date.timetuple())
        cls.index.models += [
            Model(
                hyperid=666666,
                title='MODEL666',
                hid=666,
                opinion=Opinion(rating=666, precise_rating=666.666, rating_count=6660),
                new=True,
                sale_begin_ts=sale_timestamp,
                ts=8001,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="расхлопнутая модель 666666.1", key='666666_1'),
                    UngroupedModel(group_id=2, title="расхлопнутая модель 666666.2", key='666666_2'),
                ],
            ),
            Model(
                hyperid=777777,
                title='MODEL777',
                hid=666,
                opinion=Opinion(rating=777, precise_rating=676.767, rating_count=6767),
                new=True,
                sale_begin_ts=sale_timestamp,
                ts=8002,
            ),
        ]
        cls.index.offers += [
            Offer(
                sku=666,
                hid=666,
                price=100500,
                hyperid=666666,
                title='OFFER666',
                ungrouped_model_blue=1,
                ts=8003,
                fesh=99,
                picture=Picture(width=100, height=100, group_id=12345),
                waremd5='xMpCOKC5I4INzFCab3WEmQ',
                glparams=[GLParam(param_id=1, value=6661), GLParam(param_id=2, value=6662)],
            ),
            Offer(
                sku=777,
                hid=666,
                price=500100,
                hyperid=666666,
                title='OFFER777',
                ungrouped_model_blue=2,
                ts=8004,
                fesh=99,
                picture=Picture(width=100, height=100, group_id=12345),
                waremd5='Y9_iwgA17yd3FCI0LRhC_w',
                glparams=[GLParam(param_id=1, value=7771), GLParam(param_id=2, value=7772)],
            ),
            Offer(
                sku=888,
                hid=666,
                price=500500,
                hyperid=777777,
                title='OFFER888',
                ts=8005,
                fesh=99,
                picture=Picture(width=100, height=100, group_id=12345),
                waremd5='Z9_iwgA17yd3FCI0LRhC_w',
                glparams=[GLParam(param_id=1, value=8881), GLParam(param_id=2, value=8882)],
            ),
        ]
        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition()
            .add(hyperid=666666, reasons=[{"why?": "good as hell"}])
            .add(hyperid=777777, reasons=[{"why?": "good as hell"}])
        ]
        cls.index.category_restrictions += [
            CategoryRestriction(
                name="Hell made",
                hids=[666],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        display_only_matched_offers=False,
                        delivery=True,
                        disclaimers=[Disclaimer(name='Temperature', text='Hot as hellfire', short_text='Very hot')],
                    ),
                ],
            )
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 8003).respond(0.9)
            cls.matrixnet.on_place(place, 8004).respond(0.9)
            cls.matrixnet.on_place(place, 8005).respond(0.9)

            cls.matrixnet.on_place(place, 8001).respond(0.7)
            cls.matrixnet.on_place(place, 8002).respond(0.7)

    def sku_entity(
        self,
        id,
        offer_title,
        filters_count,
        sale_date,
        total_count,
        min_price,
        max_price,
        precise_rating,
        rating_count,
        withDO,
        withFilters,
    ):
        frag = {
            'entity': 'sku',
            'id': str(id),
            'filters': [
                {
                    'xslname': 'colorType',
                    'valuesCount': filters_count,
                },
                # TODO(ants) fix these filters later
                # {
                #    'xslname' : 'packageType',
                # }
            ],
            'offers': {'items': [{'titles': {'raw': offer_title}}]},
            'preciseRating': precise_rating,
            'ratingCount': rating_count,
            'reasonsToBuy': [{'why?': 'good as hell'}],
            'warnings': {'common': [{'type': 'Temperature', 'value': {'full': 'Hot as hellfire'}}]},
            'skuOffersCount': 1,
            'skuPrices': {'currency': 'RUR', 'max': str(max_price), 'min': str(min_price)},
            'saleBeginDate': sale_date,
            'skuStats': {'totalCount': total_count},
        }
        if not withDO:
            del frag['offers']
            del frag['skuPrices']
            del frag['skuOffersCount']
        if not withFilters:
            del frag['filters']
        return frag

    def correct_response(self, withDO=False, withFilters=False):
        frag = {
            'search': {
                'results': [
                    self.sku_entity(
                        666,
                        'OFFER666',
                        2,
                        T.sale_date.isoformat(),
                        2,
                        100500,
                        100500,
                        666.666,
                        6660,
                        withDO,
                        withFilters,
                    ),
                    self.sku_entity(
                        777,
                        'OFFER777',
                        2,
                        T.sale_date.isoformat(),
                        2,
                        500100,
                        500100,
                        666.666,
                        6660,
                        withDO,
                        withFilters,
                    ),
                    self.sku_entity(
                        888,
                        'OFFER888',
                        1,
                        T.sale_date.isoformat(),
                        1,
                        500500,
                        500500,
                        676.767,
                        6767,
                        withDO,
                        withFilters,
                    ),
                ]
            }
        }
        return frag

    def test_inherited_fields(self):
        textless_request = 'place=prime&hid=666&rids=213&allow-collapsing=1'
        request = textless_request + '&text=SKU'
        # check that none of sku metadoc search specfic fields present in result
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, self.correct_response(withDO=True))
        self.assertFragmentNotIn(response, self.correct_response(withDO=False))

        # check that fields are correct when DO IS requested
        response = self.report.request_json(request + '&rearr-factors=market_metadoc_search=skus&use-default-offers=1')
        self.assertFragmentIn(response, self.correct_response(withDO=True, withFilters=True), allow_different_len=False)

        # check that fields are correct when DO IS NOT requested
        # TODO(ants) find out why doesn't this work with text
        # market_min_models_to_request чтобы в head не попадали модели иначе схлапывается все в модель
        response = self.report.request_json(
            textless_request
            + '&rearr-factors=market_metadoc_search=skus;market_min_models_to_request=0&use-default-offers=0'
        )
        self.assertFragmentIn(response, self.correct_response(withDO=False, withFilters=True), allow_different_len=True)

    @classmethod
    def prepare_skus_sorting_by_model_stats(cls):
        cls.index.models += [
            Model(hyperid=2801999, hid=196, title='MODEL1999', ts=101999),
            Model(hyperid=1152012, hid=196, title='MODEL2012', ts=102012),
            Model(hyperid=1202021, hid=196, title='MODEL2021', ts=102021),
            Model(hyperid=802020, hid=196, title='MODEL2020', ts=102020),
            Model(hyperid=10101969, hid=196, title='SKU0MODEL1969', ts=101969),
        ]

        cls.index.mskus += [
            MarketSku(sku=1000004, hyperid=2801999, hid=196, title='SKU1000001', ts=100001),
            MarketSku(sku=1000003, hyperid=1152012, hid=196, title='SKU1000002', ts=100002),
            MarketSku(sku=1000002, hyperid=1202021, hid=196, title='SKU1000003', ts=100003),
            MarketSku(sku=1000001, hyperid=802020, hid=196, title='SKU1000004', ts=100004),
        ]

        cls.index.offers += [
            Offer(sku=1000004, hid=196, price=1010, hyperid=2801999, title='OFFER2000001', ts=2999),
            Offer(sku=1000004, hid=196, price=102, hyperid=2801999, title='OFFER2000002', ts=999),
            Offer(sku=1000004, hid=196, price=103, hyperid=2801999, title='OFFER2000003', ts=1999),
            Offer(sku=1000003, hid=196, price=1010, hyperid=1152012, title='OFFER2000004', ts=3012),
            Offer(sku=1000003, hid=196, price=105, hyperid=1152012, title='OFFER2000005', ts=2012),
            Offer(sku=1000002, hid=196, price=1010, hyperid=1202021, title='OFFER2000006', ts=3021),
            Offer(sku=1000002, hid=196, price=107, hyperid=1202021, title='OFFER2000007', ts=4021),
            Offer(sku=1000002, hid=196, price=108, hyperid=1202021, title='OFFER2000008', ts=2021),
            Offer(sku=1000002, hid=196, price=109, hyperid=1202021, title='OFFER2000009', ts=1021),
            Offer(sku=1000001, hid=196, price=1010, hyperid=802020, title='OFFER2000010', ts=1020),
            Offer(sku=1000001, hid=196, price=112, hyperid=802020, title='OFFER2000011', ts=2020),
            Offer(sku=1000001, hid=196, price=113, hyperid=802020, title='OFFER2000012', ts=3020),
            Offer(hid=196, price=1010, hyperid=10101969, title='OFFER2000013', ts=2969),
            Offer(hid=196, price=114, hyperid=10101969, title='OFFER2000014', ts=969),
            Offer(hid=196, price=115, hyperid=10101969, title='OFFER2000015', ts=1969),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            for ts in [1969, 1999, 2012, 2020, 2021]:
                cls.matrixnet.on_place(place, ts).respond(0.9)
            for ts in [2999, 999, 3012, 3021, 4021, 1021, 1020, 3020, 2969, 969]:
                cls.matrixnet.on_place(place, ts).respond(0.1)

    def test_skus_sorting_by_model_stats(self):
        response = self.report.request_json(
            'place=prime&hid=196&rearr-factors=market_metadoc_search=skus&use-default-offers=1&text=SKU&how=aprice'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'sku', 'id': '1000004', 'offers': {'items': [{'titles': {'raw': 'OFFER2000003'}}]}},
                        {'entity': 'sku', 'id': '1000003', 'offers': {'items': [{'titles': {'raw': 'OFFER2000005'}}]}},
                        {'entity': 'sku', 'id': '1000002', 'offers': {'items': [{'titles': {'raw': 'OFFER2000008'}}]}},
                        {'entity': 'sku', 'id': '1000001', 'offers': {'items': [{'titles': {'raw': 'OFFER2000011'}}]}},
                        {
                            'entity': 'product',
                            'id': 10101969,
                            'offers': {'items': [{'titles': {'raw': 'OFFER2000015'}}]},
                        },
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_relevance_tweaking(cls):
        cls.index.relevance_tweaks_records += [RelevanceTweakRecord('#vendor_id="666', '10')]

        cls.index.models += [
            Model(hyperid=666007, hid=6607, title='MODEL', ts=67),
        ]

        cls.index.mskus += [
            MarketSku(sku=777118, vendor_id=666, hyperid=666007, hid=6607, title='BOOSTED SKU', ts=78),
        ]

        cls.index.offers += [
            Offer(sku=777118, hid=6607, price=100500, hyperid=666007, title='OFFER', ts=89),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 89).respond(0.9)

    def test_relevance_tweaking(self):
        tweakReq = 'place=prime&hid=6607&rearr-factors=market_metadoc_search=skus&use-default-offers=1&text=BOOSTED&debug=da&rearr-factors=market_enable_relevance_tweaking=%d'
        response = self.report.request_json(tweakReq % 0)
        docRel = int(response.root['search']['results'][0]['debug']['docRel'])
        response = self.report.request_json(tweakReq % 1)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [{'debug': {'docRel': str(docRel * int(self.index.relevance_tweaks_records[0].coef))}}]
                }
            },
        )

    @classmethod
    def prepare_offer_counts(cls):
        cls.index.models += [
            Model(hyperid=141197, hid=111, title='MODEL141197'),
            Model(hyperid=101017, hid=111, title='MODEL101017'),
            Model(hyperid=130276, hid=111, title='MODEL130276'),
            Model(hyperid=220198, hid=111, title='SKU0MODEL220198'),
        ]

        cls.index.mskus += [
            MarketSku(sku=20202, hyperid=141197, hid=111, title='SKU20202'),
            MarketSku(sku=30303, hyperid=101017, hid=111, title='SKU30303'),
            MarketSku(sku=40404, hyperid=130276, hid=111, title='SKU40404'),
        ]

        cls.index.offers += [
            Offer(sku=20202, hid=111, price=1230, hyperid=141197, title='OFFER123123'),
            Offer(sku=20202, hid=111, price=1231, hyperid=141197, title='OFFER123124'),
            Offer(sku=20202, hid=111, price=1232, hyperid=141197, title='OFFER123125'),
            Offer(hid=111, price=1233, hyperid=141197, title='OFFER123126'),
            Offer(sku=30303, hid=111, price=1230, hyperid=101017, title='OFFER123127'),
            Offer(sku=30303, hid=111, price=1231, hyperid=101017, title='OFFER123128'),
            Offer(hid=111, price=1232, hyperid=101017, title='OFFER123129'),
            Offer(sku=40404, hid=111, price=1230, hyperid=130276, title='OFFER123130'),
            Offer(hid=111, price=100500, hyperid=220198, title='OFFER100500220198'),
        ]

    def test_offer_counts(self):
        request = 'place=prime&hid=111&rearr-factors=market_metadoc_search=skus&use-default-offers=1&text=SKU'
        replace_flag = '&rearr-factors=market_replace_metadoc_id_with_child_id=0'
        test_frag = {
            'search': {
                'results': [
                    {'entity': 'sku', 'id': '20202', 'offers': {'count': 3}},
                    {'entity': 'sku', 'id': '30303', 'offers': {'count': 2}},
                    {'entity': 'sku', 'id': '40404', 'offers': {'count': 1}},
                ]
            }
        }
        response = self.report.request_json(request)
        self.assertFragmentIn(response, test_frag, preserve_order=False, allow_different_len=True)

        response = self.report.request_json(request + replace_flag)
        self.assertFragmentIn(response, test_frag, preserve_order=False, allow_different_len=True)

    @classmethod
    def prepare_only_metadoc_by_search_literal_for_empty_req(cls):
        cls.index.mskus += [
            MarketSku(sku=101, title='SKU1', hid=101, waremd5='1000000000000000000001'),
            MarketSku(sku=102, title='SKU2', hid=101, waremd5='1000000000000000000002'),
        ]

        cls.index.offers += [
            Offer(sku=101, hid=101, title='SKU1 OFFER1', waremd5='1000000000000000000003'),
            Offer(sku=101, hid=101, title='SKU1 OFFER2', waremd5='1000000000000000000004'),
            Offer(sku=102, hid=101, title='SKU2 OFFER3', waremd5='1000000000000000000005'),
            Offer(sku=102, hid=101, title='SKU2 OFFER4', waremd5='1000000000000000000006'),
            Offer(hid=101, title='OFFER5', waremd5='1000000000000000000007'),
            Offer(hid=101, title='OFFER6', waremd5='1000000000000000000008'),
        ]

    def test_only_metadoc_by_search_literal_for_empty_req(self):
        """
        Проверяем, что cо взведенным флагом market_metadoc_search
        при поиске только по литералу попадут только метадокументы
        """
        emptyReqWithMetadocSearch = 'place=prime&hid=101&rearr-factors=market_metadoc_search=offers'
        response = self.report.request_json(emptyReqWithMetadocSearch)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': Contains('SKU1')}},
                    {'titles': {'raw': Contains('SKU2')}},
                    {'titles': {'raw': 'OFFER5'}},
                    {'titles': {'raw': 'OFFER6'}},
                ]
            },
            allow_different_len=False,
        )

        emptyReqWithoutMetadocSearch = 'place=prime&hid=101&rearr-factors=market_metadoc_search=no'
        response = self.report.request_json(emptyReqWithoutMetadocSearch)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'titles': {'raw': 'SKU1 OFFER1'}},
                    {'titles': {'raw': 'SKU1 OFFER2'}},
                    {'titles': {'raw': 'SKU2 OFFER3'}},
                    {'titles': {'raw': 'SKU2 OFFER4'}},
                    {'titles': {'raw': 'OFFER5'}},
                    {'titles': {'raw': 'OFFER6'}},
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_dsbs(cls):
        cls.index.shops += [Shop(fesh=20, datafeed_id=20, priority_region=213, cpa=Shop.CPA_REAL, tax_system=Tax.OSN)]

        cls.index.mskus += [
            MarketSku(sku=20, title='наскучивший осьминог'),
        ]

        cls.index.offers += [
            Offer(
                price=2,
                fesh=20,
                feedid=20,
                sku=20,
                waremd5='22222222222222ggggMODg',
                title='наскучивший dsbs-осьминог',
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cpa=Offer.CPA_REAL,
                delivery_buckets=[5678],
                picture=Picture(width=100, height=100, group_id=12345),
            ),
        ]

    def test_dsbs(self):
        """
        Проверяем, что дсбсы выбираются на CPA-шарде байбоксом
        """

        response = self.report.request_json(
            'place=prime&text=осьминог&debug=da&rids=213&'
            'rearr-factors=market_metadoc_search=offers&'
            'allow-collapsing=1&use-default-offers=1&'
            'debug-doc-count=100'
        )

        self.assertFragmentIn(
            response,
            {
                'basesearch': {
                    'documents': [
                        {
                            'properties': {
                                'METADOC_SKU': '20',
                                'CHILD_WAREMD5': '22222222222222ggggMODg',
                                'CHILD_SELECTED_BY': EqualToOneOf('BUYBOX', 'COMPOSITE'),
                            }
                        }
                    ]
                }
            },
        )

    """
                                    Models in metadoc search

    12 configurations:

    1. model first and found              1. found model with DO with sku1, sku1 of found model
    2. sku first, model found         x   2. found model with DO with sku1, sku2 of found model
    3. model first and collapsed          3. found model with non-sku DO, sku of found model
    4. sku first and model collapsed

    """

    COMMON_QUERY = (
        'place=prime&text={}&rids=213&debug=da&'
        'rearr-factors=market_metadoc_search=offers&'
        'allow-collapsing=1&use-default-offers=1&platform=touch&'
        'local-offers-first=0&rearr-factors=prefer_do_with_sku=0'
    )

    class Elem:
        def __init__(self, sku_title, sku, model, collapsed, metadoc):
            self.__sku_title = sku_title
            self.__sku = sku
            self.__model = model
            self.__collapsed = collapsed
            self.__metadoc = metadoc

        def get_offers_output(self):
            sku_aware_titles = Absent()
            if self.__sku_title is not None:
                sku_aware_titles = {'raw': self.__sku_title}

            metadoc = Absent()
            if self.__metadoc:
                metadoc = {'sku': self.__sku}

            return {
                'id': self.__model,
                'offers': {
                    'items': [
                        {
                            'skuAwareTitles': sku_aware_titles,
                        }
                    ]
                },
                'debug': {'isCollapsed': self.__collapsed, 'metadoc': metadoc},
            }

        def get_skus_output(self):
            if self.__metadoc:
                return {
                    'entity': 'sku',
                    'id': self.__sku,
                    'titles': {'raw': self.__sku_title},
                    'product': {'id': self.__model},
                    'debug': {'metadoc': {'sku': self.__sku}},
                }
            else:
                return self.get_offers_output()

    def check_metadoc_request(self, text, elems):
        common_query = (
            'place=prime&text={}&rids=213&debug=da&'
            'rearr-factors=market_metadoc_search={}&'
            'allow-collapsing=1&use-default-offers=1&platform=touch&'
            'local-offers-first=0&rearr-factors=prefer_do_with_sku=0'
        )

        response = self.report.request_json(common_query.format(text, 'offers'))

        self.assertFragmentIn(
            response, {'results': [elem.get_offers_output() for elem in elems]}, allow_different_len=False
        )

        response = self.report.request_json(common_query.format(text, 'skus'))

        self.assertFragmentIn(
            response, {'results': [elem.get_skus_output() for elem in elems]}, allow_different_len=False
        )

    @classmethod
    def prepare_model_first_and_found(cls):
        # 1x1

        cls.index.models += [
            Model(hyperid=20, title="оборзевшая антилопа", ts=20),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=200,
                title='оборзевшая от скуки антилопа',
                hyperid=20,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл', feedid=12, ts=21, waremd5='sku-200-blue-1-oq57hRg', hyperid=20
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=20, title='тоже нерелевантный тайтл', fesh=100, sku=200, ts=19),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 20).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 21).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 19).respond(0.7)

        # 1x2

        cls.index.models += [
            Model(
                hyperid=21,
                title="незрелый орангутанг",
                ts=22,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="Расхлопнутая модель 21.0", key='21_0'),
                    UngroupedModel(group_id=2, title="Расхлопнутая модель 21.1", key='21_1'),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                sku=210,
                title='незрелый скулящий орангутанг 0',
                ungrouped_model_blue=1,
                hyperid=21,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл', feedid=12, ts=23, waremd5='sku-210-blue-1-oq57hRg', hyperid=21
                    ),
                ],
                delivery_buckets=[1234],
            ),
            MarketSku(
                sku=211,
                title='незрелый скулящий орангутанг 1',
                ungrouped_model_blue=2,
                hyperid=21,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл', feedid=12, ts=24, waremd5='sku-211-blue-1-oq57hRg', hyperid=21
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 22).respond(0.9)
            cls.matrixnet.on_place(place, 23).respond(0.8)
            cls.matrixnet.on_place(place, 24).respond(0.2)

        # 1x3

        cls.index.models += [
            Model(
                hyperid=22,
                title="окрепший мамонт",
                ts=25,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="Расхлопнутая модель 22.0", key='22_0'),
                    UngroupedModel(group_id=2, title="Расхлопнутая модель 22.1", key='22_1'),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(sku=220, title='окрепший, паскуда, мамонт', ungrouped_model_blue=1, hyperid=22, blue_offers=[]),
            MarketSku(sku=221, title='нерелевантный тайтл', ungrouped_model_blue=2, hyperid=22, blue_offers=[]),
        ]

        cls.index.offers += [
            Offer(hyperid=22, title='тоже нерелевантный тайтл', fesh=100, ts=27),
            Offer(
                hyperid=22, title='поскутченный нерелевантный тайтл', ungrouped_model_blue=1, fesh=100, ts=28, sku=220
            ),
            Offer(hyperid=22, title='нерелевантный тайтл', ungrouped_model_blue=2, fesh=100, ts=281, sku=221),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 27).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 28).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 281).respond(0.4)

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 25).respond(0.9)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 27).respond(0.5)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 28).respond(0.7)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 281).respond(0.4)

    def test_model_first_and_found(self):
        """
        1. Проверяем, как ведут себя в поиске по ску модели, найденные по запросу
        (не схлопнутые) и идущие выше найденных ску
        """

        # 1x1

        self.check_metadoc_request(
            'антилопа',
            [
                T.Elem(sku_title='оборзевшая от скуки антилопа', sku='200', model=20, collapsed=False, metadoc=False),
            ],
        )

        # 1x2

        self.check_metadoc_request(
            'орангутанг',
            [
                T.Elem(sku_title='незрелый скулящий орангутанг 0', sku='210', model=21, collapsed=False, metadoc=False),
                T.Elem(sku_title='незрелый скулящий орангутанг 1', sku='211', model=21, collapsed=True, metadoc=True),
            ],
        )

        # 1x3

        self.check_metadoc_request(
            'мамонт',
            [
                T.Elem(sku_title=None, sku=None, model=22, collapsed=False, metadoc=False),
                T.Elem(sku_title='окрепший, паскуда, мамонт', sku='220', model=22, collapsed=True, metadoc=True),
            ],
        )

    @classmethod
    def prepare_sku_first_and_model_found(cls):
        # 2x1

        cls.index.models += [
            Model(hyperid=23, title="немой попугай", ts=29),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=230,
                title='немая скульптура попугая',
                hyperid=23,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл', feedid=12, ts=30, waremd5='sku-230-blue-1-oq57hRg', hyperid=23
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=23, title='тоже нерелевантный тайтл', fesh=100, sku=230, ts=31),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 29).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 30).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31).respond(0.7)

        # 2x2

        cls.index.models += [
            Model(
                hyperid=24,
                title="отважный страус",
                ts=32,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="Расхлопнутая модель 24.0", key='24_0'),
                    UngroupedModel(group_id=2, title="Расхлопнутая модель 24.1", key='24_1'),
                ],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                sku=240,
                title='отважный скученный страус 0',
                ungrouped_model_blue=1,
                hyperid=24,
                blue_offers=[],
                delivery_buckets=[1234],
            ),
            MarketSku(
                sku=241,
                title='отважный скученный страус 1',
                ungrouped_model_blue=2,
                hyperid=24,
                blue_offers=[],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=24, title='нерелевантный тайтл', ungrouped_model_blue=2, fesh=100, ts=33, fee=10000, sku=241),
            Offer(
                hyperid=24, title='ещё нерелевантный тайтл', ungrouped_model_blue=1, fesh=100, ts=34, fee=10, sku=240
            ),
        ]

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 34).respond(0.9)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 32).respond(0.8)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 33).respond(0.4)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 33).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 32).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 34).respond(0.4)

        # 2x3

        cls.index.models += [
            Model(
                hyperid=25,
                title="дикий котёнок",
                ts=35,
                ungrouped_blue=[UngroupedModel(group_id=1, title="Расхлопнутая модель 25.0", key='25_0')],
            )
        ]

        cls.index.mskus += [
            MarketSku(
                sku=250,
                title='дикий скуластый котёнок',
                ungrouped_model_blue=1,
                hyperid=25,
                blue_offers=[],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=25, title='нерелевантный тайтл', fesh=100, ts=36, fee=10000),
            Offer(
                hyperid=25, title='ещё нерелевантный тайтл', ungrouped_model_blue=1, fesh=100, ts=37, fee=10, sku=250
            ),
        ]

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 37).respond(0.9)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 35).respond(0.8)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 36).respond(0.4)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 36).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 35).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 37).respond(0.4)

    def test_sku_first_and_model_found(self):
        """
        2. Проверяем, как ведут себя в поиске по ску модели, найденные по запросу
        (не схлопнутые) и идущие ниже найденных ску
        """

        # 2x1

        self.check_metadoc_request(
            'попугай',
            [
                T.Elem(sku_title='немая скульптура попугая', sku='230', model=23, collapsed=True, metadoc=True),
            ],
        )

        # 2x2

        self.check_metadoc_request(
            'страус',
            [
                T.Elem(sku_title='отважный скученный страус 0', sku='240', model=24, collapsed=True, metadoc=True),
                T.Elem(sku_title='отважный скученный страус 1', sku='241', model=24, collapsed=False, metadoc=False),
            ],
        )

        # 2x3

        self.check_metadoc_request(
            'котёнок',
            [
                T.Elem(sku_title='дикий скуластый котёнок', sku='250', model=25, collapsed=True, metadoc=True),
                T.Elem(sku_title=None, sku=None, model=25, collapsed=False, metadoc=False),
            ],
        )

    @classmethod
    def prepare_model_first_and_collapsed(cls):
        # 3x1

        cls.index.models += [
            Model(hyperid=26, title="нерелевантный тайтл", ts=38),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=260,
                title='скудоумная чихающая сова',
                hyperid=26,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл', feedid=12, ts=39, waremd5='sku-260-blue-1-oq57hRg', hyperid=26
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=26, title='мудрая чихающая сова', fesh=100, ts=40),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 40).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 39).respond(0.8)

        # 3x2 && 3x3 are impossible as DO with sku is always preferred

    def test_model_first_and_collapsed(self):
        """
        3, Проверяем, как ведут себя в поиске по ску схопнутые
        модели, идущие выше найденных ску
        """

        # 3x1

        self.check_metadoc_request(
            'сова',
            [
                T.Elem(sku_title='скудоумная чихающая сова', sku='260', model=26, collapsed=True, metadoc=False),
            ],
        )

        # 3x2 && 3x3 are impossible as DO with sku is always preferred

    @classmethod
    def prepare_sku_first_and_model_collapsed(cls):
        # 4x1

        cls.index.models += [
            Model(hyperid=29, title="нерелевантный тайтл", ts=48),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=290,
                title='ароматный скунс',
                hyperid=29,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл', feedid=12, ts=49, waremd5='sku-290-blue-0-oq57hRg', hyperid=29
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=29, title='менее ароматный зверь', fesh=100, ts=50),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 49).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 50).respond(0.8)

        # 4x2

        cls.index.models += [
            Model(
                hyperid=30,
                title="нерелевантный тайтл",
                ts=51,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="Расхлопнутая модель 30.1", key='30_1'),
                    UngroupedModel(group_id=2, title="Расхлопнутая модель 30.2", key='30_2'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=300,
                title='скущий пельмени удав 0',
                ungrouped_model_blue=1,
                hyperid=30,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл',
                        feedid=12,
                        ts=52,
                        waremd5='sku-300-blue-0-oq57hRg',
                        hyperid=30,
                        price=100,
                    ),
                ],
                delivery_buckets=[1234],
            ),
            MarketSku(
                sku=301,
                title='скущий пельмени кто-то нерелевантный 1',
                ungrouped_model_blue=2,
                hyperid=30,
                blue_offers=[
                    BlueOffer(
                        title='нерелевантный тайтл',
                        feedid=12,
                        ts=53,
                        waremd5='sku-301-blue-0-oq57hRg',
                        hyperid=30,
                        price=10,
                    ),
                ],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [Offer(hyperid=30, title='беспельменный удав', fesh=100, ts=54)]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 52).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 53).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 54).respond(0.5)

        # 4x3

        cls.index.models += [
            Model(
                hyperid=31,
                title="нерелевантный тайтл",
                ts=55,
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="Расхлопнутая модель 31.1", key='31_1'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=310,
                title='лемур сторонник Чаушеску',
                ungrouped_model_blue=1,
                hyperid=31,
                blue_offers=[],
                delivery_buckets=[1234],
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=31,
                sku=310,
                ungrouped_model_blue=1,
                title='белый лемур сторонник Чаушеску',
                fesh=100,
                ts=56,
                waremd5='sku-310-white--oq57hRg',
            ),
            Offer(
                hyperid=31,
                title='белый лемур революционер',
                fesh=100,
                ts=57,
                no_picture=True,
                waremd5='non-sku-white--oq57hRg',
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 57).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 56).respond(0.6)

    def test_sku_first_and_model_collapsed(self):
        """
        4, Проверяем, как ведут себя в поиске по ску схопнутые
        модели, идущие ниже найденных ску
        """

        # 4x1

        self.check_metadoc_request(
            'скунс',
            [
                T.Elem(sku_title='ароматный скунс', sku='290', model=29, collapsed=True, metadoc=True),
            ],
        )

        # 4x2

        self.check_metadoc_request(
            'удав',
            [
                T.Elem(sku_title='скущий пельмени удав 0', sku='300', model=30, collapsed=True, metadoc=True),
                T.Elem(
                    sku_title='скущий пельмени кто-то нерелевантный 1',
                    sku='301',
                    model=30,
                    collapsed=True,
                    metadoc=False,
                ),
            ],
        )

        # 4x3

        self.check_metadoc_request(
            'лемур',
            [
                T.Elem(sku_title='лемур сторонник Чаушеску', sku='310', model=31, collapsed=True, metadoc=True),
                T.Elem(sku_title=None, sku=None, model=31, collapsed=True, metadoc=False),
            ],
        )

    @classmethod
    def prepare_collapse_metadocs(cls):
        cls.index.models += [
            Model(
                hyperid=32,
                title='нерелевантный тайтл',
                ungrouped_blue=[
                    UngroupedModel(group_id=1, title="расхлопнутая модель 32.1", key='32_1'),
                    UngroupedModel(group_id=2, title="расхлопнутая модель 32.2", key='32_2'),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title='косатка',
                hyperid=32,
                sku=3210,
                blue_offers=[BlueOffer(feedid=12)],
                delivery_buckets=[1234],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                title='косатка',
                hyperid=32,
                sku=3211,
                blue_offers=[BlueOffer(feedid=12)],
                delivery_buckets=[1234],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                title='косатка',
                hyperid=32,
                sku=3220,
                blue_offers=[BlueOffer(feedid=12)],
                delivery_buckets=[1234],
                ungrouped_model_blue=2,
            ),
        ]

    def test_collapse_metadocs(self):
        """
        Проверяем, что в поиске по ску схлопывание происходит с учётом
        параметров ску (как в старом расхлопывании)
        """

        query = (
            'place=prime&text=косатка&rids=213&debug=da&'
            'rearr-factors=market_metadoc_search={}&'
            'allow-collapsing=1&use-default-offers=1&platform=touch&'
            'local-offers-first=0&debug-doc-count=100'
        )

        response = self.report.request_json(query.format('offers'))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'offers': {'items': [{'sku': EqualToOneOf('3210', '3211')}]},
                    },
                    {
                        'offers': {'items': [{'sku': '3220'}]},
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query.format('skus'))
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'id': EqualToOneOf('3210', '3211'),
                    },
                    {
                        'entity': 'sku',
                        'id': '3220',
                    },
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_no_direct(cls):
        cls.index.mskus += [
            MarketSku(title='скушай шашлык', hyperid=603, sku=600, blue_offers=[], delivery_buckets=[1234]),
        ]

        cls.index.offers += [
            Offer(title='шашлык 1', hyperid=601, is_direct=False),
            Offer(title='шашлык 2', hyperid=602, is_direct=True),
            Offer(title='шашлык 3', hyperid=603, sku=600, price=10, is_direct=False),
            Offer(title='шашлык 4', hyperid=603, sku=600, price=5, is_direct=True),
        ]

    def test_no_direct(self):
        """
        Проверяем, что в поиске по метадокам фильтруются офферы директа
        """

        query = 'place=prime&text=шашлык&allow-collapsing=1&use-default-offers=1&debug=da'
        no_sku_flag = '&rearr-factors=market_metadoc_search=no'
        flag = '&rearr-factors=market_metadoc_search=offers'
        flag_show_direct_offers = '&rearr-factors=market_enable_direct_offers=1'

        response = self.report.request_json(query + no_sku_flag)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'offerTitle': 'шашлык 1'}},
                    {'debug': {'offerTitle': 'шашлык 3'}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + flag)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'offerTitle': 'шашлык 1'}},
                    {'debug': {'metadoc': NotEmpty(), 'offerTitle': 'шашлык 3'}},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(query + flag_show_direct_offers)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'debug': {'offerTitle': 'шашлык 1'}},
                    {'debug': {'offerTitle': 'шашлык 2'}},
                    {'debug': {'offerTitle': 'шашлык 4'}},
                ]
            },
            allow_different_len=False,
        )

    def test_cgi_literal(self):
        """
        Проверяем, что с поиском по фешу метадоковый поиск не включается
        """

        response = self.report.request_json(
            'place=prime&text=скучная+горилла&fesh=100'
            '&allow-collapsing=1&use-default-offers=1'
            '&rearr-factors=market_metadoc_search=offers&debug=da'
        )
        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {'debug': {'metadoc': NotEmpty()}},
                ]
            },
        )

    def test_text_and_textless_flags(self):
        """
        Проверяем, что работают флаги, ограничивающие метадоковый поиск на текст/бестекст
        """

        base_request = 'place=prime&debug=da&allow-collapsing=1'
        text = '&text=косуля'
        textless = '&hid=1'
        offers_flag = '&rearr-factors=market_metadoc_search=offers'
        skus_flag = '&rearr-factors=market_metadoc_search=skus'
        text_flag = '&rearr-factors=market_metadoc_text_only=1'
        textless_flag = '&rearr-factors=market_metadoc_textless_only=1'
        force_textless_flag = '&rearr-factors=market_metadoc_force_on_textless_everywhere=1'

        for metadoc_flag, search_type in ((offers_flag, 'IsMetadocSearchOffers'), (skus_flag, 'IsMetadocSearchSkus')):

            request = base_request + metadoc_flag

            for force_textless in ('', force_textless_flag):
                self.assertFragmentIn(
                    self.report.request_json(request + text + text_flag + force_textless),
                    {'logicTrace': [Regex('{}: 1'.format(search_type))]},
                )

                self.assertFragmentIn(
                    self.report.request_json(request + text + textless_flag + force_textless),
                    {'logicTrace': [Regex('{}: 0'.format(search_type))]},
                )

            # по дефолту на бестексте включено
            self.assertFragmentIn(
                self.report.request_json(request + textless),
                {'logicTrace': [Regex('{}: 1'.format(search_type))]},
            )
            # по дефолту на тексте включено
            self.assertFragmentIn(
                self.report.request_json(request + text),
                {'logicTrace': [Regex('{}: 1'.format(search_type))]},
            )

    @classmethod
    def prepare_specs(cls):
        cls.index.model_description_templates += [
            ModelDescriptionTemplates(
                hid=3,
                friendlymodel=['model friendly {sku_filter_2}'],
                model=[("Основное", {'model full': '{sku_filter_2}'})],
            ),
        ]

        cls.index.gltypes += [
            GLType(
                hid=3,
                param_id=101,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
                xslname='sku_filter_2',
                model_filter_index=1,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=5,
                hid=3,
                ts=60,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=5,
                hid=3,
                sku=501,
                glparams=[GLParam(param_id=101, value=2)],
                blue_offers=[BlueOffer(waremd5='sku-501--blue--oq57hRg', ts=59, price=100)],
            ),
        ]

        cls.index.offers += [
            Offer(
                price=200,
                fesh=20,
                feedid=20,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cpa=Offer.CPA_REAL,
                delivery_buckets=[5678],
                picture=Picture(width=100, height=100, group_id=12345),
                hyperid=5,
                hid=3,
                sku=501,
                waremd5='sku-501-white--oq57hRg',
                ts=58,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 59).respond(0.8)
            cls.matrixnet.on_place(place, 58).respond(0.5)
            cls.matrixnet.on_place(place, 60).respond(0.3)

    def test_specs(self):
        """
        Проверяем, что skuAwareSpecs проставляются корректно
        независимо от заполненности оффера
        """

        response = self.report.request_json(
            'place=prime&hid=3&show-models-specs=msku-friendly,msku-full'
            '&allow-collapsing=1&use-default-offers=1&debug=da&cpa=real'
            '&rearr-factors=market_metadoc_search=offers'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'debug': {'wareId': 'sku-501--blue--oq57hRg'},
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'sku-501-white--oq57hRg',
                                    'skuAwareSpecs': NotEmpty(),
                                }
                            ]
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_soft_cpa(cls):
        cls.index.models += [
            Model(
                hyperid=6,
                hid=4,
                ts=63,
            ),
        ]

        cls.index.regional_models += [
            RegionalModel(6, rids=[213], has_good_cpa=True, has_cpa=True),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=6,
                hid=4,
                sku=601,
                title='малюта скуратов',
            ),
        ]

        cls.index.offers += [
            # cpa
            Offer(
                price=200,
                fesh=20,
                feedid=20,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cpa=Offer.CPA_REAL,
                delivery_buckets=[5678],
                picture=Picture(width=100, height=100, group_id=12345),
                hyperid=6,
                hid=4,
                sku=601,
                waremd5='sku-601---cpa--oq57hRg',
                ts=61,
            ),
            # non-cpa
            Offer(
                fesh=100,
                hyperid=6,
                hid=4,
                sku=601,
                waremd5='sku-601---cpc--oq57hRg',
                ts=62,
                cpa=Offer.CPA_NO,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 61).respond(0.8)
            cls.matrixnet.on_place(place, 62).respond(0.7)
            cls.matrixnet.on_place(place, 63).respond(0.3)

    def test_soft_cpa(self):
        """
        Проверяем, что, если оффер пофильтруется по soft cpa, это произойдёт
        не только в ДО, но и в поиске внутри метадока
        """

        self.dynamic.market_dynamic.disabled_market_sku += [
            DynamicMarketSku(supplier_id=100, shop_sku='skuchnaya_gorilla_36'),
        ]

        response = self.report.request_json(
            'place=prime&text=малюта&allow-collapsing=1&use-default-offers=1'
            '&rearr-factors=market_metadoc_search=skus&debug=da&rids=213'
        )

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'offers': EmptyDict(),
                    }
                ]
            },
        )

    def test_empty_request(self):
        """
        Проверяем, что на пустой запрос репорт по-прежнему генерирует ошибку
        даже несмотря на is_metadoc-литерал
        """

        flag_field = '&rearr-factors=market_metadoc_search={}'
        flags = ('', flag_field.format('no'), flag_field.format('offers'), flag_field.format('skus'))

        for flag in flags:
            response = self.report.request_json('place=prime{}'.format(flag), strict=False)
            self.assertEqual(response.code, 400)
            self.assertFragmentIn(response, {'error': {'code': 'EMPTY_REQUEST'}})

    def test_app(self):
        """
        Проверяем, как разные типы поиска по ску работают на приложении
        """

        base_request = 'place=prime&debug=da&allow-collapsing=1'
        text = '&text=косуля'
        textless = '&hid=3'
        app1 = '&client=ANDROID'
        app2 = '&client=IOS'
        app3 = '&api=content&content-api-client=18932'
        offers = '&rearr-factors=market_metadoc_search=offers'
        skus = '&rearr-factors=market_metadoc_search=skus'
        app_rev_flag = '&rearr-factors=market_metadoc_on_app=0'
        force_textless = '&rearr-factors=market_metadoc_force_on_textless_everywhere=1'

        for app in (app1, app2, app3):

            self.assertFragmentIn(
                self.report.request_json(base_request + text + app + offers),
                {'logicTrace': [Regex('IsMetadocSearchOffers: 1')]},
            )
            self.assertFragmentIn(
                self.report.request_json(base_request + text + app + skus),
                {'logicTrace': [Regex('IsMetadocSearchSkus: 1')]},
            )

            self.assertFragmentIn(
                self.report.request_json(base_request + textless + app + offers),
                {'logicTrace': [Regex('IsMetadocSearchOffers: 1')]},
            )
            self.assertFragmentIn(
                self.report.request_json(base_request + textless + app + skus),
                {'logicTrace': [Regex('IsMetadocSearchSkus: 1')]},
            )

            self.assertFragmentIn(
                self.report.request_json(base_request + textless + force_textless + app + offers),
                {'logicTrace': [Regex('IsMetadocSearchOffers: 1')]},
            )
            self.assertFragmentIn(
                self.report.request_json(base_request + textless + force_textless + app + skus),
                {'logicTrace': [Regex('IsMetadocSearchSkus: 1')]},
            )

            self.assertFragmentIn(
                self.report.request_json(base_request + text + app + offers + app_rev_flag),
                {'logicTrace': [Regex('IsMetadocSearchOffers: 0')]},
            )
            self.assertFragmentIn(
                self.report.request_json(base_request + text + app + skus + app_rev_flag),
                {'logicTrace': [Regex('IsMetadocSearchSkus: 0')]},
            )

            # по умолчанию включен поиск по метадокам
            self.assertFragmentIn(
                self.report.request_json(base_request + text + app + offers),
                {'logicTrace': [Regex('IsMetadocSearchOffers: 1')]},
            )
            self.assertFragmentIn(
                self.report.request_json(base_request + text + app + skus),
                {'logicTrace': [Regex('IsMetadocSearchSkus: 1')]},
            )

            self.assertFragmentIn(
                self.report.request_json(base_request + textless + app + offers),
                {'logicTrace': [Regex('IsMetadocSearchOffers: 1')]},
            )
            self.assertFragmentIn(
                self.report.request_json(base_request + textless + app + skus),
                {'logicTrace': [Regex('IsMetadocSearchSkus: 1')]},
            )

    @classmethod
    def prepare_b2b(cls):
        cls.index.offers += [
            Offer(title='тереби b2b', is_b2b=True),
            Offer(title='тереби не b2b', is_b2b=False),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=12345,
                title='тереби ску',
                blue_offers=[
                    BlueOffer(title='тереби синий b2b', is_b2b=True),
                ],
                delivery_buckets=[123],
            ),
            MarketSku(
                sku=67890,
                title='тереби ску 2',
                blue_offers=[
                    BlueOffer(title='тереби синий не b2b', is_b2b=False),
                ],
                delivery_buckets=[123],
            ),
        ]

    def test_b2b(self):
        '''
        Проверяем, что с параметром &available-for-business=1
        метадоковый поиск не включается
        '''

        base_req = 'place=prime&text=тереби&debug=da'

        for param in '', '&available-for-business=0':
            response = self.report.request_json(base_req + param)

            self.assertFragmentIn(
                response,
                {
                    'logicTrace': [
                        Regex('IsMetadocSearchOffers: 1'),
                        Regex('IsMetadocSearchSkus: 0'),
                    ]
                },
            )

        response = self.report.request_json(base_req + '&available-for-business=1')

        self.assertFragmentIn(
            response,
            {
                'logicTrace': [
                    Regex('IsMetadocSearchOffers: 0'),
                    Regex('IsMetadocSearchSkus: 0'),
                ]
            },
        )

    @classmethod
    def prepare_filters(cls):
        cls.index.gltypes += [
            GLType(
                hid=5,
                param_id=1,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                    GLValue(value_id=3, text='value3'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=7,
                hid=5,
                sku=701,
                glparams=[GLParam(param_id=1, value=1)],
                title='искусство',
            ),
            MarketSku(
                hyperid=7,
                hid=5,
                sku=702,
                glparams=[GLParam(param_id=1, value=3)],
                title='искусство',
            ),
        ]

        cls.index.offers += [
            Offer(
                price=200,
                fesh=20,
                feedid=20,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cpa=Offer.CPA_REAL,
                delivery_buckets=[5678],
                picture=Picture(width=100, height=100, group_id=12345),
                hyperid=7,
                hid=5,
                sku=701,
                waremd5='sku-701---cpa--oq57hRg',
                glparams=[GLParam(param_id=1, value=2)],
                title='искусство',
            ),
            Offer(
                price=200,
                fesh=20,
                feedid=20,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cpa=Offer.CPA_REAL,
                delivery_buckets=[5678],
                picture=Picture(width=100, height=100, group_id=12345),
                hyperid=7,
                hid=5,
                sku=702,
                waremd5='sku-702---cpa--oq57hRg',
                glparams=[GLParam(param_id=1, value=3)],
                title='искусство',
            ),
        ]

    def test_filters(self):
        response = self.report.request_json('place=prime&text=искусство&hid=5&glfilter=1:1')
        self.assertFragmentIn(
            response, {'id': '1', 'values': [{'id': '1'}, {'id': '2'}, {'id': '3'}]}, allow_different_len=False
        )

    @classmethod
    def prepare_eda(cls):
        cls.index.mskus += [
            MarketSku(
                hyperid=8,
                sku=801,
                title='макарошки',
            ),
        ]

        for i in range(10):
            cls.index.offers += [
                Offer(
                    price=2,
                    fesh=20,
                    feedid=20,
                    dimensions=OfferDimensions(length=20, width=30, height=10),
                    cpa=Offer.CPA_REAL,
                    delivery_buckets=[5678],
                    picture=Picture(width=100, height=100, group_id=12345),
                    sku=801,
                    is_eda=True,
                )
            ]

    def test_eda(self):
        '''
        Проверяем, что офферы еды не учитываются в прюнкаунтах
        '''

        response = self.report.request_json(
            'place=prime&text=макарошки&rearr-factors=market_metadoc_pruncount=5&debug=da&debug-doc-count=10&cpa=real'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'basesearch': {
                        'documents': [
                            {'properties': {'METADOC_FILTERING': 'EDA: 10'}},
                        ]
                    }
                }
            },
        )

    @classmethod
    def prepare_collapsed_as_do(cls):
        for i in range(20):
            cls.index.mskus += [
                MarketSku(
                    sku=38934700 + i,
                    hyperid=38934700 + i,
                    title='СКУ - это строительно-квартирное управление',
                    blue_offers=[BlueOffer(feedid=1)],
                )
            ]

    def test_collapsed_as_do(self):
        """
        Проверяем что схлопнутый оффер используется как ДО
        если ДО-оффер не запрашивался
        market_dynstat_count=5 означает что ДО запрашивается только для первых топ5 офферов (на самом деле для
        """

        response = self.report.request_json(
            'place=prime&text=СКУ&page=2&pp=18&allow-collapsing=1&use-default-offers=1&allow-ungrouping=1&numdoc=5'
            '&rearr-factors=market_metadoc_search=skus;market_dynstat_count=5&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'sku',
                            'offers': {
                                'items': [
                                    {
                                        'entity': 'offer',
                                        'debug': {
                                            'docRel': Greater(0)
                                        },  # непустой docRel означает что документ найден через поиск
                                    }
                                ]
                            },
                        }
                        for _ in range(5)
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_no_metadoc_search_on_DO_subrequest(self):
        """Проверяем что подзапрос за ДО не использует метадоковый поиск"""

        response = self.report.request_json(
            'place=prime&text=СКУ&cpa=real&pp=18&allow-collapsing=1&use-default-offers=1&allow-ungrouping=1&debug=da'
        )
        # метадоковый поиск включен
        self.assertFragmentIn(
            response,
            {'debug': {'metasearch': {'name': ''}, 'report': {'how': [{'args': Contains('metadoc_search: true')}]}}},
        )
        # подзапросе за ДО метадоковый поиск выключен
        self.assertFragmentIn(
            response,
            {'metasearch': {'name': 'Default offer'}, 'report': {'how': [{'args': Contains('metadoc_search: false')}]}},
        )

    @classmethod
    def prepare_empty_metadoc(cls):
        """
        Заводим в индексе одну пустую скуху (совсем без офферов)
        """

        cls.index.mskus += [
            MarketSku(
                sku=938383,
                hyperid=938383,
                title='СКУ - Союз коммунистов Украины',
                blue_offers=[BlueOffer(feedid=1)],
            ),
            MarketSku(sku=954848, hyperid=954848, title='СКУ - Семейный кодекс Украины', empty=True),
        ]

    def test_panther_ignore_empty_metadoc(self):
        """
        Проверяем что под флагом market_metadoc_search_ignore_empty=1
        скухи без офферов если они есть в индексе не нагребаются
        """

        response = self.report.request_json(
            'place=prime&text=Украина&debug=da&allow-collapsing=1&allow-ungrouping=1'
            '&rearr-factors=market_metadoc_search_ignore_empty=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {'total': 1},
                'debug': {
                    'brief': {
                        'reqwizardText': Contains('is_metadoc:"1"'),
                        'filters': {'CHILDLESS_METADOC': 1},
                    }
                },
            },
        )

        response = self.report.request_json('place=prime&text=Украина&debug=da&allow-collapsing=1&allow-ungrouping=1')
        self.assertFragmentIn(
            response,
            {
                'search': {'total': 1},
                'debug': {
                    'brief': {
                        'reqwizardText': Contains('is_not_empty_metadoc:"1"'),
                        'filters': {'CHILDLESS_METADOC': Absent()},
                    }
                },
            },
        )

    @classmethod
    def prepare_far_pages_optimization(cls):
        modelsCount = 200

        cls.index.models += [Model(hyperid=230622 + i, title='model_' + str(i), hid=230622) for i in range(modelsCount)]

        cls.index.mskus += [
            MarketSku(hyperid=230622 + i, sku=230622 + i, title='sku_' + str(i)) for i in range(modelsCount)
        ]

        cls.index.offers += [
            Offer(hyperid=230622 + i, sku=230622 + i, title='offer_' + str(i) + '_model_' + str(i), price=100 + i)
            for i in range(modelsCount)
        ]

    def test_no_do_on_far_pages(self):
        request = 'place=prime&hid=230622&use-default-offers=1&numdoc=10&page=%d&debug=da&rearr-factors=market_req_do_on_far_pages=%d'

        response = self.report.request_json(request % (7, 1))
        self.assertFragmentIn(response, {'logicTrace': [Contains('Request DO on far pages:YES')]})
        self.assertFragmentIn(
            response, {'debug': {'metasearch': {'subrequests': [{'metasearch': {'name': 'Default offer'}}]}}}
        )

        response = self.report.request_json(request % (7, 0))
        self.assertFragmentIn(response, {'logicTrace': [Contains('Request DO on far pages:NO')]})
        self.assertFragmentNotIn(
            response, {'debug': {'metasearch': {'subrequests': [{'metasearch': {'name': 'Default offer'}}]}}}
        )

        response = self.report.request_json(request % (6, 0))
        self.assertFragmentIn(response, {'logicTrace': [Contains('Request DO on far pages:YES')]})
        self.assertFragmentIn(
            response, {'debug': {'metasearch': {'subrequests': [{'metasearch': {'name': 'Default offer'}}]}}}
        )

    def test_no_meta_rearrange_on_far_pages(self):
        request = 'place=prime&hid=230622&use-default-offers=1&numdoc=20&page=%d&debug=da&rearr-factors=market_enable_meta_rearrange_on_far_pages=%d'

        response = self.report.request_json(request % (9, 1))
        self.assertFragmentIn(response, {'logicTrace': [Contains('Enable meta rearrange on far pages:YES')]})

        response = self.report.request_json(request % (9, 0))
        self.assertFragmentIn(response, {'logicTrace': [Contains('Enable meta rearrange on far pages:NO')]})

        response = self.report.request_json(request % (8, 0))
        self.assertFragmentIn(response, {'logicTrace': [Contains('Enable meta rearrange on far pages:YES')]})


if __name__ == '__main__':
    main()
