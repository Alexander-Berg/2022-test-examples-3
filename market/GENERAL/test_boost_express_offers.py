#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, timedelta
import calendar
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    ExpressDeliveryService,
    ExpressSupplier,
    MarketSku,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.matcher import Round, GreaterFloat
from core.types.offer import OfferDimensions
from core.types.express_partners import EatsWarehousesEncoder
from core.types.payment_methods import Payment
from core.testcase import TestCase, main


class _Constants:
    russia_rids = 225
    moscow_rids = 213

    category_id = 1
    category_express_id = 2

    class _Partners:
        fesh = 2
        blue_fesh = 3

        feed_id = 2
        blue_feed_id = 3

        warehouse_id = 10

        delivery_service_id = 100

        courier_bucket_dc_id = 1000
        courier_bucket_id = 2000
        blue_courier_bucket_id = 2001

        dc_day_from = 1
        dc_day_to = 1
        dc_delivery_cost = 50

    class _ExpressPartners:
        fesh = 4
        blue_fesh = 5

        feed_id = 4
        blue_feed_id = 5

        warehouse_id = 11
        blue_warehouse_id = 12

        delivery_service_id = 101

        courier_bucket_dc_id = 1001
        courier_bucket_id = 2003
        blue_courier_bucket_id = 2004

        dc_day_from = 0
        dc_day_to = 0
        dc_delivery_cost = 50


class _Shops:
    # Поставщик для синего оффера
    dropship_shop = Shop(
        fesh=_Constants._Partners.blue_fesh,
        datafeed_id=_Constants._Partners.blue_feed_id,
        warehouse_id=_Constants._Partners.warehouse_id,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    # Экспресс-поставщик для синего оффера (добавляется в express_partners)
    dropship_express_shop = Shop(
        fesh=_Constants._ExpressPartners.blue_fesh,
        datafeed_id=_Constants._ExpressPartners.blue_feed_id,
        warehouse_id=_Constants._ExpressPartners.blue_warehouse_id,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
        with_express_warehouse=True,
    )

    # просто магазин для DSBS
    plain_shop = Shop(
        fesh=_Constants._Partners.fesh,
        datafeed_id=_Constants._Partners.feed_id,
        warehouse_id=_Constants._Partners.warehouse_id,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        cpa=Shop.CPA_REAL,
    )

    # Магазин с экспресс-доставкой для DSBS (тоже добавляется в express_partners)
    express_shop = Shop(
        fesh=_Constants._ExpressPartners.fesh,
        datafeed_id=_Constants._ExpressPartners.feed_id,
        warehouse_id=_Constants._ExpressPartners.warehouse_id,
        priority_region=_Constants.moscow_rids,
        regions=[_Constants.moscow_rids],
        cpa=Shop.CPA_REAL,
        with_express_warehouse=True,
    )

    # Склады для отладки времени доставки
    express_shop_10min = Shop(
        fesh=100500,
        datafeed_id=100500,
        warehouse_id=100500,
        with_express_warehouse=True,
    )

    express_shop_50min = Shop(
        fesh=100501,
        datafeed_id=100501,
        warehouse_id=100501,
        with_express_warehouse=True,
    )


class _Offers:
    """
    Набор офферов для индекса. Два белых оффера (обычный и с экспресс-доставкой), два синих (аналогично).
    У каждого оффера свой магазин/поставщик, также для экспресс-доставки используются отдельные склады.
    Тест работает в предположении, что для проверки возможности экспресс-доставки используются данные LMS
    (экспресс-партнеры, склады и т.п.)
    """

    blue_offer = BlueOffer(
        offerid='dropship_sku',
        waremd5='DropshipWaremd5_____bQ',
        price=30,
        feedid=_Constants._Partners.blue_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._Partners.blue_fesh,
        delivery_buckets=[_Constants._Partners.blue_courier_bucket_id],
    )

    blue_express_offer = BlueOffer(
        offerid='dropship_sku_express',
        waremd5='DropshipWaremd5_____eQ',
        price=30,
        feedid=_Constants._ExpressPartners.blue_feed_id,
        weight=5,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Constants._ExpressPartners.blue_fesh,
        delivery_buckets=[_Constants._ExpressPartners.blue_courier_bucket_id],
        is_express=True,
    )

    blue_msku = MarketSku(
        title="Синий оффер",
        hyperid=_Constants.category_id,
        sku=1,
        blue_offers=[blue_offer],
    )

    blue_express_msku = MarketSku(
        title="Синий экспресс оффер",
        hyperid=_Constants.category_express_id,
        sku=2,
        blue_offers=[blue_express_offer],
    )

    white_msku = MarketSku(
        title="Белый оффер",
        hyperid=_Constants.category_id,
        sku=11,
    )

    white_express_msku = MarketSku(
        title="Белый экспресс оффер",
        hyperid=_Constants.category_express_id,
        sku=12,
    )

    white_offer = Offer(
        waremd5='white_offer______wwwww',
        hyperid=white_msku.hyperid,
        sku=white_msku.sku,
        fesh=_Constants._Partners.fesh,
        price=30,
        weight=5,
        title=white_msku.title,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        delivery_buckets=[_Constants._Partners.courier_bucket_id],
        is_express=False,
    )

    white_express_offer = Offer(
        waremd5='white_offer______eeeeQ',
        hyperid=white_express_msku.hyperid,
        sku=white_express_msku.sku,
        fesh=_Constants._ExpressPartners.fesh,
        price=30,
        weight=5,
        title=white_express_msku.title,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        delivery_buckets=[_Constants._ExpressPartners.courier_bucket_id],
        is_express=True,
    )


class _Requests:
    prime_request = (
        'place=prime' '&pp=18' '&rids=' + str(_Constants.moscow_rids) + '&local-offers-first=0' '&regset=2' '&debug=1'
    )


class T(TestCase):
    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Constants.moscow_rids, name="Москва", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops.dropship_shop,
            _Shops.dropship_express_shop,
            _Shops.plain_shop,
            _Shops.express_shop,
            _Shops.express_shop_10min,
            _Shops.express_shop_50min,
        ]

    @classmethod
    def prepare_express_partners(cls):
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.feed_id,
                supplier_id=_Constants._ExpressPartners.fesh,
                warehouse_id=_Constants._ExpressPartners.warehouse_id,
            ),
            ExpressSupplier(
                feed_id=_Constants._ExpressPartners.blue_feed_id,
                supplier_id=_Constants._ExpressPartners.blue_fesh,
                warehouse_id=_Constants._ExpressPartners.blue_warehouse_id,
            ),
        ]
        cls.index.express_partners.delivery_services += [
            ExpressDeliveryService(
                delivery_service_id=_Constants._ExpressPartners.delivery_service_id, delivery_price_for_user=350
            )
        ]

        cls.settings.disable_random = 1
        # set for all requests Tuesday Mar 02 2021 12:40:00 GMT+0300
        # since we test shop in moscow region - we should convert time to the moscow region time zone
        tuesday_afternoon = datetime(2021, 3, 2, 12, 40) - timedelta(hours=3)
        cls.settings.microseconds_for_disabled_random = calendar.timegm(tuesday_afternoon.timetuple()) * 1000000

    @classmethod
    def prepare_delivery_buckets(cls):
        def make_bucket(partners, is_blue):
            return DeliveryBucket(
                bucket_id=(partners.blue_courier_bucket_id if is_blue else partners.courier_bucket_id),
                dc_bucket_id=partners.courier_bucket_dc_id,
                fesh=(partners.blue_fesh if is_blue else partners.fesh),
                carriers=[partners.delivery_service_id],
                regional_options=[
                    RegionalDelivery(
                        rid=_Constants.moscow_rids,
                        options=[
                            DeliveryOption(
                                price=partners.dc_delivery_cost,
                                day_from=partners.dc_day_from,
                                day_to=partners.dc_day_to,
                            )
                        ],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )

        cls.index.delivery_buckets += [
            make_bucket(_Constants._Partners, True),
            make_bucket(_Constants._Partners, False),
            make_bucket(_Constants._ExpressPartners, True),
            make_bucket(_Constants._ExpressPartners, False),
        ]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Constants.russia_rids, _Constants.moscow_rids],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=_Constants._Partners.warehouse_id, priority=1),
                    WarehouseWithPriority(warehouse_id=_Constants._ExpressPartners.warehouse_id, priority=1),
                    WarehouseWithPriority(warehouse_id=_Constants._ExpressPartners.blue_warehouse_id, priority=1),
                ],
            )
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.mskus += [
            _Offers.blue_msku,
            _Offers.blue_express_msku,
            _Offers.white_msku,
            _Offers.white_express_msku,
        ]

        cls.index.offers += [
            _Offers.white_offer,
            _Offers.white_express_offer,
        ]

    def test_exp_flag_for_text_search(self):
        """
        При текстовом поиске на базовом и на мете бустятся x2 офферы с экспресс-доставкой, если активен флаг market_boost_express_delivery_offers_mnvalue_coef_text.
        Буст для бестекстового поиска применяться не должен.
        Схлопнутые из офферов модели и приоритезация региона пользователя отключены, поэтому в выдаче только entity==offer.
        Смотрим на значения формул на базовом (tag==Default), на значение множителя у оффера на базовом и на мете
        и на то, что CPM домножен на это значение. Значение формулы на мете (tag==Meta) логируется до применения множителя.
        """

        rearr = '&rearr-factors=market_boost_express_delivery_offers_mnvalue_coef_text=2.0;market_boost_express_delivery_offers_mnvalue_coef_textless=3.0'

        response = self.report.request_json(_Requests.prime_request + rearr + '&text=оффер&allow-collapsing=0')

        # Буст обычного белого CPA-оффера с экспресс-доставкой
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.white_express_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('2.0'),
                        'CPM': '60000',
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('2.0'),
                        'CPM': '60000',
                    },
                },
            },
        )

        # Буст синего оффера с экспресс-доставкой
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.6'},
                        {'tag': 'Meta', 'value': '0.3'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.blue_express_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('2.0'),
                        'CPM': '60000',
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': Round('2.0'),
                        'CPM': '60000',
                    },
                },
            },
        )

        # Офферы без экспресса не трогаем
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': False,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.blue_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('1.0'),
                        'CPM': '30000',
                    },
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': False,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.white_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('1.0'),
                        'CPM': '30000',
                    },
                },
            },
        )

        # Проверяем, что флаг > 1.0 по умолчанию
        response = self.report.request_json(_Requests.prime_request + rearr + '&text=оффер&allow-collapsing=0')

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': GreaterFloat(0.3)},
                        {'tag': 'Meta', 'value': GreaterFloat(0.15)},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.white_express_offer.waremd5,
                        'BOOST_MULTIPLIER': GreaterFloat(1.0),
                    },
                    'metaProperties': {
                        'BOOST_MULTIPLIER': GreaterFloat(1.0),
                    },
                },
            },
        )

    def test_exp_flag_for_textless_search(self):
        """
        В бестекстовой выдаче на базовом бустятся x3 офферы с экспресс-доставкой, если указан флаг market_boost_express_delivery_offers_mnvalue_coef_textless.
        Буст x2 для текстового запроса применяться не должен.
        На мете ранжирование не делается, поэтому на формулу на мете не смотрим.
        Схлопывание офферов в модели выключено.
        """
        rearr = '&rearr-factors=market_boost_express_delivery_offers_mnvalue_coef_text=2.0;market_boost_express_delivery_offers_mnvalue_coef_textless=3.0'

        """Все 4 оффера - в двух категориях"""
        response = self.report.request_json(_Requests.prime_request + rearr + '&hyperid=1,2&allow-collapsing=0')

        # Буст обычного белого CPA-оффера с экспресс-доставкой
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.9'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.white_express_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('3.0'),
                        'CPM': '90000',
                    },
                },
            },
        )

        # Буст синего оффера с экспресс-доставкой
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.9'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.blue_express_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('3.0'),
                        'CPM': '90000',
                    },
                },
            },
        )

        # Офферы без экспресса не трогаем
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': False,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.blue_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('1.0'),
                        'CPM': '30000',
                    },
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': False,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.3'},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.white_offer.waremd5,
                        'BOOST_MULTIPLIER': Round('1.0'),
                        'CPM': '30000',
                    },
                },
            },
        )

        # По умолчанию буст > 1.0
        response = self.report.request_json(_Requests.prime_request + rearr + '&hyperid=1,2&allow-collapsing=0')

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': GreaterFloat(0.3)},
                    ],
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': _Offers.white_express_offer.waremd5,
                        'BOOST_MULTIPLIER': GreaterFloat(1.0),
                    },
                },
            },
        )

    def test_exp_flag_for_collapsed_offers(self):
        """
        Здесь включаем allow-collapsing и смотрим на схлопнутые модели.
        Бустятся x3 модели, схлопнутые из экспресс-офферов (в нашем индексе это два оффера с hyperid == 2).
        Смотрим только на значение формулы на базовом (tag==Default), на значение множителя у оффера/модели
        и на то, что CPM домножен на это значение.
        """

        rearr = '&rearr-factors=market_boost_express_delivery_offers_mnvalue_coef_textless=3.0'

        response = self.report.request_json(_Requests.prime_request + rearr + '&hyperid=2&allow-collapsing=1')

        # Буст любого из офферов с экспресс-доставкой
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'trace': {
                    'fullFormulaInfo': [
                        {'tag': 'Default', 'value': '0.9'},
                    ],
                },
                'debug': {
                    'properties': {
                        'BOOST_MULTIPLIER': Round('3.0'),
                        'CPM': '90000',
                    },
                },
            },
        )

    def check_express_delivery_time_boosting(self, use_business_flag):
        '''
        Для закрытых на ночь магазинов экспресс бустинга нет
        Остальные магазины приоретизируются в зависимости от времени доставки.
        Самые быстрые имеют самый большой коэффициент
        '''

        rearr = (
            '&rearr-factors='
            'market_boost_express_delivery_offers_mnvalue_coef_textless=3.0;'
            'market_downgrade_express_closed_shops={closed};'
            'market_express_speed_coef={range};'
            'market_hyperlocal_context_mmap_version={mmap_version};'
            'market_use_business_offer=0;'
        )
        rearr += use_business_flag

        warehouses = '&eats-warehouses-compressed={}'

        def create_answer(ware_md5, coef, cpm):
            return {
                'entity': 'offer',
                'delivery': {
                    'isExpress': True,
                },
                'debug': {
                    'properties': {
                        'WARE_MD5': ware_md5,
                        'BOOST_MULTIPLIER': Round(coef),
                        'CPM': cpm,
                    },
                },
            }

        # Один склад доставляет за 20 минут, а второй за 40. Коэффициент разный
        express_wh_compressed_20_40_minutes = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.warehouse_id,
                wh_priority=1,
                delivery_time_minutes=20,
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.blue_warehouse_id,
                wh_priority=2,
                delivery_time_minutes=40,
            )
            .encode()
        )

        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.9, closed=1, mmap_version=3)
            + warehouses.format(express_wh_compressed_20_40_minutes)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(response, create_answer(_Offers.white_express_offer.waremd5, "3.0", "90000"))
        self.assertFragmentIn(
            response, create_answer(_Offers.blue_express_offer.waremd5, "1.2", "36000")  # 3.0 - (3.0 - 1.0) * 0.9
        )

        # Разный диапазон изменений (0,8 вместо 0,9) по-разному влияет на коэффициенты
        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.8, closed=1, mmap_version=3)
            + warehouses.format(express_wh_compressed_20_40_minutes)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            create_answer(_Offers.white_express_offer.waremd5, "3.0", "90000"),  # На самый быстрый склад не влияет
        )
        self.assertFragmentIn(
            response, create_answer(_Offers.blue_express_offer.waremd5, "1.4", "42000")  # 3.0 - (3.0 - 1.0) * 0.8
        )

        # четыре склада с разным сроком доставки
        # проверяем линейность
        express_wh_compressed_10_20_40_50_minutes = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=100500,
                wh_priority=1,
                delivery_time_minutes=10,
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.warehouse_id,
                wh_priority=1,
                delivery_time_minutes=20,
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.blue_warehouse_id,
                wh_priority=2,
                delivery_time_minutes=40,
            )
            .add_warehouse(
                wh_id=100501,
                wh_priority=1,
                delivery_time_minutes=50,
            )
            .encode()
        )

        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.9, closed=1, mmap_version=3)
            + warehouses.format(express_wh_compressed_10_20_40_50_minutes)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response,
            create_answer(
                _Offers.white_express_offer.waremd5,
                "2.55",  # 3.0 - (3.0 - 1.0) * 0.9 * (20мин - 10мин) / (50мин - 10мин)
                "76500",
            ),
        )
        self.assertFragmentIn(
            response,
            create_answer(
                _Offers.blue_express_offer.waremd5,
                "1.65",  # 3.0 - (3.0 - 1.0) * 0.9 * (40мин - 10мин) / (50мин - 10мин)
                "49500",
            ),
        )

        # Один склад закрыт. Для него бустинга нет
        express_wh_compressed_10_closed_40_50_minutes = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=100500,
                wh_priority=1,
                delivery_time_minutes=10,
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.warehouse_id,
                wh_priority=1,
                delivery_time_minutes=20,
                available_in_hours=10,  # Склад откроется через 10 часов
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.blue_warehouse_id,
                wh_priority=2,
                delivery_time_minutes=40,
            )
            .add_warehouse(
                wh_id=100501,
                wh_priority=1,
                delivery_time_minutes=50,
            )
            .encode()
        )

        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.9, closed=1, mmap_version=3)
            + warehouses.format(express_wh_compressed_10_closed_40_50_minutes)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response, create_answer(_Offers.white_express_offer.waremd5, "1.0", "30000")  # Бустинга нет
        )
        self.assertFragmentIn(
            response,
            create_answer(
                _Offers.blue_express_offer.waremd5,
                "1.65",  # 3.0 - (3.0 - 1.0) * 0.9 * (40мин - 10мин) / (50мин - 10мин)
                "49500",
            ),
        )

        # Но если дебустинг закрытого экспресса запрещен, то его коэффициент все-равно больше 1.
        # Считается как максимальный срок доставки
        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.9, closed=0, mmap_version=3)
            + warehouses.format(express_wh_compressed_10_closed_40_50_minutes)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(
            response, create_answer(_Offers.white_express_offer.waremd5, "1.2", "36000")  # Бустинг по минимуму
        )
        self.assertFragmentIn(
            response,
            create_answer(
                _Offers.blue_express_offer.waremd5,
                "1.65",  # 3.0 - (3.0 - 1.0) * 0.9 * (40мин - 10мин) / (50мин - 10мин)
                "49500",
            ),
        )

        # Ни у одного склада нет времени доставки
        # Коэффициенты не меняются
        express_wh_compressed = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.warehouse_id,
                wh_priority=1,
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.blue_warehouse_id,
                wh_priority=2,
            )
            .encode()
        )

        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.9, closed=1, mmap_version=3)
            + warehouses.format(express_wh_compressed)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(response, create_answer(_Offers.white_express_offer.waremd5, "3.0", "90000"))
        self.assertFragmentIn(response, create_answer(_Offers.blue_express_offer.waremd5, "3.0", "90000"))

        # Версия протокола не поддерживает передачу срока доставки. Переходный период
        # Коэффициенты не меняются
        express_wh_compressed = (
            EatsWarehousesEncoder()
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.warehouse_id,
                wh_priority=1,
                delivery_time_minutes=10,
            )
            .add_warehouse(
                wh_id=_Constants._ExpressPartners.blue_warehouse_id,
                wh_priority=2,
                delivery_time_minutes=20,
            )
            .encode()
        )

        response = self.report.request_json(
            _Requests.prime_request
            + rearr.format(range=0.9, closed=1, mmap_version=1)
            + warehouses.format(express_wh_compressed)
            + '&hyperid=1,2&allow-collapsing=0'
        )

        self.assertFragmentIn(response, create_answer(_Offers.white_express_offer.waremd5, "3.0", "90000"))
        self.assertFragmentIn(response, create_answer(_Offers.blue_express_offer.waremd5, "3.0", "90000"))

    def test_express_delivery_time_boosting(self):
        self.check_express_delivery_time_boosting('')

    def test_express_delivery_time_boosting_business_offer(self):
        '''
        Проверка работы ранжирования при использовании логики мультисклада.
        В этом режиме данные о гиперлокальных складах передаются в другом порядке
        '''
        self.check_express_delivery_time_boosting('&rearr-factors=market_use_business_offer=2')


if __name__ == '__main__':
    main()
