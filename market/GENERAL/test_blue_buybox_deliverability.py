#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import copy
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    Model,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main

RID_RUSSIA = 225

RID_BLUE1 = 101
RID_BLUE2 = 102

WH_1 = 11

CARRIER_B1 = 257
CARRIER_B2 = 258

BUCKET_1 = 801
BUCKET_2 = 802
BUCKET_3 = 803


blue_offer_1 = BlueOffer(
    price=999,
    vat=Vat.NO_VAT,
    feedid=2,
    offerid='blue_offer_1',
    waremd5='Sku1--------iLVm1Goleg',
    delivery_buckets=[BUCKET_1],
)
blue_offer_2 = BlueOffer(
    price=100,
    vat=Vat.NO_VAT,
    feedid=3,
    offerid='blue_offer_2',
    waremd5='Sku2--------iLVm1Goleg',
    delivery_buckets=[BUCKET_2, BUCKET_3],
)


def get_warehouse_and_delivery_service(warehouse_id, service_id, enabled=True):
    date_switch_hours = [
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=RID_BLUE1),
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
    @classmethod
    def prepare(cls):

        # Общие настройки
        cls.index.regiontree += [
            Region(rid=RID_BLUE1, region_type=Region.CITY),
            Region(rid=RID_BLUE2, region_type=Region.CITY),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.settings.lms_autogenerate = False
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

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
                fulfillment_virtual=color is Shop.VIRTUAL_SHOP_BLUE,
                supplier_type=None if color is Shop.VIRTUAL_SHOP_BLUE else Shop.FIRST_PARTY,
            )
            for shop_id, feed_id, color, wh_id in [
                (101, 1, Shop.VIRTUAL_SHOP_BLUE, None),
                (102, 2, None, WH_1),
                (102, 3, None, WH_1),
            ]
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WH_1, home_region=RID_RUSSIA, holidays_days_set_key=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WH_1, warehouse_to=WH_1),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE1, warehouses=[WH_1]),
            DynamicWarehousesPriorityInRegion(region=RID_BLUE2, warehouses=[WH_1]),
            get_warehouse_and_delivery_service(WH_1, CARRIER_B1),
            # get_warehouse_and_delivery_service(WH_1, CARRIER_B2),                   связь отключена, чтобы оффер-2 не доставлялся в регион-1, но индекс (литералы по регионам) про это ничего не знал
            DynamicDeliveryServiceInfo(CARRIER_B1, "B_" + str(CARRIER_B1)),
            DynamicDeliveryServiceInfo(CARRIER_B2, "B_" + str(CARRIER_B2)),
        ]

        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

        cls.index.models += [
            Model(hyperid=1, hid=1, title='model 1'),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=bucket_id,
                carriers=[carrier_id],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=region_id, options=[DeliveryOption(price=15, day_from=1, day_to=2)])
                    for region_id in region_ids
                ],
            )
            for bucket_id, carrier_id, region_ids in [
                (BUCKET_1, CARRIER_B1, [RID_BLUE1, RID_BLUE2]),
                (BUCKET_2, CARRIER_B2, [RID_BLUE1, RID_BLUE2]),
                (BUCKET_3, CARRIER_B1, [RID_BLUE2]),
            ]
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=1,
                blue_offers=[blue_offer_1, blue_offer_2],
            ),
        ]

    def query_blue(self, rid, extra=None):
        return self.report.request_json('place=prime&hid=1&rgb=blue&rids=' + rid + (extra or ''))

    def check_blue(self, rid, wareId, extra=None):
        res1 = {"results": [{"entity": "product", "offers": {"count": 1, "items": [{"wareId": wareId}]}}]}
        res2 = {"search": {"total": 0}}
        self.assertFragmentIn(self.query_blue(str(rid), extra), res2 if wareId is None else res1)

    def test_blue(self):
        # учёт доставки до вычисления buybox, ящик должен выиграть более дорогой оффер, т.к. дешевый отлетает по доставке до проверки цены
        self.check_blue(RID_BLUE1, blue_offer_1.waremd5)

        # проверяем что в регион-2 ящик всегда выигрывает оффер-2, не зависимо от пред-проверки доставки
        self.check_blue(RID_BLUE2, blue_offer_2.waremd5)


if __name__ == '__main__':
    main()
