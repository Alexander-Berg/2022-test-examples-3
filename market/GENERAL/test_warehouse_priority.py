#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GpsCoord,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        priority_region=213,
        name='Beru!',
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        delivery_service_outlets=[2001],
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
    )

    dropship_supplier_id = 4

    def make_supplier(supplier_id, feed_id, name, warehouse, type=Shop.THIRD_PARTY, is_fulfillment=True):
        return Shop(
            fesh=supplier_id,
            datafeed_id=feed_id,
            priority_region=2,
            name=name,
            tax_system=Tax.OSN,
            supplier_type=type,
            blue=Shop.BLUE_REAL,
            cpa=Shop.CPA_REAL if is_fulfillment else Shop.CPA_NO,
            warehouse_id=warehouse,
            fulfillment_program=is_fulfillment,
        )

    blue_supplier_145 = make_supplier(2, 21, 'shop1_priority_2_145', 145, type=Shop.FIRST_PARTY)
    blue_supplier_146 = make_supplier(2, 22, 'shop1_priority_2_146', 146, type=Shop.FIRST_PARTY)
    blue_supplier_147 = make_supplier(2, 23, 'shop1_priority_1_147', 147, type=Shop.FIRST_PARTY)
    blue_supplier2_147 = make_supplier(3, 3, 'shop2_priority_1', 147)
    blue_supplier3_146 = make_supplier(5, 5, 'shop3_priority_1', 146)
    dropship_444 = make_supplier(dropship_supplier_id, 4, 'dropship at warehouse 444', 444, is_fulfillment=False)


class _Offers(object):
    def make_offer(price, shop_sku, feed_id, ware_md5):
        return BlueOffer(price=price, vat=Vat.VAT_10, feedid=feed_id, offerid=shop_sku, waremd5=ware_md5)

    alyonka_s1_145 = make_offer(49, 'Alyonka_145', 21, 'Alyonka_s1_145_______g')
    alyonka_s1_147 = make_offer(49, 'Alyonka_147', 23, 'Alyonka_s1_147_______g')

    dove_s1_145_expensive = make_offer(80, 'Dove_145', 22, 'Dove_s1_145__________g')
    dove_s1_146_cheap = make_offer(75, 'Dove_146', 21, 'Dove_s1_146__________g')

    def make_dropship_offer(price, shop_sku, feed_id, ware_md5):
        return BlueOffer(
            price=price,
            vat=Vat.VAT_20,
            feedid=feed_id,
            offerid=shop_sku,
            waremd5=ware_md5,
            is_fulfillment=False,
            weight=75,
            dimensions=OfferDimensions(length=80, width=60, height=220),
        )

    fridge_147_offer = make_offer(35000, 'Fridge_147', 3, 'Refrigerator_147_____g')
    fridge_444_offer = make_dropship_offer(35000, 'Fridge_444', 4, 'Refrigerator_444_____g')

    rotfront_s1_145 = make_offer(97, 'Rotfront_s1_145', 21, 'Rotfront_s1_145______g')
    rotfront_s3_146 = make_offer(97, 'Rotfront_s3_146', 5, 'Rotfront_s3_146______g')


class _MSKUs(object):
    alyonka = MarketSku(
        title="Alyonka",
        hyperid=1,
        sku=1,
        blue_offers=[_Offers.alyonka_s1_145, _Offers.alyonka_s1_147],
        delivery_buckets=[801, 802],
    )
    dove = MarketSku(
        title="Dove",
        hyperid=2,
        sku=2,
        blue_offers=[_Offers.dove_s1_146_cheap, _Offers.dove_s1_145_expensive],
        delivery_buckets=[801, 802],
    )

    fridge = MarketSku(
        title='Fridge',
        hyperid=3,
        sku=3,
        blue_offers=[_Offers.fridge_147_offer, _Offers.fridge_444_offer],
        delivery_buckets=[803],
    )

    rotfront = MarketSku(
        title="Rotfront",
        hyperid=4,
        sku=4,
        blue_offers=[_Offers.rotfront_s1_145, _Offers.rotfront_s3_146],
        delivery_buckets=[801, 802],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213),
            Region(rid=111, region_type=Region.COUNTRY),  # no delivery to this region
            Region(rid=444, region_type=Region.COUNTRY),  # delivery only from warehouse 444
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        def delivery_service_region_to_region_info(region_from=213, region_to=225):
            return DeliveryServiceRegionToRegionInfo(region_from=region_from, region_to=region_to, days_key=1)

        def link_warehouse_delivery_service(warehouse_id, delivery_service_id, region_to=225):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=delivery_service_id,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=region_to)],
            )

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=146, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=147, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=444, home_region=213),
            DynamicDeliveryServiceInfo(103, "c_103", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(
                111, "courier", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(
                165,
                "dropship_delivery",
                region_to_region_info=[
                    delivery_service_region_to_region_info(213, 225),
                    delivery_service_region_to_region_info(213, 444),
                ],
            ),
            DynamicDaysSet(key=1, days=[]),
            link_warehouse_delivery_service(warehouse_id=145, delivery_service_id=103),
            link_warehouse_delivery_service(warehouse_id=146, delivery_service_id=103),
            link_warehouse_delivery_service(warehouse_id=147, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=444, delivery_service_id=165, region_to=444),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[444, 145, 146, 147]),
            DynamicWarehousesPriorityInRegion(region=444, warehouses=[444]),
        ]

        # WarehouseWithPriority 145 и 146  склады имеют одинаковый приоретет (100):
        # 146 как дефолтный приоритет, а 145 - как выставленный
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[444, 213, 225],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 100),
                    WarehouseWithPriority(147, 1),
                    WarehouseWithPriority(444, 1),
                ],
            )
        ]
        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.blue_supplier_145,
            _Shops.blue_supplier_146,
            _Shops.blue_supplier_147,
            _Shops.blue_supplier2_147,
            _Shops.blue_supplier3_146,
            _Shops.dropship_444,
        ]

        cls.index.mskus += [_MSKUs.alyonka, _MSKUs.dove, _MSKUs.fridge, _MSKUs.rotfront]

        std_options = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=801, fesh=1, carriers=[103], regional_options=std_options),
            DeliveryBucket(
                bucket_id=802,
                fesh=1,
                carriers=[111],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=803,
                fesh=4,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
        ]

    def test_get_offer_from_warehouse_with_highiest_priority(self):
        """
        Что проверяем: При одинаковой цене выбираем оффер с более приоритетного склада
        """

        response = self.report.request_json('place=sku_offers&market-sku=1')
        self.assertFragmentIn(
            response, {'supplier': {'id': 2}, 'prices': {'value': '49'}, 'supplierSku': 'Alyonka_147'}
        )

    def test_get_cheaper_offer(self):
        """
        Что проверяем: со складов с одинаковым приоритетом нужно выбрать более дешевый оффер
        """
        response = self.report.request_json('place=sku_offers&market-sku=2')
        self.assertFragmentIn(response, {'supplier': {'id': 2}, 'prices': {'value': '75'}, 'supplierSku': 'Dove_146'})

    def test_get_dropship_offer(self):
        """
        Что проверяем: при равных приоритетов склада и дропшипа, при одинаковой цене выбираем Кроссдок
        """
        response = self.report.request_json('place=sku_offers&market-sku=3')
        self.assertFragmentIn(
            response, {'supplier': {'id': 4}, 'prices': {'value': '35000'}, 'supplierSku': 'Fridge_444'}
        )

    def test_hide_dropship_offer(self):
        """
        Что проверяем: при передачи нулевого коэффициента приоритизации дропшипов, он должен быть скрыт
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=3&rearr-factors=market_dropship_priority_coefficient=0;market_blue_buybox_disable_old_buybox_algo=0'
        )
        self.assertFragmentIn(
            response, {'supplier': {'id': 3}, 'prices': {'value': '35000'}, 'supplierSku': 'Fridge_147'}
        )

    def test_get_3p_offer(self):
        """
        Что проверяем: при равном приоритете складов, при одинаковой цене, выбрать нужно 3р оффер
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=4&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0'
        )
        self.assertFragmentIn(
            response, {'supplier': {'id': 5}, 'prices': {'value': '97'}, 'supplierSku': 'Rotfront_s3_146'}
        )

    def test_hide_3p_offer(self):
        """
        Что проверяем: при передачи нулевого коэффициента приоритизации 3p, должны получить 1p оффер
        """
        response = self.report.request_json(
            'place=sku_offers&market-sku=4&rearr-factors=market_thirdp_priority_coefficient=0'
        )
        self.assertFragmentIn(
            response, {'supplier': {'id': 2}, 'prices': {'value': '97'}, 'supplierSku': 'Rotfront_s1_145'}
        )


if __name__ == '__main__':
    main()
