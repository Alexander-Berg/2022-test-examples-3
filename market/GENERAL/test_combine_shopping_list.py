#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, Contains
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    ExpressSupplier,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PrescriptionManagementSystem,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalByDay,
    TimeIntervalInfo,
)
from core.types.combinator import CombinatorGpsCoords, CombinatorExpressWarehouse
from core.types.offer import OfferDimensions
from core.testcase import TestCase, main


class _Rids:
    spb = 2
    russia = 225
    moscow = 213


class _DeliveryServices:
    internal = 99


class _Params:
    drugs_category_id = 15758037


class _Categories:
    medical_white_cpa = 1
    medical_white_cpa_1 = 2
    medical_white_cpa_2 = 3
    prescription_white_cpa = 4
    medical_white_cpc = 5
    medical_white_cpa_no_delivery = 6
    medical_blue_cpa = 7
    common_white_cpa = 8
    medical_type_a_cpa = 9
    medical_type_b_cpa = 10
    medical_type_c_cpa = 11
    medical_no_stock_cpa = 12
    medical_share = 13
    medical_blue_cpa_no_delivery = 14
    baa_white_cpa = 15
    medical_blue_express = 16


class _Feshes:
    class _White:
        medical_1 = 10
        medical_2 = 20
        medical_3 = 30
        medical_4 = 40
        medical_5 = 50
        medical_express = 60
        medical_share = 70

    class _Blue:
        virtual = 80
        medical_share = 90
        medical = 100
        medical_express = 110


class _Feeds:
    class _White:
        medical_1 = 100
        medical_2 = 200
        medical_3 = 300
        medical_4 = 400
        medical_5 = 500
        medical_express = 600
        medical_share_1 = 700
        medical_share_2 = 800

    class _Blue:
        virtual = 900
        medical_1 = 1000
        medical_2 = 1100
        medical_3 = 1200
        medical_express_1 = 1300
        medical_express_2 = 1400
        medical_express_3 = 1500


class _ClientIds:
    class _White:
        medical_1 = 101
        medical_2 = 201
        medical_3 = 301
        medical_4 = 401
        medical_5 = 501
        medical_express = 601
        medical_share_1 = 701
        medical_share_2 = 801

    class _Blue:
        medical_1 = 901
        medical_2 = 1001
        medical_3 = 1101
        medical_express_1 = 1201
        medical_express_2 = 1301
        medical_express_3 = 1401


class _Warehouses:
    class _White:
        medical_1 = 1000
        medical_2 = 2000
        medical_3 = 3000
        medical_4 = 4000
        medical_5 = 5000
        medical_express = 6000

    class _Blue:
        medical_1 = 7000
        medical_2 = 8000
        medical_3 = 9000
        medical_express_1 = 10000
        medical_express_2 = 11000
        medical_express_3 = 12000


class _Outlets:
    class _White:
        medical_id = 5000

        def create(fesh, point_id):
            return Outlet(
                fesh=fesh,
                point_id=point_id,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
            )

        medical = create(fesh=_Feshes._White.medical_1, point_id=medical_id)

    class _Blue:
        medical_id = 6000

        def create(fesh, point_id):
            return Outlet(
                fesh=fesh,
                point_id=point_id,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
            )

        medical = create(fesh=_Feshes._Blue.medical_share, point_id=medical_id)


class _Shops:
    class _White:
        def create(
            fesh,
            datafeed_id,
            client_id,
            warehouse_id,
            priority_region,
            regions,
            name,
            prescription_system=PrescriptionManagementSystem.PS_NONE,
            medical_booking=None,
        ):
            return Shop(
                business_fesh=fesh + 1,
                fesh=fesh,
                datafeed_id=datafeed_id,
                client_id=client_id,
                warehouse_id=warehouse_id,
                priority_region=priority_region,
                regions=regions,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                medicine_courier=True,
                name=name,
                prescription_management_system=prescription_system,
                medical_booking=medical_booking,
            )

        medical_1 = create(
            fesh=_Feshes._White.medical_1,
            datafeed_id=_Feeds._White.medical_1,
            client_id=_ClientIds._White.medical_1,
            warehouse_id=_Warehouses._White.medical_1,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop 1',
            prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
            medical_booking=True,
        )

        medical_2 = create(
            fesh=_Feshes._White.medical_2,
            datafeed_id=_Feeds._White.medical_2,
            client_id=_ClientIds._White.medical_2,
            warehouse_id=_Warehouses._White.medical_2,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop 2',
            prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
        )

        medical_3 = create(
            fesh=_Feshes._White.medical_3,
            datafeed_id=_Feeds._White.medical_3,
            client_id=_ClientIds._White.medical_3,
            warehouse_id=_Warehouses._White.medical_3,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop 3',
            prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
        )

        medical_4 = create(
            fesh=_Feshes._White.medical_4,
            datafeed_id=_Feeds._White.medical_4,
            client_id=_ClientIds._White.medical_4,
            warehouse_id=_Warehouses._White.medical_4,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop 4',
        )

        medical_5 = create(
            fesh=_Feshes._White.medical_5,
            datafeed_id=_Feeds._White.medical_5,
            client_id=_ClientIds._White.medical_5,
            warehouse_id=_Warehouses._White.medical_5,
            priority_region=_Rids.spb,
            regions=[_Rids.spb],
            name='White CPA CIS medical shop 5',
            prescription_system=PrescriptionManagementSystem.PS_MEDICATA,
        )

        medical_express = create(
            fesh=_Feshes._White.medical_express,
            datafeed_id=_Feeds._White.medical_express,
            client_id=_ClientIds._White.medical_express,
            warehouse_id=_Warehouses._White.medical_express,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop express',
        )

        medical_share_1 = create(
            fesh=_Feshes._White.medical_share,
            datafeed_id=_Feeds._White.medical_share_1,
            client_id=_ClientIds._White.medical_share_1,
            warehouse_id=_Warehouses._White.medical_1,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop share 1 (multi warehouse)',
        )

        medical_share_2 = create(
            fesh=_Feshes._White.medical_share,
            datafeed_id=_Feeds._White.medical_share_2,
            client_id=_ClientIds._White.medical_share_2,
            warehouse_id=_Warehouses._White.medical_2,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical shop share 2 (multi warehouse)',
        )

    class _Blue:
        def create(fesh, datafeed_id, client_id, warehouse_id, priority_region, name, medical_booking=None):
            return Shop(
                business_fesh=fesh + 1,
                fesh=fesh,
                datafeed_id=datafeed_id,
                client_id=client_id,
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
            fesh=_Feshes._Blue.medical_share,
            datafeed_id=_Feeds._Blue.medical_1,
            client_id=_ClientIds._Blue.medical_1,
            warehouse_id=_Warehouses._Blue.medical_1,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical shop 1 (multi warehouses)',
        )

        medical_2 = create(
            fesh=_Feshes._Blue.medical_share,
            datafeed_id=_Feeds._Blue.medical_2,
            client_id=_ClientIds._Blue.medical_2,
            warehouse_id=_Warehouses._Blue.medical_2,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical shop 2 (multi warehouses)',
        )

        medical_pickup = create(
            fesh=_Feshes._Blue.medical,
            datafeed_id=_Feeds._Blue.medical_3,
            client_id=_ClientIds._Blue.medical_3,
            warehouse_id=_Warehouses._Blue.medical_3,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical shop 3 (pickup)',
            medical_booking=True,
        )

        virtual = Shop(
            business_fesh=_Feshes._Blue.virtual + 1,
            fesh=_Feshes._Blue.virtual,
            datafeed_id=_Feeds._Blue.virtual,
            priority_region=_Rids.moscow,
            tax_system=Tax.OSN,
            fulfillment_virtual=True,
            cpa=Shop.CPA_REAL,
            virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            name='Virtual blue shop',
        )

        medical_express_1 = create(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express_1,
            client_id=_ClientIds._Blue.medical_express_1,
            warehouse_id=_Warehouses._Blue.medical_express_1,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical express shop 1 (multi warehouses)',
        )

        medical_express_2 = create(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express_2,
            client_id=_ClientIds._Blue.medical_express_2,
            warehouse_id=_Warehouses._Blue.medical_express_2,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical express shop 2 (multi warehouses)',
        )

        medical_express_3 = create(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express_3,
            client_id=_ClientIds._Blue.medical_express_3,
            warehouse_id=_Warehouses._Blue.medical_express_3,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical express shop 3 (multi warehouses)',
        )


class _Buckets:
    medical_id_1 = 10000
    medical_id_2 = 20000
    medical_id_3 = 30000
    medical_id_4 = 40000
    medical_id_5 = 50000
    medical_id_blue_1 = 60000
    medical_id_blue_2 = 70000
    medical_id_pickup_white = 80000
    medical_id_pickup_blue = 90000
    medical_id_express = 100000
    medical_id_blue_express_1 = 110000
    medical_id_blue_express_2 = 120000
    medical_id_blue_express_3 = 130000

    def create_delivery(bucket_id, shop, carriers, regional_options=None):
        return DeliveryBucket(
            bucket_id=bucket_id,
            dc_bucket_id=bucket_id,
            fesh=shop.fesh,
            carriers=carriers,
            regional_options=regional_options,
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        )

    def create_pickup(bucket_id, outlet_id, shop, carriers):
        return PickupBucket(
            bucket_id=bucket_id,
            dc_bucket_id=bucket_id,
            fesh=shop.fesh,
            carriers=carriers,
            options=[
                PickupOption(
                    outlet_id=outlet_id,
                    price=100,
                    day_from=1,
                    day_to=3,
                )
            ],
            delivery_program=DeliveryBucket.REGULAR_PROGRAM,
        )

    medical_1 = create_delivery(medical_id_1, _Shops._White.medical_1, [_DeliveryServices.internal])
    medical_2 = create_delivery(medical_id_2, _Shops._White.medical_2, [_DeliveryServices.internal])
    medical_3 = create_delivery(medical_id_3, _Shops._White.medical_3, [_DeliveryServices.internal])
    medical_4 = create_delivery(medical_id_4, _Shops._White.medical_4, [_DeliveryServices.internal])
    medical_5 = create_delivery(medical_id_5, _Shops._White.medical_5, [_DeliveryServices.internal])
    medical_express = create_delivery(medical_id_express, _Shops._White.medical_express, [_DeliveryServices.internal])

    medical_pickup_white = create_pickup(
        medical_id_pickup_white, _Outlets._White.medical_id, _Shops._White.medical_1, [_DeliveryServices.internal]
    )

    medical_pickup_blue = create_pickup(
        medical_id_pickup_blue, _Outlets._Blue.medical_id, _Shops._Blue.medical_pickup, [_DeliveryServices.internal]
    )

    medical_blue_1 = create_delivery(
        bucket_id=medical_id_blue_1,
        shop=_Shops._Blue.medical_1,
        carriers=[_DeliveryServices.internal],
        regional_options=[
            RegionalDelivery(
                rid=_Rids.moscow,
                options=[
                    DeliveryOption(price=5, day_from=1, day_to=4, shop_delivery_price=10),
                ],
            )
        ],
    )

    medical_blue_2 = create_delivery(
        bucket_id=medical_id_blue_2,
        shop=_Shops._Blue.medical_2,
        carriers=[_DeliveryServices.internal],
        regional_options=[
            RegionalDelivery(
                rid=_Rids.moscow,
                options=[
                    DeliveryOption(price=5, day_from=1, day_to=4, shop_delivery_price=10),
                ],
            )
        ],
    )

    medical_blue_express_1 = create_delivery(
        bucket_id=medical_id_blue_express_1,
        shop=_Shops._Blue.medical_express_1,
        carriers=[_DeliveryServices.internal],
        regional_options=[
            RegionalDelivery(
                rid=_Rids.moscow,
                options=[
                    DeliveryOption(price=5, day_from=0, day_to=0, shop_delivery_price=10),
                ],
            )
        ],
    )

    medical_blue_express_2 = create_delivery(
        bucket_id=medical_id_blue_express_2,
        shop=_Shops._Blue.medical_express_2,
        carriers=[_DeliveryServices.internal],
        regional_options=[
            RegionalDelivery(
                rid=_Rids.moscow,
                options=[
                    DeliveryOption(price=5, day_from=0, day_to=0, shop_delivery_price=10),
                ],
            )
        ],
    )

    medical_blue_express_3 = create_delivery(
        bucket_id=medical_id_blue_express_3,
        shop=_Shops._Blue.medical_express_3,
        carriers=[_DeliveryServices.internal],
        regional_options=[
            RegionalDelivery(
                rid=_Rids.moscow,
                options=[
                    DeliveryOption(price=5, day_from=0, day_to=0, shop_delivery_price=10),
                ],
            )
        ],
    )


class _Mskus:
    medical_cpa_id = 100000
    medical_cpa_1_id = 200000
    medical_cpa_2_id = 300000
    prescription_cpa_id = 400000
    medical_cpc_id = 500000
    medical_cpa_no_delivery_id = 600000
    medical_cpa_blue_id = 700000
    common_cpa_id = 800000
    medical_type_a_id = 900000
    medical_type_b_id = 1000000
    medical_type_c_id = 1100000
    medical_no_stock_id = 1200000
    medical_share_id = 1300000
    medical_cpa_blue_no_delivery_id = 1400000
    unknown_id = 1500000
    baa_cpa_id = 1600000
    medical_express_id = 1700000

    def create(title, sku, hyperid, blue_offers=None):
        return MarketSku(title=title, sku=sku, hyperid=hyperid, blue_offers=blue_offers)

    medical_cpa = create("Medical MSKU (cpa)", medical_cpa_id, _Categories.medical_white_cpa)

    medical_cpa_1 = create("Medical MSKU (cpa + 1)", medical_cpa_1_id, _Categories.medical_white_cpa_1)

    medical_cpa_2 = create("Medical MSKU (cpa + 2)", medical_cpa_2_id, _Categories.medical_white_cpa_2)

    prescription_cpa = create(
        "Prescription medical MSKU (cpa)", prescription_cpa_id, _Categories.prescription_white_cpa
    )

    medical_cpc = create("Medical MSKU (cpc)", medical_cpc_id, _Categories.medical_white_cpc)

    medical_cpa_no_delivery = create(
        "Medical MSKU (cpa + no delivery)", medical_cpa_no_delivery_id, _Categories.medical_white_cpa_no_delivery
    )

    blue_offer_1 = BlueOffer(
        waremd5='medical1_b_cpa_shop1_g',
        fesh=_Shops._Blue.medical_1.fesh,
        feedid=_Shops._Blue.medical_1.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_1,
        sku=medical_cpa_blue_id,
        hyperid=_Categories.medical_blue_cpa,
        delivery_buckets=[_Buckets.medical_blue_1.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
        stock_store_count=10,
        cpa=Offer.CPA_REAL,
        is_medicine=True,
    )

    blue_offer_2 = BlueOffer(
        waremd5='medical2_b_cpa_shop1_g',
        fesh=_Shops._Blue.medical_2.fesh,
        feedid=_Shops._Blue.medical_2.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_2,
        sku=medical_cpa_blue_id,
        hyperid=_Categories.medical_blue_cpa,
        delivery_buckets=[_Buckets.medical_blue_2.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
        stock_store_count=10,
        cpa=Offer.CPA_REAL,
        is_medicine=True,
    )

    blue_offer_pickup_1 = BlueOffer(
        waremd5='medical_p1____shop_p_g',
        fesh=_Shops._Blue.medical_pickup.fesh,
        feedid=_Shops._Blue.medical_pickup.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_3,
        sku=medical_cpa_blue_no_delivery_id,
        hyperid=_Categories.medical_blue_cpa_no_delivery,
        pickup_buckets=[_Buckets.medical_pickup_blue.bucket_id],
        price=10,
        stock_store_count=10,
        cpa=Offer.CPA_REAL,
        is_medicine=True,
        is_medical_booking=True,
    )

    blue_offer_pickup_2 = BlueOffer(
        waremd5='medical_p2____shop_p_g',
        fesh=_Shops._Blue.medical_pickup.fesh,
        feedid=_Shops._Blue.medical_pickup.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_3,
        sku=medical_cpa_blue_no_delivery_id,
        hyperid=_Categories.medical_blue_cpa_no_delivery,
        pickup_buckets=[_Buckets.medical_pickup_blue.bucket_id],
        price=10,
        stock_store_count=10,
        cpa=Offer.CPA_REAL,
        is_medicine=True,
    )

    blue_express_offer_1 = BlueOffer(
        waremd5='medical_b_expr_shop1_g',
        fesh=_Shops._Blue.medical_express_1.fesh,
        feedid=_Shops._Blue.medical_express_1.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_express_1,
        sku=medical_express_id,
        hyperid=_Categories.medical_blue_express,
        delivery_buckets=[_Buckets.medical_blue_express_1.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
        stock_store_count=10,
        is_medicine=True,
        is_express=True,
    )

    blue_express_offer_2 = BlueOffer(
        waremd5='medical_b_expr_shop2_g',
        fesh=_Shops._Blue.medical_express_2.fesh,
        feedid=_Shops._Blue.medical_express_2.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_express_2,
        sku=medical_express_id,
        hyperid=_Categories.medical_blue_express,
        delivery_buckets=[_Buckets.medical_blue_express_2.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
        stock_store_count=10,
        is_medicine=True,
        is_express=True,
    )

    blue_express_offer_3 = BlueOffer(
        waremd5='medical_b_expr_shop3_g',
        fesh=_Shops._Blue.medical_express_3.fesh,
        feedid=_Shops._Blue.medical_express_3.datafeed_id,
        supplier_id=_ClientIds._Blue.medical_express_3,
        sku=medical_express_id,
        hyperid=_Categories.medical_blue_express,
        delivery_buckets=[_Buckets.medical_blue_express_3.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
        stock_store_count=10,
        is_medicine=True,
        is_express=True,
    )

    medical_cpa_blue_no_delivery = create(
        title="Medical MSKU (cpa blue + no delivery)",
        sku=medical_cpa_blue_no_delivery_id,
        hyperid=_Categories.medical_blue_cpa_no_delivery,
        blue_offers=[blue_offer_pickup_1, blue_offer_pickup_2],
    )

    medical_cpa_blue = create(
        "Medical MSKU (cpa blue)", medical_cpa_blue_id, _Categories.medical_blue_cpa, [blue_offer_1, blue_offer_2]
    )

    medical_express_blue = create(
        "Medical MSKU (express blue)",
        medical_express_id,
        _Categories.medical_blue_express,
        [blue_express_offer_1, blue_express_offer_2, blue_express_offer_3],
    )

    common_cpa = create("Common MSKU (cpa)", common_cpa_id, _Categories.common_white_cpa)

    baa_cpa = create("Baa MSKU (cpa)", baa_cpa_id, _Categories.baa_white_cpa)

    medical_no_stock = create("Medical MSKU (no stock)", medical_no_stock_id, _Categories.medical_no_stock_cpa)

    medical_share = create("Medical MSKU (share)", medical_share_id, _Categories.medical_share)

    medical_type_a = create("Medical MSKU (type a)", medical_type_a_id, _Categories.medical_type_a_cpa)

    medical_type_b = create("Medical MSKU (type b)", medical_type_b_id, _Categories.medical_type_b_cpa)

    medical_type_c = create("Medical MSKU (type c)", medical_type_c_id, _Categories.medical_type_c_cpa)


class _Models:
    def create(hid, msku):
        return Model(hid=hid, hyperid=msku.hyperid)

    medical_cpa = create(_Params.drugs_category_id, _Mskus.medical_cpa)
    medical_cpa_1 = create(_Params.drugs_category_id, _Mskus.medical_cpa_1)
    medical_cpa_2 = create(_Params.drugs_category_id, _Mskus.medical_cpa_2)
    prescription_cpa = create(_Params.drugs_category_id, _Mskus.prescription_cpa)
    medical_cpc = create(_Params.drugs_category_id, _Mskus.medical_cpc)
    medical_cpa_no_delivery = create(_Params.drugs_category_id, _Mskus.medical_cpa_no_delivery)
    medical_cpa_blue = create(_Params.drugs_category_id, _Mskus.medical_cpa_blue)
    medical_cpa_blue_no_delivery = create(_Params.drugs_category_id, _Mskus.medical_cpa_blue_no_delivery)
    medical_no_stock = create(_Params.drugs_category_id, _Mskus.medical_no_stock)
    medical_share = create(_Params.drugs_category_id, _Mskus.medical_share)
    medical_type_a = create(_Params.drugs_category_id, _Mskus.medical_type_a)
    medical_type_b = create(_Params.drugs_category_id, _Mskus.medical_type_b)
    medical_type_c = create(_Params.drugs_category_id, _Mskus.medical_type_c)
    baa_cpa = create(_Params.drugs_category_id, _Mskus.baa_cpa)
    medical_express_blue = create(_Params.drugs_category_id, _Mskus.medical_express_blue)


class _Offers:
    class _White:
        def create(
            waremd5,
            shop,
            supplier_id,
            msku,
            price,
            weight,
            dimensions,
            delivery_options,
            cpa,
            delivery_buckets=None,
            pickup_buckets=None,
            is_express=False,
            stock_store_count=10,
            is_medicine=True,
            is_prescription=False,
            is_baa=False,
            is_medical_booking=False,
        ):
            return Offer(
                waremd5=waremd5,
                fesh=shop.fesh,
                feedid=shop.datafeed_id,
                supplier_id=supplier_id,
                sku=msku.sku,
                hyperid=msku.hyperid,
                price=price,
                weight=weight,
                dimensions=dimensions,
                delivery_options=delivery_options,
                stock_store_count=stock_store_count,
                cpa=cpa,
                delivery_buckets=delivery_buckets,
                pickup_buckets=pickup_buckets,
                is_express=is_express,
                is_medicine=is_medicine,
                is_prescription=is_prescription,
                is_baa=is_baa,
                is_medical_booking=is_medical_booking,
            )

        medical_cpa_1_shop_1 = create(
            waremd5='medical1_w_cpa_shop1_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            is_medicine=True,
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
        )

        medical_cpa_1_shop_2 = create(
            waremd5='medical1_w_cpa_shop2_g',
            shop=_Shops._White.medical_2,
            supplier_id=_ClientIds._White.medical_2,
            msku=_Mskus.medical_cpa,
            delivery_buckets=[_Buckets.medical_2.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            is_medicine=True,
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
        )

        medical_cpa_1_shop_5 = create(
            waremd5='medical1_w_cpa_shop5_g',
            shop=_Shops._White.medical_5,
            supplier_id=_ClientIds._White.medical_5,
            msku=_Mskus.medical_cpa,
            delivery_buckets=[_Buckets.medical_5.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            is_medicine=True,
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
        )

        prescription_cpa_1_shop_1 = create(
            waremd5='prescription1_shop1__g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.prescription_cpa,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_prescription=True,
        )

        prescription_cpa_1_shop_2 = create(
            waremd5='prescription1_shop2__g',
            shop=_Shops._White.medical_2,
            supplier_id=_ClientIds._White.medical_2,
            msku=_Mskus.prescription_cpa,
            delivery_buckets=[_Buckets.medical_2.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_prescription=True,
        )

        medical_cpc_1_shop_1 = create(
            waremd5='medical1_w_cpc_shop1_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpc,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_NO,
            is_medicine=True,
        )

        medical_cpa_no_delivery_1_shop_1 = create(
            waremd5='medical1_no_del_shop1g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_medical_booking=True,
        )

        medical_cpa_no_delivery_2_shop_1 = create(
            waremd5='medical2_no_del_shop1g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_cpa_no_delivery,
            pickup_buckets=[_Buckets.medical_pickup_white.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_cpa_1_shop_3 = create(
            waremd5='medical1_w_cpa_shop3_g',
            shop=_Shops._White.medical_3,
            supplier_id=_ClientIds._White.medical_3,
            msku=_Mskus.medical_cpa,
            delivery_buckets=[_Buckets.medical_3.bucket_id],
            price=100,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_cpa_2_shop_3 = create(
            waremd5='medical2_w_cpa_shop3_g',
            shop=_Shops._White.medical_3,
            supplier_id=_ClientIds._White.medical_3,
            msku=_Mskus.medical_cpa_1,
            delivery_buckets=[_Buckets.medical_3.bucket_id],
            price=100,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_cpa_2_shop_4 = create(
            waremd5='medical2_w_cpa_shop4_g',
            shop=_Shops._White.medical_4,
            supplier_id=_ClientIds._White.medical_4,
            msku=_Mskus.medical_cpa_2,
            delivery_buckets=[_Buckets.medical_4.bucket_id],
            price=100,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        prescription_cpa_1_shop_3 = create(
            waremd5='prescription1_shop3__g',
            shop=_Shops._White.medical_3,
            supplier_id=_ClientIds._White.medical_3,
            msku=_Mskus.prescription_cpa,
            delivery_buckets=[_Buckets.medical_3.bucket_id],
            price=100,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_prescription=True,
        )

        prescription_cpa_1_shop_4 = create(
            waremd5='prescription1_shop4__g',
            shop=_Shops._White.medical_4,
            supplier_id=_ClientIds._White.medical_4,
            msku=_Mskus.prescription_cpa,
            delivery_buckets=[_Buckets.medical_4.bucket_id],
            price=100,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
            is_prescription=True,
        )

        common_cpa_1_shop_1 = create(
            waremd5='common1_w_cpa_shop1__g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.common_cpa,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=False,
        )

        baa_cpa_shop_1 = create(
            waremd5='baa_w_cpa_shop1______g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.baa_cpa,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=False,
            is_baa=True,
        )

        medical_no_stock_shop_1 = create(
            waremd5='medical1_0stock_shop1g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_no_stock,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            stock_store_count=0,
            is_medicine=True,
        )

        medical_share_1 = create(
            waremd5='medical_share_1______g',
            shop=_Shops._White.medical_share_1,
            supplier_id=_ClientIds._White.medical_share_1,
            msku=_Mskus.medical_share,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_share_2 = create(
            waremd5='medical_share_2______g',
            shop=_Shops._White.medical_share_2,
            supplier_id=_ClientIds._White.medical_2,
            msku=_Mskus.medical_share,
            delivery_buckets=[_Buckets.medical_2.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=2, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_express_shop_msku_a = create(
            waremd5='medical_exp_msku_a___g',
            shop=_Shops._White.medical_express,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.medical_type_a,
            delivery_buckets=[_Buckets.medical_express.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_express=True,
            is_medicine=True,
        )

        medical_express_shop_msku_b = create(
            waremd5='medical_exp_msku_b___g',
            shop=_Shops._White.medical_express,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.medical_type_b,
            delivery_buckets=[_Buckets.medical_express.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_express=True,
            is_medicine=True,
        )

        medical_express_shop_msku_c = create(
            waremd5='medical_exp_msku_c___g',
            shop=_Shops._White.medical_express,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.medical_type_c,
            delivery_buckets=[_Buckets.medical_express.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=1, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_express=True,
            is_medicine=True,
        )

        medical_shop_1_msku_a = create(
            waremd5='medical_shop1_msku_a_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_type_a,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_shop_1_msku_b = create(
            waremd5='medical_shop1_msku_b_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_type_b,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )

        medical_shop_1_msku_c = create(
            waremd5='medical_shop1_msku_c_g',
            shop=_Shops._White.medical_1,
            supplier_id=_ClientIds._White.medical_1,
            msku=_Mskus.medical_type_c,
            delivery_buckets=[_Buckets.medical_1.bucket_id],
            price=10,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=1, day_to=1, order_before=14)],
            cpa=Offer.CPA_REAL,
            is_medicine=True,
        )


class _GpsWarehouses:
    """
    Подготовленная информация о приоритетах складов для последующего заполнения express-warehouses
    в запросе.
    Чем меньше значение поля priority, тем более приоритетным является склад.
    """

    blue_express_1_best = [
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_1,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=1,
        ),
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_2,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=2,
        ),
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_3,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=3,
        ),
    ]

    blue_express_2_best = [
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_1,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=2,
        ),
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_2,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=1,
        ),
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_3,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=3,
        ),
    ]

    blue_express_3_best = [
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_1,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=3,
        ),
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_2,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=2,
        ),
        CombinatorExpressWarehouse(
            warehouse_id=_Warehouses._Blue.medical_express_3,
            zone_id=1,
            business_id=_Feshes._Blue.medical_express + 1,
            priority=1,
        ),
    ]

    class _Gps:
        def __init__(self, lat, lon):
            self._lat = lat
            self._lon = lon

            self.location_combinator = CombinatorGpsCoords(self._lat, self._lon)
            self.location_str = 'lat:{lat};lon:{lon}'.format(lat=self._lat, lon=self._lon)

    gps_1 = _Gps(11.0, 11.0)
    gps_2 = _Gps(12.0, 12.0)
    gps_3 = _Gps(13.0, 13.0)


class _WorkingShedules:
    def create_intervals_set(key, from_time, to_time):
        return DynamicTimeIntervalsSet(key=key, intervals=[TimeIntervalInfo(from_time, to_time)])

    # круглосуточный режим работы
    round_the_clock = [TimeIntervalByDay(day=i, time_from='0:00', time_to='23:59') for i in range(0, 7)]
    round_the_clock_intervals = [create_intervals_set(i, TimeInfo(0, 0), TimeInfo(23, 59)) for i in range(1, 8)]

    # будние дни с 8:00 до 22:00
    workdays = [TimeIntervalByDay(day=i, time_from='8:00', time_to='22:00') for i in range(0, 5)]
    workdays_intervals = [create_intervals_set(i, TimeInfo(8, 0), TimeInfo(22, 0)) for i in range(1, 6)]

    # каждый день с 10:00 до 18:00
    everyday = [TimeIntervalByDay(day=i, time_from='10:00', time_to='18:00') for i in range(0, 7)]
    everyday_intervals = [create_intervals_set(i, TimeInfo(10, 0), TimeInfo(18, 0)) for i in range(1, 8)]


class _ExpressSuppliers:
    medical_express = ExpressSupplier(
        feed_id=_Shops._White.medical_express.datafeed_id,
        supplier_id=_Shops._White.medical_express.fesh,
        warehouse_id=_Shops._White.medical_express.warehouse_id,
    )

    # Следующие поставщики нужны для указания расписания работы складов, чтобы в дальнейшем
    # использовать данное расписание при вычислении приоритета

    # Склад работает круглосуточно
    medical_express_1 = ExpressSupplier(
        feed_id=_Feeds._Blue.medical_express_1,
        supplier_id=_Feshes._Blue.medical_express,
        warehouse_id=_Warehouses._Blue.medical_express_1,
        working_schedule=_WorkingShedules.round_the_clock,
    )
    # Склад работает с 8:00 до 22:00 по будним дням
    medical_express_2 = ExpressSupplier(
        feed_id=_Feeds._Blue.medical_express_2,
        supplier_id=_Feshes._Blue.medical_express,
        warehouse_id=_Warehouses._Blue.medical_express_2,
        working_schedule=_WorkingShedules.workdays,
    )
    # Склад работает с 10:00 до 18:00 ежедневно
    medical_express_3 = ExpressSupplier(
        feed_id=_Feeds._Blue.medical_express_3,
        supplier_id=_Feshes._Blue.medical_express,
        warehouse_id=_Warehouses._Blue.medical_express_3,
        working_schedule=_WorkingShedules.everyday,
    )


class _DynamicWarehouseDelivery:
    def create(warehouse_id, min_days=1, max_days=1):
        return DynamicWarehouseDelivery(
            warehouse_id,
            {
                _Rids.moscow: [
                    DynamicDeliveryRestriction(
                        max_phys_weight=40000,
                        max_dim_sum=250,
                        max_dimensions=[100, 100, 100],
                        min_days=min_days,
                        max_days=max_days,
                    )
                ]
            },
        )

    medical_1 = create(_Warehouses._Blue.medical_1)
    medical_2 = create(_Warehouses._Blue.medical_2)

    medical_express_1 = create(_Warehouses._Blue.medical_express_1, min_days=0, max_days=0)
    medical_express_2 = create(_Warehouses._Blue.medical_express_2, min_days=0, max_days=0)
    medical_express_3 = create(_Warehouses._Blue.medical_express_3, min_days=0, max_days=0)


class _DynamicDeliveryServiceInfo:
    def create(name):
        return DynamicDeliveryServiceInfo(
            id=_DeliveryServices.internal,
            name=name,
            region_to_region_info=[
                DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
            ],
        )

    shop_1 = create('shop_1_delivery_service')
    shop_2 = create('shop_2_delivery_service')
    shop_3 = create('shop_3_delivery_service')
    shop_4 = create('shop_4_delivery_service')
    shop_5 = create('shop_5_delivery_service')
    shop_blue_1 = create('shop_blue_1_delivery_service')
    shop_blue_2 = create('shop_blue_2_delivery_service')
    shop_blue_3 = create('shop_blue_3_delivery_service')
    shop_express_1 = create('shop_express_blue_1_delivery_service')
    shop_express_2 = create('shop_express_blue_2_delivery_service')
    shop_express_3 = create('shop_express_blue_3_delivery_service')


class _DynamicWarehouseAndDeliveryServiceInfo:
    def create(warehouse_id):
        return DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=warehouse_id,
            delivery_service_id=_DeliveryServices.internal,
            operation_time=0,
            date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=_Rids.russia)],
        )

    shop_1 = create(_Warehouses._White.medical_1)
    shop_2 = create(_Warehouses._White.medical_2)
    shop_3 = create(_Warehouses._White.medical_3)
    shop_4 = create(_Warehouses._White.medical_4)
    shop_5 = create(_Warehouses._White.medical_5)
    shop_blue_1 = create(_Warehouses._Blue.medical_1)
    shop_blue_2 = create(_Warehouses._Blue.medical_2)
    shop_blue_3 = create(_Warehouses._Blue.medical_3)
    shop_express_1 = create(_Warehouses._Blue.medical_express_1)
    shop_express_2 = create(_Warehouses._Blue.medical_express_2)
    shop_express_3 = create(_Warehouses._Blue.medical_express_3)


class _DynamicWarehouseInfo:
    def create(warehouse_id, is_express=False, working_schedule=_WorkingShedules.round_the_clock_intervals):
        return DynamicWarehouseInfo(
            id=warehouse_id,
            home_region=_Rids.moscow,
            holidays_days_set_key=2,
            is_express=is_express,
            shipment_schedule=working_schedule,
        )

    def create_rel(warehouse_id):
        return DynamicWarehouseToWarehouseInfo(warehouse_from=warehouse_id, warehouse_to=warehouse_id)

    shop_1 = create(_Warehouses._White.medical_1)
    shop_2 = create(_Warehouses._White.medical_2)
    shop_3 = create(_Warehouses._White.medical_3)
    shop_4 = create(_Warehouses._White.medical_4)
    shop_5 = create(_Warehouses._White.medical_5)
    shop_blue_1 = create(_Warehouses._Blue.medical_1)
    shop_blue_2 = create(_Warehouses._Blue.medical_2)
    shop_blue_3 = create(_Warehouses._Blue.medical_3)
    shop_express_1 = create(
        _Warehouses._Blue.medical_express_1,
        is_express=True,
        working_schedule=_WorkingShedules.round_the_clock_intervals,
    )
    shop_express_2 = create(
        _Warehouses._Blue.medical_express_2, is_express=True, working_schedule=_WorkingShedules.workdays_intervals
    )
    shop_express_3 = create(
        _Warehouses._Blue.medical_express_3, is_express=True, working_schedule=_WorkingShedules.everyday_intervals
    )
    shop_express = create(_Warehouses._White.medical_express, is_express=True)

    shop_1_rel = create_rel(_Warehouses._White.medical_1)
    shop_2_rel = create_rel(_Warehouses._White.medical_2)
    shop_3_rel = create_rel(_Warehouses._White.medical_3)
    shop_4_rel = create_rel(_Warehouses._White.medical_4)
    shop_5_rel = create_rel(_Warehouses._White.medical_5)
    shop_blue_1_rel = create_rel(_Warehouses._Blue.medical_1)
    shop_blue_2_rel = create_rel(_Warehouses._Blue.medical_2)
    shop_blue_3_rel = create_rel(_Warehouses._Blue.medical_3)
    shop_express_1_rel = create_rel(_Warehouses._Blue.medical_express_1)
    shop_express_2_rel = create_rel(_Warehouses._Blue.medical_express_2)
    shop_express_3_rel = create_rel(_Warehouses._Blue.medical_express_3)
    shop_express_rel = create_rel(_Warehouses._White.medical_express)


class _CombinatorExpressWarehouse:
    """
    Подготовленная информация о складах для combinator'а.
    """

    medical_express_1_priority_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_1,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=1,
    )
    medical_express_1_priority_2 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_1,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=2,
    )
    medical_express_1_priority_3 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_1,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=3,
    )

    medical_express_2_priority_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_2,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=1,
    )
    medical_express_2_priority_2 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_2,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=2,
    )

    medical_express_3_priority_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_3,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=1,
    )
    medical_express_3_priority_3 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_3,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express + 1,
        priority=3,
    )

    # комбинации проиритетов складов для разных значений gps
    LOCATION_A = [medical_express_1_priority_1, medical_express_2_priority_2, medical_express_3_priority_3]
    LOCATION_B = [medical_express_1_priority_2, medical_express_2_priority_1, medical_express_3_priority_3]
    LOCATION_C = [medical_express_1_priority_3, medical_express_2_priority_2, medical_express_3_priority_1]


class _Gps:
    class _Lat:
        location_a = 11.1111
        location_b = 22.2222
        location_c = 33.3333

    class _Lon:
        location_a = 44.4444
        location_b = 55.5555
        location_c = 66.6666


class _CombinatorGps:
    location_a = CombinatorGpsCoords(_Gps._Lat.location_a, _Gps._Lon.location_a)
    location_b = CombinatorGpsCoords(_Gps._Lat.location_b, _Gps._Lon.location_b)
    location_c = CombinatorGpsCoords(_Gps._Lat.location_c, _Gps._Lon.location_c)


class _RequestGps:
    def create(lat, lon):
        return 'lat:{lat};lon:{lon}'.format(lat=lat, lon=lon)

    location_a = create(_Gps._Lat.location_a, _Gps._Lon.location_a)
    location_b = create(_Gps._Lat.location_b, _Gps._Lon.location_b)
    location_c = create(_Gps._Lat.location_c, _Gps._Lon.location_c)


class _Requests:
    courier = (
        'place=combine'
        '&delivery-type=courier'
        '&pp=18'
        '&rids=213'
        '&rearr-factors=market_not_prescription_drugs_delivery={not_prescription_drugs_delivery}'
        '&rearr-factors=enable_prescription_drugs_delivery={prescription_drugs_delivery}'
    )

    pickup = (
        'place=combine'
        '&delivery-type=pickup'
        '&pp=18'
        '&rids=213'
        '&rearr-factors=market_not_prescription_drugs_delivery={not_prescription_drugs_delivery}'
        '&rearr-factors=enable_prescription_drugs_delivery={prescription_drugs_delivery}'
    )

    strategy = (
        'place=combine'
        '&split-strategy={strategy}'
        '&pp=18'
        '&rids={rid}'
        '&rearr-factors=market_not_prescription_drugs_delivery={not_prescription_drugs_delivery}'
        '&rearr-factors=enable_prescription_drugs_delivery={prescription_drugs_delivery}'
    )

    gps = (
        'place=combine'
        '&delivery-type=courier'
        '&pp=18'
        '&rids=213'
        '&gps={gps}'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=enable_prescription_drugs_delivery=1'
    )


class T(TestCase):
    """
    Набор тестов для проекта "Покупка списком на карте".
    Краткое описание:
    - Для запрашиваемой корзины товаров вернуть (пока акцент на медицинские товары):
      а) все точки продаж где есть в наличии запрашиваемые товары (далее сценарий с самовывозом)
      б) самый дешевый магазин где есть в наличии запрашиваемые товары и доставка (далее сценарий с доставкой)
    - Для сценария с самовывозом используется временно place=geo
    - Для сценария с доставкой используется place=combine и delivery-type=courier
    - Для выделения медицинской посылки используется place=combine и split-strategy=split-medicine
    - Для выбора вариантов доставки filter-express-delivery, preferable-courier-delivery-day
    - Для выбора режима подбора неполной посылки market_enable_incomplete_bucket_selection
    - Для выбора режима подбора с дроблением на подпосылки market_enable_multi_medical_parcels
    См.: https://st.yandex-team.ru/MARKETPROJECT-5706
    См.: https://st.yandex-team.ru/MARKETOUT-41263
    """

    @classmethod
    def prepare_experiments(cls):
        cls.settings.default_search_experiment_flags += [
            'enable_cart_split_on_combinator=0',
        ]
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=_Rids.moscow, name="Moscow", tz_offset=10800)]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._White.medical_1,
            _Shops._White.medical_2,
            _Shops._White.medical_3,
            _Shops._White.medical_4,
            _Shops._White.medical_5,
            _Shops._White.medical_express,
            _Shops._White.medical_share_1,
            _Shops._White.medical_share_2,
        ]

        cls.index.shops += [
            _Shops._Blue.medical_1,
            _Shops._Blue.medical_2,
            _Shops._Blue.medical_pickup,
            _Shops._Blue.virtual,
            _Shops._Blue.medical_express_1,
            _Shops._Blue.medical_express_2,
            _Shops._Blue.medical_express_3,
        ]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            _Mskus.medical_cpa,
            _Mskus.medical_cpa_1,
            _Mskus.medical_cpa_2,
            _Mskus.prescription_cpa,
            _Mskus.medical_cpc,
            _Mskus.medical_cpa_no_delivery,
            _Mskus.medical_cpa_blue,
            _Mskus.medical_cpa_blue_no_delivery,
            _Mskus.common_cpa,
            _Mskus.medical_no_stock,
            _Mskus.medical_share,
            _Mskus.medical_type_a,
            _Mskus.medical_type_b,
            _Mskus.medical_type_c,
            _Mskus.baa_cpa,
            _Mskus.medical_express_blue,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers._White.medical_cpc_1_shop_1,
            _Offers._White.common_cpa_1_shop_1,
            _Offers._White.baa_cpa_shop_1,
            _Offers._White.medical_cpa_1_shop_1,
            _Offers._White.medical_cpa_no_delivery_1_shop_1,
            _Offers._White.medical_cpa_no_delivery_2_shop_1,
            _Offers._White.prescription_cpa_1_shop_1,
            _Offers._White.medical_no_stock_shop_1,
            _Offers._White.medical_cpa_1_shop_2,
            _Offers._White.prescription_cpa_1_shop_2,
            _Offers._White.medical_cpa_1_shop_3,
            _Offers._White.medical_cpa_2_shop_3,
            _Offers._White.prescription_cpa_1_shop_3,
            _Offers._White.medical_cpa_2_shop_4,
            _Offers._White.prescription_cpa_1_shop_4,
            _Offers._White.medical_share_1,
            _Offers._White.medical_share_2,
            _Offers._White.medical_express_shop_msku_a,
            _Offers._White.medical_express_shop_msku_b,
            _Offers._White.medical_express_shop_msku_c,
            _Offers._White.medical_shop_1_msku_a,
            _Offers._White.medical_shop_1_msku_b,
            _Offers._White.medical_shop_1_msku_c,
            _Offers._White.medical_cpa_1_shop_5,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            _Models.medical_cpa,
            _Models.medical_cpa_1,
            _Models.medical_cpa_2,
            _Models.prescription_cpa,
            _Models.medical_cpc,
            _Models.medical_cpa_no_delivery,
            _Models.medical_cpa_blue,
            _Models.medical_cpa_blue_no_delivery,
            _Models.medical_no_stock,
            _Models.medical_share,
            _Models.medical_type_a,
            _Models.medical_type_b,
            _Models.medical_type_c,
            _Models.baa_cpa,
            _Models.medical_express_blue,
        ]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [
            _Buckets.medical_1,
            _Buckets.medical_2,
            _Buckets.medical_3,
            _Buckets.medical_4,
            _Buckets.medical_5,
            _Buckets.medical_blue_1,
            _Buckets.medical_blue_2,
            _Buckets.medical_express,
            _Buckets.medical_blue_express_1,
            _Buckets.medical_blue_express_2,
            _Buckets.medical_blue_express_3,
        ]

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += [_Buckets.medical_pickup_white, _Buckets.medical_pickup_blue]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [_Outlets._White.medical, _Outlets._Blue.medical]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        cls.dynamic.lms += [
            _DynamicDeliveryServiceInfo.shop_1,
            _DynamicDeliveryServiceInfo.shop_2,
            _DynamicDeliveryServiceInfo.shop_3,
            _DynamicDeliveryServiceInfo.shop_4,
            _DynamicDeliveryServiceInfo.shop_5,
            _DynamicDeliveryServiceInfo.shop_blue_1,
            _DynamicDeliveryServiceInfo.shop_blue_2,
            _DynamicDeliveryServiceInfo.shop_blue_3,
            _DynamicDeliveryServiceInfo.shop_express_1,
            _DynamicDeliveryServiceInfo.shop_express_2,
            _DynamicDeliveryServiceInfo.shop_express_3,
        ]

        cls.dynamic.lms += [
            _DynamicWarehouseAndDeliveryServiceInfo.shop_1,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_2,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_3,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_4,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_5,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_blue_1,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_blue_2,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_blue_3,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_express_1,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_express_2,
            _DynamicWarehouseAndDeliveryServiceInfo.shop_express_3,
        ]

        cls.dynamic.lms += [
            _DynamicWarehouseInfo.shop_1,
            _DynamicWarehouseInfo.shop_2,
            _DynamicWarehouseInfo.shop_3,
            _DynamicWarehouseInfo.shop_4,
            _DynamicWarehouseInfo.shop_5,
            _DynamicWarehouseInfo.shop_blue_1,
            _DynamicWarehouseInfo.shop_blue_2,
            _DynamicWarehouseInfo.shop_blue_3,
            _DynamicWarehouseInfo.shop_express_1,
            _DynamicWarehouseInfo.shop_express_2,
            _DynamicWarehouseInfo.shop_express_3,
            _DynamicWarehouseInfo.shop_express,
            _DynamicWarehouseInfo.shop_1_rel,
            _DynamicWarehouseInfo.shop_2_rel,
            _DynamicWarehouseInfo.shop_3_rel,
            _DynamicWarehouseInfo.shop_4_rel,
            _DynamicWarehouseInfo.shop_5_rel,
            _DynamicWarehouseInfo.shop_blue_1_rel,
            _DynamicWarehouseInfo.shop_blue_2_rel,
            _DynamicWarehouseInfo.shop_blue_3_rel,
            _DynamicWarehouseInfo.shop_express_1_rel,
            _DynamicWarehouseInfo.shop_express_2_rel,
            _DynamicWarehouseInfo.shop_express_3_rel,
            _DynamicWarehouseInfo.shop_express_rel,
        ]

    @classmethod
    def prepare_express_partners(cls):
        cls.index.express_partners.suppliers += [
            _ExpressSuppliers.medical_express,
            _ExpressSuppliers.medical_express_1,
            _ExpressSuppliers.medical_express_2,
            _ExpressSuppliers.medical_express_3,
        ]

    @classmethod
    def prepare_blue_respond(cls):
        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_1
        ).respond([_Buckets.medical_id_blue_1], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_2
        ).respond([_Buckets.medical_id_blue_2], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_express_1
        ).respond([_Buckets.medical_id_blue_express_1], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_express_2
        ).respond([_Buckets.medical_id_blue_express_2], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_express_3
        ).respond([_Buckets.medical_id_blue_express_3], [], [])

    @classmethod
    def prepare_nordstream(cls):
        # Sets up delivery schedule response for blue offers
        cls.dynamic.nordstream += [
            _DynamicWarehouseDelivery.medical_1,
            _DynamicWarehouseDelivery.medical_2,
            _DynamicWarehouseDelivery.medical_express_1,
            _DynamicWarehouseDelivery.medical_express_2,
            _DynamicWarehouseDelivery.medical_express_3,
        ]

    @classmethod
    def prepare_combinator(cls):
        rearr_factors = (
            'market_not_prescription_drugs_delivery=1'
            ';parallel_smm=1.0'
            ';ext_snippet=1'
            ';no_snippet_arc=1'
            ';market_enable_sins_offers_wizard=1'
            ';enable_prescription_drugs_delivery=1'
        )

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Rids.moscow,
            gps_coords=_CombinatorGps.location_a,
            rear_factors=rearr_factors,
        ).respond_with_express_warehouses(_CombinatorExpressWarehouse.LOCATION_A)

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Rids.moscow,
            gps_coords=_CombinatorGps.location_b,
            rear_factors=rearr_factors,
        ).respond_with_express_warehouses(_CombinatorExpressWarehouse.LOCATION_B)

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Rids.moscow,
            gps_coords=_CombinatorGps.location_c,
            rear_factors=rearr_factors,
        ).respond_with_express_warehouses(_CombinatorExpressWarehouse.LOCATION_C)

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Rids.moscow,
            gps_coords=_GpsWarehouses.gps_1.location_combinator,
            rear_factors=rearr_factors,
        ).respond_with_express_warehouses(_GpsWarehouses.blue_express_1_best)

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Rids.moscow,
            gps_coords=_GpsWarehouses.gps_2.location_combinator,
            rear_factors=rearr_factors,
        ).respond_with_express_warehouses(_GpsWarehouses.blue_express_2_best)

        cls.combinatorExpress.on_express_warehouses_request(
            region_id=_Rids.moscow,
            gps_coords=_GpsWarehouses.gps_3.location_combinator,
            rear_factors=rearr_factors,
        ).respond_with_express_warehouses(_GpsWarehouses.blue_express_3_best)

    # White tests

    def test_delivery_white_cpa_best_option(self):
        """
        Проверям что на выдачу попадает магазин с самыми дешевыми товарами и с
        полным ассортиментом.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={medical_ware_id}:1;msku:{medical_msku},'
            '{prescription_ware_id}:1;msku:{prescription_msku},'
            '{medical_ware_id_1}:1;msku:{medical_msku_1}'
        ).format(
            medical_ware_id=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            medical_msku=_Offers._White.medical_cpa_1_shop_1.sku,
            prescription_ware_id=_Offers._White.prescription_cpa_1_shop_2.ware_md5,
            prescription_msku=_Offers._White.prescription_cpa_1_shop_2.sku,
            medical_ware_id_1=_Offers._White.medical_cpa_2_shop_3.ware_md5,
            medical_msku_1=_Offers._White.medical_cpa_2_shop_3.sku,
        )

        # Без указания вариантов доставки расчет происходит по наиболее широкому диапазону
        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_3.fesh,
                                    "IsMedicalParcel": True,
                                    "deliveryDayFrom": 1,
                                    "deliveryDayTo": 4,
                                    "deliveryTotalPrice": {"currency": "RUR", "value": "99"},
                                    "offersTotalPrice": {"currency": "RUR", "value": "300"},
                                    "deliveryPartnerTypes": ["SHOP"],
                                    "offers": [
                                        {
                                            "count": 1,
                                            "marketSku": _Offers._White.prescription_cpa_1_shop_3.sku,
                                            "wareId": _Offers._White.prescription_cpa_1_shop_3.ware_md5,
                                        },
                                        {
                                            "count": 1,
                                            "marketSku": _Offers._White.medical_cpa_1_shop_3.sku,
                                            "wareId": _Offers._White.medical_cpa_1_shop_3.ware_md5,
                                        },
                                        {
                                            "count": 1,
                                            "marketSku": _Offers._White.medical_cpa_2_shop_3.sku,
                                            "wareId": _Offers._White.medical_cpa_2_shop_3.ware_md5,
                                        },
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 7,
                }
            },
            allow_different_len=False,
        )

    def test_delivery_white_cpa_best_option_with_prescription_delivery_restriction(self):
        """
        Проверям что на выдаче ничего нет из-за запрета доставки рецептурных препаратов.
        """

        request = _Requests.courier.format(
            not_prescription_drugs_delivery=1, prescription_drugs_delivery=0  # restriction
        )

        offers_list = (
            '&offers-list={medical_ware_id}:1;msku:{medical_msku},' '{prescription_ware_id}:1;msku:{prescription_msku}'
        ).format(
            medical_ware_id=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            medical_msku=_Offers._White.medical_cpa_1_shop_1.sku,
            prescription_ware_id=_Offers._White.prescription_cpa_1_shop_2.ware_md5,
            prescription_msku=_Offers._White.prescription_cpa_1_shop_2.sku,
        )

        arguments = '&rearr-factors=market_enable_incomplete_bucket_selection=0'

        response = self.report.request_json(request + offers_list + arguments)
        self.assertFragmentIn(
            response, {"search": {"results": [{"buckets": []}], "total": 0}}, allow_different_len=False
        )

    def test_delivery_white_cpa_with_all_delivery_restriction(self):
        """
        Проверям что на выдачу не попадает ничего исходя из ограниений на доставку
        любых медицинских препаратов.
        """

        request = _Requests.courier.format(
            not_prescription_drugs_delivery=0, prescription_drugs_delivery=0  # restriction  # restriction
        )

        offers_list = (
            '&offers-list={medical_ware_id}:1;msku:{medical_msku},' '{prescription_ware_id}:1;msku:{prescription_msku}'
        ).format(
            medical_ware_id=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            medical_msku=_Offers._White.medical_cpa_1_shop_1.sku,
            prescription_ware_id=_Offers._White.prescription_cpa_1_shop_2.ware_md5,
            prescription_msku=_Offers._White.prescription_cpa_1_shop_2.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response, {"search": {"results": [{"buckets": []}], "total": 0}}, allow_different_len=False
        )

    def test_delivery_white_cpc_exclude(self):
        """
        Проверям что на выдачу не попадают белые СPC оффера.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=_Offers._White.medical_cpc_1_shop_1.ware_md5, msku=_Offers._White.medical_cpc_1_shop_1.sku
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response, {"search": {"results": [{"buckets": []}], "total": 0}}, allow_different_len=False
        )

    def test_delivery_white_cpa_non_courier_exclude(self):
        """
        Проверям что на выдачу не попадают белые cpa оффера без доставки.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=_Offers._White.medical_cpa_no_delivery_1_shop_1.ware_md5,
            msku=_Offers._White.medical_cpa_no_delivery_1_shop_1.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response, {"search": {"results": [{"buckets": []}], "total": 0}}, allow_different_len=False
        )

    def test_delivery_white_cpa_store_stock_count(self):
        """
        Проверям что на выдачу отображаются белые сpa оффера с доступным количеством
        на складе, если запришиваемое больше.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:100;msku:{msku}').format(
            ware_id=_Offers._White.medical_cpa_1_shop_1.ware_md5, msku=_Offers._White.medical_cpa_1_shop_1.sku
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "count": 10,  # запрашивали 100, доступно 10
                                            "wareId": _Offers._White.medical_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )

    def test_delivery_white_cpa_zero_store_stock_count(self):
        """
        Проверям что на выдачу попадают белые сpa оффера только с положительным
        количеством на складе (если оффер не скрыт, а складское значение 0
        будет выставлено 1).
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:10;msku:{msku}').format(
            ware_id=_Offers._White.medical_no_stock_shop_1.ware_md5, msku=_Offers._White.medical_no_stock_shop_1.sku
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "count": 1,  # replace 0 -> 1
                                            "marketSku": _Offers._White.medical_no_stock_shop_1.sku,
                                            "wareId": _Offers._White.medical_no_stock_shop_1.ware_md5,
                                        }
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 1,
                }
            },
            allow_different_len=False,
        )

    @staticmethod
    def _prepare_delivery_offer_multi_warehouses_request(
        ware_id, msku, disable_multiple_warehouses_flag, use_warehouse_priorities_filtering_flag
    ):
        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=ware_id,
            msku=msku,
        )

        extra_flags = (
            '&rearr-factors=market_disable_shopping_list_multiple_warehouses={disable_multiple_warehouses_flag}'
            '&rearr-factors=market_use_warehouse_priorities_filtering={use_warehouse_priorities_filtering_flag}'
            '&debug=1'
        ).format(
            disable_multiple_warehouses_flag=disable_multiple_warehouses_flag,
            use_warehouse_priorities_filtering_flag=use_warehouse_priorities_filtering_flag,
        )

        return request + offers_list + extra_flags

    def _assert_white_cpa_offers_no_filtering(self, response):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_share_2.fesh,
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "marketSku": _Offers._White.medical_share_2.sku,
                                            "wareId": _Offers._White.medical_share_2.ware_md5,
                                        }
                                    ],
                                },
                                {
                                    "offers": [
                                        {
                                            "reason": "ABSENCE_OF_HYPER_ID",
                                            "replacedId": _Offers._White.medical_share_1.ware_md5,
                                            "wareId": "",
                                        }
                                    ]
                                },
                                {
                                    "shopId": _Shops._White.medical_share_1.fesh,
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "marketSku": _Offers._White.medical_share_1.sku,
                                            "wareId": _Offers._White.medical_share_1.ware_md5,
                                        }
                                    ],
                                },
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def _assert_white_cpa_offers_with_filtering(self, response):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_share_2.fesh,
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "marketSku": _Offers._White.medical_share_2.sku,
                                            "wareId": _Offers._White.medical_share_2.ware_md5,
                                        }
                                    ],
                                },
                                {
                                    "offers": [
                                        {
                                            "reason": "ABSENCE_OF_HYPER_ID",
                                            "replacedId": _Offers._White.medical_share_1.ware_md5,
                                            "wareId": "",
                                        }
                                    ]
                                },
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def test_multi_warehouses_white_cpa_offer_without_ignoring_without_filtering(self):
        """
        Проверям что на выдачу попадают белые cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 0.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 0.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Offers._White.medical_share_1.ware_md5,
            msku=_Offers._White.medical_share_1.sku,
            disable_multiple_warehouses_flag=0,
            use_warehouse_priorities_filtering_flag=0,
        )
        response = self.report.request_json(request)

        self._assert_white_cpa_offers_no_filtering(response)

    def test_multi_warehouses_white_cpa_offer_without_ignoring_with_filtering(self):
        """
        Проверям что на выдачу попадают белые cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 0.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 1.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Offers._White.medical_share_1.ware_md5,
            msku=_Offers._White.medical_share_1.sku,
            disable_multiple_warehouses_flag=0,
            use_warehouse_priorities_filtering_flag=1,
        )
        response = self.report.request_json(request)

        self._assert_white_cpa_offers_no_filtering(response)

        self.assertFragmentIn(
            response, 'List of hyperlocal warehouses is not specified. Filtering will not be performed.'
        )

    def test_multi_warehouses_white_cpa_offer_with_ignoring_without_filtering(self):
        """
        Проверям что на выдачу попадают белые cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 1.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 0.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Offers._White.medical_share_1.ware_md5,
            msku=_Offers._White.medical_share_1.sku,
            disable_multiple_warehouses_flag=1,
            use_warehouse_priorities_filtering_flag=0,
        )
        response = self.report.request_json(request)

        self._assert_white_cpa_offers_with_filtering(response)

    def test_multi_warehouses_white_cpa_offer_with_ignoring_with_filtering(self):
        """
        Проверям что на выдачу попадают белые cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 1.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 1.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Offers._White.medical_share_1.ware_md5,
            msku=_Offers._White.medical_share_1.sku,
            disable_multiple_warehouses_flag=1,
            use_warehouse_priorities_filtering_flag=1,
        )
        response = self.report.request_json(request)

        self._assert_white_cpa_offers_with_filtering(response)

        self.assertFragmentIn(
            response, 'List of hyperlocal warehouses is not specified. Filtering will not be performed.'
        )

    def test_pickup_white_cpa(self):
        """
        Проверям что на выдачу не попадают белые cpa оффера без cамовывоза.
        """

        request = _Requests.pickup.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={courier_ware_id}:1;msku:{courier_msku},'
            '{pickup_ware_id_1}:1;msku:{pickup_msku_1},'
            '{pickup_ware_id_2}:1;msku:{pickup_msku_2}'
        ).format(
            courier_ware_id=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            courier_msku=_Offers._White.medical_cpa_1_shop_1.sku,
            pickup_ware_id_1=_Offers._White.medical_cpa_no_delivery_1_shop_1.ware_md5,
            pickup_msku_1=_Offers._White.medical_cpa_no_delivery_1_shop_1.sku,
            pickup_ware_id_2=_Offers._White.medical_cpa_no_delivery_2_shop_1.ware_md5,
            pickup_msku_2=_Offers._White.medical_cpa_no_delivery_2_shop_1.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "hasPickup": True,
                                    "offers": [
                                        {
                                            "marketSku": _Offers._White.medical_cpa_no_delivery_1_shop_1.sku,
                                            "wareId": _Offers._White.medical_cpa_no_delivery_1_shop_1.ware_md5,
                                        },
                                        {
                                            "marketSku": _Offers._White.medical_cpa_no_delivery_2_shop_1.sku,
                                            "wareId": _Offers._White.medical_cpa_no_delivery_2_shop_1.ware_md5,
                                        },
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def test_delivery_white_cpa_baa_exclude(self):
        """
        Проверям что на выдачу не попадают невходящие в разрешенную группу
        медицинские препараты для проекта покупка списком (это БАДы).
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={baa_ware_id}:1;msku:{baa_msku}').format(
            baa_ware_id=_Offers._White.baa_cpa_shop_1.ware_md5,
            baa_msku=_Offers._White.baa_cpa_shop_1.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response, {"search": {"results": [{"buckets": []}], "total": 0}}, allow_different_len=False
        )

    def test_medical_booking_filter_white(self):
        """
        Проверям что на выдачу попадают белые cpa оффера c cамовывозом и признаком
        'бронирование из наличия/medical_booking'.
        """

        request = _Requests.pickup.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=_Offers._White.medical_cpa_no_delivery_1_shop_1.ware_md5,
            msku=_Offers._White.medical_cpa_no_delivery_1_shop_1.sku,
        )

        response = self.report.request_json(request + offers_list + "&filter-medical-booking=1")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "hasPickup": True,
                                    "offers": [
                                        {
                                            "marketSku": _Offers._White.medical_cpa_no_delivery_1_shop_1.sku,
                                            "wareId": _Offers._White.medical_cpa_no_delivery_1_shop_1.ware_md5,
                                        },
                                    ],
                                },
                            ],
                        },
                    ],
                    "offers": {
                        "items": [
                            {
                                "wareId": _Offers._White.medical_cpa_no_delivery_1_shop_1.ware_md5,
                                "specs": {
                                    "internal": [
                                        {
                                            "type": "spec",
                                            "value": "medicine",
                                            "usedParams": [],
                                        },
                                        {
                                            "type": "spec",
                                            "value": "medical_booking",
                                            "usedParams": [],
                                        },
                                    ],
                                },
                            },
                        ],
                    },
                    "total": 1,
                },
            },
            allow_different_len=False,
        )

    # Blue tests

    def test_delivery_blue_cpa_offer(self):
        """
        Проверям что на выдачу попадают синие cpa офферы c доставкой.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=_Mskus.blue_offer_1.waremd5, msku=_Mskus.medical_cpa_blue_id
        )

        response = self.report.request_json(request + offers_list + '&preferable-courier-delivery-day=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._Blue.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "deliveryDayFrom": 1,
                                    "deliveryDayTo": 1,
                                    "offersTotalPrice": {"currency": "RUR", "value": "10"},
                                    "deliveryPartnerTypes": ["YANDEX_MARKET"],
                                    "offers": [{"count": 1, "marketSku": str(_Mskus.blue_offer_1.sku)}],
                                }
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def _assert_blue_cpa_offers_no_filtering(self, response):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._Blue.medical_1.fesh,
                                    "offers": [
                                        {
                                            "marketSku": str(_Mskus.blue_offer_2.sku),
                                            "wareId": _Mskus.blue_offer_2.waremd5,
                                        }
                                    ],
                                },
                                {
                                    "offers": [
                                        {
                                            "reason": "ABSENCE_OF_HYPER_ID",
                                            "replacedId": _Mskus.blue_offer_1.waremd5,
                                            "wareId": "",
                                        }
                                    ]
                                },
                                {
                                    "shopId": _Shops._Blue.medical_1.fesh,
                                    "offers": [
                                        {
                                            "marketSku": str(_Mskus.blue_offer_1.sku),
                                            "wareId": _Mskus.blue_offer_1.waremd5,
                                        }
                                    ],
                                },
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def _assert_blue_cpa_offers_with_filtering(self, response):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._Blue.medical_1.fesh,
                                    "offers": [
                                        {
                                            "marketSku": str(_Mskus.blue_offer_2.sku),
                                            "wareId": _Mskus.blue_offer_2.waremd5,
                                        }
                                    ],
                                },
                                {
                                    "offers": [
                                        {
                                            "reason": "ABSENCE_OF_HYPER_ID",
                                            "replacedId": _Mskus.blue_offer_1.waremd5,
                                            "wareId": "",
                                        }
                                    ]
                                },
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def test_multi_warehouses_blue_cpa_offer_without_ignoring_without_filtering(self):
        """
        Проверям что на выдачу попадают синие cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 0.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 0.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Mskus.blue_offer_1.waremd5,
            msku=_Mskus.medical_cpa_blue_id,
            disable_multiple_warehouses_flag=0,
            use_warehouse_priorities_filtering_flag=0,
        )
        response = self.report.request_json(request)

        self._assert_blue_cpa_offers_no_filtering(response)

    def test_multi_warehouses_blue_cpa_offer_without_ignoring_with_filtering(self):
        """
        Проверям что на выдачу попадают синие cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 0.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 1.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Mskus.blue_offer_1.waremd5,
            msku=_Mskus.medical_cpa_blue_id,
            disable_multiple_warehouses_flag=0,
            use_warehouse_priorities_filtering_flag=1,
        )
        response = self.report.request_json(request)

        self._assert_blue_cpa_offers_no_filtering(response)

        self.assertFragmentIn(
            response, 'List of hyperlocal warehouses is not specified. Filtering will not be performed.'
        )

    def test_multi_warehouses_blue_cpa_offer_with_ignoring_without_filtering(self):
        """
        Проверям что на выдачу попадают синие cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 1.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 0.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Mskus.blue_offer_1.waremd5,
            msku=_Mskus.medical_cpa_blue_id,
            disable_multiple_warehouses_flag=1,
            use_warehouse_priorities_filtering_flag=0,
        )
        response = self.report.request_json(request)

        self._assert_blue_cpa_offers_with_filtering(response)

    def test_multi_warehouses_blue_cpa_offer_with_ignoring_with_filtering(self):
        """
        Проверям что на выдачу попадают синие cpa многоскладовые офферы.
        Также проверяем выдачу при выставлении запрета на многоскладовость -
        rearr-флаг market_disable_shopping_list_multiple_warehouses - значение в тесте 1.
        Также проверяем выдачу при выставлении фильтрации складов по приоритетам -
        rearr-флаг market_use_warehouse_priorities_filtering - значение в тесте 1.
        """

        request = self._prepare_delivery_offer_multi_warehouses_request(
            ware_id=_Mskus.blue_offer_1.waremd5,
            msku=_Mskus.medical_cpa_blue_id,
            disable_multiple_warehouses_flag=1,
            use_warehouse_priorities_filtering_flag=1,
        )
        response = self.report.request_json(request)

        self._assert_blue_cpa_offers_with_filtering(response)

        self.assertFragmentIn(
            response, 'List of hyperlocal warehouses is not specified. Filtering will not be performed.'
        )

    def test_pickup_blue_cpa(self):
        """
        Проверям что на выдачу не попадают синие cpa оффера без cамовывоза.
        """

        request = _Requests.pickup.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={courier_ware_id}:1;msku:{courier_msku},'
            '{pickup_ware_id_1}:1;msku:{pickup_msku_1},'
            '{pickup_ware_id_2}:1;msku:{pickup_msku_2}'
        ).format(
            courier_ware_id=_Mskus.blue_offer_1.waremd5,
            courier_msku=_Mskus.medical_cpa_blue_id,
            pickup_ware_id_1=_Mskus.blue_offer_pickup_1.waremd5,
            pickup_msku_1=_Mskus.medical_cpa_blue_no_delivery_id,
            pickup_ware_id_2=_Mskus.blue_offer_pickup_2.waremd5,
            pickup_msku_2=_Mskus.medical_cpa_blue_no_delivery_id,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._Blue.medical_pickup.fesh,
                                    "IsMedicalParcel": True,
                                    "hasPickup": True,
                                    "offers": [
                                        {
                                            "marketSku": str(_Mskus.blue_offer_pickup_1.sku),
                                            "wareId": _Mskus.blue_offer_pickup_1.waremd5,
                                        },
                                        {
                                            "marketSku": str(_Mskus.blue_offer_pickup_2.sku),
                                            "wareId": _Mskus.blue_offer_pickup_2.waremd5,
                                        },
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 2,
                }
            },
            allow_different_len=False,
        )

    def test_medical_booking_filter_blue(self):
        """
        Проверям что на выдачу попадают синие cpa оффера c cамовывозом и признаком
        'бронирование из наличия/medical_booking'.
        """

        request = _Requests.pickup.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=_Mskus.blue_offer_pickup_1.waremd5,
            msku=_Mskus.medical_cpa_blue_no_delivery_id,
        )

        response = self.report.request_json(request + offers_list + "&filter-medical-booking=1")
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._Blue.medical_pickup.fesh,
                                    "IsMedicalParcel": True,
                                    "hasPickup": True,
                                    "offers": [
                                        {
                                            "marketSku": str(_Mskus.blue_offer_pickup_1.sku),
                                            "wareId": _Mskus.blue_offer_pickup_1.waremd5,
                                        },
                                    ],
                                }
                            ]
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "wareId": _Mskus.blue_offer_pickup_1.waremd5,
                                "specs": {
                                    "internal": [
                                        {
                                            "type": "spec",
                                            "value": "medicine",
                                            "usedParams": [],
                                        },
                                        {
                                            "type": "spec",
                                            "value": "medical_booking",
                                            "usedParams": [],
                                        },
                                    ],
                                },
                            },
                        ],
                    },
                    "total": 1,
                },
            },
            allow_different_len=False,
        )

    # Split strategy tests

    def test_split_combine_strategy_medicine_with_medical_offers(self):
        """
        Проверям что на выдаче происходит выделение медицинской посылки даже если
        на вход переданы исключительно медицинские оффера.
        """

        request = _Requests.strategy.format(
            strategy='split-medicine',
            rid=_Rids.moscow,
            not_prescription_drugs_delivery=1,
            prescription_drugs_delivery=1,
        )

        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1};cart_item_id:1,'
            '{ware_id_2}:1;msku:{msku_2};cart_item_id:2,'
            '{ware_id_3}:1;msku:{msku_3};cart_item_id:3'
        ).format(
            ware_id_1=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            msku_1=_Offers._White.medical_cpa_1_shop_1.sku,
            ware_id_2=_Offers._White.prescription_cpa_1_shop_2.ware_md5,
            msku_2=_Offers._White.prescription_cpa_1_shop_2.sku,
            ware_id_3=_Mskus.blue_offer_1.waremd5,
            msku_3=_Mskus.medical_cpa_blue_id,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.medical_cpa_1_shop_1.sku,
                                            "replacedId": _Offers._White.medical_cpa_1_shop_1.ware_md5,
                                            "wareId": _Offers._White.medical_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [2],
                                            "marketSku": _Offers._White.prescription_cpa_1_shop_2.sku,
                                            "replacedId": _Offers._White.prescription_cpa_1_shop_2.ware_md5,
                                            "wareId": _Offers._White.prescription_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [3],
                                            "marketSku": str(_Mskus.medical_cpa_blue_id),
                                            "replacedId": _Mskus.blue_offer_1.waremd5,
                                            "wareId": _Mskus.blue_offer_1.waremd5,
                                        }
                                    ],
                                },
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_split_combine_strategy_medicine_with_medical_offers_and_different_region(self):
        """
        Проверям что на выдаче происходит выделение медицинской посылки при
        переключении региона.
        """

        request = _Requests.strategy.format(
            strategy='split-medicine',
            rid=_Rids.spb,  # Оффер в запросе Московский
            not_prescription_drugs_delivery=1,
            prescription_drugs_delivery=1,
        )

        offers_list = ('&offers-list={ware_id}:1;msku:{msku};cart_item_id:1,').format(
            ware_id=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            msku=_Offers._White.medical_cpa_1_shop_1.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [1],
                                            "marketSku": _Offers._White.medical_cpa_1_shop_1.sku,
                                            "replacedId": _Offers._White.medical_cpa_1_shop_1.ware_md5,
                                            "wareId": _Offers._White.medical_cpa_1_shop_5.ware_md5,
                                        }
                                    ],
                                },
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_split_combine_strategy_medicine_with_differnet_offers(self):
        """
        Проверям что на выдаче происходит разделение на разные посылки
        (для медицины просто разделение, без расчета доставки).
        """

        request = _Requests.strategy.format(
            strategy='split-medicine',
            rid=_Rids.moscow,
            not_prescription_drugs_delivery=1,
            prescription_drugs_delivery=1,
        )

        offers_list = (
            '&offers-list={common_ware_id}:1;msku:{common_msku};cart_item_id:1,'
            '{baa_ware_id}:1;msku:{baa_msku};cart_item_id:2,'
            '{medical_ware_id_1}:1;msku:{medical_msku_1};cart_item_id:3,'
            '{medical_ware_id_2}:1;msku:{medical_msku_2};cart_item_id:4'
        ).format(
            common_ware_id=_Offers._White.common_cpa_1_shop_1.ware_md5,
            common_msku=_Offers._White.common_cpa_1_shop_1.sku,
            baa_ware_id=_Offers._White.baa_cpa_shop_1.ware_md5,
            baa_msku=_Offers._White.baa_cpa_shop_1.sku,
            medical_ware_id_1=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            medical_msku_1=_Offers._White.medical_cpa_1_shop_1.sku,
            medical_ware_id_2=_Offers._White.prescription_cpa_1_shop_2.ware_md5,
            medical_msku_2=_Offers._White.prescription_cpa_1_shop_2.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "IsMedicalParcel": Absent(),
                                    "offers": [
                                        {"cartItemIds": [1], "replacedId": _Offers._White.common_cpa_1_shop_1.ware_md5},
                                        {"cartItemIds": [2], "replacedId": _Offers._White.baa_cpa_shop_1.ware_md5},
                                    ],
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [3],
                                            "marketSku": _Offers._White.medical_cpa_1_shop_1.sku,
                                            "replacedId": _Offers._White.medical_cpa_1_shop_1.ware_md5,
                                            "wareId": _Offers._White.medical_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "offers": [
                                        {
                                            "cartItemIds": [4],
                                            "marketSku": _Offers._White.prescription_cpa_1_shop_2.sku,
                                            "replacedId": _Offers._White.prescription_cpa_1_shop_2.ware_md5,
                                            "wareId": _Offers._White.prescription_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                },
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_split_combine_strategy_medicine_with_common_offers(self):
        """
        Проверям что на выдаче не происходит разделение на разные посылки,
        если нет в запросе медицинских офферов.
        """

        request = _Requests.strategy.format(
            strategy='split-medicine',
            rid=_Rids.moscow,
            not_prescription_drugs_delivery=1,
            prescription_drugs_delivery=1,
        )

        offers_list = (
            '&offers-list={common_ware_id}:1;msku:{common_msku};cart_item_id:1,'
            '{baa_ware_id}:1;msku:{baa_msku};cart_item_id:2'
        ).format(
            common_ware_id=_Offers._White.common_cpa_1_shop_1.ware_md5,
            common_msku=_Offers._White.common_cpa_1_shop_1.sku,
            baa_ware_id=_Offers._White.baa_cpa_shop_1.ware_md5,
            baa_msku=_Offers._White.baa_cpa_shop_1.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": Absent(),
                                    "offers": [
                                        {"cartItemIds": [1], "wareId": _Offers._White.common_cpa_1_shop_1.ware_md5},
                                        {"cartItemIds": [2], "wareId": _Offers._White.baa_cpa_shop_1.ware_md5},
                                    ],
                                }
                            ],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_split_combine_strategy_all(self):
        """
        Проверям что на выдаче не происходит разделения на разные посылки.
        """

        request = _Requests.strategy.format(
            strategy='all', rid=_Rids.moscow, not_prescription_drugs_delivery=1, prescription_drugs_delivery=1
        )

        offers_list = (
            '&offers-list={common_ware_id}:1;msku:{common_msku};cart_item_id:1,'
            '{baa_ware_id}:1;msku:{baa_msku};cart_item_id:2,'
            '{medical_ware_id}:1;msku:{medical_msku};cart_item_id:3'
        ).format(
            common_ware_id=_Offers._White.common_cpa_1_shop_1.ware_md5,
            common_msku=_Offers._White.common_cpa_1_shop_1.sku,
            baa_ware_id=_Offers._White.baa_cpa_shop_1.ware_md5,
            baa_msku=_Offers._White.baa_cpa_shop_1.sku,
            medical_ware_id=_Offers._White.medical_cpa_1_shop_2.ware_md5,
            medical_msku=_Offers._White.medical_cpa_1_shop_2.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": Absent(),
                                    "offers": [
                                        {"wareId": _Offers._White.common_cpa_1_shop_1.ware_md5},
                                        {"wareId": _Offers._White.baa_cpa_shop_1.ware_md5},
                                    ],
                                },
                                {
                                    "shopId": _Shops._White.medical_2.fesh,
                                    "IsMedicalParcel": Absent(),
                                    "offers": [{"wareId": _Offers._White.medical_cpa_1_shop_2.ware_md5}],
                                },
                            ],
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )

    # Delivery option tests

    def test_delivery_option_express(self):
        """
        Проверям что на выдачу попадает посылка только от экспресс поставщика.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={type_a_ware_id}:1;msku:{type_a_msku},'
            '{type_b_ware_id}:1;msku:{type_b_msku},'
            '{type_c_ware_id}:1;msku:{type_c_msku}'
        ).format(
            type_a_ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5,
            type_a_msku=_Offers._White.medical_shop_1_msku_a.sku,
            type_b_ware_id=_Offers._White.medical_shop_1_msku_b.ware_md5,
            type_b_msku=_Offers._White.medical_shop_1_msku_b.sku,
            type_c_ware_id=_Offers._White.medical_shop_1_msku_c.ware_md5,
            type_c_msku=_Offers._White.medical_shop_1_msku_c.sku,
        )

        response = self.report.request_json(request + offers_list + '&filter-express-delivery=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_express.fesh,
                                    "IsMedicalParcel": True,
                                    "IsExpressParcel": True,
                                    "deliveryDayFrom": 0,
                                    "deliveryDayTo": 1,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_express_shop_msku_a.ware_md5},
                                        {"wareId": _Offers._White.medical_express_shop_msku_b.ware_md5},
                                        {"wareId": _Offers._White.medical_express_shop_msku_c.ware_md5},
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )

    def test_delivery_option_today(self):
        """
        Проверям что на выдачу попадает посылка только от обычного поставщика
        со сроками доставки "сегодня".
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={type_a_ware_id}:2;msku:{type_a_msku},'
            '{type_b_ware_id}:2;msku:{type_b_msku},'
            '{type_c_ware_id}:2;msku:{type_c_msku}'
        ).format(
            type_a_ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5,
            type_a_msku=_Offers._White.medical_shop_1_msku_a.sku,
            type_b_ware_id=_Offers._White.medical_shop_1_msku_b.ware_md5,
            type_b_msku=_Offers._White.medical_shop_1_msku_b.sku,
            type_c_ware_id=_Offers._White.medical_shop_1_msku_c.ware_md5,
            type_c_msku=_Offers._White.medical_shop_1_msku_c.sku,
        )

        response = self.report.request_json(
            request
            + offers_list
            + '&preferable-courier-delivery-day=0'
            + '&rearr-factors=market_enable_incomplete_bucket_selection=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "IsExpressParcel": False,
                                    "deliveryDayFrom": 0,
                                    "deliveryDayTo": 0,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_shop_1_msku_a.ware_md5},
                                        {"wareId": _Offers._White.medical_shop_1_msku_b.ware_md5},
                                    ],
                                    "offersTotalPrice": {"value": "40"},
                                }
                            ]
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )

    def test_delivery_option_tomorrow(self):
        """
        Проверям что на выдачу попадает посылка только от обычного поставщика
        со сроками доставки "завтра" (допускается включение офферов со сроками
        доставки "сегодня" если есть хотя бы один со сроками доставки "завтра").
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={type_a_ware_id}:1;msku:{type_a_msku},'
            '{type_b_ware_id}:1;msku:{type_b_msku},'
            '{type_c_ware_id}:1;msku:{type_c_msku}'
        ).format(
            type_a_ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5,
            type_a_msku=_Offers._White.medical_shop_1_msku_a.sku,
            type_b_ware_id=_Offers._White.medical_shop_1_msku_b.ware_md5,
            type_b_msku=_Offers._White.medical_shop_1_msku_b.sku,
            type_c_ware_id=_Offers._White.medical_shop_1_msku_c.ware_md5,
            type_c_msku=_Offers._White.medical_shop_1_msku_c.sku,
        )

        response = self.report.request_json(request + offers_list + '&preferable-courier-delivery-day=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "IsExpressParcel": False,
                                    "deliveryDayFrom": 1,
                                    "deliveryDayTo": 1,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_shop_1_msku_a.ware_md5},
                                        {"wareId": _Offers._White.medical_shop_1_msku_b.ware_md5},
                                        {"wareId": _Offers._White.medical_shop_1_msku_c.ware_md5},
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )

    def test_delivery_option_tomorrow_with_today_offers(self):
        """
        Проверям что на выдачу не попадет посылка, потому что ее оффера
        не соответсвуют срокам доставки "завтра" (все оффера с "сегодня" графиком).
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={type_a_ware_id}:1;msku:{type_a_msku},' '{type_b_ware_id}:1;msku:{type_b_msku},'
        ).format(
            type_a_ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5,
            type_a_msku=_Offers._White.medical_shop_1_msku_a.sku,
            type_b_ware_id=_Offers._White.medical_shop_1_msku_b.ware_md5,
            type_b_msku=_Offers._White.medical_shop_1_msku_b.sku,
        )

        arguments = (
            '&preferable-courier-delivery-day=1' '&debug=1' '&rearr-factors=market_enable_incomplete_bucket_selection=0'
        )

        response = self.report.request_json(request + offers_list + arguments)
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    Contains(
                        "[ME]",
                        "ShopId '{id}' parcel does not match with delivery option, drop it ...".format(
                            id=_Shops._White.medical_1.fesh
                        ),
                    ),
                    Contains(
                        "[ME]",
                        "ShopId '{id}' does not have full size parcel (0/2), drop it ...".format(
                            id=_Shops._White.medical_1.fesh
                        ),
                    ),
                ]
            },
        )

    def test_delivery_option_other(self):
        """
        Проверям что на выдачу попадает посылка от самого "подходящего" поставщика с
        комбинированными сроками доставки (если не было указано никаких опций доставки).
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={type_a_ware_id}:1;msku:{type_a_msku},'
            '{type_b_ware_id}:1;msku:{type_b_msku},'
            '{type_c_ware_id}:1;msku:{type_c_msku}'
        ).format(
            type_a_ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5,
            type_a_msku=_Offers._White.medical_shop_1_msku_a.sku,
            type_b_ware_id=_Offers._White.medical_shop_1_msku_b.ware_md5,
            type_b_msku=_Offers._White.medical_shop_1_msku_b.sku,
            type_c_ware_id=_Offers._White.medical_shop_1_msku_c.ware_md5,
            type_c_msku=_Offers._White.medical_shop_1_msku_c.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_1.fesh,
                                    "IsMedicalParcel": True,
                                    "IsExpressParcel": False,
                                    "deliveryDayFrom": 0,
                                    "deliveryDayTo": 1,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_shop_1_msku_a.ware_md5},
                                        {"wareId": _Offers._White.medical_shop_1_msku_b.ware_md5},
                                        {"wareId": _Offers._White.medical_shop_1_msku_c.ware_md5},
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )

    # Other tests

    def test_invalid_msku_cgi(self):
        """
        Проверям что обрабатывается отсутствие mksu.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1').format(ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5)

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI_MSKU"}})
        self.error_log.expect(code=3043)

    def test_invalid_delivery_day_cgi(self):
        """
        Проверям что обрабатывается недопустимое значение дня доставки.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
            ware_id=_Offers._White.medical_shop_1_msku_a.ware_md5, msku=_Offers._White.medical_shop_1_msku_a.sku
        )

        response = self.report.request_json(request + offers_list + '&preferable-courier-delivery-day=2')
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI_DELIVERY_DAY"}})
        self.error_log.expect(code=3043)

    def test_incomplete_bucket(self):
        """
        Проверяем что при разрешении на подбор неполной медицинской корзины
        будет выбрана такая, у которой: наибольшее количество товаров по наименьшей цене.
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        for flag in [0, 1]:
            offers_list = (
                '&offers-list={medical_unique_id}:1;msku:{medical_unique_msku},'
                '{medical_common_id_1}:1;msku:{medical_common_msku_1},'
                '{medical_common_id_2}:1;msku:{medical_common_msku_2},'
                '&rearr-factors=market_enable_incomplete_bucket_selection={flag}'
            ).format(
                medical_unique_id=_Offers._White.medical_cpa_2_shop_4.ware_md5,
                medical_unique_msku=_Offers._White.medical_cpa_2_shop_4.sku,
                medical_common_id_1=_Offers._White.medical_cpa_1_shop_1.ware_md5,
                medical_common_msku_1=_Offers._White.medical_cpa_1_shop_1.sku,
                medical_common_id_2=_Offers._White.prescription_cpa_1_shop_1.ware_md5,
                medical_common_msku_2=_Offers._White.prescription_cpa_1_shop_1.sku,
                flag=flag,
            )

            response = self.report.request_json(request + offers_list)

            if flag == 1:
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "buckets": [
                                        {
                                            "shopId": _Shops._White.medical_1.fesh,
                                            "offers": [
                                                {"wareId": _Offers._White.medical_cpa_1_shop_1.waremd5},
                                                {"wareId": _Offers._White.prescription_cpa_1_shop_1.waremd5},
                                            ],
                                        }
                                    ]
                                }
                            ]
                        }
                    },
                    allow_different_len=False,
                )
            else:
                self.assertFragmentIn(
                    response,
                    {"search": {"results": [{"buckets": []}]}},
                    allow_different_len=False,
                )

    def test_multi_parcels_offer(self):
        """
        Проверяем что при разрешении на подбор множества медицинских посылок:
        - в случае отсутствия наличия полной посылки у одного из поставщиков
        - будет произведено дробление на подпосылки
        - допускается что поддпосылки могут не предложить всех запрошенных товаров
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={medical_id_1}:2;msku:{medical_msku_1},'
            '{medical_id_2}:2;msku:{medical_msku_2},'
            '{medical_id_3}:2;msku:{medical_msku_3},'
            '{medical_id_4}:2;msku:{medical_msku_4},'
            '{medical_id_5}:2;msku:{medical_msku_5}'
        ).format(
            medical_id_1=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            medical_msku_1=_Offers._White.medical_cpa_1_shop_1.sku,
            medical_id_2=_Offers._White.prescription_cpa_1_shop_1.ware_md5,
            medical_msku_2=_Offers._White.prescription_cpa_1_shop_1.sku,
            medical_id_3=_Offers._White.medical_cpa_2_shop_3.ware_md5,
            medical_msku_3=_Offers._White.medical_cpa_2_shop_3.sku,
            medical_id_4=_Offers._White.medical_cpa_2_shop_4.ware_md5,
            medical_msku_4=_Offers._White.medical_cpa_2_shop_4.sku,
            medical_id_5='unknown_waremd5',  # predefined unknown wareid
            medical_msku_5=_Mskus.unknown_id,  # predefined unknown msku
        )

        # Проверяем дробление на подпосылки
        response = self.report.request_json(
            request
            + offers_list
            + '&rearr-factors=market_enable_incomplete_bucket_selection=1'
            + '&rearr-factors=market_enable_multi_medical_parcels=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_3.fesh,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_cpa_2_shop_3.ware_md5},
                                        {"wareId": _Offers._White.prescription_cpa_1_shop_3.ware_md5},
                                        {"wareId": _Offers._White.medical_cpa_1_shop_3.ware_md5},
                                    ],
                                    "offersTotalPrice": {"value": "600"},
                                },
                                {
                                    "shopId": _Shops._White.medical_4.fesh,
                                    "offers": [{"wareId": _Offers._White.medical_cpa_2_shop_4.ware_md5}],
                                    "offersTotalPrice": {"value": "200"},
                                },
                            ]
                        }
                    ],
                    "total": 8,
                }
            },
            allow_different_len=False,
        )

        # С выключенным флагом дробление не происходит
        response = self.report.request_json(
            request
            + offers_list
            + '&rearr-factors=market_enable_incomplete_bucket_selection=1'
            + '&rearr-factors=market_enable_multi_medical_parcels=0'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_3.fesh,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_cpa_2_shop_3.ware_md5},
                                        {"wareId": _Offers._White.prescription_cpa_1_shop_3.ware_md5},
                                        {"wareId": _Offers._White.medical_cpa_1_shop_3.ware_md5},
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 8,
                }
            },
            allow_different_len=False,
        )

    def test_single_parcel_offer(self):
        """
        Проверяем что при разрешении на подбор множества медицинских посылок:
        - в случае наличия полной посылки у одного из поставщиков
        - не будет производиться дробление на подпосылки
        """

        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = (
            '&offers-list={medical_id_1}:1;msku:{medical_msku_1},'
            '{medical_id_2}:1;msku:{medical_msku_2},'
            '{medical_id_3}:1;msku:{medical_msku_3}'
        ).format(
            medical_id_1=_Offers._White.medical_cpa_1_shop_1.ware_md5,
            medical_msku_1=_Offers._White.medical_cpa_1_shop_1.sku,
            medical_id_2=_Offers._White.prescription_cpa_1_shop_1.ware_md5,
            medical_msku_2=_Offers._White.prescription_cpa_1_shop_1.sku,
            medical_id_3=_Offers._White.medical_cpa_2_shop_3.ware_md5,
            medical_msku_3=_Offers._White.medical_cpa_2_shop_3.sku,
        )

        # Имеетя поставщик с "полной" посылкой
        response = self.report.request_json(
            request
            + offers_list
            + '&rearr-factors=market_enable_incomplete_bucket_selection=1'
            + '&rearr-factors=market_enable_multi_medical_parcels=1'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "shopId": _Shops._White.medical_3.fesh,
                                    "offers": [
                                        {"wareId": _Offers._White.medical_cpa_2_shop_3.ware_md5},
                                        {"wareId": _Offers._White.prescription_cpa_1_shop_3.ware_md5},
                                        {"wareId": _Offers._White.medical_cpa_1_shop_3.ware_md5},
                                    ],
                                }
                            ]
                        }
                    ],
                    "total": 7,
                }
            },
            allow_different_len=False,
        )

    def test_combine_warehouse_priorities(self):
        """
        Проверяем, что при передаче различных приоритетов складов будет выбран лучший склад и оффер в соответствии с приоритетом.
        """

        for (gps, warehouse_id, ware_id) in [
            # "Стандартное" значение приоритетов складов для магазина; лучшими считаются оффера с первого склада
            (
                _GpsWarehouses.gps_1.location_str,
                _Warehouses._Blue.medical_express_1,
                _Mskus.blue_express_offer_1.waremd5,
            ),
            # Модифицированные приоритеты складов; лучшими считаются оффера со второго склада
            (
                _GpsWarehouses.gps_2.location_str,
                _Warehouses._Blue.medical_express_2,
                _Mskus.blue_express_offer_2.waremd5,
            ),
            # Модифицированные приоритеты складов; лучшими считаются оффера с третьего склада
            (
                _GpsWarehouses.gps_3.location_str,
                _Warehouses._Blue.medical_express_3,
                _Mskus.blue_express_offer_3.waremd5,
            ),
        ]:
            request = _Requests.gps.format(gps=gps)

            offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
                ware_id=_Mskus.blue_express_offer_1.waremd5, msku=_Mskus.blue_express_offer_1.sku
            )

            extra_flags = '&filter-express-delivery=1'

            response = self.report.request_json(request + offers_list + extra_flags)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "shopId": _Shops._Blue.medical_express_1.fesh,
                                        "IsMedicalParcel": True,
                                        "deliveryDayFrom": 0,
                                        "deliveryDayTo": 0,
                                        "offers": [
                                            {
                                                "count": 1,
                                                "marketSku": str(_Mskus.blue_express_offer_1.sku),
                                                "wareId": ware_id,
                                            }
                                        ],
                                        "warehouseId": warehouse_id,
                                    }
                                ]
                            }
                        ],
                        "total": 3,
                    }
                },
                allow_different_len=False,
            )

    def test_combine_warehouse_priorities_gps(self):
        """
        Проверяем, что при передаче различных приоритетов складов от комбинатора
        будет выбран лучший склад и оффер в соответствии с приоритетом.
        """

        for (gps, warehouse_id, ware_id) in [
            # "Стандартное" значение приоритетов складов для магазина; лучшими считаются оффера с первого склада
            (
                _RequestGps.location_a,
                _Warehouses._Blue.medical_express_1,
                _Mskus.blue_express_offer_1.waremd5,
            ),
            # Модифицированные приоритеты складов; лучшими считаются оффера со второго склада
            (
                _RequestGps.location_b,
                _Warehouses._Blue.medical_express_2,
                _Mskus.blue_express_offer_2.waremd5,
            ),
            # Модифицированные приоритеты складов; лучшими считаются оффера с третьего склада
            (
                _RequestGps.location_c,
                _Warehouses._Blue.medical_express_3,
                _Mskus.blue_express_offer_3.waremd5,
            ),
        ]:
            request = _Requests.gps.format(gps=gps)

            offers_list = ('&offers-list={ware_id}:1;msku:{msku}').format(
                ware_id=_Mskus.blue_express_offer_1.waremd5, msku=_Mskus.blue_express_offer_1.sku
            )

            extra_flags = '&filter-express-delivery=1'

            response = self.report.request_json(request + offers_list + extra_flags)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "shopId": _Shops._Blue.medical_express_1.fesh,
                                        "IsMedicalParcel": True,
                                        "deliveryDayFrom": 0,
                                        "deliveryDayTo": 0,
                                        "offers": [
                                            {
                                                "count": 1,
                                                "marketSku": str(_Mskus.blue_express_offer_1.sku),
                                                "wareId": ware_id,
                                            }
                                        ],
                                        "warehouseId": warehouse_id,
                                    }
                                ]
                            }
                        ],
                        "total": 3,
                    }
                },
                allow_different_len=False,
            )

    def test_prescription_management_system(self):
        """
        Проверяем, что при разрешении на доставку рецептурных препаратов на выдачу
        попадают посылки только от поставщика, подключенного к Электронному Рецепту.
        """
        request = _Requests.courier.format(not_prescription_drugs_delivery=1, prescription_drugs_delivery=1)

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}').format(
            ware_id_1=_Offers._White.prescription_cpa_1_shop_1.ware_md5,
            msku_1=_Offers._White.prescription_cpa_1_shop_1.sku,
        )

        extra_flags = '&rearr-factors=enable_medical_shop_prescription_delivery_filter=1' '&debug=1'

        response = self.report.request_json(request + offers_list + extra_flags)

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "buckets": [
                                {
                                    "offers": [
                                        {
                                            "reason": "ABSENCE_OF_HYPER_ID",
                                            "replacedId": _Offers._White.prescription_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                    "shopId": 0,
                                    "warehouseId": 0,
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "hasCourier": True,
                                    "offers": [
                                        {
                                            "marketSku": str(_Offers._White.prescription_cpa_1_shop_1.sku),
                                            "wareId": _Offers._White.prescription_cpa_1_shop_1.ware_md5,
                                        }
                                    ],
                                    "shopId": _Shops._White.medical_1.fesh,
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "hasCourier": True,
                                    "offers": [
                                        {
                                            "marketSku": str(_Offers._White.prescription_cpa_1_shop_2.sku),
                                            "wareId": _Offers._White.prescription_cpa_1_shop_2.ware_md5,
                                        }
                                    ],
                                    "shopId": _Shops._White.medical_2.fesh,
                                },
                                {
                                    "IsMedicalParcel": True,
                                    "hasCourier": True,
                                    "offers": [
                                        {
                                            "marketSku": str(_Offers._White.prescription_cpa_1_shop_3.sku),
                                            "wareId": _Offers._White.prescription_cpa_1_shop_3.ware_md5,
                                        }
                                    ],
                                    "shopId": _Shops._White.medical_3.fesh,
                                },
                            ]
                        }
                    ],
                    "total": 3,
                }
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "filters": {
                    "DELIVERY_SHOP_PRESCRIPTION_MANAGEMENT_SYSTEM": 1,
                }
            },
        )


if __name__ == '__main__':
    main()
