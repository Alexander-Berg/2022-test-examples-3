#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GpsCoord,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    Phone,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    Tax,
    TimeInfo,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.types.delivery import OutletWorkingTime


class _Rids:
    russia = 225
    moscow = 213


class _Params:
    drugs_category_id = 15758037


class _DeliveryServices:
    class _White:
        service_1 = 99
        service_2 = 99

    class _Blue:
        service_1 = 99
        service_2 = 99


class _Hyperids:
    class _White:
        medical_1 = 1
        medical_2 = 2
        medical_0_stocks = 3
        medical_0_outlets = 4
        medical_cpc = 5
        baa = 10

    class _Blue:
        medical_1 = 6
        medical_2 = 7
        medical_0_stocks = 8
        medical_cpc = 9


class _Feshes:
    class _White:
        medical_1 = 10
        medical_2 = 11

    class _Blue:
        medical_1 = 20
        medical_2 = 21
        medical_replace = 534145
        virtual = 30


class _Feeds:
    class _White:
        medical_1 = 100
        medical_2 = 110

    class _Blue:
        medical_1 = 200
        medical_2 = 210
        medical_replace = 220
        virtual = 300


class _Warehouses:
    class _White:
        medical_1 = 1000
        medical_2 = 1100

    class _Blue:
        medical_1 = 2000
        medical_2 = 2100


class _Skus:
    class _White:
        medical_1 = 10000
        medical_2 = 11000
        medical_0_stocks = 12000
        medical_0_outlets = 13000
        medical_cpc = 14000
        unknown = 15000
        baa = 16000

    class _Blue:
        medical_1 = 20000
        medical_2 = 21000
        medical_0_stocks = 22000
        medical_0_outlets = 23000
        medical_cpc = 24000
        medical_replace = 25000
        unknown = 26000


class _Buckets:
    class _White:
        medical_1 = 100000
        medical_2 = 110000

    class _Blue:
        medical_1 = 200000
        medical_2 = 210000


class _Shops:
    class _White:
        def create(fesh, datafeed_id, warehouse_id, priority_region, regions, name, medical_booking=False):
            return Shop(
                fesh=fesh,
                datafeed_id=datafeed_id,
                warehouse_id=warehouse_id,
                priority_region=priority_region,
                regions=regions,
                name=name,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                medicine_courier=True,
                medical_booking=medical_booking,
            )

        medical_1 = create(
            fesh=_Feshes._White.medical_1,
            datafeed_id=_Feeds._White.medical_1,
            warehouse_id=_Warehouses._White.medical_1,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White medical shop 1',
            medical_booking=True,
        )

        medical_2 = create(
            fesh=_Feshes._White.medical_2,
            datafeed_id=_Feeds._White.medical_2,
            warehouse_id=_Warehouses._White.medical_2,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White medical shop 2',
        )

    class _Blue:
        def create(fesh, datafeed_id, warehouse_id, priority_region, name, medical_booking=False):
            return Shop(
                fesh=fesh,
                client_id=fesh,
                datafeed_id=datafeed_id,
                warehouse_id=warehouse_id,
                priority_region=priority_region,
                name=name,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                medicine_courier=True,
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                medical_booking=medical_booking,
            )

        medical_1 = create(
            fesh=_Feshes._Blue.medical_1,
            datafeed_id=_Feeds._Blue.medical_1,
            warehouse_id=_Warehouses._Blue.medical_1,
            priority_region=_Rids.moscow,
            name='Blue medical shop 1',
            medical_booking=True,
        )

        medical_2 = create(
            fesh=_Feshes._Blue.medical_2,
            datafeed_id=_Feeds._Blue.medical_2,
            warehouse_id=_Warehouses._Blue.medical_2,
            priority_region=_Rids.moscow,
            name='Blue medical shop 2',
        )

        medical_2 = create(
            fesh=_Feshes._Blue.medical_2,
            datafeed_id=_Feeds._Blue.medical_2,
            warehouse_id=_Warehouses._Blue.medical_2,
            priority_region=_Rids.moscow,
            name='Blue medical shop 2',
        )

        virtual = Shop(
            fesh=_Feshes._Blue.virtual,
            datafeed_id=_Feeds._Blue.virtual,
            priority_region=_Rids.moscow,
            cpa=Shop.CPA_REAL,
            tax_system=Tax.OSN,
            fulfillment_virtual=True,
            virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            name='Blue virtual shop',
        )


class _Offers:
    class _White:
        def create(
            waremd5,
            hyperid,
            sku,
            fesh,
            title,
            price,
            pickup_buckets,
            cpa=Offer.CPA_REAL,
            stock_store_count=10,
            is_medicine=False,
            is_prescription=False,
            is_baa=False,
        ):
            return Offer(
                waremd5=waremd5,
                hyperid=hyperid,
                sku=sku,
                fesh=fesh,
                title=title,
                price=price,
                pickup_buckets=pickup_buckets,
                cpa=cpa,
                stock_store_count=stock_store_count,
                is_medicine=is_medicine,
                is_prescription=is_prescription,
                is_baa=is_baa,
            )

        medical_1 = create(
            waremd5='white_cpa_1__________g',
            hyperid=_Hyperids._White.medical_1,
            sku=_Skus._White.medical_1,
            fesh=_Feshes._White.medical_1,
            title="White medical offer 1",
            price=1111,
            pickup_buckets=[_Buckets._White.medical_1],
            is_medicine=True,
            is_prescription=True,
        )

        medical_1_1 = create(
            waremd5='white_cpa_1_1________g',
            hyperid=_Hyperids._White.medical_1,
            sku=_Skus._White.medical_1,
            fesh=_Feshes._White.medical_1,
            title="White medical offer 1-1",
            price=1111,
            pickup_buckets=[_Buckets._White.medical_1],
            is_medicine=True,
            is_prescription=True,
        )

        medical_2 = create(
            waremd5='white_cpa_2__________g',
            hyperid=_Hyperids._White.medical_2,
            sku=_Skus._White.medical_2,
            fesh=_Feshes._White.medical_2,
            title="White medical offer 2",
            price=2222,
            pickup_buckets=[_Buckets._White.medical_2],
            is_medicine=True,
        )

        medical_2_1 = create(
            waremd5='white_cpa_2_1________g',
            hyperid=_Hyperids._White.medical_2,
            sku=_Skus._White.medical_2,
            fesh=_Feshes._White.medical_2,
            title="White medical offer 2-1",
            price=2222,
            pickup_buckets=[_Buckets._White.medical_2],
            is_medicine=True,
        )

        medical_0_stocks = create(
            waremd5='white_cpa_3__________g',
            hyperid=_Hyperids._White.medical_0_stocks,
            sku=_Skus._White.medical_0_stocks,
            fesh=_Feshes._White.medical_2,
            title="White medical offer 0-stocks",
            price=3333,
            stock_store_count=0,
            pickup_buckets=[_Buckets._White.medical_2],
            is_medicine=True,
        )

        medical_0_outlets = create(
            waremd5='white_cpa_4__________g',
            hyperid=_Hyperids._White.medical_0_outlets,
            sku=_Skus._White.medical_0_outlets,
            fesh=_Feshes._White.medical_2,
            title="White medical offer 0-outlets",
            price=4444,
            pickup_buckets=None,
            is_medicine=True,
        )

        medical_cpc = create(
            waremd5='white_cpc_5__________g',
            hyperid=_Hyperids._White.medical_cpc,
            sku=_Skus._White.medical_cpc,
            fesh=_Feshes._White.medical_2,
            title="White medical offer cpc",
            price=5555,
            cpa=Offer.CPA_NO,
            pickup_buckets=[_Buckets._White.medical_2],
            is_medicine=True,
        )

        baa = create(
            waremd5='baa_cpa______________g',
            hyperid=_Hyperids._White.baa,
            sku=_Skus._White.baa,
            fesh=_Feshes._White.medical_1,
            title="White baa offer cpa",
            price=1111,
            pickup_buckets=[_Buckets._White.medical_1],
            is_baa=True,
        )

    class _Blue:
        def create(
            waremd5,
            sku,
            hyperid,
            fesh,
            feedid,
            pickup_buckets,
            title,
            price,
            stock_store_count=10,
            cpa=Offer.CPA_REAL,
            is_medicine=False,
            is_prescription=False,
        ):
            return BlueOffer(
                waremd5=waremd5,
                fesh=fesh,
                supplier_id=fesh,
                feedid=feedid,
                sku=sku,
                hyperid=hyperid,
                pickup_buckets=pickup_buckets,
                title=title,
                price=price,
                stock_store_count=stock_store_count,
                cpa=cpa,
                is_medicine=is_medicine,
                is_prescription=is_prescription,
            )

        medical_1 = create(
            waremd5='blue_cpa_1___________g',
            sku=_Skus._Blue.medical_1,
            hyperid=_Hyperids._Blue.medical_1,
            fesh=_Feshes._Blue.medical_1,
            feedid=_Feeds._Blue.medical_1,
            pickup_buckets=[_Buckets._Blue.medical_1],
            title="Blue medical offer 1",
            price=1111,
            is_medicine=True,
            is_prescription=True,
        )

        medical_2 = create(
            waremd5='blue_cpa_2___________g',
            sku=_Skus._Blue.medical_2,
            hyperid=_Hyperids._Blue.medical_2,
            fesh=_Feshes._Blue.medical_2,
            feedid=_Feeds._Blue.medical_2,
            pickup_buckets=[_Buckets._Blue.medical_2],
            title="Blue medical offer 2",
            price=2222,
            is_medicine=True,
        )

        medical_0_stocks = create(
            waremd5='blue_cpa_3___________g',
            sku=_Skus._Blue.medical_0_stocks,
            hyperid=_Hyperids._Blue.medical_0_stocks,
            fesh=_Feshes._Blue.medical_2,
            feedid=_Feeds._Blue.medical_2,
            pickup_buckets=[_Buckets._Blue.medical_2],
            title="Blue medical offer 0-stocks",
            price=3333,
            stock_store_count=0,
            is_medicine=True,
        )

        medical_cpc = create(
            waremd5='blue_cpc_5___________g',
            sku=_Skus._Blue.medical_cpc,
            hyperid=_Hyperids._Blue.medical_cpc,
            fesh=_Feshes._Blue.medical_2,
            feedid=_Feeds._Blue.medical_2,
            pickup_buckets=[_Buckets._Blue.medical_2],
            title="Blue medical offer cpc",
            price=5555,
            cpa=Offer.CPA_NO,
            is_medicine=True,
        )


class _Mskus:
    class _White:
        def create(title, sku, hyperid):
            return MarketSku(title=title, sku=sku, hyperid=hyperid)

        medical_1 = create(title="White medical msku 1", sku=_Skus._White.medical_1, hyperid=_Hyperids._White.medical_1)

        medical_2 = create(title="White medical msku 2", sku=_Skus._White.medical_2, hyperid=_Hyperids._White.medical_2)

        medical_0_stocks = create(
            title="White medical msku 0-stocks",
            sku=_Skus._White.medical_0_stocks,
            hyperid=_Hyperids._White.medical_0_stocks,
        )

        medical_0_outlets = create(
            title="White medical msku 0-outlets",
            sku=_Skus._White.medical_0_outlets,
            hyperid=_Hyperids._White.medical_0_outlets,
        )

        medical_cpp = create(
            title="White medical msku cpc", sku=_Skus._White.medical_cpc, hyperid=_Hyperids._White.medical_cpc
        )

        baa = create(title="White baa msku cpa", sku=_Skus._White.baa, hyperid=_Hyperids._White.baa)

    class _Blue:
        def create(title, sku, hyperid, blue_offers):
            return MarketSku(title=title, sku=sku, hyperid=hyperid, blue_offers=blue_offers)

        medical_1 = create(
            title="Blue medical msku 1",
            sku=_Skus._Blue.medical_1,
            hyperid=_Hyperids._Blue.medical_1,
            blue_offers=[_Offers._Blue.medical_1],
        )

        medical_2 = create(
            title="Blue medical msku 2",
            sku=_Skus._Blue.medical_2,
            hyperid=_Hyperids._Blue.medical_2,
            blue_offers=[_Offers._Blue.medical_2],
        )

        medical_0_stocks = create(
            title="Blue medical msku 0-stocks",
            sku=_Skus._Blue.medical_0_stocks,
            hyperid=_Hyperids._Blue.medical_0_stocks,
            blue_offers=[_Offers._Blue.medical_0_stocks],
        )

        medical_cpc = create(
            title="Blue medical msku cpc",
            sku=_Skus._Blue.medical_cpc,
            hyperid=_Hyperids._Blue.medical_cpc,
            blue_offers=[_Offers._Blue.medical_cpc],
        )


class _Models:
    class _White:
        def create(hid, msku):
            return Model(hid=hid, hyperid=msku.hyperid)

        medical_1 = create(hid=_Params.drugs_category_id, msku=_Mskus._White.medical_1)
        medical_2 = create(hid=_Params.drugs_category_id, msku=_Mskus._White.medical_2)
        medical_0_stocks = create(hid=_Params.drugs_category_id, msku=_Mskus._White.medical_0_stocks)
        medical_0_outlets = create(hid=_Params.drugs_category_id, msku=_Mskus._White.medical_0_outlets)
        medical_cpc = create(hid=_Params.drugs_category_id, msku=_Mskus._White.medical_cpp)
        baa = create(hid=_Params.drugs_category_id, msku=_Mskus._White.baa)

    class _Blue:
        def create(hid, msku):
            return Model(hid=hid, hyperid=msku.hyperid)

        medical_1 = create(hid=_Params.drugs_category_id, msku=_Mskus._Blue.medical_1)
        medical_2 = create(hid=_Params.drugs_category_id, msku=_Mskus._Blue.medical_2)
        medical_0_stocks = create(hid=_Params.drugs_category_id, msku=_Mskus._Blue.medical_0_stocks)
        medical_cpc = create(hid=_Params.drugs_category_id, msku=_Mskus._Blue.medical_cpc)


class _Outlets:
    class _White:
        def create(fesh, point_id):
            return Outlet(
                fesh=fesh,
                point_id=point_id,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
                gps_coord=GpsCoord(37.1, 55.1),
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='09:00',
                        hours_till='21:00',
                    )
                ],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                locality_name='Moscow',
                thoroughfare_name='Karl Marx av.',
                premise_number='1',
                block='2',
                km='3',
                estate='4',
                office_number='5',
                building='6',
                phones=[Phone('+7-495-123-45-67*89')],
                storage_period=10,
            )

        medical_1_1_id = 1000000
        medical_1_2_id = 1000001
        medical_1_3_id = 1000002

        medical_2_1_id = 1100000
        medical_2_2_id = 1100001
        medical_2_3_id = 1100002

        medical_1_1 = create(fesh=_Feshes._White.medical_1, point_id=medical_1_1_id)
        medical_1_2 = create(fesh=_Feshes._White.medical_1, point_id=medical_1_2_id)
        medical_1_3 = create(fesh=_Feshes._White.medical_1, point_id=medical_1_3_id)

        medical_2_1 = create(fesh=_Feshes._White.medical_2, point_id=medical_2_1_id)
        medical_2_2 = create(fesh=_Feshes._White.medical_2, point_id=medical_2_2_id)
        medical_2_3 = create(fesh=_Feshes._White.medical_2, point_id=medical_2_3_id)

    class _Blue:
        def create(fesh, point_id):
            return Outlet(
                fesh=fesh,
                point_id=point_id,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
                gps_coord=GpsCoord(37.1, 55.1),
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_times=[
                    OutletWorkingTime(
                        days_from=OutletWorkingTime.MONDAY,
                        days_till=OutletWorkingTime.SUNDAY,
                        hours_from='09:00',
                        hours_till='21:00',
                    )
                ],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                locality_name='Moscow',
                thoroughfare_name='Karl Marx av.',
                premise_number='1',
                block='2',
                km='3',
                estate='4',
                office_number='5',
                building='6',
                phones=[Phone('+7-495-123-45-67*89')],
                storage_period=10,
            )

        medical_1_1_id = 2000000
        medical_1_2_id = 2000001
        medical_1_3_id = 2000002

        medical_2_1_id = 2100000
        medical_2_2_id = 2100001
        medical_2_3_id = 2100002

        medical_1_1 = create(fesh=_Feshes._Blue.medical_1, point_id=medical_1_1_id)
        medical_1_2 = create(fesh=_Feshes._Blue.medical_1, point_id=medical_1_2_id)
        medical_1_3 = create(fesh=_Feshes._Blue.medical_1, point_id=medical_1_3_id)

        medical_2_1 = create(fesh=_Feshes._Blue.medical_2, point_id=medical_2_1_id)
        medical_2_2 = create(fesh=_Feshes._Blue.medical_2, point_id=medical_2_2_id)
        medical_2_3 = create(fesh=_Feshes._Blue.medical_2, point_id=medical_2_3_id)


class _Pickup:
    class _White:
        def create(fesh, bucket_id, options, carriers):
            return PickupBucket(
                fesh=fesh,
                bucket_id=bucket_id,
                options=options,
                carriers=carriers,
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )

        medical_1 = create(
            fesh=_Feshes._White.medical_1,
            bucket_id=_Buckets._White.medical_1,
            options=[
                PickupOption(outlet_id=_Outlets._White.medical_1_1_id, day_from=0, day_to=0, price=50),
                PickupOption(outlet_id=_Outlets._White.medical_1_2_id, day_from=1, day_to=1, price=50),
                PickupOption(outlet_id=_Outlets._White.medical_1_3_id, day_from=2, day_to=2, price=50),
            ],
            carriers=[_DeliveryServices._White.service_1],
        )

        medical_2 = create(
            fesh=_Feshes._White.medical_2,
            bucket_id=_Buckets._White.medical_2,
            options=[
                PickupOption(outlet_id=_Outlets._White.medical_2_1_id, day_from=0, day_to=0, price=50),
                PickupOption(outlet_id=_Outlets._White.medical_2_2_id, day_from=1, day_to=1, price=50),
                PickupOption(outlet_id=_Outlets._White.medical_2_3_id, day_from=2, day_to=2, price=50),
            ],
            carriers=[_DeliveryServices._White.service_2],
        )

    class _Blue:
        def create(fesh, bucket_id, options, carriers):
            return PickupBucket(
                fesh=fesh,
                bucket_id=bucket_id,
                options=options,
                carriers=carriers,
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )

        medical_1 = create(
            fesh=_Feshes._White.medical_1,
            bucket_id=_Buckets._Blue.medical_1,
            options=[
                PickupOption(outlet_id=_Outlets._Blue.medical_1_1_id, day_from=0, day_to=0, price=50),
                PickupOption(outlet_id=_Outlets._Blue.medical_1_2_id, day_from=1, day_to=1, price=50),
                PickupOption(outlet_id=_Outlets._Blue.medical_1_3_id, day_from=2, day_to=2, price=50),
            ],
            carriers=[_DeliveryServices._Blue.service_1],
        )

        medical_2 = create(
            fesh=_Feshes._White.medical_2,
            bucket_id=_Buckets._Blue.medical_2,
            options=[
                PickupOption(outlet_id=_Outlets._Blue.medical_2_1_id, day_from=0, day_to=0, price=50),
                PickupOption(outlet_id=_Outlets._Blue.medical_2_2_id, day_from=1, day_to=1, price=50),
                PickupOption(outlet_id=_Outlets._Blue.medical_2_3_id, day_from=2, day_to=2, price=50),
            ],
            carriers=[_DeliveryServices._Blue.service_2],
        )


class _Requests:
    geo_shopping_list = (
        'place=geo'
        '&rids=213'
        '&mskus-list={msku}:{count}'
        '&show-outlet=offers'
        '&zoom=10'
        '&max-outlets=100'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=enable_prescription_drugs_delivery=1'
        '&debug=1'
    )

    geo_shopping_list_multiple = (
        'place=geo'
        '&rids=213'
        '&mskus-list={msku1}:{count1},{msku2}:{count2}'
        '&show-outlet=offers'
        '&zoom=10'
        '&max-outlets=100'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=enable_prescription_drugs_delivery=1'
        '&debug=1'
    )


class T(TestCase):
    """
    Набор тестов для проекта "Покупка списком на карте".
    Краткое описание:
    - Для запрашиваемой корзины товаров вернуть (пока акцент на медицинские товары):
      а) все точки продаж где есть в наличии запрашиваемые товары (далее сценарий с самовывозом)
      б) самый дешевый магазин где есть в наличии запрашиваемые товары и доставка (далее сценарий с доставкой)
    - На вход передается список покупок (msku и количество).
    - На выходе группировка магазин->точка продажи->оффер.
    См.: https://st.yandex-team.ru/MARKETPROJECT-5706
    """

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow, name="Moscow", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [_Shops._White.medical_1, _Shops._White.medical_2]

        cls.index.shops += [
            _Shops._Blue.medical_1,
            _Shops._Blue.medical_2,
            _Shops._Blue.virtual,
        ]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            _Mskus._White.medical_1,
            _Mskus._White.medical_2,
            _Mskus._White.medical_0_stocks,
            _Mskus._White.medical_0_outlets,
            _Mskus._White.medical_cpp,
            _Mskus._White.baa,
        ]

        cls.index.mskus += [
            _Mskus._Blue.medical_1,
            _Mskus._Blue.medical_2,
            _Mskus._Blue.medical_0_stocks,
            _Mskus._Blue.medical_cpc,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers._White.medical_1,
            _Offers._White.medical_1_1,
            _Offers._White.medical_2,
            _Offers._White.medical_2_1,
            _Offers._White.medical_0_stocks,
            _Offers._White.medical_0_outlets,
            _Offers._White.medical_cpc,
            _Offers._White.baa,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            _Models._White.medical_1,
            _Models._White.medical_2,
            _Models._White.medical_0_stocks,
            _Models._White.medical_0_outlets,
            _Models._White.medical_cpc,
            _Models._White.baa,
        ]

        cls.index.models += [
            _Models._Blue.medical_1,
            _Models._Blue.medical_2,
            _Models._Blue.medical_0_stocks,
            _Models._Blue.medical_cpc,
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            _Outlets._White.medical_1_1,
            _Outlets._White.medical_1_2,
            _Outlets._White.medical_1_3,
            _Outlets._White.medical_2_1,
            _Outlets._White.medical_2_2,
            _Outlets._White.medical_2_3,
        ]

        cls.index.outlets += [
            _Outlets._Blue.medical_1_1,
            _Outlets._Blue.medical_1_2,
            _Outlets._Blue.medical_1_3,
            _Outlets._Blue.medical_2_1,
            _Outlets._Blue.medical_2_2,
            _Outlets._Blue.medical_2_3,
        ]

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += [_Pickup._White.medical_1, _Pickup._White.medical_2]

        cls.index.pickup_buckets += [_Pickup._Blue.medical_1, _Pickup._Blue.medical_2]

    @classmethod
    def prepare_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[_Rids.russia, _Rids.moscow],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=_Warehouses._White.medical_1, priority=1),
                    WarehouseWithPriority(warehouse_id=_Warehouses._White.medical_2, priority=1),
                    WarehouseWithPriority(warehouse_id=_Warehouses._Blue.medical_1, priority=1),
                    WarehouseWithPriority(warehouse_id=_Warehouses._Blue.medical_2, priority=1),
                ],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Warehouses._White.medical_1, _DeliveryServices._White.service_1, 'white_pickup_delivery_service_1'),
            (_Warehouses._White.medical_2, _DeliveryServices._White.service_2, 'white_pickup_delivery_service_2'),
            (_Warehouses._Blue.medical_1, _DeliveryServices._Blue.service_1, 'blue_pickup_delivery_service_1'),
            (_Warehouses._Blue.medical_2, _DeliveryServices._Blue.service_2, 'blue_pickup_delivery_service_2'),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Rids.moscow),
                DynamicWarehouseToWarehouseInfo(warehouse_from=wh_id, warehouse_to=wh_id),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[
                        DateSwitchTimeAndRegionInfo(
                            date_switch_hour=2,
                            region_to=_Rids.russia,
                            date_switch_time=TimeInfo(19, 0),
                            packaging_time=TimeInfo(3, 30),
                        )
                    ],
                ),
            ]

    # White tests

    def test_white_cpc_exclude(self):
        """
        Проверям что на выдачу не попадают белые СPC оффера.
        """

        request = _Requests.geo_shopping_list.format(msku=_Mskus._White.medical_cpp.sku, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "shopOutlets": 0,
                    "total": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 0,
                    "offers": {},
                    "shops": [],
                }
            },
            allow_different_len=False,
        )

    def test_white_cpa_zero_outlets_exclude(self):
        """
        Проверям что на выдачу не попадают белые оффера без точек продаж.
        """

        request = _Requests.geo_shopping_list.format(msku=_Mskus._White.medical_0_outlets.sku, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "cpaCount": 0,
                    "offers": {"meta": []},
                    "shopOutlets": 0,
                    "shops": [],
                    "total": 0,
                    "totalFreeOffers": 0,
                    "totalModels": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 1,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 1,
                }
            },
            allow_different_len=False,
        )

    def test_white_cpa_zero_stock_replace(self):
        """
        Проверям что на выдаче у белых офферов не происходит игнорирование при
        наличии на складе 0 (подразумеваем что если оффер доступен то хотя 1
        штука есть в наличии).
        """

        request = _Requests.geo_shopping_list.format(msku=_Mskus._White.medical_0_stocks.sku, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "cpaCount": 1,
                    "offers": {"meta": []},
                    "shopOutlets": 3,
                    "shops": [{}],
                    "total": 1,
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 1,
                    "totalPassedAllGlFilters": 1,
                    "totalShopsBeforeFilters": 1,
                }
            },
            allow_different_len=False,
        )

    def test_white_cpa_search_by_multiple_msku(self):
        '''
        Проверям что происходит поиск по нескольким msku для белых офферов.
        '''

        request = _Requests.geo_shopping_list_multiple.format(
            msku2=_Offers._White.medical_2.sku, count2=2, msku1=_Offers._White.medical_1.sku, count1=2
        )

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "total": 4,
                    "totalOffers": 4,
                    "totalOffersBeforeFilters": 4,
                    "totalPassedAllGlFilters": 4,
                    "totalShopsBeforeFilters": 2,
                }
            },
            allow_different_len=False,
        )

    def test_white_cpa_search_by_single_msku(self):
        '''
        Проверям что происходит поиск по msku для белых офферов.
        '''

        request = _Requests.geo_shopping_list.format(msku=_Offers._White.medical_1.sku, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "total": 2,
                    "totalOffers": 2,
                    "totalOffersBeforeFilters": 2,
                    "totalPassedAllGlFilters": 2,
                    "totalShopsBeforeFilters": 1,
                }
            },
            allow_different_len=False,
        )

    def test_white_cpa_search_by_unknown_msku(self):
        '''
        Проверям что на выдачу не попадают белые оффера по несуществующему msku.
        '''

        request = _Requests.geo_shopping_list.format(msku=_Skus._White.unknown, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "shopOutlets": 0,
                    "total": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 0,
                    "offers": {},
                    "shops": [],
                }
            },
            allow_different_len=False,
        )

    def test_white_booking_option_available(self):
        """
        Проверяем наличие признака hasBooking у белых медицинских поставщиков с возможностью
        бронирования из наличия.
        """
        request = _Requests.geo_shopping_list_multiple.format(
            msku1=_Skus._White.medical_1, count1=1, msku2=_Skus._White.medical_2, count2=1
        )

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "shops": [
                        {
                            "entity": "shop",
                            "id": _Shops._White.medical_1.fesh,
                            "name": _Shops._White.medical_1.name,
                            "hasBooking": True,
                        },
                        {
                            "entity": "shop",
                            "id": _Shops._White.medical_2.fesh,
                            "name": _Shops._White.medical_2.name,
                            "hasBooking": Absent(),
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    # Blue tests

    def test_blue_cpc_offer_exclude(self):
        """
        Проверям что на выдачу не попадают синие СPC оффера.
        """

        request = _Requests.geo_shopping_list.format(msku=_Mskus._Blue.medical_cpc.sku, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "shopOutlets": 0,
                    "total": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 0,
                    "offers": {},
                    "shops": [],
                }
            },
            allow_different_len=False,
        )

    def test_blue_cpa_zero_stock_replace(self):
        """
        Проверям что на выдаче у синих офферов не происходит игнорирование при
        наличии на складе 0 (подразумеваем что условно бесконечный запас).
        """

        count = 2
        request = _Requests.geo_shopping_list.format(msku=_Mskus._Blue.medical_0_stocks.sku, count=count)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "cpaCount": 1,
                "offers": {"meta": []},
                "shopOutlets": 3,
                "shops": [
                    {
                        "entity": "shop",
                        "id": _Feshes._Blue.medical_2,
                        "name": _Shops._Blue.medical_2.name,
                        "offers": [
                            {
                                "count": count,
                                "entity": "offer",
                                "marketSku": str(_Offers._Blue.medical_0_stocks.sku),
                                "price": {"currency": "RUR", "value": str(_Offers._Blue.medical_0_stocks.price)},
                                "stockStoreCount": count,
                                "wareId": _Offers._Blue.medical_0_stocks.waremd5,
                            }
                        ],
                        "offersTotalPrice": {
                            "currency": "RUR",
                            "value": str(count * _Offers._Blue.medical_0_stocks.price),
                        },
                    }
                ],
                "total": 1,
                "totalOffers": 1,
                "totalOffersBeforeFilters": 1,
                "totalPassedAllGlFilters": 1,
                "totalShopsBeforeFilters": 1,
            },
            allow_different_len=False,
        )

    def test_blue_cpa_search_by_multiple_msku(self):
        '''
        Проверям что происходит поиск по нескольким msku для синих офферов.
        '''
        count = 2
        request = _Requests.geo_shopping_list_multiple.format(
            msku2=_Offers._Blue.medical_2.sku, count2=count, msku1=_Offers._Blue.medical_1.sku, count1=count
        )

        request = (
            'place=geo'
            '&rids=213'
            '&mskus-list={msku2}:2,{msku1}:2'
            '&show-outlet=offers'
            '&zoom=10'
            '&max-outlets=100'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
            '&debug=1'
        ).format(msku2=_Offers._Blue.medical_2.sku, msku1=_Offers._Blue.medical_1.sku)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "cpaCount": 2,
                    "offers": {"meta": []},
                    "shopOutlets": 6,
                    "shops": [
                        {
                            "entity": "shop",
                            "id": _Feshes._Blue.medical_2,
                            "name": _Shops._Blue.medical_2.name,
                            "offers": [
                                {
                                    "count": count,
                                    "entity": "offer",
                                    "marketSku": str(_Offers._Blue.medical_2.sku),
                                    "price": {"currency": "RUR", "value": str(_Offers._Blue.medical_2.price)},
                                    "stockStoreCount": count,
                                    "wareId": _Offers._Blue.medical_2.waremd5,
                                }
                            ],
                            "offersTotalPrice": {
                                "currency": "RUR",
                                "value": str(count * _Offers._Blue.medical_2.price),
                            },
                        },
                        {
                            "entity": "shop",
                            "id": _Feshes._Blue.medical_1,
                            "name": _Shops._Blue.medical_1.name,
                            "offers": [
                                {
                                    "count": count,
                                    "entity": "offer",
                                    "marketSku": str(_Offers._Blue.medical_1.sku),
                                    "price": {"currency": "RUR", "value": str(_Offers._Blue.medical_1.price)},
                                    "stockStoreCount": count,
                                    "wareId": _Offers._Blue.medical_1.waremd5,
                                }
                            ],
                            "offersTotalPrice": {
                                "currency": "RUR",
                                "value": str(count * _Offers._Blue.medical_1.price),
                            },
                        },
                    ],
                    "total": 2,
                    "totalOffers": 2,
                    "totalOffersBeforeFilters": 2,
                    "totalPassedAllGlFilters": 2,
                    "totalShopsBeforeFilters": 1,
                }
            },
            allow_different_len=False,
        )

    def test_blue_cpa_search_by_single_msku(self):
        '''
        Проверям что происходит поиск по msku для синих офферов.
        '''

        request = _Requests.geo_shopping_list.format(msku=_Offers._Blue.medical_1.sku, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "total": 1,
                    "totalOffers": 1,
                    "totalOffersBeforeFilters": 1,
                    "totalPassedAllGlFilters": 1,
                    "totalShopsBeforeFilters": 1,
                }
            },
            allow_different_len=False,
        )

    def test_blue_cpa_search_by_unknown_msku(self):
        '''
        Проверям что на выдачу не попадают синие оффера по несуществующему msku.
        '''

        request = _Requests.geo_shopping_list.format(msku=_Skus._Blue.unknown, count=2)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "shopOutlets": 0,
                    "total": 0,
                    "totalOffers": 0,
                    "totalOffersBeforeFilters": 0,
                    "totalPassedAllGlFilters": 0,
                    "totalShopsBeforeFilters": 0,
                    "offers": {},
                    "shops": [],
                }
            },
            allow_different_len=False,
        )

    def test_blue_booking_option_available(self):
        """
        Проверяем наличие признака hasBooking у синих медицинских поставщиков с возможностью
        бронирования из наличия.
        """
        request = _Requests.geo_shopping_list_multiple.format(
            msku1=_Skus._Blue.medical_1, count1=1, msku2=_Skus._Blue.medical_2, count2=1
        )

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "shops": [
                        {
                            "entity": "shop",
                            "id": _Shops._Blue.medical_1.fesh,
                            "name": _Shops._Blue.medical_1.name,
                            "hasBooking": True,
                        },
                        {
                            "entity": "shop",
                            "id": _Shops._Blue.medical_2.fesh,
                            "name": _Shops._Blue.medical_2.name,
                            "hasBooking": Absent(),
                        },
                    ],
                }
            },
            allow_different_len=False,
        )

    # Other tests

    def test_white_and_blue_cpa_search_by_msku(self):
        '''
        Проверям что происходит подбор и белых и синих офферов.
        '''

        request = (
            'place=geo'
            '&rids=213'
            '&mskus-list={msku1}:{count1},{msku2}:{count2},{msku3}:{count3},{msku4}:{count4}'
            '&show-outlet=offers'
            '&zoom=10'
            '&max-outlets=100'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
            '&debug=1'
        ).format(
            msku1=_Offers._Blue.medical_1.sku,
            count1=2,
            msku2=_Offers._Blue.medical_2.sku,
            count2=2,
            msku3=_Offers._White.medical_2.sku,
            count3=2,
            msku4=_Offers._White.medical_1.sku,
            count4=2,
        )

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "total": 6,
                    "totalOffers": 6,
                    "totalOffersBeforeFilters": 6,
                    "totalPassedAllGlFilters": 6,
                    "totalShopsBeforeFilters": 3,
                }
            },
            allow_different_len=False,
        )

    def test_white_and_blue_cpa_render_without_outlets(self):
        '''
        Проверям что точки продаж не приходят, а сам ответ
        имеет упрощенную структуру.
        '''
        count = 2
        request = (
            'place=geo'
            '&rids=213'
            '&mskus-list={msku1}:{count1},{msku2}:{count2},{msku3}:{count3}'
            '&show-outlet=offers'
            '&zoom=10'
            '&max-outlets=100'
            '&rearr-factors=market_not_prescription_drugs_delivery=1'
            '&rearr-factors=enable_prescription_drugs_delivery=1'
            '&debug=1'
        ).format(
            msku1=_Offers._Blue.medical_1.sku,
            count1=count,
            msku2=_Offers._White.medical_2.sku,
            count2=count,
            msku3=_Offers._White.medical_1.sku,
            count3=count,
        )
        white_shop_1_offers = [_Offers._White.medical_1, _Offers._White.medical_1_1]
        white_shop_2_offers = [_Offers._White.medical_2, _Offers._White.medical_2_1]
        blue_shop_offers = [_Offers._Blue.medical_1]

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "search": {
                    "total": 5,
                    "totalOffers": 5,
                    "totalOffersBeforeFilters": 5,
                    "totalPassedAllGlFilters": 5,
                    "totalShopsBeforeFilters": 3,
                }
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(result, {"offers": {"meta": []}})
        self.assertFragmentIn(
            result,
            {
                "shops": [
                    {
                        "entity": "shop",
                        "id": _Feshes._White.medical_1,
                        "name": _Shops._White.medical_1.name,
                        "offers": [
                            {
                                "entity": "offer",
                                "marketSku": str(offer.sku),
                                "wareId": offer.waremd5,
                                "stockStoreCount": count,
                                "count": count,
                                "price": {"currency": "RUR", "value": str(offer.price)},
                            }
                            for offer in white_shop_1_offers
                        ],
                        "offersTotalPrice": {
                            "currency": "RUR",
                            "value": str(count * sum(offer.price for offer in white_shop_1_offers)),
                        },
                    }
                ]
                + [
                    {
                        "entity": "shop",
                        "id": _Feshes._White.medical_2,
                        "name": _Shops._White.medical_2.name,
                        "offers": [
                            {
                                "entity": "offer",
                                "marketSku": str(offer.sku),
                                "wareId": offer.waremd5,
                                "stockStoreCount": count,
                                "count": count,
                                "price": {"value": str(offer.price)},
                            }
                            for offer in white_shop_2_offers
                        ],
                        "offersTotalPrice": {"value": str(count * sum(offer.price for offer in white_shop_2_offers))},
                    }
                ]
                + [
                    {
                        "entity": "shop",
                        "id": _Feshes._Blue.medical_1,
                        "name": _Shops._Blue.medical_1.name,
                        "offers": [
                            {
                                "entity": "offer",
                                "marketSku": str(offer.sku),
                                "wareId": offer.waremd5,
                                "stockStoreCount": count,
                                "count": count,
                                "price": {"value": str(offer.price)},
                            }
                            for offer in blue_shop_offers
                        ],
                        "offersTotalPrice": {"value": str(count * sum(offer.price for offer in blue_shop_offers))},
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_baa_cpa_offer_exclude(self):
        """
        Проверям что на выдачу не попадают невходящие в разрешенную группу
        медицинские препараты для проекта покупка списком (это БАДы).
        """

        request = _Requests.geo_shopping_list.format(msku=_Offers._White.baa.sku, count=1)

        result = self.report.request_json(request)
        self.assertFragmentIn(
            result,
            {
                "logicTrace": [
                    r"\[ME\].*? '{wareid}' is medical category: 1, is shopping-list medicine: 0, stocks: 10, shop: 10".format(
                        wareid=_Offers._White.baa.waremd5
                    )
                ]
            },
            use_regex=True,
        )


if __name__ == '__main__':
    main()
