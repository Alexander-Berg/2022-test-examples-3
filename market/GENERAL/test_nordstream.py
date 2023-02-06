#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime

from core.types import (
    Const,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    GpsCoord,
    Model,
    NavCategory,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    TimeInfo,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    Payment,
)
from core.logs import ErrorCodes
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    create_user_info,
    DeliveryItem,
    Destination,
)
from core.matcher import Absent, NotEmpty
from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

NORDSTREAM_OPTIONS = (
    ("&rearr-factors=market_nordstream_relevance=1;", True),
    ("&rearr-factors=market_nordstream_relevance=1;market_nordstream=0" + USE_DEPRECATED_DIRECT_SHIPPING_FLOW, False),
    ("&rearr-factors=market_nordstream_relevance=1;market_nordstream=1", True),
)
TODAY = datetime.date(2020, 5, 18)
DATE_FROM = TODAY + datetime.timedelta(days=2)


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[
            2000,
        ],
    )

    supplier_3p, supplier_3p_nordstream, supplier_3p_no_try_fashion, supplier_3p_fashion = [
        Shop(
            fesh=id,
            datafeed_id=id,
            warehouse_id=id,
            name=name,
            priority_region=213,
            tax_system=Tax.OSN,
            supplier_type=Shop.THIRD_PARTY,
            blue=Shop.BLUE_REAL,
            fulfillment_program=True,
        )
        for id, name in [
            (2, "3P supplier"),
            (3, "3P supplier (combinator only)"),
            (20, "3P supplier fashion without trying"),
            (30, "3P supplier fashion"),
        ]
    ]

    only_report_delivery_supplier = Shop(
        fesh=6,
        datafeed_id=6,
        warehouse_id=6,
        name='3P Supplier(report only)',
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=True,
    )

    all_suppliers = (
        supplier_3p,
        supplier_3p_nordstream,
        supplier_3p_fashion,
        supplier_3p_no_try_fashion,
        only_report_delivery_supplier,
    )


class _Offers(object):
    def _create_offer(id, shop, add_report_delivery, price=None, is_b2b=None, cargo_types=None):
        if add_report_delivery:
            delivery_buckets, pickup_buckets = [800 + _Shops.supplier_3p.fesh], [5000]
        else:
            delivery_buckets, pickup_buckets = [], []

        return BlueOffer(
            price=(price or 111),
            vat=Vat.VAT_10,
            feedid=shop.datafeed_id,
            offerid='blue.offer.{}'.format(id),
            waremd5=id,
            weight=5,
            blue_weight=5,
            dimensions=OfferDimensions(length=10, width=20, height=30),
            blue_dimensions=OfferDimensions(length=10, width=20, height=30),
            delivery_buckets=delivery_buckets,
            pickup_buckets=pickup_buckets,
            is_b2b=is_b2b,
            cargo_types=cargo_types,
        )

    offer_3p_fashion = _create_offer(
        'TestOffer_3P_fashion_g', _Shops.supplier_3p_fashion, True, price=10, cargo_types=[600]
    )
    offer_3p_in_fashion = _create_offer('TestOffer_3PInFashiong', _Shops.supplier_3p_fashion, True, price=10)
    offer_3p_no_try_fashion = _create_offer(
        'Nordstream_3PFashion_g', _Shops.supplier_3p_no_try_fashion, False, cargo_types=[600]
    )
    offer_3p = _create_offer('TestOffer_3P_________g', _Shops.supplier_3p, True, price=10)
    offer_3p_nordstream = _create_offer('Nordstream_3P_Offer__g', _Shops.supplier_3p_nordstream, False)
    offer_no_delivery_in_nordstream = _create_offer(
        'ReportOnly_Offer_____g', _Shops.only_report_delivery_supplier, True
    )

    all_offers_no_fashion = (
        (offer_3p, _Shops.supplier_3p),
        (offer_no_delivery_in_nordstream, _Shops.only_report_delivery_supplier),
        (offer_3p_nordstream, _Shops.supplier_3p_nordstream),
    )

    @classmethod
    def is_direct_shipping(cls, ware_md5):
        return ware_md5 in (
            cls.offer_3p.waremd5,
            cls.offer_3p_nordstream.waremd5,
            cls.offer_no_delivery_in_nordstream.waremd5,
            cls.b2b_offer_reachable.waremd5,
            cls.b2b_offer_unreachable.waremd5,
        )

    @classmethod
    def is_combinator_offer(cls, ware_md5):
        return ware_md5 in (cls.offer_3p_nordstream.waremd5,)

    @classmethod
    def is_offer_available(cls, offer, is_nordstream_enabled):
        if cls.is_combinator_offer(offer.waremd5):
            return is_nordstream_enabled
        else:
            return not is_nordstream_enabled or offer.waremd5 != cls.offer_no_delivery_in_nordstream.waremd5

    b2b_offer_reachable = _create_offer('B2BOffer_3P__________g', _Shops.supplier_3p, True, price=10, is_b2b=True)
    b2b_offer_unreachable = _create_offer('B2BOffer_3P_unreach__g', _Shops.supplier_3p_nordstream, False, is_b2b=True)

    b2b_offers = (
        (b2b_offer_reachable, _Shops.supplier_3p),
        (b2b_offer_unreachable, _Shops.supplier_3p_nordstream),
    )

    @classmethod
    def is_b2b_offer_available(cls, offer, is_available_for_business):
        if is_available_for_business:
            return offer == cls.b2b_offer_reachable
        else:
            return offer != cls.offer_no_delivery_in_nordstream


class CategoryTree(object):
    FIRST_WHITE_NID = 0
    FIRST_BLUE_NID = 1000


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.check_combinator_errors = True
        # Current date 18/05/2020 @ 23:16 MSK
        cls.settings.microseconds_for_disabled_random = 1589833013000000
        cls.settings.report_subrole = 'blue-main'
        cls.settings.lms_autogenerate = False
        cls.settings.nordstream_autogenerate = False

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
        ]

        cls.index.shops += [_Shops.blue_virtual_shop]
        cls.index.shops.extend(_Shops.all_suppliers)

        for shop in _Shops.all_suppliers:
            cls.dynamic.lms.append(DynamicWarehouseInfo(id=shop.warehouse_id, home_region=213))

        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicWarehousesPriorityInRegion(
                region=225, warehouses=[shop.warehouse_id for shop in _Shops.all_suppliers]
            ),
            DynamicDeliveryServiceInfo(
                9,
                "ПЭК",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=shop.warehouse_id,
                delivery_service_id=9,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213, packaging_time=TimeInfo(3))
                ],
            )
            for shop in (_Shops.supplier_3p, _Shops.only_report_delivery_supplier)
        ]

        cls.index.models += [Model(hyperid=1, hid=1)]
        cls.index.mskus += [
            MarketSku(
                title="Кубометр носков",
                hyperid=1,
                sku=10,
                blue_offers=[
                    _Offers.offer_3p_fashion,
                    _Offers.offer_3p_in_fashion,
                    _Offers.offer_3p_no_try_fashion,
                ],
            ),
            MarketSku(
                title="Кубометр газа",
                hyperid=1,
                sku=1,
                blue_offers=[
                    _Offers.offer_3p,
                    _Offers.offer_3p_nordstream,
                    _Offers.offer_no_delivery_in_nordstream,
                ],
            ),
            MarketSku(
                title="Кубометр газа для бизнеса",
                hyperid=2,
                sku=2,
                blue_offers=[
                    _Offers.b2b_offer_reachable,
                    _Offers.b2b_offer_unreachable,
                ],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2000,
                delivery_service_id=9,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=9, day_from=1, day_to=2, order_before=12, work_in_holiday=True, price=150
                ),
                working_days=[i for i in range(15)],
                gps_coord=GpsCoord(37.12, 55.32),
            )
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=800 + _Shops.supplier_3p.fesh,
                dc_bucket_id=800 + _Shops.supplier_3p.fesh,
                fesh=_Shops.supplier_3p.fesh,
                carriers=[9],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
            )
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5000,
                dc_bucket_id=5000,
                fesh=_Shops.supplier_3p.fesh,
                carriers=[9],
                options=[PickupOption(outlet_id=2000, day_from=1, day_to=2, price=150)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(
                _Shops.supplier_3p_fashion.warehouse_id, [_Shops.supplier_3p_fashion.warehouse_id], True
            ),
            DynamicWarehouseLink(
                _Shops.supplier_3p_no_try_fashion.warehouse_id, [_Shops.supplier_3p_no_try_fashion.warehouse_id], False
            ),
            DynamicWarehouseLink(_Shops.supplier_3p.warehouse_id, [_Shops.supplier_3p.warehouse_id]),
            DynamicWarehouseLink(
                _Shops.supplier_3p_nordstream.warehouse_id, [_Shops.supplier_3p_nordstream.warehouse_id]
            ),
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=20000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            prohibited_cargo_types=[2],
                            max_payment_weight=50,
                            density=10,
                            min_days=1,
                            max_days=3,
                            tariff_user_mask=tariff_user_mask,
                            is_trying_available=True,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=250,
                            max_dimensions=[100, 100, 100],
                            min_days=3,
                            max_days=4,
                            tariff_user_mask=tariff_user_mask,
                            is_trying_available=True,
                        ),
                    ],
                    225: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=50000,
                            max_dim_sum=220,
                            max_dimensions=[80, 80, 80],
                            min_days=5,
                            max_days=6,
                            is_trying_available=True,
                        )
                    ],
                },
            )
            for warehouse_id, tariff_user_mask in (
                (_Shops.supplier_3p.warehouse_id, 3),
                (_Shops.supplier_3p_nordstream.warehouse_id, 1),
                (_Shops.supplier_3p_fashion.warehouse_id, 3),
                (_Shops.supplier_3p_no_try_fashion.warehouse_id, 1),
            )
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=_Shops.blue_virtual_shop.fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_YANDEX,
                            Payment.PT_CASH_ON_DELIVERY,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_PREPAYMENT_CARD,
                        ],
                    ),
                ],
            )
        ]

    @classmethod
    def prepare_courier_options(cls):
        cls.settings.default_search_experiment_flags += [
            'enable_dsbs_combinator_request_in_actual_delivery=0',
        ]
        OFFERS = list(_Offers.all_offers_no_fashion) + list(_Offers.b2b_offers)
        for offer, shop in OFFERS:
            if offer == _Offers.offer_no_delivery_in_nordstream:
                continue
            price = 111
            if shop is _Shops.supplier_3p:
                price = 10
            for is_b2b in (False, True):
                user_info = create_user_info(b2b=is_b2b)
                cls.combinator.on_courier_options_request(
                    items=[
                        DeliveryItem(
                            required_count=1,
                            weight=5000,
                            dimensions=[10, 20, 30],
                            cargo_types=[],
                            offers=[
                                CombinatorOffer(
                                    shop_sku="blue.offer.{md5}".format(md5=offer.waremd5),
                                    shop_id=shop.warehouse_id,
                                    partner_id=shop.warehouse_id,
                                    available_count=1,
                                )
                            ],
                            price=price,
                        ),
                    ],
                    destination=Destination(region_id=213),
                    payment_methods=[],
                    total_price=price,
                    user_info=user_info,
                ).respond_with_courier_options(
                    options=[
                        create_delivery_option(
                            cost=5,
                            date_from=DATE_FROM,
                            date_to=DATE_FROM,
                            time_from=datetime.time(10, 0),
                            time_to=datetime.time(22, 0),
                            delivery_service_id=123,
                            payment_methods=[
                                PaymentMethod.PT_YANDEX,
                                PaymentMethod.PT_CARD_ON_DELIVERY,
                            ],
                            leave_at_the_door=True,
                        )
                    ],
                )

    @staticmethod
    def __offer_response(waremd5, shop, enable_nordstream, ff_warehouse=None):
        is_direct_shipping = _Offers.is_direct_shipping(waremd5)
        atSupplierWarehouse = not is_direct_shipping
        if ff_warehouse is None:
            if is_direct_shipping:
                ff_warehouse = shop.warehouse_id
            elif _Offers.is_combinator_offer(waremd5):
                ff_warehouse = _Shops.supplier_3p_nordstream.warehouse_id
            else:
                ff_warehouse = _Shops.supplier_3p.warehouse_id

        return {
            "entity": "offer",
            "supplier": {"name": shop.name, "warehouseId": shop.warehouse_id},
            "wareId": waremd5,
            "isFulfillment": True,
            "atSupplierWarehouse": atSupplierWarehouse,
            "fulfillmentWarehouse": ff_warehouse,
        }

    def __offers_response(self, waremd5, shop, has_count, enable_nordstream):
        return {
            "count": 1 if has_count else Absent(),
            "items": [self.__offer_response(waremd5, shop, enable_nordstream)],
        }

    def __check_response_is_empty(self, request):
        self.assertFragmentIn(
            self.report.request_json(request),
            {"search": {"total": 0, "results": []}},
            allow_different_len=False,
        )

    def test_sku_offers(self):
        """place=sku_offers: офферы с доставкой только через комбинатор заменяются на обычные
        TODO: под флагом nordstream должны находиться все офферы, кроме ReportOnly_Offer
        """

        def __check_sku_offer(offer, rgb, rearr, expected_shop, enable_nordstream, expected_offer=None):
            request = "place=sku_offers&rids=213&market-sku=1&offerid={}{}".format(offer.waremd5, rgb)
            if rearr is not None:
                request += rearr
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "offers": self.__offers_response(
                                (expected_offer or offer).waremd5, expected_shop, False, enable_nordstream
                            ),
                        }
                    ]
                },
                allow_different_len=False,
            )

        for rearr, enable_nordstream in NORDSTREAM_OPTIONS:
            for rgb in ('', '&rgb=blue'):
                for offer, shop in _Offers.all_offers_no_fashion:
                    if _Offers.is_offer_available(offer, enable_nordstream):
                        __check_sku_offer(offer, rgb, rearr, shop, enable_nordstream)
                    else:
                        # Замена оффера без доставки на 3P
                        __check_sku_offer(offer, rgb, rearr, _Shops.supplier_3p, enable_nordstream, _Offers.offer_3p)

    def test_prime(self):
        """place=prime отдаёт офферы с доставкой, без включённого nordstream это только репортные офферы"""
        PRIME_REQUEST = "place=prime&pp=18&rids=213&offerid={}{}"

        for rearr, enable_nordstream in NORDSTREAM_OPTIONS:
            for rgb in ('', '&rgb=blue'):
                for offer, shop in _Offers.all_offers_no_fashion:
                    request = PRIME_REQUEST.format(offer.waremd5, rgb) + rearr

                    if _Offers.is_offer_available(offer, enable_nordstream):
                        if rgb == '&rgb=blue':  # offers collapse to model
                            expected_results = [
                                {
                                    "entity": "product",
                                    "offers": self.__offers_response(offer.waremd5, shop, True, enable_nordstream),
                                }
                            ]
                        else:  # rgb=white -> no collapsing
                            expected_results = [self.__offer_response(offer.waremd5, shop, enable_nordstream)]

                        self.assertFragmentIn(
                            self.report.request_json(request),
                            {"search": {"total": 1, "results": expected_results}},
                            allow_different_len=False,
                        )
                    else:
                        self.__check_response_is_empty(request)

    def test_actual_delivery(self):
        """place=actual_delivery: комбинаторные офферы отбрасываются релевантностью без северного потока"""
        DELIVERY_REQUEST = (
            "place=actual_delivery&rgb=blue&rids=213&offers-list={}:1&rearr-factors=rty_delivery_cart=0&debug=1"
            + "&rearr-factors=disable_delivery_calculator_call_for_blue_offers=0"
        )
        once = True
        for rearr, enable_nordstream in NORDSTREAM_OPTIONS:
            for offer, shop in _Offers.all_offers_no_fashion:
                request = DELIVERY_REQUEST.format(offer.waremd5) + rearr

                if _Offers.is_offer_available(offer, enable_nordstream):
                    self.assertFragmentIn(
                        self.report.request_json(request),
                        {
                            "search": {
                                "total": 1,
                                "results": [
                                    {
                                        "entity": "deliveryGroup",
                                        "delivery": {
                                            "options": NotEmpty(),
                                        },
                                        "offers": [self.__offer_response(offer.waremd5, shop, enable_nordstream)],
                                    }
                                ],
                            }
                        },
                        allow_different_len=False,
                    )
                else:
                    self.__check_response_is_empty(request)
                    if once and enable_nordstream and offer.waremd5 == 'ReportOnly_Offer_____g':
                        self.error_log.expect(code=ErrorCodes.COMBINATOR_NO_DELIVERY_OPTIONS)
                        once = False

    def test_b2b_actual_delivery(self):
        DELIVERY_REQUEST = (
            "place=actual_delivery&rgb=blue&rids=213&offers-list={}:1&rearr-factors=rty_delivery_cart=0&debug=1"
            + "&rearr-factors=disable_delivery_calculator_call_for_blue_offers=0"
        )
        REARR_PARAMS = (
            ("", False),
            ("&available-for-business=1", True),
            ("&available-for-business=0", False),
        )
        OFFERS = list(_Offers.all_offers_no_fashion) + list(_Offers.b2b_offers)

        for rearr, is_available_for_business in REARR_PARAMS:
            for offer, shop in OFFERS:
                request = DELIVERY_REQUEST.format(offer.waremd5) + rearr
                if _Offers.is_b2b_offer_available(offer, is_available_for_business):
                    self.assertFragmentIn(
                        self.report.request_json(request),
                        {
                            "search": {
                                "total": 1,
                                "results": [
                                    {
                                        "entity": "deliveryGroup",
                                        "delivery": {
                                            "options": NotEmpty(),
                                        },
                                        "offers": [self.__offer_response(offer.waremd5, shop, True)],
                                    }
                                ],
                            }
                        },
                        allow_different_len=False,
                    )
                else:
                    self.__check_response_is_empty(request)

    def test_offer_info(self):
        OFFER_INFO_REQUEST = "place=offerinfo&regset=2&show-urls=direct&rids=213&offerid={}{}"

        for rearr, enable_nordstream in NORDSTREAM_OPTIONS:
            for rgb in ('', '&rgb=blue'):
                for offer, shop in _Offers.all_offers_no_fashion:
                    request = OFFER_INFO_REQUEST.format(offer.waremd5, rgb) + rearr

                    if _Offers.is_offer_available(offer, enable_nordstream):
                        self.assertFragmentIn(
                            self.report.request_json(request),
                            {
                                "search": {
                                    "total": 1,
                                    "results": [self.__offer_response(offer.waremd5, shop, enable_nordstream)],
                                }
                            },
                            allow_different_len=False,
                        )
                    else:
                        self.__check_response_is_empty(request)

    @classmethod
    def prepare_nids_info(cls):
        def blue_offer(shop_sku, supplier):
            return BlueOffer(
                feedid=supplier.datafeed_id,
                offerid=shop_sku,
                is_fulfillment=True,
                weight=3,
                blue_weight=3,
                dimensions=OfferDimensions(length=15, width=21, height=16),
                blue_dimensions=OfferDimensions(length=15, width=21, height=16),
            )

        cls.index.mskus += [
            # only combinator delivery (bucket 802 is needed for report)
            MarketSku(
                sku=2, hyperid=2, delivery_buckets=[800, 801], blue_offers=[blue_offer("o3p", _Shops.supplier_3p)]
            ),
        ]
        cls.index.models += [Model(hyperid=i, hid=i) for i in range(2, 5)]
        for i in range(1, 5):
            cls.index.navtree.append(
                NavCategory(nid=CategoryTree.FIRST_WHITE_NID + i, hid=i, is_blue=True, name='NavCategory {}'.format(i))
            )
            cls.index.navtree_blue.append(
                NavCategory(nid=CategoryTree.FIRST_BLUE_NID + i, hid=i, is_blue=True, name='NavCategory {}'.format(i))
            )

    def test_nids_info(self):
        """Проверка корректности работы навигационного дерева"""
        request = 'place=nids_info&rids=213&rgb=blue&use-multi-navigation-trees=1' + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        response = self.report.request_json(request)
        start_nid, root = CategoryTree.FIRST_WHITE_NID, Const.ROOT_NID
        self.assertFragmentIn(
            response,
            {
                "allowedNids": [
                    root,
                ]
            },
            allow_different_len=True,
        )
        # узла с nid=4 нет, для него доставка есть только от комбинатора
        self.assertFragmentIn(response, {"emptyNids": [start_nid + 4]}, allow_different_len=True)

    def test_stat_numbers(self):
        """Проверка корректности офферных статистик"""
        stats_request_template = 'place=stat_numbers&rids=213&rgb=blue&supplier-id={}'

        def check_supplier_stats(supplier_id, expected_count, rearr, no_buybox, expected_filter_reasons={}):
            request = stats_request_template.format(supplier_id) + rearr
            if no_buybox:
                request += "&nobuybox=1"
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {"result": {"offersCount": expected_count, "filters": expected_filter_reasons}},
                allow_different_len=False,
            )

        for rearr, enable_nordstream in NORDSTREAM_OPTIONS:
            for no_buybox in (True, False):
                check_supplier_stats(_Shops.blue_virtual_shop.fesh, 0, rearr, no_buybox)
                if enable_nordstream:
                    check_supplier_stats(_Shops.supplier_3p.fesh, 3 if no_buybox else 2, rearr, no_buybox)
                    check_supplier_stats(_Shops.supplier_3p_nordstream.fesh, 2, rearr, no_buybox)
                else:
                    check_supplier_stats(_Shops.supplier_3p.fesh, 2, rearr, no_buybox, {"DELIVERY_BLUE": 1})
                    check_supplier_stats(_Shops.supplier_3p_nordstream.fesh, 0, rearr, no_buybox, {"DELIVERY_BLUE": 2})

    def test_trying_available_for_supplier_with_trying(self):
        """
        Проверяем, что у поставщика с примеркой, при доступности примерки у служб доставки, приходит "isTryingAvailable": True
        """
        request = "place=offerinfo&regset=2&show-urls=direct&rids=213&offerid=" + _Offers.offer_3p_fashion.waremd5
        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"isPartialCheckoutAvailable": True, "delivery": {"options": [{"isTryingAvailable": True}]}}
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_trying_available_for_supplier_with_no_trying_for_offer(self):
        """
        Проверяем, что у поставщика с примеркой, при доступности примерки у служб доставки,
        приходит "isTryingAvailable": False т.к. оффер без примерки, без 600 карготипа
        """
        request = "place=offerinfo&regset=2&show-urls=direct&rids=213&offerid=" + _Offers.offer_3p_in_fashion.waremd5
        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"isPartialCheckoutAvailable": False, "delivery": {"options": [{"isTryingAvailable": False}]}}
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_trying_available_for_supplier_without_trying(self):
        """
        Проверяем, что у поставщика без примерки, при доступности примерки у служб доставки, приходит "isTryingAvailable": False
        """
        request = (
            "place=offerinfo&regset=2&show-urls=direct&rids=213&offerid=" + _Offers.offer_3p_no_try_fashion.waremd5
        )
        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"isPartialCheckoutAvailable": False, "delivery": {"options": [{"isTryingAvailable": False}]}}
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_trying_available_prime_for_supplier_with_trying(self):
        """
        Проверяем, что на прайме у поставщика с примеркой, при доступности примерки у служб доставки, приходит "isTryingAvailable": True
        """
        request = "place=prime&pp=18&rids=213&offerid=" + _Offers.offer_3p_fashion.waremd5
        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"isPartialCheckoutAvailable": True, "delivery": {"options": [{"isTryingAvailable": True}]}}
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_trying_available_prime_for_supplier_without_trying(self):
        """
        Проверяем, что на прайме у поставщика без примерки, при доступности примерки у служб доставки, приходит "isTryingAvailable": False
        """
        request = "place=prime&pp=18&rids=213&offerid=" + _Offers.offer_3p_no_try_fashion.waremd5
        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "search": {
                    "total": 1,
                    "results": [
                        {"isPartialCheckoutAvailable": False, "delivery": {"options": [{"isTryingAvailable": False}]}}
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_payment_method_restrictions(self):
        """
        Проверяем, что при задании флага market_only_prepayment_on_product_card, у всех синих офферов на выдаче пропадает оплата при получении
        (и картой и наличкой, остается только предоплата)
        """
        OFFERS = [
            _Offers.offer_3p,
            _Offers.offer_3p_nordstream,
        ]
        PREPAYMENT = ["YANDEX"]
        ALL_PAYMENT = ["CASH_ON_DELIVERY", "CARD_ON_DELIVERY"] + PREPAYMENT
        prepayment_flag = "&rearr-factors=market_only_prepayment_on_product_card={}"
        for offer in OFFERS:
            for flag in (0, 1):
                request = "place=prime&pp=18&rids=213&offerid=" + offer.waremd5 + prepayment_flag.format(flag)
                self.assertFragmentIn(
                    self.report.request_json(request),
                    {
                        "search": {
                            "total": 1,
                            "results": [
                                {
                                    "entity": "offer",
                                    "wareId": offer.waremd5,
                                    "delivery": {
                                        "options": [
                                            {
                                                "paymentMethods": PREPAYMENT if flag else ALL_PAYMENT,
                                            }
                                        ]
                                    },
                                    "payments": {
                                        "deliveryCard": not flag,
                                        "deliveryCash": not flag,
                                        "prepaymentCard": True,
                                        "prepaymentOther": False,
                                    },
                                }
                            ],
                        },
                        # проверяем, что фильтры соответствуют выдаче
                        "filters": [
                            {
                                "id": "payments",
                                "type": "enum",
                                "name": "Способы оплаты",
                                "values": [
                                    {
                                        "initialFound": 1,
                                        "found": 1,
                                        "value": "Картой на сайте",
                                        "id": "prepayment_card",
                                    },
                                    {
                                        "initialFound": 0 if flag else 1,
                                        "found": 0 if flag else 1,
                                        "value": "Картой курьеру",
                                        "id": "delivery_card",
                                    },
                                    {
                                        "initialFound": 0 if flag else 1,
                                        "found": 0 if flag else 1,
                                        "value": "Наличными курьеру",
                                        "id": "delivery_cash",
                                    },
                                ],
                            }
                        ],
                    },
                )
                # при фильтрации по предоплате, оффер всегда на выдаче
                request_with_filter = request + "&payments=prepayment_card"
                self.assertFragmentIn(
                    self.report.request_json(request_with_filter),
                    {
                        "search": {
                            "total": 1,
                            "results": [
                                {
                                    "entity": "offer",
                                    "wareId": offer.waremd5,
                                }
                            ],
                        }
                    },
                )
                # применяем фильтры по оплате при получении картой и наличными - оффер на выдаче только при выключенном флаге
                for payment in ("delivery_cash", "delivery_card"):
                    request_with_filter = request + "&payments=" + payment
                    if flag:
                        self.assertFragmentIn(
                            self.report.request_json(request_with_filter),
                            {
                                "search": {
                                    "total": 0 if flag else 1,
                                    "totalOffersBeforeFilters": 1,
                                    "results": [
                                        {
                                            "entity": "offer",
                                            "wareId": offer.waremd5,
                                        }
                                    ]
                                    if not flag
                                    else [],
                                }
                            },
                        )


if __name__ == '__main__':
    main()
