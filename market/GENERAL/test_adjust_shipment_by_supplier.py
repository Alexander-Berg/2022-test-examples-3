#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicCapacityDaysOff,
    DynamicCapacityInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HandlingRegionToRegionInfo,
    MarketSku,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
    Vat,
)
from core.types.offer import OfferDimensions


def make_supplier(supplier_id, feed_id, name, warehouse_id, supplier_type, is_fulfillment, direct_shipping):
    return Shop(
        fesh=supplier_id,
        datafeed_id=feed_id,
        priority_region=213,
        name=name,
        tax_system=Tax.OSN,
        supplier_type=supplier_type,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL if is_fulfillment else Shop.CPA_NO,
        warehouse_id=warehouse_id,
        fulfillment_program=is_fulfillment,
        client_id=supplier_id,
        direct_shipping=direct_shipping,
    )


class _Shops(object):
    dropship_444 = make_supplier(
        supplier_id=111,
        feed_id=111,
        name='dropship_sort_center',
        warehouse_id=444,
        supplier_type=Shop.THIRD_PARTY,
        is_fulfillment=False,
        direct_shipping=False,
    )

    supplier_3p = make_supplier(
        supplier_id=112,
        feed_id=112,
        name='supplier_3p',
        warehouse_id=333,
        supplier_type=Shop.THIRD_PARTY,
        is_fulfillment=True,
        direct_shipping=True,
    )


def make_dropship_offer(price, shop_sku, supplier_id, feed_id, ware_md5):
    return BlueOffer(
        price=price,
        vat=Vat.VAT_10,
        feedid=feed_id,
        offerid=shop_sku,
        waremd5=ware_md5,
        is_fulfillment=False,
        supplier_id=supplier_id,
        weight=25,
        dimensions=OfferDimensions(length=20, width=30, height=35),
        stock_store_count=10,
    )


class _Offers(object):
    fridge_444_offer = make_dropship_offer(
        price=3500, shop_sku='Fridge_444', supplier_id=111, feed_id=111, ware_md5='Refrigerator_444_____g'
    )


class _MSKUs(object):
    fridge_msku = MarketSku(
        title='Refrigerator_msku', hyperid=101, sku=101, blue_offers=[_Offers.fridge_444_offer], delivery_buckets=[803]
    )


wh2wh_dropship_sortcenter = DynamicWarehouseToWarehouseInfo(
    warehouse_from=444,
    warehouse_to=333,
    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=8, region_to=225)],
    inbound_time=TimeInfo(1),
    operation_time=0,
)


wh2ds_relation_wh333_ds179 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=333,
    delivery_service_id=179,
    operation_time=0,
    date_switch_time_infos=[
        DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=213, packaging_time=TimeInfo(1))
    ],
    capacity_by_region=[
        DynamicCapacityInfo(
            region_to=213,
            capacity_days_off=[DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_COURIER, days_key=1)],
        )
    ],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.index.regiontree += [
            Region(rid=213),
        ]

        cls.index.mskus += [
            _MSKUs.fridge_msku,
        ]

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            # dropship warehouse
            DynamicWarehouseInfo(
                id=444,
                home_region=213,
                holidays_days_set_key=2,
                handling_info=[HandlingRegionToRegionInfo(region_from=225, region_to=225, handling_time=TimeInfo(0))],
            ),
            # sorting center
            DynamicWarehouseInfo(id=333, home_region=213, holidays_days_set_key=2),
        ]

        cls.dynamic.lms += [
            wh2wh_dropship_sortcenter,
        ]

        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[0, 1, 2, 3, 4, 5]),
            DynamicDaysSet(key=2, days=[]),
        ]

        cls.dynamic.lms += [
            DynamicTimeIntervalsSet(key=1, intervals=[TimeIntervalInfo(TimeInfo(23, 0), TimeInfo(23, 45))]),
        ]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(
                id=179,
                name='ds179',
                rating=1,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=2)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=2)]
                    )
                ],
            )
        ]

        cls.dynamic.lms += [
            wh2ds_relation_wh333_ds179,
        ]

        cls.dynamic.lms += [
            DynamicWarehousesPriorityInRegion(
                region=225,
                warehouses=[
                    333,
                    444,
                ],
            )
        ]

        cls.index.shops += [
            _Shops.dropship_444,
            _Shops.supplier_3p,
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=803,
                fesh=111,
                carriers=[179],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)]),
                ],
            )
        ]

    def test_supplier_shipment_day(self):
        '''
        Проверяем, что при отсутствии флага market_blue_adjust_shipment_by_supplier и флаге==1 дата отгрузки со
        склада поставщика (shipmentBySupplier) сдвигается вправо ближе к дате отгрузки с FF-склада (shipmentDay).
        Если флаг==0, то shipmentBySupplier равен своему обычному значению, без дополнительного сдвига вправо.
        '''
        request = 'place=actual_delivery&rgb=blue&rids=213&offers-list=Refrigerator_444_____g:1'
        request += '&preferable-courier-delivery-service=179&preferable-courier-delivery-day=8&combinator=0'
        request += '&rearr-factors=market_blue_shipment_by_supplier=1'
        for rearr_flag in (None, 0, 1):
            if rearr_flag is not None:
                request += '&rearr-factors=market_blue_adjust_shipment_by_supplier={}'.format(rearr_flag)
            response = self.report.request_json(request)
            supplier_shipment = '1985-06-{}T08:00:00+00:00'.format(24 if rearr_flag == 0 else 29)

            # Set shipment_date_time and reception_time according to inbound time of one hour from lms warehouse-to-warehouse relation
            reception_time = supplier_shipment[:12] + '9' + supplier_shipment[13:]

            fragment = {
                'search': {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': {
                                'options': [
                                    {
                                        'dayFrom': 7,
                                        'dayTo': 8,
                                        'isDefault': True,
                                        'serviceId': '179',
                                        'shipmentDay': 6,
                                        'supplierProcessing': [
                                            {
                                                'warehouseId': 444,
                                                'startDateTime': supplier_shipment,
                                                'shipmentDateTime': reception_time,
                                                'shipmentBySupplier': supplier_shipment,
                                                'receptionByWarehouse': reception_time,
                                            }
                                        ],
                                        'shipmentBySupplier': supplier_shipment,
                                        'receptionByWarehouse': reception_time,
                                    }
                                ]
                            },
                        }
                    ]
                }
            }
            self.assertFragmentIn(response, fragment, allow_different_len=False)


if __name__ == '__main__':
    main()
