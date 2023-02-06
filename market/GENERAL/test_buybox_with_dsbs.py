#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicShop,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    GLParam,
    GLType,
    GLValue,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NotEmpty, ElementCount, Greater, Absent, Capture
from unittest import skip


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"

    return result


def disable_nordstream_buybox(rearr_flags):
    rearr_flags['market_nordstream_buybox'] = 0
    return rearr_flags


SEARCH_FILTERS = '&mcpricefrom=100&mcpriceto=800&&offer-shipping=store&manufacturer_warranty=1&qrfrom=4&free_delivery=1&home_region_filter=225&delivery_interval=2&fesh=18001,18002,18003,18004,18006,18007&show-book-now-only=1&filter-discount-only=1'  # noqa
SEARCH_GL_FILTER = '&glfilter=1203:1'
RED, GREEN, BLUE = 1, 2, 3

FREE_OPTION = DeliveryOption(price=0, day_from=1, day_to=1)


class T(TestCase):
    """
    Тесты для логики выбора dsbs на равне с md в байбоксе и ДО https://st.yandex-team.ru/MARKETMONEY-457
    """

    @classmethod
    def prepare(cls):
        cls.settings.put_white_cpa_offer_to_the_blue_shard = True
        # cls.index.creation_time = 1583830801 # Tue Mar 10 12:00:00 STD 2020 , full on individual min bids
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        cls.index.regiontree += [Region(rid=213, name='Нерезиновая')]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                regions=[225],
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
                regions=[225],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик клон Анатолия",
                warehouse_id=145,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                business_fesh=4,
                name="dsbs магазин Пети",
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                business_fesh=6,
                name="dsbs магазин клон Пети",
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                cpc=Shop.CPC_NO,
                priority_region=213,
            ),
            Shop(fesh=7, datafeed_id=7, name="cpc магазин Несчастный", priority_region=213, regions=[213]),
            Shop(
                fesh=8,
                datafeed_id=8,
                business_fesh=8,
                name="dsbs магазин клон клона клона Пети",
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                priority_region=213,
            ),
            Shop(
                fesh=9,
                datafeed_id=9,
                business_fesh=9,
                name="магазин без курьерской и региональной доставки",
                regions=[214],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                priority_region=214,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
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
                regional_options=[RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            ),
            DeliveryBucket(
                bucket_id=4240,
                fesh=6,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4241,
                fesh=8,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=15, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5004,
                fesh=9,
                options=[PickupOption(outlet_id=1), PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]

        cls.index.outlets += [
            Outlet(point_id=1, region=214, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=2, region=214, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.models += [
            Model(hid=1, ts=501, hyperid=1, title='model_1', vbid=10),
            Model(hid=2, ts=501, hyperid=2, title='model_2', vbid=10),
            Model(hid=4, ts=504, hyperid=4, title='model_4', vbid=10),  # 5 offers 1 good gmv
            Model(hid=5, ts=505, hyperid=5, title='model_5', vbid=10),  # 5 offers 2 md + 1 dsbs with good gmv
            Model(hid=6, ts=506, hyperid=6, title='model_6', vbid=10),  # 4 offers 2 md + 2 dsbs, elasticity ~ const
            Model(
                hid=7, ts=507, hyperid=7, title='model_7', vbid=10
            ),  # 3 offers 1 md + 2 dsbs, win with the best gmv and the least matrixnet
            Model(hid=8, ts=508, hyperid=8, title='model_8', vbid=10),  # duplicated offers (won by random)
            Model(
                hid=11, ts=511, hyperid=11, title='model_11', vbid=10
            ),  # 8 offers, test merging in top 6 by msku + supplier
            Model(hid=962050067, ts=962050067, hyperid=962050067, title='model_962050067'),
            Model(hid=962050068, ts=962050068, hyperid=962050068, title='model_962050068'),
            # YandexPlus models
            Model(hid=999042001, ts=999042001, hyperid=999042001, title='model_YandexPlus'),
        ]

        cls.index.mskus += [
            MarketSku(
                title="msku_1",
                hyperid=1,
                sku=100001,
                hid=1,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=100, feedid=2, waremd5="BLUE-100001-FEED-2222Q", ts=2),
                    BlueOffer(price=103, feedid=3, waremd5="BLUE-100001-FEED-3333g", ts=3),
                    BlueOffer(price=300, feedid=4, waremd5="BLUE-100001-FEED-4444w", ts=4),
                ],
            ),
            MarketSku(
                title="msku_2",
                hyperid=1,
                sku=100002,
                hid=1,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=100, feedid=2, waremd5="BLUE-100002-FEED-2222g", ts=5),
                    BlueOffer(price=103, feedid=3, waremd5="BLUE-100002-FEED-3333Q", ts=6),
                ],
            ),
            MarketSku(
                title="msku_3",
                hyperid=2,
                sku=200001,
                hid=2,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[BlueOffer(price=99, feedid=2, waremd5="BLUE-200001-FEED-2222g", ts=9)],
            ),
            MarketSku(
                title="msku_4",
                hyperid=4,
                sku=4,
                hid=4,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=129, feedid=2, waremd5="BLUE-4-2----FEED-XXXXg", ts=42),
                    BlueOffer(price=139, feedid=3, waremd5="BLUE-4-3----FEED-XXXXg", ts=43),
                    BlueOffer(price=149, feedid=4, waremd5="BLUE-4-4----FEED-XXXXg", ts=44),
                ],
            ),
            MarketSku(
                title="msku_5",
                hyperid=5,
                sku=5,
                hid=5,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=252, feedid=2, waremd5="BLUE-5-2----FEED-XXXXg", ts=52, business_id=10),
                    BlueOffer(price=153, feedid=3, waremd5="BLUE-5-3----FEED-XXXXg", ts=53, business_id=11),
                    BlueOffer(price=154, feedid=4, waremd5="BLUE-5-4----FEED-XXXXg", ts=54, business_id=12),
                ],
            ),
            MarketSku(
                title="msku_6",
                hyperid=6,
                sku=6,
                hid=6,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=500, demand_mean=400),
                    Elasticity(price_variant=600, demand_mean=399),
                    Elasticity(price_variant=700, demand_mean=398),
                ],
                blue_offers=[
                    BlueOffer(price=505, feedid=2, waremd5="BLUE-6-2----FEED-XXXXg", ts=62),
                    BlueOffer(price=506, feedid=4, waremd5="BLUE-6-3----FEED-XXXXg", ts=63),
                ],
            ),
            MarketSku(
                title="msku_7",
                hyperid=7,
                sku=7,
                hid=7,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=500, demand_mean=400),
                    Elasticity(price_variant=530, demand_mean=350),
                    Elasticity(price_variant=600, demand_mean=250),
                ],
                blue_offers=[BlueOffer(price=515, feedid=2, waremd5="BLUE-7-2----FEED-XXXXg", ts=72)],
            ),
            MarketSku(
                title="msku_8",
                hyperid=8,
                sku=8,
                hid=8,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=200, feedid=2, waremd5="BLUE-8-2----FEED-XXXXg", ts=82),
                    BlueOffer(price=200, feedid=2, waremd5="BLUE-8-3----FEED-XXXXg", ts=83),
                    BlueOffer(price=200, feedid=2, waremd5="BLUE-8-4----FEED-XXXXg", ts=84),
                ],
            ),
            MarketSku(
                title="msku_11",
                hyperid=11,
                sku=11,
                hid=11,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=200, feedid=2, waremd5="BLUE-112----FEED-XXXXg", ts=1102),
                    BlueOffer(price=200, feedid=2, waremd5="BLUE-113----FEED-XXXXg", ts=1103),
                    BlueOffer(price=200, feedid=3, waremd5="BLUE-114----FEED-XXXXg", ts=1104),
                    BlueOffer(price=200, feedid=3, waremd5="BLUE-115----FEED-XXXXg", ts=1105),
                ],
            ),
            MarketSku(
                title="msku_101324536886",
                hyperid=962050067,
                sku=101324536886,
                hid=962050067,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=2000, feedid=2, waremd5="Z2PF6gLhC-ct_2qXQPlMag", ts=1013245368862),
                    BlueOffer(price=1000, feedid=2, waremd5="Z2PF6gLhC-yt_2qXQPlMag", ts=1013245368862),
                ],
            ),
            MarketSku(
                title="msku_YandexPlus12",
                hyperid=999042001,
                sku=101371501752,
                hid=999042001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=1690, feedid=2, waremd5="Z2PF6gLhC-101371501752", ts=101371501752),
                ],
            ),
            MarketSku(
                title="msku_YandexPlus6",
                hyperid=999042001,
                sku=101371487758,
                hid=999042001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=980, feedid=2, waremd5="Z2PF6gLhC-101371487758", ts=101371487758),
                ],
            ),
            MarketSku(
                title="msku_YandexPlus3",
                hyperid=999042001,
                sku=101371493742,
                hid=999042001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=580, feedid=2, waremd5="Z2PF6gLhC-101371493742", ts=101371493742),
                ],
            ),
            MarketSku(
                title="msku_YandexPlus1",
                hyperid=999042001,
                sku=101371497748,
                hid=999042001,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=199, feedid=2, waremd5="Z2PF6gLhC-101371497748", ts=101371497748),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="market DSBS Offer",
                hid=1,
                hyperid=1,
                price=198,
                fesh=5,
                business_id=3,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-lbqQ',
                delivery_buckets=[1234],
            ),
            Offer(
                title="market DSBS Offer 2",
                hid=1,
                hyperid=1,
                price=99,
                fesh=6,
                business_id=3,
                sku=100002,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-222Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offer",
                ts=7,
            ),
            Offer(
                title="market DSBS Offer with pickup delivery only",
                hid=1,
                hyperid=1,
                price=99,
                fesh=9,
                business_id=3,
                sku=100002,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-xxxQ',
                feedid=9,
                delivery_buckets=[5004],
                offerid="proh.offer",
                ts=7,
            ),
            Offer(
                title="market DSBS Offer 3",
                hid=2,
                hyperid=2,
                price=106,
                fesh=6,
                business_id=3,
                sku=200001,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-333Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe2r",
                ts=8,
            ),
            Offer(
                title="market DSBS Offer 4-5",
                hid=4,
                hyperid=4,
                price=106,
                fesh=5,
                business_id=5,
                sku=4,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-4-5Q',
                feedid=5,
                delivery_buckets=[1234],
                offerid="proh.offe4-5",
                ts=45,
            ),
            Offer(
                title="market DSBS Offer 4-6",
                hid=4,
                hyperid=4,
                price=146,
                fesh=6,
                business_id=6,
                sku=4,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-4-6Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe4-6",
                ts=46,
            ),
            Offer(
                title="market DSBS Offer 5-5",
                hid=5,
                hyperid=5,
                price=155,
                fesh=5,
                business_id=5,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-5-5Q',
                feedid=5,
                delivery_buckets=[1234],
                offerid="proh.offe5-5",
                ts=55,
            ),
            Offer(
                title="market DSBS Offer 5-6",
                hid=5,
                hyperid=5,
                price=266,
                fesh=6,
                business_id=6,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-5-6Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe5-6",
                ts=56,
            ),
            Offer(
                title="market DSBS Offer 6-4",
                hid=6,
                hyperid=6,
                price=510,
                fesh=6,
                business_id=6,
                sku=6,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-6-4Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe6-4",
                ts=64,
            ),
            Offer(
                title="market DSBS Offer 6-5",
                hid=6,
                hyperid=6,
                price=512,
                fesh=6,
                business_id=6,
                sku=6,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-6-5Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe6-5",
                ts=65,
            ),
            Offer(
                title="market DSBS Offer 7-3",
                hid=7,
                hyperid=7,
                price=515,
                fesh=6,
                business_id=6,
                sku=7,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-7-3Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe7-3",
                ts=73,
            ),
            Offer(
                title="market DSBS Offer 7-4",
                hid=7,
                hyperid=7,
                price=500,
                fesh=8,
                business_id=8,
                sku=7,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-7-4Q',
                feedid=8,
                delivery_buckets=[4240],
                offerid="proh.offe7-4",
                ts=74,
            ),
            Offer(
                title="market DSBS Offer 116",
                hid=11,
                hyperid=11,
                price=200,
                fesh=6,
                business_id=6,
                sku=11,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-116Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe116",
                ts=1106,
            ),
            Offer(
                title="market DSBS Offer 117",
                hid=11,
                hyperid=11,
                price=200,
                fesh=6,
                business_id=6,
                sku=11,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-117Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe117",
                ts=1107,
            ),
            Offer(
                title="market DSBS Offer 118",
                hid=11,
                hyperid=11,
                price=200,
                fesh=8,
                business_id=8,
                sku=11,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-118Q',
                feedid=8,
                delivery_buckets=[4240],
                offerid="proh.offe118",
                ts=1108,
            ),
            Offer(
                title="market DSBS Offer 119",
                hid=11,
                hyperid=11,
                price=200,
                fesh=8,
                business_id=8,
                sku=11,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-119Q',
                feedid=8,
                delivery_buckets=[4240],
                offerid="proh.offe119",
                ts=1109,
            ),
            Offer(
                title="market DSBS Offer",
                hid=962050067,
                hyperid=962050067,
                price=100,
                fesh=5,
                business_id=3,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-120Q',
                delivery_buckets=[1234],
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 7).respond(0.003)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8).respond(0.003)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 45).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 46).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 72).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 73).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 74).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 82).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 83).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 84).respond(0.1)

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145]:
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                    },
                ),
            ]

    def test_default_offer_choose_dsbs_msku_case(self):
        """
        Тестирую, что в ДО выбирается dsbs по скору байбокса. В запросе есть параметр msku.
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&hyperid=1&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "sgf1xWYFqdGiLh4TT-222Q",
                    "debug": {"buyboxDebug": {"Offers": ElementCount(3), "Won": 1}},
                },
            )

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_dsbs_conversion_coef": 1,
            "prefer_do_with_sku": 1,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }
        test(rearr_flags_dict)

    def test_default_offer_choose_dsbs_msku_case_2(self):
        """
        Тестирую, что в ДО выбирается dsbs по скору байбокса. В запросе есть параметр msku.
        У dsbs оффера большой скор по формуле ДО, но он должен проиграть синему по скору байбокска и не попасть в ДО.
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=200001&rgb=green_with_blue&pp=6&hyperid=2&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "BLUE-200001-FEED-2222g",
                    "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(1)}},
                },
            )

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            "market_blue_buybox_max_price_rel_add_diff": 0,
        }
        test(rearr_flags_dict)

    def test_default_offer_choose_dsbs_msku_case_2_range_in_do_by_gmv(self):
        """
        Тестирую, что c флагами market_blue_buybox_filter_skip_if_product_offers=1 в ДО выбирается синий оффер по скору байбокса, а не dsbs поскольку он дороже.
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=200001&rgb=green_with_blue&pp=6&hyperid=2&rids=213&debug=da&offers-set=defaultList&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "BLUE-200001-FEED-2222g",
                    "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(1)}},
                },
            )

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 0,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_blue_buybox_max_price_rel_add_diff": 0,
        }
        test(rearr_flags_dict)

    def test_default_offer_choose_dsbs_msku_5_offers_range_in_do_by_gmv(self):
        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=4&rgb=green_with_blue&pp=6&hyperid=4&rids=213&debug=da&offers-set=defaultList&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;%s'
                % rearr_flags_str
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "BLUE-4-2----FEED-XXXXg",
                    "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(4)}},
                },
            )
            self.assertFragmentIn(
                response,
                "rel for doc sgf1xWYFqdGiLh4TT-4-5Q DELIVERY_TYPE=Country OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0",
            )
            self.assertFragmentIn(
                response,
                "rel for doc sgf1xWYFqdGiLh4TT-4-6Q DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0 PRICE=14600",
            )
            self.assertFragmentIn(
                response,
                "rel for doc BLUE-4-2----FEED-XXXXg DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=1 GMV_UE=14461",
            )
            self.assertFragmentIn(
                response,
                "rel for doc BLUE-4-3----FEED-XXXXg DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0 PRICE=13900",
            )
            self.assertFragmentIn(
                response,
                "rel for doc BLUE-4-4----FEED-XXXXg DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0 PRICE=14900",
            )
            self.assertFragmentIn(response, "IsCardRequest == 1")

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 0,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_blue_buybox_skip_detail_computation_if_one_offer_pass_filters": 0,
            "market_blue_add_rel_field_to_buybox_debug": 1,
            "market_blue_buybox_max_price_rel_add_diff": 0,
        }
        test(rearr_flags_dict)

    def test_default_offer_choose_dsbs_msku_5_offers_dsbs_wins_by_ml_range_in_do_by_gmv(self):
        """
        Тестирую, что c флагами market_blue_buybox_filter_skip_if_product_offers=1 в ДО выбирается синий оффер по скору байбокса, а не dsbs поскольку он дороже.
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=4&rgb=green_with_blue&pp=6&hyperid=4&rids=213&debug=da&offers-set=defaultList&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "sgf1xWYFqdGiLh4TT-4-6Q",
                    "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(4)}},
                },
            )

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 0,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 1,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 1,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 0,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_blue_buybox_max_price_rel_add_diff": 0,
        }
        test(rearr_flags_dict)

    def test_default_offer_choose_dsbs_msku_5_offers_2_exact_range_in_do_by_gmv(self):
        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&market-sku=5&rgb=green_with_blue&pp=6&hyperid=5&rids=213&debug=da&offers-set=defaultList&yandexuid=1&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "BLUE-5-3----FEED-XXXXg",
                    # resolve, why delivery for dsbs is 255 days here
                    #    "debug": {
                    #        "buyboxDebug": {
                    #            "Offers": ElementCount(3),
                    #            "RejectedOffers": ElementCount(2)
                    #        }
                    #    }
                },
            )
            self.assertFragmentIn(
                response,
                "rel for doc sgf1xWYFqdGiLh4TT-5-5Q DELIVERY_TYPE=Country OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0 PRICE=15500",
            )
            self.assertFragmentIn(
                response,
                "rel for doc sgf1xWYFqdGiLh4TT-5-6Q DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0 PRICE=26600",
            )
            self.assertFragmentIn(
                response,
                "rel for doc BLUE-5-2----FEED-XXXXg DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=0 PRICE=25200",
            )
            self.assertFragmentIn(
                response,
                "rel for doc BLUE-5-3----FEED-XXXXg DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=1 GMV_UE=14433",
            )
            self.assertFragmentIn(
                response,
                "rel for doc BLUE-5-4----FEED-XXXXg DELIVERY_TYPE=Priority OFFER_TYPE_PRIORITY=PAID_DSBS IS_EXACT_GMV_UE=1 GMV_UE=14425",
            )
            self.assertFragmentIn(
                response,  # check 255 days delivery impact on conversion coeff (treat 255 days as no information about delivery days)
                {
                    "Offer": {
                        "WareMd5": "sgf1xWYFqdGiLh4TT-5-5Q",
                        "DeliveryContext": {"CourierDaysMin": 255, "CourierDaysMax": 255},
                        "ConversionByDeliveryDayCoef": 1,
                    }
                },
            )

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 0,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_blue_add_rel_field_to_buybox_debug": 1,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }
        rearr_flags_dict = disable_nordstream_buybox(rearr_flags_dict)
        test(rearr_flags_dict)

    def test_default_offer_choose_dsbs_model_case(self):
        """
        Тестирую, что в ДО выбирается dsbs по скору байбокса. Нет параметра msku.
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=productoffers&rgb=green_with_blue&pp=6&hyperid=1&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "benefit": {"type": "cheapest"},
                    'wareId': "sgf1xWYFqdGiLh4TT-222Q",
                    "debug": {"buyboxDebug": {"Offers": ElementCount(3), "Won": 1}},
                },
            )

        """
        Флаг market_blue_buybox_with_dsbs_white
        """
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_dsbs_conversion_coef": 1,
            "prefer_do_with_sku": 1,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }

        test(rearr_flags_dict)

    def test_prime_dsbs_buybox(self):
        """
        Тестирую, что ДО в прайме выбирается dsbs и dsbs был байбоксе
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            response = self.report.request_json(
                'place=prime&hid=1&rgb=green_with_blue&pp=7&use-default-offers=1&allow-collapsing=1&hyperid=1&rids=213&debug=da&rearr-factors=%s'
                % rearr_flags_str
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        "results": [
                            {
                                'entity': 'product',
                                'slug': 'model-1',
                                'type': 'model',
                                'offers': {
                                    'items': [
                                        {
                                            'entity': "offer",
                                            'wareId': "sgf1xWYFqdGiLh4TT-222Q",
                                            "debug": {"buyboxDebug": {"Offers": ElementCount(3), "Won": 1}},
                                        }
                                    ]
                                },
                            }
                        ]
                    }
                },
            )

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_dsbs_conversion_coef": 1,
            "prefer_do_with_sku": 1,
            "market_metadoc_search": "no",
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }
        test(rearr_flags_dict)

    @classmethod
    def prepare_filters(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=13,
                output_type=HyperCategoryType.SIMPLE,
                children=[
                    HyperCategory(hid=3, output_type=HyperCategoryType.GURU),
                ],
            )
        ]

        cls.index.gltypes += [
            GLType(
                hid=3,
                param_id=101,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[
                    GLValue(value_id=1, text='value1'),
                    GLValue(value_id=2, text='value2'),
                ],
                model_filter_index=1,
                xslname='sku_filter',
            )
        ]

        cls.index.models += [Model(hid=3, ts=501, hyperid=3, title='model_3')]

        cls.index.mskus += [
            MarketSku(
                title="msku_3",
                hyperid=3,
                glparams=[GLParam(param_id=101, value=1)],
                sku=300001,
                hid=3,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[BlueOffer(price=106, price_old=200, feedid=2, waremd5="BLUE-300001-FEED-1111g", ts=9)],
            )
        ]

        cls.index.offers += [
            Offer(
                title="market DSBS no warranty",
                hid=3,
                hyperid=3,
                price=99,
                fesh=6,
                business_id=3,
                sku=300001,
                manufacturer_warranty=False,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-444Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe2r",
                cmagic='f2dfc75bbd15ae22fbd2e35b21675aab',
                ts=8,
            ),
            Offer(
                title='CPC with gl',
                price=800,
                fesh=7,
                feedid=7,
                hid=3,
                hyperid=3,
                sku=300001,
                glparams=[GLParam(param_id=101, value=1)],
                waremd5='sgf1xWYFqdGiLh4TT-CPCQ',
                ts=10,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 10).respond(0.006)

    def test_filters_before_buybox(self):
        """
        Тестируем, что под флагом market_rel_filters_before_buybox=1 фильтры в productoffers применяются до байбокса.
        Без флага должны получить пустую выдачу, так как победитель байбокса фильтруется.
        """

        request_base = 'place=productoffers&market-sku=300001&rgb=green_with_blue&pp=6&hid=3&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'

        def request_with_warranty_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(('manufacturer_warranty=1&' + request_base) % _rearr_flags_str)

        def request_with_discounts_only_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(('filter-discount-only=1&' + request_base) % _rearr_flags_str)

        def request_no_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(request_base % _rearr_flags_str)

        def request_gl_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json('glfilter=101:1&' + request_base % _rearr_flags_str)

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
            "market_blue_buybox_max_price_rel_add_diff": 0,
        }

        """
        Без флага market_rel_filters_before_buybox и фильров в запросе побеждает оффер "market DSBS no warranty"
        """
        response = request_no_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "sgf1xWYFqdGiLh4TT-444Q",
                "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(1)}},
            },
        )

        """
        При добавлении фильтра 'manufacturer_warranty=1&' выдается cpc оффер, так как победитель байбокса фильтруется
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 0
        response = request_with_warranty_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "sgf1xWYFqdGiLh4TT-CPCQ",
            },
        )

        """
        С флагом market_rel_filters_before_buybox побеждает синий оффер, dsbs отфильтровался до байбокса
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 1
        response = request_with_warranty_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "BLUE-300001-FEED-1111g",
                "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(0)}},
            },
        )

        """
        При добавлении фильтра gl фильтра выдается cpc оффер, так как байбоксы фильтруются
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 0
        response = request_gl_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "sgf1xWYFqdGiLh4TT-CPCQ",
            },
        )

        """
        С флагом market_rel_filters_before_buybox побеждает синий оффер, dsbs отфильтровался до байбокса
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 1
        response = request_gl_filter(rearr_flags_dict)

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "BLUE-300001-FEED-1111g",
                "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(0)}},
            },
        )

        """
        При добавлении фильтра filter-discount-only выдается пустой результат
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 0
        response = request_with_discounts_only_filter(rearr_flags_dict)
        self.assertFragmentIn(response, {'results': []})

        """
        С флагом filter-discount-only побеждает синий оффер, dsbs отфильтровался до байбокса
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 1
        response = request_with_discounts_only_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "BLUE-300001-FEED-1111g",
                "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(0)}},
            },
        )

        self.dynamic.market_dynamic.disabled_blue_suppliers += [
            DynamicShop(6),
            DynamicShop(8),
        ]

        """
        "market DSBS no warranty" фильтруется по динамику
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 0
        response = request_no_filter(rearr_flags_dict)
        self.assertFragmentIn(response, {'results': []})

        """
        С флагом market_rel_filters_before_buybox побеждает синий оффер, dsbs отфильтровался до байбокса по динамику
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 1
        response = request_no_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "BLUE-300001-FEED-1111g",
                "debug": {"buyboxDebug": {"Offers": ElementCount(1), "RejectedOffers": ElementCount(0)}},
            },
        )

    @classmethod
    def prepare_filter_no_sku(cls):
        cls.index.models += [Model(hid=3, ts=501, hyperid=10, title='model_5')]
        cls.index.offers += [
            Offer(
                title="market DSBS warranty",
                hid=3,
                hyperid=10,
                price=99,
                fesh=6,
                business_id=6,
                manufacturer_warranty=True,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-100Q',
                feedid=6,
                delivery_buckets=[4240],
                offerid="proh.offe2r",
                cmagic='f2dfc75bbd15ae22fbd2e35b21675aab',
                ts=11,
            ),
            Offer(
                title="market DSBS no warranty",
                hid=3,
                hyperid=10,
                price=99,
                fesh=8,
                business_id=8,
                manufacturer_warranty=False,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-101Q',
                feedid=8,
                delivery_buckets=[4241],
                cmagic='f2dfc75bbd15ae22fbd2e35b21675aab',
                ts=12,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 11).respond(0.002)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 12).respond(0.003)

    def test_filters_before_buybox_no_sku_case(self):
        """
        Проверяем, что при фильтрации до байбокса, фильтрация работает корректно для dsbs без msku
        """

        request_base = 'place=productoffers&rgb=green_with_blue&pp=6&hyperid=10&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'

        def request_with_warranty_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(('manufacturer_warranty=1&' + request_base) % _rearr_flags_str)

        def request_no_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(request_base % _rearr_flags_str)

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
        }

        """
        Без фильтров побеждает оффер с самым большим скором "sgf1xWYFqdGiLh4TT-101Q"
        """
        response = request_no_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "sgf1xWYFqdGiLh4TT-101Q",
            },
        )

        """
        Без флага market_rel_filters_before_buybox и с фильтром по гарантии, побеждает оффер c гарантией, но худшим скором
        """
        response = request_with_warranty_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": NotEmpty()},
                'wareId': "sgf1xWYFqdGiLh4TT-100Q",
            },
        )

        """
        С флагом market_rel_filters_before_buybox и с фильтром по гарантии, побеждает оффер c гарантией, но худшим скором
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 1
        response = request_with_warranty_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "benefit": {"type": "cheapest"},
                'wareId': "sgf1xWYFqdGiLh4TT-100Q",
            },
        )

    def test_search_traces(self):
        """
        Проверяем трейсы поиска в product offers https://st.yandex-team.ru/MARKETOUT-40959
        """
        request_base = 'place=productoffers&rgb=green_with_blue&pp=6&hyperid=10&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'

        def request_with_warranty_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(('manufacturer_warranty=1&' + request_base) % _rearr_flags_str)

        def request_no_filter(_rearr_flags_dict):
            _rearr_flags_str = dict_to_rearr(_rearr_flags_dict)
            return self.report.request_json(request_base % _rearr_flags_str)

        # Сначала проверяем трейсы поиска для моделей
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
            "market_documents_search_trace_default_offer": 10,  # документы, для которых нужны трейсы поиска: hyperid=10
        }

        # Для нескольких стадий: index, accept_doc, ... ответ один и тот же - офферы сохраняются до стадии rearrange, дальше остаётся только победитель
        offers_in_stage = {
            "count": 2,
            "examples": [
                "sgf1xWYFqdGiLh4TT-101Q",
                "sgf1xWYFqdGiLh4TT-100Q",
            ],
        }
        offers_in_stage_filtered = {
            "count": 1,
            "examples": [
                "sgf1xWYFqdGiLh4TT-100Q",  # Этот оффер пройдёт фильтр
            ],
        }

        # Прогоняем запрос с фильтром и без - в них побеждают разные офферы
        for request_getter, winner, offers_since_relevance in zip(
            [request_no_filter, request_with_warranty_filter],
            ["sgf1xWYFqdGiLh4TT-101Q", "sgf1xWYFqdGiLh4TT-100Q"],
            [offers_in_stage, offers_in_stage_filtered],
        ):
            response = request_getter(rearr_flags_dict)
            self.assertFragmentIn(
                response,
                {
                    "docs_search_trace_default_offer": {
                        "traces": [
                            {
                                "document": "10",
                                "type": "MODEL",
                                "in_index": True,
                                "in_accept_doc": False,  # Трейс запросили для модели, а не для оффера, поэтому False
                                "on_page": False,  # Трейс запросили для модели, а не для оффера, поэтому False
                                "stats_for_model": {
                                    "model_id": 10,
                                    "offers_in_index": offers_in_stage,
                                    "offers_in_accept_doc": {"1": offers_in_stage},
                                    # Без фильтров тут всё ещё 2 оффера, но с фильтрами в relevance попадает только 1 (другой фильтруется)
                                    "offers_in_relevance": {"1": offers_since_relevance},
                                    "offers_passed_relevance": {"1": offers_since_relevance},
                                    "offers_in_rearrange": {
                                        "1": {
                                            "count": 1,
                                            "examples": [
                                                winner,  # Победитель байбокса
                                            ],
                                        },
                                    },
                                },
                            },
                        ],
                    },
                },
            )

        # Проверяем трейсы поиска для офферов (для офферов трейсы немного другие)
        rearr_flags_dict[
            "market_documents_search_trace_default_offer"
        ] = "sgf1xWYFqdGiLh4TT-101Q,sgf1xWYFqdGiLh4TT-100Q"  # waremd5 офферов через запятую
        # Запрос с фильтром по гарантии - 1 оффер отфильтруется, 1 останется и выиграет байбокс
        response = request_with_warranty_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_default_offer": {
                    "traces": [
                        {
                            "document": "sgf1xWYFqdGiLh4TT-101Q",
                            "type": "OFFER_BY_WARE_MD5",
                            "in_index": True,
                            "in_accept_doc": True,
                            "passed_accept_doc": False,  # отфильтровался фильтром по гарантии
                            "accept_doc_filtered_reason": "WARRANTY",
                            "in_relevance": Absent(),  # стадия passed_accept_doc не пройдена, все последующие стадии не пишутся в ответ
                        },
                        {
                            "document": "sgf1xWYFqdGiLh4TT-100Q",
                            "type": "OFFER_BY_WARE_MD5",
                            "in_index": True,
                            "in_accept_doc": True,
                            "passed_accept_doc": True,
                            "in_relevance": True,
                            "passed_relevance": True,
                            "in_rearrange": True,
                            "on_page": True,  # Победил байбокс
                            "passed_rearrange": True,
                        },
                    ],
                },
            },
        )
        # Запрос без фильтров - оба оффера попадут в байбокс, выиграет байбокс только один
        response = request_no_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_default_offer": {
                    "traces": [
                        {
                            "document": "sgf1xWYFqdGiLh4TT-101Q",
                            "type": "OFFER_BY_WARE_MD5",
                            "in_index": True,
                            "in_accept_doc": True,
                            "passed_accept_doc": True,
                            "in_relevance": True,
                            "passed_relevance": True,
                            "in_rearrange": True,
                            "on_page": True,
                            "passed_rearrange": True,  # выиграл байбокс
                        },
                        {
                            "document": "sgf1xWYFqdGiLh4TT-100Q",
                            "type": "OFFER_BY_WARE_MD5",
                            "in_index": True,
                            "in_accept_doc": True,
                            "passed_accept_doc": True,
                            "in_relevance": True,
                            "passed_relevance": True,
                            "in_rearrange": False,  # проиграл байбокс, не попал в ответ плейса product default offer
                            "on_page": Absent(),  # стадия in_rearrange не пройдена, все последующие стадии не пишутся в ответ
                            "passed_rearrange": Absent(),
                        },
                    ],
                },
            },
        )
        # Запрос с msku
        # Ответ практически бессмысленный, так как productoffers не вызывает AcceptDocWithHits для msku, соответственно, трейс не заполнится полезной информацией
        request_base = 'place=productoffers&market-sku=300001&rgb=green_with_blue&pp=6&hid=3&hyperid=3&rids=213&debug=da&offers-set=defaultList,listCpa&rearr-factors=%s'
        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
            "market_documents_search_trace_default_offer": 300001,  # документы, для которых нужны трейсы поиска - msku=300001
        }

        """
        Из теста выше: при добавлении фильтра 'manufacturer_warranty=1&' выдается cpc оффер, так как победитель байбокса фильтруется
        """
        rearr_flags_dict['market_rel_filters_before_buybox'] = 0
        response = request_with_warranty_filter(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_default_offer": {
                    "traces": [
                        {
                            # Для msku трейсы в productoffers смысл не несут
                            "document": "300001",
                            "type": "MODEL",
                            "in_index": False,
                        },
                    ],
                },
            },
        )

    def test_won_by_random(self):
        """
        Проверяю, что в ДО выбирается оффер по рандому (офферы с равными GMV), при этом причина выбора (рандом) логируется
        """

        def get_response(rearr_flags, yandex_uid):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            return self.report.request_json(
                'place=productoffers&market-sku=8&rgb=green_with_blue&pp=6&hyperid=8&rids=213&debug=da&allow_collapsing=0&yandexuid={}&offers-set=defaultList&rearr-factors=%s'.format(
                    yandex_uid
                )
                % rearr_flags_str
            )

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_operational_rating_everywhere": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_default_offer_by_random": 1,  # choose DO by random (offers have same GMV)
            "get_rid_of_direct_shipping": 0,
        }
        # yandex_uid = 1
        rearr_flags_dict = disable_nordstream_buybox(rearr_flags_dict)
        response = get_response(rearr_flags_dict, 1)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-8-3----FEED-XXXXg",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-8-3----FEED-XXXXg",
                                        "IsWinnerByRandom": True,
                                        "IsLoserByRandom": False,
                                        "SeedForGmvRandomization": 53955,
                                        "GmvRandomizationAdditive": -30.0176,
                                    },
                                    {
                                        "WareMd5": "BLUE-8-4----FEED-XXXXg",
                                        "IsWinnerByRandom": False,
                                        "IsLoserByRandom": True,
                                        "SeedForGmvRandomization": 50218,
                                        "GmvRandomizationAdditive": -39.7051,
                                    },
                                ],
                            }
                        },
                    }
                ]
            },
        )
        # yandex_uid = 2, для этого пользователя рандомно выигрывает другой оффер
        response = get_response(rearr_flags_dict, 2)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-8-4----FEED-XXXXg",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-8-3----FEED-XXXXg",
                                        "IsWinnerByRandom": False,
                                        "IsLoserByRandom": True,
                                        "SeedForGmvRandomization": 42650,
                                        "GmvRandomizationAdditive": -59.3223,
                                    },
                                    {
                                        "WareMd5": "BLUE-8-4----FEED-XXXXg",
                                        "IsWinnerByRandom": True,
                                        "IsLoserByRandom": False,
                                        "SeedForGmvRandomization": 45171,
                                        "GmvRandomizationAdditive": -52.7871,
                                    },
                                ],
                            }
                        },
                    }
                ]
            },
        )
        # yandex_uid = 1, проверяем, что одному и тому же пользователю возвращается одинаковая выдача
        response = get_response(rearr_flags_dict, 1)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-8-3----FEED-XXXXg",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-8-3----FEED-XXXXg",
                                        "SeedForGmvRandomization": 53955,
                                        "GmvRandomizationAdditive": -30.0176,
                                    }
                                ]
                            }
                        },
                    }
                ]
            },
        )

    def test_won_by_random_on_search(self):
        """
        Проверяю, что в случае победы по рандому на поиске (prime, blender) логируются необходимые поля
        """

        def get_response(rearr_flags, yandex_uid, blender_flag):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            return self.report.request_json(
                'place=prime{}&text=msku_8&market-sku=8&pp=7&rids=213&debug=da&use-default-offers=1&allow-collapsing=1&yandexuid={}&rearr-factors=%s'.format(
                    blender_flag, yandex_uid
                )
                % rearr_flags_str
            )

        rearr_flags_dict = {
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_default_offer_by_random": 1,  # choose DO by random (offers have the same GMV)
            "market_buybox_auction_search_sponsored_places_web": 0,
        }
        rearr_flags_dict = disable_nordstream_buybox(rearr_flags_dict)
        for blender_flag in ['', '&blender=1']:
            response = get_response(
                rearr_flags_dict, yandex_uid=1, blender_flag=blender_flag
            )  # yandex_uid=1 для воспроизводимости
            self.assertFragmentIn(response, {"WareMd5": "BLUE-8-3----FEED-XXXXg", "IsWinnerByRandom": True})
            self.assertFragmentIn(response, {"WareMd5": "BLUE-8-4----FEED-XXXXg", "IsLoserByRandom": True})
        # Запись в логах 2 раза - за поиск в prime и за поиск в blender
        self.show_log.expect(
            pp=7,
            ware_md5="BLUE-8-3----FEED-XXXXg",
            won_method=1,
            url_type=6,
            is_local_warehouse=0,
            do_type=0,
            is_winner_by_random=1,
        ).times(2)

    @classmethod
    def prepare_buybox_no_delivery_prime(cls):
        cls.index.models += [Model(hid=9, ts=901, hyperid=9, title='model_9')]

        cls.index.mskus += [
            MarketSku(
                title="msku_9",
                hyperid=9,
                sku=900001,
                hid=9,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=100),
                    Elasticity(price_variant=105, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(title="veryvery offer 1", price=101, feedid=2, waremd5="BLUE-900001-FEED-2221g", ts=902),
                    BlueOffer(title="veryvery offer 2", price=105, feedid=3, waremd5="BLUE-900001-FEED-2222g", ts=903),
                ],
            )
        ]

    def test_buybox_no_delivery_prime_no_flag(self):
        """
        Тестирую флаг market_buybox_no_delivery_prime . При его включении для все офферов в байбоксе на прайме ConversionByDeliveryDayCoef
        должен быть равен одному.
        """

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_operational_rating_everywhere": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
            "market_buybox_no_delivery_prime": 0,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }

        base_request = 'place=prime&pp=7&debug=da&numdoc=20&use-default-offers=0&allow-collapsing=0&text=veryvery&hid=9&rgb=green_with_blue&rids=213&debug=da&rearr-factors=market_metadoc_search=no&rearr-factors=%s'  # noqa

        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [{"WareMd5": "BLUE-900001-FEED-2221g", "ConversionByDeliveryDayCoef": 1.13}]
                    }
                }
            },
        )

        rearr_flags_dict["market_buybox_no_delivery_prime"] = 1
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [{"WareMd5": "BLUE-900001-FEED-2221g", "ConversionByDeliveryDayCoef": 1}],
                        'WonMethod': 'WON_BY_EXCHANGE',
                    }
                }
            },
        )

        rearr_flags_dict["market_buybox_allow_new_algo_no_delivery"] = 1
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [
                            {"Gmv": Greater(0), "WareMd5": "BLUE-900001-FEED-2221g", "ConversionByDeliveryDayCoef": 1}
                        ],
                        'WonMethod': "WON_BY_EXCHANGE",
                    }
                }
            },
        )

    def test_won_by_gmv_w_slight_changing_elasticity(self):
        """
        Проверяем, что побеждает самый дорогой dsbs оффер, так как эластичность почти не меняется
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            return self.report.request_json(
                'place=productoffers&market-sku=6&rgb=green_with_blue&pp=6&hyperid=6&rids=213&debug=da&offers-set=defaultList&yandexuid=1&rearr-factors=%s'
                % rearr_flags_str
            )

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_operational_rating_everywhere": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_blue_buybox_dsbs_conversion_coef": 1,
            "market_rel_filters_before_buybox": 0,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }

        rearr_flags_dict = disable_nordstream_buybox(rearr_flags_dict)
        response = test(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "sgf1xWYFqdGiLh4TT-6-5Q",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-6-4Q", "GmvUeToRelField": 17772, "PriceAfterCashback": 510},
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-6-5Q", "GmvUeToRelField": 17785, "PriceAfterCashback": 512},
                            {"WareMd5": "BLUE-6-2----FEED-XXXXg", "GmvUeToRelField": 17696, "PriceAfterCashback": 505},
                            {"WareMd5": "BLUE-6-3----FEED-XXXXg", "GmvUeToRelField": 17698, "PriceAfterCashback": 506},
                        ],
                    }
                },
            },
        )

        # Проверяем, что логируется WON_METHOD (логируется числами, WON_BY_EXCHANGE соответствует 1)
        # do_type=4(Cheapest)
        self.show_log.expect(pp=200, won_method=1, do_type=4)

    def test_won_by_gmv_least_matrixnet(self):
        """
        Проверяем, что побеждает dsbs оффер по GMV, но у него самый низкий matrixnet score
        """

        def test(rearr_flags_dict):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            return self.report.request_json(
                'place=productoffers&market-sku=7&rgb=green_with_blue&pp=6&hyperid=7&rids=213&debug=da&offers-set=defaultList&yandexuid=1&rearr-factors=%s'
                % rearr_flags_str
            )

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_operational_rating_everywhere": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "market_blue_buybox_with_dsbs_white_do": 0,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 0,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
            "market_blue_buybox_dsbs_conversion_coef": 1,
            "market_rel_filters_before_buybox": 0,
            "get_rid_of_direct_shipping": 0,
        }

        rearr_flags_dict = disable_nordstream_buybox(rearr_flags_dict)
        response = test(rearr_flags_dict)
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "sgf1xWYFqdGiLh4TT-7-4Q",
                "debug": {
                    "buyboxDebug": {
                        "WonMethod": "WON_BY_EXCHANGE",
                        "Offers": [
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-7-4Q"},
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-7-3Q"},
                            {"WareMd5": "BLUE-7-2----FEED-XXXXg"},
                        ],
                    }
                },
            },
        )

    def test_merge_offers_by_msku_supplier(self):
        """
        Проверка на схлопывание по поставщику и мскю в топ 6
        """

        def get_response(rearr_flags):
            rearr_flags_str = dict_to_rearr(rearr_flags_dict)
            return self.report.request_json(
                'place=productoffers&market-sku=11&rgb=green_with_blue&pp=6&hyperid=11&rids=213&debug=da&allow_collapsing=0&yandexuid=1&rearr-factors=%s'
                % rearr_flags_str
            )

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_operational_rating_everywhere": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white": 1,
            "prefer_do_with_sku": 1,
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_blue_buybox_use_gmv_in_rel_fields": 1,
            "market_blue_buybox_gvm_ue_rand_low": 0.99,
            "market_blue_buybox_gvm_ue_rand_delta": 0.01,
            "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef": 1000,
            "market_blue_buybox_default_elasticity": 1,
            "market_blue_buybox_always_compute_approx_gmv_ue": 0,
            "market_blue_buybox_filter_skip_always": 0,
            "market_blue_buybox_filter_skip_if_product_offers": 1,
            "market_blue_buybox_if_no_exact_gmv_use_do": 0,
            "market_blue_buybox_if_no_exact_gmv_use_approx_gmv": 1,
            "market_blue_buybox_if_no_exact_gmv_use_price": 1,
            "market_blue_buybox_range_by_gmv_div_price": 0,
        }
        response = get_response(rearr_flags_dict)
        # Офферы одного поставщика одной msku должны были схлопнуться в топ 6
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-112----FEED-XXXXg",
                    },
                    {
                        "entity": "offer",
                        "wareId": "BLUE-114----FEED-XXXXg",
                    },
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-116Q",
                    },
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-119Q",
                    },
                ]
            },
        )
        # Аналогичные офферы не должны показываться из-за схлопывания
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-113----FEED-XXXXg",
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-115----FEED-XXXXg",
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-117Q",
                    }
                ]
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "sgf1xWYFqdGiLh4TT-118Q",
                    }
                ]
            },
        )

    @skip('Выставил market_alisa_priority_msku = 0')
    def test_priority_msku(self):
        """
        Тестирую флаг market_alisa_priority_msku.
        """

        response = self.report.request_json(
            'place=productoffers&market-sku=101324536886&rgb=green_with_blue&pp=6&offers-set=defaultList&hyperid=962050067&rids=213'
        )
        self.assertFragmentIn(response, {"wareId": "Z2PF6gLhC-ct_2qXQPlMag"})

    def test_yandex_plus_priority_msku(self):
        """
        Тестирую флаг market_yandex_plus_priority_msku.
        """

        rearr_flags_dict = {"market_yandex_plus_priority_msku": 1}
        base_request = 'place=prime&pp=7&rids=213&text=YandexPlus&numdoc=8&use-default-offers=1&rearr-factors=%s'
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))

        self.assertFragmentIn(response, {"wareId": "Z2PF6gLhC-10137150175w", "slug": "msku-yandexplus12"})

    def test_buybox_non_courier_delivery_coef(self):
        """
        Тестирую флаг market_blue_buybox_non_courier_delivery_conversion_coef. При его включении у всех офферов без курьерской доставки ConversionByDeliveryDayCoef
        должен быть равен market_blue_buybox_non_courier_delivery_conversion_coef.
        """

        rearr_flags_dict = {
            "market_blue_buybox_courier_delivery_priority": 0,
            "market_blue_buybox_non_courier_delivery_conversion_coef": 1,
        }

        base_request = 'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&debug=da&offers-set=defaultList&rearr-factors=%s'
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-222Q", "ConversionByDeliveryDayCoef": 1.302},
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-xxxQ", "ConversionByDeliveryDayCoef": 1},
                        ],
                    }
                }
            },
        )

        rearr_flags_dict["market_blue_buybox_non_courier_delivery_conversion_coef"] = 0.123
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-222Q", "ConversionByDeliveryDayCoef": 1.302},
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-xxxQ", "ConversionByDeliveryDayCoef": 0.123},
                        ],
                    }
                }
            },
        )

        """
        Тестируем жесткую деприоритезацию офферов без курьерской доставки при наличии офферов с курьеркой на msku
        """

        rearr_flags_dict = {
            "market_blue_buybox_non_courier_delivery_conversion_coef": 1,
            "market_blue_buybox_courier_delivery_priority": 0,
        }

        base_request = 'place=productoffers&market-sku=100002&rgb=green_with_blue&pp=6&debug=da&offers-set=defaultList&rearr-factors=%s'
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-222Q", "ConversionByDeliveryDayCoef": 1.302},
                            {"WareMd5": "sgf1xWYFqdGiLh4TT-xxxQ", "ConversionByDeliveryDayCoef": 1},
                        ],
                    }
                }
            },
        )

        rearr_flags_dict["market_blue_buybox_courier_delivery_priority"] = 1
        # в данном случае все оффера без курьерки должны отфильтроваться
        response = self.report.request_json(base_request % dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "buyboxDebug": {
                        "Offers": [{"WareMd5": "sgf1xWYFqdGiLh4TT-222Q", "ConversionByDeliveryDayCoef": 1.302}],
                        "RejectedOffers": [
                            {
                                'Offer': {"WareMd5": "sgf1xWYFqdGiLh4TT-xxxQ"},
                                "RejectReason": "COURIER_DELIVERY_PRIORITY",
                            }
                        ],
                    }
                }
            },
        )

    def test_delivery_type_coefs_in_response_no_business(self):
        """
        Проверяем флаги - множители конверсии для GMV в байбоксе, для каждого типа доставки свой флаг.
        Проверяем, что множители, переданные флагами, корректно попадают в выдачу.
        Аналогичные флаги, но для байбокса по бизнес-id проверяются тесте test_delivery_type_coefs_in_response_business
        """

        coeffs = {  # Берём разные коэффициенты, чтобы понимать, верно ли берётся коэффициент для своего типа доставки
            "ff": 1.1,
            "dropship": 1.2,
            "dsbs": 1.05,
        }

        def get_coeff_type(is_dsbs, is_fulfillment):
            if is_dsbs:
                return "dsbs"
            elif is_fulfillment:
                return "ff"
            return "dropship"

        has_delivery_type = {  # Словарь, чтобы проверить, что тестом были охвачены все типы доставок
            "ff": False,
            "dropship": False,
            "dsbs": False,
        }

        rearr_flags_dict = {
            "market_buybox_delivery_type_in_conv_coef_write_trace": 1,
            "market_blue_buybox_courier_delivery_priority": 0,
            "market_buybox_fulfillment_in_conv_coef": coeffs["ff"],
            "market_buybox_dropship_in_conv_coef": coeffs["dropship"],
            "market_blue_buybox_dsbs_conversion_coef": coeffs["dsbs"],
        }
        hyperid = 5  # Берём модель, где 5 офферов (2 МД + 1 dsbs)

        base_request = "place=productoffers&hyperid={}&rgb=green_with_blue&pp=6&debug=da&rids=213&offers-set=defaultList&debug=da".format(
            hyperid
        )
        rearrs_preffix = "&rearr-factors="
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(base_request + rearrs_preffix + rearr_flags_str)

        buybox_competitors = 3  # 2 оффера будут в розыгрыше байбокса
        rejected = 2  # 3 оффера будут реджектнуты и не попадут в байбокс

        # Хотим парсить следующие поля в выдаче
        competitors_caputres = {
            "IsDsbs": [Capture() for _ in range(buybox_competitors)],
            "IsFulfillment": [Capture() for _ in range(buybox_competitors)],
            "DeliveryTypeConversionCoef": [Capture() for _ in range(buybox_competitors)],
            "WareMd5": [Capture() for _ in range(buybox_competitors)],
        }
        rejected_captures = {
            "IsDsbs": [Capture() for _ in range(rejected)],
            "IsFulfillment": [Capture() for _ in range(rejected)],
            "DeliveryTypeConversionCoef": [Capture() for _ in range(rejected)],
            "WareMd5": [Capture() for _ in range(rejected)],
        }
        # Парсим выдачу, проверяем, что коэффициенты берутся корректно для офферов (каждому типу доставки соответствует верный коэффициент)
        self.assertFragmentIn(
            response,
            {
                "buyboxDebug": {
                    "Offers": [
                        {key: NotEmpty(capture=competitors_caputres[key][i]) for key in competitors_caputres}
                        for i in range(buybox_competitors)
                    ],
                    "RejectedOffers": [
                        {"Offer": {key: NotEmpty(capture=rejected_captures[key][i]) for key in rejected_captures}}
                        for i in range(rejected)
                    ],
                }
            },
        )

        # Проходим по всем данным, проверяем, что коэффициент типа доставки соответствует переданному флагу для каждого типа
        for captures_dict, offers_in_dict in zip(
            [competitors_caputres, rejected_captures], [buybox_competitors, rejected]
        ):
            for i in range(offers_in_dict):
                coeff_type = get_coeff_type(captures_dict["IsDsbs"][i].value, captures_dict["IsFulfillment"][i].value)
                self.assertAlmostEqual(
                    coeffs[coeff_type], captures_dict["DeliveryTypeConversionCoef"][i].value, delta=1e-4
                )
                has_delivery_type[coeff_type] = True  # Отмечаем, что тип доставки был охвачен тестом
                # Тут же проверим, что коэффициент был записан в трейс байбокса
                self.assertFragmentIn(
                    response,
                    "For "
                    + captures_dict["WareMd5"][i].value
                    + " DeliveryTypeConversionCoef="
                    + str(captures_dict["DeliveryTypeConversionCoef"][i].value),
                )
        # Проверяем, что все типы доставки были проверены тестом
        for key in has_delivery_type:
            self.assertTrue(has_delivery_type[key] and "There is a type of delivery which has not been checked")

        # Проверим теперь, что коэффициенты не записываются в трейс байбокса, если выключен флаг
        rearr_flags_dict["market_buybox_delivery_type_in_conv_coef_write_trace"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(base_request + rearrs_preffix + rearr_flags_str)
        self.assertFragmentNotIn(response, "DeliveryTypeConversionCoef=")
        # Но при этом в buyboxDebug коэффициент всё равно попадает
        self.assertFragmentIn(response, "DeliveryTypeConversionCoef")

    def test_delivery_type_coefs_in_response_business(self):
        """
        Проверяем флаги - множители конверсии для GMV в байбоксе, для каждого типа доставки свой флаг.
        Тут байбокс разыгрывается по business_id, в этом случае берутся специальные флаги учёта типа доставки.
        Проверяем, что множители, переданные флагами, корректно попадают в выдачу.
        """

        coeffs = {  # Берём разные коэффициенты, чтобы понимать, верно ли берётся коэффициент для своего типа доставки
            "ff": 1.1,
            "dropship": 1.2,
            "dsbs": 1.05,
        }

        def get_coeff_type(is_dsbs, is_fulfillment):
            if is_dsbs:
                return "dsbs"
            elif is_fulfillment:
                return "ff"
            return "dropship"

        has_delivery_type = {  # Словарь, чтобы проверить, что тестом были охвачены все типы доставок
            "ff": False,
            "dropship": False,
            "dsbs": False,
        }

        rearr_flags_dict = {
            "market_buybox_fulfillment_in_conv_coef_by_business": coeffs["ff"],
            "market_buybox_dropship_in_conv_coef_by_business": coeffs["dropship"],
            "market_blue_buybox_dsbs_conversion_coef_by_business": coeffs["dsbs"],
            # Этими флагами включаем байбокс по business_id
            "enable_business_id": 1,
            "market_enable_buybox_by_business": 1,
            "market_blue_buybox_disable_old_buybox_algo": 0,
        }
        hyperid = 5  # Берём модель, где 5 офферов (2 МД + 1 dsbs)

        base_request = (
            "place=productoffers&hyperid={}&rgb=green_with_blue&pp=6&debug=da&rids=213&grhow=supplier&debug=da".format(
                hyperid
            )
        )
        rearrs_preffix = "&rearr-factors="
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(base_request + rearrs_preffix + rearr_flags_str)

        offers_in_response = 4  # 4 оффера попадут в ответ (ДО + топ6)

        # Хотим парсить следующие поля в выдаче
        offers_captures = {
            "IsDsbs": [Capture() for _ in range(offers_in_response)],
            "IsFulfillment": [Capture() for _ in range(offers_in_response)],
            "DeliveryTypeConversionCoef": [Capture() for _ in range(offers_in_response)],
        }
        # Парсим выдачу, проверяем, что коэффициенты берутся корректно для офферов (каждому типу доставки соответствует верный коэффициент)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [{key: NotEmpty(capture=offers_captures[key][i]) for key in offers_captures}],
                            },
                        },
                    }
                    for i in range(offers_in_response)
                ]
            },
        )

        # Проходим по всем данным, проверяем, что коэффициент типа доставки соответствует переданному флагу для каждого типа
        for i in range(offers_in_response):
            coeff_type = get_coeff_type(offers_captures["IsDsbs"][i].value, offers_captures["IsFulfillment"][i].value)
            self.assertAlmostEqual(
                coeffs[coeff_type], offers_captures["DeliveryTypeConversionCoef"][i].value, delta=1e-4
            )
            has_delivery_type[coeff_type] = True  # Отмечаем, что тип доставки был охвачен тестом

        # Проверяем, что все типы доставки были проверены тестом
        for key in has_delivery_type:
            self.assertTrue(has_delivery_type[key] and "There is a type of delivery which has not been checked")

    def test_delivery_type_coefs_not_confused(self):
        """
        Проверяем флаги - множители конверсии для GMV в байбоксе, для каждого типа доставки свой флаг.
        Для байбокса по business_id флаги свои - проверяем, что они действительно свои и независимые.
        """

        # Задаём заведомо разные значения для обычных флагов и флагов байбокса по business_id
        business_id_flags = 1.5
        usual_flags = 1.2

        rearr_flags_dict = {
            "market_buybox_delivery_type_in_conv_coef_write_trace": 1,  # Будем проверять флаги по строке в трейсах байбокса
            "market_buybox_fulfillment_in_conv_coef_by_business": business_id_flags,
            "market_buybox_dropship_in_conv_coef_by_business": business_id_flags,
            "market_blue_buybox_dsbs_conversion_coef_by_business": business_id_flags,
            "market_buybox_fulfillment_in_conv_coef": usual_flags,
            "market_buybox_dropship_in_conv_coef": usual_flags,
            "market_blue_buybox_dsbs_conversion_coef": usual_flags,
            # Тут не включаем байбокс по business_id
        }
        hyperid = 5  # Берём модель, где 5 офферов (2 МД + 1 dsbs)

        base_request = "place=productoffers&hyperid={}&rgb=green_with_blue&pp=6&debug=da&rids=213&offers-set=defaultList&debug=da".format(
            hyperid
        )
        rearrs_preffix = "&rearr-factors="
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(base_request + rearrs_preffix + rearr_flags_str)

        # Чтобы убедиться, что флаги для business_id независимы от обычных флагов, достаточно не найти в ответе флаги для business_id
        self.assertFragmentNotIn(response, "DeliveryTypeConversionCoef=" + str(business_id_flags))
        # Всё-таки проверим, что логирование коэффициентов в трейсы работает, иначе тест некорректный
        self.assertFragmentIn(response, "DeliveryTypeConversionCoef=" + str(usual_flags))

    def test_delivery_type_coefs_affect_conversion_no_business(self):
        """
        Проверяем флаги - множители конверсии для GMV в байбоксе, для каждого типа доставки свой флаг.
        Проверяем, что флаги правильно влияют на конверсию.
        Этот тест для обычных флагов, для флагов байбокса по business_id см. тест test_delivery_type_coefs_affect_conversion_business
        """

        coeffs = {  # Берём разные коэффициенты, чтобы понимать, верно ли берётся коэффициент для своего типа доставки
            "ff": 1.2,
            "dropship": 1.3,
            "dsbs": 1.1,
        }

        rearr_flags_dict_common = {
            # Флаги, которые нужны, чтобы конверсия вычислялась
            "market_blue_buybox_always_compute_approx_gmv_ue": 1,
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_rel_filters_before_buybox": 0,
            "market_blue_buybox_courier_delivery_priority": 0,  # without this flag dropship coef couldn't be tested
        }
        rearr_flags_dict_delivery_type_coefs = {
            "market_buybox_fulfillment_in_conv_coef": coeffs["ff"],
            "market_buybox_dropship_in_conv_coef": coeffs["dropship"],
            "market_blue_buybox_dsbs_conversion_coef": coeffs["dsbs"],
        }
        hyperid = 5  # Берём модель, где 5 офферов (2 МД + 1 dsbs)

        base_request = "place=productoffers&hyperid={}&rgb=green_with_blue&pp=6&offers-set=defaultList&debug=da".format(
            hyperid
        )
        rearrs_preffix = "&rearr-factors="
        rearr_flags_str_common = dict_to_rearr(rearr_flags_dict_common)
        rearr_flags_str_custom = dict_to_rearr(rearr_flags_dict_delivery_type_coefs)

        # Будем посылать 2 запроса - с дефолтными флагами и с кастомными, сравним конверсию
        default_request = base_request + rearrs_preffix + rearr_flags_str_common
        custom_request = default_request + ";" + rearr_flags_str_custom

        responses = {
            "Default": self.report.request_json(default_request),
            "Custom": self.report.request_json(custom_request),
        }

        buybox_competitors = {"Default": 3, "Custom": 3}  # офферы, которые в розыгрыше байбокса (кол-во)
        rejected = {"Default": 2, "Custom": 2}  # офферы, которые реджектнуты и не в байбоксе (кол-во)

        # Создаём Capture, чтобы парсить поля
        competitors_caputres = {
            request_type: {
                # Хотим парсить следующие поля
                "DeliveryTypeConversionCoef": [Capture() for _ in range(buybox_competitors[request_type])],
                "Conversion": [Capture() for _ in range(buybox_competitors[request_type])],
                "WareMd5": [Capture() for _ in range(buybox_competitors[request_type])],
            }
            for request_type in ["Default", "Custom"]
        }
        rejected_captures = {
            request_type: {
                "DeliveryTypeConversionCoef": [Capture() for _ in range(rejected[request_type])],
                "Conversion": [Capture() for _ in range(rejected[request_type])],
                "WareMd5": [Capture() for _ in range(rejected[request_type])],
            }
            for request_type in ["Default", "Custom"]
        }

        # Для каждого запроса парсим выдачу
        for request_type in ["Default", "Custom"]:
            self.assertFragmentIn(
                responses[request_type],
                {
                    "buyboxDebug": {
                        "Offers": [
                            {
                                key: NotEmpty(capture=competitors_caputres[request_type][key][i])
                                for key in competitors_caputres[request_type]
                            }
                            for i in range(buybox_competitors[request_type])
                        ],
                        "RejectedOffers": [
                            {
                                "Offer": {
                                    key: NotEmpty(capture=rejected_captures[request_type][key][i])
                                    for key in rejected_captures[request_type]
                                }
                            }
                            for i in range(rejected[request_type])
                        ],
                    }
                },
            )

        # Коэффициенты входят в конверсию мультипликативно, поэтому проверим, что конверсии пропорциональны коэффициентам
        # Сопоставим конверсии для разных запросов, используя WareMd5 для идентификации оффера (офферы могут идти в разном порядке)
        coeffs_by_waremd5 = {}
        for request_type in ["Default", "Custom"]:
            for captures_dict, offers_in_dict in zip(
                [competitors_caputres, rejected_captures], [buybox_competitors, rejected]
            ):
                for i in range(offers_in_dict[request_type]):
                    curr_waremd5 = captures_dict[request_type]["WareMd5"][i].value
                    curr_conversion = captures_dict[request_type]["Conversion"][i].value
                    curr_coeff = captures_dict[request_type]["DeliveryTypeConversionCoef"][i].value

                    if curr_waremd5 not in coeffs_by_waremd5:
                        coeffs_by_waremd5[curr_waremd5] = {"Default": {}, "Custom": {}}

                    self.assertTrue(curr_coeff != 0 and "DeliveryTypeConversionCoef should not be 0")
                    coeffs_by_waremd5[curr_waremd5][request_type] = {"conv": curr_conversion, "coeff": curr_coeff}

        # Проверим пропорциональность коэффициентов в Default и Custom запросах
        # Лучше сравнивать отношение конверсий с отношением коэффициентов, чтобы делились числа одного порядка (так будет точнее)
        for curr_ware_md5 in coeffs_by_waremd5:
            convs = (
                coeffs_by_waremd5[curr_ware_md5]["Default"]["conv"] / coeffs_by_waremd5[curr_ware_md5]["Custom"]["conv"]
            )
            deliv_type_coeffs = (
                coeffs_by_waremd5[curr_ware_md5]["Default"]["coeff"]
                / coeffs_by_waremd5[curr_ware_md5]["Custom"]["coeff"]
            )
            self.assertAlmostEqual(convs, deliv_type_coeffs, delta=1e-4)

    def test_delivery_type_coefs_affect_conversion_business(self):
        """
        Проверяем флаги - множители конверсии для GMV в байбоксе, для каждого типа доставки свой флаг.
        Проверяем, что флаги правильно влияют на конверсию.
        Это тест для флагов байбокса по business_id
        """

        coeffs = {  # Берём разные коэффициенты, чтобы понимать, верно ли берётся коэффициент для своего типа доставки
            "ff": 1.2,
            "dropship": 1.3,
            "dsbs": 1.1,
        }

        rearr_flags_dict_common_business = {
            # Этими флагами включаем байбокс по business_id
            "enable_business_id": 1,
            "market_enable_buybox_by_business": 1,
            "market_blue_buybox_disable_old_buybox_algo": 0,
            # Флаги, которые нужны, чтобы конверсия вычислялась
            "market_blue_buybox_always_compute_approx_gmv_ue": 1,
            "market_blue_buybox_gmv_ue_mix_coef": 1,
            "market_rel_filters_before_buybox": 0,
        }
        rearr_flags_dict_delivery_type_coefs = {
            "market_buybox_fulfillment_in_conv_coef_by_business": coeffs["ff"],
            "market_buybox_dropship_in_conv_coef_by_business": coeffs["dropship"],
            "market_blue_buybox_dsbs_conversion_coef_by_business": coeffs["dsbs"],
        }
        hyperid = 5  # Берём модель, где 5 офферов (2 МД + 1 dsbs)

        base_request = (
            "place=productoffers&hyperid={}&rgb=green_with_blue&pp=6&debug=da&rids=213&grhow=supplier&debug=da".format(
                hyperid
            )
        )
        rearrs_preffix = "&rearr-factors="
        rearr_flags_str_common = dict_to_rearr(rearr_flags_dict_common_business)
        rearr_flags_str_custom = dict_to_rearr(rearr_flags_dict_delivery_type_coefs)

        # Будем посылать 2 запроса - с дефолтными флагами и с кастомными, сравним конверсию
        default_request = base_request + rearrs_preffix + rearr_flags_str_common
        custom_request = default_request + ";" + rearr_flags_str_custom

        responses = {
            "Default": self.report.request_json(default_request),
            "Custom": self.report.request_json(custom_request),
        }

        offers_in_result = {"Default": 4, "Custom": 4}  # В обоих запросах по 4 оффера в ответе

        # Создаём Capture, чтобы парсить поля
        offers_captures = {
            request_type: {
                # Хотим парсить следующие поля
                "DeliveryTypeConversionCoef": [Capture() for _ in range(offers_in_result[request_type])],
                "Conversion": [Capture() for _ in range(offers_in_result[request_type])],
                "WareMd5": [Capture() for _ in range(offers_in_result[request_type])],
            }
            for request_type in ["Default", "Custom"]
        }

        # Для каждого запроса парсим выдачу
        for request_type in ["Default", "Custom"]:
            self.assertFragmentIn(
                responses[request_type],
                {
                    "results": [
                        {
                            "debug": {
                                "buyboxDebug": {
                                    "Offers": [
                                        {
                                            key: NotEmpty(capture=offers_captures[request_type][key][i])
                                            for key in offers_captures[request_type]
                                        }
                                    ]
                                }
                            }
                        }
                        for i in range(offers_in_result[request_type])
                    ]
                },
            )

        # Коэффициенты входят в конверсию мультипликативно, поэтому проверим, что конверсии пропорциональны коэффициентам
        # Сопоставим конверсии для разных запросов, используя WareMd5 для идентификации оффера (офферы могут идти в разном порядке)
        coeffs_by_waremd5 = {}
        for request_type in ["Default", "Custom"]:
            for i in range(offers_in_result[request_type]):
                curr_waremd5 = offers_captures[request_type]["WareMd5"][i].value
                curr_conversion = offers_captures[request_type]["Conversion"][i].value
                curr_coeff = offers_captures[request_type]["DeliveryTypeConversionCoef"][i].value

                if curr_waremd5 not in coeffs_by_waremd5:
                    coeffs_by_waremd5[curr_waremd5] = {"Default": {}, "Custom": {}}

                self.assertTrue(curr_coeff != 0 and "DeliveryTypeConversionCoef should not be 0")
                coeffs_by_waremd5[curr_waremd5][request_type] = {"conv": curr_conversion, "coeff": curr_coeff}

        # Проверим пропорциональность коэффициентов в Default и Custom запросах
        # Лучше сравнивать отношение конверсий с отношением коэффициентов, чтобы делились числа одного порядка (так будет точнее)
        for curr_ware_md5 in coeffs_by_waremd5:
            convs = (
                coeffs_by_waremd5[curr_ware_md5]["Default"]["conv"] / coeffs_by_waremd5[curr_ware_md5]["Custom"]["conv"]
            )
            deliv_type_coeffs = (
                coeffs_by_waremd5[curr_ware_md5]["Default"]["coeff"]
                / coeffs_by_waremd5[curr_ware_md5]["Custom"]["coeff"]
            )
            self.assertAlmostEqual(convs, deliv_type_coeffs, delta=1e-4)


if __name__ == '__main__':
    main()
