#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DeliveryOption,
    DynamicWarehouseInfo,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    ExpressSupplier,
    MarketSku,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    generate_dsbs,
)
from core.types.delivery import BlueDeliveryTariff
from core.types.offer import OfferDimensions
from core.testcase import TestCase, main
from core.types.combinator import DeliveryType


class _Constants:
    MIN_STOCKS = 1
    AVG_STOCKS = 2
    MAX_STOCKS = 3
    MIN_PRICE = 10
    AVG_PRICE = 20
    MAX_PRICE = 30
    MIN_DAY_FROM = 1
    MIN_DAY_TO = 1
    AVG_DAY_FROM = 2
    AVG_DAY_TO = 2
    MAX_DAY_FROM = 3
    MAX_DAY_TO = 3
    MIN_PICKUP_PRICE = 5
    MIN_BOOKING_PRICE = 10
    MIN_COURIER_PRICE = 50
    DELIVERY_PRICE = 99


class _Rids:
    russia = 225
    moscow = 213


class _DeliveryServices:
    internal = 99


class _Params:
    drugs_category_1_id = 15758037
    drugs_category_2_id = 15756914
    drugs_category_3_id = 15756919
    drugs_category_4_id = 15756921
    drugs_category_5_id = 15756897
    common_category_id = 1234567890


class _Categories:
    medical_all = 1
    medical_express = 2
    medical_courier = 3
    medical_booking = 4
    medical_pickup = 5
    common = 6
    medical_without_nordstream = 7


class _Feshes:
    class _White:
        medical_pickup = 10
        medical_booking = 20
        medical_courier = 30
        medical_express = 40
        medical_without_nordstream = 50


class _Feeds:
    class _White:
        medical_pickup = 100
        medical_booking = 200
        medical_courier = 300
        medical_express = 400
        medical_without_nordstream = 500


class _ClientIds:
    class _White:
        medical_pickup = 101
        medical_booking = 201
        medical_courier = 301
        medical_express = 401
        medical_without_nordstream = 501


class _Warehouses:
    class _White:
        medical_pickup = 1000
        medical_booking = 2000
        medical_courier = 3000
        medical_express = 4000
        medical_without_nordstream = 5000


class _Outlets:
    class _White:
        medical_pickup_id = 10000
        medical_booking_id = 20000

        def create(fesh, point_id):
            return Outlet(
                fesh=fesh,
                point_id=point_id,
                region=_Rids.moscow,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=103),
                working_days=[i for i in range(3)],
            )

        medical_pickup = create(fesh=_Feshes._White.medical_pickup, point_id=medical_pickup_id)
        medical_booking = create(fesh=_Feshes._White.medical_booking, point_id=medical_booking_id)


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
            medical_booking=False,
        ):
            return Shop(
                fesh=fesh,
                datafeed_id=datafeed_id,
                client_id=client_id,
                warehouse_id=warehouse_id,
                priority_region=priority_region,
                regions=regions,
                name=name,
                cpa=Shop.CPA_REAL,
                cis=Shop.CIS_REAL,
                medicine_courier=True,
                medical_booking=medical_booking,
            )

        medical_pickup = create(
            fesh=_Feshes._White.medical_pickup,
            datafeed_id=_Feeds._White.medical_pickup,
            client_id=_ClientIds._White.medical_pickup,
            warehouse_id=_Warehouses._White.medical_pickup,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='Medical shop with pickup delivery',
        )

        medical_booking = create(
            fesh=_Feshes._White.medical_booking,
            datafeed_id=_Feeds._White.medical_booking,
            client_id=_ClientIds._White.medical_booking,
            warehouse_id=_Warehouses._White.medical_booking,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='Medical shop with booking delivery',
        )

        medical_courier = create(
            fesh=_Feshes._White.medical_courier,
            datafeed_id=_Feeds._White.medical_courier,
            client_id=_ClientIds._White.medical_courier,
            warehouse_id=_Warehouses._White.medical_courier,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='Medical shop with courier delivery',
        )

        medical_express = create(
            fesh=_Feshes._White.medical_express,
            datafeed_id=_Feeds._White.medical_express,
            client_id=_ClientIds._White.medical_express,
            warehouse_id=_Warehouses._White.medical_express,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='Medical shop with express delivery',
        )

        medical_without_nordstream = create(
            fesh=_Feshes._White.medical_without_nordstream,
            datafeed_id=_Feeds._White.medical_without_nordstream,
            client_id=_ClientIds._White.medical_without_nordstream,
            warehouse_id=_Warehouses._White.medical_without_nordstream,
            priority_region=_Rids.moscow,
            regions=[_Rids.moscow],
            name='Medical shop without nordstream',
        )


class _Buckets:
    class _White:
        medical_pickup_id = 10000
        medical_booking_id = 20000
        medical_courier_id = 30000
        medical_express_id = 40000

        def create_delivery(bucket_id, shop, carriers):
            return DeliveryBucket(
                bucket_id=bucket_id,
                dc_bucket_id=bucket_id,
                fesh=shop.fesh,
                carriers=carriers,
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
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )

        medical_pickup = create_pickup(
            bucket_id=medical_pickup_id,
            outlet_id=_Outlets._White.medical_pickup_id,
            shop=_Shops._White.medical_pickup,
            carriers=[_DeliveryServices.internal],
        )
        medical_booking = create_pickup(
            bucket_id=medical_booking_id,
            outlet_id=_Outlets._White.medical_booking_id,
            shop=_Shops._White.medical_booking,
            carriers=[_DeliveryServices.internal],
        )
        medical_courier = create_delivery(
            bucket_id=medical_courier_id,
            shop=_Shops._White.medical_courier,
            carriers=[_DeliveryServices.internal],
        )
        medical_express = create_delivery(
            bucket_id=medical_express_id,
            shop=_Shops._White.medical_express,
            carriers=[_DeliveryServices.internal],
        )


class _Mskus:
    medical_all_id = 100000
    medical_express_id = 200000
    medical_courier_id = 300000
    medical_booking_id = 400000
    medical_pickup_id = 500000
    common_id = 600000
    medical_without_nordstream_id = 700000

    medical_all = MarketSku(
        title="Medical MSKU (all delivery options)",
        sku=medical_all_id,
        hyperid=_Categories.medical_all,
    )
    medical_express = MarketSku(
        title="Medical MSKU (express delivery option)",
        sku=medical_express_id,
        hyperid=_Categories.medical_express,
    )
    medical_courier = MarketSku(
        title="Medical MSKU (courier delivery option)",
        sku=medical_courier_id,
        hyperid=_Categories.medical_courier,
    )
    medical_booking = MarketSku(
        title="Medical MSKU (booking delivery option)",
        sku=medical_booking_id,
        hyperid=_Categories.medical_booking,
    )
    medical_pickup = MarketSku(
        title="Medical MSKU (pickup delivery option)",
        sku=medical_pickup_id,
        hyperid=_Categories.medical_pickup,
    )
    common = MarketSku(
        title="Common MSKU",
        sku=common_id,
        hyperid=_Categories.common,
    )
    medical_without_nordstream = MarketSku(
        title="Medical MSKU without nordstream",
        sku=medical_without_nordstream_id,
        hyperid=_Categories.medical_without_nordstream,
    )


class _Models:
    medical_all = Model(
        hid=_Params.drugs_category_1_id,
        hyperid=_Mskus.medical_all.hyperid,
        is_medicine=True,
    )
    medical_express = Model(
        hid=_Params.drugs_category_2_id,
        hyperid=_Mskus.medical_express.hyperid,
        is_medicine=True,
    )
    medical_courier = Model(
        hid=_Params.drugs_category_3_id,
        hyperid=_Mskus.medical_courier.hyperid,
        is_medicine=True,
    )
    medical_booking = Model(
        hid=_Params.drugs_category_4_id,
        hyperid=_Mskus.medical_booking.hyperid,
        is_medicine=True,
        is_prescription=True,
    )
    medical_pickup = Model(
        hid=_Params.drugs_category_4_id,
        hyperid=_Mskus.medical_pickup.hyperid,
        is_medicine=True,
        is_prescription=True,
    )
    common = Model(
        hid=_Params.common_category_id,
        hyperid=_Mskus.common.hyperid,
    )
    medical_without_nordstream = Model(
        hid=_Params.drugs_category_5_id,
        hyperid=_Mskus.medical_without_nordstream.hyperid,
    )


class _Offers:
    class _White:
        def create(
            waremd5,
            shop,
            supplier_id,
            msku,
            price,
            stock_store_count,
            weight=1,
            cpa=Offer.CPA_REAL,
            dimensions=OfferDimensions(length=3, width=3, height=3),
            delivery_options=None,
            delivery_buckets=None,
            pickup_buckets=None,
            is_medicine=False,
            is_express=False,
            is_prescription=False,
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
                is_medical_booking=is_medical_booking,
            )

        medical_express = create(
            waremd5='medical_express_only_g',
            shop=_Shops._White.medical_express,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.medical_express,
            price=_Constants.MIN_PRICE,
            stock_store_count=_Constants.MIN_STOCKS,
            delivery_buckets=[_Buckets._White.medical_express.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.MIN_DAY_FROM,
                    day_to=_Constants.MIN_DAY_TO,
                )
            ],
            is_medicine=True,
            is_express=True,
        )

        medical_courier = create(
            waremd5='medical_courier_only_g',
            shop=_Shops._White.medical_courier,
            supplier_id=_ClientIds._White.medical_courier,
            msku=_Mskus.medical_courier,
            price=_Constants.AVG_PRICE,
            stock_store_count=_Constants.AVG_STOCKS,
            delivery_buckets=[_Buckets._White.medical_courier.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.AVG_DAY_FROM,
                    day_to=_Constants.AVG_DAY_TO,
                )
            ],
            is_medicine=True,
        )

        medical_booking = create(
            waremd5='medical_booking_only_g',
            shop=_Shops._White.medical_booking,
            supplier_id=_ClientIds._White.medical_booking,
            msku=_Mskus.medical_booking,
            price=_Constants.MAX_PRICE,
            stock_store_count=_Constants.MAX_STOCKS,
            pickup_buckets=[_Buckets._White.medical_booking.bucket_id],
            is_medicine=True,
            is_prescription=True,
            is_medical_booking=True,
        )

        medical_pickup = create(
            waremd5='medical_pickup_only__g',
            shop=_Shops._White.medical_pickup,
            supplier_id=_ClientIds._White.medical_pickup,
            msku=_Mskus.medical_pickup,
            price=_Constants.AVG_PRICE,
            stock_store_count=_Constants.MAX_STOCKS,
            pickup_buckets=[_Buckets._White.medical_pickup.bucket_id],
            is_medicine=True,
            is_prescription=True,
        )

        medical_express_all = create(
            waremd5='medical_express_all__g',
            shop=_Shops._White.medical_express,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.medical_all,
            price=_Constants.MIN_PRICE,
            stock_store_count=_Constants.MIN_STOCKS,
            delivery_buckets=[_Buckets._White.medical_express.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.MIN_DAY_FROM,
                    day_to=_Constants.MIN_DAY_TO,
                )
            ],
            is_medicine=True,
            is_express=True,
        )

        medical_courier_all = create(
            waremd5='medical_courier_all__g',
            shop=_Shops._White.medical_courier,
            supplier_id=_ClientIds._White.medical_courier,
            msku=_Mskus.medical_all,
            price=_Constants.AVG_PRICE,
            stock_store_count=_Constants.AVG_STOCKS,
            delivery_buckets=[_Buckets._White.medical_courier.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.AVG_DAY_FROM,
                    day_to=_Constants.AVG_DAY_TO,
                )
            ],
            is_medicine=True,
        )

        medical_booking_all = create(
            waremd5='medical_booking_all__g',
            shop=_Shops._White.medical_booking,
            supplier_id=_ClientIds._White.medical_booking,
            msku=_Mskus.medical_all,
            price=_Constants.MAX_PRICE,
            stock_store_count=_Constants.MAX_STOCKS,
            pickup_buckets=[_Buckets._White.medical_booking.bucket_id],
            is_medicine=True,
            is_prescription=True,
            is_medical_booking=True,
        )

        medical_pickup_all = create(
            waremd5='medical_pickup_all___g',
            shop=_Shops._White.medical_pickup,
            supplier_id=_ClientIds._White.medical_pickup,
            msku=_Mskus.medical_all,
            price=_Constants.AVG_PRICE,
            stock_store_count=_Constants.MAX_STOCKS,
            pickup_buckets=[_Buckets._White.medical_pickup.bucket_id],
            is_medicine=True,
            is_prescription=True,
        )

        medical_without_nordstream = create(
            waremd5='medical_wo_nordstreamg',
            shop=_Shops._White.medical_without_nordstream,
            supplier_id=_ClientIds._White.medical_without_nordstream,
            msku=_Mskus.medical_without_nordstream,
            price=_Constants.AVG_PRICE,
            stock_store_count=0,  # We emulate partner error zero stock value
            delivery_buckets=[_Buckets._White.medical_courier.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.AVG_DAY_FROM,
                    day_to=_Constants.AVG_DAY_TO,
                )
            ],
            is_medicine=True,
        )

        common_courier = create(
            waremd5='common_courier_______g',
            shop=_Shops._White.medical_courier,
            supplier_id=_ClientIds._White.medical_pickup,
            msku=_Mskus.common,
            price=_Constants.AVG_PRICE,
            stock_store_count=_Constants.AVG_STOCKS,
            delivery_buckets=[_Buckets._White.medical_courier.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.AVG_DAY_FROM,
                    day_to=_Constants.AVG_DAY_TO,
                )
            ],
        )

        common_express = create(
            waremd5='common_express_______g',
            shop=_Shops._White.medical_express,
            supplier_id=_ClientIds._White.medical_express,
            msku=_Mskus.common,
            price=_Constants.MIN_PRICE,
            stock_store_count=_Constants.MIN_STOCKS,
            delivery_buckets=[_Buckets._White.medical_express.bucket_id],
            delivery_options=[
                DeliveryOption(
                    price=100,
                    order_before=14,
                    day_from=_Constants.MIN_DAY_FROM,
                    day_to=_Constants.MIN_DAY_TO,
                )
            ],
            is_express=True,
        )


class _BlueDeliveryTariff:
    # тарифы подготовлены в соответствии с ценами офферов
    # тариф действует на обычный самовывоз и курьерскую доставку
    courier_price = BlueDeliveryTariff(
        user_price=_Constants.DELIVERY_PRICE,
        courier_price=_Constants.MIN_COURIER_PRICE,
        pickup_price=_Constants.MIN_PICKUP_PRICE,
        price_to=_Constants.AVG_PRICE + 1,
    )
    # тариф действует на бронирование из наличия
    booking_price = BlueDeliveryTariff(user_price=_Constants.DELIVERY_PRICE, pickup_price=_Constants.MIN_BOOKING_PRICE)

    ALL = [courier_price, booking_price]


class _Requests:
    prime = (
        'place=prime'
        '&pp=18'
        '&rids=213'
        '&allow-collapsing=1'
        '&use-default-offers=1'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=market_enable_aggregate_offer={is_aggregate_offer}'
        '&market-sku={msku}'
    )

    productoffers = (
        'place=productoffers'
        '&pp=18'
        '&rids=213'
        '&offers-set=defaultList'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=market_enable_aggregate_offer={is_aggregate_offer}'
        '&hyperid={hyperid}'
    )

    combine = (
        'place=combine'
        '&split-strategy=split-medicine'
        '&pp=18'
        '&rids=213'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=market_enable_aggregate_offer={is_aggregate_offer}'
    )

    offerinfo = (
        'place=offerinfo'
        '&pp=18'
        '&rids=213'
        '&regset=2'
        '&show-urls=direct'
        '&rearr-factors=market_not_prescription_drugs_delivery=1'
        '&rearr-factors=market_enable_aggregate_offer={is_aggregate_offer}'
        '&offerid={offerid}'
    )


class T(TestCase):
    """
    Набор тестов для проекта "Агрегированный оффер".
    Краткое описание:
    - сбор статистики по msku для медицины (относящейся к проекту покупка списком)
    - цена, опции доставки, сроки доставки, складские значения, минимальная стоимость доставки
    - place= prime, productoffers, offerinfo, combine
    См.: https://st.yandex-team.ru/MARKETOUT-45038
    """

    @classmethod
    def prepare_experiments(cls):
        cls.settings.default_search_experiment_flags += [
            'enable_cart_split_on_combinator=0',
        ]

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [
            Region(rid=_Rids.moscow, name="Moscow", tz_offset=10800),
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            _Shops._White.medical_pickup,
            _Shops._White.medical_booking,
            _Shops._White.medical_courier,
            _Shops._White.medical_express,
            _Shops._White.medical_without_nordstream,
        ]

    @classmethod
    def prepare_mskus(cls):
        cls.index.mskus += [
            _Mskus.medical_all,
            _Mskus.medical_express,
            _Mskus.medical_courier,
            _Mskus.medical_booking,
            _Mskus.medical_pickup,
            _Mskus.medical_without_nordstream,
            _Mskus.common,
        ]

    @classmethod
    def prepare_models(cls):
        cls.index.models += [
            _Models.medical_all,
            _Models.medical_express,
            _Models.medical_courier,
            _Models.medical_booking,
            _Models.medical_pickup,
            _Models.medical_without_nordstream,
            _Models.common,
        ]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            _Offers._White.medical_express,
            _Offers._White.medical_courier,
            _Offers._White.medical_booking,
            _Offers._White.medical_pickup,
            _Offers._White.medical_express_all,
            _Offers._White.medical_courier_all,
            _Offers._White.medical_booking_all,
            _Offers._White.medical_pickup_all,
            _Offers._White.medical_without_nordstream,
            _Offers._White.common_express,
            _Offers._White.common_courier,
        ]

    @classmethod
    def prepare_delivery_buckets(cls):
        cls.index.delivery_buckets += [
            _Buckets._White.medical_courier,
            _Buckets._White.medical_express,
        ]

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += [
            _Buckets._White.medical_pickup,
            _Buckets._White.medical_booking,
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            _Outlets._White.medical_pickup,
            _Outlets._White.medical_booking,
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        for wh_id, ds_id, ds_name in [
            (_Warehouses._White.medical_pickup, _DeliveryServices.internal, 'shop_pickup_delivery_service'),
            (_Warehouses._White.medical_booking, _DeliveryServices.internal, 'shop_booking_delivery_service'),
            (_Warehouses._White.medical_courier, _DeliveryServices.internal, 'shop_courier_delivery_service'),
            (
                _Warehouses._White.medical_without_nordstream,
                _DeliveryServices.internal,
                'shop_wo_nordstream_delivery_service',
            ),
        ]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name=ds_name,
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=_Rids.moscow, region_to=_Rids.russia, days_key=1)
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=_Rids.moscow, holidays_days_set_key=2),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=2, region_to=_Rids.russia)],
                ),
            ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(
                id=_Warehouses._White.medical_express,
                home_region=_Rids.moscow,
                is_express=True,
            )
        ]

    @classmethod
    def prepare_express_partners(cls):
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=_Shops._White.medical_express.datafeed_id,
                supplier_id=_Shops._White.medical_express.fesh,
                warehouse_id=_Shops._White.medical_express.warehouse_id,
            )
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Warehouses._White.medical_pickup, [_Warehouses._White.medical_pickup])
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Warehouses._White.medical_booking, [_Warehouses._White.medical_booking])
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Warehouses._White.medical_courier, [_Warehouses._White.medical_courier])
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Warehouses._White.medical_express, [_Warehouses._White.medical_express])
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                _Warehouses._White.medical_pickup,
                {
                    _Rids.moscow: [
                        DynamicDeliveryRestriction(
                            min_days=_Constants.MAX_DAY_FROM,
                            max_days=_Constants.MAX_DAY_TO,
                            delivery_type=DeliveryType.PICKUP,
                        ),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                _Warehouses._White.medical_booking,
                {
                    _Rids.moscow: [
                        DynamicDeliveryRestriction(
                            min_days=_Constants.MAX_DAY_FROM,
                            max_days=_Constants.MAX_DAY_TO,
                            delivery_type=DeliveryType.PICKUP,
                        ),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                _Warehouses._White.medical_courier,
                {
                    _Rids.moscow: [
                        DynamicDeliveryRestriction(
                            min_days=_Constants.AVG_DAY_FROM,
                            max_days=_Constants.AVG_DAY_TO,
                            delivery_type=DeliveryType.COURIER,
                        ),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                _Warehouses._White.medical_express,
                {
                    _Rids.moscow: [
                        DynamicDeliveryRestriction(
                            min_days=_Constants.MIN_DAY_FROM,
                            max_days=_Constants.MIN_DAY_TO,
                            delivery_type=DeliveryType.COURIER,
                        ),
                    ],
                },
            ),
        ]

    @classmethod
    def prepare_tariffs(cls):
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=_BlueDeliveryTariff.ALL,
        )

    @classmethod
    def prepare_zyx(cls):
        """Методы `prepare_{}` зовутся в алфавитном порядке.
        Но для `generate_dsbs` нужны разные сущности в `cls.index`.
        Поэтому добавляем такой суфикс, чтобы нас позвали последними.
        """
        cls.dynamic.nordstream += generate_dsbs(cls.index)

    # Tests: place=combine

    def test_combine_common_all_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины происходит
        агрегирование информации только для медицинских офферов.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.common_express.ware_md5,
            msku=_Offers._White.common_express.sku,
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
                                    "offers": [{"replacedId": _Offers._White.common_express.ware_md5}],
                                },
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.common_express.sku,
                                "aggregateMskuInfo": Absent(),
                            }
                        ]
                    },
                }
            },
            allow_different_len=False,
        )

    def test_combine_all_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины происходит
        агрегирование информации только для медицинских офферов.
        На выдаче вся агрегированная информация.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_pickup_all.ware_md5,
            msku=_Offers._White.medical_pickup_all.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_pickup_all.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_pickup_all.sku,
                                "aggregateMskuInfo": {
                                    "hasExpress": True,
                                    "expressDayFrom": _Constants.MIN_DAY_FROM,
                                    "expressDayTo": _Constants.MIN_DAY_TO,
                                    "hasCourier": True,
                                    "courierDayFrom": _Constants.AVG_DAY_FROM,
                                    "courierDayTo": _Constants.AVG_DAY_TO,
                                    "hasPickup": True,
                                    "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                    "pickupDayTo": _Constants.MAX_DAY_TO,
                                    "minStocks": _Constants.MIN_STOCKS,
                                    "maxStocks": _Constants.MAX_STOCKS,
                                    "hasBooking": True,
                                    "prices": {
                                        "min": str(_Constants.MIN_PRICE),
                                        "max": str(_Constants.MAX_PRICE),
                                        "currency": "RUR",
                                    },
                                    "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                    "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                    "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_combine_no_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины не происходит
        агрегирование информации.
        """

        # При выключеном эксперименте не происходит агрегирования!
        request = _Requests.combine.format(is_aggregate_offer=False)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_pickup_all.ware_md5,
            msku=_Offers._White.medical_pickup_all.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_pickup_all.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_pickup_all.sku,
                                "aggregateMskuInfo": Absent(),
                            }
                        ]
                    },
                }
            },
            allow_different_len=False,
        )

    def test_combine_without_nordstream(self):
        """
        Проверям что при запросе на выделение медицинской корзины
        без информации из Nordstream не имеют на выдаче агрегированных сроков доставки.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_without_nordstream.ware_md5,
            msku=_Offers._White.medical_without_nordstream.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_without_nordstream.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_without_nordstream.sku,
                                "aggregateMskuInfo": {
                                    "hasExpress": False,
                                    "expressDayFrom": Absent(),
                                    "expressDayTo": Absent(),
                                    "hasCourier": True,
                                    "courierDayFrom": Absent(),
                                    "courierDayTo": Absent(),
                                    "hasPickup": False,
                                    "pickupDayFrom": Absent(),
                                    "pickupDayTo": Absent(),
                                    "minStocks": _Constants.MIN_STOCKS,  # Because we emulate 0 stock
                                    "maxStocks": _Constants.MIN_STOCKS,  # partner error
                                    "hasBooking": False,
                                    "prices": {
                                        "min": str(_Constants.AVG_PRICE),
                                        "max": str(_Constants.AVG_PRICE),
                                        "currency": "RUR",
                                    },
                                    "minPickupPrice": Absent(),
                                    "minBookingPrice": Absent(),
                                    "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_combine_express_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины происходит
        агрегирование информации только для медицинских офферов.
        На выдаче агрегированная информация только для экспресс доставки.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_express.ware_md5,
            msku=_Offers._White.medical_express.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_express.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_express.sku,
                                "aggregateMskuInfo": {
                                    "hasExpress": True,
                                    "expressDayFrom": _Constants.MIN_DAY_FROM,
                                    "expressDayTo": _Constants.MIN_DAY_TO,
                                    "hasCourier": False,
                                    "courierDayFrom": Absent(),
                                    "courierDayTo": Absent(),
                                    "hasPickup": False,
                                    "pickupDayFrom": Absent(),
                                    "pickupDayTo": Absent(),
                                    "minStocks": _Constants.MIN_STOCKS,
                                    "maxStocks": _Constants.MIN_STOCKS,
                                    "hasBooking": False,
                                    "prices": {
                                        "min": str(_Constants.MIN_PRICE),
                                        "max": str(_Constants.MIN_PRICE),
                                        "currency": "RUR",
                                    },
                                    "minPickupPrice": Absent(),
                                    "minBookingPrice": Absent(),
                                    "minCourierPrice": Absent(),
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_combine_courier_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины происходит
        агрегирование информации только для медицинских офферов.
        На выдаче агрегированная информация только для курьерской доставки.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_courier.ware_md5,
            msku=_Offers._White.medical_courier.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_courier.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_courier.sku,
                                "aggregateMskuInfo": {
                                    "hasExpress": False,
                                    "expressDayFrom": Absent(),
                                    "expressDayTo": Absent(),
                                    "hasCourier": True,
                                    "courierDayFrom": _Constants.AVG_DAY_FROM,
                                    "courierDayTo": _Constants.AVG_DAY_TO,
                                    "hasPickup": False,
                                    "pickupDayFrom": Absent(),
                                    "pickupDayTo": Absent(),
                                    "minStocks": _Constants.AVG_STOCKS,
                                    "maxStocks": _Constants.AVG_STOCKS,
                                    "hasBooking": False,
                                    "prices": {
                                        "min": str(_Constants.AVG_PRICE),
                                        "max": str(_Constants.AVG_PRICE),
                                        "currency": "RUR",
                                    },
                                    "minPickupPrice": Absent(),
                                    "minBookingPrice": Absent(),
                                    "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_combine_booking_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины происходит
        агрегирование информации только для медицинских офферов.
        На выдаче агрегированная информация только для бронирования.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_booking.ware_md5,
            msku=_Offers._White.medical_booking.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_booking.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_booking.sku,
                                "aggregateMskuInfo": {
                                    "hasExpress": False,
                                    "expressDayFrom": Absent(),
                                    "expressDayTo": Absent(),
                                    "hasCourier": False,
                                    "courierDayFrom": Absent(),
                                    "courierDayTo": Absent(),
                                    "hasPickup": True,
                                    "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                    "pickupDayTo": _Constants.MAX_DAY_TO,
                                    "minStocks": _Constants.MAX_STOCKS,
                                    "maxStocks": _Constants.MAX_STOCKS,
                                    "hasBooking": True,
                                    "prices": {
                                        "min": str(_Constants.MAX_PRICE),
                                        "max": str(_Constants.MAX_PRICE),
                                        "currency": "RUR",
                                    },
                                    "minPickupPrice": Absent(),
                                    "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                    "minCourierPrice": Absent(),
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    def test_combine_pickup_info(self):
        """
        Проверям что при запросе на выделение медицинской корзины происходит
        агрегирование информации только для медицинских офферов.
        На выдаче агрегированная информация только для самовывоза.
        """

        request = _Requests.combine.format(is_aggregate_offer=True)

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_pickup.ware_md5,
            msku=_Offers._White.medical_pickup.sku,
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
                                    "offers": [{"replacedId": _Offers._White.medical_pickup.ware_md5}],
                                }
                            ],
                        }
                    ],
                    "offers": {
                        "items": [
                            {
                                "marketSku": _Offers._White.medical_pickup.sku,
                                "aggregateMskuInfo": {
                                    "hasExpress": False,
                                    "expressDayFrom": Absent(),
                                    "expressDayTo": Absent(),
                                    "hasCourier": False,
                                    "courierDayFrom": Absent(),
                                    "courierDayTo": Absent(),
                                    "hasPickup": True,
                                    "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                    "pickupDayTo": _Constants.MAX_DAY_TO,
                                    "minStocks": _Constants.MAX_STOCKS,
                                    "maxStocks": _Constants.MAX_STOCKS,
                                    "hasBooking": False,
                                    "prices": {
                                        "min": str(_Constants.AVG_PRICE),
                                        "max": str(_Constants.AVG_PRICE),
                                        "currency": "RUR",
                                    },
                                    "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                    "minBookingPrice": Absent(),
                                    "minCourierPrice": Absent(),
                                },
                            },
                        ],
                    },
                },
            },
            allow_different_len=False,
        )

    # Tests: place=prime

    def test_prime_all_info(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.prime.format(
            is_aggregate_offer=True,
            msku=_Offers._White.medical_pickup_all.sku,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_pickup_all.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": {
                                            "hasExpress": True,
                                            "expressDayFrom": _Constants.MIN_DAY_FROM,
                                            "expressDayTo": _Constants.MIN_DAY_TO,
                                            "hasCourier": True,
                                            "courierDayFrom": _Constants.AVG_DAY_FROM,
                                            "courierDayTo": _Constants.AVG_DAY_TO,
                                            "hasPickup": True,
                                            "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                            "pickupDayTo": _Constants.MAX_DAY_TO,
                                            "minStocks": _Constants.MIN_STOCKS,
                                            "maxStocks": _Constants.MAX_STOCKS,
                                            "hasBooking": True,
                                            "prices": {
                                                "min": str(_Constants.MIN_PRICE),
                                                "max": str(_Constants.MAX_PRICE),
                                                "currency": "RUR",
                                            },
                                            "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                            "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                            "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_no_info(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer не имеют на выдаче агрегированную информацию.
        """

        # При выключеном эксперименте не происходит агрегирования!
        request = _Requests.prime.format(
            is_aggregate_offer=False,
            msku=_Offers._White.medical_pickup_all.sku,
        )

        offers_list = ('&offers-list={wareid}:1;msku:{msku}').format(
            wareid=_Offers._White.medical_pickup_all.ware_md5,
            msku=_Offers._White.medical_pickup_all.sku,
        )

        response = self.report.request_json(request + offers_list)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_pickup_all.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": Absent(),
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_without_nordstream(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer без информации из Nordstream не имеют на выдаче
        агрегированных сроков доставки.
        """

        request = _Requests.prime.format(
            is_aggregate_offer=True,
            msku=_Offers._White.medical_without_nordstream.sku,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_without_nordstream.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": {
                                            "hasExpress": False,
                                            "expressDayFrom": Absent(),
                                            "expressDayTo": Absent(),
                                            "hasCourier": True,
                                            "courierDayFrom": Absent(),
                                            "courierDayTo": Absent(),
                                            "hasPickup": False,
                                            "pickupDayFrom": Absent(),
                                            "pickupDayTo": Absent(),
                                            "minStocks": _Constants.MIN_STOCKS,  # Because we emulate 0 stock
                                            "maxStocks": _Constants.MIN_STOCKS,  # partner error
                                            "hasBooking": False,
                                            "prices": {
                                                "min": str(_Constants.AVG_PRICE),
                                                "max": str(_Constants.AVG_PRICE),
                                                "currency": "RUR",
                                            },
                                            "minPickupPrice": Absent(),
                                            "minBookingPrice": Absent(),
                                            "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_express_info(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.prime.format(
            is_aggregate_offer=True,
            msku=_Offers._White.medical_express.sku,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_express.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": {
                                            "hasExpress": True,
                                            "expressDayFrom": _Constants.MIN_DAY_FROM,
                                            "expressDayTo": _Constants.MIN_DAY_TO,
                                            "hasCourier": False,
                                            "courierDayFrom": Absent(),
                                            "courierDayTo": Absent(),
                                            "hasPickup": False,
                                            "pickupDayFrom": Absent(),
                                            "pickupDayTo": Absent(),
                                            "minStocks": _Constants.MIN_STOCKS,
                                            "maxStocks": _Constants.MIN_STOCKS,
                                            "hasBooking": False,
                                            "prices": {
                                                "min": str(_Constants.MIN_PRICE),
                                                "max": str(_Constants.MIN_PRICE),
                                                "currency": "RUR",
                                            },
                                            "minPickupPrice": Absent(),
                                            "minBookingPrice": Absent(),
                                            "minCourierPrice": Absent(),
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_courier_info(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.prime.format(
            is_aggregate_offer=True,
            msku=_Offers._White.medical_courier.sku,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_courier.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": {
                                            "hasExpress": False,
                                            "expressDayFrom": Absent(),
                                            "expressDayTo": Absent(),
                                            "hasCourier": True,
                                            "courierDayFrom": _Constants.AVG_DAY_FROM,
                                            "courierDayTo": _Constants.AVG_DAY_TO,
                                            "hasPickup": False,
                                            "pickupDayFrom": Absent(),
                                            "pickupDayTo": Absent(),
                                            "minStocks": _Constants.AVG_STOCKS,
                                            "maxStocks": _Constants.AVG_STOCKS,
                                            "hasBooking": False,
                                            "prices": {
                                                "min": str(_Constants.AVG_PRICE),
                                                "max": str(_Constants.AVG_PRICE),
                                                "currency": "RUR",
                                            },
                                            "minPickupPrice": Absent(),
                                            "minBookingPrice": Absent(),
                                            "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_booking_info(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.prime.format(
            is_aggregate_offer=True,
            msku=_Offers._White.medical_booking.sku,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_booking.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": {
                                            "hasExpress": False,
                                            "expressDayFrom": Absent(),
                                            "expressDayTo": Absent(),
                                            "hasCourier": False,
                                            "courierDayFrom": Absent(),
                                            "courierDayTo": Absent(),
                                            "hasPickup": True,
                                            "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                            "pickupDayTo": _Constants.MAX_DAY_TO,
                                            "minStocks": _Constants.MAX_STOCKS,
                                            "maxStocks": _Constants.MAX_STOCKS,
                                            "hasBooking": True,
                                            "prices": {
                                                "min": str(_Constants.MAX_PRICE),
                                                "max": str(_Constants.MAX_PRICE),
                                                "currency": "RUR",
                                            },
                                            "minPickupPrice": Absent(),
                                            "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                            "minCourierPrice": Absent(),
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_prime_pickup_info(self):
        """
        Проверям что при запросе в place=prime медицинские оффера, которые выиграли
        в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.prime.format(
            is_aggregate_offer=True,
            msku=_Offers._White.medical_pickup.sku,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "marketSku": _Offers._White.medical_pickup.sku,
                                        "isDefaultOffer": True,
                                        "aggregateMskuInfo": {
                                            "hasExpress": False,
                                            "expressDayFrom": Absent(),
                                            "expressDayTo": Absent(),
                                            "hasCourier": False,
                                            "courierDayFrom": Absent(),
                                            "courierDayTo": Absent(),
                                            "hasPickup": True,
                                            "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                            "pickupDayTo": _Constants.MAX_DAY_TO,
                                            "minStocks": _Constants.MAX_STOCKS,
                                            "maxStocks": _Constants.MAX_STOCKS,
                                            "hasBooking": False,
                                            "prices": {
                                                "min": str(_Constants.AVG_PRICE),
                                                "max": str(_Constants.AVG_PRICE),
                                                "currency": "RUR",
                                            },
                                            "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                            "minBookingPrice": Absent(),
                                            "minCourierPrice": Absent(),
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    # Tests: place=productoffers

    def test_productoffers_all_info(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые
        выиграли в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.productoffers.format(
            is_aggregate_offer=True,
            hyperid=_Offers._White.medical_pickup_all.hyperid,
        )

        response = self.report.request_json(request + '&add-sku-stats=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": {
                                "hasExpress": True,
                                "expressDayFrom": _Constants.MIN_DAY_FROM,
                                "expressDayTo": _Constants.MIN_DAY_TO,
                                "hasCourier": True,
                                "courierDayFrom": _Constants.AVG_DAY_FROM,
                                "courierDayTo": _Constants.AVG_DAY_TO,
                                "hasPickup": True,
                                "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                "pickupDayTo": _Constants.MAX_DAY_TO,
                                "minStocks": _Constants.MIN_STOCKS,
                                "maxStocks": _Constants.MAX_STOCKS,
                                "hasBooking": True,
                                "prices": {
                                    "min": str(_Constants.MIN_PRICE),
                                    "max": str(_Constants.MAX_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                            },
                        },
                        {
                            # Двойной байбокс из-за экспресса
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_productoffers_no_info(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые
        выиграли в default-offer не имеют на выдаче агрегированную информацию.
        """

        # При выключеном эксперименте не происходит агрегирования!
        request = _Requests.productoffers.format(
            is_aggregate_offer=False,
            hyperid=_Offers._White.medical_pickup_all.hyperid,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": Absent(),
                        },
                        {
                            # Двойной байбокс из-за экспресса
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_productoffers_without_nordstream(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые выиграли
        в default-offer без информации из Nordstream не имеют на выдаче
        агрегированных сроков доставки.
        """

        request = _Requests.productoffers.format(
            is_aggregate_offer=True,
            hyperid=_Offers._White.medical_without_nordstream.hyperid,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": True,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": False,
                                "pickupDayFrom": Absent(),
                                "pickupDayTo": Absent(),
                                "minStocks": _Constants.MIN_STOCKS,  # Because we emulate 0 stock
                                "maxStocks": _Constants.MIN_STOCKS,  # partner error
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.AVG_PRICE),
                                    "max": str(_Constants.AVG_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_productoffers_express_info(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые
        выиграли в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.productoffers.format(
            is_aggregate_offer=True,
            hyperid=_Offers._White.medical_express.hyperid,
        )

        response = self.report.request_json(request + '&add-sku-stats=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": {
                                "hasExpress": True,
                                "expressDayFrom": _Constants.MIN_DAY_FROM,
                                "expressDayTo": _Constants.MIN_DAY_TO,
                                "hasCourier": False,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": False,
                                "pickupDayFrom": Absent(),
                                "pickupDayTo": Absent(),
                                "minStocks": _Constants.MIN_STOCKS,
                                "maxStocks": _Constants.MIN_STOCKS,
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.MIN_PRICE),
                                    "max": str(_Constants.MIN_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": Absent(),
                            },
                        },
                        {
                            # Двойной байбокс из-за экспресса
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_productoffers_courier_info(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые
        выиграли в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.productoffers.format(
            is_aggregate_offer=True,
            hyperid=_Offers._White.medical_courier.hyperid,
        )

        response = self.report.request_json(request + '&add-sku-stats=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": True,
                                "courierDayFrom": _Constants.AVG_DAY_FROM,
                                "courierDayTo": _Constants.AVG_DAY_TO,
                                "hasPickup": False,
                                "pickupDayFrom": Absent(),
                                "pickupDayTo": Absent(),
                                "minStocks": _Constants.AVG_STOCKS,
                                "maxStocks": _Constants.AVG_STOCKS,
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.AVG_PRICE),
                                    "max": str(_Constants.AVG_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_productoffers_booking_info(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые
        выиграли в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.productoffers.format(
            is_aggregate_offer=True,
            hyperid=_Offers._White.medical_booking.hyperid,
        )

        response = self.report.request_json(request + '&add-sku-stats=1')
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": False,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": True,
                                "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                "pickupDayTo": _Constants.MAX_DAY_TO,
                                "minStocks": _Constants.MAX_STOCKS,
                                "maxStocks": _Constants.MAX_STOCKS,
                                "hasBooking": True,
                                "prices": {
                                    "min": str(_Constants.MAX_PRICE),
                                    "max": str(_Constants.MAX_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                "minCourierPrice": Absent(),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_productoffers_pickup_info(self):
        """
        Проверям что при запросе в place=productoffers медицинские оффера, которые
        выиграли в default-offer имеют на выдаче агрегированную информацию.
        """

        request = _Requests.productoffers.format(
            is_aggregate_offer=True,
            hyperid=_Offers._White.medical_pickup.hyperid,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "isDefaultOffer": True,
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": False,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": True,
                                "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                "pickupDayTo": _Constants.MAX_DAY_TO,
                                "minStocks": _Constants.MAX_STOCKS,
                                "maxStocks": _Constants.MAX_STOCKS,
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.AVG_PRICE),
                                    "max": str(_Constants.AVG_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": Absent(),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    # Tests: place=offerinfo

    def test_offerinfo_common_all_info(self):
        """
        Проверям что при запросе в place=offerinfo только медицинские оффера,
        имеют на выдаче агрегированную информацию.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.common_express.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": Absent(),
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_offerinfo_all_info(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, имеют на
        выдаче агрегированную информацию.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.medical_pickup_all.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": {
                                "hasExpress": True,
                                "expressDayFrom": _Constants.MIN_DAY_FROM,
                                "expressDayTo": _Constants.MIN_DAY_TO,
                                "hasCourier": True,
                                "courierDayFrom": _Constants.AVG_DAY_FROM,
                                "courierDayTo": _Constants.AVG_DAY_TO,
                                "hasPickup": True,
                                "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                "pickupDayTo": _Constants.MAX_DAY_TO,
                                "minStocks": _Constants.MIN_STOCKS,
                                "maxStocks": _Constants.MAX_STOCKS,
                                "hasBooking": True,
                                "prices": {
                                    "min": str(_Constants.MIN_PRICE),
                                    "max": str(_Constants.MAX_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_offerinfo_no_info(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, не имеют
        на выдаче агрегированную информацию.
        """

        # При выключеном эксперименте не происходит агрегирования!
        request = _Requests.offerinfo.format(
            is_aggregate_offer=False,
            offerid=_Offers._White.medical_pickup_all.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": Absent(),
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_offerinfo_without_nordstream(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, которые
        без информации из Nordstream не имеют на выдаче агрегированных сроков
        доставки.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.medical_without_nordstream.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": True,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": False,
                                "pickupDayFrom": Absent(),
                                "pickupDayTo": Absent(),
                                "minStocks": _Constants.MIN_STOCKS,  # Because we emulate 0 stock
                                "maxStocks": _Constants.MIN_STOCKS,  # partner error
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.AVG_PRICE),
                                    "max": str(_Constants.AVG_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_offerinfo_express_info(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, имеют на
        выдаче агрегированную информацию.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.medical_express.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": {
                                "hasExpress": True,
                                "expressDayFrom": _Constants.MIN_DAY_FROM,
                                "expressDayTo": _Constants.MIN_DAY_TO,
                                "hasCourier": False,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": False,
                                "pickupDayFrom": Absent(),
                                "pickupDayTo": Absent(),
                                "minStocks": _Constants.MIN_STOCKS,
                                "maxStocks": _Constants.MIN_STOCKS,
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.MIN_PRICE),
                                    "max": str(_Constants.MIN_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": Absent(),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_offerinfo_courier_info(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, имеют на
        выдаче агрегированную информацию.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.medical_courier.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": True,
                                "courierDayFrom": _Constants.AVG_DAY_FROM,
                                "courierDayTo": _Constants.AVG_DAY_TO,
                                "hasPickup": False,
                                "pickupDayFrom": Absent(),
                                "pickupDayTo": Absent(),
                                "minStocks": _Constants.AVG_STOCKS,
                                "maxStocks": _Constants.AVG_STOCKS,
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.AVG_PRICE),
                                    "max": str(_Constants.AVG_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": str(_Constants.MIN_COURIER_PRICE),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_offerinfo_booking_info(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, имеют на
        выдаче агрегированную информацию.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.medical_booking.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": False,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": True,
                                "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                "pickupDayTo": _Constants.MAX_DAY_TO,
                                "minStocks": _Constants.MAX_STOCKS,
                                "maxStocks": _Constants.MAX_STOCKS,
                                "hasBooking": True,
                                "prices": {
                                    "min": str(_Constants.MAX_PRICE),
                                    "max": str(_Constants.MAX_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": Absent(),
                                "minBookingPrice": str(_Constants.MIN_BOOKING_PRICE),
                                "minCourierPrice": Absent(),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_offerinfo_pickup_info(self):
        """
        Проверям что при запросе в place=offerinfo медицинские оффера, имеют на
        выдаче агрегированную информацию.
        """

        request = _Requests.offerinfo.format(
            is_aggregate_offer=True,
            offerid=_Offers._White.medical_pickup.ware_md5,
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "aggregateMskuInfo": {
                                "hasExpress": False,
                                "expressDayFrom": Absent(),
                                "expressDayTo": Absent(),
                                "hasCourier": False,
                                "courierDayFrom": Absent(),
                                "courierDayTo": Absent(),
                                "hasPickup": True,
                                "pickupDayFrom": _Constants.MAX_DAY_FROM,
                                "pickupDayTo": _Constants.MAX_DAY_TO,
                                "minStocks": _Constants.MAX_STOCKS,
                                "maxStocks": _Constants.MAX_STOCKS,
                                "hasBooking": False,
                                "prices": {
                                    "min": str(_Constants.AVG_PRICE),
                                    "max": str(_Constants.AVG_PRICE),
                                    "currency": "RUR",
                                },
                                "minPickupPrice": str(_Constants.MIN_PICKUP_PRICE),
                                "minBookingPrice": Absent(),
                                "minCourierPrice": Absent(),
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
