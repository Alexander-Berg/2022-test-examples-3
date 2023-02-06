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
    DeliveryServiceRegionToRegionInfo,
    DynamicDeliveryServiceInfo,
    DynamicPriceControlData,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    RecommendedFee,
    Region,
    RegionalDelivery,
    RegionalMsku,
    ReservePriceFee,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.matcher import Wildcard, Round, Capture, NotEmpty, Absent
from core.cpc import Cpc

DEFAULT_TRANSACTION_FEE = 200
FEE_MULTIPLIER = 10000.0
CPM_MULTIPLIER = 100000
RECOMMENDED_FEE = 20
RESERVED_FEE = 10
VENDOR_CPC_TO_CPA_CONVERSION = 0.075


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


def total_ue_shifted(ue, price, e, f):
    return ue + e + f / price


def softmax_plus(ue, price, e, f, p):
    ue = total_ue_shifted(ue, price, e, f)
    return (ue + pow(1 + pow(abs(ue), p), 1 / p)) / 2


def convert_vendor_bid_to_fee(bid, price):
    return int(bid * 0.3 / (VENDOR_CPC_TO_CPA_CONVERSION * price / FEE_MULTIPLIER))


class BlueOfferEnriched(object):
    def __init__(self, blue_offer, vendor_fee, p_purchase, purchase_price=None, is_local=False):
        self.blue_offer = blue_offer
        self.p_purchase = p_purchase
        self.purchase_price = purchase_price
        self.fee = blue_offer.fee
        self.vendor_fee = vendor_fee
        self.ts = blue_offer.ts
        self.is_local = is_local

    def get_blue_offer(self):
        return self.blue_offer

    def ue_total(self, config):
        return config.g * self.ue_raw() + (self.fee + self.vendor_fee - config.w * RESERVED_FEE) / FEE_MULTIPLIER

    def ue_raw(self):
        if self.purchase_price:
            return (self.blue_offer.price - self.purchase_price) / float(self.blue_offer.price)
        else:
            return 0.02

    def smooth_ue(self, config):
        if config.softmax:
            return softmax_plus(self.ue_total(config), self.blue_offer.price, config.e, config.f, config.p)
        else:
            return total_ue_shifted(self.ue_total(config), self.blue_offer.price, config.e, config.f)

    def cpa_cpm(self, config):
        cpm = (
            self.p_purchase
            * self.price
            * (config.b * self.smooth_ue(config) + config.a + config.c / float(self.price))
            * CPM_MULTIPLIER
        )
        if self.is_local:
            cpm = cpm * config.locality_boosting_coeff
        return cpm * config.cpa_multiplier + config.d

    def make_copy(self):
        return BlueOfferEnriched(self.blue_offer, self.vendor_fee, self.p_purchase, self.purchase_price, self.is_local)

    def __getattr__(self, attr):
        if attr in self.__class__.__dict__:
            return getattr(self, attr)
        else:
            return getattr(self.blue_offer, attr)


def calculate_bids(first, second, config):
    first_copy = first.make_copy()
    first_copy.fee = 1
    while (first_copy.cpa_cpm(config) <= second.cpa_cpm(config)) and 0 < first_copy.fee < FEE_MULTIPLIER:
        first_copy.fee += 1

    result_fee = first_copy.fee
    if first.vendor_fee > 0:
        result_fee = int(first.fee * (float(result_fee) / (first.fee + first.vendor_fee)))
    return min(result_fee, first.fee)


def make_ts(price, feed_id):
    return price * 100 + feed_id


def vbid_to_autostrategy_vendor(vbid):
    return (
        None
        if vbid == 0
        else AutostrategyWithDatasourceId(
            id=444,
            datasource_id=5,
            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=vbid),
        )
    )


def create_blue_offer(price, feedid, delivery_buckets, fee, p_purchase, waremd5, purchase_price=None, vbid=0):
    ts = make_ts(price, feedid)

    blue_offer = BlueOffer(
        price=price,
        feedid=feedid,
        waremd5=waremd5,
        delivery_buckets=delivery_buckets,
        fee=fee,
        ts=ts,
        autostrategy_vendor_with_datasource_id=vbid_to_autostrategy_vendor(vbid),
    )
    return BlueOfferEnriched(
        blue_offer=blue_offer,
        vendor_fee=convert_vendor_bid_to_fee(vbid, price),
        p_purchase=p_purchase,
        purchase_price=purchase_price,
    )


def create_dsbs_offer(price, feedid, title, hyperid, bid, fee, p_purchase, vbid=0):
    ts = make_ts(price, feedid)
    dsbs_offer = Offer(
        fesh=feedid,
        price=price,
        feedid=feedid,
        title=title,
        hyperid=hyperid,
        bid=bid,
        cpa=Offer.CPA_REAL,
        fee=fee,
        ts=ts,
        autostrategy_vendor_with_datasource_id=vbid_to_autostrategy_vendor(vbid),
    )
    return BlueOfferEnriched(
        blue_offer=dsbs_offer,
        vendor_fee=convert_vendor_bid_to_fee(vbid, price),
        p_purchase=p_purchase,
        purchase_price=None,
    )


msku701_purchase_price = 6500
blue_offers_701 = [
    create_blue_offer(
        price=7000, feedid=36, waremd5='THIRD-PARTY-F36-07000w', delivery_buckets=[1234], fee=1500, p_purchase=0.2
    ),
    create_blue_offer(
        price=7000,
        feedid=34,
        waremd5='FIRST-PARTY-F34-07000w',
        delivery_buckets=[1236],
        fee=500,
        p_purchase=0.2,
        purchase_price=msku701_purchase_price,
    ),
    create_blue_offer(
        price=7500, feedid=35, waremd5='THIRD-PARTY-F35-07500w', delivery_buckets=[1236], fee=1050, p_purchase=0.1
    ),
]
blue_offers_701_dict = dict((offer.waremd5, offer) for offer in blue_offers_701)

dsbs_offers_701 = [
    create_dsbs_offer(price=7000, feedid=20, title='DSBS-701-20', hyperid=701, bid=10, fee=1050, p_purchase=0.2),
    create_dsbs_offer(price=7000, feedid=21, title='DSBS-701-21', hyperid=701, bid=10, fee=100, p_purchase=0.2),
]
dsbs_offers_701_dict = dict((offer.title, offer) for offer in dsbs_offers_701)

cpc_offers_701_dict = {
    'CPC-30': Offer(price=6500, fesh=30, title='CPC-30', hyperid=701, bid=10, ts=700100),
    'CPC-31': Offer(price=7500, fesh=31, title='CPC-31', hyperid=701, bid=10, ts=700200),
}
# TODO: purchase price is strange
msku701 = MarketSku(
    hyperid=701,
    sku=701,
    purchase_price=msku701_purchase_price,
    blue_offers=list(map(lambda x: x.get_blue_offer(), blue_offers_701)),
)

purchase_price_801 = 6500
blue_offers_801 = [
    create_blue_offer(
        price=7000, feedid=17, waremd5='FIRST-PARTY-F17-07000w', delivery_buckets=[1234], fee=540, p_purchase=0.2
    ),
    create_blue_offer(
        price=7000,
        feedid=15,
        waremd5='THIRD-PARTY-F15-07000w',
        delivery_buckets=[1236],
        fee=500,
        p_purchase=0.2,
        purchase_price=purchase_price_801,
    ),
]
blue_offers_801_dict = dict((offer.waremd5, offer) for offer in blue_offers_801)
msku801 = MarketSku(
    hyperid=801,
    sku=801,
    purchase_price=purchase_price_801,
    delivery_buckets=[1234],
    blue_offers=list(map(lambda x: x.get_blue_offer(), blue_offers_801)),
)

purchase_price_911 = 6500
blue_offers_911 = [
    create_blue_offer(
        price=7000,
        feedid=36,
        waremd5='911-3P-F36P7000-F1500w',
        delivery_buckets=[1234],
        fee=1500,
        p_purchase=0.2,
        vbid=1,
    ),
    create_blue_offer(
        price=7000,
        feedid=34,
        waremd5='911-1P-F34P7000-F0500w',
        delivery_buckets=[1236],
        fee=500,
        p_purchase=0.2,
        purchase_price=msku701_purchase_price,
        vbid=2,
    ),
    create_blue_offer(
        price=7500,
        feedid=35,
        waremd5='911-3P-F35P7500-F1050w',
        delivery_buckets=[1236],
        fee=1050,
        p_purchase=0.1,
        vbid=3,
    ),
]
blue_offers_911_dict = dict((offer.waremd5, offer) for offer in blue_offers_911)
msku911 = MarketSku(
    hyperid=911,
    sku=911,
    purchase_price=purchase_price_911,
    delivery_buckets=[1234],
    blue_offers=list(map(lambda x: x.get_blue_offer(), blue_offers_911)),
)


class AuctionConfig(object):
    def __init__(self, e, f, p, a, c, d, b, g, w=0, softmax=False, locality_boosting_coeff=1.0, cpa_multiplier=1.0):
        self.e = e
        self.f = f
        self.p = p
        self.a = a
        self.c = c
        self.d = d
        self.b = b
        self.g = g
        self.w = w
        self.softmax = softmax
        self.locality_boosting_coeff = locality_boosting_coeff
        self.cpa_multiplier = cpa_multiplier


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.001)

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=2, region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(
                fesh=10,
                datafeed_id=10,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="3P-Магазин 140 склад",
                warehouse_id=140,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.FIRST_PARTY,
                name="1P-Магазин 141 склад",
                warehouse_id=141,
            ),
            Shop(
                fesh=12,
                datafeed_id=12,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.FIRST_PARTY,
                name="1P-Магазин 142 склад",
                warehouse_id=142,
            ),
            Shop(
                fesh=13,
                datafeed_id=13,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="3P-Магазин 143 склад",
                warehouse_id=143,
            ),
            Shop(
                fesh=14,
                datafeed_id=14,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="3P-Магазин 144 склад",
                warehouse_id=144,
            ),
            Shop(
                fesh=15,
                datafeed_id=15,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="3P-Магазин 145 склад",
                warehouse_id=145,
            ),
            Shop(
                fesh=16,
                datafeed_id=16,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="3P-Магазин 146 склад",
                warehouse_id=146,
            ),
            Shop(
                fesh=17,
                datafeed_id=17,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.FIRST_PARTY,
                name="1P-Магазин 147 склад",
                warehouse_id=147,
                is_supplier=True,
            ),
            Shop(fesh=20, datafeed_id=20, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=21, datafeed_id=21, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=30, datafeed_id=30, priority_region=213),
            Shop(fesh=31, datafeed_id=31, priority_region=213),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="Какойто 3p магазин 1475",
                warehouse_id=1475,
            ),
            Shop(
                fesh=33,
                datafeed_id=33,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="Какойто 3p магазин 1470",
                warehouse_id=1470,
            ),
            Shop(
                fesh=34,
                datafeed_id=34,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.FIRST_PARTY,
                name="Какойто 1p магазин 1470",
                warehouse_id=1470,
            ),
            Shop(
                fesh=35,
                datafeed_id=35,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="Какойто 3p магазин 1470",
                warehouse_id=1470,
            ),
            Shop(
                fesh=36,
                datafeed_id=36,
                priority_region=213,
                regions=[225],
                blue=Shop.BLUE_REAL,
                supplier_type=Shop.THIRD_PARTY,
                name="Какойто 3p магазин 1470",
                warehouse_id=1470,
            ),
        ]

        cls.dynamic.market_dynamic.dynamic_price_control += [
            DynamicPriceControlData(32, 150, 0),
            DynamicPriceControlData(33, 150, 1),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=39),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(
                id=157,
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=3),
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=39, days_key=3),
                ],
            ),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            WarehousesPriorityInRegion(
                regions=[39],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=227, options=[DeliveryOption(price=48, shop_delivery_price=48, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=158, shop_delivery_price=58, day_from=4, day_to=6)]
                    ),
                    RegionalDelivery(
                        rid=39, options=[DeliveryOption(price=58, shop_delivery_price=58, day_from=1, day_to=2)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1235,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=224, options=[DeliveryOption(price=35, shop_delivery_price=35, day_from=2, day_to=4)]
                    ),
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=250, shop_delivery_price=55, day_from=4, day_to=6)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1236,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=224, options=[DeliveryOption(price=35, shop_delivery_price=35, day_from=2, day_to=4)]
                    ),
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=256, shop_delivery_price=55, day_from=4, day_to=6)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1237,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=224, options=[DeliveryOption(price=35, shop_delivery_price=35, day_from=2, day_to=4)]
                    ),
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=257, shop_delivery_price=55, day_from=4, day_to=6)]
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=201,
                sku=201,
                purchase_price=100,
                blue_offers=[
                    BlueOffer(
                        price=83,
                        feedid=10,
                        waremd5='THIRD-PARTY-F10--83-2w',
                        delivery_buckets=[1234],
                        ts=200010,
                        fee=403,
                    ),
                    BlueOffer(
                        price=71,
                        feedid=11,
                        waremd5='FIRST-PARTY-F11--71-2w',
                        delivery_buckets=[1235],
                        ts=200011,
                        fee=401,
                    ),
                    BlueOffer(
                        price=61,
                        feedid=12,
                        waremd5='FIRST-PARTY-F12--61-2w',
                        delivery_buckets=[1236],
                        ts=200012,
                        fee=401,
                    ),
                    BlueOffer(
                        price=53,
                        feedid=13,
                        waremd5='THIRD-PARTY-F13--53-2w',
                        delivery_buckets=[1235],
                        ts=200013,
                        fee=403,
                    ),
                    BlueOffer(
                        price=43,
                        feedid=14,
                        waremd5='THIRD-PARTY-F14--43-2w',
                        delivery_buckets=[1236],
                        ts=200014,
                        fee=403,
                    ),
                    BlueOffer(
                        price=33,
                        feedid=15,
                        waremd5='THIRD-PARTY-F15--33-2w',
                        delivery_buckets=[1234],
                        ts=200015,
                        fee=403,
                    ),
                    BlueOffer(
                        price=23,
                        feedid=16,
                        waremd5='THIRD-PARTY-F16--23-2w',
                        delivery_buckets=[1235],
                        ts=200016,
                        fee=403,
                    ),
                    BlueOffer(
                        price=11,
                        feedid=17,
                        waremd5='FIRST-PARTY-F17--11-2w',
                        delivery_buckets=[1236],
                        ts=200017,
                        fee=401,
                    ),
                ],
            ),
            MarketSku(
                hyperid=301,
                sku=301,
                purchase_price=1000,
                blue_offers=[
                    BlueOffer(
                        price=26003,
                        feedid=10,
                        waremd5='THIRD-PARTY-F10--83-3w',
                        delivery_buckets=[1234],
                        ts=200010,
                        fee=83,
                    ),
                    BlueOffer(
                        price=26001,
                        feedid=11,
                        waremd5='FIRST-PARTY-F11--71-3w',
                        delivery_buckets=[1235],
                        ts=200011,
                        fee=71,
                    ),
                    BlueOffer(
                        price=25001,
                        feedid=12,
                        waremd5='FIRST-PARTY-F12--61-3w',
                        delivery_buckets=[1236],
                        ts=200012,
                        fee=61,
                    ),
                    BlueOffer(
                        price=2603,
                        feedid=13,
                        waremd5='THIRD-PARTY-F13--53-3w',
                        delivery_buckets=[1235],
                        ts=200013,
                        fee=53,
                    ),
                    BlueOffer(
                        price=263,
                        feedid=14,
                        waremd5='THIRD-PARTY-F14--43-3w',
                        delivery_buckets=[1236],
                        ts=200014,
                        fee=43,
                    ),
                    BlueOffer(
                        price=33,
                        feedid=15,
                        waremd5='THIRD-PARTY-F15--33-3w',
                        delivery_buckets=[1234],
                        ts=200015,
                        fee=33,
                    ),
                    BlueOffer(
                        price=23,
                        feedid=16,
                        waremd5='THIRD-PARTY-F16--23-3w',
                        delivery_buckets=[1235],
                        ts=200016,
                        fee=23,
                    ),
                    BlueOffer(
                        price=11,
                        feedid=17,
                        waremd5='FIRST-PARTY-F17--11-3w',
                        delivery_buckets=[1236],
                        ts=200017,
                        fee=11,
                    ),
                ],
            ),
            MarketSku(
                hyperid=401,
                sku=401,
                purchase_price=1000,
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=10,
                        waremd5='THIRD-PARTY-F10-40003w',
                        delivery_buckets=[1234],
                        ts=400003,
                        fee=83,
                    ),
                ],
            ),
            MarketSku(
                hyperid=501,
                sku=501,
                ref_min_price=1000,
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=18,
                        waremd5='THIRD-PARTY-F18-50103w',
                        delivery_buckets=[1234],
                        ts=500003,
                        fee=400,
                    ),
                    BlueOffer(
                        price=1100,
                        feedid=33,
                        waremd5='THIRD-PARTY-F33-50103w',
                        delivery_buckets=[1234],
                        ts=500004,
                        fee=500,
                    ),
                ],
            ),
            MarketSku(
                hyperid=501,
                sku=6010,
                ref_min_price=1000,
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=16,
                        waremd5='THIRD-PARTY-F16-60103w',
                        delivery_buckets=[1234],
                        ts=600003,
                        fee=300,
                    ),
                    BlueOffer(
                        price=1100,
                        feedid=32,
                        waremd5='THIRD-PARTY-F32-60103w',
                        delivery_buckets=[1234],
                        ts=600004,
                        fee=200,
                    ),
                ],
            ),
            MarketSku(
                hyperid=601,
                sku=601,
                purchase_price=900,
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=34,
                        waremd5='FIRST-PARTY-F34-601-3w',
                        delivery_buckets=[1236],
                        ts=200017,
                        fee=11,
                        supplier_id=34,
                        offerid="FIRST-PARTY-F34-601-3w",
                    )
                ],
            ),
            msku701,
            msku911,
        ]
        # Делаем много офферов в msku, чтобы проверить "Цены", а не топ 6
        cls.index.mskus += [
            MarketSku(
                hyperid=901,
                sku=901,
                purchase_price=900,
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=34,
                        waremd5='FIRST-PARTY-F34-901-3w',
                        title='FIRST-PARTY-F34-901-3w',
                        delivery_buckets=[1236],
                        ts=900017,
                        fee=11,
                        supplier_id=34,
                        offerid="FIRST-PARTY-F34-901-3w",
                        autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                            id=444,
                            datasource_id=5,
                            strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=20),
                        ),
                    ),
                ],
            ),
        ]

        cls.index.blue_regional_mskus += [
            RegionalMsku(msku_id=6010, offers=2, price_min=1000, price_max=1100, rids=[213]),
        ]

        cls.index.models += [
            Model(hyperid=201, hid=101),
            Model(hyperid=301, hid=101),
            Model(hyperid=401, hid=101),
            Model(hyperid=501, hid=101),
            Model(hyperid=601, hid=101),
            Model(hyperid=701, hid=101),
            Model(hyperid=911, hid=101),
            Model(hyperid=901, hid=101, vbid=20),
            Model(hyperid=991, hid=101, vbid=10),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100010).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100011).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100012).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100013).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100014).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100015).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100016).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100017).respond(0.01)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200010).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200011).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200012).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200013).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200014).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200015).respond(0.06)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200016).respond(0.07)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200017).respond(0.08)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300010).respond(0.11)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300011).respond(0.12)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300012).respond(0.13)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300013).respond(0.14)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300014).respond(0.15)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300015).respond(0.16)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300016).respond(0.17)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300017).respond(0.18)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 199017).respond(0.50)

        not_blue_offers_cnt_for_901 = 10
        cls.index.offers += [
            Offer(
                fesh=20,
                title='paid dsbs 901_' + str(i) + ' prices',
                hyperid=901,
                price=100 + i,
                bid=10,
                ts=900120 + i,
                cpa=Offer.CPA_REAL,
                fee=600 + i * 50,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=5,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=20),
                ),
            )
            for i in range(not_blue_offers_cnt_for_901)
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 900017).respond(0.18)
        for i in range(not_blue_offers_cnt_for_901):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 900120 + i).respond(0.20 + 0.01 * i)

        not_blue_offers_cnt_for_991 = 5
        cls.index.offers += [
            Offer(
                fesh=20,
                title='paid dsbs 991_' + str(i) + ' expensive',
                hyperid=991,
                price=100 + i,
                bid=10,
                ts=199018 + i,
                cpa=Offer.CPA_REAL,
                fee=600 + i * 50,
                autostrategy_vendor_with_datasource_id=AutostrategyWithDatasourceId(
                    id=444,
                    datasource_id=5,
                    strategy=Autostrategy(type=AutostrategyType.POSITIONAL, position=1, maxbid=10),
                ),
            )
            for i in range(not_blue_offers_cnt_for_991)
        ]
        for i in range(not_blue_offers_cnt_for_991):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 199018 + i).respond(0.50 + 0.01 * i)

        cls.index.offers += [
            Offer(
                fesh=20,
                title='paid dsbs 20 expensive',
                hyperid=101,
                price=7000,
                bid=10,
                ts=100020,
                cpa=Offer.CPA_REAL,
                fee=1000,
            ),
            Offer(
                fesh=21,
                feedid=21,
                title='paid dsbs 21 expensive',
                hyperid=101,
                price=7001,
                bid=10,
                ts=100021,
                cpa=Offer.CPA_REAL,
                fee=1100,
            ),
            Offer(fesh=30, title='paid cpc 30 expensive', hyperid=101, price=6000, bid=10, ts=100030),
            Offer(fesh=31, title='paid cpc 31 expensive', hyperid=101, price=6001, bid=10, ts=100031),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100020).respond(0.03)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100021).respond(0.04)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100030).respond(0.05)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 100031).respond(0.06)

        cls.index.offers += [
            Offer(fesh=20, title='paid dsbs 20 cheap', hyperid=201, price=15, bid=10, ts=200020, cpa=Offer.CPA_REAL),
            Offer(
                fesh=21,
                waremd5='DSBS1-DSBS1-F17--11-2w',
                title='paid dsbs 21 cheap',
                hyperid=201,
                price=16,
                bid=10,
                ts=200021,
                cpa=Offer.CPA_REAL,
                fee=100,
            ),
            Offer(fesh=30, title='paid cpc 30 cheap', hyperid=201, price=10, bid=10, ts=200030),
            Offer(fesh=31, title='paid cpc 31 cheap', hyperid=201, price=11, bid=10, ts=200031),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200020).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200021).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200030).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 200031).respond(0.6)

        cls.index.offers += [
            Offer(
                fesh=20,
                title='paid dsbs 20 middle',
                hyperid=301,
                price=1500,
                bid=10,
                ts=300020,
                cpa=Offer.CPA_REAL,
                fee=100,
            ),
            Offer(
                fesh=21,
                title='paid dsbs 21 middle',
                hyperid=301,
                price=160,
                bid=10,
                ts=300021,
                cpa=Offer.CPA_REAL,
                fee=100,
            ),
            Offer(fesh=30, title='paid cpc 30 middle', hyperid=301, price=1000, bid=10, ts=300030),
            Offer(fesh=31, title='paid cpc 31 middle', hyperid=301, price=110, bid=10, ts=300031),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300020).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300021).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300030).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 300031).respond(0.6)

        cls.index.offers += [
            Offer(
                fesh=20, title='paid dsbs 20 middle', hyperid=401, price=1000, ts=400001, cpa=Offer.CPA_REAL, fee=100
            ),
            Offer(fesh=30, title='paid cpc 30 middle', hyperid=401, price=1000, ts=400002),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 400001).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 400002).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 400003).respond(0.1)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500003).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 500004).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 600003).respond(0.1)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 600004).respond(0.1)

        cls.index.offers += list(map(lambda x: x.get_blue_offer(), dsbs_offers_701))
        cls.index.offers += list(cpc_offers_701_dict.values())

        for dsbs_offer in dsbs_offers_701:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, dsbs_offer.ts).respond(dsbs_offer.p_purchase)

        for blue_offer in blue_offers_701:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, blue_offer.ts).respond(blue_offer.p_purchase)

        # TODO read from dict
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 700100).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 700200).respond(0.1)

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=101, recommended_bid=RECOMMENDED_FEE / FEE_MULTIPLIER),
        ]

        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=101, reserveprice_fee=RESERVED_FEE / FEE_MULTIPLIER),
        ]

    def test_main(self):
        def check_response(response):
            # Создаем Capture что бы распарсить из выдачи cpm-ы
            dropship_f35_cpm = Capture()
            dropship_f36_cpm = Capture()
            onep_f34_cpm = Capture()
            dsbs_f20_cpm = Capture()
            dsbs_f21_cpm = Capture()

            drop_f36_offer = blue_offers_701_dict["THIRD-PARTY-F36-07000w"]
            drop_f35_offer = blue_offers_701_dict["THIRD-PARTY-F35-07500w"]
            onep_f34_offer = blue_offers_701_dict["FIRST-PARTY-F34-07000w"]

            dsbs_f20_offer = dsbs_offers_701_dict["DSBS-701-20"]
            dsbs_f21_offer = dsbs_offers_701_dict["DSBS-701-21"]
            brokered_fee_12 = calculate_bids(drop_f36_offer, drop_f35_offer, config)
            brokered_fee_23 = calculate_bids(drop_f35_offer, onep_f34_offer, config)

            brokered_fee_45 = calculate_bids(dsbs_f20_offer, dsbs_f21_offer, config)
            self.assertFragmentIn(
                response,
                [
                    {
                        "fee": str(brokered_fee_12 / FEE_MULTIPLIER),
                        "feeShowPlain": Wildcard("fee: \"" + str(brokered_fee_12 / FEE_MULTIPLIER) + "\"*"),
                        "debug": {
                            "wareId": drop_f36_offer.waremd5,
                            "properties": {
                                'UE_TOTAL_RAW': Round(drop_f36_offer.ue_total(config)),
                                'SMOOTHED_UE_TO_PRICE': Round(drop_f36_offer.smooth_ue(config)),
                                'CPM': NotEmpty(capture=dropship_f36_cpm),
                            },
                            "sale": {"shopFee": drop_f36_offer.fee, "brokeredFee": brokered_fee_12},
                        },
                    },
                    {
                        "debug": {
                            "wareId": drop_f35_offer.waremd5,
                            "properties": {
                                'UE_TOTAL_RAW': Round(drop_f35_offer.ue_total(config)),
                                'SMOOTHED_UE_TO_PRICE': Round(drop_f35_offer.smooth_ue(config)),
                                'CPM': NotEmpty(capture=dropship_f35_cpm),
                            },
                            "sale": {"shopFee": drop_f35_offer.fee, "brokeredFee": brokered_fee_23},
                        }
                    },
                    {
                        "debug": {
                            "wareId": onep_f34_offer.waremd5,
                            "properties": {
                                'UE_TOTAL_RAW': Round(onep_f34_offer.ue_total(config)),
                                'SMOOTHED_UE_TO_PRICE': Round(onep_f34_offer.smooth_ue(config)),
                                'CPM': NotEmpty(capture=onep_f34_cpm),
                            },
                            "sale": {"shopFee": onep_f34_offer.fee, "brokeredFee": 0},
                        }
                    },
                    {
                        "debug": {
                            "offerTitle": dsbs_f20_offer.title,
                            "properties": {
                                'UE_TOTAL_RAW': Round(dsbs_f20_offer.ue_total(config)),
                                'SMOOTHED_UE_TO_PRICE': Round(dsbs_f20_offer.smooth_ue(config)),
                                'CPM': NotEmpty(capture=dsbs_f20_cpm),
                            },
                            "sale": {"shopFee": dsbs_f20_offer.fee, "brokeredFee": brokered_fee_45},
                        }
                    },
                    {
                        "debug": {
                            "offerTitle": dsbs_f21_offer.title,
                            "properties": {
                                'UE_TOTAL_RAW': Round(dsbs_f21_offer.ue_total(config)),
                                'SMOOTHED_UE_TO_PRICE': Round(dsbs_f21_offer.smooth_ue(config)),
                                'CPM': NotEmpty(capture=dsbs_f21_cpm),
                            },
                            "sale": {"shopFee": dsbs_f21_offer.fee, "brokeredFee": 0},
                        }
                    },
                    {
                        "debug": {
                            "offerTitle": "CPC-30",
                            "sale": {"minBid": 9, "clickPrice": 9, "brokeredFee": 0, "shopFee": 0},
                        }
                    },
                ],
                preserve_order=True,
            )

            self.assertAlmostEqual(drop_f36_offer.cpa_cpm(config), int(dropship_f36_cpm.value), delta=1500)
            self.assertAlmostEqual(drop_f35_offer.cpa_cpm(config), int(dropship_f35_cpm.value), delta=1500)
            self.assertAlmostEqual(onep_f34_offer.cpa_cpm(config), int(onep_f34_cpm.value), delta=1500)
            self.assertAlmostEqual(dsbs_f20_offer.cpa_cpm(config), int(dsbs_f20_cpm.value), delta=1500)
            self.assertAlmostEqual(dsbs_f21_offer.cpa_cpm(config), int(dsbs_f21_cpm.value), delta=1500)

            self.show_log.expect(
                ware_md5=drop_f36_offer.waremd5,
                ue_operations=Round(drop_f36_offer.ue_raw()),
                shop_fee_ab=brokered_fee_12,
                fee=DEFAULT_TRANSACTION_FEE,
                shop_fee=drop_f36_offer.fee,
            )
            self.show_log.expect(
                ware_md5=drop_f35_offer.waremd5,
                ue_operations=Round(drop_f35_offer.ue_raw()),
                shop_fee_ab=brokered_fee_23,
                fee=DEFAULT_TRANSACTION_FEE,
                shop_fee=drop_f35_offer.fee,
            )
            self.show_log.expect(
                ware_md5=onep_f34_offer.waremd5,
                ue_operations=Round(onep_f34_offer.ue_raw()),
                shop_fee_ab=0,
                fee=0,
                shop_fee=onep_f34_offer.fee,
            )

        """
        Тестируем CPA аукцион с дефольными настройками
        """
        hyper_id = 701

        config = AuctionConfig(e=0, f=0, p=2.0, a=0, c=50, d=0, b=1, g=0, cpa_multiplier=50)

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 50,
            "market_ranging_blue_offer_priority_eq_dsbs": 0,
            "market_set_1p_fee_recommended": 0,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # check top 6 (pp=6)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=6&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        check_response(response)

        # popular sorting (pp=18)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=18&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        check_response(response)

    def test_fees_and_bids_null(self):
        """
        Проверяем, что флаг market_set_fees_and_bids_null зануляет фи и делает ставки минимальными
        """
        hyper_id = 701

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 50,
            "market_ranging_blue_offer_priority_eq_dsbs": 0,
            "market_set_fees_and_bids_null": 1,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # check top 6 (pp=6)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=6&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        offers_count = 6  # 5 CPA офферов и 1 CPC оффер
        # Для CPA офферов тоже есть bid и minBid, как и для CPC, для них тоже bid == minBid в этом эксперименте
        # Для каждого оффера создаём Capture на каждое из свойств: bid, minBid, clickPrice, brokeredClickPrice
        sale_descriptors = [
            {
                prop_name: Capture()
                for prop_name in ['bid', 'minBid', 'clickPrice', 'brokeredClickPrice', 'shopFee', 'brokeredFee']
            }
            for _ in range(offers_count)
        ]

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "debug": {
                            "sale": {prop_name: NotEmpty(capture=sale_props[prop_name]) for prop_name in sale_props},
                        },
                    }
                    for sale_props in sale_descriptors
                ],
            },
        )

        for sale_prop in sale_descriptors:
            # Проверяем, что ставки и списания равны minbid
            curr_minbid = sale_prop['minBid'].value
            for prop_name in ['bid', 'clickPrice', 'brokeredClickPrice']:
                self.assertAlmostEqual(curr_minbid, sale_prop[prop_name].value, delta=0.0001)
            # Проверяем, что minbid не занулился
            self.assertTrue(curr_minbid != 0)
            # Проверяем, что фи занулились
            for fee_prop_name in ['shopFee', 'brokeredFee']:
                self.assertAlmostEqual(sale_prop[fee_prop_name].value, 0, delta=0.0001)

    def test_main_blue_offer_priority_eq_dsbs(self):
        """
        @see https://st.yandex-team.ru/MARKETOUT-36627
        Тестируем, когда приоритет синих офферов равен приоритету DSBS
        В результате в выдаче оффера меняются местами. dsbs_f20_offer встает на вторую позицию.
        Оффера сортируются по cpm
        """
        hyper_id = 701

        config = AuctionConfig(e=0, f=0, p=2.0, a=0, c=50, d=0, b=1, g=0, cpa_multiplier=50)

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 50,
            "market_ranging_blue_offer_priority_eq_dsbs": 1,
            "market_set_1p_fee_recommended": 0,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )

        # Создаем Capture что бы распарсить из выдачи cpm-ы
        dropship_f35_cpm = Capture()
        dropship_f36_cpm = Capture()
        onep_f34_cpm = Capture()
        dsbs_f20_cpm = Capture()
        dsbs_f21_cpm = Capture()

        drop_f36_offer = blue_offers_701_dict["THIRD-PARTY-F36-07000w"]
        drop_f35_offer = blue_offers_701_dict["THIRD-PARTY-F35-07500w"]
        onep_f34_offer = blue_offers_701_dict["FIRST-PARTY-F34-07000w"]
        dsbs_f20_offer = dsbs_offers_701_dict["DSBS-701-20"]
        dsbs_f21_offer = dsbs_offers_701_dict["DSBS-701-21"]

        brokered_fee_1 = calculate_bids(drop_f36_offer, dsbs_f20_offer, config)
        brokered_fee_2 = calculate_bids(dsbs_f20_offer, drop_f35_offer, config)
        brokered_fee_3 = calculate_bids(drop_f35_offer, onep_f34_offer, config)
        brokered_fee_4 = calculate_bids(onep_f34_offer, dsbs_f21_offer, config)

        self.assertFragmentIn(
            response,
            [
                {
                    "fee": str(brokered_fee_1 / FEE_MULTIPLIER),
                    "feeShowPlain": Wildcard("fee: \"" + str(brokered_fee_1 / FEE_MULTIPLIER) + "\"*"),
                    "debug": {
                        "wareId": drop_f36_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(drop_f36_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(drop_f36_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dropship_f36_cpm),
                        },
                        "sale": {"shopFee": drop_f36_offer.fee, "brokeredFee": brokered_fee_1},
                    },
                },
                {
                    "debug": {
                        "offerTitle": dsbs_f20_offer.title,
                        "properties": {
                            'UE_TOTAL_RAW': Round(dsbs_f20_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(dsbs_f20_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dsbs_f20_cpm),
                        },
                        "sale": {"shopFee": dsbs_f20_offer.fee, "brokeredFee": brokered_fee_2},
                    }
                },
                {
                    "debug": {
                        "wareId": drop_f35_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(drop_f35_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(drop_f35_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dropship_f35_cpm),
                        },
                        "sale": {"shopFee": drop_f35_offer.fee, "brokeredFee": brokered_fee_3},
                    }
                },
                {
                    "debug": {
                        "wareId": onep_f34_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(onep_f34_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(onep_f34_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=onep_f34_cpm),
                        },
                        "sale": {"shopFee": onep_f34_offer.fee, "brokeredFee": brokered_fee_4},
                    }
                },
                {
                    "debug": {
                        "offerTitle": dsbs_f21_offer.title,
                        "properties": {
                            'UE_TOTAL_RAW': Round(dsbs_f21_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(dsbs_f21_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dsbs_f21_cpm),
                        },
                        "sale": {"shopFee": dsbs_f21_offer.fee, "brokeredFee": 0},
                    }
                },
                {
                    "debug": {
                        "offerTitle": "CPC-30",
                        "sale": {"minBid": 9, "clickPrice": 9, "brokeredFee": 0, "shopFee": 0},
                    }
                },
                {
                    "debug": {
                        "offerTitle": "CPC-31",
                        "sale": {"minBid": 9, "clickPrice": 9, "brokeredFee": 0, "shopFee": 0},
                    }
                },
            ],
            preserve_order=True,
        )

        self.assertAlmostEqual(drop_f36_offer.cpa_cpm(config), int(dropship_f36_cpm.value), delta=1500)
        self.assertAlmostEqual(drop_f35_offer.cpa_cpm(config), int(dropship_f35_cpm.value), delta=1500)
        self.assertAlmostEqual(onep_f34_offer.cpa_cpm(config), int(onep_f34_cpm.value), delta=1500)
        self.assertAlmostEqual(dsbs_f20_offer.cpa_cpm(config), int(dsbs_f20_cpm.value), delta=1500)
        self.assertAlmostEqual(dsbs_f21_offer.cpa_cpm(config), int(dsbs_f21_cpm.value), delta=1500)

        self.show_log.expect(
            ware_md5=drop_f36_offer.waremd5,
            ue_operations=Round(drop_f36_offer.ue_raw()),
            shop_fee_ab=brokered_fee_1,
            fee=DEFAULT_TRANSACTION_FEE,
            shop_fee=drop_f36_offer.fee,
        )
        self.show_log.expect(
            ware_md5=drop_f35_offer.waremd5,
            ue_operations=Round(drop_f35_offer.ue_raw()),
            shop_fee_ab=brokered_fee_3,
            fee=DEFAULT_TRANSACTION_FEE,
            shop_fee=drop_f35_offer.fee,
        )
        self.show_log.expect(
            ware_md5=onep_f34_offer.waremd5,
            ue_operations=Round(onep_f34_offer.ue_raw()),
            shop_fee_ab=brokered_fee_4,
            fee=0,
            shop_fee=onep_f34_offer.fee,
        )

    @classmethod
    def prepare_locality_boosting(cls):
        cls.index.models += [Model(hyperid=801, hid=101)]

        cls.index.mskus += [msku801]

        for blue_offer in blue_offers_801:
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, blue_offer.ts).respond(blue_offer.p_purchase)

        cls.index.regiontree += [
            Region(
                rid=26,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=11029,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=39, children=[Region(rid=123)]),
                        ],
                    ),
                ],
            )
        ]

    def test_external_cpc_params_doesnt_affect_sorting_auction(self):
        """
        Тестируем CPA аукцион с дефольными настройками
        """
        hyper_id = 701

        config = AuctionConfig(e=0, f=0, p=2.0, a=0, c=50, d=0, b=1, g=0, cpa_multiplier=50)

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 50,
            "market_ranging_blue_offer_priority_eq_dsbs": 0,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        dropship_f36_cpm = Capture()

        drop_f36_offer = blue_offers_701_dict["THIRD-PARTY-F36-07000w"]
        drop_f35_offer = blue_offers_701_dict["THIRD-PARTY-F35-07500w"]

        brokered_fee_12 = calculate_bids(drop_f36_offer, drop_f35_offer, config)

        cpc = Cpc.create_for_offer(
            click_price=71,
            offer_id='THIRD-PARTY-F36-07000w',
            bid=80,
            shop_id=1006,
            shop_fee=750,
            fee=750,
            minimal_fee=111,
        )
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=6&cpc=%s&debug=da&rearr-factors=%s"
            % (hyper_id, cpc, rearr_flags_str)
        )

        """
        Проверяем, что cpc не влияет на топ6
        """
        self.assertFragmentIn(
            response,
            [
                {
                    "fee": str(brokered_fee_12 / FEE_MULTIPLIER),
                    "feeShowPlain": Wildcard("fee: \"" + str(brokered_fee_12 / FEE_MULTIPLIER) + "\"*"),
                    "debug": {
                        "wareId": drop_f36_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(drop_f36_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(drop_f36_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dropship_f36_cpm),
                        },
                        "sale": {"shopFee": drop_f36_offer.fee, "brokeredFee": brokered_fee_12},
                    },
                }
            ],
            preserve_order=True,
        )
        self.assertAlmostEqual(drop_f36_offer.cpa_cpm(config), int(dropship_f36_cpm.value), delta=1500)

    def test_external_cpc_params_affect_do(self):
        """
        Проверяем, что cpc влияет на ДО
        """
        hyper_id = 201
        shop_fee = 750
        brokered_fee = 300
        waremd5 = 'FIRST-PARTY-F17--11-2w'

        cpc = Cpc.create_for_offer(
            click_price=71, offer_id=waremd5, bid=80, shop_id=21, shop_fee=shop_fee, fee=brokered_fee, minimal_fee=111
        )

        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_ranging_blue_offer_priority_eq_dsbs": 1,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
            "market_blue_buybox_max_price_rel_add_diff": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=defaultList&pp=6&cpc=%s&debug=da"
            "&rearr-factors=%s" % (hyper_id, cpc, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "fee": format(brokered_fee / FEE_MULTIPLIER, '.4f'),
                    "feeShowPlain": Wildcard("fee: \"" + format(brokered_fee / FEE_MULTIPLIER, '.4f') + "\"*"),
                    "debug": {"wareId": waremd5, "sale": {"shopFee": shop_fee, "brokeredFee": brokered_fee}},
                }
            ],
            preserve_order=True,
        )

    def test_cpc_param_for_another_offer_no_affect_do(self):
        """
        Проверяем, что cpc параметр от другого оффера, но с совпадающим shop_id не меняет ставку в ДО
        """
        hyper_id = 201
        cpc_shop_fee = 750
        cpc_brokered_fee = 300
        initial_shop_fee = 100

        waremd5 = 'DSBS1-DSBS1-F17--11-2w'
        ware_md5_another_offer = 'THIRD-PARTY-F16--23-2w'
        cpc = Cpc.create_for_offer(
            click_price=71,
            offer_id=ware_md5_another_offer,
            bid=80,
            shop_id=21,
            shop_fee=cpc_shop_fee,
            fee=cpc_brokered_fee,
            minimal_fee=111,
        )

        rearr_flags_dict = {
            "use_offer_type_priority_as_main_factor_in_do": 1,
            "market_ranging_blue_offer_priority_eq_dsbs": 1,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=defaultList&pp=6&cpc=%s&debug=da"
            "&rearr-factors=%s" % (hyper_id, cpc, rearr_flags_str)
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    "fee": format(initial_shop_fee / FEE_MULTIPLIER, '.4f'),
                    "feeShowPlain": Wildcard("fee: \"" + format(initial_shop_fee / FEE_MULTIPLIER, '.4f') + "\"*"),
                    "debug": {
                        "wareId": waremd5,
                        "sale": {"shopFee": initial_shop_fee, "brokeredFee": initial_shop_fee},
                    },
                }
            ],
            preserve_order=True,
        )

    def test_no_competition(self):
        """
        Тестируем, что в предельном случае, когда в каждой группе один оффер,
        все cpc bid-ы амнистируются к мин. бидам, а в fee к нулю.
        """

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_algo": "softpluspow",
            "market_ranging_cpa_by_ue_in_top_soft_plus_coef": 1,
            "market_ranging_cpa_by_ue_in_top_deg_soft_plus_pow": 2,
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_priority_blue_fee_from_params": 200,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 1,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_f": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 0,
            "market_ranging_cpa_by_ue_in_top_ue_coef_g": 1,
            "market_ranging_cpa_by_ue_in_top_addition_constant_d": 1000000,
            "market_ranging_blue_offer_priority_eq_dsbs": 0,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=401&offers-set=top&debug=da&rearr-factors=%s" % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "debug": {"wareId": "THIRD-PARTY-F10-40003w", "sale": {"brokeredFee": 0, "shopFee": 83}},
                },
                {
                    "entity": "offer",
                    "debug": {"offerTitle": "paid dsbs 20 middle", "sale": {"brokeredFee": 0, "shopFee": 100}},
                },
                {
                    "debug": {
                        "offerTitle": "paid cpc 30 middle",
                        "sale": {"brokeredClickPrice": 2, "brokeredFee": 0, "shopFee": 0},
                    }
                },
            ],
            preserve_order=True,
        )

    # DynamicPriceControlData(32, 150, 0),
    # DynamicPriceControlData(33, 150, 1)
    def test_dynamic_pricing_strategies_in_ranking(self):
        """
        Тестируем, что cpm расчитывается исходя из цены после стратегий дин. ценообразования.
        """
        hyper_id = 501
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_priority_blue_fee_from_params": 200,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 1,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_ranging_cpa_by_ue_in_top_ue_coef_g": 1,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
            "enable_offline_buybox_price": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # CPM = p*price * smooth(UE + fee) + RangingCpaByUeInTopAdditionConstantD
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "wareId": "THIRD-PARTY-F33-50103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F18-50103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F16-60103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F32-60103w", "prices": {"value": "1000"}},
            ],
        )

        rearr_flags_dict['enable_offline_buybox_price'] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )
        # c оффлайн байбоксом без указания региона, цена байбокса не определяется
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "wareId": "THIRD-PARTY-F33-50103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F18-50103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F16-60103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F32-60103w", "prices": {"value": "1100"}},
            ],
        )
        # c rids=213 цена падает до наименьшей по Москве
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&debug=da&rids=213&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            [
                {"entity": "offer", "wareId": "THIRD-PARTY-F33-50103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F18-50103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F16-60103w", "prices": {"value": "1000"}},
                {"entity": "offer", "wareId": "THIRD-PARTY-F32-60103w", "prices": {"value": "1000"}},
            ],
        )

    def test_dsbs_ue_contains_transaction_fee(self):
        """
        Тестируем, что UE DSBS офферов содержит transaction fee
        """
        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_algo": "softpluspow",
            "market_ranging_cpa_by_ue_in_top_soft_plus_coef": 1,
            "market_ranging_cpa_by_ue_in_top_deg_soft_plus_pow": 2,
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_priority_blue_fee_from_params": 200,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 1,
            "market_ranging_cpa_by_ue_in_top_ue_coef_g": 1,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            "place=productoffers&hyperid=401&offers-set=top&debug=da&rearr-factors=%s" % rearr_flags_str
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "debug": {
                        "offerTitle": "paid dsbs 20 middle",
                        "sale": {"brokeredFee": 0, "shopFee": 100},
                        "properties": {"TRANSACTION_FEE": "200", "FEE": "100"},
                    },
                },
            ],
            preserve_order=True,
        )

    def test_transaction_fee_for_1p(self):
        """
        Тестируем, что transaction_fee для 1p офферов это price - purchase price.
        """
        hyper_id = 601

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_coef_g": 1,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "FIRST-PARTY-F34-601-3w",
                    "debug": {"properties": {"TRANSACTION_FEE": "0", 'UE_TO_PRICE_RAW': Round(0.1)}},
                },
            ],
            preserve_order=True,
        )

    def test_all_ranging_params_are_set(self):
        """
        Тестируем CPA аукцион с настройками полностью отличными от дефолтных. Проверяем, что все параметры учитываются.
        """
        hyper_id = 701

        config = AuctionConfig(e=0.5, f=0.3, p=2.0, a=1, c=100, d=2000000, b=1.2, g=0.5, cpa_multiplier=50)

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": config.e,
            "market_ranging_cpa_by_ue_in_top_ue_add_f": config.f,
            "market_ranging_cpa_by_ue_in_top_deg_soft_plus_pow": config.p,
            "market_ranging_cpa_by_ue_in_top_gmv_coef_a": config.a,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": config.c,
            "market_ranging_cpa_by_ue_in_top_addition_constant_d": config.d,
            "market_ranging_cpa_by_ue_in_top_ue_coef_g": config.g,
            "market_ranging_cpa_by_ue_in_top_coef_b": config.b,
            "market_set_1p_fee_recommended": 0,
            "market_buybox_auction_coef_w": 0,
            "market_ranging_cpa_by_ue_in_top_rp_fee_coef_w": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )

        dropship_f36_cpm = Capture()
        dropship_f36_offer = blue_offers_701_dict["THIRD-PARTY-F36-07000w"]
        self.assertFragmentIn(
            response,
            [
                {
                    "debug": {
                        "wareId": dropship_f36_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(dropship_f36_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(dropship_f36_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dropship_f36_cpm),
                        },
                    }
                }
            ],
        )

        self.assertAlmostEqual(dropship_f36_offer.cpa_cpm(config), int(dropship_f36_cpm.value), delta=1500)

    def test_prices_not_top6(self):
        """
        Тестируем "Цены"
        """
        hyper_id = 901

        rearr_flags_dict = {
            "market_buybox_by_supplier_on_white": 1,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_blue_offer_priority_eq_dsbs": 0,
            "market_documents_search_trace_offers_list": hyper_id,  # Проверим ещё, что в ценах работают трейсы поиска https://st.yandex-team.ru/MARKETOUT-40959
        }

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        prices_desktop_pp = 21
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=%s&debug=da&rearr-factors=%s"
            % (hyper_id, prices_desktop_pp, rearr_flags_str)
        )
        # Первым идёт ДО, но его не проверяем - тест на "Цены"
        shop_fees = [1050, 1000, 950, 900, 850, 800, 750, 700, 650]
        brokered_fees = [546, 478, 410, 339, 266, 191, 116, 35, 0]
        offer_titles = ["paid dsbs 901_" + str(i) + " prices" for i in range(9, 0, -1)]

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "offerTitle": offer_title,
                            "sale": {
                                "vBid": 20,
                                "shopFee": shop_fee,
                                "brokeredFee": brokered_fee,
                            },
                        },
                    }
                    for offer_title, shop_fee, brokered_fee in zip(offer_titles, shop_fees, brokered_fees)
                ],
            },
        )
        # Проверим, что трейсы поиска работают для цен
        self.assertFragmentIn(
            response,
            {
                "docs_search_trace_offers_list": {
                    "traces": [
                        {  # Трейс для модели
                            "document": str(hyper_id),
                            "type": "MODEL",
                            "stats_for_model": {
                                # Проверяем, что каждую стадию проходят все офферы
                                stats_stage_name: {"1": {"count": 11}}  # Всего 11 офферов
                                for stats_stage_name in [
                                    "offers_in_accept_doc",
                                    "offers_passed_accept_doc",
                                    "offers_in_relevance",
                                    "offers_passed_relevance",
                                    "offers_in_rearrange",
                                ]
                            },
                        }
                    ]
                }
            },
        )

        # Теперь проверим, что флаг работает - с другой конверсией будут другие brokered fee
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_prices"] = 0.05
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=%s&debug=da&rearr-factors=%s"
            % (hyper_id, prices_desktop_pp, rearr_flags_str)
        )
        brokered_fees = [366, 296, 226, 153, 79, 3, 0, 0, 0]
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "offerTitle": offer_title,
                            "sale": {
                                "vBid": 20,
                                "shopFee": shop_fee,
                                "brokeredFee": brokered_fee,
                            },
                        },
                    }
                    for offer_title, shop_fee, brokered_fee in zip(offer_titles, shop_fees, brokered_fees)
                ],
            },
        )

    def test_vendor_conversion_flag(self):
        """
        Тестируем, что флаг с конверсией для топ 6 работает (market_money_vendor_cpc_to_cpa_conversion_top6)
        Тестируем с дефолтным значением флага
        """
        hyper_id = 991

        rearr_flags_dict = {
            "market_buybox_by_supplier_on_white": 1,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_blue_offer_priority_eq_dsbs": 0,
            "market_set_1p_fee_recommended": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # check top 6 (pp=6)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=6&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )

        # Первым идёт ДО, но его не проверяем - тест на топ 6
        shop_fees = [800, 750, 700, 650, 600]
        brokered_fees = [97, 83, 70, 57, RESERVED_FEE]
        offers_descriptors = [
            "paid dsbs 991_4 expensive",
            "paid dsbs 991_3 expensive",
            "paid dsbs 991_2 expensive",
            "paid dsbs 991_1 expensive",
            "paid dsbs 991_0 expensive",
        ]

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "offerTitle": offer_descriptor,
                            "sale": {"shopFee": shop_fee, "brokeredFee": brokered_fee, "vBid": 10},
                        },
                    }
                    for offer_descriptor, shop_fee, brokered_fee in zip(offers_descriptors, shop_fees, brokered_fees)
                ],
            },
            preserve_order=True,
        )

        """
        Ставим другое значение конверсии
        """
        rearr_flags_dict["market_money_vendor_cpc_to_cpa_conversion_top6"] = 0.05
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=list&pp=6&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )

        # Для другой конверсии поменяются списанные fee при тех же ставках
        brokered_fees = [65, 54, 45, 36, RESERVED_FEE]

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "offerTitle": offer_descriptor,
                            "sale": {"shopFee": shop_fee, "brokeredFee": brokered_fee, "vBid": 10},
                        },
                    }
                    for offer_descriptor, shop_fee, brokered_fee in zip(offers_descriptors, shop_fees, brokered_fees)
                ],
            },
            preserve_order=True,
        )

    def test_main_rp_fee_in_top6(self):
        """
        Тестируем учёт rp_fee в топ-6 при равном приоритете DSBS
        """
        hyper_id = 701

        config = AuctionConfig(e=0, f=0, p=2.0, a=0, c=50, d=0, b=1, g=0, w=1, cpa_multiplier=50)

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 50,
            "market_ranging_blue_offer_priority_eq_dsbs": 1,
            "market_set_1p_fee_recommended": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&pp=6&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )

        # Создаем Capture что бы распарсить из выдачи cpm-ы
        dropship_f35_cpm = Capture()
        dropship_f36_cpm = Capture()
        onep_f34_cpm = Capture()
        dsbs_f20_cpm = Capture()
        dsbs_f21_cpm = Capture()

        drop_f36_offer = blue_offers_701_dict["THIRD-PARTY-F36-07000w"]
        drop_f35_offer = blue_offers_701_dict["THIRD-PARTY-F35-07500w"]
        onep_f34_offer = blue_offers_701_dict["FIRST-PARTY-F34-07000w"]
        dsbs_f20_offer = dsbs_offers_701_dict["DSBS-701-20"]
        dsbs_f21_offer = dsbs_offers_701_dict["DSBS-701-21"]

        brokered_fee_1 = calculate_bids(drop_f36_offer, dsbs_f20_offer, config)
        brokered_fee_2 = calculate_bids(dsbs_f20_offer, drop_f35_offer, config)
        brokered_fee_3 = calculate_bids(drop_f35_offer, onep_f34_offer, config)
        brokered_fee_4 = calculate_bids(onep_f34_offer, dsbs_f21_offer, config)

        self.assertFragmentIn(
            response,
            [
                {
                    "fee": str(brokered_fee_1 / FEE_MULTIPLIER),
                    "feeShowPlain": Wildcard("fee: \"" + str(brokered_fee_1 / FEE_MULTIPLIER) + "\"*"),
                    "debug": {
                        "wareId": drop_f36_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(drop_f36_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(drop_f36_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dropship_f36_cpm),
                        },
                        "sale": {"shopFee": drop_f36_offer.fee, "brokeredFee": brokered_fee_1},
                    },
                },
                {
                    "debug": {
                        "offerTitle": dsbs_f20_offer.title,
                        "properties": {
                            'UE_TOTAL_RAW': Round(dsbs_f20_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(dsbs_f20_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dsbs_f20_cpm),
                        },
                        "sale": {"shopFee": dsbs_f20_offer.fee, "brokeredFee": brokered_fee_2},
                    }
                },
                {
                    "debug": {
                        "wareId": drop_f35_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(drop_f35_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(drop_f35_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dropship_f35_cpm),
                        },
                        "sale": {"shopFee": drop_f35_offer.fee, "brokeredFee": brokered_fee_3},
                    }
                },
                {
                    "debug": {
                        "wareId": onep_f34_offer.waremd5,
                        "properties": {
                            'UE_TOTAL_RAW': Round(onep_f34_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(onep_f34_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=onep_f34_cpm),
                        },
                        "sale": {"shopFee": onep_f34_offer.fee, "brokeredFee": brokered_fee_4},
                    }
                },
                {
                    "debug": {
                        "offerTitle": dsbs_f21_offer.title,
                        "properties": {
                            'UE_TOTAL_RAW': Round(dsbs_f21_offer.ue_total(config)),
                            'SMOOTHED_UE_TO_PRICE': Round(dsbs_f21_offer.smooth_ue(config)),
                            'CPM': NotEmpty(capture=dsbs_f21_cpm),
                        },
                        "sale": {"shopFee": dsbs_f21_offer.fee, "brokeredFee": RESERVED_FEE},
                    }
                },
                {
                    "debug": {
                        "offerTitle": "CPC-30",
                        "sale": {"minBid": 9, "clickPrice": 9, "brokeredFee": 0, "shopFee": 0},
                    }
                },
            ],
            preserve_order=True,
        )

        self.assertAlmostEqual(drop_f36_offer.cpa_cpm(config), int(dropship_f36_cpm.value), delta=1500)
        self.assertAlmostEqual(drop_f35_offer.cpa_cpm(config), int(dropship_f35_cpm.value), delta=1500)
        self.assertAlmostEqual(onep_f34_offer.cpa_cpm(config), int(onep_f34_cpm.value), delta=1500)
        self.assertAlmostEqual(dsbs_f20_offer.cpa_cpm(config), int(dsbs_f20_cpm.value), delta=1500)
        self.assertAlmostEqual(dsbs_f21_offer.cpa_cpm(config), int(dsbs_f21_cpm.value), delta=1500)

        self.show_log.expect(
            ware_md5=drop_f36_offer.waremd5,
            ue_operations=Round(drop_f36_offer.ue_raw()),
            shop_fee_ab=brokered_fee_1,
            fee=DEFAULT_TRANSACTION_FEE,
            shop_fee=drop_f36_offer.fee,
        )
        self.show_log.expect(
            ware_md5=drop_f35_offer.waremd5,
            ue_operations=Round(drop_f35_offer.ue_raw()),
            shop_fee_ab=brokered_fee_3,
            fee=DEFAULT_TRANSACTION_FEE,
            shop_fee=drop_f35_offer.fee,
        )
        self.show_log.expect(
            ware_md5=onep_f34_offer.waremd5,
            ue_operations=Round(onep_f34_offer.ue_raw()),
            shop_fee_ab=brokered_fee_4,
            fee=0,
            shop_fee=onep_f34_offer.fee,
        )

    def test_main_vendor_fee_with_rp_fee(self):
        hyper_id = 911

        config = AuctionConfig(e=0, f=0, p=2.0, a=0, c=50, d=0, b=1, g=0, w=1, cpa_multiplier=50)

        rearr_flags_dict = {
            "market_ranging_cpa_by_ue_in_top": 1,
            "use_offer_type_priority_as_main_factor_in_top": 1,
            "market_buybox_by_supplier_on_white": 1,
            "market_uncollapse_supplier": 1,
            "market_ranging_cpa_by_ue_in_top_use_ue_to_price": 0,
            "market_ranging_cpa_by_ue_in_top_av_courier_shop_price": 0,
            "market_blue_buybox_storage_dropship_split_cost": 0,
            "market_ranging_cpa_by_ue_in_top_ue_add_e": 0,
            "market_ranging_cpa_by_ue_in_top_purchase_coef_c": 50,
            "market_ranging_blue_offer_priority_eq_dsbs": 1,
            "market_set_1p_fee_recommended": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=top&pp=6&debug=da&rearr-factors=%s" % (hyper_id, rearr_flags_str)
        )

        drop_f36_offer = blue_offers_911_dict["911-3P-F36P7000-F1500w"]
        drop_f35_offer = blue_offers_911_dict["911-3P-F35P7500-F1050w"]
        onep_f34_offer = blue_offers_911_dict["911-1P-F34P7000-F0500w"]

        brokered_fee_1 = calculate_bids(drop_f36_offer, drop_f35_offer, config)
        brokered_fee_2 = calculate_bids(drop_f35_offer, onep_f34_offer, config)

        self.assertFragmentIn(
            response,
            [
                {
                    "fee": str(brokered_fee_1 / FEE_MULTIPLIER),
                    "feeShowPlain": Wildcard("fee: \"" + str(brokered_fee_1 / FEE_MULTIPLIER) + "\"*"),
                    "debug": {
                        "wareId": drop_f36_offer.waremd5,
                        "sale": {"shopFee": drop_f36_offer.fee, "brokeredFee": brokered_fee_1},
                    },
                },
                {
                    "debug": {
                        "wareId": drop_f35_offer.waremd5,
                        "sale": {"shopFee": drop_f35_offer.fee, "brokeredFee": brokered_fee_2},
                    }
                },
                {
                    "debug": {
                        "wareId": onep_f34_offer.waremd5,
                        "sale": {"shopFee": onep_f34_offer.fee, "brokeredFee": RESERVED_FEE},
                    }
                },
            ],
            preserve_order=True,
        )

        self.show_log.expect(
            ware_md5=drop_f36_offer.waremd5,
            ue_operations=Round(drop_f36_offer.ue_raw()),
            shop_fee_ab=brokered_fee_1,
            fee=DEFAULT_TRANSACTION_FEE,
            shop_fee=drop_f36_offer.fee,
            offer_vendor_fee=drop_f36_offer.vendor_fee,
            reserve_price_fee=RESERVED_FEE,
        )
        self.show_log.expect(
            ware_md5=drop_f35_offer.waremd5,
            ue_operations=Round(drop_f35_offer.ue_raw()),
            shop_fee_ab=brokered_fee_2,
            fee=DEFAULT_TRANSACTION_FEE,
            shop_fee=drop_f35_offer.fee,
            offer_vendor_fee=drop_f35_offer.vendor_fee,
            reserve_price_fee=RESERVED_FEE,
        )
        self.show_log.expect(
            ware_md5=onep_f34_offer.waremd5,
            ue_operations=Round(onep_f34_offer.ue_raw()),
            shop_fee_ab=RESERVED_FEE,
            fee=0,
            shop_fee=onep_f34_offer.fee,
            offer_vendor_fee=onep_f34_offer.vendor_fee,
            reserve_price_fee=RESERVED_FEE,
        )

    def test_documents_tracer(self):
        """
        Тестируем трейсы поиска в топ 6
        """
        hyper_id = 701
        requested_offer = "FIRST-PARTY-F34-07000w"

        rearr_flags_dict = {
            "market_documents_search_trace_offers_list": str(hyper_id)
            + ","
            + requested_offer,  # Трейс поиска для модели 701
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        # check top 6 (pp=6)
        response = self.report.request_json(
            "place=productoffers&hyperid=%s&offers-set=listCpa&pp=6&debug=da&rearr-factors=%s"
            % (hyper_id, rearr_flags_str)
        )

        wareIds = [Capture() for _ in range(6)]  # Для каждого из топ 6 распарсить wareId

        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "wareId": NotEmpty(capture=wareId_capture),
                        }
                        for wareId_capture in wareIds
                    ]
                },
                "debug": {
                    "docs_search_trace_default_offer": Absent(),  # Трейсер для байбокса отсутствует, он включается другим флагом
                    "docs_search_trace_offers_list": {  # Трейсер для топ 6
                        "traces": [
                            {
                                "document": "701",  # Трейс для модели
                                "type": "MODEL",
                                "on_page": False,  # Трейс для модели, всегда будет False
                                "stats_for_model": {
                                    stats_stage_name: {  # На каждой из стадий по 7 офферов, никто нигде не потеряется
                                        "1": {
                                            "count": 7,
                                        }
                                    }
                                    for stats_stage_name in [
                                        "offers_in_accept_doc",
                                        "offers_passed_accept_doc",
                                        "offers_in_relevance",
                                        "offers_passed_relevance",
                                        "offers_in_rearrange",
                                    ]
                                },
                            },
                            {
                                "document": requested_offer,  # Трейс для оффера
                                "type": "OFFER_BY_WARE_MD5",
                                "in_index": True,
                                "in_accept_doc": True,
                                "passed_accept_doc": True,
                                "in_relevance": True,
                                "passed_relevance": True,
                                "in_rearrange": True,
                                "on_page": True,  # Показан на странице
                                "passed_rearrange": True,  # Прошёл в топ 6
                            },
                        ],
                    },
                },
            },
        )

        # Проверим, что оффер действительно показался (что соответствует трассировке)
        self.assertTrue(requested_offer in set([wareId.value for wareId in wareIds]))

    def test_top6_formula_in_shows_log(self):
        """
        Проверяем, что формула топ 6 пишется в shows log
        """
        hyper_id = 701

        self.report.request_json("place=productoffers&hyperid=%s&offers-set=listCpa&pp=6&debug=da" % hyper_id)

        self.show_log_tskv.expect(
            top6_formula_value=Round(0.2, 2),
        )
        self.show_log_tskv.expect(
            top6_formula_value=Round(0.1, 2),
        )


if __name__ == '__main__':
    main()
