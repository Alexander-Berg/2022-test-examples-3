#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
import json

from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicBlueGenericBundlesPromos,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    GLType,
    GLParam,
    GLValue,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Promo,
    PromoByCart,
    Region,
    RegionalDelivery,
    Shop,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.offer_promo import PromoType
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.matcher import Absent, Contains, ElementCount, Greater, NoKey, NotEmpty, Regex, NotEmptyList
from core.types.offer_promo import make_generic_bundle_content, OffersMatchingRules
from core.report import REQUEST_TIMESTAMP
from core.types.combinator import (
    CombinatorOffer,
    DeliveryItem,
    Destination,
    OrdersSplitRequest,
    SplitBasket,
    OrdersSplitResponse,
)

from collections import namedtuple

from market.combinator.proto.grpc.combinator_pb2 import (
    SplitStatus,
    PickupPointType,
)

from core.combinator import DeliveryStats

PLACE_COMBINE_WITH_RGB = "place=combine&rgb=green_with_blue&use-virt-shop=0"
USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

EKB_WAREHOUSE_ID = 300
SPB_WAREHOUSE_ID = 301
SAMARA_WAREHOUSE_ID = 302
NOVOSIBIRSK_WAREHOUSE_ID = 303

CombinatorRequestItem = namedtuple("CombinatorRequestItem", "offer count warehouse")


class _Hids:
    any_category = 1234567


class _GlParamIds:
    color = 14871214
    jump_table_size = 355436465
    jump_table_color = 253263464
    woman_dress_size_units = 34325346


class _ColorIds:
    gray = 14896295
    grafit = 23526426
    yellow = 34535664


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

    red_virtual_shop = Shop(
        fesh=101,
        name='red virtual_shop',
        fulfillment_virtual=True,
        virtual_shop_color=Shop.VIRTUAL_SHOP_RED,
        cpa=Shop.CPA_REAL,
    )

    DROPSHIP_SUPPLIER_ID = 4
    CHILD_WORLD_ID = 9
    FIRST_DIGITAL_SUPPLIER_ID = 1003
    SECOND_DIGITAL_SUPPLIER_ID = 1004
    FASHION_1P_SUPPLIER_ID = 2001
    FASHION_1P_SUPPLIER_FEED = 3001

    def make_supplier(
        supplier_id,
        feed_id,
        name,
        warehouse_id,
        type=Shop.THIRD_PARTY,
        is_fulfillment=True,
        client_id=None,
        direct_shipping=True,
    ):
        return Shop(
            fesh=supplier_id,
            datafeed_id=feed_id,
            priority_region=2,
            name=name,
            tax_system=Tax.OSN,
            supplier_type=type,
            blue=Shop.BLUE_REAL,
            cpa=Shop.CPA_REAL if is_fulfillment else Shop.CPA_NO,
            warehouse_id=warehouse_id,
            fulfillment_program=is_fulfillment,
            client_id=client_id or supplier_id,
            direct_shipping=direct_shipping,
        )

    # здесь везде один client supplier = 2
    blue_supplier_172 = make_supplier(2, 21, 'shop1_priority_1', 172, type=Shop.FIRST_PARTY)
    blue_supplier_147 = make_supplier(6, 22, 'shop1_priority_2', 147, type=Shop.FIRST_PARTY, client_id=2)
    blue_supplier_148 = make_supplier(2, 23, 'warehouse_disabled_in_lms', 148, type=Shop.FIRST_PARTY)
    blue_supplier_149 = make_supplier(2, 24, 'shop1_priority_3', 149, type=Shop.FIRST_PARTY)
    blue_supplier_145 = make_supplier(2, 25, 'shop1_priority_4', 145, type=Shop.FIRST_PARTY)

    # здесь другой client supplier = 3
    blue_supplier2_147 = make_supplier(6, 3, 'shop2_priority_1', 147, client_id=3)

    # здесь еще один client supplier = 10
    blue_supplier3_172 = make_supplier(6, 66, 'shop3_priority_1', 172, client_id=10)

    # здесь умышленно другой supplier, но тот же client supplier = 10 (для проверки дубликатов)
    blue_supplier4_172 = make_supplier(10, 101, 'shop4_priority_1', 172, client_id=10)

    yaphone_supplier_1 = make_supplier(7, 31, 'yaphone_supplier_1_172', 172, client_id=6)
    yaphone_supplier_2 = make_supplier(8, 32, 'yaphone_supplier_2_147', 147, client_id=6)
    yaphone_supplier_3 = make_supplier(9, 33, 'yaphone_supplier_2_300', EKB_WAREHOUSE_ID, client_id=6)
    yaphone_supplier_4 = make_supplier(10, 34, 'yaphone_supplier_2_302', SAMARA_WAREHOUSE_ID, client_id=6)
    yaphone_supplier_5 = make_supplier(11, 35, 'yaphone_supplier_2_301', SPB_WAREHOUSE_ID, client_id=6)
    yaphone_supplier_6 = make_supplier(12, 36, 'yaphone_supplier_2_303', NOVOSIBIRSK_WAREHOUSE_ID, client_id=6)
    dropship_444 = make_supplier(DROPSHIP_SUPPLIER_ID, 4, 'dropship at warehouse 444', 444, is_fulfillment=False)
    dropship_555 = make_supplier(DROPSHIP_SUPPLIER_ID, 5, 'dropship at warehouse 555', 555, is_fulfillment=False)
    dropship_666 = make_supplier(DROPSHIP_SUPPLIER_ID, 6, 'dropship at warehouse 666', 666, is_fulfillment=False)
    child_world_888 = make_supplier(
        supplier_id=CHILD_WORLD_ID,
        feed_id=88,
        name='Child world at warehouse 888',
        warehouse_id=888,
        direct_shipping=False,
        client_id=8,
    )
    child_world_999 = make_supplier(
        supplier_id=CHILD_WORLD_ID,
        feed_id=99,
        name='Child world at warehouse 999',
        warehouse_id=999,
        direct_shipping=False,
        client_id=8,
    )

    fashion_1p_suplier = make_supplier(
        supplier_id=FASHION_1P_SUPPLIER_ID,
        feed_id=FASHION_1P_SUPPLIER_FEED,
        name='shop_fashion_1p',
        warehouse_id=172,
        type=Shop.FIRST_PARTY,
    )

    EXPRESS_DELIVERY_SUPPLIER_ID = 4001
    EXPRESS_DELIVERY_FEED_ID = 4002
    EXPRESS_DELIVERY_WAREHOUSE_ID = 4003
    EXPRESS_DELIVERY_BUCKET_ID = 4004

    LOW_PRIORITY_DROPSHIP_SUPPLIER_ID = 5001
    LOW_PRIORITY_DROPSHIP_FEED_ID = 5002
    LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID = 5003
    LOW_PRIORITY_DROPSHIP_BUCKET_ID = 5004

    HIGH_PRIORITY_DROPSHIP_SUPPLIER_ID = 6001
    HIGH_PRIORITY_DROPSHIP_FEED_ID = 6002
    HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID = 6003
    HIGH_PRIORITY_DROPSHIP_BUCKET_ID = 6004

    express_dropship_shop = Shop(
        fesh=EXPRESS_DELIVERY_SUPPLIER_ID,
        datafeed_id=EXPRESS_DELIVERY_FEED_ID,
        warehouse_id=EXPRESS_DELIVERY_WAREHOUSE_ID,
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    dropship_low_priority = Shop(
        fesh=LOW_PRIORITY_DROPSHIP_SUPPLIER_ID,
        datafeed_id=LOW_PRIORITY_DROPSHIP_FEED_ID,
        warehouse_id=LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID,
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )

    dropship_high_priority = Shop(
        fesh=HIGH_PRIORITY_DROPSHIP_SUPPLIER_ID,
        datafeed_id=HIGH_PRIORITY_DROPSHIP_FEED_ID,
        warehouse_id=HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID,
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=False,
    )


class _Offers(object):
    def make_offer(price, shop_sku, supplier_id, feed_id, ware_md5, stock_store_count, cargo_types=[]):
        return BlueOffer(
            price=price,
            vat=Vat.VAT_10,
            feedid=feed_id,
            offerid=shop_sku,
            waremd5=ware_md5,
            supplier_id=supplier_id,
            stock_store_count=stock_store_count,
            weight=1,
            dimensions=OfferDimensions(length=10, width=10, height=10),
            cargo_types=cargo_types,
        )

    alyonka_s1_172 = make_offer(50, 'Alyonka_172', 2, 21, 'Alyonka_s1_172_______g', 15)
    alyonka_s1_147 = make_offer(40, 'Alyonka_147', 6, 22, 'Alyonka_s1_147_______g', 2)
    alyonka_s1_148 = make_offer(49, 'Alyonka_148', 2, 23, 'Alyonka_s1_148_______g', 7)
    alyonka_s1_149 = make_offer(51, 'Alyonka_149', 2, 24, 'Alyonka_s1_149_______g', 18)
    alyonka_s1_145 = make_offer(52, 'Alyonka_145', 2, 25, 'Alyonka_s1_145_______g', 18)
    alyonka_promo_s1_149 = make_offer(50, 'Alyonka_149_promo', 2, 24, 'Alyonka_s1_149_promo_g', 12)
    alyonka_s2_147 = make_offer(60, 'Alyonka_147_2', 6, 3, 'Alyonka_s2_147_______g', 1)
    alyonka_s3_172 = make_offer(50, 'Alyonka_172_3', 6, 66, 'Alyonka_s3_172_______g', 15)
    alyonka_s4_172 = make_offer(50, 'Alyonka_172_4', 10, 101, 'Alyonka_s4_172_______g', 15)

    iphone_s1_1001 = make_offer(115000, 'iPhone_11', 2, 21, 'iPhone_s1_1001_______g', 15)
    gift_case_1003 = make_offer(50, 'iPhoneCase_11', 2, 21, 'iPhoneCase_11________g', 1)

    coke_s1_172 = make_offer(34, 'Coke_172_1', 2, 21, 'CocaCola_s1_172______g', 8)
    coke_s1_148 = make_offer(35, 'Coke_148_1', 2, 23, 'CocaCola_s1_148______g', 25)
    coke_s2_147 = make_offer(35, 'Coke_147_2', 3, 3, 'CocaCola_s2_147______g', 20)

    fanta_s1_145 = make_offer(20, 'Fanta_145', 2, 25, 'Fanta_s1_145_________g', 1)
    fanta_s2_145 = make_offer(40, 'Fanta_145', 2, 25, 'Fanta_s2_145_________g', 10)
    fanta_s1_172 = make_offer(31, 'Fanta_172', 2, 21, 'Fanta_s1_172_________g', 10)
    fanta_s1_147 = make_offer(19, 'Fanta_147', 6, 22, 'Fanta_s1_147_________g', 10)

    sprite_s1_145 = make_offer(40, 'Sprite_145', 2, 25, 'Sprite_s1_145________g', 1)
    sprite_s1_172 = make_offer(30, 'Sprite_172', 2, 21, 'Sprite_s1_172________g', 10)
    sprite_s1_147 = make_offer(20, 'Sprite_147', 2, 22, 'Sprite_s1_147________g', 10)

    avocado_s2_147 = make_offer(35, 'Avocado_147_1', 3, 3, 'Avocado_s1_147_______g', 25)
    avocado_s1_148 = make_offer(35, 'Avocado_148_1', 2, 23, 'Avocado_s1_148_______g', 20)

    hoover_s1_147 = make_offer(15000, 'Hoover_147', 2, 22, 'Hoover_s1_147________g', 8)
    hoover_s1_147_blue = make_offer(15000, 'Hoover_147_blue', 2, 22, 'Hoover_s1_147_blue___g', 7)

    notebook_out_of_stock = make_offer(50000, 'Notebook_out_of_stock', 2, 21, 'Notebook_out_of_stockg', 0)

    offer_fashion_1p_600_cargo = make_offer(
        price=555,
        shop_sku='fashion_1p_600_cargo_t',
        supplier_id=_Shops.FASHION_1P_SUPPLIER_ID,
        feed_id=_Shops.FASHION_1P_SUPPLIER_FEED,
        ware_md5='fashion_1p_600_cargo_t',
        stock_store_count=20,
        cargo_types=[600],
    )

    blue_offer_any_category = make_offer(37, 'Any_shop_sku', 2, 21, 'vqJ08ZwpokiSjM4hG1J1Dv', 15)

    def make_dropship_offer(price, shop_sku, feed_id, ware_md5, stock_store_count, cargo_types=[]):
        return BlueOffer(
            price=price,
            vat=Vat.VAT_20,
            feedid=feed_id,
            offerid=shop_sku,
            waremd5=ware_md5,
            is_fulfillment=False,
            supplier_id=_Shops.DROPSHIP_SUPPLIER_ID,
            weight=1,
            dimensions=OfferDimensions(length=10, width=10, height=10),
            stock_store_count=stock_store_count,
            cargo_types=cargo_types,
        )

    fridge_444_offer = make_dropship_offer(35000, 'Fridge_444', 4, 'Refrigerator_444_____g', 7)
    fridge_555_offer = make_dropship_offer(35000, 'Fridge_555', 5, 'Refrigerator_555_____g', 8)
    fridge_555_offer_invalid = make_dropship_offer(
        35000, 'Fridge_555', 5, 'Refrigerat0r_555_____g', 8
    )  # протухший офер, в индексе его не будет, поскольку не добавляем в MSKU
    hoover_444_offer = make_dropship_offer(14990, 'Hoover_444', 4, 'VacuumCleaner_444____g', 10)
    hoover_555_offer = make_dropship_offer(15000, 'Hoover_555', 5, 'VacuumCleaner_555____g', 24)
    hoover_666_offer = make_dropship_offer(15000, 'Hoover_666', 6, 'VacuumCleaner_666____g', 25)
    microwave_555_offer = make_dropship_offer(12990, 'Microwave_555', 5, 'MicrowaveOven_555____g', 17)
    music_555_offer = make_dropship_offer(123, 'Music_555', 5, 'SoundOfMusic__555____g', 11)
    music_666_offer = make_dropship_offer(123, 'Music_666', 6, 'SoundOfMusic__666____g', 10)

    router_s1_172 = make_offer(2900, 'Router_172', 7, 31, 'Router_s1_172________g', 5)
    router_s1_147 = make_offer(2900, 'Router_147', 8, 32, 'Router_s1_147________g', 5)

    phone_1_172 = make_offer(9000, 'phone_1_172', 7, 31, 'yaphone_1_172________g', 5)
    phone_1_147 = make_offer(9000, 'phone_1_147', 8, 32, 'yaphone_1_147________g', 8)
    phone_1_300 = make_offer(9500, 'phone_1_300', 9, 33, 'yaphone_1_300________g', 2)
    phone_1_302 = make_offer(9600, 'phone_1_302', 10, 34, 'yaphone_1_302________g', 2)
    phone_1_301 = make_offer(9700, 'phone_1_301', 11, 35, 'yaphone_1_301________g', 2)
    phone_1_303 = make_offer(9800, 'phone_1_303', 12, 36, 'yaphone_1_303________g', 2)

    express_flowers = BlueOffer(
        offerid='express_flowers',
        waremd5='ExpressDropshipWaremdw',
        price=700,
        feedid=_Shops.EXPRESS_DELIVERY_FEED_ID,
        weight=5,
        is_express=True,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Shops.EXPRESS_DELIVERY_SUPPLIER_ID,
        delivery_buckets=[_Shops.EXPRESS_DELIVERY_BUCKET_ID],
        stock_store_count=10,
    )

    usual_flowers_low_priority = BlueOffer(
        offerid='usual_flowers_low_priority',
        waremd5='UsualDropshipWaremd01w',
        price=700,
        feedid=_Shops.LOW_PRIORITY_DROPSHIP_FEED_ID,
        weight=5,
        is_express=False,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Shops.LOW_PRIORITY_DROPSHIP_SUPPLIER_ID,
        delivery_buckets=[_Shops.LOW_PRIORITY_DROPSHIP_BUCKET_ID],
        stock_store_count=10,
    )

    usual_flowers_high_priority = BlueOffer(
        offerid='usual_flowers_high_priority',
        waremd5='UsualDropshipWaremd02w',
        price=700,
        feedid=_Shops.HIGH_PRIORITY_DROPSHIP_FEED_ID,
        weight=5,
        is_express=False,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Shops.HIGH_PRIORITY_DROPSHIP_SUPPLIER_ID,
        delivery_buckets=[_Shops.HIGH_PRIORITY_DROPSHIP_BUCKET_ID],
        stock_store_count=10,
    )

    retail_tushenka_offer = BlueOffer(
        offerid='retail_tushenka',
        waremd5='RetailTushenkaOffer__g',
        price=100,
        feedid=_Shops.EXPRESS_DELIVERY_FEED_ID,
        weight=5,
        is_express=False,
        dimensions=OfferDimensions(length=30, width=30, height=30),
        supplier_id=_Shops.EXPRESS_DELIVERY_SUPPLIER_ID,
        delivery_buckets=[_Shops.EXPRESS_DELIVERY_BUCKET_ID],
        stock_store_count=10,
        is_eda_retail=True,
    )


class _MSKUs(object):
    alyonka = MarketSku(
        title="Alyonka",
        hyperid=1,
        sku=1,
        blue_offers=[
            _Offers.alyonka_s1_172,
            _Offers.alyonka_s1_147,
            _Offers.alyonka_s1_148,
            _Offers.alyonka_s1_149,
            _Offers.alyonka_s1_145,
            _Offers.alyonka_s2_147,
            _Offers.alyonka_promo_s1_149,
            _Offers.alyonka_s3_172,
            _Offers.alyonka_s4_172,
        ],
        delivery_buckets=[801, 802],
    )

    shoes = MarketSku(
        title="Shoes",
        hyperid=3,
        sku=14,
        blue_offers=[_Offers.offer_fashion_1p_600_cargo],
        delivery_buckets=[801, 802],
    )

    coke = MarketSku(
        title="Coca Cola",
        hyperid=2,
        sku=2,
        blue_offers=[_Offers.coke_s1_172, _Offers.coke_s1_148, _Offers.coke_s2_147],
        delivery_buckets=[801, 802],
    )

    avocado = MarketSku(
        title="Avocado",
        hyperid=9,
        sku=11,
        blue_offers=[_Offers.avocado_s1_148, _Offers.avocado_s2_147],
        delivery_buckets=[801, 802],
    )

    notebook = MarketSku(
        title="Notebook", hyperid=2, sku=9, blue_offers=[_Offers.notebook_out_of_stock], delivery_buckets=[801, 802]
    )

    fridge = MarketSku(
        title="Refrigerator",
        hyperid=101,
        sku=101,
        blue_offers=[_Offers.fridge_444_offer, _Offers.fridge_555_offer],
        delivery_buckets=[803, 804],
    )

    hoover = MarketSku(
        title="Vacuum cleaner",
        hyperid=3,
        sku=3,
        blue_offers=[
            _Offers.hoover_s1_147,
            _Offers.hoover_s1_147_blue,
            _Offers.hoover_444_offer,
            _Offers.hoover_555_offer,
            _Offers.hoover_666_offer,
        ],
        delivery_buckets=[802, 803, 804, 805],
    )

    microwave = MarketSku(
        title="Microwave oven", hyperid=4, sku=4, blue_offers=[_Offers.microwave_555_offer], delivery_buckets=[804]
    )

    router = MarketSku(
        title="YaRouter",
        hyperid=66,
        hid=1,
        sku=66,
        blue_offers=[_Offers.router_s1_172, _Offers.router_s1_147],
        delivery_buckets=[801, 802],
    )

    phone = MarketSku(
        title="YaPhone",
        hyperid=6,
        sku=6,
        hid=2,
        blue_offers=[
            _Offers.phone_1_172,
            _Offers.phone_1_147,
            _Offers.phone_1_300,
            _Offers.phone_1_302,
            _Offers.phone_1_301,
            _Offers.phone_1_303,
        ],
        delivery_buckets=[801, 802, 899],
    )

    music = MarketSku(
        title="Sound of music",
        hyperid=7,
        sku=7,
        blue_offers=[_Offers.music_555_offer, _Offers.music_666_offer],
        delivery_buckets=[805],
    )

    unicorn = MarketSku(title="Unicorn", blue_offers=[], hyperid=5, sku=5)  # No offers

    iphone1 = MarketSku(
        title='iPhone', hyperid=6, sku=1001, blue_offers=[_Offers.iphone_s1_1001], delivery_buckets=[801, 802]
    )

    gift_case1 = MarketSku(
        title='iPhone Case Red',
        hyperid=1002,
        sku=1003,
        blue_offers=[_Offers.gift_case_1003],
        delivery_buckets=[801, 802],
    )

    fanta = MarketSku(
        title='Fanta',
        hyperid=1,
        sku=8,
        blue_offers=[_Offers.fanta_s1_145, _Offers.fanta_s2_145, _Offers.fanta_s1_147, _Offers.fanta_s1_172],
        delivery_buckets=[801, 802],
    )

    sprite = MarketSku(
        title='Sprite',
        hyperid=1,
        sku=13,
        blue_offers=[_Offers.sprite_s1_145, _Offers.sprite_s1_147, _Offers.sprite_s1_172],
        delivery_buckets=[801, 802],
    )

    flowers = MarketSku(
        title="Flowers",
        hyperid=154,
        sku=154,
        blue_offers=[
            _Offers.express_flowers,
            _Offers.usual_flowers_low_priority,
            _Offers.usual_flowers_high_priority,
        ],
    )

    retail_tushenka = MarketSku(
        title="Tushenka",
        hyperid=6666,
        sku=301,
        blue_offers=[_Offers.retail_tushenka_offer],
    )


class _WhiteOffers(object):
    no_msku_1001 = Offer(
        cpa=Offer.CPA_REAL,
        hyperid=7001,
        fesh=1001,
        price=100,
        waremd5='22222222222222gggggggg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[256, 10],
    )
    alyonka_1001 = Offer(
        cpa=Offer.CPA_REAL,
        hyperid=7001,
        fesh=1001,
        price=100,
        waremd5='33333333333333gggggggg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[256, 10],
        sku=7001,
    )
    no_msku_1002 = Offer(
        cpa=Offer.CPA_REAL,
        hyperid=7001,
        fesh=1002,
        price=100,
        waremd5='44444444444444gggggggg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[256, 10],
    )
    no_msku_1003 = Offer(
        cpa=Offer.CPA_REAL,
        hyperid=7010,
        fesh=1010,
        price=100,
        waremd5='55555555555555gggggggg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[256, 10],
        pickup_buckets=[5012],
    )


class _Promos(object):
    promo_iphone1 = Promo(
        promo_type=PromoType.GENERIC_BUNDLE,
        feed_id=21,
        key='JVvklxUgdnawSJPG4UhZ-1',
        url='http://localhost.ru/',
        generic_bundles_content=[
            make_generic_bundle_content(_Offers.iphone_s1_1001.offerid, _Offers.gift_case_1003.offerid, 1),
        ],
        offers_matching_rules=[OffersMatchingRules(feed_offer_ids=[[21, _Offers.iphone_s1_1001.offerid]])],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += [
            'market_nordstream=0',
            'enable_cart_split_on_combinator=0',
        ]
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        _Offers.iphone_s1_1001.promo = [_Promos.promo_iphone1]
        cls.index.hypertree += [
            HyperCategory(hid=1, name="Роутеры", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=2, name="Телефоны", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=3, name="Одежда", output_type=HyperCategoryType.GURU),
            HyperCategory(hid=91307, name="Еда", output_type=HyperCategoryType.GURU),
        ]

        cls.index.models += [
            Model(hid=1, title="Роутеры", hyperid=66),
            Model(hid=2, title="Телефоны", hyperid=6),
            Model(hid=3, title="Одежда", hyperid=667),
            Model(hid=91307, title="Еда", hyperid=6666),
        ]

        cls.index.regiontree += [
            Region(
                rid=213,
                children=[
                    Region(
                        rid=11029,
                        children=[  # let Rostov Region be in Moscow, for simpler tests)
                            Region(rid=39),
                        ],
                    ),
                    Region(
                        rid=11266,
                        children=[  # let Ekb Region be in Moscow, for simpler tests)
                            Region(rid=222),
                        ],
                    ),
                    Region(
                        rid=11084,
                        children=[  # let Samara Region be in Moscow, for simpler tests)
                            Region(rid=333),
                        ],
                    ),
                    Region(
                        rid=11316, children=[Region(rid=666)]  # let Novosibirsk Region be in Moscow, for simpler tests)
                    ),
                ],
            ),
            Region(rid=111, region_type=Region.COUNTRY),  # no delivery to this region
            Region(rid=444, region_type=Region.COUNTRY),  # delivery only from warehouse 444
            Region(
                rid=2,
                children=[
                    Region(rid=555),
                ],
            ),
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
            Outlet(fesh=1010, point_type=Outlet.FOR_PICKUP, region=213, point_id=10),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=5001,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=1, price=100)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5012,
                fesh=1010,
                carriers=[99],
                options=[PickupOption(outlet_id=10)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
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
            DynamicWarehouseInfo(id=172, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=147, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=EKB_WAREHOUSE_ID, home_region=11266, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=SAMARA_WAREHOUSE_ID, home_region=11084, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=SPB_WAREHOUSE_ID, home_region=2, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=NOVOSIBIRSK_WAREHOUSE_ID, home_region=11316, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=148, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=149, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=444, home_region=213),
            DynamicWarehouseInfo(id=555, home_region=213),
            DynamicWarehouseInfo(id=666, home_region=213),
            DynamicWarehouseInfo(id=888, home_region=213),
            DynamicWarehouseInfo(id=999, home_region=213),
            DynamicWarehouseInfo(id=1999, home_region=213),
            DynamicWarehouseInfo(id=2999, home_region=213),
            DynamicWarehouseInfo(id=_Shops.EXPRESS_DELIVERY_WAREHOUSE_ID, home_region=213, is_express=True),
            DynamicWarehouseInfo(id=_Shops.LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID, home_region=213, is_express=False),
            DynamicWarehouseInfo(id=_Shops.HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID, home_region=214, is_express=False),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=_Shops.child_world_888.warehouse_id,
                warehouse_to=172,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=3, region_to=225)],
            ),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=_Shops.child_world_999.warehouse_id,
                warehouse_to=147,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=3, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(
                99, "self-delivery", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
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
            DynamicDeliveryServiceInfo(
                167, "express all country courier", region_to_region_info=[delivery_service_region_to_region_info()]
            ),
            DynamicDeliveryServiceInfo(
                168,
                "only 213 region courier",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=1)],
            ),
            DynamicDeliveryServiceInfo(
                169,
                "only 214 region courier",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=214, days_key=1)],
            ),
            DynamicDaysSet(key=1, days=[]),
            link_warehouse_delivery_service(warehouse_id=145, delivery_service_id=103),
            link_warehouse_delivery_service(warehouse_id=145, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=172, delivery_service_id=103),
            link_warehouse_delivery_service(warehouse_id=172, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=147, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=EKB_WAREHOUSE_ID, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=SAMARA_WAREHOUSE_ID, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=SPB_WAREHOUSE_ID, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=NOVOSIBIRSK_WAREHOUSE_ID, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=148, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=149, delivery_service_id=111),
            link_warehouse_delivery_service(warehouse_id=444, delivery_service_id=99),
            link_warehouse_delivery_service(warehouse_id=444, delivery_service_id=165, region_to=444),
            link_warehouse_delivery_service(warehouse_id=555, delivery_service_id=165),
            link_warehouse_delivery_service(warehouse_id=666, delivery_service_id=165, region_to=2),
            link_warehouse_delivery_service(warehouse_id=888, delivery_service_id=165),
            link_warehouse_delivery_service(warehouse_id=999, delivery_service_id=165),
            link_warehouse_delivery_service(warehouse_id=_Shops.EXPRESS_DELIVERY_WAREHOUSE_ID, delivery_service_id=167),
            link_warehouse_delivery_service(
                warehouse_id=_Shops.LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID, delivery_service_id=168
            ),
            link_warehouse_delivery_service(
                warehouse_id=_Shops.HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID, delivery_service_id=169
            ),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseToWarehouseInfo(warehouse_from=id, warehouse_to=id)
            for id in [
                172,
                145,
                147,
                EKB_WAREHOUSE_ID,
                SAMARA_WAREHOUSE_ID,
                SPB_WAREHOUSE_ID,
                NOVOSIBIRSK_WAREHOUSE_ID,
                148,
                149,
                444,
                555,
                666,
                1999,
                2999,
                _Shops.EXPRESS_DELIVERY_WAREHOUSE_ID,
                _Shops.LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID,
                _Shops.HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID,
            ]
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225],
                warehouse_with_priority=[
                    WarehouseWithPriority(444, 1),
                    WarehouseWithPriority(555, 2),
                    WarehouseWithPriority(666, 2),
                    WarehouseWithPriority(172, 3),
                    WarehouseWithPriority(145, 3),
                    WarehouseWithPriority(147, 4),
                    WarehouseWithPriority(EKB_WAREHOUSE_ID, 4),
                    WarehouseWithPriority(SAMARA_WAREHOUSE_ID, 4),
                    WarehouseWithPriority(SPB_WAREHOUSE_ID, 4),
                    WarehouseWithPriority(NOVOSIBIRSK_WAREHOUSE_ID, 4),
                    WarehouseWithPriority(149, 5),
                    WarehouseWithPriority(888, 3),
                    WarehouseWithPriority(999, 4),
                    WarehouseWithPriority(1999, 4),
                    WarehouseWithPriority(2999, 4),
                    WarehouseWithPriority(_Shops.EXPRESS_DELIVERY_WAREHOUSE_ID, 8),
                    WarehouseWithPriority(_Shops.HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID, 7),
                    WarehouseWithPriority(_Shops.LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID, 9),
                ],
            ),
            WarehousesPriorityInRegion(regions=[444], warehouse_with_priority=[WarehouseWithPriority(444, 1)]),
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(555, 1),
                    WarehouseWithPriority(666, 1),
                ],
            ),
        ]

        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.red_virtual_shop,
            _Shops.blue_supplier_172,
            _Shops.blue_supplier_145,
            _Shops.blue_supplier_147,
            _Shops.blue_supplier_148,
            _Shops.blue_supplier_149,
            _Shops.blue_supplier2_147,
            _Shops.blue_supplier3_172,
            _Shops.blue_supplier4_172,
            _Shops.yaphone_supplier_1,
            _Shops.yaphone_supplier_2,
            _Shops.yaphone_supplier_3,
            _Shops.yaphone_supplier_4,
            _Shops.yaphone_supplier_5,
            _Shops.yaphone_supplier_6,
            _Shops.dropship_444,
            _Shops.dropship_555,
            _Shops.dropship_666,
            _Shops.child_world_888,
            _Shops.child_world_999,
            Shop(fesh=1001, priority_region=213, cpa=Shop.CPA_REAL, client_id=1001),
            Shop(fesh=1002, priority_region=213, cpa=Shop.CPA_REAL, client_id=1002),
            Shop(fesh=1010, priority_region=2, regions=[213], client_id=11, cpa=Shop.CPA_REAL),
            Shop(
                fesh=_Shops.FIRST_DIGITAL_SUPPLIER_ID,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='Digital goods supplier 1',
            ),
            Shop(
                fesh=_Shops.SECOND_DIGITAL_SUPPLIER_ID,
                priority_region=213,
                cpa=Shop.CPA_REAL,
                name='Digital goods supplier 2',
            ),
            _Shops.fashion_1p_suplier,
            _Shops.express_dropship_shop,
            _Shops.dropship_low_priority,
            _Shops.dropship_high_priority,
        ]

        cls.index.mskus += [
            _MSKUs.alyonka,
            _MSKUs.coke,
            _MSKUs.avocado,
            _MSKUs.fridge,
            _MSKUs.hoover,
            _MSKUs.microwave,
            _MSKUs.unicorn,
            _MSKUs.phone,
            _MSKUs.router,
            _MSKUs.music,
            _MSKUs.iphone1,
            _MSKUs.gift_case1,
            _MSKUs.fanta,
            _MSKUs.sprite,
            _MSKUs.shoes,
            _MSKUs.flowers,
            _MSKUs.retail_tushenka,
        ]

        cls.index.offers += [
            _WhiteOffers.no_msku_1001,
            _WhiteOffers.alyonka_1001,
            _WhiteOffers.no_msku_1002,
            _WhiteOffers.no_msku_1003,
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7002,
                fesh=_Shops.FIRST_DIGITAL_SUPPLIER_ID,
                price=15,
                download=True,
                waremd5='DiGitalOfferInShop1_1g',
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7003,
                fesh=_Shops.FIRST_DIGITAL_SUPPLIER_ID,
                price=41,
                download=True,
                waremd5='DiGitalOfferInShop1_2g',
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7004,
                fesh=_Shops.SECOND_DIGITAL_SUPPLIER_ID,
                price=25,
                download=True,
                waremd5='DiGitalOfferInShop2_1g',
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7005,
                fesh=_Shops.SECOND_DIGITAL_SUPPLIER_ID,
                price=35,
                download=False,
                waremd5='NeDiGitalOfferInShop2g',
            ),
        ]

        std_options = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=1, day_to=2)])]
        std_options_2 = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=2, day_to=3)])]
        std_options_3 = [RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=3, day_to=4)])]
        std_options_spb = [RegionalDelivery(rid=2, options=[DeliveryOption(price=5, day_from=3, day_to=4)])]
        cls.index.delivery_buckets += [
            DeliveryBucket(bucket_id=801, dc_bucket_id=801, fesh=1, carriers=[103], regional_options=std_options),
            DeliveryBucket(
                bucket_id=802,
                dc_bucket_id=802,
                fesh=1,
                carriers=[111],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options_2,
            ),
            DeliveryBucket(
                bucket_id=803,
                dc_bucket_id=803,
                fesh=4,
                carriers=[99],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options,
            ),
            DeliveryBucket(
                bucket_id=804,
                dc_bucket_id=804,
                fesh=4,
                carriers=[165],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options_3,
            ),
            DeliveryBucket(
                bucket_id=805,
                dc_bucket_id=805,
                fesh=4,
                carriers=[165],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=444, options=[DeliveryOption(price=50, day_from=3, day_to=4)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=50, day_from=3, day_to=4)]),
                ],
            ),
            DeliveryBucket(
                bucket_id=899,
                dc_bucket_id=899,
                fesh=1,
                carriers=[111],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=std_options_spb,
            ),
            DeliveryBucket(
                bucket_id=_Shops.EXPRESS_DELIVERY_BUCKET_ID,
                dc_bucket_id=_Shops.EXPRESS_DELIVERY_BUCKET_ID,
                fesh=_Shops.EXPRESS_DELIVERY_SUPPLIER_ID,
                carriers=[167],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(
                                price=50,
                                day_from=0,
                                day_to=0,
                            )
                        ],
                    ),
                    RegionalDelivery(
                        rid=214,
                        options=[
                            DeliveryOption(
                                price=50,
                                day_from=0,
                                day_to=0,
                            )
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=_Shops.LOW_PRIORITY_DROPSHIP_BUCKET_ID,
                dc_bucket_id=_Shops.LOW_PRIORITY_DROPSHIP_BUCKET_ID,
                fesh=_Shops.LOW_PRIORITY_DROPSHIP_SUPPLIER_ID,
                carriers=[168],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(
                                price=50,
                                day_from=1,
                                day_to=2,
                            )
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=_Shops.HIGH_PRIORITY_DROPSHIP_BUCKET_ID,
                dc_bucket_id=_Shops.HIGH_PRIORITY_DROPSHIP_BUCKET_ID,
                fesh=_Shops.HIGH_PRIORITY_DROPSHIP_SUPPLIER_ID,
                carriers=[169],
                regional_options=[
                    RegionalDelivery(
                        rid=214,
                        options=[
                            DeliveryOption(
                                price=50,
                                day_from=3,
                                day_to=4,
                            )
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=148).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=149).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=444).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=10, length=10, width=10, warehouse_id=666).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=444).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=3, height=22, length=22, width=11, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=3, height=22, length=22, width=11, warehouse_id=444).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, height=22, length=22, width=11, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, height=22, length=22, width=11, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, height=22, length=22, width=11, warehouse_id=148).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, height=22, length=22, width=11, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, height=22, length=22, width=11, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=22, length=22, width=22, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=22, length=22, width=22, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=22, length=22, width=22, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, height=22, length=22, width=22, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=22, length=22, width=22, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=22, length=22, width=22, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=22, length=22, width=22, warehouse_id=444).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=22, length=22, width=22, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=22, length=22, width=22, warehouse_id=666).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=10, height=32, length=22, width=22, warehouse_id=444).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=12, height=32, length=22, width=22, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=12, height=32, length=22, width=22, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=12, height=32, length=22, width=22, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=13, height=32, length=32, width=22, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=13, height=32, length=32, width=22, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=15, height=32, length=32, width=22, warehouse_id=444).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=16, height=32, length=32, width=22, warehouse_id=555).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=16, height=32, length=32, width=22, warehouse_id=666).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=17, height=32, length=32, width=22, warehouse_id=149).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=2, height=22, length=11, width=11, warehouse_id=148).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=17, height=32, length=32, width=22, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=3, height=22, length=22, width=11, warehouse_id=172).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=3, height=22, length=22, width=11, warehouse_id=145).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, height=22, length=22, width=22, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=3, height=22, length=22, width=11, warehouse_id=147).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, height=22, length=22, width=22, warehouse_id=148).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, height=22, length=22, width=22, warehouse_id=148).respond(
            [801, 802, 803, 804, 805], [5001], []
        )
        cls.delivery_calc.on_request_offer_buckets(
            weight=4, height=22, length=22, width=11, warehouse_id=EKB_WAREHOUSE_ID
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=4, height=22, length=22, width=11, warehouse_id=SAMARA_WAREHOUSE_ID
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=4, height=22, length=22, width=11, warehouse_id=SPB_WAREHOUSE_ID
        ).respond([801, 802, 803, 804, 805, 899], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=4, height=22, length=22, width=11, warehouse_id=NOVOSIBIRSK_WAREHOUSE_ID
        ).respond([801, 802, 803, 804, 805], [5001], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Shops.EXPRESS_DELIVERY_WAREHOUSE_ID
        ).respond([_Shops.EXPRESS_DELIVERY_BUCKET_ID], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Shops.LOW_PRIORITY_DROPSHIP_WAREHOUSE_ID
        ).respond([_Shops.LOW_PRIORITY_DROPSHIP_BUCKET_ID], [], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=30, length=30, width=30, warehouse_id=_Shops.HIGH_PRIORITY_DROPSHIP_WAREHOUSE_ID
        ).respond([_Shops.HIGH_PRIORITY_DROPSHIP_BUCKET_ID], [], [])

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=feed_id, warehouse_id=warehouse_id)
            for feed_id, warehouse_id in zip(
                [21, 22, 23, 24, 25, 3, 66, 101, 31, 32, 4, 5, 6, 7, 88, 99, 33, 34, 35, 36],
                [
                    172,
                    147,
                    148,
                    149,
                    145,
                    147,
                    172,
                    172,
                    172,
                    147,
                    444,
                    555,
                    666,
                    888,
                    999,
                    EKB_WAREHOUSE_ID,
                    SAMARA_WAREHOUSE_ID,
                    SPB_WAREHOUSE_ID,
                    NOVOSIBIRSK_WAREHOUSE_ID,
                ],
            )
        ]
        cls.index.promos += [_Promos.promo_iphone1]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[_Promos.promo_iphone1.key])]

    def test_invalid_parameters(self):
        """
        Проверка правильности обработки некорректных запросов
        """

        def expect_response_error(request, message, code="INVALID_USER_CGI"):
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {"error": {"code": code, "message": message}})
            self.error_log.expect(code=3043)

        prefix = PLACE_COMBINE_WITH_RGB
        for incomplete_params in ('', '&rids=213', '&offers-list=99:1'):
            expect_response_error(prefix + incomplete_params, "rids and offers-list are required parameters")

    def request_combine(
        self, mskus, offers, region=213, count=1, flags='', no_replace_offers=[], marshrut_warehouse_priority=True
    ):
        request = PLACE_COMBINE_WITH_RGB + '&debug=1&rids={}'.format(region)
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        assert len(mskus) == len(offers), 'len(mskus) == {} is not equal to len(offers) == {}'.format(
            len(mskus), len(offers)
        )

        request_offers = []
        for cart_item_id, (msku, offer) in enumerate(zip(mskus, offers)):
            no_replace = offer in set(no_replace_offers)
            request_offers += [
                '{}:{};msku:{};cart_item_id:{}{}'.format(
                    offer.waremd5,
                    count,
                    msku.sku,
                    cart_item_id + 1,
                    ';no_replace:1' if no_replace else '',
                )
            ]
        if request_offers:
            request += '&offers-list=' + ','.join(request_offers)

        request += '&rearr-factors=market_marshrut_warehouse_priority={market_marshrut_warehouse_priority}'.format(
            market_marshrut_warehouse_priority=1 if marshrut_warehouse_priority else 0
        )
        request += flags
        return self.report.request_json(request)

    def test_dublicate_offers_in_request(self):
        """
        Проверка нового критерия дубликатов мультиоферов:
        оферы одного msku разных поставщиков (supplier), но имеющие одинаковые клиенты поставщиков (supplier client)
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.alyonka), (_Offers.alyonka_s3_172, _Offers.alyonka_s4_172)
        )
        self.assertFragmentIn(
            response,
            {"logicTrace": [Contains("The request contains different offers for MSKU = '1', supplier client = '10'")]},
        )

    def test_unavailable_strategy(self):
        response = self.request_combine((_MSKUs.alyonka,), (_Offers.alyonka_s1_149,), flags="&split-strategy=invalid")
        self.assertFragmentIn(response, {"search": {"errors": ["There are no available strategies"]}})

    def check_offer_in_strategy(
        self, response, strategy_name, shop_id, offer, count=1, fail=False, original_offer=None, cart_item_id=None
    ):
        if fail:
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "name": strategy_name,
                            "buckets": [
                                {"shopId": 0, "offers": [{"wareId": "", "replacedId": offer.waremd5, "count": count}]}
                            ],
                        }
                    ]
                },
            )
        else:
            expected_json_offer = {
                "wareId": offer.waremd5,
                "count": count,
            }

            if original_offer:
                expected_json_offer["replacedId"] = original_offer.waremd5

            if cart_item_id:
                expected_json_offer["cartItemIds"] = cart_item_id if isinstance(cart_item_id, list) else [cart_item_id]
            else:
                expected_json_offer["cartItemIds"] = NoKey('cartItemIds')

            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"name": strategy_name, "buckets": [{"shopId": shop_id, "offers": [expected_json_offer]}]}
                    ]
                },
            )

    def check_parcel_in_strategy(
        self, response, strategy_name, shop_id, has_courier, has_pickup, partner_type, is_automatic
    ):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": strategy_name,
                        "automaticReplacement": is_automatic,
                        "buckets": [
                            {
                                "shopId": shop_id,
                                "hasCourier": has_courier,
                                "hasPickup": has_pickup,
                                "deliveryPartnerTypes": [partner_type],
                            }
                        ],
                    }
                ]
            },
        )

    def check_parcel_restrictions_lists(
        self, response, shop_id, success, overcome_restrictions=[], current_restrictions=[]
    ):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "hasOvercomeRestrictions": success,
                        "buckets": [
                            {
                                "shopId": shop_id,
                                "overcomeRestrictionsForReplacement": overcome_restrictions,
                                "currentRestrictionsForReplacement": current_restrictions,
                            }
                        ],
                    }
                ]
            },
        )

    def check_total_buckets_in_strategy(self, response, strategy_name, expected):
        if expected > 0:
            self.assertFragmentIn(
                response,
                {"results": [{"entity": "split-strategy", "name": strategy_name, "buckets": ElementCount(expected)}]},
            )
        else:
            self.assertFragmentNotIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "name": strategy_name,
                        }
                    ]
                },
            )

    def check_default_strategy(self, response, strategy_name, default):
        self.assertFragmentIn(
            response, {"results": [{"entity": "split-strategy", "name": strategy_name, "default": default}]}
        )

    CONSOLIDATE_WITHOUT_CROSSDOCK = "consolidate-without-crossdock"
    CONSOLIDATE_WITH_SUPPLIER_REPLACE = "consolidate-with-supplier-replace"
    FAIL_STRATEGY = "failed-strategy"

    MARKET_PARTNER_TYPE = "YANDEX_MARKET"
    SHOP_PARTNER_TYPE = "SHOP"

    PROMO_RESTRICTION = "EXCLUDE_PROMO_IN_CONSOLIDATION"
    ROSTOV_RESTRICTION = "EXCLUDE_ROSTOV_IN_CONSOLIDATION"
    EKB_RESTRICTION = "EXCLUDE_EKB_IN_CONSOLIDATION"
    SPB_RESTRICTION = "EXCLUDE_SPB_IN_CONSOLIDATION"
    SAMARA_RESTRICTION = "EXCLUDE_SAMARA_IN_CONSOLIDATION"
    FIXED_OFFER_RESTRICTIONS = "EXCLUDE_FIXED_OFFER_IN_CONSOLIDATION"
    NOVOSIBIRSK_RESTRICTION = "EXCLUDE_NOVOSIBIRSK_IN_CONSOLIDATION"

    def test_empty_priority_warehouses_list(self):
        response = self.request_combine((_MSKUs.coke,), (_Offers.coke_s1_172,), region=111)
        self.check_offer_in_strategy(response, self.FAIL_STRATEGY, 1, _Offers.coke_s1_172, fail=True, cart_item_id=1)

    def test_single_offer(self):
        """
        Проверка "комбайна" для заказов из одного товара: выбираем наиболее подходящий оффер
            проходом по разным складам в порядке уменьшения их приоритета.
        Условие выбора: по сравнению с оффером в запросе не должен измениться поставщик и вырасти цена.
        """
        for offer in (_Offers.alyonka_s1_172, _Offers.alyonka_s2_147):
            response = self.request_combine([_MSKUs.alyonka], (offer,))
            # в запросе приходит наиболее приоритетный склад
            self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, offer, cart_item_id=1)

        for offer in (_Offers.alyonka_s1_147, _Offers.alyonka_s1_148):
            response = self.request_combine([_MSKUs.alyonka], (offer,))
            # Для оффера со 147 склада не применяется замена на 172, т.к. на 172-м складе цена выше
            # Оффер со 148 склада взять не можем, он не разрешён в LMS, заменяем его оффером на 147-м складе
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_147, cart_item_id=1
            )

        for offer in (_Offers.fridge_555_offer, _Offers.fridge_444_offer):
            response = self.request_combine([_MSKUs.fridge], (offer,))
            # У дропшипа склад 444 приоритетнее
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_444_offer, cart_item_id=1
            )

        # Два оффера одного поставщика на один MSKU на одном складе, проверяем, что подмены нет, если передать один из них
        for offer in (_Offers.hoover_s1_147, _Offers.hoover_s1_147_blue):
            response = self.request_combine([_MSKUs.hoover], (offer,))
            self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, offer, cart_item_id=1)

        # Должен выбраться оффер со склада, где есть требуемое количество при одинаковой цене
        response = self.request_combine([_MSKUs.alyonka], (_Offers.alyonka_promo_s1_149,), count=13)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 13, cart_item_id=1
        )

        # Должен выбраться оффер со склада, где есть требуемое количество для того же client_id
        response = self.request_combine([_MSKUs.phone], (_Offers.phone_1_172,), count=7)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.phone_1_147, 7, cart_item_id=1
        )

        # Не подменяем 3P-оффер со склада Беру дропшип-оффером.
        response = self.request_combine((_MSKUs.alyonka,), (_Offers.alyonka_s2_147,))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s2_147, cart_item_id=1
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_combine_success_marshrut(self):
        """
        Проверка успешного прохождения подмен'
        """
        replacements_flag = (
            '&split-strategy=consolidate-without-crossdock&rearr-factors=exclude_banned_warehouses_in_consolidation=0'
        )

        # Проверяем "костыль" со 145 складом (проверяем, что подменяем на 145)
        response = self.request_combine(
            (_MSKUs.fanta, _MSKUs.hoover), (_Offers.fanta_s1_172, _Offers.hoover_s1_147), flags=replacements_flag
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.fanta_s1_145, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # Проверяем "костыль" со 145 складом (проверяем, что 145 не меняем)
        response = self.request_combine(
            (_MSKUs.fanta, _MSKUs.hoover), (_Offers.fanta_s1_145, _Offers.hoover_s1_147), flags=replacements_flag
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.fanta_s1_145, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # Проверяем "костыль" со 145 складом (проверяем, что 145 не выбираем, если выше цена)
        response = self.request_combine(
            (_MSKUs.sprite, _MSKUs.hoover), (_Offers.sprite_s1_172, _Offers.hoover_s1_147), flags=replacements_flag
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.sprite_s1_147, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # Проверяем, что если эксперент с "костылем" выключен, то все работает как раньше
        response = self.request_combine(
            (_MSKUs.fanta, _MSKUs.hoover),
            (_Offers.fanta_s1_172, _Offers.hoover_s1_147),
            flags=replacements_flag,
            marshrut_warehouse_priority=False,
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.fanta_s1_147, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_combine_success(self):
        """
        Проверка успешного прохождения подмен'
        """
        replacements_flag = (
            '&split-strategy=consolidate-without-crossdock&rearr-factors=exclude_banned_warehouses_in_consolidation=0'
        )
        # Пылесос есть только на 147 складе, поэтому шоколадку тоже берём оттуда (по старой логике)

        # По новой логике шоколадка будет браться с более приоритетного склада, так как нам выгоднее брыть из нее
        for chocolate in (_Offers.alyonka_s1_172, _Offers.alyonka_s1_148):
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.hoover), (chocolate, _Offers.hoover_s1_147), flags=replacements_flag
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_147, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147, cart_item_id=2
            )
            self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # Подмена на 172 склад товаров с менее приоритетных складов
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s1_149, _Offers.coke_s1_148), flags=replacements_flag
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # Дропшип (по приоритету лучше 444)
        for fridge in (_Offers.fridge_444_offer, _Offers.fridge_555_offer):
            response = self.request_combine(
                (_MSKUs.hoover, _MSKUs.fridge), (_Offers.hoover_555_offer, fridge), flags=replacements_flag
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_444_offer, cart_item_id=2
            )
            self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # Дропшип - 3 товара, подменяются на 555, т.к. микроволновка есть только там
        response = self.request_combine(
            (_MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave),
            (_Offers.hoover_555_offer, _Offers.fridge_444_offer, _Offers.microwave_555_offer),
            flags=replacements_flag,
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_555_offer, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_555_offer, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # Пылесос есть только на 147 складе, поэтому Яндекс.Телефон тоже берём оттуда
        response = self.request_combine(
            (_MSKUs.phone, _MSKUs.hoover), (_Offers.phone_1_172, _Offers.hoover_s1_147), flags=replacements_flag
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.phone_1_147, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_no_replacement(self):
        """
        Проверка случаев, когда подмены происходить не должны
        """
        # Поставщика менять нельзя, поэтому всё остаётся на месте, хотя на 172 складе предложение лучше
        response = self.request_combine((_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s2_147, _Offers.coke_s2_147))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s2_147, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s2_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # Дропшип - пылесос на складе 444 дешевле, чем на 555, поэтому не заменяем
        response = self.request_combine(
            (_MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave),
            (_Offers.hoover_444_offer, _Offers.fridge_444_offer, _Offers.microwave_555_offer),
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_444_offer, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # Нет требуемого количества, поэтому не заменяем
        response = self.request_combine((_MSKUs.alyonka,), (_Offers.alyonka_s1_149,), count=17)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_149, 17, cart_item_id=1
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_no_delivery_to_region(self):
        """
        Проверка работы при отсутствии доставки с определённого склада в регион.
        В 444 регион допускается доставка только с 444 склада, поэтому ожидаем, что с других складов офферы не вернутся
        """
        response = self.request_combine((_MSKUs.alyonka,), (_Offers.alyonka_s1_172,))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        response = self.request_combine((_MSKUs.alyonka,), (_Offers.alyonka_s1_172,), region=444)
        # В ответе тот же список офферов, с которым пришли в репорт
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, fail=True, cart_item_id=1
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_multi_offer_no_combine(self):
        """
        Проверка работы мультиофера — два офера одного msku от разных поставщиков (supplier client у них тоже разные)
        У поставщика s3 нет офера на 147м складе -> комбайинга нет
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.alyonka), (_Offers.alyonka_s3_172, _Offers.alyonka_s2_147)
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s3_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s2_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    def test_multi_offer_combine_successful(self):
        """
        Проверка работы мультиофера — два офера одного msku от разных поставщиков (supplier client у них тоже разные)
        У поставщика s1 есть офер на 147м складе -> комбайинг успешен
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.alyonka),
            (_Offers.alyonka_s1_172, _Offers.alyonka_s2_147),
            flags='&rearr-factors=exclude_banned_warehouses_in_consolidation=0',
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_147, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s2_147, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_invalid_offer_in_request(self):
        """
        Если в запросе приходит оффер, который мы не можем использовать (например, его вообще нет в индексе),
        мы пытаемся вместо него использовать байбокс этого MSKU.
        """

        def check_price_has_changed(response, price_has_changed):
            self.assertFragmentIn(response, {"results": [{"priceHasChanged": price_has_changed}]})

        # invalid offerid -> replace it to a valid one
        # MARKETOUT-43721 Enable countries in rearr-flag
        response = self.request_combine((_MSKUs.fridge,), (_Offers.hoover_444_offer,))
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_444_offer,
            original_offer=_Offers.hoover_444_offer,
            cart_item_id=1,
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # invalid multiple offerids -> replace them to a single valid one (buybox)
        response = self.request_combine(
            (_MSKUs.fridge, _MSKUs.fridge), (_Offers.hoover_444_offer, _Offers.hoover_555_offer)
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_444_offer,
            2,
            original_offer=_Offers.hoover_555_offer,
            cart_item_id=[1, 2],
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        check_price_has_changed(response, price_has_changed=True)

        # hoover_s1_147 can't be delivered to region 444, so we choose another (valid) offer as a replacement
        response = self.request_combine(
            (_MSKUs.hoover,), (_Offers.hoover_s1_147,), region=444, flags='&rearr-factors=cpa_enabled_countries=111,444'
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.hoover_444_offer,
            original_offer=_Offers.hoover_s1_147,
            cart_item_id=1,
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        check_price_has_changed(response, price_has_changed=True)

        # replacement
        response = self.request_combine((_MSKUs.hoover, _MSKUs.fridge), (_Offers.alyonka_s1_148, _Offers.coke_s1_172))
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.hoover_444_offer,
            original_offer=_Offers.alyonka_s1_148,
            cart_item_id=1,
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_444_offer,
            original_offer=_Offers.coke_s1_172,
            cart_item_id=2,
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        check_price_has_changed(response, price_has_changed=True)

        # no replacement available – render received offers
        response = self.request_combine((_MSKUs.unicorn,), (_Offers.hoover_444_offer,))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_444_offer, fail=True, cart_item_id=[1, 2]
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_invalid_offer_and_combine(self):
        """
        В запросе приходит протухший мультиоффер — мы пытаемся вместо него использовать байбокс этого MSKU
        Количество суммируется по всем позициям мультиоффера — для дальнейшего использования в логике combine
        Здесь везде — протухший офер = несоответствующий своему MSKU (MSKUs.fridge и Offers.coke_*)
        """
        # replaced invalid multioffer and another offer from different WH -> no combining
        response = self.request_combine(
            (_MSKUs.fridge, _MSKUs.fridge, _MSKUs.alyonka),
            (_Offers.coke_s1_172, _Offers.coke_s2_147, _Offers.alyonka_s1_172),
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_444_offer,
            2,
            original_offer=_Offers.coke_s2_147,
            cart_item_id=[1, 2],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # replaced invalid multioffer and another offer from the same WH -> successful consolidation
        # another offer (hoover_555_offer) WH changed, because 444 has higher priority
        response = self.request_combine(
            (_MSKUs.fridge, _MSKUs.fridge, _MSKUs.hoover),
            (_Offers.coke_s1_172, _Offers.coke_s2_147, _Offers.hoover_555_offer),
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_444_offer,
            2,
            original_offer=_Offers.coke_s2_147,
            cart_item_id=[1, 2],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # replaced invalid multioffer from other WH with enough stock -> successful consolidation
        # replaced invalid multioffer (fridge_444_offer) WH changed because not enough stock
        # fridge_444_offer stock = 7 - not enough, need 4 + 4 = 8
        # fridge_555_offer stock = 8 - enough
        response = self.request_combine(
            (_MSKUs.fridge, _MSKUs.fridge, _MSKUs.hoover),
            (_Offers.coke_s1_172, _Offers.coke_s2_147, _Offers.hoover_555_offer),
            count=4,
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_555_offer,
            8,
            original_offer=_Offers.coke_s2_147,
            cart_item_id=[1, 2],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_555_offer, 4, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # replaced invalid multioffer not enough stock on any WH -> no consolidation
        # replaced invalid multioffer (fridge_444_offer) WH not changed
        # fridge_444_offer stock = 7 - not enough, need 5 + 5 = 10
        # fridge_555_offer stock = 8 - not enough too
        response = self.request_combine(
            (_MSKUs.fridge, _MSKUs.fridge, _MSKUs.hoover),
            (_Offers.coke_s1_172, _Offers.coke_s2_147, _Offers.hoover_555_offer),
            count=5,
        )
        self.check_offer_in_strategy(
            response,
            self.CONSOLIDATE_WITHOUT_CROSSDOCK,
            4,
            _Offers.fridge_444_offer,
            10,
            original_offer=_Offers.coke_s2_147,
            cart_item_id=[1, 2],
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, 5, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_offers_stay_in_place_if_same_priority_warehouse(self):
        """
        Проверка того, что не перекладываем офферы на другой склад, если у альтернативного склада тот же приоритет.
        """
        response = self.request_combine((_MSKUs.hoover,), (_Offers.hoover_555_offer,), region=2, count=8)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_555_offer, 8, cart_item_id=1
        )

        response = self.request_combine((_MSKUs.hoover,), (_Offers.hoover_666_offer,), region=2, count=8)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_666_offer, 8, cart_item_id=1
        )

        response = self.request_combine(
            (_MSKUs.hoover, _MSKUs.music), (_Offers.hoover_555_offer, _Offers.music_555_offer), region=2, count=8
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_555_offer, 8, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.music_555_offer, 8, cart_item_id=2
        )

        response = self.request_combine(
            (_MSKUs.hoover, _MSKUs.music), (_Offers.hoover_666_offer, _Offers.music_666_offer), region=2, count=8
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_666_offer, 8, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.music_666_offer, 8, cart_item_id=2
        )

    def test_exclude_rostov_warehouses_in_replacement(self):
        """
        Проверяем, что в Ростовском складе не работают подмены под флагом exclude_banned_warehouses_in_consolidation
        (по-умолчанию вкл)
        """
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0', False),
        )
        for consolidation_flag, disable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.phone),
                (_Offers.alyonka_s1_172, _Offers.coke_s1_148, _Offers.phone_1_147),
                region=39,
                count=4,
                flags=consolidation_flag,
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
            )
            self.check_offer_in_strategy(
                response,
                self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                1,
                _Offers.phone_1_147 if disable_consolidation else _Offers.phone_1_172,
                4,
                cart_item_id=3,
            )
            if disable_consolidation:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "currentRestrictionsForReplacement": [self.ROSTOV_RESTRICTION],
                                    }
                                ]
                            }
                        ]
                    },
                )

    def test_exclude_ekb_warehouses_in_replacement(self):
        """
        Проверяем, что нв складе Екб не работают подмены под флагом exclude_banned_warehouses_in_consolidation
        (по-умолчанию вкл)
        """
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0', False),
        )
        for consolidation_flag, disable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.phone),
                (_Offers.alyonka_s1_172, _Offers.phone_1_300),
                region=222,
                count=4,
                flags=consolidation_flag,
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response,
                self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                1,
                _Offers.phone_1_300 if disable_consolidation else _Offers.phone_1_172,
                4,
                cart_item_id=2,
            )
            if disable_consolidation:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "currentRestrictionsForReplacement": [self.EKB_RESTRICTION],
                                    }
                                ]
                            }
                        ]
                    },
                )

    def test_exclude_samara_warehouses_in_replacement(self):
        """
        Проверяем, что в Самарском складе не работают подмены под флагом exclude_banned_warehouses_in_consolidation
        (по-умолчанию вкл)
        """
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('&rearr-factors=market_hidden_warehouses=301', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1;;market_hidden_warehouses=301', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0;;market_hidden_warehouses=301', False),
        )
        for consolidation_flag, disable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.phone),
                (_Offers.alyonka_s1_172, _Offers.phone_1_302),
                region=333,
                count=4,
                flags=consolidation_flag,
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response,
                self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                1,
                _Offers.phone_1_302 if disable_consolidation else _Offers.phone_1_172,
                4,
                cart_item_id=2,
            )
            if disable_consolidation:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "currentRestrictionsForReplacement": [self.SAMARA_RESTRICTION],
                                    }
                                ]
                            }
                        ]
                    },
                )

    def test_exclude_spb_warehouses_in_replacement(self):
        """
        Проверяем, что на складе СПБ не работают подмены под флагом exclude_banned_warehouses_in_consolidation
        (по-умолчанию вкл)
        """
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('&rearr-factors=market_hidden_warehouses=302', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1;;market_hidden_warehouses=302', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0;;market_hidden_warehouses=302', False),
        )
        for consolidation_flag, disable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.phone,), (_Offers.phone_1_301,), region=555, count=4, flags=consolidation_flag
            )
            self.check_offer_in_strategy(
                response,
                self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                1,
                _Offers.phone_1_301 if disable_consolidation else _Offers.phone_1_147,
                4,
                cart_item_id=1,
            )
            if disable_consolidation:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "currentRestrictionsForReplacement": [self.SPB_RESTRICTION],
                                    }
                                ]
                            }
                        ]
                    },
                )

    def test_exclude_novosibirsk_warehouses_in_replacement(self):
        """
        Проверяем, что в Новосибирском складе не работают подмены под флагом exclude_banned_warehouses_in_consolidation
        (по-умолчанию вкл)
        """
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1', True),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0', False),
        )
        for consolidation_flag, disable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.phone),
                (_Offers.alyonka_s1_172, _Offers.phone_1_303),
                region=666,
                count=4,
                flags=consolidation_flag,
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response,
                self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                1,
                _Offers.phone_1_303 if disable_consolidation else _Offers.phone_1_172,
                4,
                cart_item_id=2,
            )
            if disable_consolidation:
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "buckets": [
                                    {
                                        "currentRestrictionsForReplacement": [self.NOVOSIBIRSK_RESTRICTION],
                                    }
                                ]
                            }
                        ]
                    },
                )

    def test_ignore_exclude_flag_in_non_rostov_region(self):
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('', False),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1', False),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0', False),
        )
        for consolidation_flag, disable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.phone),
                (_Offers.alyonka_s1_172, _Offers.coke_s1_148, _Offers.phone_1_147),
                region=213,
                count=4,
                flags=consolidation_flag,
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
            )
            self.check_offer_in_strategy(
                response,
                self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                1,
                _Offers.phone_1_147 if disable_consolidation else _Offers.phone_1_172,
                4,
                cart_item_id=3,
            )

    def test_partial_replacement(self):
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave),
            (
                _Offers.alyonka_s1_172,
                _Offers.coke_s1_148,
                _Offers.hoover_444_offer,
                _Offers.fridge_555_offer,
                _Offers.microwave_555_offer,
            ),
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=3
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_444_offer, cart_item_id=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=5
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 3)

    def test_exclude_promo_offers(self):
        """
        Проверяем, что если офферу задать promo_type, promo_id, то они не будут участвовать в подменах
        """
        EXCLUDE_PROMO_FLAG = '&rearr-factors=exclude_promo_in_consolidation=1'
        request_without_promo = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=213&offers-list=Alyonka_s1_172_______g:4;msku:1;cart_item_id:1,CocaCola_s1_148______g:4;msku:2;cart_item_id:2;'
        )
        # Проверяем без promo
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s1_172, _Offers.coke_s1_148), region=213, count=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # Проверяем с promo
        response = self.report.request_json(request_without_promo + 'promo_type:price;promo_id:abrakadabra')
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_148, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # Проверяем, что для промо из списка, в том числе для кешбека, замена происходит без флагов,
        # но под флагом exclude_promo_in_consolidation=1 замены нет
        for promo in (PromoType.BLUE_3P_FLASH_DISCOUNT, PromoType.CART_DISCOUNT, PromoType.BLUE_CASHBACK):
            response = self.report.request_json(
                request_without_promo + 'promo_type:{};promo_id:abrakadabra'.format(promo)
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
            )
            self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

            response = self.report.request_json(
                request_without_promo + 'promo_type:{};promo_id:abrakadabra'.format(promo) + EXCLUDE_PROMO_FLAG
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_148, 4, cart_item_id=2
            )
            self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # Если один из promo параметров не передали считаем, что оффер не участвует в промо-акции
        response = self.report.request_json(request_without_promo + ';promo_type:price')
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        response = self.report.request_json(request_without_promo + ';promo_id:abrakadabra')
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

    def test_allow_replacement_for_promo_offers(self):
        """
        Проверяем, что не возникает ситуации, когда в ответе репорта два одинаковых оффера
        появляются в разных позициях в посылке
        """
        request_without_promo = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=213&offers-list=Alyonka_s1_172_______g:3;msku:1;cart_item_id:1,Alyonka_s1_148_______g:4;msku:1;cart_item_id:2;'
        )
        # Если пришли два оффера одного msku, они схлопнутся в одну позицию с более приоритетного склада (148) и их количество просуммируется
        response = self.report.request_json(request_without_promo)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_148, 7, cart_item_id=[1, 2]
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # Если пришли два оффера, на одном из которых промо из списка ниже, также схлопываем их в позицию с более приоритетного склада (148)
        for promo in (PromoType.BLUE_3P_FLASH_DISCOUNT, PromoType.CART_DISCOUNT, PromoType.BLUE_CASHBACK):
            response = self.report.request_json(
                request_without_promo + 'promo_type:{};promo_id:abrakadabra'.format(promo)
            )
            self.check_offer_in_strategy(
                response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_148, 7, cart_item_id=[1, 2]
            )
            self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # Если пришли  два оффера, на одном из которых промо не из списка, не пытаемся сделать подмену вообще
        response = self.report.request_json(request_without_promo + 'promo_type:price;promo_id:abrakadabra')
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 3, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_148, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    def test_promo_offers(self):
        """
        Проверяем, что, если в корзине два товара из одного промо, то вторичный товар попадает в offers верхнего уровня
        """
        offers = [
            (1, _Offers.iphone_s1_1001.waremd5, _MSKUs.iphone1.sku),
            (2, _Offers.gift_case_1003.waremd5, _MSKUs.gift_case1.sku),
        ]
        params = PLACE_COMBINE_WITH_RGB + '&rids=213&offers-list={}'
        response = self.report.request_json(
            params.format(
                ','.join(
                    '{}:1;msku:{};cart_item_id:{}'.format(offer_id, msku_id, cart_id)
                    for cart_id, offer_id, msku_id in offers
                ),
            )
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.iphone_s1_1001, 1, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.gift_case_1003, 1, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # проверяем что в выдаче есть список офферов
        self.assertFragmentIn(
            response,
            {
                # Проверяем, что промо есть в выдаче
                'search': {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': _Offers.iphone_s1_1001.waremd5,
                                'promos': [
                                    {
                                        'type': _Promos.promo_iphone1.type_name,
                                        'key': _Promos.promo_iphone1.key,
                                        'itemsInfo': {
                                            'additionalOffers': [
                                                {
                                                    'offer': {
                                                        'offerId': _Offers.gift_case_1003.waremd5,
                                                        "entity": "showPlace",
                                                    }
                                                }
                                            ],
                                            'constraints': NotEmpty(),
                                        },
                                    }
                                ],
                            },
                            {
                                'entity': 'offer',
                                'wareId': _Offers.gift_case_1003.waremd5,
                            },
                        ],
                    },
                },
                # Проверяем, что в выдаче есть вторичные офферы
                'offers': [
                    {
                        'entity': 'offer',
                        'wareId': _Offers.gift_case_1003.waremd5,
                    }
                ],
            },
            allow_different_len=False,
        )

        # Проверяем, что только один вторичный офер попал в offers верхнего уровня
        self.assertFragmentIn(response, {'offers': ElementCount(1)})

    def test_exclude_multi_offers_benefit(self):
        """
        Проверяем, что если офферу задать benefit то offer не будут участвовать в подменах
        """
        # Проверяем без benefit
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s1_172, _Offers.coke_s1_148), region=213, count=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # Проверяем с benefit
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + '&rids=213&offers-list=Alyonka_s1_172_______g:4;msku:1;cart_item_id:1,CocaCola_s1_148______g:4;msku:2;cart_item_id:2;benefit:faster'
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_148, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    def test_exclude_fixed_offers(self):
        """
        Проверяем, что если офферу задать параметр fixed то offer не будет участвовать в подменах
        """
        # Проверяем без фиксации
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s1_172, _Offers.coke_s1_148), region=213, count=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # Проверяем с фиксацией офферов
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + '&rids=213&offers-list=Alyonka_s1_172_______g:4;msku:1;cart_item_id:1,CocaCola_s1_148______g:4;msku:2;cart_item_id:2;fixed:1'
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_148, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)
        # Причина запрета подмены не должна отразиться в списке ограничений на подмену в ответе,
        # параметр fixed будет использоваться только при повторном походе в combine,
        # когда распределение по посылкам уже сформировано - причина запрета
        # техническая и не должна отразиться в статистике
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "buckets": [
                            {
                                "currentRestrictionsForReplacement": self.FIXED_OFFER_RESTRICTIONS,
                            }
                        ]
                    }
                ]
            },
        )

    @classmethod
    def prepare_exclude_price_drop(cls):
        cls.index.promos_by_cart += [
            PromoByCart(cart_hid=2, promo_hid=1, percent=20, supplier=3),
        ]

    def test_exclude_price_drop(self):
        """
        Проверяем, что если одна категория дает скидку другой категории, то мы не подменяем эти оффер
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.router, _MSKUs.phone),
            (_Offers.alyonka_s1_172, _Offers.router_s1_147, _Offers.phone_1_147),
            region=213,
            flags='&rearr-factors=market_promo_by_user_cart_hids=1;market_promo_cart_force_pricedrop_return_nothing=0',
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.router_s1_147, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.phone_1_147, cart_item_id=3
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    def test_unused_bucket_position_in_strategy(self):
        """
        Проверяем, что посылка с разобранными офферами идет последней.
        Это означает, что бакет с "warehouseId": 0 должен идти самым последним
        """
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave, _MSKUs.notebook),
            (
                _Offers.alyonka_s1_172,
                _Offers.coke_s1_148,
                _Offers.hoover_444_offer,
                _Offers.fridge_555_offer,
                _Offers.microwave_555_offer,
                _Offers.notebook_out_of_stock,
            ),
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, cart_item_id=2
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.hoover_444_offer, cart_item_id=3
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.fridge_444_offer, cart_item_id=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4, _Offers.microwave_555_offer, cart_item_id=5
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.notebook_out_of_stock, fail=True, cart_item_id=6
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 4)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "name": self.CONSOLIDATE_WITHOUT_CROSSDOCK,
                        "buckets": [
                            {"warehouseId": 444, "offers": ElementCount(2)},
                            {"warehouseId": 172, "offers": ElementCount(2)},
                            {"warehouseId": 555, "offers": ElementCount(1)},
                            {"warehouseId": 0, "offers": ElementCount(1)},
                        ],
                    }
                ]
            },
        )

        parsed_response = json.loads(str(response))
        buckets = parsed_response['search']['results'][0]['buckets']

        self.assertTrue(buckets[-1]['warehouseId'] == 0)

    def test_delivery_day_and_sorting(self):
        """
        Проверяем, что под флагом calculate_delivery_day_on_combine
        вычисляются и выводятся в бакетах даты доставки
        и, кроме того, бакеты сортируются по их возрастанию
        (бакеты без доставки остаются последними)
        """

        def bucket(warehouse_id, delivery_day_from=None):
            return {
                "warehouseId": warehouse_id,
                "deliveryDayFrom": delivery_day_from or Absent(),
            }

        def expected_buckets(enable_calculate):
            if enable_calculate:
                return [
                    bucket(444, 1),
                    bucket(172, 2),
                    bucket(555, 3),
                    bucket(0),
                ]
            else:
                return [
                    bucket(172),
                    bucket(444),
                    bucket(555),
                    bucket(0),
                ]

        for enable_calculate in [False, True]:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.hoover, _MSKUs.fridge, _MSKUs.microwave, _MSKUs.notebook),
                (
                    _Offers.alyonka_s1_172,
                    _Offers.coke_s1_148,
                    _Offers.hoover_444_offer,
                    _Offers.fridge_555_offer,
                    _Offers.microwave_555_offer,
                    _Offers.notebook_out_of_stock,
                ),
                flags='&rearr-factors=calculate_delivery_day_on_combine={}'.format(1 if enable_calculate else 0),
            )

            self.assertFragmentIn(
                response,
                {"results": [{"entity": "split-strategy", "buckets": expected_buckets(enable_calculate)}]},
                preserve_order=True,
            )

    def test_filter_reason(self):
        """
        Проверяем, что у недоступных офферов присутствует причина фильтрации
        """

        for waremd5, msku, reason in [
            ("Alyonka_s1_172_______g", ";msku:1", "DELIVERY_BLUE"),
            ("22222222222222gggggggg", "", "DELIVERY"),
            (
                "22222222222221gggggggg",
                "",
                "DELIVERY",
            ),  # Несуществующий офер — для него также временно будет показываться фиктивная причина DELIVERY
            # до починки https://st.yandex-team.ru/MARKETOUT-37075
        ]:
            response = self.report.request_json(
                PLACE_COMBINE_WITH_RGB + "&debug=1&rids=111&offers-list={}:4{};cart_item_id:1".format(waremd5, msku)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "split-strategy",
                            "name": "failed-strategy",
                            "buckets": [
                                {
                                    "warehouseId": 0,
                                    "shopId": 0,
                                    "isFulfillment": False,
                                    "offers": [
                                        {
                                            "wareId": "",
                                            "replacedId": waremd5,
                                            "count": 4,
                                            "reason": reason,
                                            "cartItemIds": [1],
                                        }
                                    ],
                                }
                            ],
                        }
                    ]
                },
            )

        # Проверяем, что у белых офферов причина фильтрации прокидывается и может быть не только "DELIVERY"
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + "&rids=213&offers-list={}:4{};cart_item_id:1&min-delivery-priority=priority".format(
                "55555555555555gggggggg", ""
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "warehouseId": 0,
                                "shopId": 0,
                                "isFulfillment": False,
                                "offers": [
                                    {
                                        "wareId": "",
                                        "replacedId": "55555555555555gggggggg",
                                        "count": 4,
                                        "reason": "DELIVERY_PRIORITY",
                                        "cartItemIds": [1],
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
        )

    def test_white_offer(self):
        """
        Проверяем, что ручка комбайн для DSBS (белых) оферов подготавливает отдельную посылку без warehouseId
        Также проверяем основные комбинации в корзине с синими оферами, с другими белыми того же и другого магазина
        и с белыми, имеющими msku
        """

        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + "&rids=213&offers-list=Alyonka_s1_172_______g:1;msku:1,22222222222222gggggggg:1;cart_item_id:1"
        )
        self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "shopId": 1001,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": "22222222222222gggggggg",
                                        "replacedId": "22222222222222gggggggg",
                                        "count": 1,
                                        "cartItemIds": [1],
                                    }
                                ],
                            }
                        ],
                    }
                ],
                "offers": {
                    "items": [
                        {"wareId": "22222222222222gggggggg"},
                        {"wareId": "Alyonka_s1_172_______g"},
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "offers": {
                    "items": [
                        {"wareId": "33333333333333gggggggg"},  # тот же msku, что и у Alyonka_s1_172_______g
                    ]
                }
            },
        )

        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + "&rids=213&offers-list=Alyonka_s1_172_______g:1;msku:1,33333333333333gggggggg:1;cart_item_id:1"
        )
        self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "shopId": 1001,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": "33333333333333gggggggg",
                                        "replacedId": "33333333333333gggggggg",
                                        "count": 1,
                                        "cartItemIds": [1],
                                    }
                                ],
                            }
                        ],
                    }
                ],
                "offers": {
                    "items": [
                        {"wareId": "33333333333333gggggggg"},
                        {"wareId": "Alyonka_s1_172_______g"},
                    ]
                },
            },
        )

        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB + "&rids=213&offers-list=22222222222222gggggggg:3;cart_item_id:1&debug=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "shopId": 1001,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": "22222222222222gggggggg",
                                        "replacedId": "22222222222222gggggggg",
                                        "count": 3,
                                        "cartItemIds": [1],
                                    }
                                ],
                            }
                        ],
                    }
                ],
                "offers": {
                    "items": [
                        {"wareId": "22222222222222gggggggg"},
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    Regex(r'\[ME\] .* Candidate offer .* from warehouse [0-9]+'),  # отсутствует подбор синих оферов
                ]
            },
        )

        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + "&rids=213&offers-list=22222222222222gggggggg:3;cart_item_id:1,33333333333333gggggggg:4;cart_item_id:2&debug=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "shopId": 1001,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": "22222222222222gggggggg",
                                        "replacedId": "22222222222222gggggggg",
                                        "count": 3,
                                        "cartItemIds": [1],
                                    },
                                    {
                                        "wareId": "33333333333333gggggggg",
                                        "replacedId": "33333333333333gggggggg",
                                        "count": 4,
                                        "cartItemIds": [2],
                                    },
                                ],
                            }
                        ],
                    }
                ],
                "offers": {
                    "items": [
                        {"wareId": "22222222222222gggggggg"},
                        {"wareId": "33333333333333gggggggg"},
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    Regex(r'\[ME\] .* Candidate offer .* from warehouse [0-9]+'),  # отсутствует подбор синих оферов
                ]
            },
        )

        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + "&rids=213&offers-list=22222222222222gggggggg:3;cart_item_id:1,44444444444444gggggggg:4;cart_item_id:2&debug=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "shopId": 1001,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": "22222222222222gggggggg",
                                        "replacedId": "22222222222222gggggggg",
                                        "count": 3,
                                        "cartItemIds": [1],
                                    }
                                ],
                            },
                            {
                                "shopId": 1002,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": "44444444444444gggggggg",
                                        "replacedId": "44444444444444gggggggg",
                                        "count": 4,
                                        "cartItemIds": [2],
                                    }
                                ],
                            },
                        ],
                    }
                ],
                "offers": {
                    "items": [
                        {"wareId": "22222222222222gggggggg"},
                        {"wareId": "44444444444444gggggggg"},
                    ]
                },
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "logicTrace": [
                    Regex(r'\[ME\] .* Candidate offer .* from warehouse [0-9]+'),  # отсутствует подбор синих оферов
                ]
            },
        )

    def test_white_with_msku(self):
        """Проверяем что если попросить белый dsbs с msku то мы не пытаемся добавить в замену синие офера с тем же
        msku"""

        def get_cgi_offer(offer, msku=None, count=1, cart_item_id=None):
            result = '%s:%s' % (offer.waremd5, count)
            if msku:
                result += ';msku:%s' % (msku)
            if cart_item_id:
                result += ';cart_item_id:%s' % (cart_item_id)
            return result

        # просим только белый 33333333333333gggggggg с msku==1
        # проверяем что синий Alyonka_s1_172_______g с тем же msku тут не подмешивается
        offers = ','.join([get_cgi_offer(_WhiteOffers.alyonka_1001, 1, cart_item_id=1)])
        response = self.report.request_json(PLACE_COMBINE_WITH_RGB + "&rids=213&offers-list=%s" % (offers))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1001, _WhiteOffers.alyonka_1001, cart_item_id=1
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # в search.offers.items должен быть только 1 офер: белый 33333333333333gggggggg
        self.assertFragmentIn(
            response,
            {"search": {"offers": {"items": [{"wareId": "33333333333333gggggggg"}]}}},
            allow_different_len=False,
        )

        # просим белый 33333333333333gggggggg с msku==1 и синий Alyonka_s1_172_______g с тем же msku
        # оба должны быть в ответе, и ничего кроме них
        offers = ','.join(
            [
                get_cgi_offer(_WhiteOffers.alyonka_1001, 1, cart_item_id=1),
                get_cgi_offer(_Offers.alyonka_s1_172, 1, cart_item_id=2),
            ]
        )
        response = self.report.request_json(PLACE_COMBINE_WITH_RGB + "&rids=213&offers-list=%s" % (offers))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1001, _WhiteOffers.alyonka_1001, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "offers": {"items": [{"wareId": "33333333333333gggggggg"}, {"wareId": "Alyonka_s1_172_______g"}]}
                }
            },
            allow_different_len=False,
        )

        # просим белый 33333333333333gggggggg с msku==1 и белый 22222222222222gggggggg
        offers = ','.join(
            [
                get_cgi_offer(_WhiteOffers.alyonka_1001, 1, cart_item_id=1),
                get_cgi_offer(_WhiteOffers.no_msku_1001, cart_item_id=2),
            ]
        )
        response = self.report.request_json(PLACE_COMBINE_WITH_RGB + "&rids=213&offers-list=%s" % (offers))
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1001, _WhiteOffers.alyonka_1001, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1001, _WhiteOffers.no_msku_1001, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "offers": {"items": [{"wareId": "33333333333333gggggggg"}, {"wareId": "22222222222222gggggggg"}]}
                }
            },
            allow_different_len=False,
        )

    def test_cart_item_id(self):
        """
        Проверяем что если в запросе нет cartItemId или там мусор то и в ответе не будет cart_item_id
        """

        request = (
            PLACE_COMBINE_WITH_RGB + '&debug=1&rids=39&offers-list='
            'Alyonka_s1_172_______g:1;msku:1,'
            'Hoover_s1_147________g:1;msku:3;cart_item_id:I,'
            'Notebook_out_of_stockg:1;msku:9;cart_item_id:2'
        )

        response = self.report.request_json(request)

        self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172)
        self.check_offer_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.hoover_s1_147)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.notebook_out_of_stock, cart_item_id=2, fail=True
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 3)

    def check_separate_parcel(self, response, shop_id, offers_list, is_digital):
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "buckets": [
                            {
                                "shopId": shop_id,
                                "isDigital": is_digital,
                                "offers": [{"wareId": offer_id, "count": count} for offer_id, count in offers_list],
                            }
                        ]
                    }
                ]
            },
        )

    def test_digital_goods(self):
        offer_list = [
            ('DiGitalOfferInShop1_1g', 42),
            ('DiGitalOfferInShop1_2g', 1),
            ('DiGitalOfferInShop2_1g', 15),
            ('NeDiGitalOfferInShop2g', 2),
        ]

        request = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=213'
            + '&offers-list='
            + ','.join(['{waremd5}:{count}'.format(waremd5=offer_id, count=count) for offer_id, count in offer_list])
        )

        response = self.report.request_json(request)
        # Два электронных товара от одного магазина
        self.check_separate_parcel(
            response,
            shop_id=_Shops.FIRST_DIGITAL_SUPPLIER_ID,
            offers_list=[('DiGitalOfferInShop1_1g', 42), ('DiGitalOfferInShop1_2g', 1)],
            is_digital=True,
        )
        # Электронный товар от второго магазина
        self.check_separate_parcel(
            response,
            shop_id=_Shops.SECOND_DIGITAL_SUPPLIER_ID,
            offers_list=[('DiGitalOfferInShop2_1g', 15)],
            is_digital=True,
        )
        # Обычный товар от второго магазина в отдельной посылке
        self.check_separate_parcel(
            response,
            shop_id=_Shops.SECOND_DIGITAL_SUPPLIER_ID,
            offers_list=[('NeDiGitalOfferInShop2g', 2)],
            is_digital=False,
        )

    def test_restrictions_lists(self):
        EXCLUDE_BANNED_WAREHOUSES_FLAG = (
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=1', False),
            ('&rearr-factors=exclude_banned_warehouses_in_consolidation=0', True),
        )
        EXCLUDE_PROMO_FLAG = '&rearr-factors=exclude_promo_in_consolidation=1'

        for consolidation_flag, enable_consolidation in EXCLUDE_BANNED_WAREHOUSES_FLAG:
            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.coke),
                (_Offers.alyonka_s1_149, _Offers.coke_s2_147),
                flags=consolidation_flag + EXCLUDE_PROMO_FLAG,
                count=17,
                region=39,
            )
            if enable_consolidation:
                # с разрешением замен с ростовского склада, ROSTOV_RESTRICTION не появится в обоих списках ограничений,
                # во первом так как мы не пользуемся возможностью заменить товар с ростова, во втором так как ограничения нет
                # при этом в общем стратегия неуспешна, так как нет ни одного преодоленного ограничения
                self.check_parcel_restrictions_lists(
                    response, 1, success=False, overcome_restrictions=[], current_restrictions=[]
                )
            else:
                # без разрешения замен с ростовского склада, ROSTOV_RESTRICTION появится в текущих ограничениях на замены
                self.check_parcel_restrictions_lists(
                    response,
                    1,
                    success=False,
                    overcome_restrictions=[],
                    current_restrictions=[
                        self.ROSTOV_RESTRICTION,
                    ],
                )

            response = self.request_combine(
                (_MSKUs.alyonka, _MSKUs.coke, _MSKUs.phone),
                (_Offers.alyonka_s1_172, _Offers.coke_s1_148, _Offers.phone_1_147),
                flags=consolidation_flag + EXCLUDE_PROMO_FLAG,
                region=39,
            )

            if enable_consolidation:
                # в данном случае, мы воспользовались возможностью замены на товар с ростовского склада,
                # поэтому в списке преодоленных ограничений появится ROSTOV_RESTRICTION
                # в этом случае, стратегия в общем успешная
                self.check_parcel_restrictions_lists(
                    response, 1, success=True, overcome_restrictions=[self.ROSTOV_RESTRICTION], current_restrictions=[]
                )
            else:
                self.check_parcel_restrictions_lists(
                    response,
                    1,
                    success=False,
                    overcome_restrictions=[],
                    current_restrictions=[
                        self.ROSTOV_RESTRICTION,
                    ],
                )

    def test_unique_restriction(self):
        """
        Проверяем, что в списках ограничений на замены элементы не дублируются
        """
        request_with_two_promo = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=213&offers-list=Alyonka_s1_147_______g:4;msku:1;cart_item_id:1;promo_type:'
            + PromoType.BLUE_CASHBACK
            + ';promo_id:abrakadabra,'
            + 'Hoover_s1_147________g:4;msku:3;cart_item_id:2;promo_type:'
            + PromoType.BLUE_CASHBACK
            + ';promo_id:abrakadabra'
            + '&rearr-factors=exclude_banned_warehouses_in_consolidation=0;exclude_promo_in_consolidation=1'
        )

        response = self.report.request_json(request_with_two_promo)
        self.check_parcel_restrictions_lists(
            response, 1, success=False, overcome_restrictions=[], current_restrictions=[self.PROMO_RESTRICTION]
        )

    def test_timeout_bug_with_msku_0(self):
        """
        Проверяем, что msku:0 не выключает фильтрацию по msku (приводя к поиску по всем оферам без msku)
        """

        # Не должно ничего находиться: офер несуществующий и msku не указан
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB + "&debug=1&rids=213&offers-list=22222222222221gggggggg:1"
        )
        self.assertFragmentNotIn(response, {"TOTAL_DOCUMENTS_ACCEPTED": Greater(0)})

        # Не должно ничего находиться: офер несуществующий и msku несуществующий
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB + "&debug=1&rids=213&offers-list=22222222222221gggggggg:1;msku:dummy"
        )
        self.assertFragmentNotIn(response, {"TOTAL_DOCUMENTS_ACCEPTED": Greater(0)})

        # Не должно ничего находиться: офер несуществующий и msku:0 (как будто не был указан, считаем 0 невалидным значением)
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB + "&debug=1&rids=213&offers-list=22222222222221gggggggg:1;msku:0"
        )
        self.assertFragmentNotIn(response, {"TOTAL_DOCUMENTS_ACCEPTED": Greater(0)})

    def test_exclude_bundles(self):
        """
        Проверяем, что если для офферов указан bundle_id, то они не подменяются
        """
        # Проверяем без бандла
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s1_172, _Offers.coke_s1_148), region=213, count=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        # Проверяем с указанием бандла в параметрах
        response = self.report.request_json(
            PLACE_COMBINE_WITH_RGB
            + '&rids=213&offers-list=Alyonka_s1_172_______g:4;msku:1;cart_item_id:1,CocaCola_s1_148______g:4;msku:2;cart_item_id:2;bundle_id:123445'
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_148, 4, cart_item_id=2
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    @classmethod
    def mock_combinator_response(
        cls,
        request_orders,
        response_baskets,
        response_unreachable_items,
        add_delivery_stats=True,
        split_status=SplitStatus.SPLIT_OK,
        baskets_with_partial_delivery=[],
    ):
        cls.combinator.on_split_orders_request(
            orders=[
                OrdersSplitRequest(
                    order_id=id,
                    items=[
                        DeliveryItem(
                            required_count=count,
                            weight=offer.weight * 1000,
                            price=offer.price,
                            dimensions=[offer.dimensions.length, offer.dimensions.width, offer.dimensions.height],
                            cargo_types=offer.cargo_types,
                            offers=[
                                CombinatorOffer(
                                    shop_sku=offer.offerid,
                                    shop_id=offer.supplier_id,
                                    partner_id=warehouse,
                                    available_count=count,
                                )
                            ],
                        )
                        for offer, count, warehouse in order
                    ],
                )
                for id, order in enumerate(request_orders)
            ],
            destination=Destination(region_id=213),
        ).respond_with_split_orders(
            response_orders=[
                OrdersSplitResponse(
                    order_id=id,
                    status=split_status,
                    unreachable_items=[
                        DeliveryItem(
                            required_count=count,
                            weight=offer.weight * 1000,
                            price=offer.price,
                            dimensions=[offer.dimensions.length, offer.dimensions.width, offer.dimensions.height],
                            cargo_types=offer.cargo_types,
                            offers=[
                                CombinatorOffer(
                                    shop_sku=offer.offerid,
                                    shop_id=offer.supplier_id,
                                    partner_id=warehouse,
                                    available_count=count,
                                )
                            ],
                        )
                        for offer, count, warehouse in response_unreachable_items
                    ],
                    baskets=[
                        SplitBasket(
                            courier_stats=DeliveryStats(cost=10, day_from=1, day_to=3) if add_delivery_stats else None,
                            pickup_stats=DeliveryStats(cost=20, day_from=2, day_to=3) if add_delivery_stats else None,
                            post_stats=DeliveryStats(cost=30, day_from=3, day_to=3) if add_delivery_stats else None,
                            on_demand_stats=DeliveryStats(cost=40, day_from=2, day_to=4)
                            if add_delivery_stats
                            else None,
                            partial_delivery=baskets_with_partial_delivery[oid]
                            if len(baskets_with_partial_delivery) > oid
                            else False,
                            point_types=[PickupPointType.SERVICE_POINT, PickupPointType.POST_OFFICE],
                            items=[
                                DeliveryItem(
                                    required_count=count,
                                    weight=offer.weight * 1000,
                                    price=offer.price,
                                    dimensions=[
                                        offer.dimensions.length,
                                        offer.dimensions.width,
                                        offer.dimensions.height,
                                    ],
                                    cargo_types=offer.cargo_types,
                                    offers=[
                                        CombinatorOffer(
                                            shop_sku=offer.offerid,
                                            shop_id=offer.supplier_id,
                                            partner_id=warehouse,
                                            available_count=count,
                                        )
                                    ],
                                )
                                for offer, count, warehouse in order
                            ],
                        )
                        for oid, order in enumerate(basket)
                    ],
                )
                for id, basket in enumerate(response_baskets)
            ]
        )

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

    @classmethod
    def prepare_split_cart_on_combinator(cls):
        # посылка из двух товаров сплитуется на две посылки комбинатором
        alenka = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=4, warehouse=172)
        coke = CombinatorRequestItem(offer=_Offers.coke_s1_172, count=4, warehouse=172)

        request_order = [alenka, coke]
        response_order1 = [alenka]
        response_order2 = [coke]
        cls.mock_combinator_response(
            request_orders=(request_order,),
            response_baskets=(
                [  # basket
                    response_order1,  # order
                    response_order2,  # order
                ],
            ),
            response_unreachable_items=[],
        )

    def test_split_cart_on_combinator(self):
        """Проверяется, что посылка корректно сплитуется на две посылки, если комбинатор
        прислал такое разделение"""

        # запрос без доразбиения посылок на комбинаторе
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke), (_Offers.alyonka_s1_172, _Offers.coke_s1_148), region=213, count=4
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        # в корзине одна посылка
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)

        # флаг "wasSplitByCombinator" показывает, что посылка не была разбита
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "warehouseId": 172,
                                "wasSplitByCombinator": False,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        # запрос с доразбиением посылок на комбинаторе
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke),
            (_Offers.alyonka_s1_172, _Offers.coke_s1_148),
            region=213,
            count=4,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 4, cart_item_id=2
        )
        # в корзине две посылки
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

        # у каждой посылки флаг "wasSplitByCombinator" показывает, что посылка была разбита комбинатором
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "warehouseId": 172,
                                "wasSplitByCombinator": True,
                                "hasCourier": True,
                                "hasPickup": True,
                                "hasOnDemand": True,
                                "deliveryDayFrom": 1,
                            },
                            {
                                "warehouseId": 172,
                                "wasSplitByCombinator": True,
                                "hasCourier": True,
                                "hasPickup": True,
                                "hasOnDemand": True,
                                "deliveryDayFrom": 1,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_unreachable_items(cls):
        # посылка из двух товаров сплитуется на одну доствляемую посылку и один недоставляемый оффер
        alenka = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=3, warehouse=172)
        coke = CombinatorRequestItem(offer=_Offers.coke_s1_172, count=3, warehouse=172)

        request_order = [alenka, coke]
        response_order = [alenka]
        unreachable_items = [coke]

        cls.mock_combinator_response(
            request_orders=(request_order,),
            response_baskets=(
                [  # basket
                    response_order,
                ],
            ),
            response_unreachable_items=unreachable_items,
        )

    def test_unreachable_items(self):
        """Проверяется, что у недоставляемых офферов выставляется причина недоступности "reason": "DELIVERY_OPTIONS"
        и происходит выделение таких офферов в отдельную посылку"""
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke),
            (_Offers.alyonka_s1_172, _Offers.coke_s1_148),
            region=213,
            count=3,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 3, cart_item_id=1
        )
        # в ответе комбинатора две посылки
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 3, cart_item_id=2
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "warehouseId": 172,
                                "offers": [
                                    {
                                        "wareId": _Offers.coke_s1_172.waremd5,
                                        "replacedId": _Offers.coke_s1_148.waremd5,
                                        "count": 3,
                                        "reason": "DELIVERY_OPTIONS",
                                        "cartItemIds": [2],
                                    }
                                ],
                            }
                        ],
                    }
                ]
            },
        )

    @classmethod
    def prepare_combinator_response_without_delivery(cls):
        # в первом случае комбинатор ничего не сплитует, но считает статистику доставки
        # во втором случае ответ комбинатора аналогичный, но нет статистики доставки
        alenka_count_1 = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=1, warehouse=172)
        alenka_count_2 = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=2, warehouse=172)

        request_order_1 = [alenka_count_1]
        cls.mock_combinator_response(
            request_orders=(request_order_1,),
            response_baskets=(
                [  # basket
                    request_order_1,  # order
                ],
            ),
            response_unreachable_items=[],
            add_delivery_stats=True,
            split_status=SplitStatus.NOTHING,
        )

        request_order_2 = [alenka_count_2]
        cls.mock_combinator_response(
            request_orders=(request_order_2,),
            response_baskets=(
                [  # basket
                    request_order_2,  # order
                ],
            ),
            response_unreachable_items=[],
            add_delivery_stats=False,
            split_status=SplitStatus.NOTHING,
        )

    def test_combinator_response_without_delivery(self):
        """Проверяется, что если комбинатор не прислал статистику доставки посылки, то происходит расчет
        статистики обычным способом"""
        response = self.request_combine(
            (_MSKUs.alyonka,),
            (_Offers.alyonka_s1_172,),
            region=213,
            count=1,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )

        # берем статистику доставки из ответа комбинатора
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "warehouseId": 172,
                                "wasSplitByCombinator": False,
                                "hasCourier": True,
                                "hasPickup": True,
                                "hasOnDemand": True,
                                "deliveryDayFrom": 1,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.request_combine(
            (_MSKUs.alyonka,),
            (_Offers.alyonka_s1_172,),
            region=213,
            count=2,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )

        # отсутствует статистика дотавки в ответе комбинатора, рассчитываем обычным способом
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "warehouseId": 172,
                                "wasSplitByCombinator": False,
                                "hasCourier": True,
                                "hasPickup": False,
                                "hasOnDemand": False,
                                "deliveryDayFrom": 2,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_check_unreachable_items_size(cls):
        # Все товары из посылки перекладываются в unreachable items
        alenka = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=2, warehouse=172)
        coke = CombinatorRequestItem(offer=_Offers.coke_s1_172, count=2, warehouse=172)

        request_order = [alenka, coke]
        unreachable_items = [alenka, coke]

        cls.mock_combinator_response(
            request_orders=(request_order,),
            response_baskets=([],),  # basket
            response_unreachable_items=unreachable_items,
        )

    def test_check_unreachable_items_size(self):
        """Проверяется, что если все товары из посылки переложились комбинатором в unreachable_items, =
        то не верим комбинатору, и оставляем посылку как есть. Сделано на случай отключения служб доставки
        по крупным регионам"""
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.coke),
            (_Offers.alyonka_s1_172, _Offers.coke_s1_148),
            region=213,
            count=2,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1&debug=1",
        )
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 2, cart_item_id=1
        )
        # в ответе комбинатора две посылки
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.coke_s1_172, 2, cart_item_id=2
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "split-strategy",
                        "buckets": [
                            {
                                "warehouseId": 172,
                                "wasSplitByCombinator": False,
                                "offers": [
                                    {
                                        "wareId": _Offers.alyonka_s1_172.waremd5,
                                        "replacedId": _Offers.alyonka_s1_172.waremd5,
                                        "count": 2,
                                        "reason": Absent(),
                                        "cartItemIds": [1],
                                    },
                                    {
                                        "wareId": _Offers.coke_s1_172.waremd5,
                                        "replacedId": _Offers.coke_s1_148.waremd5,
                                        "count": 2,
                                        "reason": Absent(),
                                        "cartItemIds": [2],
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {'logicTrace': [Contains('Unreachable items size is equal of offers count in parcel')]},
        )

    @classmethod
    def prepare_split_offer_quantity(cls):
        # посылка из четырех одинаковых товаров разбивается на две посылки по два и четыре товара комбинатором
        alenka = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=6, warehouse=172)
        alenka_4 = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=4, warehouse=172)
        alenka_2 = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=2, warehouse=172)

        request_order = [alenka]
        response_order1 = [alenka_4]
        response_order2 = [alenka_2]
        cls.mock_combinator_response(
            request_orders=(request_order,),
            response_baskets=(
                [  # basket
                    response_order1,  # order
                    response_order2,  # order
                ],
            ),
            response_unreachable_items=[],
        )

    def test_split_offer_quantity(self):
        """Проверяется, что посылка из 6 офферов корректно сплитуется на две посылки по 4 и 2 оффера, если комбинатор
        прислал такое разделение"""

        # запрос с доразбиением посылок на комбинаторе
        response = self.request_combine(
            (_MSKUs.alyonka,),
            (_Offers.alyonka_s1_172,),
            region=213,
            count=6,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )
        # проверяем, что есть посылка с 4 офферами
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 4, cart_item_id=1
        )
        # проверяем, что есть посылка с 2 офферами
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.alyonka_s1_172, 2, cart_item_id=1
        )
        # в корзине две посылки
        self.check_total_buckets_in_strategy(response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 2)

    @classmethod
    def prepare_split_partial_checkout(cls):
        # посылка из двух товаров, один из которых 1р и 600 карготип сплитуется на две посылки
        alenka = CombinatorRequestItem(offer=_Offers.alyonka_s1_172, count=4, warehouse=172)
        shoes = CombinatorRequestItem(offer=_Offers.offer_fashion_1p_600_cargo, count=4, warehouse=172)

        request_order = [alenka, shoes]
        response_order1 = [alenka]
        response_order2 = [shoes]
        cls.mock_combinator_response(
            request_orders=(request_order,),
            response_baskets=(
                [
                    response_order1,
                    response_order2,
                ],
            ),
            response_unreachable_items=[],
            baskets_with_partial_delivery=[False, True],
        )

    def test_split_partial_checkout(self):

        # запрос с доразбиением посылок на комбинаторе
        response = self.request_combine(
            (_MSKUs.alyonka, _MSKUs.shoes),
            (_Offers.alyonka_s1_172, _Offers.offer_fashion_1p_600_cargo),
            region=213,
            count=4,
            flags="&rearr-factors=enable_cart_split_on_combinator=1&combinator=1",
        )

        # у каждой посылки флаг "isPartialDeliveryAvailable" показывает, что посылка предназначена для частичного выкупа
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "buckets": [
                            {
                                "wasSplitByCombinator": True,
                                "isPartialDeliveryAvailable": False,
                            },
                            {
                                "wasSplitByCombinator": True,
                                "isPartialDeliveryAvailable": True,
                            },
                        ],
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_exclude_express_offers_in_consolidation(self):
        # Проверяем, что экспресс оффера не участвуют в консолидации
        # Проверка номер раз:
        # В 213 регионе для мскю с цветами есть два оффера: usual_flowers_low_priority и express_flowers, второй с более приоритетного склада
        # Делаем запрос в combine с usual_flowers_low_priority и проверяем, что оффер не подменится на экспресс
        response = self.request_combine([_MSKUs.flowers], (_Offers.usual_flowers_low_priority,), region=213)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.usual_flowers_low_priority, cart_item_id=1
        )

        # Проверка номер два:
        # В 214 регионе для мскю с цветами есть два оффера: usual_flowers_high_priority и express_flowers, второй с менее приоритетного склада
        # Делаем запрос в combine с express_flowers и проверяем, что экспресс оффер не подменится на обычный
        response = self.request_combine([_MSKUs.flowers], (_Offers.express_flowers,), region=214)
        self.check_offer_in_strategy(
            response, self.CONSOLIDATE_WITHOUT_CROSSDOCK, 1, _Offers.express_flowers, cart_item_id=1
        )

    def test_beru_warehouse_id(self):
        """
        Проверяем, что параметр at-beru-warehouses работает
        """

        response = self.report.request_json(
            "place=prime&text=Alyonka&at-beru-warehouse=1" + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )
        self.assertFragmentIn(response, {"results": NotEmptyList()})

    @classmethod
    def prepare_test_filters_in_output(cls):

        cls.index.gltypes += [
            GLType(
                hid=_Hids.any_category,
                param_id=_GlParamIds.jump_table_size,
                unit_param_id=_GlParamIds.woman_dress_size_units,
                subtype='size',
                name="vendor size",
                xslname="size",
                cluster_filter=True,
                gltype=GLType.ENUM,
                model_filter_index=1,
                position=-1,
                values=[
                    # original glparams
                    GLValue(position=1, value_id=1, text='36', unit_value_id=11),
                    GLValue(position=2, value_id=2, text='40', unit_value_id=11),
                    GLValue(position=3, value_id=3, text='42', unit_value_id=11),
                    # unit INT
                    GLValue(position=4, value_id=4, text='M', unit_value_id=22),
                    GLValue(position=5, value_id=5, text='S', unit_value_id=22),
                    GLValue(position=6, value_id=6, text='L', unit_value_id=22),
                    # unit RU
                    GLValue(position=7, value_id=7, text='42', unit_value_id=33),
                    GLValue(position=8, value_id=8, text='44', unit_value_id=33),
                    GLValue(position=9, value_id=9, text='46', unit_value_id=33),
                    GLValue(position=10, value_id=10, text='48', unit_value_id=33),
                ],
            ),
            GLType(
                hid=_Hids.any_category,
                param_id=_GlParamIds.jump_table_color,
                name="Color",
                xslname="color",
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[_ColorIds.grafit, _ColorIds.yellow],
                model_filter_index=2,
                position=-1,
            ),
            GLType(
                param_id=_GlParamIds.woman_dress_size_units,
                hid=_Hids.any_category,
                gltype=GLType.ENUM,
                name="woman dress size units",
                values=[
                    GLValue(value_id=11, text='EU'),
                    GLValue(value_id=22, text='INT'),
                    GLValue(value_id=33, text='RU'),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hid=_Hids.any_category,
                title="Model any category",
                hyperid=_Hids.any_category,
                glparams=[GLParam(param_id=_GlParamIds.color, value=_ColorIds.gray)],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=_Hids.any_category,
                hyperid=_Hids.any_category,
                sku=50,
                title="woman dress, M",
                blue_offers=[_Offers.blue_offer_any_category],
                glparams=[
                    GLParam(param_id=_GlParamIds.jump_table_size, value=2),
                    GLParam(param_id=_GlParamIds.jump_table_size, value=4),
                    GLParam(param_id=_GlParamIds.jump_table_size, value=8),
                    GLParam(param_id=_GlParamIds.jump_table_size, value=9),
                    GLParam(param_id=_GlParamIds.jump_table_color, value=_ColorIds.grafit),
                ],
                delivery_buckets=[801, 802],
            ),
        ]

    # Проверяем что в выдачу попадают только фильтры из карты переходов (офферные фильтры, фильтры 2-го рода), модельный фильтр не попадает
    def test_filters_in_output(self):
        request = PLACE_COMBINE_WITH_RGB + "&rids=213&debug=1&offers-list={}:4;msku:50".format(
            _Offers.blue_offer_any_category.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": str(_GlParamIds.jump_table_size), "values": [{"id": str(2)}], "kind": 2},
                    {"id": str(_GlParamIds.jump_table_color), "values": [{"id": str(_ColorIds.grafit)}], "kind": 2},
                ]
            },
            allow_different_len=False,
        )

    # Проверяем что в выдачу попадает таблица размеров
    def test_size_table_in_output(self):
        request = PLACE_COMBINE_WITH_RGB + "&rids=213&debug=1&offers-list={}:4;msku:50".format(
            _Offers.blue_offer_any_category.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "sizes_table": {
                    "header": [
                        {
                            "unit_name": "vendor size",
                        },
                        {
                            "unit_name": "INT",
                        },
                        {
                            "unit_name": "RU",
                        },
                    ],
                    "msku_list": [
                        {
                            "values": [
                                {
                                    "unit_name": "vendor size",
                                    "value_min": "40",
                                    "value_max": "40",
                                },
                                {"unit_name": "INT", "value_min": "M", "value_max": "M"},
                                {"unit_name": "RU", "value_min": "44", "value_max": "46"},
                            ],
                        },
                    ],
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
