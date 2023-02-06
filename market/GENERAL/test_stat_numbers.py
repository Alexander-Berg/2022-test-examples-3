#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import copy
from socket import getfqdn
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicShop,
    DynamicSkuOffer,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from core.matcher import Absent
from core.logs import ErrorCodes

RID_RUSSIA = 225

RID_WHITE1_PROVINCE = 21
RID_WHITE1 = 213
RID_WHITE1_1 = 2131
RID_WHITE1_2 = 2132
RID_WHITE2 = 38
RID_WHITE3 = 37
RID_WHITE4_PROVINCE = 410
RID_WHITE4 = 411
RID_WHITE5 = 511
RID_WHITE6 = 611
RID_WHITE7 = 711
RID_WHITE8 = 811

RID_WHITE_AND_BLUE = 99

RID_BLUE1 = 101
RID_BLUE2 = 102
RID_BLUE2_1 = 1021
RID_BLUE2_2 = 1022
RID_BLUE3 = 103
RID_BLUE4 = 104
RID_BLUE5 = 105
RID_BLUE6 = 106
RID_BLUE7 = 107
RID_BLUE8 = 108
RID_BLUE9 = 109
RID_BLUE10 = 110

RID_FAR_AWAY = 8888

COUNT_FESH1 = 2
COUNT_FESH2 = 4
COUNT_FESH3 = 8
COUNT_FESH4 = 16
COUNT_FESH5 = 32
COUNT_FESH6 = 1
COUNT_FESH7 = 1
COUNT_FESH8 = 1

WH_1 = 11
WH_2 = 22
WH_3 = 33
WH_4 = 44
WH_5 = 55
WH_6 = 66
WH_7 = 77
WH_8 = 88
WH_9 = 99
WH_10 = 1010
WH_11 = 1111
WH_12 = 1212

CARRIER_W1 = 41
CARRIER_W2 = 42
CARRIER_B1 = 257
CARRIER_B2 = 258
CARRIER_B3 = 259
CARRIER_B4 = 260
CARRIER_B5 = 261
CARRIER_B6 = 262

BUCKET_WHITE_1 = 10
BUCKET_WHITE_PICK_1 = 11
BUCKET_WHITE_PICK_2 = 12
BUCKET_WHITE_PICK_3 = 13
BUCKET_BLUE_PICK_4 = 14
BUCKET_WHITE_2 = 20
BUCKET_WHITE_3 = 30
BUCKET_1 = 801
BUCKET_2 = 802
BUCKET_3 = 803
BUCKET_4 = 804
BUCKET_5 = 805
BUCKET_6 = 806
BUCKET_7 = 807
BUCKET_8 = 808
BUCKET_9 = 809
BUCKET_10 = 810
BUCKET_11 = 811
BUCKET_12 = 812
BUCKET_13 = 813


def get_warehouse_and_delivery_service(warehouse_id, service_id, enabled=True):
    date_switch_hours = [
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=RID_RUSSIA),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=RID_BLUE2),
    ]
    return DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=warehouse_id,
        delivery_service_id=service_id,
        operation_time=0,
        date_switch_time_infos=date_switch_hours,
        shipment_holidays_days_set_key=6,
        is_active=enabled,
    )


class T(TestCase):
    def query_rid(self, rid):
        return self.report.request_json('place=stat_numbers' + ('' if rid is None else '&rids=' + str(rid)))

    def check_rid_count(self, rid, count):
        self.assertFragmentIn(self.query_rid(rid), {"result": {'offersCount': count}})

    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0', 'market_nordstream_dsbs=0']

        # Общие настройки
        cls.index.regiontree += [
            Region(
                rid=RID_WHITE1_PROVINCE,
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=RID_WHITE1,
                        region_type=Region.CITY,
                        children=[
                            Region(rid=RID_WHITE1_1, region_type=Region.CITY_DISTRICT),
                            Region(rid=RID_WHITE1_2, region_type=Region.CITY_DISTRICT),
                        ],
                    ),
                ],
            ),
            Region(rid=RID_WHITE2, region_type=Region.CITY),
            Region(rid=RID_WHITE3, region_type=Region.CITY),
            Region(
                rid=RID_WHITE4_PROVINCE,
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=RID_WHITE4, region_type=Region.CITY),
                ],
            ),
            Region(rid=RID_WHITE5, region_type=Region.CITY),
            Region(rid=RID_WHITE6, region_type=Region.CITY),
            Region(rid=RID_WHITE7, region_type=Region.CITY),
            Region(rid=RID_WHITE_AND_BLUE, region_type=Region.CITY),
            Region(rid=RID_BLUE1, region_type=Region.CITY),
            Region(
                rid=RID_BLUE2,
                region_type=Region.CITY,
                children=[
                    Region(rid=RID_BLUE2_1, region_type=Region.CITY_DISTRICT),
                    Region(rid=RID_BLUE2_2, region_type=Region.CITY_DISTRICT),
                ],
            ),
            Region(rid=RID_BLUE3, region_type=Region.CITY),
            Region(rid=RID_BLUE4, region_type=Region.CITY),
            Region(rid=RID_FAR_AWAY, region_type=Region.FEDERATIVE_SUBJECT),
        ]

    @classmethod
    def prepare_simple_rids(cls):
        # fesh [000 ... 099]
        for shop_id, region in (
            # локальная доставка (белая)
            (1, RID_WHITE1),
            (2, RID_WHITE2),
            (3, RID_WHITE1),
            (4, RID_WHITE1_2),
            # региональная доставка (белая)
            (5, RID_WHITE_AND_BLUE),
        ):
            cls.index.shops.append(Shop(fesh=shop_id, datafeed_id=20 + shop_id, priority_region=region))

        cls.index.shops.append(
            Shop(fesh=6, datafeed_id=26, priority_region=None, delivery_service_outlets=[555], cpa=Shop.CPA_REAL)
        )
        # dsbs shop
        cls.index.shops.append(
            Shop(fesh=7, datafeed_id=27, priority_region=RID_WHITE8, regions=[RID_WHITE8], cpa=Shop.CPA_REAL)
        )

        cls.index.shops.append(
            Shop(fesh=8, datafeed_id=28, priority_region=RID_WHITE8, regions=[RID_WHITE8], cpa=Shop.CPA_NO)
        )

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=BUCKET_WHITE_1,
                fesh=5,
                regional_options=[
                    RegionalDelivery(rid=RID_WHITE5, options=[DeliveryOption(price=40, day_from=0, day_to=0)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=BUCKET_WHITE_2,
                fesh=7,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=RID_WHITE8, options=[DeliveryOption(price=40, day_from=0, day_to=0)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=BUCKET_WHITE_3,
                fesh=8,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(rid=RID_WHITE8, options=[DeliveryOption(price=40, day_from=0, day_to=0)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=3, region=RID_WHITE6, point_type=Outlet.FOR_PICKUP, point_id=1),
            Outlet(fesh=3, region=RID_WHITE6, point_type=Outlet.FOR_STORE, point_id=2),
            Outlet(region=RID_WHITE7, point_type=Outlet.FOR_POST_TERM, point_id=555, delivery_service_id=103),
            Outlet(region=RID_BLUE3, point_type=Outlet.FOR_PICKUP, point_id=100),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=BUCKET_WHITE_PICK_1,
                fesh=3,
                carriers=[CARRIER_W1],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=BUCKET_WHITE_PICK_2,
                fesh=3,
                carriers=[CARRIER_W1],
                options=[PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=BUCKET_WHITE_PICK_3,
                carriers=[CARRIER_W2],
                options=[PickupOption(outlet_id=555)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=BUCKET_BLUE_PICK_4,
                carriers=[CARRIER_B2],
                options=[PickupOption(outlet_id=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        local_delivery = [DeliveryOption(price=1, day_from=0, day_to=2, order_before=24)]
        cls.index.offers += (
            [Offer(title='T1_' + str(n), fesh=1, delivery_options=local_delivery) for n in range(COUNT_FESH1)]
            + [Offer(title='T2_' + str(n), fesh=2, delivery_options=local_delivery) for n in range(COUNT_FESH2)]
            + [
                Offer(
                    title='T3_' + str(n),
                    fesh=3,
                    delivery_options=local_delivery,
                    pickup_buckets=[BUCKET_WHITE_PICK_1, BUCKET_WHITE_PICK_2],
                )
                for n in range(COUNT_FESH3)
            ]
            + [Offer(title='T4_' + str(n), fesh=4, delivery_options=local_delivery) for n in range(COUNT_FESH4)]
            + [
                Offer(title='T5_' + str(n), fesh=5, delivery_options=local_delivery, delivery_buckets=[BUCKET_WHITE_1])
                for n in range(COUNT_FESH5)
            ]
            + [
                Offer(
                    title='T6_' + str(n),
                    fesh=6,
                    has_delivery_options=False,
                    store=False,
                    post_term_delivery=True,
                    cpa=Offer.CPA_REAL,
                    pickup_buckets=[BUCKET_WHITE_PICK_3],
                )
                for n in range(COUNT_FESH6)
            ]
            + [
                Offer(
                    title='T7_w_cpa_' + str(n),
                    fesh=7,
                    delivery_options=local_delivery,
                    delivery_buckets=[BUCKET_WHITE_2],
                    cpa=Offer.CPA_REAL,
                )
                for n in range(COUNT_FESH7)
            ]
            + [
                Offer(
                    title='T8_' + str(n),
                    fesh=8,
                    delivery_options=local_delivery,
                    delivery_buckets=[BUCKET_WHITE_3],
                    cpa=Offer.CPA_NO,
                )
                for n in range(COUNT_FESH8)
            ]
        )

    def test_errors(self):
        # не числовой rids
        self.query_rid('foo')
        self.error_log.expect(message='can not parse')

        # очень большое число
        self.query_rid('999999999999')
        self.error_log.expect(message='can not parse')

    def test_hostname(self):
        response = self.query_rid(None)
        self.assertFragmentIn(response, {"result": {'hostname': getfqdn()}})

    def test_shop_rids(self):
        # общее кол-во документов
        msku_count = len(self.index.mskus)
        total_offer_count = sum(
            [
                COUNT_FESH1,
                COUNT_FESH2,
                COUNT_FESH3,
                COUNT_FESH4,
                COUNT_FESH5,
                COUNT_FESH6,
                COUNT_FESH7,
                COUNT_FESH8,
                msku_count,
            ]
        )
        self.check_rid_count(None, total_offer_count)
        self.check_rid_count(0, total_offer_count)

        # с фильтром по региону
        self.check_rid_count(RID_WHITE1, COUNT_FESH1 + COUNT_FESH3)
        self.check_rid_count(RID_WHITE1_1, COUNT_FESH1 + COUNT_FESH3)
        self.check_rid_count(RID_WHITE1_2, COUNT_FESH1 + COUNT_FESH3 + COUNT_FESH4)
        self.check_rid_count(RID_WHITE2, COUNT_FESH2)
        self.check_rid_count(RID_WHITE3, 0)
        self.check_rid_count(RID_WHITE4, 0)
        self.check_rid_count(RID_WHITE5, COUNT_FESH5)  # белая региональная
        self.check_rid_count(RID_WHITE6, COUNT_FESH3)  # белая региональная, пункт выдачи
        self.check_rid_count(RID_WHITE7, COUNT_FESH6)  # белая региональная, терминал
        self.check_rid_count(RID_WHITE_AND_BLUE, COUNT_FESH5 + 1)
        self.check_rid_count(RID_FAR_AWAY, 0)

    def test_white_shop_filter(self):
        for feed, fesh, count, rid in ((21, 1, COUNT_FESH1, RID_WHITE1), (22, 2, COUNT_FESH2, RID_WHITE2)):
            request = 'place=stat_numbers&rids={}'.format(rid)
            for param in ('&feed_shoffer_id={}-*'.format(feed), '&fesh={}'.format(fesh)):
                response = self.report.request_json(request + param)
                self.assertFragmentIn(response, {"result": {'offersCount': count}})

    @classmethod
    def prepare_blue_offers(cls):
        cls.settings.lms_autogenerate = False

        # fesh [100 ... 199]
        cls.index.shops += [
            Shop(
                fesh=shop_id,
                datafeed_id=feed_id,
                virtual_shop_color=color,
                warehouse_id=wh_id,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                blue=None if color is Shop.VIRTUAL_SHOP_BLUE else Shop.BLUE_REAL,
                fulfillment_virtual=color == Shop.VIRTUAL_SHOP_BLUE,
                supplier_type=supplier_type,
                is_supplier=supplier_type is not None,
                fulfillment_program=shop_id != 107,
            )
            for shop_id, feed_id, color, wh_id, supplier_type in [
                (101, 1, Shop.VIRTUAL_SHOP_BLUE, None, None),
                (102, 2, None, WH_1, Shop.FIRST_PARTY),
                (102, 3, None, WH_2, Shop.FIRST_PARTY),
                (102, 4, None, WH_3, Shop.FIRST_PARTY),
                (102, 5, None, WH_4, Shop.FIRST_PARTY),
                (103, 6, None, None, Shop.FIRST_PARTY),
                (104, 7, None, WH_5, Shop.FIRST_PARTY),
                (105, 8, None, WH_5, Shop.FIRST_PARTY),
                (106, 9, None, WH_6, Shop.FIRST_PARTY),
                (107, 10, None, WH_7, Shop.FIRST_PARTY),
                (108, 11, None, WH_8, Shop.FIRST_PARTY),
                (109, 12, None, WH_9, Shop.FIRST_PARTY),
                (110, 13, None, WH_10, Shop.FIRST_PARTY),
                (111, 14, None, WH_10, Shop.THIRD_PARTY),
                (465852, 15, None, WH_11, Shop.FIRST_PARTY),
                (113, 16, None, WH_12, Shop.THIRD_PARTY),
            ]
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WH_1, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_2, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_3, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_4, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_5, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_6, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_7, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_8, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_9, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_10, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_11, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=WH_12, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_1, warehouse_to=WH_1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_2, warehouse_to=WH_2),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_3, warehouse_to=WH_3),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_4, warehouse_to=WH_4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_5, warehouse_to=WH_5),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_6, warehouse_to=WH_6),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_7, warehouse_to=WH_7),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_8, warehouse_to=WH_8),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_9, warehouse_to=WH_9),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_10, warehouse_to=WH_10),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_11, warehouse_to=WH_11),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_12, warehouse_to=WH_12),
            DynamicWarehousesPriorityInRegion(
                region=RID_BLUE2,
                warehouses=[
                    WH_1,
                ],
            ),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE3, warehouses=[WH_2, WH_6]),
            DynamicWarehousesPriorityInRegion(
                region=RID_BLUE4,
                warehouses=[
                    WH_3,
                ],
            ),
            DynamicWarehousesPriorityInRegion(
                region=RID_RUSSIA,
                warehouses=[
                    WH_4,
                ],
            ),
            DynamicWarehousesPriorityInRegion(
                region=RID_WHITE_AND_BLUE,
                warehouses=[
                    WH_5,
                ],
            ),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE5, warehouses=[WH_7, WH_8]),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE6, warehouses=[WH_9]),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE7, warehouses=[WH_10]),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE8, warehouses=[WH_10]),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE9, warehouses=[WH_11]),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE10, warehouses=[WH_12]),
            get_warehouse_and_delivery_service(WH_1, CARRIER_B1),
            get_warehouse_and_delivery_service(WH_2, CARRIER_B2),
            get_warehouse_and_delivery_service(WH_3, CARRIER_B2),
            get_warehouse_and_delivery_service(WH_5, CARRIER_B2),
            get_warehouse_and_delivery_service(WH_6, CARRIER_B2),
            get_warehouse_and_delivery_service(WH_7, CARRIER_B1),
            get_warehouse_and_delivery_service(WH_8, CARRIER_B1),
            get_warehouse_and_delivery_service(WH_9, CARRIER_B3),
            get_warehouse_and_delivery_service(WH_10, CARRIER_B4),
            get_warehouse_and_delivery_service(WH_11, CARRIER_B5),
            get_warehouse_and_delivery_service(WH_12, CARRIER_B6),
            DynamicDeliveryServiceInfo(CARRIER_B1, "B_" + str(CARRIER_B1)),
            DynamicDeliveryServiceInfo(CARRIER_B2, "B_" + str(CARRIER_B2)),
            DynamicDeliveryServiceInfo(CARRIER_B3, "B_" + str(CARRIER_B3)),
            DynamicDeliveryServiceInfo(CARRIER_B4, "B_" + str(CARRIER_B4)),
            DynamicDeliveryServiceInfo(CARRIER_B5, "B_" + str(CARRIER_B5)),
            DynamicDeliveryServiceInfo(CARRIER_B6, "B_" + str(CARRIER_B6)),
        ]

        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

        cls.index.models += [
            Model(hyperid=1, hid=1, title='model 1'),
            Model(hyperid=2, hid=2, title='model 2'),
            Model(hyperid=3, hid=3, title='pmodel 1', is_pmodel='1', vendor_id=3),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=bucket_id,
                carriers=[carrier_id],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=region_id, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=RID_BLUE2_1, forbidden=True),
                ],
            )
            for bucket_id, carrier_id, region_id in [
                (BUCKET_1, CARRIER_B1, RID_BLUE2),
                (BUCKET_2, CARRIER_B2, RID_BLUE3),
                (BUCKET_3, CARRIER_B2, RID_BLUE4),
                (BUCKET_4, CARRIER_B1, RID_RUSSIA),
                (BUCKET_5, CARRIER_B2, RID_WHITE_AND_BLUE),
                (BUCKET_6, CARRIER_B2, RID_BLUE3),
                (BUCKET_7, CARRIER_B1, RID_BLUE5),
                (BUCKET_8, CARRIER_B1, RID_BLUE5),
                (BUCKET_9, CARRIER_B3, RID_BLUE6),
                (BUCKET_10, CARRIER_B4, RID_BLUE7),
                (BUCKET_11, CARRIER_B4, RID_BLUE8),
                (BUCKET_12, CARRIER_B5, RID_BLUE9),
                (BUCKET_13, CARRIER_B6, RID_BLUE10),
            ]
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=7,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.1.18',
                        waremd5='Sku1Price78IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=7,
                        price_old=7,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.1.17',
                        waremd5='Sku1Price77IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=6,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.2',
                        waremd5='Sku1Price6-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=8,
                        vat=Vat.VAT_10,
                        feedid=5,
                        offerid='blue.offer.1.3',
                        waremd5='Sku1Price8-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=8,
                        vat=Vat.VAT_10,
                        feedid=6,
                        offerid='blue.offer.1.4',
                        waremd5='Sku1Price9-IiLVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_1, BUCKET_2, BUCKET_4],
                pickup_buckets=[BUCKET_BLUE_PICK_4],
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=6,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.2.1',
                        waremd5='Sku2Price6-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=7,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.2.2',
                        waremd5='Sku2Price7-IiLVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_1, BUCKET_2],
                pickup_buckets=[BUCKET_BLUE_PICK_4],
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=1,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer3.1',
                        waremd5='Sku3Price5-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=6,
                        vat=Vat.VAT_10,
                        feedid=4,
                        offerid='blue.offer3.2',
                        waremd5='Sku3Price6-IiLVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_2, BUCKET_3],
                pickup_buckets=[BUCKET_BLUE_PICK_4],
            ),
            MarketSku(
                title="blue offer sku4",
                hyperid=1,
                sku=4,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=7,
                        offerid='blue.offer4.1',
                        waremd5='Sku4Price1-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=2,
                        vat=Vat.VAT_10,
                        feedid=8,
                        offerid='blue.offer4.2',
                        waremd5='Sku4Price2-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=3,
                        vat=Vat.VAT_10,
                        feedid=8,
                        offerid='blue.offer4.3',
                        waremd5='Sku4Price3-IiLVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_5],
            ),
            MarketSku(
                title="blue offer sku5",
                hyperid=2,
                sku=5,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=9,
                        offerid='blue.offer5.1',
                        waremd5='Sku5Price1-IiLVm1Goleg',
                        model_title='model 2',
                    ),
                ],
                delivery_buckets=[BUCKET_6],
            ),
            MarketSku(
                title="blue offer sku6",
                hyperid=1,
                sku=6,
                blue_offers=[
                    BlueOffer(
                        price=2,
                        vat=Vat.VAT_10,
                        feedid=10,
                        offerid='blue.offer6.2',
                        waremd5='Sku6Price2-IiLVm1Goleg',
                        is_fulfillment=False,
                    ),
                ],
                delivery_buckets=[BUCKET_7],
            ),
            MarketSku(
                title="blue offer sku7",
                hyperid=1,
                sku=7,
                blue_offers=[
                    BlueOffer(
                        price=2,
                        vat=Vat.VAT_10,
                        feedid=11,
                        offerid='blue.offer7.2',
                        waremd5='Sku7Price2-IiLVm1Goleg',
                    ),
                    BlueOffer(
                        price=3,
                        vat=Vat.VAT_10,
                        feedid=11,
                        offerid='blue.offer7.3',
                        waremd5='Sku7Price3-IiLVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_8],
            ),
            MarketSku(
                title="blue offer sku8",
                hyperid=3,
                vendor_id=3,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=8,
                blue_offers=[
                    BlueOffer(
                        price=2,
                        vat=Vat.VAT_10,
                        feedid=12,
                        offerid='blue.offer8.1',
                        waremd5='Sku8Price1-IiLVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_9],
            ),
            MarketSku(
                title="blue offer sku9",
                hyperid=4,
                vendor_id=3,
                sku=9,
                blue_offers=[
                    BlueOffer(
                        price=2, vat=Vat.VAT_10, feedid=13, offerid='blue.offer9.1', waremd5='Sku9Price1-IiLVm1Goleg'
                    ),
                ],
                delivery_buckets=[BUCKET_10],
            ),
            MarketSku(
                title="blue offer sku10",
                hyperid=4,
                vendor_id=3,
                sku=10,
                blue_offers=[
                    BlueOffer(
                        price=3, vat=Vat.VAT_10, feedid=14, offerid='blue.offer10.1', waremd5='Sku10Price1-ILVm1Goleg'
                    ),
                ],
                delivery_buckets=[BUCKET_11],
            ),
            MarketSku(
                title="blue offer sku11",
                hyperid=4,
                vendor_id=3,
                sku=11,
                blue_offers=[
                    BlueOffer(
                        price=3,
                        vat=Vat.VAT_10,
                        feedid=15,
                        offerid='blue.offer11.1',
                        waremd5='Sku11Price1-ILVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_12],
            ),
            MarketSku(
                title="blue offer sku12",
                hyperid=5,
                vendor_id=3,
                sku=12,
                blue_offers=[
                    BlueOffer(
                        price=1,
                        vat=Vat.VAT_10,
                        feedid=16,
                        offerid='blue.offer12.1',
                        waremd5='Sku12Price1-ILVm1Goleg',
                    ),
                ],
                delivery_buckets=[BUCKET_13],
            ),
            MarketSku(
                title="blue offer sku13",
                hyperid=5,
                vendor_id=3,
                sku=13,
                blue_offers=[
                    BlueOffer(
                        price=2,
                        vat=Vat.VAT_10,
                        feedid=16,
                        offerid='blue.offer13.2',
                        waremd5='Sku13Price1-ILVm1Goleg',
                        is_express=True,
                    ),
                ],
                delivery_buckets=[BUCKET_13],
            ),
        ]

    def query_blue(self, rid, extra=None):
        return self.report.request_json(
            'place=stat_numbers&rgb=blue'
            + ('' if rid is None else '&rids=' + str(rid))
            + ('' if extra is None else '&' + extra)
        )

    def check_blue_count(self, rid, count, extra=None):
        self.assertFragmentIn(self.query_blue(rid, extra), {"result": {'offersCount': count}})

    def query_green(self, rid, extra=None):
        return self.report.request_json(
            'place=stat_numbers&rgb=green'
            + ('' if rid is None else '&rids=' + str(rid))
            + ('' if extra is None else '&' + extra)
        )

    def check_green_count(self, rid, count, extra=None):
        self.assertFragmentIn(self.query_green(rid, extra), {"result": {'offersCount': count}})

    def test_shop_rids_blue(self):
        # без привязки к региону
        total_msku_count = len(self.index.mskus)
        self.check_blue_count(None, total_msku_count)
        self.check_blue_count(0, total_msku_count)

        # с фильтрацией по id региона
        self.check_blue_count(RID_RUSSIA, 0)
        self.check_blue_count(RID_WHITE1_PROVINCE, 0)
        self.check_blue_count(RID_WHITE1, 0)
        self.check_blue_count(RID_BLUE4, 1)
        self.check_blue_count(RID_FAR_AWAY, 0)
        self.check_blue_count(RID_BLUE3, 4)
        self.check_blue_count(RID_BLUE2, 2)
        self.check_blue_count(RID_BLUE2_1, 0)  # запрещённый регион
        self.check_blue_count(RID_BLUE2_2, 2)
        self.check_blue_count(RID_WHITE_AND_BLUE, 1)

        # только со скидкой
        self.check_blue_count(RID_BLUE2, 1, "discount_only=1&nobuybox=1")

        # по складу
        self.check_blue_count(RID_BLUE3, 3, "warehouse_id={}".format(WH_2))
        self.check_blue_count(RID_BLUE3, 1, "warehouse_id={}".format(WH_6))
        self.check_blue_count(RID_BLUE3, 0, "warehouse_id={}".format(WH_1))
        self.check_blue_count(RID_BLUE4, 0, "warehouse_id={}".format(WH_1))
        self.check_blue_count(RID_BLUE4, 1, "warehouse_id={}".format(WH_3))

        self.check_blue_count(RID_BLUE3, 0, "warehouse_id={}".format(0))
        self.error_log.expect(code=ErrorCodes.CGI_UNKNOWN_WAREHOUSE_ID)

        # по поставщику
        self.check_blue_count(RID_BLUE3, 1, "supplier-id={}".format(106))
        self.check_blue_count(RID_BLUE3, 0, "supplier-id={}".format(666))
        self.check_green_count(RID_BLUE3, 1, "supplier-id={}".format(106))
        self.check_green_count(RID_BLUE3, 0, "supplier-id={}".format(666))

        # по модели
        self.check_blue_count(RID_BLUE3, 1, "hyperid={}".format(2))
        self.check_blue_count(RID_BLUE3, 1, "modelid={}".format(2))

        # кол-во Market SKU в рамках данной модели без фильтрации по региону
        # ('&rids=0' или вообще не указан)
        self.check_blue_count(None, 6, "hyperid={}".format(1))
        self.check_blue_count(None, 1, "hyperid={}".format(2))
        self.check_blue_count(None, 1, "hyperid={}".format(3))
        self.check_blue_count(0, 6, "hyperid={}".format(1))
        self.check_blue_count(0, 1, "hyperid={}".format(2))
        self.check_blue_count(0, 1, "hyperid={}".format(3))

        # фулфиллмент
        self.check_blue_count(
            RID_BLUE5, 5, "nobuybox=1"
        )  # 2 - RID_RUSSIA (со скидкой и без), 2 - RID_BLUE5 + no FF, 1 - RID_BLUE5 + FF
        self.check_blue_count(RID_BLUE5, 2)
        self.check_blue_count(RID_BLUE5, 1, "fulfillment-offers-only=1")

        # отключение buybox
        self.check_blue_count(
            RID_RUSSIA, 2, "nobuybox=1"
        )  # при отключении buybox открывается два msku=1 (со скидкой и без) по RID_RUSSIA
        self.check_blue_count(RID_WHITE_AND_BLUE, 5, "nobuybox=1")

        # динамики
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=102, sku='blue.offer3.2', warehouse_id=WH_3),
        ]
        self.check_blue_count(RID_BLUE4, 0)
        self.check_blue_count(RID_BLUE3, 4)
        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            DynamicShop(102),
            DynamicShop(104),
            DynamicShop(105),
        ]
        self.check_blue_count(RID_BLUE3, 1)
        self.check_blue_count(RID_BLUE2, 0)
        self.check_blue_count(RID_WHITE_AND_BLUE, 0)

    def test_filter_reason(self):
        '''
        Проверяем показ причин фильтрации
        '''
        response = self.report.request_json('place=stat_numbers&rids=213&rgb=blue')
        self.assertFragmentIn(response, {"result": {"filters": {"DELIVERY_BLUE": 1, "UNKNOWN_WAREHOUSE": 4}}})

    def test_deliverable_by(self):
        """Проверяется секция deliverableBy, в которой отображается доставляемость офферов всеми типами доставки"""
        request = 'place=stat_numbers&rids=103&rgb=blue'

        """    Проверяется, что секции нет, если не указан &delivery-stats=1"""
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"result": {"offersCount": 4, "deliverableBy": Absent()}})

        """    Проверяется, что секция с нужными статистиками есть"""
        response = self.report.request_json(request + '&delivery-stats=1')
        self.assertFragmentIn(
            response, {"result": {"offersCount": 4, "deliverableBy": {"courier": 4, "pickup": 3, "post": 0}}}
        )

        response = self.report.request_json(request + '&delivery-stats=1&nobuybox=1')
        self.assertFragmentIn(
            response, {"result": {"offersCount": 7, "deliverableBy": {"courier": 7, "pickup": 4, "post": 0}}}
        )

    def test_psku(self):
        """Проверяется, что PSKU учитываются в счётчике stat_numbers"""
        request = 'place=stat_numbers&rgb=blue&rids=106'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )
        # Если убрать флаг show-partner-documents, то PSKU перестают учитываться
        response = self.report.request_json(request + '&show-partner-documents=0')
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 0,
                    'filters': {
                        'IS_CREATED_BY_PARTNER': 1,
                    },
                }
            },
        )

    def test_supplier_type_literal_1p(self):
        """Проверяется, что в place=stat_numbers учитываются поисковый литерал supplier_type: 1"""
        # Если не передавать параметр &supplier_type= в запроc, то в ответе репорта один 1p оффер
        request = 'place=stat_numbers&rgb=blue&rids=107'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )
        # Если передать в запрос параметр &supplier_type=1, то 1p-оффер есть на выдаче
        request = 'place=stat_numbers&rgb=blue&rids=107&supplier_type=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )
        # Если передать в запрос параметр &supplier_type=3, то 1p-оффер отфильтруется
        request = 'place=stat_numbers&rgb=blue&rids=107&supplier_type=3'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 0,
                }
            },
        )

    def test_supplier_type_literal_3p(self):
        """Проверяется, что в place=stat_numbers учитываются поисковый литерал supplier_type: 3"""
        # Если не передавать параметр &supplier_type= в запроc, то в ответе репорта один 3p оффер
        request = 'place=stat_numbers&rgb=blue&rids=108'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )
        # Если передать в запрос параметр &supplier_type=3, то 3p-оффер есть на выдаче
        request = 'place=stat_numbers&rgb=blue&rids=108&supplier_type=3'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )
        # Если передать в запрос параметр &supplier_type=1, то 3p-оффер отфильтруется
        request = 'place=stat_numbers&rgb=blue&rids=108&supplier_type=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 0,
                }
            },
        )

    def test_dsbs_only(self):
        """Проверяется, что при передаче параметра &dsbs_only=1 на выдаче остаются только dsbs-офферы"""
        request = 'place=stat_numbers&rgb=green&rids={}'.format(RID_WHITE8)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 2,
                }
            },
        )
        # При добавлении параметра &dsbs_only=1 не-dsbs оффер фильтруется из выдачи
        request = 'place=stat_numbers&rgb=green&rids={}&dsbs_only=1'.format(RID_WHITE8)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )

    def test_cpa_filter(self):
        """Проверяется, что в place=stat_numbers фильтруются cpa-офферы под флагом market_filter_cpa_in_stat_numbers"""
        # Без флага market_filter_cpa_in_stat_numbers и с флагом=0 на выдаче оба
        # оффера - cpa=REAL и cpa=NO (независимо от параметра &cpa=).
        # При добавлении параметра &market_filter_cpa_in_stat_numbers=1 из выдачи фильтруется оффер
        # с cpa=REAL (если в запрос передали &cpa=no) или c cpa=NO (если в запрос передали &cpa=real)
        # Если параметр &cpa= в запросе отсутствует, то офферу проставляетя DefaultCpaMask == 7 и фильтрации по cpa не происходит
        for filter_cpa_flag in (None, 0, 1):
            for cpa in (None, 'real', 'no'):
                request = 'place=stat_numbers&rgb=green&rids={}'.format(RID_WHITE8)
                if filter_cpa_flag is not None:
                    request += '&rearr-factors=market_filter_cpa_in_stat_numbers={}'.format(filter_cpa_flag)
                if cpa is not None:
                    request += '&cpa={}'.format(cpa)
                response = self.report.request_json(request)
                expected_count = 2
                if filter_cpa_flag == 1:
                    if cpa is None:
                        expected_count = 2
                    else:
                        expected_count = 1
                self.assertFragmentIn(
                    response,
                    {
                        'result': {
                            'offersCount': expected_count,
                        }
                    },
                )

    def test_3p_only(self):
        """Проверяется, что при передаче параметра &3p_only=1 на выдаче остаются только 3p-офферы.
        При этом 1p-офферы (те, у которых supplier_id=465852) должны отфильтроваться
        """
        total_msku_count = len(self.index.mskus)
        request = 'place=stat_numbers&rgb=green&fesh=101'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': total_msku_count,
                }
            },
        )
        # При добавлении параметра &3p_only=1 1p-oффер от поставщика supplier_id=465852 фильтруется из выдачи
        request = 'place=stat_numbers&rgb=green&fesh=101&3p_only=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': total_msku_count - 1,
                }
            },
        )

    def test_express_only(self):
        """Проверяется, что при передаче параметра &filter-express-delivery=1
        на выдаче остаются только экспресс офферы"""

        # Если параметр не передавать, то отдаются все ооферы
        request = 'place=stat_numbers&rgb=blue&rids={}'.format(RID_BLUE10)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 2,
                }
            },
        )

        # При добавлении параметра &filter-express-delivery=1 не-экспресс оффер фильтруется из выдачи
        request = 'place=stat_numbers&rgb=blue&rids={}&filter-express-delivery=1'.format(RID_BLUE10)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offersCount': 1,
                }
            },
        )


if __name__ == '__main__':
    main()
