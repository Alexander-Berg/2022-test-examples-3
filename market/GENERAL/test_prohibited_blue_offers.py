#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from collections import namedtuple

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    GLValue,
    GpsCoord,
    HyperCategory,
    ImagePickerData,
    Model,
    NavCategory,
    Outlet,
    OutletDeliveryOption,
    ParameterValue,
    Payment,
    PickupBucket,
    PickupOption,
    Picture,
    ProhibitedBlueOffers,
    Region,
    RegionalDelivery,
    Shop,
    TimeInfo,
    WarehouseDeliveryRegions,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.autogen import Const

from core.types.hypercategory import (
    # TODO: выпилить здесь: https://st.yandex-team.ru/MARKETOUT-33898
    # TODO: выпилить ^^^^^^
    MARKET_SUBSCRIPTIONS_CATEG_ID,
)

from core.types.taxes import (
    Vat,
    Tax,
)
from core.types.delivery import BlueDeliveryTariff
from core.types.picture import thumbnails_config


def date_switch_time_info(switch_hour):
    return DateSwitchTimeAndRegionInfo(date_switch_hour=switch_hour, region_to=225)


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


warehouse145_delivery_service_157 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=145, delivery_service_id=157, operation_time=0, date_switch_time_infos=[date_switch_time_info(2)]
)
warehouse145_delivery_service_158 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=145, delivery_service_id=158, operation_time=0, date_switch_time_infos=[date_switch_time_info(3)]
)
warehouse147_delivery_service_147147 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=147, delivery_service_id=147147, operation_time=0, date_switch_time_infos=[date_switch_time_info(3)]
)
warehouse166_delivery_service_169 = DynamicWarehouseAndDeliveryServiceInfo(
    warehouse_id=166, delivery_service_id=169, operation_time=0, date_switch_time_infos=[date_switch_time_info(3)]
)

FF_1P_SHOP = 465852


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        delivery_service_outlets=[2001, 2003, 2004],
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        work_schedule='virtual shop work schedule',
    )

    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        name='blue_shop_1',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=145,
        work_schedule='work schedule supplier 3',
    )

    blue_shop_2 = Shop(
        fesh=4,
        datafeed_id=4,
        priority_region=213,
        name='blue_shop_2',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=145,
    )

    book_shop = Shop(
        fesh=577858,
        datafeed_id=58,
        priority_region=200,
        name='MyShop.ru',
        currency=Currency.RUR,
        supplier_type=Shop.THIRD_PARTY,
        tax_system=Tax.OSN,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=220,
    )

    blue_shop_1470 = Shop(
        fesh=FF_1P_SHOP,
        datafeed_id=14700,
        priority_region=39,
        name='blue_shop_1470',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        cpa=Shop.CPA_REAL,
        warehouse_id=147,
        work_schedule='work schedule supplier 1470',
    )

    blue_shop_3 = Shop(
        fesh=43,
        datafeed_id=4343,
        priority_region=213,
        name='blue_shop_3',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        fulfillment_program=True,
        blue=Shop.BLUE_REAL,
        warehouse_id=166,
    )

    dsbs_shop = Shop(
        fesh=42,
        datafeed_id=4242,
        priority_region=213,
        name='DBS shop',
        cpa=Shop.CPA_REAL,
        warehouse_id=145,
        is_dsbs=True,
    )


class _Offers(object):

    offer_1 = BlueOffer(
        price=1,
        vat=Vat.VAT_10,
        feedid=4,
        offerid='blue.offer.14.1',
        waremd5='Sku14Price1-IiLVm1GolQ',
    )
    offer_2 = BlueOffer(
        price=2000,
        vat=Vat.VAT_10,
        feedid=14700,
        offerid='blue.offer.14.2',
        waremd5='Sku14Price2-IiLVm1GolQ',
    )
    offer_3 = BlueOffer(
        price=325,
        vat=Vat.VAT_18,
        feedid=58,
        waremd5='ReuEngDict___________g',
        offerid='rus.eng.dict',
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=311,
                fesh=1,
                carriers=[
                    201,
                ],
                regional_options=[
                    RegionalDelivery(
                        rid=811 + offset,
                        options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)],
                    )
                    for offset in range(0, 3)
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=312,
                fesh=1,
                carriers=[
                    201,
                ],
                regional_options=[
                    RegionalDelivery(
                        rid=811 + offset,
                        options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)],
                    )
                    for offset in range(1, 3)
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=71 + offset,
                delivery_service_id=201,
                region=814 + offset,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            )
            for offset in range(0, 3)
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=411,
                fesh=1,
                carriers=[201],
                options=[
                    PickupOption(outlet_id=71, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=72, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=73, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.prohibited_blue_offers += [
            ProhibitedBlueOffers(region_id=811, categories=[1]),
            ProhibitedBlueOffers(region_id=814, categories=[1]),
            ProhibitedBlueOffers(region_id=811, categories=[21]),
            ProhibitedBlueOffers(region_id=814, categories=[21]),
            ProhibitedBlueOffers(region_id=815, categories=[21]),
            ProhibitedBlueOffers(region_id=812, categories=[22]),
            ProhibitedBlueOffers(region_id=814, categories=[22]),
            ProhibitedBlueOffers(region_id=816, categories=[22]),
            ProhibitedBlueOffers(region_id=811, categories=[31]),
            ProhibitedBlueOffers(region_id=815, categories=[31]),
            ProhibitedBlueOffers(region_id=812, categories=[32]),
            ProhibitedBlueOffers(region_id=814, categories=[32]),
            ProhibitedBlueOffers(region_id=816, categories=[32]),
            ProhibitedBlueOffers(region_id=811, categories=[41]),
            ProhibitedBlueOffers(region_id=811, categories=[51]),
        ]

        cls.settings.report_subrole = 'blue-main'

        _ = Picture(
            picture_id="KdwwrYb4czANgt9-3poEQQ",
            width=500,
            height=600,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )
        _ = Picture(
            picture_id="KdwwrYb4caANgt9-3poEQQ",
            width=400,
            height=700,
            thumb_mask=thumbnails_config.get_mask_by_names(['1x1', '100x100']),
            group_id=1234,
        )

        cls.index.hypertree = [
            HyperCategory(
                hid=1,
                fee=123,
                children=[
                    HyperCategory(hid=4, fee=321),
                ],
            ),
            HyperCategory(hid=MARKET_SUBSCRIPTIONS_CATEG_ID, fee=123),
            HyperCategory(uniq_name="Books", hid=90829, children=[HyperCategory(hid=90831, uniq_name='Dictionaries')]),
        ]

        cls.index.navtree = [
            NavCategory(hid=1, nid=1, children=[NavCategory(hid=4, nid=4)]),
            NavCategory(nid=100, children=[NavCategory(nid=101, hid=1)]),
            NavCategory(
                nid=90829,
                hid=90829,
                is_blue=True,
                name="Books",
                children=[NavCategory(nid=90831, hid=90831, is_blue=True, name="Dictionaries")],
            ),
        ]

        cls.index.navtree += [NavCategory(nid=99001, hid=1000, is_blue=True)]

        cls.index.navtree_blue += [NavCategory(nid=99001, hid=9000)]
        cls.settings.blue_market_free_delivery_threshold = 55
        cls.settings.blue_market_prime_free_delivery_threshold = 53
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = 52
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[BlueDeliveryTariff(user_price=99)], ya_plus_threshold=52
        )

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
            Outlet(
                point_id=2002,
                fesh=1,
                region=213,
                point_type=Outlet.FOR_STORE,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=1, day_to=1, order_before=1, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(38.12, 54.67),
            ),
            Outlet(
                point_id=2003,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=4, day_to=6, order_before=2, work_in_holiday=False, price=100
                ),
                working_days=[i for i in range(7)],
                gps_coord=GpsCoord(37.12, 55.33),
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=103, day_from=2, day_to=2, order_before=1, price=100),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(36.13, 55.45),
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[103],
                options=[
                    PickupOption(outlet_id=2004, day_from=2, day_to=2, price=100),
                    PickupOption(outlet_id=2002, day_from=1, day_to=1, price=100),
                    PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=1,
                carriers=[103],
                options=[
                    PickupOption(outlet_id=2002, day_from=1, day_to=1, price=100),
                    PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2003, day_from=2, day_to=2, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        def delivery_service_region_to_region_info():
            return DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=2),
            DynamicWarehouseInfo(id=147, home_region=39, holidays_days_set_key=2),
            DynamicWarehouseInfo(id=666, home_region=213),
            DynamicWarehouseInfo(id=777, home_region=213),
            DynamicWarehouseInfo(id=1213, home_region=213),
            DynamicWarehouseInfo(id=220, home_region=213),
            DynamicDeliveryServiceInfo(
                99, "self-delivery", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(103, "c_103", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(157, "c_157", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(158, "c_158", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(169, "c_169", region_to_region_info=[delivery_service_region_to_region_info()]),
            DynamicDeliveryServiceInfo(
                147147,
                "c_147147",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=39, region_to=26, days_key=1),
                    DeliveryServiceRegionToRegionInfo(region_from=39, region_to=456, days_key=1),
                ],
            ),
            DynamicDeliveryServiceInfo(
                165, "dropship_delivery", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(
                163,
                "books_delivery",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=200, region_to=213, days_key=1)],
            ),
            DynamicDaysSet(key=1, days=[]),
            DynamicDaysSet(key=2, days=[0, 1, 2, 5, 6, 14, 20, 21, 27, 28]),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=103,
                operation_time=0,
                date_switch_time_infos=[date_switch_time_info(2)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=220,
                delivery_service_id=163,
                operation_time=0,
                date_switch_time_infos=[date_switch_time_info(2)],
            ),
            warehouse145_delivery_service_157,
            warehouse145_delivery_service_158,
            warehouse147_delivery_service_147147,
            warehouse166_delivery_service_169,
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=666, delivery_service_id=165, date_switch_time_infos=[date_switch_time_info(21)]
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=777, delivery_service_id=99, date_switch_time_infos=[date_switch_time_info(22)]
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=777, delivery_service_id=165, date_switch_time_infos=[date_switch_time_info(21)]
            ),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=1213,
                warehouse_to=145,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=18, region_to=225)],
                inbound_time=TimeInfo(1, 30),
                transfer_time=TimeInfo(4),
                operation_time=1,
            ),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(regions=[225], warehouse_with_priority=[WarehouseWithPriority(220, 100)])
        ]

        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.book_shop,
            _Shops.blue_shop_1,
            _Shops.blue_shop_1470,
            _Shops.blue_shop_2,
            _Shops.blue_shop_3,
            _Shops.dsbs_shop,
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='blue and green model',
                glparams=[
                    GLParam(param_id=101, value=1),
                    GLParam(param_id=102, value=1),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=104, value=1),
                ],
                parameter_value_links=[
                    ParameterValue(
                        201,
                        3,
                        ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_3/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1_201_3',
                        ),
                    ),
                    ParameterValue(
                        201,
                        2,
                        ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_2/orig',
                            namespace="get-mpic",
                            group_id='466729',
                            image_name='img_model1_201_2',
                        ),
                    ),
                ],
            ),
            Model(hyperid=2, hid=1, title='blue only model'),
            Model(hyperid=3, hid=1, title='not blue model'),
            Model(hyperid=5, hid=2, title='Model for jump table test'),
            Model(hyperid=90829, hid=90829, title='Books'),
            Model(hyperid=90831, hid=90831, title='Dictionaries'),
        ]

        cls.index.models += [
            Model(hyperid=14000, hid=15002),
        ]

        cls.index.gltypes += [
            GLType(param_id=101, hid=1, cluster_filter=False, gltype=GLType.ENUM, values=[1, 2]),
            GLType(param_id=102, hid=1, cluster_filter=False, gltype=GLType.NUMERIC),
            GLType(param_id=103, hid=1, cluster_filter=False, gltype=GLType.BOOL, hasboolno=False),
            GLType(param_id=104, hid=1, cluster_filter=False, gltype=GLType.BOOL, hasboolno=True),
            GLType(
                param_id=201,
                hid=1,
                cluster_filter=True,
                gltype=GLType.ENUM,
                subtype='image_picker',
                model_filter_index=0,
                values=[
                    GLValue(
                        1,
                        image=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_mbo_201_1/orig',
                            namespace="get-mpic",
                            group_id="466729",
                            image_name="img_mbo_201_1",
                        ),
                        position=1,
                    ),
                    3,
                    GLValue(
                        2,
                        image=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_mbo_201_2/orig',
                            namespace="get-mpic",
                            group_id="466729",
                            image_name="img_mbo_201_2",
                        ),
                        position=2,
                    ),
                ],
            ),
            GLType(param_id=202, hid=1, cluster_filter=True, model_filter_index=1, gltype=GLType.ENUM),
            GLType(param_id=203, hid=1, cluster_filter=True, model_filter_index=2, gltype=GLType.BOOL, hasboolno=False),
            GLType(param_id=204, hid=1, cluster_filter=True, model_filter_index=3, gltype=GLType.BOOL, hasboolno=True),
            GLType(param_id=205, hid=1, cluster_filter=True, model_filter_index=4, gltype=GLType.NUMERIC),
            # Фильтры, к которым не прикреплены оферы. У такого фильтра initialFound равен 0. Он должен быть скрыт с выдачи на синем прайме
            GLType(param_id=301, hid=1, cluster_filter=True, model_filter_index=5, gltype=GLType.ENUM, values=[1, 2]),
            # Фильтры для проверки неточного перехода
            GLType(param_id=201, hid=2, cluster_filter=True, model_filter_index=6, gltype=GLType.ENUM),
            GLType(param_id=205, hid=2, cluster_filter=True, model_filter_index=7, gltype=GLType.NUMERIC),
            GLType(param_id=204, hid=2, cluster_filter=True, model_filter_index=8, gltype=GLType.BOOL, hasboolno=True),
        ]

        cls.index.mskus += [
            MarketSku(
                title='RusEngDict',
                sku=908,
                waremd5='RusEngDict_IiLVm1goleg',
                hyperid=90831,
                blue_offers=[_Offers.offer_3],
                delivery_buckets=[813],
                randx=13,
            ),
            MarketSku(
                title="prohibited",
                hyperid=14000,
                sku=14,
                blue_offers=[_Offers.offer_1, _Offers.offer_2],
                randx=3500,
                delivery_buckets=[808, 800147],
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=213,
                children=[
                    Region(rid=123, children=[Region(rid=200, children=[Region(rid=100)])]),
                    Region(rid=234),
                    Region(rid=345),
                    Region(rid=456),
                    Region(
                        rid=26,
                        children=[
                            Region(
                                rid=977,
                                children=[
                                    Region(rid=121220),
                                ],
                            ),
                            Region(rid=39),
                        ],
                    ),
                    Region(rid=40),
                    Region(rid=2),
                    Region(rid=24),
                    Region(rid=969),
                    Region(rid=10867),
                    Region(rid=10876),
                    Region(rid=10870),
                    Region(rid=18),
                    Region(rid=14),
                    Region(rid=6),
                    Region(rid=192),
                    Region(rid=11),
                    Region(rid=15),
                ],
            ),
        ]

        cls.index.allowed_regions_for_books += [200]

        cls.index.hypertree += [HyperCategory(hid=15001, children=[HyperCategory(hid=15002)])]

        cls.index.prohibited_blue_offers += [
            ProhibitedBlueOffers(region_id=123, categories=[15001], is_soft=True),
            ProhibitedBlueOffers(region_id=100, mskus=[123456789]),
            ProhibitedBlueOffers(region_id=234, categories=[15002]),
            ProhibitedBlueOffers(region_id=345, mskus=[14]),
            ProhibitedBlueOffers(region_id=456, mskus=[14], allowed_warehouses=[147]),
            ProhibitedBlueOffers(region_id=26, categories=[15001], allowed_warehouses=[147]),
        ]

        std_options = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=801, fesh=1, carriers=[157], regional_options=std_options),
            DeliveryBucket(
                bucket_id=803,
                fesh=6,
                carriers=[165],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=804,
                fesh=1,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=805,
                fesh=7,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=806,
                fesh=7,
                carriers=[165],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=807,
                fesh=1,
                carriers=[158],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=808,
                fesh=4,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=800147,
                fesh=FF_1P_SHOP,
                carriers=[147147],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=811,
                fesh=10,
                carriers=[161],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=5, day_from=1, day_to=2)],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
            ),
            DeliveryBucket(
                bucket_id=813,
                fesh=577858,
                carriers=[163],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])],
            ),
        ]

    def test_prohibited_blue_offers(self):
        request_base = 'place=prime&hyperid=14000&rgb=blue&rearr-factors={}&rids='.format(
            make_rearr(market_use_warehouse_delivery_regions=0),
        )
        """Проверяется, что оффер откидывается для заданных регионов по причине запрещенности в них
        (по категории для rids=123 и rids=234, по msku для rids=345)
        Для региона 100 проверяется, что офферы категории 15001 фильтруюутся не зависимо от того,
        что для этого региона нет запрета этой категории (он есть для родительского региона 123)
        Для регионов 456 и 39 оффер склада 145 (Sku14Price1-IiLVm1GolQ) тоже скрыт,
        потому что склада 145 нет в белом списке складов"""
        for r in ['123', '100', '234', '345', '456', '39']:
            response = self.report.request_json(request_base + r)
            self.assertFragmentNotIn(response, {'wareId': 'Sku14Price1-IiLVm1GolQ'})

        """Для регионов 456 и 39 оффер склада 147 (Sku14Price2-IiLVm1GolQ) доступен,
        потому что склад 147 есть в белом списке складов:
        для региона 456 с правилом по msku,
        для региона 26 (родителя региона 39) - по категории
        Оффер 145 склада скрыт (хотя имеет меньшую цену, чем оффер 147 склада)"""
        for r in ['456', '39']:
            response = self.report.request_json(request_base + r)
            self.assertFragmentIn(response, {'wareId': 'Sku14Price2-IiLVm1GolQ'})
            self.assertFragmentNotIn(response, {'wareId': 'Sku14Price1-IiLVm1GolQ'})

        """Для региона 213 оффер 145 склада остается на выдаче (как самый дешевый)"""
        for r in ['213']:
            response = self.report.request_json(request_base + r)
            self.assertFragmentIn(response, {'wareId': 'Sku14Price1-IiLVm1GolQ'})

        """Для региона 100 словарь должен быть доступен так как разрешен его родительский регион"""
        response = self.report.request_json('place=prime&rids=100&hyperid=90831')
        self.assertFragmentIn(response, {'wareId': 'ReuEngDict___________g'})

        """Для другого региона словарь не доступен"""
        response = self.report.request_json('place=prime&rids=213&hyperid=90831&debug=da&debug-doc-count=10')
        self.assertFragmentNotIn(response, {'wareId': 'ReuEngDict___________g'})
        self.assertFragmentIn(
            response,
            {
                'METADOC_SKU': '908',
                'METADOC_FILTERING': "PROHIBITED_BOOKS_IN_REGION: 1",
                'DROP_REASON': 'CHILDLESS_METADOC',
            },
        )

    @classmethod
    def prepare_for_prohibited(cls):

        cls.index.regiontree += [Region(rid=93, name='Регион_39')]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=3, generation_id=3, warehouse_id=145),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                dc_bucket_id=16,
                fesh=7,
                carriers=[145],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=166, home_region=213, is_express=True),
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[145]),
            DynamicDeliveryServiceInfo(
                id=145,
                name='service from parent region',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=1)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=145, delivery_service_id=145),
        ]

        cls.index.hypertree += [HyperCategory(hid=161616)]
        cls.index.prohibited_blue_offers += [
            ProhibitedBlueOffers(region_id=213, categories=[161616]),
        ]
        cls.delivery_calc.on_request_offer_buckets(weight=16, width=16, height=16, length=16).respond([16], [16], [16])

    def test_actual_delivery_prohibited_blue_offers(self):
        """Проверяем что офферы вне индекса корректно фильтруются по запрещенным категориям"""
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rgb=blue'

        # в регионе 213 - запрет на категорию 161616
        response = self.report.request_json(request.format('fakeOffer_0:1;w:16;d:16x16x16;wh:145;hid:161616', 213))
        self.assertFragmentNotIn(
            response,
            {
                "fakeOffers": [
                    {
                        "wareId": "fakeOffer_0",
                    }
                ]
            },
        )
        self.assertFragmentIn(
            response,
            {
                "offerProblems": [{"problems": ["NONEXISTENT_OFFER"], "wareId": "fakeOffer_0"}],
            },
        )

        # а в регионе 93 - запрета нет, должен быть выдан не существующий в индексе оффер
        response = self.report.request_json(request.format('fakeOffer_0:1;w:16;d:16x16x16;wh:145;hid:161616', 93))
        self.assertFragmentIn(
            response,
            {
                "fakeOffers": [
                    {
                        "wareId": "fakeOffer_0",
                    }
                ]
            },
        )

        # 166 склад express - игнор скрытия
        response = self.report.request_json(request.format('fakeOffer_0:1;w:16;d:16x16x16;wh:166;hid:161616', 213))
        self.assertFragmentIn(
            response,
            {
                "fakeOffers": [
                    {
                        "wareId": "fakeOffer_0",
                    }
                ]
            },
        )

        # shop 42 - dsbs - игнор скрытия
        response = self.report.request_json(
            request.format('fakeOffer_0:1;w:16;d:16x16x16;supplier_id:42;wh:145;hid:161616', 213)
        )
        self.assertFragmentIn(
            response,
            {
                "fakeOffers": [
                    {
                        "wareId": "fakeOffer_0",
                    }
                ]
            },
        )

    @classmethod
    def prepare_check_delivery_available_prohibited_blue_offers(cls):

        cls.dynamic.nordstream += [
            DynamicWarehouseLink(1, [1]),
            DynamicWarehouseDelivery(1, {5005: [DynamicDeliveryRestriction(min_days=1, max_days=2)]}),
        ]

        cls.index.regiontree += [
            Region(rid=10714, region_type=Region.CITY, name='Город_А'),
            Region(rid=5005, name='parent', children=[Region(rid=10012, name='child')]),
        ]
        NON_ROOT_HID = 91299
        cls.index.hypertree += [
            HyperCategory(NON_ROOT_HID),
        ]
        cls.index.prohibited_blue_offers += [
            ProhibitedBlueOffers(region_id=10714, categories=[Const.ROOT_HID]),
            ProhibitedBlueOffers(region_id=5005, categories=[NON_ROOT_HID]),
        ]

    def test_check_delivery_available_prohibited_blue_offers(self):
        """
        Проверяется, что в регион, для которого в файле prohibited_blue_offers.json
        запрещена корневая категория товарного дерева (hid=90401), нет доставки.
        Если запрещена некорневая категория, то достaвка есть.
        """
        request = 'place=check_delivery_available&rgb=blue&rids=10714&rearr-factors=market_nordstream=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"deliveryAvailable": False})

        request = 'place=check_delivery_available&rgb=blue&rids=5005&rearr-factors=market_nordstream=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"deliveryAvailable": True})

    @classmethod
    def prepare_warehouse_delivery_regions(cls):
        cls.index.warehouse_delivery_regions += [
            WarehouseDeliveryRegions(warehouse=147, regions=[39]),  # 147 1p => доставка разрешена только в 39
        ]

    def test_warehouse_delivery_regions(self):
        """Проверяем что офер исчезает с выдачи, потому что правило выше разрешает только регион 39 для 147 1p."""
        Spec = namedtuple('Spec', ['region', 'feature', 'has_offer'])
        specs = [
            Spec(39, 1, True),
            Spec(39, 0, True),
            Spec(456, 1, False),  # нет доставки в 456 если включена фича
            Spec(456, 0, True),
        ]
        for spec in specs:
            request_base = 'place=prime&hyperid=14000&rgb=blue&rearr-factors={rearr}&rids={rids}&debug=1'.format(
                rearr=make_rearr(market_use_warehouse_delivery_regions=spec.feature),
                rids=spec.region,
            )
            response = self.report.request_json(request_base)
            param = {'wareId': 'Sku14Price2-IiLVm1GolQ'}
            if spec.has_offer:
                self.assertFragmentIn(response, param)
            else:
                self.assertFragmentNotIn(response, param)

    def test_soft_prohibited_blue_offers(self):
        request_base = 'place=prime&hyperid=14000&rgb=blue&rearr-factors={}&rids='.format(
            make_rearr(market_use_warehouse_delivery_regions=0, market_ignore_soft_prohibited_blue_offers=0),
        )
        # Проверяется, что оффер откидывается для заданных регионов по причине запрещенности в них.
        # Запрет по категории 15001 в регионах rids=123 (родительский) rids=100 (дочерний).
        # Запрет по категории 15002 в регионе rids=234.
        # Запрет по msku 14 в регионах rids=345, rids=456 и rids=39 (дочерний).
        for r in ['123', '100', '234', '345', '456', '39']:
            response = self.report.request_json(request_base + r)
            self.assertFragmentNotIn(response, {'wareId': 'Sku14Price1-IiLVm1GolQ'})

        request_base = 'place=prime&hyperid=14000&rgb=blue&rearr-factors={}&rids='.format(
            make_rearr(market_use_warehouse_delivery_regions=0, market_ignore_soft_prohibited_blue_offers=1),
        )
        # Для категории 15001 указана "мягкая" блокировка.
        # Поэтому запрет снимается в регионах rids=123 (родительский) rids=100 (дочерний).
        for r in ['123', '100']:
            response = self.report.request_json(request_base + r)
            self.assertFragmentIn(response, {'wareId': 'Sku14Price1-IiLVm1GolQ'})

        # Запреты для регионов rids=234, rids=345, rids=456 и rids=39 остаются без изменений.
        for r in ['234', '345', '456', '39']:
            response = self.report.request_json(request_base + r)
            self.assertFragmentNotIn(response, {'wareId': 'Sku14Price1-IiLVm1GolQ'})


if __name__ == '__main__':
    main()
