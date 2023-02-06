#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicShop,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    Model,
    Offer,
    Picture,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.types.autogen import Const
from core.matcher import Round, NoKey, EqualToOneOf, NotEmpty, GreaterEq


class T(TestCase):
    """На первом этапе хочется все офферы (cpa и cpc) ранжировать по cpc-аукциону
    На поиске и на КМ
    Тесты для CPA аукциона по fee в test_cpa_in_top_ranging.py
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.regiontree += [Region(rid=2, name='Питер'), Region(rid=213, name='Нерезиновая')]
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=4, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=5, priority_region=213, regions=[225]),
        ]

        cls.index.models += [
            Model(hid=1, hyperid=1, title='market 4.0 cpa'),
            Model(hid=1, hyperid=2, title='market 4.0 cpa', vbid=20),
            Model(hid=1, hyperid=3, title='market 4.0 cpc'),
            Model(hid=1, hyperid=4, title='market 4.0 cpa'),
            Model(hid=1, hyperid=111, rgb_type=Model.RGB_BLUE),
        ]

        def pic():
            return Picture(width=100, height=100, group_id=12345)

        cls.index.offers += [
            Offer(
                hid=1,
                title='market 4.0 cpc',
                waremd5='VkjX-08eqaaf0Q_MrNfQBw',
                hyperid=1,
                fesh=2,
                cbid=100,
                picture=pic(),
            ),
            Offer(
                hid=1,
                title='market 4.0 cpa',
                waremd5='mxyTpoZMOeSEF-5Ia4PDhw',
                hyperid=1,
                fesh=3,
                cbid=100,
                fee=35,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=1,
                title='market 4.0 cpc',
                waremd5='UChUMwabn69TyQDNJkL6nQ',
                hyperid=2,
                fesh=2,
                cbid=200,
                picture=pic(),
            ),
            Offer(
                hid=1,
                title='market 4.0 cpa',
                waremd5='q-u3w59LXka7z6srVl7zKw',
                hyperid=2,
                fesh=3,
                cbid=500,
                fee=15,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(hid=1, title='market 4.0 cpc', waremd5='JTU6zWcYA9rDBeRxm2pKUA', hyperid=3, fesh=2, picture=pic()),
            Offer(hid=1, title='market 4.0 cpc', waremd5='0M_6Nugytl_hRabpNgvagw', fesh=2, cbid=50, picture=pic()),
            Offer(
                hid=1,
                title='market 4.0 cpa',
                waremd5='ySODAC91fN3VLfhOaIlLXg',
                fesh=3,
                cbid=90,
                fee=20,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),
            Offer(
                hid=1,
                title='market 4.0 cpa',
                waremd5='mxyTpoZMOeSEFhgkuyhgky',
                hyperid=4,
                fesh=4,
                cbid=100,
                fee=35,
                cpa=Offer.CPA_REAL,
                picture=pic(),
            ),  # blocked by dynamic shop
        ]

        cls.dynamic.market_dynamic.disabled_auction_blue_suppliers += [
            DynamicShop(4),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='market 4.0 blue cpa',
                hid=1,
                sku=1,
                cbid=45,
                delivery_buckets=[1234],
                picture=pic(),
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='ozmCtRBXgUJgvxo4kHPBzg')],
            ),
            MarketSku(
                hyperid=111,
                title='market 4.0 blue cpa',
                hid=1,
                sku=111,
                cbid=75,
                delivery_buckets=[1234],
                picture=pic(),
                blue_offers=[BlueOffer(price=2100, feedid=11, waremd5='mBPKTnft_sOeVWOcwl5i5Q')],
            ),
        ]

    def test_prime(self):
        """под флагом market_force_search_auction=Cpc и для cpc и для cpa офферов используется CPC-аукцион"""

        response = self.report.request_json(
            'place=prime&hid=1&rids=213&debug=da&numdoc=20'
            '&rearr-factors=market_force_search_auction=Cpc;market_boost_white_cpa_only_bid=15.0;'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 2,
                'debug': {
                    'properties': {
                        'DOCUMENT_AUCTION_TYPE': 'MODEL_VENDOR',
                        'VBID': '20',
                        'AUCTION_MULTIPLIER': Round(1.131),
                    }
                },
            },
        )
        # офферы приматченные к модели не участвуют в аукционе
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'q-u3w59LXka7z6srVl7zKw',
                'debug': {
                    'properties': {
                        'DOCUMENT_AUCTION_TYPE': NoKey('DOCUMENT_AUCTION_TYPE'),
                        'AUCTION_MULTIPLIER': NoKey('AUCTION_MULTIPLIER'),
                    }
                },
            },
        )
        # cpc-оффер
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': '0M_6Nugytl_hRabpNgvagw',
                'cpa': NoKey('cpa'),
                'debug': {
                    'properties': {'DOCUMENT_AUCTION_TYPE': 'CPC', 'BID': '50', 'AUCTION_MULTIPLIER': Round(1.065)}
                },
            },
        )
        # cpa-only-оффер
        # bid для cpa-офферов не исполльзуется, берется min_bid*boost
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'ySODAC91fN3VLfhOaIlLXg',
                'cpa': 'real',
                'fee': "0.0000",  # fee передается cpa-офферам
                'debug': {
                    'properties': {
                        'DOCUMENT_AUCTION_TYPE': 'CPC',
                        'BID': '15',  # min_bid * 15
                        'MIN_BID': '1',
                        'AUCTION_MULTIPLIER': Round(1.017),
                        'CPM': '30517',
                    },
                    'sale': {'bid': 15},
                },
            },
        )

        # без флага ;market_boost_white_cpa_only_bid=15.0 с гибридным аукционом market_force_search_auction=HybridFair
        # у cpa-cpc офферов будет cpc аукцион а у cpa-only офферов будет cpa-аукцион
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&debug=da&numdoc=20' '&rearr-factors=market_force_search_auction=HybridFair'
        )

        # cpa-only-оффер
        # имеет cpa-аукцион
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'ySODAC91fN3VLfhOaIlLXg',
                'cpa': 'real',
                'fee': "0.0000",  # fee передается cpa-офферам
                'debug': {
                    'properties': {
                        'DOCUMENT_AUCTION_TYPE': 'CPA',
                        'BID': NoKey('BID'),
                        'AUCTION_MULTIPLIER': '1',
                        'CPM': '30000',
                    }
                },
            },
        )

        # без market_boost_white_cpa_only_bid=15.0
        # у cpa-only офферов будет cpa аукцион, bid=0
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&debug=da&numdoc=20'
            '&rearr-factors=market_force_search_auction=Cpc;'
            'market_boost_white_cpa_only_bid=1.0'
        )
        # cpa-only-оффер
        # bid для cpa-офферов не исполльзуется, берется min_bid*boost
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'ySODAC91fN3VLfhOaIlLXg',
                'cpa': 'real',
                'fee': "0.0000",  # fee не передается больше cpa-офферам
                'debug': {
                    'properties': {
                        'DOCUMENT_AUCTION_TYPE': 'CPC',
                        'BID': '1',
                        'MIN_BID': '1',
                        'AUCTION_MULTIPLIER': '1',
                        'CPM': '30000',
                    }
                },
            },
        )

    def test_productoffers_blocked_shop(self):
        """Проверяем, что на примере из предыдущего теста работает зануление fee по данным из динамика auction-supplier-filter.db"""

        response = self.report.request_json(
            'place=productoffers&hyperid=4&rids=213&debug=da&rearr-factors=market_boost_white_cpa_only_bid=1.0'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'mxyTpoZMOeSEFhgkuyhgkw',
                'debug': {
                    'properties': {
                        "WARE_MD5": "mxyTpoZMOeSEFhgkuyhgkw",
                        "BID": "1",
                        "FEE": "0",  # без ограничения в динамике fee = 35
                        "HYBRID_AUCTION_CTR_POWER": "1",
                        "HYBRID_AUCTION_CTR_CPC": "0.3000000119",
                        "HYBRID_AUCTION_CTR_CPA": "0",  # у cpa офферов cpa часть аукциона не используется, т.к. используем cpc-настройки аукциона
                        "HYBRID_AUCTION_SHOP_CONVERSION": "0.03999999911",
                        "HYBRID_AUCTION_MARKET_CONVERSION": "0",
                    }
                },
            },
        )

    @classmethod
    def prepare_clones(cls):
        """На КМ может появляться не более 2х офферов от одного магазина (1 cpc и 1 cpa оффер)"""

        cls.index.shops += [
            Shop(fesh=41, priority_region=213, regions=[225]),
            Shop(fesh=42, main_fesh=41, priority_region=213, regions=[225]),
            Shop(fesh=43, main_fesh=41, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=44, main_fesh=41, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
            Shop(fesh=51, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_REAL),
            Shop(fesh=61, main_fesh=61, priority_region=213, regions=[225], cpa=Shop.CPA_NO, cpc=Shop.CPC_REAL),
            Shop(fesh=62, main_fesh=61, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hid=2, hyperid=201),
        ]

        cls.index.offers += [
            # все офферы из разных dsrcid - покажется 1 cpa и 1 cpc оффер
            Offer(
                hid=2, waremd5='bUba3pTkMYS7FQ0Qz5t7Pg', hyperid=201, fesh=41, randx=1, cpa=Offer.CPA_NO, is_cpc=True
            ),
            Offer(
                hid=2, waremd5='-JpHjrUECsk5GIT8PkWFCQ', hyperid=201, fesh=42, randx=2, cpa=Offer.CPA_NO, is_cpc=True
            ),
            Offer(
                hid=2, waremd5='UHs-lwM3C0yXAlsmQIYI3Q', hyperid=201, fesh=43, randx=3, cpa=Offer.CPA_REAL, is_cpc=False
            ),
            Offer(
                hid=2, waremd5='d5hXyNxIIEIFpbLcdSSJ1A', hyperid=201, fesh=44, randx=4, cpa=Offer.CPA_REAL, is_cpc=False
            ),
            # все офферы из одного dsrcid - сгруппируются на базовых, нет возможности расхлопнуть cpa и cpc
            Offer(
                hid=2, waremd5='w51daAQVItwxekRNBeSkRA', hyperid=201, fesh=51, randx=1, cpa=Offer.CPA_NO, is_cpc=True
            ),
            Offer(
                hid=2, waremd5='SsD4FY1BZ3Fe048YmaD23Q', hyperid=201, fesh=51, randx=2, cpa=Offer.CPA_NO, is_cpc=True
            ),
            Offer(
                hid=2, waremd5='9_KMf_bfKaE8sCEcn90sDw', hyperid=201, fesh=51, randx=3, cpa=Offer.CPA_REAL, is_cpc=False
            ),
            Offer(
                hid=2, waremd5='obl5MSY2-mFd5K1YeQV3YQ', hyperid=201, fesh=51, randx=4, cpa=Offer.CPA_REAL, is_cpc=False
            ),
            # один из офферов даунгрейдится до cpc оффера - покажутся оба оффера
            Offer(
                hid=2,
                waremd5='jLpWh2ZQRCfYjdwhFLeehQ',
                hyperid=201,
                fesh=61,
                randx=1,
                cpa=Offer.CPA_REAL,
                is_cpc=True,
                override_cpa_check=True,
            ),
            Offer(
                hid=2, waremd5='qyRodq7B0pN1oMbLJEG7BA', hyperid=201, fesh=62, randx=2, cpa=Offer.CPA_REAL, is_cpc=True
            ),
        ]

    def test_duplicate_cpa_offers_from_one_shop(self):
        """Проверяем что на КМ могут быть офферы"""

        response = self.report.request_json('place=productoffers&hyperid=201&hid=2&rids=213&pp=18&debug=da&grhow=shop')

        self.assertFragmentIn(
            response,
            {
                'results': [
                    # 1 оффер от клонов 41 магазина
                    {
                        'shop': {'id': EqualToOneOf(41, 42, 43, 44)},
                    },
                    # 1 оффер от клонов 61 магазина
                    {
                        'shop': {'id': EqualToOneOf(61, 62)},
                    },
                    # поскольку идет группировка на базовых по dsrcid то мы получим только 1 оффер от 51 мгазина
                    {'shop': {'id': 51}},
                ]
            },
            allow_different_len=False,
        )

        # под флагом market_show_duplicate_cpa_offer_for_shop=1 видим что от клонов 41 магазина находится 2 оффера а не 1
        response = self.report.request_json(
            'place=productoffers&hyperid=201&hid=2&rids=213&pp=18&debug=da&grhow=shop'
            '&rearr-factors=market_show_duplicate_cpa_offer_for_shop=1'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    # 1 cpa и 1 cpc оффер от клонов 41 магазина
                    {'shop': {'id': EqualToOneOf(41, 42)}, 'cpa': NoKey('cpa')},
                    {'shop': {'id': EqualToOneOf(43, 44)}, 'cpa': 'real'},
                    # 1 cpa и 1 cpc оффер от клонов 61 магазина (оба оффера имеют признак cpa но один из офферов сдаунгрейдился)
                    # {'shop': {'id': 61}}, # Оффер скрыт из-за того что он CPA а магазин CPC
                    {'shop': {'id': 62}},
                    # поскольку идет группировка на базовых по dsrcid то мы получим только 1 оффер от 51 мгазина
                    {'shop': {'id': 51}},
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(response, {"filters": {"HIDE_CPA_PESSIMIZATION_BY_SHOP_SETTINGS": 1}})

    def test_boost_cpa_offers_and_models_text(self):
        """На базовом бустятся cpa-офферы и модели имеющие статистику с индексатора с has_cpa=True
        На мете бустятся cpa-офферы без модели (несхлопнутые) и схлопнутые модели имеющие в обновленной статистике has_cpa=True (cpa-оффер в ДО)"""
        rearr = '&rearr-factors=market_boost_cpa_docs_mnvalue_coef_textless=2.0;market_boost_cpa_docs_mnvalue_coef_text=0.5;market_metadoc_search=no'

        response = self.report.request_json('place=prime&text=market 4.0&debug=da&allow-collapsing=0&numdoc=20' + rearr)
        # синие cpa-офферы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 blue cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},  # на базовом множитель сохраняется в значение MnValue
                        {'tag': 'Meta', 'value': '0.3'},  # на мете множитель влияет только на CPM
                    ]
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': 'ozmCtRBXgUJgvxo4kHPBzg',
                        'BOOST_MULTIPLIER': '0.5',
                        'CPM': '15000',  # 0.3 * 0.5
                    },
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},  # 0.3 * 0.5
                },
            },
        )

        # белые cpa-офферы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},  # на базовом множитель сохраняется в значение MnValue
                        {'tag': 'Meta', 'value': '0.3'},  # на мете множитель влияет только на CPM
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'ySODAC91fN3VLfhOaIlLXg', 'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},  # 0.3 * 0.5
                },
            },
        )

        # cpc-офферы без модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': '0M_6Nugytl_hRabpNgvagw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                },
            },
        )

        # cpc-офферы привязанные к модели не бустятся (т.к. отключено схлапывание)
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'VkjX-08eqaaf0Q_MrNfQBw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                },
            },
        )

        # несхлопнутая cpa-модель бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 1,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},  # 0.3 * 0.5
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': False,
                    'properties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},
                    'modelStats': {'original': {'hasCpa': True}, 'updated': {'hasCpa': True}},
                },
            },
        )

        # несхлопнутая cpc-модель не бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'product',
                'id': 3,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': False,
                    'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'modelStats': {'original': {'hasCpa': False}, 'updated': {'hasCpa': False}},
                },
            },
        )

        response = self.report.request_json(
            'place=prime&text=market 4.0&debug=da&allow-collapsing=1&numdoc=20&fesh=1,2,3' + rearr
        )
        # схлопнутая cpa-модель бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {
                        # у нас схлопнулся cpc-оффер, т.к. cpa-офферы бустятся с коэффициентом 0.5
                        # на базовом есть буст, т.к. cpc-офферы привязанные к cpa-модели бустятся при схлопывании
                        'BOOST_MULTIPLIER': '0.5',
                        'WARE_MD5': 'UChUMwabn69TyQDNJkL6nQ',
                        'CPM': '15000',
                    },
                    # на мете схлопнувшаяся модель имеет статистику с cpa=True
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},
                    'modelStats': {
                        'updated': {'hasCpa': True},
                        'original': {'hasCpa': True},
                    },
                },
            },
        )

        # схлопнутая cpc-модель не бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'product',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'modelStats': {
                        'updated': {'hasCpa': False},
                        'original': {'hasCpa': False},
                    },
                },
            },
        )

    def test_boost_cpa_offers_and_models_textless(self):
        """бестекстовый поиск
        На базовом бустятся cpa-офферы и модели имеющие статистику с индексатора с has_cpa=True
        здесь не применяется переранжирование по метаформуле
        """

        rearr = '&rearr-factors=market_boost_cpa_docs_mnvalue_coef_textless=2.0;market_boost_cpa_docs_mnvalue_coef_text=0.5;market_metadoc_search=no;'

        response = self.report.request_json('place=prime&hid=1&debug=da&allow-collapsing=0&numdoc=20' + rearr)
        # синие cpa-документы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 blue cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # на базовом множитель сохраняется в значение MnValue
                    ]
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': 'ozmCtRBXgUJgvxo4kHPBzg',
                        'BOOST_MULTIPLIER': '2',
                        'CPM': '60000',  # 0.3 * 2.0
                    }
                },
            },
        )

        # белые cpa-офферы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # на базовом множитель сохраняется в значение MnValue
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'ySODAC91fN3VLfhOaIlLXg', 'BOOST_MULTIPLIER': '2', 'CPM': '60000'}
                },
            },
        )

        # cpc-офферы без модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': '0M_6Nugytl_hRabpNgvagw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'}
                },
            },
        )

        # cpc-офферы привязанные к модели не бустятся, т.к. отключено схлапывание
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'VkjX-08eqaaf0Q_MrNfQBw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'}
                },
            },
        )

        # несхлопнутые cpa-модели бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 1,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # 0.3 * 2
                    ]
                },
                'debug': {'isCollapsed': False, 'properties': {'BOOST_MULTIPLIER': '2', 'CPM': '60000'}},
            },
        )

        # несхлопнутые cpc-модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'product',
                'id': 3,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {'isCollapsed': False, 'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'}},
            },
        )

        response = self.report.request_json('place=prime&hid=1&debug=da&allow-collapsing=1&fesh=1,2,3' + rearr)
        # схлопнутая cpa-модель бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 1,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # 0.3 * 2
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {'WARE_MD5': NotEmpty(), 'BOOST_MULTIPLIER': '2', 'CPM': '60000'},
                },
            },
        )

        # схлопнутая cpc-модель не бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'product',
                'id': 3,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {'isCollapsed': True, 'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'}},
            },
        )

    def test_boost_cpa_offers_text(self):
        """На базовом бустятся cpa-офферы
        На мете бустятся cpa-офферы без модели (несхлопнутые) и модели схлопнувшиеся из cpa-оффера"""
        rearr = '&rearr-factors=market_boost_cpa_offers_mnvalue_coef_textless=2.0;market_boost_cpa_offers_mnvalue_coef_text=0.5;market_metadoc_search=no;'

        response = self.report.request_json('place=prime&text=market 4.0&debug=da&allow-collapsing=0&numdoc=20' + rearr)
        # синие cpa-офферы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 blue cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},  # на базовом множитель сохраняется в значение MnValue
                        {'tag': 'Meta', 'value': '0.3'},  # на мете множитель влияет только на CPM
                    ]
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': 'ozmCtRBXgUJgvxo4kHPBzg',
                        'BOOST_MULTIPLIER': '0.5',
                        'CPM': '15000',  # 0.3 * 0.5
                    },
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},  # 0.3 * 0.5
                },
            },
        )

        # белые cpa-офферы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},  # на базовом множитель сохраняется в значение MnValue
                        {'tag': 'Meta', 'value': '0.3'},  # на мете множитель влияет только на CPM
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'ySODAC91fN3VLfhOaIlLXg', 'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},  # 0.3 * 0.5
                },
            },
        )

        # cpc-офферы без модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': '0M_6Nugytl_hRabpNgvagw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                },
            },
        )

        # cpc-офферы привязанные к модели не бустятся (т.к. отключено схлапывание)
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'VkjX-08eqaaf0Q_MrNfQBw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                },
            },
        )

        # несхлопнутая cpa-модель не бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 1,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': False,
                    'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'modelStats': {'original': {'hasCpa': True}, 'updated': {'hasCpa': True}},
                },
            },
        )

        # несхлопнутая cpc-модель не бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'product',
                'id': 3,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': False,
                    'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'modelStats': {'original': {'hasCpa': False}, 'updated': {'hasCpa': False}},
                },
            },
        )

        response = self.report.request_json(
            'place=prime&text=market 4.0&debug=da&allow-collapsing=1&numdoc=20&fesh=1,2,3' + rearr
        )
        # схлопнутая из cpc-оффера cpa-модель не бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {
                        # у нас схлопнулся cpc-оффер, т.к. cpa-офферы бустятся с коэффициентом 0.5
                        'BOOST_MULTIPLIER': '1',
                        'WARE_MD5': 'UChUMwabn69TyQDNJkL6nQ',
                        'CPM': '30000',
                    },
                    # на мете схлопнувшаяся модель имеет статистику с cpa=True
                    'metaProperties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                    'modelStats': {
                        'updated': {'hasCpa': True},
                        'original': {'hasCpa': True},
                    },
                },
            },
        )

        response = self.report.request_json(
            'place=prime&text=market 4.0&debug=da&allow-collapsing=1&numdoc=20&fesh=3' + rearr
        )
        # схлопнутая из cpa-оффера модель бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.15'},  # 0.3 * 0.5
                        {'tag': 'Meta', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {
                        # у нас схлопнулся cpc-оффер, т.к. cpa-офферы бустятся с коэффициентом 0.5
                        'BOOST_MULTIPLIER': '0.5',
                        'WARE_MD5': 'q-u3w59LXka7z6srVl7zKw',
                        'CPM': '15000',
                    },
                    # на мете схлопнувшаяся модель имеет статистику с cpa=True
                    'metaProperties': {'BOOST_MULTIPLIER': '0.5', 'CPM': '15000'},
                    'modelStats': {
                        'updated': {'hasCpa': True},
                        'original': {'hasCpa': True},
                    },
                },
            },
        )

    def test_boost_cpa_offers_textless(self):
        """бестекстовый поиск
        На базовом бустятся cpa-офферы
        здесь не применяется переранжирование по метаформуле
        """

        rearr = '&rearr-factors=market_boost_cpa_offers_mnvalue_coef_textless=2.0;market_boost_cpa_offers_mnvalue_coef_text=0.5;market_metadoc_search=no;'

        response = self.report.request_json('place=prime&hid=1&debug=da&allow-collapsing=0&numdoc=20' + rearr)
        # синие cpa-документы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 blue cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # на базовом множитель сохраняется в значение MnValue
                    ]
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': 'ozmCtRBXgUJgvxo4kHPBzg',
                        'BOOST_MULTIPLIER': '2',
                        'CPM': '60000',  # 0.3 * 2.0
                    }
                },
            },
        )

        # белые cpa-офферы бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # на базовом множитель сохраняется в значение MnValue
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'ySODAC91fN3VLfhOaIlLXg', 'BOOST_MULTIPLIER': '2', 'CPM': '60000'}
                },
            },
        )

        # cpc-офферы без модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': '0M_6Nugytl_hRabpNgvagw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'}
                },
            },
        )

        # cpc-офферы привязанные к модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpc'},
                'entity': 'offer',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'properties': {'WARE_MD5': 'VkjX-08eqaaf0Q_MrNfQBw', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'}
                },
            },
        )

        # несхлопнутые модели не бустятся
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 1,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {'isCollapsed': False, 'properties': {'BOOST_MULTIPLIER': '1', 'CPM': '30000'}},
            },
        )

        response = self.report.request_json('place=prime&hid=1&debug=da&allow-collapsing=1&fesh=3' + rearr)
        # схлопнутая из cpa-оффера модель бустится
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 1,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},  # 0.3 * 2
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {'WARE_MD5': 'mxyTpoZMOeSEF-5Ia4PDhw', 'BOOST_MULTIPLIER': '2', 'CPM': '60000'},
                },
            },
        )

        # схлопнутая из cpc-оффера модель не бустится
        response = self.report.request_json('place=prime&hid=1&debug=da&allow-collapsing=1&fesh=2' + rearr)
        self.assertFragmentIn(
            response,
            {
                'titles': {'raw': 'market 4.0 cpa'},
                'entity': 'product',
                'id': 2,
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ]
                },
                'debug': {
                    'isCollapsed': True,
                    'properties': {'WARE_MD5': 'UChUMwabn69TyQDNJkL6nQ', 'BOOST_MULTIPLIER': '1', 'CPM': '30000'},
                },
            },
        )

    @classmethod
    def prepare_hide_non_cpa(cls):

        cls.index.models += [
            Model(hid=2, hyperid=21, title='cpa with cpc do'),
            Model(hid=2, hyperid=22, title='cpa with cpa do'),
            Model(hid=2, hyperid=23, title='cpc with cpc do'),
        ]

        cls.index.offers += [
            # при фильтре fesh=2,4 останется только cpc оффер в ДО
            Offer(hid=2, title='cpc', hyperid=21, fesh=2),
            Offer(hid=2, title='cpa', hyperid=21, fesh=3, cpa=Offer.CPA_REAL),
            # при фильтре fesh=2,4 останется только cpa оффер в ДО
            Offer(hid=2, title='cpc', hyperid=22, fesh=5),
            Offer(hid=2, title='cpa', hyperid=22, fesh=4, cpa=Offer.CPA_REAL),
            Offer(hid=2, title='cpc', hyperid=23, fesh=2),
            Offer(hid=2, title='cpc offer', fesh=2),
            Offer(hid=2, title='cpa offer', fesh=4, cpa=Offer.CPA_REAL),
        ]

    def test_hide_non_cpa(self):
        """Для снятия оффлайна заведен флаг market_hide_non_cpa
        под этим флагом все не cpa-документы с меты отфильтровываются
        (в том числе модели не имеющие cpa-ДО)
        """

        response = self.report.request_json(
            'place=prime&hid=2&debug=da&use-default-offers=1&allow-collapsing=1&fesh=2,4'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'cpc with cpc do'},
                            'offers': {'items': [{'cpa': NoKey('cpa')}]},
                        },
                        {
                            'entity': 'product',
                            'titles': {'raw': 'cpa with cpc do'},
                            'offers': {'items': [{'cpa': NoKey('cpa')}]},
                        },
                        {
                            'entity': 'product',
                            'titles': {'raw': 'cpa with cpa do'},
                            'offers': {'items': [{'cpa': 'real'}]},
                        },
                        {
                            "entity": "product",
                            "id": GreaterEq(Const.VMID_START),
                            "offers": {
                                "items": [{"marketSku": GreaterEq(Const.VMID_START), 'titles': {'raw': 'cpa offer'}}]
                            },
                        },
                        {'entity': 'offer', 'titles': {'raw': 'cpc offer'}, 'cpa': NoKey('cpa')},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

        response = self.report.request_json(
            'place=prime&hid=2&debug=da&use-default-offers=1&allow-collapsing=1&fesh=2,4'
            '&rearr-factors=market_hide_non_cpa=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'product',
                            'titles': {'raw': 'cpa with cpa do'},
                            'offers': {'items': [{'cpa': 'real'}]},
                        },
                        {
                            "entity": "product",
                            "id": GreaterEq(Const.VMID_START),
                            "offers": {
                                "items": [{"marketSku": GreaterEq(Const.VMID_START), 'titles': {'raw': 'cpa offer'}}]
                            },
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

    def test_bids_recommender_with_dsbs(self):
        response = self.report.request_json(
            'place=bids_recommender&hyperid=2&rids=213&fesh=2&bsformat=2'
            '&rearr-factors=market_ha_cpa_ctr_mult=0;market_boost_white_cpa_only_bid=120.0'
        )
        self.assertFragmentIn(
            response,
            {
                'position': [
                    {
                        'code': 4,
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
