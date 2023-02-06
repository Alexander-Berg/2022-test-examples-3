#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Autostrategy,
    AutostrategyType,
    AutostrategyWithDatasourceId,
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    ExperimentalBoostFeeReservePrice,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    VirtualModel,
    ClickType,
)
from core.testcase import TestCase, main
from core.types.reserveprice_fee import ReservePriceFee
from core.types.recommended_fee import RecommendedFee
from core.types.raise_fee_data import RaiseFeeData
from core.matcher import ElementCount
from core.matcher import Contains


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.put_white_cpa_offer_to_the_blue_shard = True
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=1,
                output_type=HyperCategoryType.GURU,
                children=[
                    HyperCategory(
                        hid=2,
                        output_type=HyperCategoryType.GURU,
                        children=[
                            HyperCategory(hid=3, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=4, output_type=HyperCategoryType.GURU),
                            HyperCategory(
                                hid=5,
                                output_type=HyperCategoryType.GURU,
                                children=[
                                    HyperCategory(hid=7, output_type=HyperCategoryType.GURU),
                                ],
                            ),
                            HyperCategory(hid=8, output_type=HyperCategoryType.GURU),
                            HyperCategory(hid=9, output_type=HyperCategoryType.GURU),
                            HyperCategory(
                                hid=11,
                                output_type=HyperCategoryType.GURU,
                                children=[
                                    HyperCategory(hid=22, output_type=HyperCategoryType.GURU),
                                    HyperCategory(hid=33, output_type=HyperCategoryType.GURU),
                                    HyperCategory(hid=44, output_type=HyperCategoryType.GURU),
                                    HyperCategory(hid=100500, output_type=HyperCategoryType.GURU),
                                ],
                            ),
                        ],
                    )
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Анатлий",
                warehouse_id=145,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик клон Анатолия",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                business_fesh=4,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                name="dsbs магазин Пети",
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                business_fesh=6,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                cpc=Shop.CPC_NO,
                priority_region=213,
                name="dsbs магазин клон Пети",
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                business_fesh=7,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                name="dsbs магазин Вовы",
            ),
            Shop(
                fesh=8,
                datafeed_id=8,
                business_fesh=8,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                name="dsbs магазин клон Вовы",
            ),
            Shop(
                fesh=9,
                datafeed_id=9,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Вовы",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=10,
                datafeed_id=10,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик клон Вовы",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[213],
                name="Первый 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=20,
                datafeed_id=20,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P Москва 1",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=21,
                datafeed_id=21,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P Москва 2",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=22,
                datafeed_id=22,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P Москва 3",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=23,
                datafeed_id=23,
                priority_region=213,
                regions=[213],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P Москва 4",
                warehouse_id=145,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145]),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.virtual_models += [
            VirtualModel(
                virtual_model_id=100505,
            ),
        ]

        cls.index.models += [
            Model(hid=2, ts=501, hyperid=12, title='model_12', vbid=11),
            Model(hid=3, ts=502, hyperid=13, title='model_13', vbid=11),
            Model(hid=4, ts=503, hyperid=14, title='model_14', vbid=11),
            Model(hid=5, ts=504, hyperid=15, title='model_15', vbid=11),
            Model(hid=8, ts=508, hyperid=18, title='model_18'),
        ]

        cls.index.mskus += [
            MarketSku(
                title="msku_1",
                hyperid=12,
                sku=100001,
                hid=2,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=2,
                        fee=40,
                        waremd5="BLUE-100001-FEED-2222Q",
                        ts=1,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=4,
                        fee=140,
                        waremd5="BLUE-100001-FEED-3333Q",
                        ts=3,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=9,
                        fee=190,
                        waremd5="BLUE-100001-FEED-4444Q",
                        ts=4,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=1000,
                        feedid=10,
                        fee=720,
                        waremd5="BLUE-100001-FEED-5555Q",
                        ts=5,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_2",
                hyperid=13,
                sku=100002,
                hid=3,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=2,
                        fee=0,
                        waremd5="BLUE-100002-FEED-2222Q",
                        ts=11,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=4,
                        fee=140,
                        waremd5="BLUE-100002-FEED-3333Q",
                        ts=13,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=9,
                        fee=190,
                        waremd5="BLUE-100002-FEED-4444Q",
                        ts=14,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=1000,
                        feedid=10,
                        fee=720,
                        waremd5="BLUE-100002-FEED-5555Q",
                        ts=15,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_3",
                hyperid=14,
                sku=100003,
                hid=4,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=2,
                        fee=40,
                        waremd5="BLUE-100003-FEED-2222Q",
                        ts=21,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=4,
                        fee=140,
                        waremd5="BLUE-100003-FEED-3333Q",
                        ts=23,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=9,
                        fee=190,
                        waremd5="BLUE-100003-FEED-4444Q",
                        ts=24,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=1000,
                        feedid=10,
                        fee=720,
                        waremd5="BLUE-100003-FEED-5555Q",
                        ts=25,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_4",
                hyperid=15,
                sku=100004,
                hid=5,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=300),
                    Elasticity(price_variant=200, demand_mean=180),
                    Elasticity(price_variant=300, demand_mean=120),
                    Elasticity(price_variant=400, demand_mean=95),
                    Elasticity(price_variant=500, demand_mean=75),
                    Elasticity(price_variant=600, demand_mean=60),
                    Elasticity(price_variant=700, demand_mean=45),
                    Elasticity(price_variant=800, demand_mean=30),
                    Elasticity(price_variant=900, demand_mean=20),
                    Elasticity(price_variant=1000, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=150,
                        feedid=20,
                        fee=50,
                        waremd5="BLUE-100004-FEED-2222Q",
                        ts=121,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=200,
                        feedid=21,
                        fee=100,
                        waremd5="BLUE-100004-FEED-3333Q",
                        ts=123,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=250,
                        feedid=22,
                        fee=750,
                        waremd5="BLUE-100004-FEED-4444Q",
                        ts=124,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                    BlueOffer(
                        price=300,
                        feedid=23,
                        fee=600,
                        waremd5="BLUE-100004-FEED-5555Q",
                        ts=125,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_5",
                hyperid=16,
                sku=100005,
                hid=6,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=80,
                        feedid=11,
                        fee=0,
                        waremd5="BLUE-100005-FEED-2222Q",
                        ts=41,  # Оффер 1P поставщика
                        title='test_1p_tag 1p',
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                    BlueOffer(
                        price=100,
                        feedid=4,
                        fee=140,
                        waremd5="BLUE-100005-FEED-3333Q",
                        ts=43,
                        title='test_1p_tag 3p',
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_6",
                hyperid=17,
                sku=100006,
                hid=7,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=300),
                    Elasticity(price_variant=200, demand_mean=180),
                    Elasticity(price_variant=300, demand_mean=120),
                    Elasticity(price_variant=400, demand_mean=95),
                    Elasticity(price_variant=500, demand_mean=75),
                    Elasticity(price_variant=600, demand_mean=60),
                    Elasticity(price_variant=700, demand_mean=45),
                    Elasticity(price_variant=800, demand_mean=30),
                    Elasticity(price_variant=900, demand_mean=20),
                    Elasticity(price_variant=1000, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=500,
                        feedid=20,
                        fee=10,
                        waremd5="BLUE-100006-FEED-1111Q",
                        ts=201,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=3,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                    BlueOffer(
                        price=500,
                        feedid=21,
                        fee=800,
                        waremd5="BLUE-100006-FEED-2222Q",
                        ts=202,
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=0),
                        ),
                    ),
                ],
            ),
            MarketSku(
                title="msku_7",
                hyperid=18,
                sku=100007,
                hid=8,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=20),
                ],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=2,
                        fee=100,
                        waremd5="BLUE-100007-FEED-2222Q",
                        ts=71,
                    ),
                    BlueOffer(
                        price=120,
                        feedid=4,
                        fee=120,
                        waremd5="BLUE-100007-FEED-3333Q",
                        ts=72,
                    ),
                    BlueOffer(
                        price=150,
                        feedid=9,
                        fee=150,
                        waremd5="BLUE-100007-FEED-4444Q",
                        ts=73,
                    ),
                    BlueOffer(
                        price=180,
                        feedid=10,
                        fee=180,
                        waremd5="BLUE-100007-FEED-5555Q",
                        ts=74,
                    ),
                    BlueOffer(
                        price=210,
                        feedid=20,
                        fee=210,
                        waremd5="BLUE-100007-FEED-6666Q",
                        ts=75,
                    ),
                    BlueOffer(
                        price=240,
                        feedid=21,
                        fee=240,
                        waremd5="BLUE-100007-FEED-7777Q",
                        ts=76,
                    ),
                    BlueOffer(
                        price=270,
                        feedid=22,
                        fee=270,
                        waremd5="BLUE-100007-FEED-8888Q",
                        ts=77,
                    ),
                    BlueOffer(
                        price=300,
                        feedid=23,
                        fee=300,
                        waremd5="BLUE-100007-FEED-9999Q",
                        ts=78,
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="market HiD1 DSBS Offer",
                hid=2,
                hyperid=12,
                price=100,
                fesh=5,
                business_id=3,
                sku=100001,
                fee=0,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-111Q',
                feedid=5,
                delivery_buckets=[1234],
                offerid="proh.offer",
                ts=31,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=5,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market HiD1 DSBS Offer 2",
                hid=2,
                hyperid=12,
                price=100,
                fesh=7,
                business_id=5,
                sku=100001,
                fee=190,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-222Q',
                feedid=7,
                delivery_buckets=[1234],
                offerid="proh.offer",
                ts=33,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=6,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market HiD2 DSBS Offer",
                hid=3,
                hyperid=13,
                price=100,
                fesh=5,
                business_id=3,
                sku=100002,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-111Z',
                feedid=5,
                delivery_buckets=[1234],
                offerid="proh.offer",
                ts=34,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=5,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market HiD2 DSBS Offer 2",
                hid=3,
                hyperid=13,
                price=100,
                fesh=7,
                business_id=5,
                sku=100002,
                fee=190,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-222Z',
                feedid=7,
                delivery_buckets=[1234],
                offerid="proh.offer",
                ts=35,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=6,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market HiD3 DSBS Offer",
                hid=4,
                hyperid=14,
                price=100,
                fesh=5,
                business_id=3,
                sku=100003,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-111X',
                feedid=5,
                delivery_buckets=[1234],
                offerid="proh.offer",
                ts=37,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=5,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(
                title="market HiD3 DSBS Offer 2",
                hid=4,
                hyperid=14,
                price=100,
                fesh=7,
                business_id=5,
                sku=100003,
                fee=190,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-222X',
                feedid=7,
                delivery_buckets=[1234],
                offerid="proh.offer",
                ts=39,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=6,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=1),
                ),
            ),
            Offer(hyperid=1, hid=11, fesh=5, ts=100010, price=100, fee=500, title='CPA офер #1-1', cpa=Offer.CPA_REAL),
            Offer(hyperid=1, hid=11, fesh=6, ts=100011, price=110, fee=1000, title='CPA офер #1-2', cpa=Offer.CPA_REAL),
            Offer(hyperid=2, hid=22, fesh=6, ts=100020, price=200, fee=800, title='CPA офер #2-1', cpa=Offer.CPA_REAL),
            Offer(hyperid=2, hid=22, fesh=7, ts=100021, price=200, fee=80, title='CPA офер #2-2', cpa=Offer.CPA_REAL),
            Offer(hyperid=3, hid=33, fesh=8, ts=100030, price=300, fee=1600, title='CPA офер #3-1', cpa=Offer.CPA_REAL),
            Offer(
                hyperid=3,
                hid=33,
                fesh=7,
                ts=100031,
                price=303,
                fee=2000,
                title='CPA лучший офер #3-2',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=4,
                hid=44,
                fesh=8,
                ts=100040,
                price=40,
                fee=400,
                title='CPA худший офер #4-1',
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hyperid=17, hid=7, fesh=7, ts=100100, price=500, fee=20, title='market HiD7 DSBS', cpa=Offer.CPA_REAL
            ),
            Offer(hyperid=19, hid=9, fesh=3, ts=201, price=500, title='market HiD9 CPC', cpa=Offer.CPA_NO, sku=100010),
            Offer(
                hyperid=19, hid=9, fesh=4, ts=202, price=500, title='market HiD9 CPC 1', cpa=Offer.CPA_NO, sku=100010
            ),
            Offer(
                fesh=4,
                price=150,
                waremd5='red_axe_from_mvideo_md',
                title='virtual model',
                virtual_model_id=100500,
                cpa=Offer.CPA_REAL,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100010).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100011).respond(0.011)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100030).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100031).respond(0.031)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100040).respond(0.004)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100040).respond(0.004)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 71).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 72).respond(0.85)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 73).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 74).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 75).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 76).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 77).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 78).respond(0.002)

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=90401, recommended_bid=0.0166),
            RecommendedFee(hyper_id=1, recommended_bid=0.1660),
            RecommendedFee(hyper_id=3, recommended_bid=0.1162),
            RecommendedFee(hyper_id=4, recommended_bid=0.0498),
            RecommendedFee(hyper_id=5, recommended_bid=0.0332),
            RecommendedFee(hyper_id=6, recommended_bid=0.0498),
            RecommendedFee(hyper_id=44, recommended_bid=0.0498),
            RecommendedFee(hyper_id=8, recommended_bid=0.0333),
        ]

        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=90401, reserveprice_fee=0.01),
            ReservePriceFee(hyper_id=1, reserveprice_fee=0.1),
            ReservePriceFee(hyper_id=3, reserveprice_fee=0.07),
            ReservePriceFee(hyper_id=4, reserveprice_fee=0.03),
            ReservePriceFee(hyper_id=5, reserveprice_fee=0.02),
            ReservePriceFee(hyper_id=6, reserveprice_fee=0.03),
            ReservePriceFee(hyper_id=44, reserveprice_fee=0.03),
            ReservePriceFee(hyper_id=8, reserveprice_fee=0.02),
            ReservePriceFee(hyper_id=9, reserveprice_fee=0.02),
        ]

        cls.index.experimental_boost_fee_reserve_prices += [
            ExperimentalBoostFeeReservePrice(90401, 166),
            ExperimentalBoostFeeReservePrice(1, 1660),
            ExperimentalBoostFeeReservePrice(3, 1162),
            ExperimentalBoostFeeReservePrice(4, 498),
            ExperimentalBoostFeeReservePrice(5, 332),
            ExperimentalBoostFeeReservePrice(6, 498),
            ExperimentalBoostFeeReservePrice(44, 498),
        ]

    # Проверяем порог срабатывания по Reserve Price для ТОП6
    #
    # Показатели Fee для офферов
    # Оффер с waremd5 BLUE-100002-FEED-2222Q
    # Это 1P оффер. Ему выставляется Fee=RecommendedFee
    # Общая fee = 0.1162
    #
    # Оффер с waremd5 BLUE-100002-FEED-3333Q
    # Общая fee = 0.074 (vendor bid=1 & shop fee=140)
    #
    # Оффер с waremd5 BLUE-100002-FEED-4444Q
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)
    #
    # Оффер с waremd5 BLUE-100002-FEED-5555Q
    # Общая fee = 0.072 (vendor bid=0 & shop fee=720)
    #
    # Оффер с waremd5 sgf1xWYFqdGiLh4TT-111Q
    # Общая fee = 0.06 (vendor bid=1 & shop fee=0)
    #
    # Оффер с waremd5 sgf1xWYFqdGiLh4TT-222Q
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)

    # Для Reserve Fee = 0.07 должны быть убраны следуюшие оффера
    # waremd5 sgf1xWYFqdGiLh4TT-111Q

    # Должно получиться:
    # входяших офферов = 5
    # Отсейных офферов = 1
    def test_top6_reserve_price_threshold(self):
        """
        Проверяем порог срабатывания по Reserve Price для ТОП6
        test_top6_reserve_price_threshold
        """
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
            "market_money_vendor_cpc_to_cpa_conversion_top6": 0.05,
            "market_enable_buybox_by_business": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        def test_body(pp):
            response = self.report.request_json(
                'place=productoffers&market-sku=100002&rgb=green_with_blue&pp={pp}&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 6,
                        "totalOffers": 6,
                        "totalOffersBeforeFilters": 6,
                        "totalPassedAllGlFilters": 6,
                        "results": ElementCount(6),  # должно быть 6 офферов. 1 дефолтный и 5 для топ6
                    },
                    'debug': {
                        'metasearch': {
                            'subrequests': [
                                "debug",
                                {
                                    "brief": {
                                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 6, "TOTAL_DOCUMENTS_ACCEPTED": 6},
                                    }
                                },
                            ],
                        },
                    },
                },
            )

            # Проверяем что CPC не фильтруются
            response = self.report.request_json(
                'place=productoffers&market-sku=100010&rgb=green_with_blue&pp={pp}&hyperid=19&debug=da&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "totalOffers": 2,
                        "totalOffersBeforeFilters": 2,
                        "totalPassedAllGlFilters": 2,
                        "results": ElementCount(3),  # должно быть 3 офферов. 1 дефолтный и 2 для топ6
                    },
                },
            )

            # Проверяем что для ТОП6 при низком Reserve Price ничего не отсеивается
            #
            # Должно получиться:
            # входяших офферов = 6
            # Отсейных офферов = 0

            response = self.report.request_json(
                'place=productoffers&market-sku=100003&rgb=green_with_blue&pp={pp}&hyperid=14&debug=da&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'metasearch': {
                            'subrequests': [
                                "debug",
                                {
                                    "brief": {
                                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 6, "TOTAL_DOCUMENTS_ACCEPTED": 6},
                                    }
                                },
                            ],
                        },
                    }
                },
            )

            # Проверяем что поиск по дереву категорий работает
            # Ищем оффера с hid=2. В таблице Reserve Price Fee таблице такой категории нет.
            # Должны дойти по дереву категорий до hyper_id=1.
            # Для этой категории Reserve Fee = 0.1
            # при таком высоком пороге ни один из офферов не должен пройти, кроме 1P оффера, так как
            # 1P не фильтруется порогом с rp_fee

            # Должно получиться:
            # входяших офферов = 6
            # офферов в топ 6

            response = self.report.request_json(
                'place=productoffers&market-sku=100001&rgb=green_with_blue&pp={pp}&hyperid=12&debug=da&offers-set=top&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'metasearch': {
                            'subrequests': [
                                "debug",
                                {
                                    "brief": {
                                        "counters": {
                                            "TOTAL_DOCUMENTS_PROCESSED": 6,
                                        },
                                    }
                                },
                            ],
                        },
                    }
                },
            )
            # Находится только одна модель - с 1P поставщиком, остальные не проходят порог RP fee
            self.assertEqual(1, response.count({"entity": "offer"}))
            self.assertFragmentIn(response, {"supplier": {"name": "Один 1P поставщик"}})

        all_tested_pp = [6, 1706, 1806]
        for pp in all_tested_pp:
            test_body(pp)

        # Проверяем что при pp21 порог не работает
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=21&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'metasearch': {
                        'subrequests': [
                            "debug",
                            {
                                "brief": {
                                    "counters": {"TOTAL_DOCUMENTS_PROCESSED": 6, "TOTAL_DOCUMENTS_ACCEPTED": 6},
                                }
                            },
                        ],
                    },
                }
            },
        )

    # Проверяем порог срабатывания на мете по Reserve Price для ТОП6
    # при выключенном выставлении Fee=RecommendedFee для 1P офферов
    #
    # Показатели Fee для офферов
    # Оффер с waremd5 BLUE-100002-FEED-2222Q
    # Это 1P оффер. У него Общая fee = 0
    #
    # Оффер с waremd5 BLUE-100002-FEED-3333Q
    # Общая fee = 0.074 (vendor bid=1 & shop fee=140)
    #
    # Оффер с waremd5 BLUE-100002-FEED-4444Q
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)
    #
    # Оффер с waremd5 BLUE-100002-FEED-5555Q
    # Общая fee = 0.072 (vendor bid=0 & shop fee=720)
    #
    # Оффер с waremd5 sgf1xWYFqdGiLh4TT-111Q
    # Общая fee = 0.06 (vendor bid=1 & shop fee=0)
    #
    # Оффер с waremd5 sgf1xWYFqdGiLh4TT-222Q
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)

    # Для Reserve Fee = 0.07 должны быть убраны следуюшие оффера
    # waremd5 sgf1xWYFqdGiLh4TT-111Q

    # Должно получиться:
    # входяших офферов = 5
    # Отсейных офферов = 2
    def test_top6_reserve_price_threshold_meta_without_1p_fee(self):
        """
        Проверяем порог срабатывания по Reserve Price для ТОП6
        test_top6_reserve_price_threshold_meta_without_1p_fee
        """
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
            "market_money_vendor_cpc_to_cpa_conversion_top6": 0.05,
            "market_set_1p_fee_recommended": 0,
            "market_enable_buybox_by_business": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        def test_body(pp):
            response = self.report.request_json(
                'place=productoffers&market-sku=100002&rgb=green_with_blue&pp={pp}&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 6,
                        "totalOffers": 6,
                        "totalOffersBeforeFilters": 6,
                        "totalPassedAllGlFilters": 6,
                        "results": ElementCount(5),  # должно быть 5 офферов. 1 дефолтный и 4 для топ6
                    },
                },
            )

        all_tested_pp = [6, 1706, 1806]
        for pp in all_tested_pp:
            test_body(pp)

    # Проверяем, что при включенном вычислении Reserve Price из Recommended Fee, все работает так же, как и прежде.
    # Эта функциональность нужна на случай, если чтение данных из динамиков не заработает.
    # После того как чтение заработает, этот тест стоит удалить.
    def test_top6_reserve_price_threshold_meta_with_conversion(self):
        """
        Проверяем порог срабатывания по Reserve Price для ТОП6
        test_top6_reserve_price_threshold
        """
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
            "market_money_enable_сonversion_recommended_fee_to_rp": 1,
            "market_money_get_recommended_fee_from_svn_data": 1,
            "market_money_vendor_cpc_to_cpa_conversion_top6": 0.05,
            "market_enable_buybox_by_business": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 6,
                    "totalOffers": 6,
                    "totalOffersBeforeFilters": 6,
                    "totalPassedAllGlFilters": 6,
                    "results": ElementCount(6),  # должно быть 6 офферов. 1 дефолтный и 5 для топ6
                },
            },
        )
        # use new conversion
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_top6"] = 0.075
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 6,
                    "totalOffers": 6,
                    "totalOffersBeforeFilters": 6,
                    "totalPassedAllGlFilters": 6,
                    "results": ElementCount(3),  # должно быть 3 оффера. 1 дефолтный и 2 для топ6
                },
            },
        )

    # Проверяем работу флага market_enable_cpc_filtering
    # Все CPC оффера из top6 должны отфильтровываться
    def test_top6_reserve_price_threshold_enable_cpc_filtering(self):
        """
        Проверяем работу флага market_enable_cpc_filtering
        Все CPC оффера из top6 должны отфильтровываться
        """
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
            "market_enable_cpc_filtering": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        def test_body(pp):
            response = self.report.request_json(
                'place=productoffers&market-sku=100010&rgb=green_with_blue&pp={pp}&hyperid=19&debug=da&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 2,
                        "totalOffers": 2,
                        "totalOffersBeforeFilters": 2,
                        "totalPassedAllGlFilters": 2,
                        "results": ElementCount(1),  # должно быть 1 офферов. 1 дефолтный и 0 для топ6
                    },
                },
            )

        all_tested_pp = [6, 1706, 1806]
        for pp in all_tested_pp:
            test_body(pp)

    # Проверяем работу флага market_money_vendor_cpc_to_cpa_conversion_top6
    def test_top6_reserve_price_threshold_reduce_total_passed_all_gl_filters(self):
        """
        Проверяем порог срабатывания по Reserve Price для ТОП6
        test_top6_reserve_price_threshold
        """
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
            "market_money_vendor_cpc_to_cpa_conversion_top6": 0.05,
            "market_enable_buybox_by_business": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        def test_body(pp):
            response = self.report.request_json(
                'place=productoffers&market-sku=100002&rgb=green_with_blue&pp={pp}&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 6,
                        "totalOffers": 6,
                        "totalOffersBeforeFilters": 6,
                        "totalPassedAllGlFilters": 6,
                        "results": ElementCount(6),  # должно быть 6 офферов. 1 дефолтный и 5 для топ6
                    },
                },
            )

        all_tested_pp = [6, 1706, 1806]
        for pp in all_tested_pp:
            test_body(pp)

    # Проверяем что виртуальные карточки не фильтруются
    def test_top6_reserve_price_threshold_virtual_model(self):
        rearr_flags_str = 'market_ranging_cpa_by_ue_in_top_rp_fee_coef_w=1;market_ranging_cpa_by_ue_in_top_purchase_coef_c=0;market_top6_reserve_price_threshold=1;market_cards_everywhere_product_offers=1;market_cards_everywhere_range=100000:200000;'  # noqa

        def test_body(pp):
            response = self.report.request_json(
                'place=productoffers&hyperid=100500&pp={pp}&rearr-factors={rearr}'.format(pp=pp, rearr=rearr_flags_str)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': 'red_axe_from_mvideo_mQ',
                            'prices': {'value': '150'},
                            "model": {
                                "id": 100500,
                            },
                        },
                    ]
                },
                allow_different_len=False,
            )

        all_tested_pp = [6, 1706, 1806]
        for pp in all_tested_pp:
            test_body(pp)

    # Проверяем порог срабатывания по Reserve Price для cpa_shop_incut врезки
    #
    # Для запроса берутся следуюшие оффера:
    # sgf1xWYFqdGiLh4TT-111Q
    # Общая fee = 0.06 (vendor bid=1 & shop fee=0)
    # Reserve Price Fee = 0.1
    # Не проходит
    #
    # sgf1xWYFqdGiLh4TT-222Q
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)
    # Reserve Price Fee = 0.1
    # Не проходит
    #
    # sgf1xWYFqdGiLh4TT-111Z
    # Общая fee = 0.06 (vendor bid=1 & shop fee=0)
    # Reserve Price Fee = 0.07
    # Не проходит
    #
    # sgf1xWYFqdGiLh4TT-222Z
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)
    # Reserve Price Fee = 0.07
    # Проходит
    #
    # sgf1xWYFqdGiLh4TT-111X
    # Общая fee = 0.06 (vendor bid=1 & shop fee=0)
    # Reserve Price Fee = 0.03
    # Проходит
    #
    # sgf1xWYFqdGiLh4TT-222X
    # Общая fee = 0.079 (vendor bid=1 & shop fee=190)
    # Reserve Price Fee = 0.03
    # Проходит

    # Должно получиться:
    # входяших офферов = 6
    # Отсейных офферов = 3

    def test_cpa_shop_incut_reserve_price_threshold(self):
        rearr_flags_dict = {
            "market_premium_ads_gallery_shop_incut_prun_count": 3000,
            "market_premium_ads_gallery_shop_incut_enable_panther": 1,
            "market_premium_ads_gallery_shop_incut_panther_coef": 0.5,
            "market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef": 0.05,
            "market_cpa_shop_incut_rp_fee_coef_w": 1,
            "market_cpa_shop_incut_reserve_price_threshold": 1,
            'market_buybox_auction_cpa_shop_incut': 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&pp=230&text=Offer&min-num-doc=0&show-urls=cpa,promotion&debug=da&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"OFFER_FILTERED_OUT_BY_RESERVE_PRICE": 3},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 6, "TOTAL_DOCUMENTS_ACCEPTED": 3},
                    }
                }
            },
        )

    def test_cpa_shop_incut_reserve_price_threshold_with_auction_in_buybox(self):
        rearr_flags_dict = {
            "market_premium_ads_gallery_shop_incut_prun_count": 3000,
            "market_premium_ads_gallery_shop_incut_enable_panther": 1,
            "market_premium_ads_gallery_shop_incut_panther_coef": 0.5,
            "market_premium_ads_gallery_shop_incut_vendor_bid_to_fee_coef": 0.05,
            "market_cpa_shop_incut_rp_fee_coef_w": 1,
            "market_cpa_shop_incut_reserve_price_threshold": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&pp=230&text=Offer&min-num-doc=0&show-urls=cpa,promotion&debug=da&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "brief": {
                        "filters": {"OFFER_FILTERED_OUT_BY_RESERVE_PRICE": 2},
                        "counters": {"TOTAL_DOCUMENTS_PROCESSED": 3, "TOTAL_DOCUMENTS_ACCEPTED": 1},
                    }
                }
            },
        )

    # Проверяем что из ТОП6 убирается оффер если в ТОП6 попал только этот один оффер и этот же оффер выбрался в ДО.
    def test_top6_duplicate_offer_erasing(self):
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
            "market_blue_buybox_use_old_randomization": 1,
        }

        def test_body(pp):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=100006&rgb=green_with_blue&pp={pp}&hyperid=17&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'wareId': "BLUE-100006-FEED-2222Q",
                            "benefit": {"type": "cheapest"},
                        },
                    ]
                },
            )

            self.assertFragmentIn(response, {'search': {'results': ElementCount(1)}})

            response = self.report.request_json(
                'place=productoffers&market-sku=100006&rgb=green_with_blue&pp={pp}&hyperid=17&rids=213&offers-set=defaultList,listCpa&rearr-factors={rearr}'.format(
                    pp=pp, rearr=rearr_flags_str
                )
            )

            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'wareId': "BLUE-100006-FEED-2222Q",
                            "benefit": {"type": "cheapest"},
                        },
                    ]
                },
            )

            self.assertFragmentIn(response, {'search': {'results': ElementCount(1)}})

        all_tested_pp = [6, 1706, 1806]
        for pp in all_tested_pp:
            test_body(pp)

    # Проверяем, что 1P оффер попадает в топ 6 (не отфильтровывается).
    # У 1P оффера shop_fee == 0, поэтому после вычитания rp_fee был бы отрицательный score, по которому фильтруем.
    def test_top6_reserve_price_for_1p(self):
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_operational_rating_everywhere": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 1,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_top6_reserve_price_threshold": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100005&rgb=green_with_blue&pp=6&hyperid=16&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "BLUE-100005-FEED-2222Q",
                    }
                ]
            },
        )

    # Проверяем, что 1P оффер попадает во врезку (не отфильтровывается).
    # У 1P оффера shop_fee == 0, поэтому после вычитания rp_fee был бы отрицательный score, по которому фильтруем.
    def test_cpa_shop_incut_reserve_price_for_1p(self):
        rearr_flags_dict = {
            "market_premium_ads_gallery_shop_incut_prun_count": 3000,
            "market_premium_ads_gallery_shop_incut_enable_panther": 1,
            "market_premium_ads_gallery_shop_incut_panther_coef": 0.5,
            "market_cpa_shop_incut_rp_fee_coef_w": 1,
            "market_cpa_shop_incut_reserve_price_threshold": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=cpa_shop_incut&rids=213&pp=230&text=test_1p_tag&min-num-doc=0&show-urls=cpa,promotion&debug=da&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "BLUE-100005-FEED-2222Q",
                    }
                ],
            },
        )

    # В тестах
    # test_cpa_shop_incut_reserve_price_sale_fee_w_0
    # test_cpa_shop_incut_reserve_price_sale_fee_w_1
    # проверяем что в  cpa_shop_incut врезках market_cpa_shop_incut_rp_fee_coef_w коэффициент
    # не влияет на списание.
    def test_cpa_shop_incut_reserve_price_sale_fee_w_0(self):
        rearr_flags_dict = {
            "market_premium_ads_gallery_default_min_num_doc": 0,
            "market_premium_ads_gallery_shop_incut_logarithm_price": 0,
            "market_cpa_shop_incut_reserve_price_threshold": 0,  # Отключаем порог так как в тесте проверяется только списание
            "market_cpa_shop_incut_rp_fee_coef_w": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json('place=cpa_shop_incut&text=офер&debug=1&rearr-factors=%s' % rearr_flags_str)

        # В каждой группе должен выбраться один офер, с учетом ставок
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '64'}],
                            "sale": {
                                "shopFee": 400,
                                "brokeredFee": 1,
                            },
                        },
                    },
                ]
            },
        )

    # В тестах
    # test_cpa_shop_incut_reserve_price_sale_fee_w_0
    # test_cpa_shop_incut_reserve_price_sale_fee_w_1
    # проверяем что в  cpa_shop_incut врезках market_cpa_shop_incut_rp_fee_coef_w коэффициент
    # не влияет на списание.
    def test_cpa_shop_incut_reserve_price_sale_fee_w_1(self):
        rearr_flags_dict = {
            "market_premium_ads_gallery_default_min_num_doc": 0,
            "market_premium_ads_gallery_shop_incut_logarithm_price": 0,
            "market_cpa_shop_incut_reserve_price_threshold": 0,  # Отключаем порог так как в тесте проверяется только списание
            "market_cpa_shop_incut_rp_fee_coef_w": 1,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json('place=cpa_shop_incut&text=офер&debug=1&rearr-factors=%s' % rearr_flags_str)

        # В каждой группе должен выбраться один офер, с учетом ставок
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'titles': {'raw': 'CPA лучший офер #3-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '18786'}],
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 1534,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #2-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '3200'}],
                            "sale": {
                                "shopFee": 800,
                                "brokeredFee": 303,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA офер #1-2'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '1210'}],
                            "sale": {
                                "shopFee": 1000,
                                "brokeredFee": 414,
                            },
                        },
                    },
                    {
                        'titles': {'raw': 'CPA худший офер #4-1'},
                        'debug': {
                            'rank': [{'name': 'CPA_SHOP_INCUT', 'value': '64'}],
                            "sale": {
                                "shopFee": 400,
                                "brokeredFee": 1,
                            },
                        },
                    },
                ]
            },
        )

    @classmethod
    def prepare_top6_cehac(cls):
        cls.index.hypertree += [HyperCategory(hid=91063)]

        cls.index.models += [Model(hyperid=801, hid=91063)]

        cls.index.mskus += [
            MarketSku(
                title="msku_801",
                hyperid=801,
                sku=100801,
                hid=91063,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=10000, demand_mean=200),
                    Elasticity(price_variant=20000, demand_mean=80),
                    Elasticity(price_variant=30000, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=10000,
                        feedid=2,
                        waremd5="BLUE-100801-FEED-2222Q",
                        ts=1008011,
                    ),
                    BlueOffer(
                        price=10000,
                        feedid=4,
                        fee=40,
                        waremd5="BLUE-100801-FEED-3333Q",
                        ts=1008013,
                    ),
                    BlueOffer(
                        price=10000,
                        feedid=9,
                        fee=90,
                        waremd5="BLUE-100801-FEED-4444Q",
                        ts=1008014,
                    ),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1008011).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1008013).respond(0.0101)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1008014).respond(0.0102)

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=91063, recommended_bid=0.0166),
        ]

        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=91063, reserveprice_fee=0.01),
        ]

    # Проверяем, что оффера категории cehac с включенным флагом market_top6_cehac_without_reserve_price попадают в топ6
    # а без флага попадают не все
    def test_top6_cehac(self):
        rearr_flags_dict = {
            "market_top6_cehac_without_reserve_price": 1,
            "market_top6_coef_1p_fee_recommended": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100801&rgb=green_with_blue&pp=6&hyperid=801&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100801-FEED-2222Q",
                        "debug": {
                            "sale": {
                                "shopFee": 166,
                                "brokeredFee": 93,  # less than rpfee
                            }
                        },
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100801-FEED-4444Q",
                        "debug": {
                            "sale": {
                                "shopFee": 90,  # less than rpfee
                                "brokeredFee": 40,  # less than rpfee
                            }
                        },
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100801-FEED-3333Q",
                        "debug": {
                            "sale": {
                                "shopFee": 40,  # less than rpfee
                                "brokeredFee": 0,
                            }
                        },
                    }
                ]
            },
        )

        rearr_flags_dict["market_top6_cehac_without_reserve_price"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100801&rgb=green_with_blue&pp=6&hyperid=801&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "wareId": "BLUE-100801-FEED-4444Q",
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "wareId": "BLUE-100801-FEED-3333Q",
            },
        )

    @classmethod
    def prepare_top6_boost_fee(cls):
        cls.index.hypertree += [HyperCategory(hid=91064)]

        cls.index.shops += [
            Shop(
                fesh=100,
                datafeed_id=100,
                priority_region=213,
                regions=[213],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                cpa=Shop.CPA_REAL,
                business_fesh=100,
            ),
        ]

        cls.index.models += [
            Model(hid=91064, hyperid=901, title='model_901'),
        ]

        cls.index.mskus += [
            MarketSku(
                title="msku_901",
                hyperid=901,
                sku=100901,
                hid=91064,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=10000, demand_mean=200),
                    Elasticity(price_variant=20000, demand_mean=80),
                    Elasticity(price_variant=30000, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=10000,
                        feedid=100,
                        waremd5="BLUE-100901-FEED-2222Q",
                        ts=1009011,
                        business_id=100,
                    ),
                ],
            ),
            MarketSku(
                title="msku_902",
                hyperid=901,
                sku=100902,
                hid=91064,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=10000, demand_mean=200),
                    Elasticity(price_variant=20000, demand_mean=80),
                    Elasticity(price_variant=30000, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(
                        price=10000,
                        feedid=100,
                        waremd5="BLUE-100902-FEED-2222Q",
                        ts=1009012,
                        fee=200,
                        business_id=100,
                    ),
                ],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1009011).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1009012).respond(0.0101)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1009013).respond(0.0102)

        cls.index.raise_fee_data += [
            RaiseFeeData(msku='100901', business='100', group=2000),
            RaiseFeeData(msku='100002', business='3', group=2000),
        ]

    # Проверяем что буст ставки работает с синими оферами.
    # У двух офферов business id одинаковый, но они различаются по msku
    def test_top6_boost_fee_msku_diff(self):
        rearr_flags_dict = {
            "market_boost_fee_by_msky_business_id": 1,
            "market_buybox_auction_cpa_fee": 1,
            "market_top6_coef_1p_fee_recommended": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&pp=6&hyperid=901&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100901-FEED-2222Q",
                        "debug": {
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 0,
                            }
                        },
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100902-FEED-2222Q",
                    },
                ]
            },
        )
        self.click_log.expect(ClickType.CPA, position=1, shop_fee=2000, shop_fee_ab=0, fuid=Contains('fee=168'))

        rearr_flags_dict["market_boost_fee_by_msky_business_id"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&pp=6&hyperid=901&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100902-FEED-2222Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100901-FEED-2222Q",
                        "debug": {
                            "sale": {
                                "shopFee": 166,
                                "brokeredFee": 100,
                            }
                        },
                    },
                ]
            },
        )

    # Проверяем что буст ставки работает с белыми оферами.
    # У двух офферов msku одинаковый, но они различаются по business id
    def test_top6_boost_fee_businessid_diff(self):
        rearr_flags_dict = {
            "market_boost_fee_by_msky_business_id": 1,
            "market_buybox_auction_cpa_fee": 1,
            "market_top6_reserve_price_threshold": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-111Q",
                        "offerColor": "white",
                        "debug": {
                            "sale": {
                                "shopFee": 2000,
                                "brokeredFee": 0,
                            }
                        },
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100002-FEED-2222Q",
                    },
                ]
            },
        )

        rearr_flags_dict["market_boost_fee_by_msky_business_id"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=13&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100002-FEED-2222Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100002-FEED-5555Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-222Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100002-FEED-4444Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100002-FEED-3333Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-111Q",
                        "debug": {
                            "sale": {
                                "shopFee": 0,
                                "brokeredFee": 0,
                            }
                        },
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
