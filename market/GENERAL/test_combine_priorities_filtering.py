#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

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
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    ExpressSupplier,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
    TimeInfo,
    TimeIntervalByDay,
    TimeIntervalInfo,
)
from core.types.combinator import CombinatorGpsCoords, CombinatorExpressWarehouse
from core.types.offer import OfferDimensions
from core.testcase import TestCase, main


def make_mock_rearr(**kwds):
    suffix = 'parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1;market_enable_sins_offers_wizard=1'
    rearr = make_rearr(**kwds)
    if rearr != '':
        rearr += ';'
    return rearr + suffix


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


class _Rids:
    russia = 225
    moscow = 213


class _DeliveryServices:
    internal = 99


class _Gps:
    _lat = 15.1234
    _lon = 13.4321

    location_combinator = CombinatorGpsCoords(_lat, _lon)
    location_str = 'lat:{lat};lon:{lon}'.format(lat=_lat, lon=_lon)


class _Params:
    drugs_category_id = 15758037
    goods_category_id = 1039394


class _Categories:
    medical_express = 1
    goods_express = 2


class _Feshes:
    class _White:
        medical_express = 10
        goods_express = 20

    class _Blue:
        medical_express = 30
        goods_express = 40


class _Feeds:
    class _White:
        medical_express_1 = 100
        medical_express_2 = 200
        medical_express_3 = 300
        goods_express_1 = 400
        goods_express_2 = 500
        goods_express_3 = 600

    class _Blue:
        medical_express_1 = 700
        medical_express_2 = 800
        medical_express_3 = 900
        goods_express_1 = 1000
        goods_express_2 = 1100
        goods_express_3 = 1200


class _ClientIds:
    class _White:
        medical_express = 101
        goods_express = 401

    class _Blue:
        medical_express = 701
        goods_express = 1001


class _Warehouses:
    class _White:
        medical_express_1 = 1000
        medical_express_2 = 2000
        medical_express_3 = 3000
        goods_express_1 = 4000
        goods_express_2 = 5000
        goods_express_3 = 6000

    class _Blue:
        medical_express_1 = 7000
        medical_express_2 = 8000
        medical_express_3 = 9000
        goods_express_1 = 10000
        goods_express_2 = 11000
        goods_express_3 = 12000


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
            )

        medical_express_1 = create(
            fesh=_Feshes._White.medical_express,
            datafeed_id=_Feeds._White.medical_express_1,
            client_id=_ClientIds._White.medical_express,
            warehouse_id=_Warehouses._White.medical_express_1,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical express shop 1 (multi warehouses)',
        )

        medical_express_2 = create(
            fesh=_Feshes._White.medical_express,
            datafeed_id=_Feeds._White.medical_express_2,
            client_id=_ClientIds._White.medical_express,
            warehouse_id=_Warehouses._White.medical_express_2,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical express shop 2 (multi warehouses)',
        )

        medical_express_3 = create(
            fesh=_Feshes._White.medical_express,
            datafeed_id=_Feeds._White.medical_express_3,
            client_id=_ClientIds._White.medical_express,
            warehouse_id=_Warehouses._White.medical_express_3,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS medical express shop 3 (multi warehouses)',
        )

        goods_express_1 = create(
            fesh=_Feshes._White.goods_express,
            datafeed_id=_Feeds._White.goods_express_1,
            client_id=_ClientIds._White.goods_express,
            warehouse_id=_Warehouses._White.goods_express_1,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS goods express shop 1 (multi warehouses)',
        )

        goods_express_2 = create(
            fesh=_Feshes._White.goods_express,
            datafeed_id=_Feeds._White.goods_express_2,
            client_id=_ClientIds._White.goods_express,
            warehouse_id=_Warehouses._White.goods_express_2,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS goods express shop 2 (multi warehouses)',
        )

        goods_express_3 = create(
            fesh=_Feshes._White.goods_express,
            datafeed_id=_Feeds._White.goods_express_3,
            client_id=_ClientIds._White.goods_express,
            warehouse_id=_Warehouses._White.goods_express_3,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='White CPA CIS goods express shop 3 (multi warehouses)',
        )

        ALL = [
            medical_express_1,
            medical_express_2,
            medical_express_3,
            goods_express_1,
            goods_express_2,
            goods_express_3,
        ]

    class _Blue:
        def create(fesh, datafeed_id, client_id, warehouse_id, priority_region, name):
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
            )

        medical_express_1 = create(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express_1,
            client_id=_ClientIds._Blue.medical_express,
            warehouse_id=_Warehouses._Blue.medical_express_1,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical express shop 1 (multi warehouses)',
        )

        medical_express_2 = create(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express_2,
            client_id=_ClientIds._Blue.medical_express,
            warehouse_id=_Warehouses._Blue.medical_express_2,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical express shop 2 (multi warehouses)',
        )

        medical_express_3 = create(
            fesh=_Feshes._Blue.medical_express,
            datafeed_id=_Feeds._Blue.medical_express_3,
            client_id=_ClientIds._Blue.medical_express,
            warehouse_id=_Warehouses._Blue.medical_express_3,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS medical express shop 3 (multi warehouses)',
        )

        goods_express_1 = create(
            fesh=_Feshes._Blue.goods_express,
            datafeed_id=_Feeds._Blue.goods_express_1,
            client_id=_ClientIds._Blue.goods_express,
            warehouse_id=_Warehouses._Blue.goods_express_1,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS goods express shop 1 (multi warehouses)',
        )

        goods_express_2 = create(
            fesh=_Feshes._Blue.goods_express,
            datafeed_id=_Feeds._Blue.goods_express_2,
            client_id=_ClientIds._Blue.goods_express,
            warehouse_id=_Warehouses._Blue.goods_express_2,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS goods express shop 2 (multi warehouses)',
        )

        goods_express_3 = create(
            fesh=_Feshes._Blue.goods_express,
            datafeed_id=_Feeds._Blue.goods_express_3,
            client_id=_ClientIds._Blue.goods_express,
            warehouse_id=_Warehouses._Blue.goods_express_3,
            priority_region=_Rids.moscow,
            name='Blue CPA CIS goods express shop 3 (multi warehouses)',
        )

        ALL = [
            medical_express_1,
            medical_express_2,
            medical_express_3,
            goods_express_1,
            goods_express_2,
            goods_express_3,
        ]


class _Outlets:
    class _White:
        medical_id = 1010
        goods_id = 2020

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

        medical = create(fesh=_Feshes._White.medical_express, point_id=medical_id)
        goods = create(fesh=_Feshes._White.goods_express, point_id=goods_id)

        ALL = [medical, goods]

    class _Blue:
        medical_id = 3030
        goods_id = 4040

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

        medical = create(fesh=_Feshes._Blue.medical_express, point_id=medical_id)
        goods = create(fesh=_Feshes._Blue.goods_express, point_id=goods_id)

        ALL = [medical, goods]


class _Buckets:
    white_medical_express_id_1 = 10000
    white_medical_express_id_2 = 20000
    white_medical_express_id_3 = 30000

    white_goods_express_id_1 = 40000
    white_goods_express_id_2 = 50000
    white_goods_express_id_3 = 60000

    blue_medical_express_id_1 = 70000
    blue_medical_express_id_2 = 80000
    blue_medical_express_id_3 = 90000

    blue_goods_express_id_1 = 100000
    blue_goods_express_id_2 = 110000
    blue_goods_express_id_3 = 120000

    white_medical_pickup_id = 130000
    white_goods_pickup_id = 140000
    blue_medical_pickup_id = 150000
    blue_goods_pickup_id = 160000

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

    white_medical_pickup = create_pickup(
        white_medical_pickup_id,
        _Outlets._White.medical_id,
        _Shops._White.medical_express_1,
        [_DeliveryServices.internal],
    )
    white_goods_pickup = create_pickup(
        white_goods_pickup_id, _Outlets._White.goods_id, _Shops._White.goods_express_1, [_DeliveryServices.internal]
    )
    blue_medical_pickup = create_pickup(
        blue_medical_pickup_id, _Outlets._Blue.medical_id, _Shops._Blue.medical_express_1, [_DeliveryServices.internal]
    )
    blue_goods_pickup = create_pickup(
        blue_goods_pickup_id, _Outlets._Blue.goods_id, _Shops._Blue.goods_express_1, [_DeliveryServices.internal]
    )

    white_medical_express_1 = create_delivery(
        white_medical_express_id_1, _Shops._White.medical_express_1, [_DeliveryServices.internal]
    )
    white_medical_express_2 = create_delivery(
        white_medical_express_id_2, _Shops._White.medical_express_2, [_DeliveryServices.internal]
    )
    white_medical_express_3 = create_delivery(
        white_medical_express_id_3, _Shops._White.medical_express_3, [_DeliveryServices.internal]
    )

    white_goods_express_1 = create_delivery(
        white_goods_express_id_1, _Shops._White.goods_express_1, [_DeliveryServices.internal]
    )
    white_goods_express_2 = create_delivery(
        white_goods_express_id_2, _Shops._White.goods_express_2, [_DeliveryServices.internal]
    )
    white_goods_express_3 = create_delivery(
        white_goods_express_id_3, _Shops._White.goods_express_3, [_DeliveryServices.internal]
    )

    blue_medical_express_1 = create_delivery(
        bucket_id=blue_medical_express_id_1,
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

    blue_medical_express_2 = create_delivery(
        bucket_id=blue_medical_express_id_2,
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

    blue_medical_express_3 = create_delivery(
        bucket_id=blue_medical_express_id_3,
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

    blue_goods_express_1 = create_delivery(
        bucket_id=blue_goods_express_id_1,
        shop=_Shops._Blue.goods_express_1,
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

    blue_goods_express_2 = create_delivery(
        bucket_id=blue_goods_express_id_2,
        shop=_Shops._Blue.goods_express_2,
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

    blue_goods_express_3 = create_delivery(
        bucket_id=blue_goods_express_id_3,
        shop=_Shops._Blue.goods_express_3,
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

    DELIVERY = [
        white_medical_express_1,
        white_medical_express_2,
        white_medical_express_3,
        white_goods_express_1,
        white_goods_express_2,
        white_goods_express_3,
        blue_medical_express_1,
        blue_medical_express_2,
        blue_medical_express_3,
        blue_goods_express_1,
        blue_goods_express_2,
        blue_goods_express_3,
    ]

    PICKUP = [
        white_medical_pickup,
        white_goods_pickup,
        blue_medical_pickup,
        blue_goods_pickup,
    ]


class _Mskus:
    white_medical_id = 100000
    white_goods_id = 200000
    blue_medical_id = 300000
    blue_goods_id = 400000

    def create(title, sku, hyperid, blue_offers=None):
        return MarketSku(title=title, sku=sku, hyperid=hyperid, blue_offers=blue_offers)

    blue_medical_offer_1 = BlueOffer(
        waremd5='medical_b_expr_shop1_g',
        fesh=_Feshes._Blue.medical_express,
        feedid=_Feeds._Blue.medical_express_1,
        supplier_id=_ClientIds._Blue.medical_express,
        sku=blue_medical_id,
        hyperid=_Categories.medical_express,
        delivery_buckets=[_Buckets.blue_medical_express_1.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
        stock_store_count=10,
        is_medicine=True,
        is_express=True,
    )

    blue_medical_offer_2 = BlueOffer(
        waremd5='medical_b_expr_shop2_g',
        fesh=_Feshes._Blue.medical_express,
        feedid=_Feeds._Blue.medical_express_2,
        supplier_id=_ClientIds._Blue.medical_express,
        sku=blue_medical_id,
        hyperid=_Categories.medical_express,
        delivery_buckets=[_Buckets.blue_medical_express_2.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
        stock_store_count=10,
        is_medicine=True,
        is_express=True,
    )

    blue_medical_offer_3 = BlueOffer(
        waremd5='medical_b_expr_shop3_g',
        fesh=_Feshes._Blue.medical_express,
        feedid=_Feeds._Blue.medical_express_3,
        supplier_id=_ClientIds._Blue.medical_express,
        sku=blue_medical_id,
        hyperid=_Categories.medical_express,
        delivery_buckets=[_Buckets.blue_medical_express_3.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
        stock_store_count=10,
        is_medicine=True,
        is_express=True,
    )

    blue_goods_offer_1 = BlueOffer(
        waremd5='goods_b_expr_shop1___g',
        fesh=_Feshes._Blue.goods_express,
        feedid=_Feeds._Blue.goods_express_1,
        supplier_id=_ClientIds._Blue.goods_express,
        sku=blue_goods_id,
        hyperid=_Categories.goods_express,
        delivery_buckets=[_Buckets.blue_goods_express_1.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
        stock_store_count=10,
        is_express=True,
    )

    blue_goods_offer_2 = BlueOffer(
        waremd5='goods_b_expr_shop2___g',
        fesh=_Feshes._Blue.goods_express,
        feedid=_Feeds._Blue.goods_express_2,
        supplier_id=_ClientIds._Blue.goods_express,
        sku=blue_goods_id,
        hyperid=_Categories.goods_express,
        delivery_buckets=[_Buckets.blue_goods_express_2.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
        stock_store_count=10,
        is_express=True,
    )

    blue_goods_offer_3 = BlueOffer(
        waremd5='goods_b_expr_shop3___g',
        fesh=_Feshes._Blue.goods_express,
        feedid=_Feeds._Blue.goods_express_3,
        supplier_id=_ClientIds._Blue.goods_express,
        sku=blue_goods_id,
        hyperid=_Categories.goods_express,
        delivery_buckets=[_Buckets.blue_goods_express_3.bucket_id],
        price=10,
        weight=1,
        dimensions=OfferDimensions(length=3, width=3, height=3),
        delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
        stock_store_count=10,
        is_express=True,
    )

    white_medical_msku = create("White medical MSKU", white_medical_id, _Categories.medical_express)

    white_goods_msku = create("White goods MSKU", white_goods_id, _Categories.goods_express)

    blue_medical_msku = create(
        "Blue medical MSKU",
        blue_medical_id,
        _Categories.medical_express,
        [blue_medical_offer_1, blue_medical_offer_2, blue_medical_offer_3],
    )

    blue_goods_msku = create(
        "Blue goods MSKU",
        blue_goods_id,
        _Categories.goods_express,
        [blue_goods_offer_1, blue_goods_offer_2, blue_goods_offer_3],
    )

    ALL = [white_medical_msku, white_goods_msku, blue_medical_msku, blue_goods_msku]


class _Models:
    def create(hid, hyperid):
        return Model(hid=hid, hyperid=hyperid)

    medical_express = create(_Params.drugs_category_id, _Categories.medical_express)
    goods_express = create(_Params.goods_category_id, _Categories.goods_express)

    ALL = [medical_express, goods_express]


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
            is_medicine=False,
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
            )

        medical_express_offer_1 = create(
            waremd5='medical_w_expr_shop1_g',
            shop=_Shops._White.medical_express_1,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.white_medical_msku,
            delivery_buckets=[_Buckets.white_medical_express_1.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
            cpa=Offer.CPA_REAL,
            is_express=True,
            is_medicine=True,
        )

        medical_express_offer_2 = create(
            waremd5='medical_w_expr_shop2_g',
            shop=_Shops._White.medical_express_2,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.white_medical_msku,
            delivery_buckets=[_Buckets.white_medical_express_2.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
            cpa=Offer.CPA_REAL,
            is_express=True,
            is_medicine=True,
        )

        medical_express_offer_3 = create(
            waremd5='medical_w_expr_shop3_g',
            shop=_Shops._White.medical_express_3,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.white_medical_msku,
            delivery_buckets=[_Buckets.white_medical_express_3.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
            cpa=Offer.CPA_REAL,
            is_express=True,
            is_medicine=True,
        )

        goods_express_offer_1 = create(
            waremd5='goods_w_expr_shop1___g',
            shop=_Shops._White.goods_express_1,
            supplier_id=_ClientIds._White.goods_express,
            msku=_Mskus.white_goods_msku,
            delivery_buckets=[_Buckets.white_goods_express_1.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
            cpa=Offer.CPA_REAL,
            is_express=True,
        )

        goods_express_offer_2 = create(
            waremd5='goods_w_expr_shop2___g',
            shop=_Shops._White.goods_express_2,
            supplier_id=_ClientIds._White.goods_express,
            msku=_Mskus.white_goods_msku,
            delivery_buckets=[_Buckets.white_goods_express_2.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
            cpa=Offer.CPA_REAL,
            is_express=True,
        )

        goods_express_offer_3 = create(
            waremd5='goods_w_expr_shop3___g',
            shop=_Shops._White.goods_express_3,
            supplier_id=_ClientIds._White.goods_express,
            msku=_Mskus.white_goods_msku,
            delivery_buckets=[_Buckets.white_goods_express_3.bucket_id],
            price=20,
            weight=1,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=[DeliveryOption(price=100, day_from=0, day_to=0)],
            cpa=Offer.CPA_REAL,
            is_express=True,
        )

        ALL = [
            medical_express_offer_1,
            medical_express_offer_2,
            medical_express_offer_3,
            goods_express_offer_1,
            goods_express_offer_2,
            goods_express_offer_3,
        ]


class _CombinatorExpressWarehouse:
    """
    Подготовленная информация о складах для combinator'а.
    """

    white_medical_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._White.medical_express_1,
        zone_id=1,
        business_id=_Feshes._White.medical_express,
        priority=1,
    )
    white_medical_2 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._White.medical_express_2,
        zone_id=1,
        business_id=_Feshes._White.medical_express,
        priority=2,
    )
    white_medical_3 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._White.medical_express_3,
        zone_id=1,
        business_id=_Feshes._White.medical_express,
        priority=3,
    )

    white_goods_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._White.goods_express_1, zone_id=1, business_id=_Feshes._White.goods_express, priority=1
    )
    white_goods_2 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._White.goods_express_2, zone_id=1, business_id=_Feshes._White.goods_express, priority=2
    )
    white_goods_3 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._White.goods_express_3, zone_id=1, business_id=_Feshes._White.goods_express, priority=3
    )

    blue_medical_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_1,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express,
        priority=1,
    )
    blue_medical_2 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_2,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express,
        priority=2,
    )
    blue_medical_3 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.medical_express_3,
        zone_id=1,
        business_id=_Feshes._Blue.medical_express,
        priority=3,
    )

    blue_goods_1 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.goods_express_1, zone_id=1, business_id=_Feshes._Blue.goods_express, priority=1
    )
    blue_goods_2 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.goods_express_2, zone_id=1, business_id=_Feshes._Blue.goods_express, priority=2
    )
    blue_goods_3 = CombinatorExpressWarehouse(
        warehouse_id=_Warehouses._Blue.goods_express_3, zone_id=1, business_id=_Feshes._Blue.goods_express, priority=3
    )

    ALL = [
        white_medical_1,
        white_medical_2,
        white_medical_3,
        white_goods_1,
        white_goods_2,
        white_goods_3,
        blue_medical_1,
        blue_medical_2,
        blue_medical_3,
        blue_goods_1,
        blue_goods_2,
        blue_goods_3,
    ]


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
    def create(shop, working_schedule):
        return ExpressSupplier(
            feed_id=shop.datafeed_id,
            supplier_id=shop.fesh,
            warehouse_id=shop.warehouse_id,
            working_schedule=working_schedule,
        )

    # Склад работает круглосуточно
    white_medical_express_1 = create(_Shops._White.medical_express_1, _WorkingShedules.round_the_clock)
    # Склад работает с 8:00 до 22:00 по будним дням
    white_medical_express_2 = create(_Shops._White.medical_express_2, _WorkingShedules.workdays)
    # Склад работает с 10:00 до 18:00 ежедневно
    white_medical_express_3 = create(_Shops._White.medical_express_3, _WorkingShedules.everyday)

    # Склад работает круглосуточно
    white_goods_express_1 = create(_Shops._White.goods_express_1, _WorkingShedules.round_the_clock)
    # Склад работает с 8:00 до 22:00 по будним дням
    white_goods_express_2 = create(_Shops._White.goods_express_2, _WorkingShedules.workdays)
    # Склад работает с 10:00 до 18:00 ежедневно
    white_goods_express_3 = create(_Shops._White.goods_express_3, _WorkingShedules.everyday)

    # Склад работает круглосуточно
    blue_medical_express_1 = create(_Shops._Blue.medical_express_1, _WorkingShedules.round_the_clock)
    # Склад работает с 8:00 до 22:00 по будним дням
    blue_medical_express_2 = create(_Shops._Blue.medical_express_2, _WorkingShedules.workdays)
    # Склад работает с 10:00 до 18:00 ежедневно
    blue_medical_express_3 = create(_Shops._Blue.medical_express_3, _WorkingShedules.everyday)

    # Склад работает круглосуточно
    blue_goods_express_1 = create(_Shops._Blue.goods_express_1, _WorkingShedules.round_the_clock)
    # Склад работает с 8:00 до 22:00 по будним дням
    blue_goods_express_2 = create(_Shops._Blue.goods_express_2, _WorkingShedules.workdays)
    # Склад работает с 10:00 до 18:00 ежедневно
    blue_goods_express_3 = create(_Shops._Blue.goods_express_3, _WorkingShedules.everyday)

    ALL = [
        white_medical_express_1,
        white_medical_express_2,
        white_medical_express_3,
        white_goods_express_1,
        white_goods_express_2,
        white_goods_express_3,
        blue_medical_express_1,
        blue_medical_express_2,
        blue_medical_express_3,
        blue_goods_express_1,
        blue_goods_express_2,
        blue_goods_express_3,
    ]


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

    blue_medical_express_1 = create(_Warehouses._Blue.medical_express_1, min_days=0, max_days=0)
    blue_medical_express_2 = create(_Warehouses._Blue.medical_express_2, min_days=0, max_days=0)
    blue_medical_express_3 = create(_Warehouses._Blue.medical_express_3, min_days=0, max_days=0)

    blue_goods_express_1 = create(_Warehouses._Blue.goods_express_1, min_days=0, max_days=0)
    blue_goods_express_2 = create(_Warehouses._Blue.goods_express_2, min_days=0, max_days=0)
    blue_goods_express_3 = create(_Warehouses._Blue.goods_express_3, min_days=0, max_days=0)

    ALL = [
        blue_medical_express_1,
        blue_medical_express_2,
        blue_medical_express_3,
        blue_goods_express_1,
        blue_goods_express_2,
        blue_goods_express_3,
    ]


class _DynamicDeliveryServiceInfo:
    def create(name):
        return DynamicDeliveryServiceInfo(
            id=_DeliveryServices.internal,
            name=name,
            region_to_region_info=[
                DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
            ],
        )

    white_medical_express_1 = create('white_medical_express_1_delivery_service')
    white_medical_express_2 = create('white_medical_express_2_delivery_service')
    white_medical_express_3 = create('white_medical_express_3_delivery_service')

    white_goods_express_1 = create('white_goods_express_1_delivery_service')
    white_goods_express_2 = create('white_goods_express_2_delivery_service')
    white_goods_express_3 = create('white_goods_express_3_delivery_service')

    blue_medical_express_1 = create('blue_medical_express_1_delivery_service')
    blue_medical_express_2 = create('blue_medical_express_2_delivery_service')
    blue_medical_express_3 = create('blue_medical_express_3_delivery_service')

    blue_goods_express_1 = create('blue_goods_express_1_delivery_service')
    blue_goods_express_2 = create('blue_goods_express_2_delivery_service')
    blue_goods_express_3 = create('blue_goods_express_3_delivery_service')

    ALL = [
        white_medical_express_1,
        white_medical_express_2,
        white_medical_express_3,
        white_goods_express_1,
        white_goods_express_2,
        white_goods_express_3,
        blue_medical_express_1,
        blue_medical_express_2,
        blue_medical_express_3,
        blue_goods_express_1,
        blue_goods_express_2,
        blue_goods_express_3,
    ]


class _DynamicWarehouseAndDeliveryServiceInfo:
    def create(warehouse_id):
        return DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=warehouse_id,
            delivery_service_id=_DeliveryServices.internal,
            operation_time=0,
            date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=_Rids.russia)],
        )

    white_medical_express_1 = create(_Warehouses._White.medical_express_1)
    white_medical_express_2 = create(_Warehouses._White.medical_express_2)
    white_medical_express_3 = create(_Warehouses._White.medical_express_3)

    white_goods_express_1 = create(_Warehouses._White.goods_express_1)
    white_goods_express_2 = create(_Warehouses._White.goods_express_2)
    white_goods_express_3 = create(_Warehouses._White.goods_express_3)

    blue_medical_express_1 = create(_Warehouses._Blue.medical_express_1)
    blue_medical_express_2 = create(_Warehouses._Blue.medical_express_2)
    blue_medical_express_3 = create(_Warehouses._Blue.medical_express_3)

    blue_goods_express_1 = create(_Warehouses._Blue.goods_express_1)
    blue_goods_express_2 = create(_Warehouses._Blue.goods_express_2)
    blue_goods_express_3 = create(_Warehouses._Blue.goods_express_3)

    ALL = [
        white_medical_express_1,
        white_medical_express_2,
        white_medical_express_3,
        white_goods_express_1,
        white_goods_express_2,
        white_goods_express_3,
        blue_medical_express_1,
        blue_medical_express_2,
        blue_medical_express_3,
        blue_goods_express_1,
        blue_goods_express_2,
        blue_goods_express_3,
    ]


class _DynamicWarehouseInfo:
    def create(warehouse_id, is_express, working_schedule):
        return DynamicWarehouseInfo(
            id=warehouse_id,
            home_region=_Rids.moscow,
            holidays_days_set_key=2,
            is_express=is_express,
            shipment_schedule=working_schedule,
        )

    def create_rel(warehouse_id):
        return DynamicWarehouseToWarehouseInfo(warehouse_from=warehouse_id, warehouse_to=warehouse_id)

    white_medical_express_1 = create(
        warehouse_id=_Warehouses._White.medical_express_1,
        is_express=True,
        working_schedule=_WorkingShedules.round_the_clock_intervals,
    )
    white_medical_express_1_rel = create_rel(warehouse_id=_Warehouses._White.medical_express_1)

    white_medical_express_2 = create(
        warehouse_id=_Warehouses._White.medical_express_2,
        is_express=True,
        working_schedule=_WorkingShedules.workdays_intervals,
    )
    white_medical_express_2_rel = create_rel(
        warehouse_id=_Warehouses._White.medical_express_2,
    )

    white_medical_express_3 = create(
        warehouse_id=_Warehouses._White.medical_express_3,
        is_express=True,
        working_schedule=_WorkingShedules.everyday_intervals,
    )
    white_medical_express_3_rel = create_rel(
        warehouse_id=_Warehouses._White.medical_express_3,
    )

    white_goods_express_1 = create(
        warehouse_id=_Warehouses._White.goods_express_1,
        is_express=True,
        working_schedule=_WorkingShedules.round_the_clock_intervals,
    )
    white_goods_express_1_rel = create_rel(
        warehouse_id=_Warehouses._White.goods_express_1,
    )

    white_goods_express_2 = create(
        warehouse_id=_Warehouses._White.goods_express_2,
        is_express=True,
        working_schedule=_WorkingShedules.workdays_intervals,
    )
    white_goods_express_2_rel = create_rel(
        warehouse_id=_Warehouses._White.goods_express_2,
    )

    white_goods_express_3 = create(
        warehouse_id=_Warehouses._White.goods_express_3,
        is_express=True,
        working_schedule=_WorkingShedules.everyday_intervals,
    )
    white_goods_express_3_rel = create_rel(
        warehouse_id=_Warehouses._White.goods_express_3,
    )

    blue_medical_express_1 = create(
        warehouse_id=_Warehouses._Blue.medical_express_1,
        is_express=True,
        working_schedule=_WorkingShedules.round_the_clock_intervals,
    )
    blue_medical_express_1_rel = create_rel(
        warehouse_id=_Warehouses._Blue.medical_express_1,
    )

    blue_medical_express_2 = create(
        warehouse_id=_Warehouses._Blue.medical_express_2,
        is_express=True,
        working_schedule=_WorkingShedules.workdays_intervals,
    )
    blue_medical_express_2_rel = create_rel(
        warehouse_id=_Warehouses._Blue.medical_express_2,
    )

    blue_medical_express_3 = create(
        warehouse_id=_Warehouses._Blue.medical_express_3,
        is_express=True,
        working_schedule=_WorkingShedules.everyday_intervals,
    )
    blue_medical_express_3_rel = create_rel(
        warehouse_id=_Warehouses._Blue.medical_express_3,
    )

    blue_goods_express_1 = create(
        warehouse_id=_Warehouses._Blue.goods_express_1,
        is_express=True,
        working_schedule=_WorkingShedules.round_the_clock_intervals,
    )
    blue_goods_express_1_rel = create_rel(
        warehouse_id=_Warehouses._Blue.goods_express_1,
    )

    blue_goods_express_2 = create(
        warehouse_id=_Warehouses._Blue.goods_express_2,
        is_express=True,
        working_schedule=_WorkingShedules.workdays_intervals,
    )
    blue_goods_express_2_rel = create_rel(
        warehouse_id=_Warehouses._Blue.goods_express_2,
    )

    blue_goods_express_3 = create(
        warehouse_id=_Warehouses._Blue.goods_express_3,
        is_express=True,
        working_schedule=_WorkingShedules.everyday_intervals,
    )
    blue_goods_express_3_rel = create_rel(
        warehouse_id=_Warehouses._Blue.goods_express_3,
    )

    ALL = [
        white_medical_express_1,
        white_medical_express_2,
        white_medical_express_3,
        white_goods_express_1,
        white_goods_express_2,
        white_goods_express_3,
        blue_medical_express_1,
        blue_medical_express_2,
        blue_medical_express_3,
        blue_goods_express_1,
        blue_goods_express_2,
        blue_goods_express_3,
        white_medical_express_1_rel,
        white_medical_express_2_rel,
        white_medical_express_3_rel,
        white_goods_express_1_rel,
        white_goods_express_2_rel,
        white_goods_express_3_rel,
        blue_medical_express_1_rel,
        blue_medical_express_2_rel,
        blue_medical_express_3_rel,
        blue_goods_express_1_rel,
        blue_goods_express_2_rel,
        blue_goods_express_3_rel,
    ]


class _Requests:
    shopping_list = 'place=combine' '&delivery-type=courier' '&pp=18' '&rids=213' '&gps=' + _Gps.location_str

    shopping_list_gps = 'place=combine' '&delivery-type=courier' '&pp=18' '&rids=213' '&gps=' + _Gps.location_str

    default = 'place=combine' '&pp=18' '&rids=213' '&gps=' + _Gps.location_str

    default_gps = 'place=combine' '&pp=18' '&rids=213' '&gps=' + _Gps.location_str


def get_rearr_flags(
    market_use_warehouse_priorities_filtering=None,
    market_use_global_warehouse_priorities_filtering=None,
):
    flags = {
        'market_disable_shopping_list_multiple_warehouses': 0,
        'market_show_express_out_of_working_hours': 1,
        'market_not_prescription_drugs_delivery': 1,
    }

    if market_use_warehouse_priorities_filtering is not None:
        flags['market_use_warehouse_priorities_filtering'] = market_use_warehouse_priorities_filtering
    if market_use_global_warehouse_priorities_filtering is not None:
        flags['market_use_global_warehouse_priorities_filtering'] = market_use_global_warehouse_priorities_filtering

    return make_rearr(**flags)


class T(TestCase):
    """
    Набор тестов для проекта "Ранжирование офферов в зависимости от расстояния до склада в рамках одного Бизнеса".
    Краткое описание:
    - Отфильтровать медицинские посылки одного бизнеса в зависимости от возврата базового поиска - флаг
      market_disable_shopping_list_multiple_warehouses
    - Отфильтровать медицинские посылки одного бизнеса в зависимости от приоритетов складов - флаг
      market_use_warehouse_priorities_filtering
    - Отфильтровать все посылки одного бизнеса в зависимости от приоритетов складов - флаг
      market_use_global_warehouse_priorities_filtering
    См.: https://st.yandex-team.ru/MARKETPROJECT-7671
    См.: https://st.yandex-team.ru/MARKETOUT-44625
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
        cls.index.shops += _Shops._White.ALL
        cls.index.shops += _Shops._Blue.ALL

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += _Mskus.ALL

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += _Offers._White.ALL

    @classmethod
    def prepare_models(cls):
        cls.index.models += _Models.ALL

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += _Buckets.DELIVERY

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += _Buckets.PICKUP

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += _Outlets._White.ALL
        cls.index.outlets += _Outlets._Blue.ALL

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        cls.dynamic.lms += _DynamicDeliveryServiceInfo.ALL
        cls.dynamic.lms += _DynamicWarehouseAndDeliveryServiceInfo.ALL
        cls.dynamic.lms += _DynamicWarehouseInfo.ALL

    @classmethod
    def prepare_express_partners(cls):
        cls.index.express_partners.suppliers += _ExpressSuppliers.ALL

    @classmethod
    def prepare_blue_respond(cls):
        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_express_1
        ).respond([_Buckets.blue_medical_express_id_1], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_express_2
        ).respond([_Buckets.blue_medical_express_id_2], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.medical_express_3
        ).respond([_Buckets.blue_medical_express_id_3], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.goods_express_1
        ).respond([_Buckets.blue_goods_express_id_1], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.goods_express_2
        ).respond([_Buckets.blue_goods_express_id_2], [], [])

        cls.delivery_calc.on_request_offer_buckets(
            weight=1, height=3, length=3, width=3, warehouse_id=_Warehouses._Blue.goods_express_3
        ).respond([_Buckets.blue_goods_express_id_3], [], [])

    @classmethod
    def prepare_nordstream(cls):
        # Sets up delivery schedule response for blue offers
        cls.dynamic.nordstream += _DynamicWarehouseDelivery.ALL

    @classmethod
    def prepare_combinator(cls):
        for (use_warehouse_priorities_filtering, use_global_warehouse_priorities_filtering) in [
            (0, 0),
            (0, 1),
            (1, 0),
            (1, 1),
        ]:
            rearr_factors = make_mock_rearr(
                market_not_prescription_drugs_delivery=1,
                market_use_warehouse_priorities_filtering=use_warehouse_priorities_filtering,
                market_use_global_warehouse_priorities_filtering=use_global_warehouse_priorities_filtering,
                market_disable_shopping_list_multiple_warehouses=0,
                market_show_express_out_of_working_hours=1,
            )

            cls.combinatorExpress.on_express_warehouses_request(
                region_id=_Rids.moscow,
                gps_coords=_Gps.location_combinator,
                rear_factors=rearr_factors,
            ).respond_with_express_warehouses(_CombinatorExpressWarehouse.ALL)

    def test_white_blue_replacement_without_warehouse_priority(self):
        """
        Проверяем стандартное поведение при отсутствии любого рода замен.
        Корзина содержит как синие, так и белые оффера.
        """
        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1},'
            '{ware_id_2}:1;msku:{msku_2},'
            '{ware_id_3}:1;msku:{msku_3},'
            '{ware_id_4}:1;msku:{msku_4}'
        ).format(
            ware_id_1=_Offers._White.medical_express_offer_3.waremd5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
            ware_id_2=_Offers._White.goods_express_offer_3.waremd5,
            msku_2=_Offers._White.goods_express_offer_3.sku,
            ware_id_3=_Mskus.blue_medical_offer_3.waremd5,
            msku_3=_Mskus.blue_medical_offer_3.sku,
            ware_id_4=_Mskus.blue_goods_offer_3.waremd5,
            msku_4=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=0,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_3,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_white_blue_replacement_with_warehouse_priority(self):
        """
        Проверяем подмены синих офферов при выставленном флаге market_use_global_warehouse_priorities_filtering=1.
        Оффера должны подмениться теми, которые находятся на складах с лучшим значением приоритета.
        В корзине находятся как синие, так и белые оффера. Для белых офферов подмены не производятся.
        """
        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1}'
            ',{ware_id_2}:1;msku:{msku_2}'
            ',{ware_id_3}:1;msku:{msku_3}'
            ',{ware_id_4}:1;msku:{msku_4}'
        ).format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
            ware_id_3=_Offers._White.medical_express_offer_3.ware_md5,
            msku_3=_Offers._White.medical_express_offer_3.sku,
            ware_id_4=_Offers._White.goods_express_offer_3.ware_md5,
            msku_4=_Offers._White.goods_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_1,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_with_warehouse_priority(self):
        """
        Проверяем подмены синих офферов при выставленном флаге market_use_global_warehouse_priorities_filtering=1.
        Оффера должны подмениться теми, которые находятся на складах с лучшим значением приоритета.
        В корзине находятся только синие оффера.
        """
        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_1,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_without_warehouse_priority(self):
        """
        Проверяем отсутствие подмен синих офферов при отсутствии флага market_use_global_warehouse_priorities_filtering=1.
        Оффера равнозначны между собой и повода для подмены нет, так как приоритет складов не учитывается.
        В корзине находятся только синие оффера.
        """
        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=0,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_3,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_white_replacement_with_warehouse_priority(self):
        """
        Проверяем отсутствие подмен белых офферов при выставленном флаге market_use_global_warehouse_priorities_filtering=1.
        В корзине находятся только белые оффера. Для белых офферов подмены не производятся.
        """
        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Offers._White.medical_express_offer_3.ware_md5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
            ware_id_2=_Offers._White.goods_express_offer_3.ware_md5,
            msku_2=_Offers._White.goods_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_white_replacement_without_warehouse_priority(self):
        """
        Проверяем отсутствие подмен белых офферов при отсутствии флага market_use_global_warehouse_priorities_filtering=1.
        В корзине находятся только белые оффера. Для белых офферов подмены не производятся.
        """
        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Offers._White.medical_express_offer_3.ware_md5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
            ware_id_2=_Offers._White.goods_express_offer_3.ware_md5,
            msku_2=_Offers._White.goods_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=0,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shopping_list_white_medicine_global_priority_filtering(self):
        """
        Проверяем, что подмены под флагом market_use_global_warehouse_priorities_filtering=1
        не влияют на подмены белой медицины в рамках покупки списком под выставленным
        флагом market_use_warehouse_priorities_filtering=1.
        """
        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}').format(
            ware_id_1=_Offers._White.medical_express_offer_3.ware_md5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=1,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.shopping_list + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "IsMedicalParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_1.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                        "warehouseId": _Warehouses._White.medical_express_1,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shopping_list_blue_medicine_global_priority_filtering(self):
        """
        Проверяем, что подмены под флагом market_use_global_warehouse_priorities_filtering=1
        не влияют на подмены синей медицины в рамках покупки списком под выставленным
        флагом market_use_warehouse_priorities_filtering=1.
        """
        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}').format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=1,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.shopping_list + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "IsMedicalParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_with_benefits_without_warehouse_priorities(self):
        """
        Проверяем, что без флага market_use_global_warehouse_priorities_filtering=1
        в обычном запросе в place=combine не происходит подмены оффера, если для него
        указаны benefit'ы.
        """
        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1};benefit:faster' ',{ware_id_2}:1;msku:{msku_2};benefit:gift'
        ).format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = '&rearr-factors=' + get_rearr_flags() + '&filter-express-delivery=1'

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_3,
                        "currentRestrictionsForReplacement": ["EXCLUDE_MULTIOFFER_IN_CONSOLIDATION"],
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                        "currentRestrictionsForReplacement": ["EXCLUDE_MULTIOFFER_IN_CONSOLIDATION"],
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_with_benefits_with_global_warehouse_priorities(self):
        """
        Проверяем, что с флагом market_use_global_warehouse_priorities_filtering=1
        в обычном запросе в place=combine при указании benefit'ов подмены происходят
        только для значения faster.
        """
        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1};benefit:faster' ',{ware_id_2}:1;msku:{msku_2};benefit:gift'
        ).format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                        "currentRestrictionsForReplacement": [],  # запрет на подмены отсутствует
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                        "currentRestrictionsForReplacement": ["EXCLUDE_MULTIOFFER_IN_CONSOLIDATION"],
                    },
                ]
            },
            allow_different_len=False,
        )

    # секция тестов с отправкой gps вместо express-warehouses

    def test_white_blue_replacement_without_warehouse_priority_gps(self):
        """
        Проверяем стандартное поведение при отсутствии любого рода замен.
        Корзина содержит как синие, так и белые оффера.
        """
        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1},'
            '{ware_id_2}:1;msku:{msku_2},'
            '{ware_id_3}:1;msku:{msku_3},'
            '{ware_id_4}:1;msku:{msku_4}'
        ).format(
            ware_id_1=_Offers._White.medical_express_offer_3.waremd5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
            ware_id_2=_Offers._White.goods_express_offer_3.waremd5,
            msku_2=_Offers._White.goods_express_offer_3.sku,
            ware_id_3=_Mskus.blue_medical_offer_3.waremd5,
            msku_3=_Mskus.blue_medical_offer_3.sku,
            ware_id_4=_Mskus.blue_goods_offer_3.waremd5,
            msku_4=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=0,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default_gps + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_3,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_white_blue_replacement_with_warehouse_priority_gps(self):
        """
        Проверяем подмены синих офферов при выставленном флаге market_use_global_warehouse_priorities_filtering=1.
        Оффера должны подмениться теми, которые находятся на складах с лучшим значением приоритета.
        В корзине находятся как синие, так и белые оффера. Для белых офферов подмены не производятся.
        """
        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1}'
            ',{ware_id_2}:1;msku:{msku_2}'
            ',{ware_id_3}:1;msku:{msku_3}'
            ',{ware_id_4}:1;msku:{msku_4}'
        ).format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
            ware_id_3=_Offers._White.medical_express_offer_3.ware_md5,
            msku_3=_Offers._White.medical_express_offer_3.sku,
            ware_id_4=_Offers._White.goods_express_offer_3.ware_md5,
            msku_4=_Offers._White.goods_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(_Requests.default_gps + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_1,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_with_warehouse_priority_gps(self):
        """
        Проверяем подмены синих офферов при выставленном флаге market_use_global_warehouse_priorities_filtering=1.
        Оффера должны подмениться теми, которые находятся на складах с лучшим значением приоритета.
        В корзине находятся только синие оффера.
        """
        request = _Requests.default_gps

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_1,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_without_warehouse_priority_gps(self):
        """
        Проверяем отсутствие подмен синих офферов при отсутствии флага market_use_global_warehouse_priorities_filtering=1.
        Оффера равнозначны между собой и повода для подмены нет, так как приоритет складов не учитывается.
        В корзине находятся только синие оффера.
        """
        request = _Requests.default_gps

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=0,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_3,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_white_replacement_with_warehouse_priority_gps(self):
        """
        Проверяем отсутствие подмен белых офферов при выставленном флаге market_use_global_warehouse_priorities_filtering=1.
        В корзине находятся только белые оффера. Для белых офферов подмены не производятся.
        """
        request = _Requests.default_gps

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Offers._White.medical_express_offer_3.ware_md5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
            ware_id_2=_Offers._White.goods_express_offer_3.ware_md5,
            msku_2=_Offers._White.goods_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_white_replacement_without_warehouse_priority_gps(self):
        """
        Проверяем отсутствие подмен белых офферов при отсутствии флага market_use_global_warehouse_priorities_filtering=1.
        В корзине находятся только белые оффера. Для белых офферов подмены не производятся.
        """
        request = _Requests.default_gps

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}' ',{ware_id_2}:1;msku:{msku_2}').format(
            ware_id_1=_Offers._White.medical_express_offer_3.ware_md5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
            ware_id_2=_Offers._White.goods_express_offer_3.ware_md5,
            msku_2=_Offers._White.goods_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=0,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Offers._White.goods_express_offer_3.waremd5}],
                        "shopId": _Feshes._White.goods_express,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shopping_list_white_medicine_global_priority_filtering_gps(self):
        """
        Проверяем, что подмены под флагом market_use_global_warehouse_priorities_filtering=1
        не влияют на подмены белой медицины в рамках покупки списком под выставленным
        флагом market_use_warehouse_priorities_filtering=1.
        """
        request = _Requests.shopping_list_gps

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}').format(
            ware_id_1=_Offers._White.medical_express_offer_3.ware_md5,
            msku_1=_Offers._White.medical_express_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=1,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "IsMedicalParcel": True,
                        "offers": [{"wareId": _Offers._White.medical_express_offer_1.waremd5}],
                        "shopId": _Feshes._White.medical_express,
                        "warehouseId": _Warehouses._White.medical_express_1,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_shopping_list_blue_medicine_global_priority_filtering_gps(self):
        """
        Проверяем, что подмены под флагом market_use_global_warehouse_priorities_filtering=1
        не влияют на подмены синей медицины в рамках покупки списком под выставленным
        флагом market_use_warehouse_priorities_filtering=1.
        """
        request = _Requests.shopping_list_gps

        offers_list = ('&offers-list={ware_id_1}:1;msku:{msku_1}').format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=1,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "IsMedicalParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_with_benefits_without_warehouse_priorities_gps(self):
        """
        Проверяем, что без флага market_use_global_warehouse_priorities_filtering=1
        в обычном запросе в place=combine не происходит подмены оффера, если для него
        указаны benefit'ы.
        """
        request = _Requests.default_gps

        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1};benefit:faster' ',{ware_id_2}:1;msku:{msku_2};benefit:gift'
        ).format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = '&rearr-factors=' + get_rearr_flags() + '&filter-express-delivery=1'

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_3,
                        "currentRestrictionsForReplacement": ["EXCLUDE_MULTIOFFER_IN_CONSOLIDATION"],
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                        "currentRestrictionsForReplacement": ["EXCLUDE_MULTIOFFER_IN_CONSOLIDATION"],
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_blue_replacement_with_benefits_with_global_warehouse_priorities_gps(self):
        """
        Проверяем, что с флагом market_use_global_warehouse_priorities_filtering=1
        в обычном запросе в place=combine при указании benefit'ов подмены происходят
        только для значения faster.
        """
        request = _Requests.default_gps

        offers_list = (
            '&offers-list={ware_id_1}:1;msku:{msku_1};benefit:faster' ',{ware_id_2}:1;msku:{msku_2};benefit:gift'
        ).format(
            ware_id_1=_Mskus.blue_medical_offer_3.waremd5,
            msku_1=_Mskus.blue_medical_offer_3.sku,
            ware_id_2=_Mskus.blue_goods_offer_3.waremd5,
            msku_2=_Mskus.blue_goods_offer_3.sku,
        )

        extra_flags = (
            '&rearr-factors='
            + get_rearr_flags(
                market_use_warehouse_priorities_filtering=0,
                market_use_global_warehouse_priorities_filtering=1,
            )
            + '&filter-express-delivery=1'
        )

        response = self.report.request_json(request + offers_list + extra_flags)
        self.assertFragmentIn(
            response,
            {
                "buckets": [
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_medical_offer_1.waremd5}],
                        "warehouseId": _Warehouses._Blue.medical_express_1,
                        "currentRestrictionsForReplacement": [],  # запрет на подмены отсутствует
                    },
                    {
                        "IsExpressParcel": True,
                        "offers": [{"wareId": _Mskus.blue_goods_offer_3.waremd5}],
                        "warehouseId": _Warehouses._Blue.goods_express_3,
                        "currentRestrictionsForReplacement": ["EXCLUDE_MULTIOFFER_IN_CONSOLIDATION"],
                    },
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
