#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, timedelta

from core.report import REQUEST_TIMESTAMP
from core.matcher import Absent, EmptyList, NotEmpty
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    BundleOfferId,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicBlueGenericBundlesPromos,
    DynamicDeliveryServiceInfo,
    DynamicQPromos,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    GpsCoord,
    MarketSku,
    Outlet,
    PickupBucket,
    PickupOption,
    Promo,
    PromoMSKU,
    PromoSaleDetails,
    PromoType,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
)
from core.types.offer_promo import make_generic_bundle_content
from unittest import skip


CATEGORY_ID = 1
MODEL_ID = 1

# По условиям акции типа 'bundle' в комплект могут входить только офферы от разных моделей
RECOM_BUNDLE_MODEL_ID = 2

MSK_RID = 213
MSK_WAREHOUSE_ID = 145
MSK_DELIVERY_SERVICE_ID = 1102

SUPPLIER_SHOP_ID = 1
VIRTUAL_SHOP_ID = 2

SUPPLIER_FEED_ID = 1
VIRTUAL_SHOP_FEED_ID = 2

DS_OUTLET_ID_1 = 10001
DS_OUTLET_ID_2 = 10002
POST_OUTLET_ID = 10003
PICKUP_OUTLET_ID = 10004

DELIVERY_BUCKET_ID = 1001
DELIVERY_BUCKET_DC_ID = 6001

POST_BUCKET_ID = 1111
POST_BUCKET_DC_ID = 5001

PICKUP_BUCKET_ID = 1112
PICKUP_BUCKET_DC_ID = 5002

DT_FORMAT = '%Y-%m-%dT%H:%M:%SZ'
DT_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)

GENERIC_BUNDLE_ID = 'Generic_bundle_promo_key'
GENERIC_BUNDLE_GIFT_PRICE = 1
GENERIC_BUNDLE_START = DT_NOW - timedelta(days=1)
GENERIC_BUNDLE_END = DT_NOW + timedelta(weeks=2600)

SECRET_SALE_ID = 'Secret_sale_promo_key'
SECRET_SALE_TITLE = "Закрытая распродажа для клиентов Сбербанка"
SECRET_SALE_DESCR = "Типичное описание распродажи"
SECRET_SALE_DISCOUNT_PERCENT = 10
SECRET_SALE_START = DT_NOW - timedelta(days=5)
SECRET_SALE_END = DT_NOW + timedelta(days=5)

OLD_SECRET_SALE_ID = 'Old_secret_sale_promo_key'
OLD_SECRET_SALE_TITLE = "Уже прошедшая распродажа"
OLD_SECRET_SALE_DESCR = "Типичное описание прошедней распродажи"
OLD_SECRET_SALE_DISCOUNT_PERCENT = 15
OLD_SECRET_SALE_START = DT_NOW - timedelta(days=10)
OLD_SECRET_SALE_END = DT_NOW - timedelta(days=5)

FLASH_3P_DISCOUNT_ID = 'Flash_3p_discount_promo_key'
FLASH_3P_DISCOUNT_START = DT_NOW - timedelta(days=100)
FLASH_3P_DISCOUNT_PERCENT = 50
FLASH_3P_DISCOUNT_OLD_PRICE = 10000
FLASH_3P_DISCOUNT_PROMO_PRICE = 5000

BUNDLE_ID = 'Bundle_promo_key'
RECOM_BUNDLE_ID = 'Recom_bundle_promo_key'

SECRET_SALE_PROMO_RESPONSE_FRAGMENT = {
    'type': PromoType.SECRET_SALE,
    'key': SECRET_SALE_ID,
    'startDate': SECRET_SALE_START.strftime(DT_FORMAT),
    'endDate': SECRET_SALE_END.strftime(DT_FORMAT),
    'description': SECRET_SALE_DESCR,
}

FLASH_3P_DISCOUNT_PROMO_RESPONSE_FRAGMENT = [
    {
        'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
        'key': FLASH_3P_DISCOUNT_ID,
        'startDate': FLASH_3P_DISCOUNT_START.strftime(DT_FORMAT),
    }
]

RECOM_BUNDLE_PROMO_RESPONSE_FRAGMENT = {'type': PromoType.BUNDLE, 'key': RECOM_BUNDLE_ID}

RECOM_BUNDLE_SECONDARY_OFFER = BlueOffer(
    price=55,
    waremd5='Sku20Price005-vm1Goleg',
    offerid='Shop1_sku20_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    hyperid=RECOM_BUNDLE_MODEL_ID,
    promo=[],
)

# Заводим еще один оффер и Market SKU для того, чтобы проверить помодельную выадчу в place=promo_bundle
# Здесь нужен самый дешевый оффер в рамках модели MODEL_ID
RECOM_BUNDLE_SALE_OFFER = BlueOffer(
    price=100,
    waremd5='Sku21Price001-vm1Goleg',
    offerid='Shop1_sku21_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    hyperid=MODEL_ID,
    blue_promo_key=SECRET_SALE_ID,
    promo=[],
)

SECRET_SALE_OFFER = BlueOffer(
    price=10000,
    offerid='Shop1_sku10_1',
    waremd5='Sku10Price10k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    blue_promo_key=SECRET_SALE_ID,
    promo=[],
)

GENERIC_BUNDLE_PRIMARY_OFFER = BlueOffer(
    price=5000,
    waremd5='Sku11Price05k-vm1Goleg',
    offerid='Shop1_sku11_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    blue_promo_key=SECRET_SALE_ID,
    promo=[],
)

GENERIC_BUNDLE_SECONDARY_OFFER = BlueOffer(
    price=150,
    waremd5='Sku12Price150-vm1Goleg',
    offerid='Shop1_sku12_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    blue_promo_key=SECRET_SALE_ID,
    promo=[],
)

PLAIN_OFFER = BlueOffer(
    price=10000,
    offerid='Shop1_sku13_1',
    waremd5='Sku13Price10k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    promo=[],
)

FLASH_3P_DISCOUNT_OFFER = BlueOffer(
    price=FLASH_3P_DISCOUNT_PROMO_PRICE,
    waremd5='Sku14Price10k-vm1Goleg',
    offerid='Shop1_sku14_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    blue_promo_key=FLASH_3P_DISCOUNT_ID,
    promo=[],
)

DISCOUNT_OFFER = BlueOffer(
    price=5000,
    price_old=10000,
    waremd5='Sku15Price10k-vm1Goleg',
    offerid='Shop1_sku15_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    promo=[],
)

DISCOUNT_SECRET_SALE_OFFER = BlueOffer(
    price=5000,
    price_old=10000,
    waremd5='Sku16Price10k-vm1Goleg',
    offerid='Shop1_sku16_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    blue_promo_key=SECRET_SALE_ID,
    promo=[],
)

OLD_SECRET_SALE_OFFER = BlueOffer(
    price=5000,
    waremd5='Sku17Price10k-vm1Goleg',
    offerid='Shop1_sku17_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    blue_promo_key=OLD_SECRET_SALE_ID,
    promo=[],
)

BUNDLE_SECONDARY_OFFER = BlueOffer(
    price=FLASH_3P_DISCOUNT_PROMO_PRICE,
    waremd5='Sku19Price05k-vm1Goleg',
    offerid='Shop1_sku19_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    promo=[],
)

BUNDLE_PRIMARY_OFFER = BlueOffer(
    price=FLASH_3P_DISCOUNT_PROMO_PRICE,
    waremd5='Sku18Price05k-vm1Goleg',
    offerid='Shop1_sku18_1',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    promo=[
        Promo(
            promo_type=PromoType.BUNDLE,
            key=BUNDLE_ID,
            bundle_price=7500,
            bundle_offer_ids=[
                BundleOfferId(feed_id=SUPPLIER_FEED_ID, offer_id='Shop1_sku18_1'),
                BundleOfferId(feed_id=BUNDLE_SECONDARY_OFFER.feedid, offer_id=BUNDLE_SECONDARY_OFFER.offerid),
            ],
            feed_id=SUPPLIER_FEED_ID,
        )
    ],
)

SECRET_SALE_MSKU = MarketSku(
    title="Тестовый синий оффер по распродаже",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=10,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[SECRET_SALE_OFFER],
)

GENERIC_BUNDLE_PRIMARY_MSKU = MarketSku(
    title="Тестовый синий главный оффер",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=11,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[GENERIC_BUNDLE_PRIMARY_OFFER],
)

GENERIC_BUNDLE_SECONDARY_MSKU = MarketSku(
    title="Тестовый синий оффер-подарок",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=12,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[GENERIC_BUNDLE_SECONDARY_OFFER],
)

PLAIN_MSKU = MarketSku(
    title="Тестовый синий оффер",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=13,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[PLAIN_OFFER],
)

FLASH_3P_DISCOUNT_MSKU = MarketSku(
    title="Тестовый синий оффер с флеш-скидкой",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=14,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[FLASH_3P_DISCOUNT_OFFER],
)

DISCOUNT_MSKU = MarketSku(
    title="Тестовый синий оффер со скидкой",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=15,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[DISCOUNT_OFFER],
)

DISCOUNT_SECRET_SALE_MSKU = MarketSku(
    title="Тестовый синий оффер со скидкой и по распродаже",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=16,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[DISCOUNT_SECRET_SALE_OFFER],
)

OLD_SECRET_SALE_MSKU = MarketSku(
    title="Тестовый синий оффер по уже прошедшей распродаже",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=17,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[OLD_SECRET_SALE_OFFER],
)

BUNDLE_PRIMARY_MSKU = MarketSku(
    title="Тестовый синий главный оффер из набора",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=18,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[BUNDLE_PRIMARY_OFFER],
)

BUNDLE_SECONDARY_MSKU = MarketSku(
    title="Тестовый синий дополнительный оффер из набора",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=19,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[BUNDLE_SECONDARY_OFFER],
)

RECOM_BUNDLE_SECONDARY_MSKU = MarketSku(
    title="Тестовый синий дополнительный оффер из набора от рекомендаций",
    hid=CATEGORY_ID,
    hyperid=RECOM_BUNDLE_MODEL_ID,
    sku=20,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[RECOM_BUNDLE_SECONDARY_OFFER],
)

RECOM_BUNDLE_SALE_MSKU = MarketSku(
    title="Тестовый дешевый синий главный оффер из набора от рекомендаций по распродаже",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku=21,
    delivery_buckets=[DELIVERY_BUCKET_ID],
    pickup_buckets=[PICKUP_BUCKET_ID],
    post_buckets=[POST_BUCKET_ID],
    blue_offers=[RECOM_BUNDLE_SALE_OFFER],
)

PRIME_REQUEST_STRING = (
    'place=prime'
    '&rgb={color}'
    '&rids={rid}'
    '&hid={category}'
    '&offerid={waremd5_list}'
    '&allowed-promos={promos}'
    '&pp=18'
    '&allow-collapsing=0'
    '&numdoc=100'
)

OFFERINFO_REQUEST_STRING = (
    'place=offerinfo'
    '&rgb={color}'
    '&rids={rid}'
    '&offerid={waremd5_list}'
    '&allowed-promos={promos}'
    '&pp=18'
    '&allow-collapsing=0'
    '&show-urls=external'
    '&regset=1'
)

PROMO_BUNDLE_REQUEST_STRING = (
    'place=promo_bundle'
    '&rgb={color}'
    '&pp=18'
    '&rids={rid}'
    '&allowed-promos={promos}'
    '&validate-bundle={validate_bundle}'
)

PLACE_PROMO_BUNDLE_RESPONSE_FRAGMENT = {
    'entity': 'promoBundle',
    'key': RECOM_BUNDLE_ID,
    'offers': [
        {
            'entity': 'offer',
            'model': {'id': RECOM_BUNDLE_SALE_OFFER.hyperid},
            'wareId': RECOM_BUNDLE_SALE_OFFER.waremd5,
            'promos': [RECOM_BUNDLE_PROMO_RESPONSE_FRAGMENT],
        },
        {
            'entity': 'offer',
            'model': {'id': RECOM_BUNDLE_SECONDARY_OFFER.hyperid},
            'wareId': RECOM_BUNDLE_SECONDARY_OFFER.waremd5,
            'promos': Absent(),
        },
    ],
}


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']
        cls.index.regiontree += [Region(rid=MSK_RID, name="Москва")]

    @classmethod
    def prepare_delivery(cls):
        cls.index.outlets += [
            Outlet(
                point_id=DS_OUTLET_ID_1,
                delivery_service_id=MSK_DELIVERY_SERVICE_ID,
                region=MSK_RID,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                bool_props=['prepayAllowed', 'cashAllowed', 'cardAllowed'],
                gps_coord=GpsCoord(37.7, 55.9),
            ),
            Outlet(
                point_id=DS_OUTLET_ID_2,
                delivery_service_id=MSK_DELIVERY_SERVICE_ID,
                region=MSK_RID,
                point_type=Outlet.FOR_POST,
                working_days=[i for i in range(10)],
                bool_props=['cashAllowed'],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=POST_OUTLET_ID,
                delivery_service_id=MSK_DELIVERY_SERVICE_ID,
                region=MSK_RID,
                point_type=Outlet.FOR_PICKUP,
                working_days=[i for i in range(10)],
                bool_props=['prepayAllowed', 'cardAllowed'],
                gps_coord=GpsCoord(97.7, 55.5),
            ),
            Outlet(
                point_id=PICKUP_OUTLET_ID,
                delivery_service_id=MSK_DELIVERY_SERVICE_ID,
                region=MSK_RID,
                point_type=Outlet.FOR_PICKUP,
                working_days=[i for i in range(10)],
                bool_props=['cardAllowed'],
                gps_coord=GpsCoord(37.7, 55.5),
            ),
        ]
        cls.index.shops += [
            Shop(
                fesh=SUPPLIER_SHOP_ID,
                datafeed_id=SUPPLIER_FEED_ID,
                priority_region=MSK_RID,
                regions=[MSK_RID],
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=MSK_WAREHOUSE_ID,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                priority_region=MSK_RID,
                regions=[MSK_RID],
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[DS_OUTLET_ID_1, DS_OUTLET_ID_2],
            ),
        ]
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=VIRTUAL_SHOP_ID, calendar_id=1111),
            DeliveryCalendar(fesh=VIRTUAL_SHOP_ID, calendar_id=1102),
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=MSK_WAREHOUSE_ID, home_region=MSK_RID),
            DynamicDeliveryServiceInfo(id=MSK_DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=MSK_WAREHOUSE_ID, delivery_service_id=MSK_DELIVERY_SERVICE_ID
            ),
        ]

    @classmethod
    def prepare_buckets(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=DELIVERY_BUCKET_ID,
                dc_bucket_id=DELIVERY_BUCKET_DC_ID,
                fesh=SUPPLIER_SHOP_ID,
                carriers=[MSK_DELIVERY_SERVICE_ID],
                regional_options=[
                    RegionalDelivery(
                        rid=MSK_RID, options=[DeliveryOption(day_from=5, day_to=25, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=POST_BUCKET_ID,
                dc_bucket_id=POST_BUCKET_DC_ID,
                fesh=VIRTUAL_SHOP_ID,
                carriers=[MSK_DELIVERY_SERVICE_ID],
                options=[PickupOption(outlet_id=POST_OUTLET_ID, day_from=5, day_to=5, price=30)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=PICKUP_BUCKET_ID,
                dc_bucket_id=PICKUP_BUCKET_DC_ID,
                fesh=VIRTUAL_SHOP_ID,
                carriers=[MSK_DELIVERY_SERVICE_ID],
                options=[PickupOption(outlet_id=PICKUP_OUTLET_ID, day_from=5, day_to=5, price=30)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            SECRET_SALE_MSKU,
            GENERIC_BUNDLE_PRIMARY_MSKU,
            GENERIC_BUNDLE_SECONDARY_MSKU,
            PLAIN_MSKU,
            FLASH_3P_DISCOUNT_MSKU,
            DISCOUNT_MSKU,
            DISCOUNT_SECRET_SALE_MSKU,
            OLD_SECRET_SALE_MSKU,
            BUNDLE_PRIMARY_MSKU,
            BUNDLE_SECONDARY_MSKU,
            RECOM_BUNDLE_SECONDARY_MSKU,
            RECOM_BUNDLE_SALE_MSKU,
        ]

    @classmethod
    def prepare_secret_sale(cls):
        promo = Promo(
            promo_type=PromoType.SECRET_SALE,
            key=SECRET_SALE_ID,
            title=SECRET_SALE_TITLE,
            description=SECRET_SALE_DESCR,
            start_date=SECRET_SALE_START,
            end_date=SECRET_SALE_END,
            secret_sale_details=PromoSaleDetails(
                msku_list=[
                    int(SECRET_SALE_MSKU.sku),
                    int(GENERIC_BUNDLE_PRIMARY_MSKU.sku),
                    int(GENERIC_BUNDLE_SECONDARY_MSKU.sku),
                    int(FLASH_3P_DISCOUNT_MSKU.sku),
                    int(DISCOUNT_SECRET_SALE_MSKU.sku),
                    int(RECOM_BUNDLE_SALE_MSKU.sku),
                ],
                discount_percent_list=[
                    SECRET_SALE_DISCOUNT_PERCENT,
                    SECRET_SALE_DISCOUNT_PERCENT,
                    SECRET_SALE_DISCOUNT_PERCENT,
                    SECRET_SALE_DISCOUNT_PERCENT,
                    SECRET_SALE_DISCOUNT_PERCENT,
                    SECRET_SALE_DISCOUNT_PERCENT,
                ],
            ),
            source_promo_id=SECRET_SALE_ID,
        )
        SECRET_SALE_OFFER.promo += [promo]
        GENERIC_BUNDLE_PRIMARY_OFFER.promo += [promo]
        FLASH_3P_DISCOUNT_OFFER.promo += [promo]
        DISCOUNT_SECRET_SALE_OFFER.promo += [promo]
        RECOM_BUNDLE_SALE_OFFER.promo += [promo]

        old_promo = Promo(
            promo_type=PromoType.SECRET_SALE,
            key=OLD_SECRET_SALE_ID,
            title=OLD_SECRET_SALE_TITLE,
            description=OLD_SECRET_SALE_DESCR,
            start_date=OLD_SECRET_SALE_START,
            end_date=OLD_SECRET_SALE_END,
            secret_sale_details=PromoSaleDetails(
                msku_list=[int(SECRET_SALE_MSKU.sku), int(OLD_SECRET_SALE_MSKU.sku)],
                discount_percent_list=[OLD_SECRET_SALE_DISCOUNT_PERCENT, OLD_SECRET_SALE_DISCOUNT_PERCENT],
            ),
            source_promo_id=OLD_SECRET_SALE_ID,
        )
        SECRET_SALE_OFFER.promo += [old_promo]
        OLD_SECRET_SALE_OFFER.promo += [old_promo]
        cls.index.promos += [promo, old_promo]
        cls.dynamic.qpromos += [DynamicQPromos([promo, old_promo])]

    @classmethod
    def prepare_blue_generic_bundle(cls):
        promo = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=SUPPLIER_FEED_ID,
            key=GENERIC_BUNDLE_ID,
            url='http://beru.ru/generic_bundle',
            start_date=GENERIC_BUNDLE_START,
            end_date=GENERIC_BUNDLE_END,
            generic_bundles_content=[
                make_generic_bundle_content(
                    GENERIC_BUNDLE_PRIMARY_OFFER.offerid,
                    GENERIC_BUNDLE_SECONDARY_OFFER.offerid,
                    GENERIC_BUNDLE_GIFT_PRICE,
                ),
            ],
        )
        GENERIC_BUNDLE_PRIMARY_OFFER.promo += [promo]
        cls.settings.loyalty_enabled = True
        cls.index.promos += [promo]
        cls.dynamic.qpromos += [DynamicQPromos([promo])]
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[GENERIC_BUNDLE_ID])]

    @classmethod
    def prepare_blue_3p_flash_discount(cls):
        promo = Promo(
            promo_type=PromoType.BLUE_3P_FLASH_DISCOUNT,
            key=FLASH_3P_DISCOUNT_ID,
            start_date=FLASH_3P_DISCOUNT_START,
            shop_promo_id=1,
            feed_id=SUPPLIER_FEED_ID,
            mskus=[
                PromoMSKU(
                    msku=str(market_sku.sku),
                    market_promo_price=FLASH_3P_DISCOUNT_PROMO_PRICE,
                    market_old_price=FLASH_3P_DISCOUNT_OLD_PRICE,
                )
                for market_sku in [FLASH_3P_DISCOUNT_MSKU, BUNDLE_PRIMARY_MSKU]
            ],
        )
        FLASH_3P_DISCOUNT_OFFER.promo += [promo]
        cls.index.promos += [promo]
        cls.dynamic.qpromos += [DynamicQPromos([promo])]

    @classmethod
    def prepare_blue_bundle(cls):
        promo = Promo(
            promo_type=PromoType.BUNDLE,
            key=RECOM_BUNDLE_ID,
            bundle_offer_ids=[
                BundleOfferId(feed_id=RECOM_BUNDLE_SALE_OFFER.feedid, offer_id=RECOM_BUNDLE_SALE_OFFER.offerid),
                BundleOfferId(
                    feed_id=RECOM_BUNDLE_SECONDARY_OFFER.feedid, offer_id=RECOM_BUNDLE_SECONDARY_OFFER.offerid
                ),
            ],
            mskus=[PromoMSKU(msku=str(RECOM_BUNDLE_SALE_MSKU.sku))],
            source_promo_id=RECOM_BUNDLE_ID,
        )
        RECOM_BUNDLE_SALE_OFFER.promo += [promo]
        RECOM_BUNDLE_SECONDARY_OFFER.promo += [promo]
        cls.index.promos += [promo]

    def __apply_discount(self, discount, price):
        return int(price * (1 - discount / 100.0))

    def __calculate_discount_percent(self, price, price_old):
        return int(100.0 * (1.0 - float(price) / float(price_old)))

    def __get_offer_response_fragment(self, offer, discount=None, promo=None):
        return {
            'entity': 'offer',
            'wareId': offer.waremd5,
            'prices': {
                'currency': Currency.RUR,
                'value': str(self.__apply_discount(discount=discount, price=offer.price))
                if discount
                else str(offer.price),
                'discount': {'oldMin': str(offer.price), 'percent': discount} if discount else Absent(),
            },
            'promos': [promo] if promo else Absent(),
        }

    @staticmethod
    def __get_secret_sale_fragment(debug=False):
        fragment = SECRET_SALE_PROMO_RESPONSE_FRAGMENT
        fragment['secretSaleDetails'] = NotEmpty() if debug else Absent()
        return fragment

    def test_promo_secret_sale(self):
        """
        Проверяем, что MSKU, участвующие в закрытой распродаже, содержат
        информацию о ней на выдаче в place=prime и place=offerinfo
        """
        fragment_list = (
            (SECRET_SALE_OFFER, SECRET_SALE_DISCOUNT_PERCENT, T.__get_secret_sale_fragment()),
            (GENERIC_BUNDLE_PRIMARY_OFFER, SECRET_SALE_DISCOUNT_PERCENT, T.__get_secret_sale_fragment()),
            (GENERIC_BUNDLE_SECONDARY_OFFER, None, {'type': PromoType.GENERIC_BUNDLE_SECONDARY}),
            (PLAIN_OFFER, None, None),
            (FLASH_3P_DISCOUNT_OFFER, SECRET_SALE_DISCOUNT_PERCENT, T.__get_secret_sale_fragment()),
            (OLD_SECRET_SALE_OFFER, None, None),
            (RECOM_BUNDLE_SALE_OFFER, SECRET_SALE_DISCOUNT_PERCENT, T.__get_secret_sale_fragment()),
        )

        request_prime = PRIME_REQUEST_STRING.format(
            color='green_with_blue',
            rid=MSK_RID,
            category=CATEGORY_ID,
            waremd5_list=','.join([offer.waremd5 for offer, discount, promo in fragment_list]),
            promos=','.join([SECRET_SALE_ID, OLD_SECRET_SALE_ID]),
        )
        request_offerinfo = OFFERINFO_REQUEST_STRING.format(
            color='green_with_blue',
            rid=MSK_RID,
            category=CATEGORY_ID,
            waremd5_list=','.join([offer.waremd5 for offer, discount, promo in fragment_list]),
            promos=','.join([SECRET_SALE_ID, OLD_SECRET_SALE_ID]),
        )

        for offer, discount, promo in fragment_list:
            for req in (request_prime, request_offerinfo):
                response = self.report.request_json(req)
                self.assertFragmentIn(
                    response,
                    {'results': [self.__get_offer_response_fragment(offer=offer, discount=discount, promo=promo)]},
                )

    def test_promo_secret_sale_white(self):
        """
        Проверяем, закрытая распродажа работает на белом
        """
        fragment_list = (
            (SECRET_SALE_OFFER, SECRET_SALE_DISCOUNT_PERCENT, T.__get_secret_sale_fragment()),
            (PLAIN_OFFER, None, None),
            (OLD_SECRET_SALE_OFFER, None, None),
            (RECOM_BUNDLE_SALE_OFFER, SECRET_SALE_DISCOUNT_PERCENT, T.__get_secret_sale_fragment()),
        )

        # Запрос в place=prime Белого Маркета
        response = self.report.request_json(
            PRIME_REQUEST_STRING.format(
                color='green_with_blue',
                rid=MSK_RID,
                category=CATEGORY_ID,
                waremd5_list=','.join([offer.waremd5 for offer, discount, promo in fragment_list]),
                promos=','.join([SECRET_SALE_ID, OLD_SECRET_SALE_ID]),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    self.__get_offer_response_fragment(offer=offer, discount=discount, promo=promo)
                    for offer, discount, promo in fragment_list
                ]
            },
        )

    @skip('Не работает пересечение белых и синих промо')
    def test_promo_secret_sale_disabled(self):
        """
        Проверяем, что инфомарция о закрытой распродаже отсутствует в
        выдаче для случаев:
            - в запросе отсутствует CGI-параметр allowed-promos
        """
        fragment_list = (
            (SECRET_SALE_OFFER, None, None),
            (PLAIN_OFFER, None, None),
            (OLD_SECRET_SALE_OFFER, None, None),
            (RECOM_BUNDLE_SALE_OFFER, None, None),
        )

        # Запрос в place=prime Беру без параметра allowed-promos
        request = (
            'place={place}'
            '&rgb={color}'
            '&rids={rid}'
            '&hid={category}'
            '&offerid={waremd5_list}'
            '&pp=18'
            '&allow-collapsing=0'
            '&numdoc=100'
        )
        response = self.report.request_json(
            request.format(
                place='prime',
                color='blue',
                rid=MSK_RID,
                category=CATEGORY_ID,
                waremd5_list=','.join([offer.waremd5 for offer, discount, promo in fragment_list]),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    self.__get_offer_response_fragment(offer=offer, discount=discount, promo=promo)
                    for offer, discount, promo in fragment_list
                ]
            },
        )

    def test_promo_mutual_priority(self):
        """
        Проверяем, что соблюден взаимный приоритет акций:
            1. Закрытые распродажи - 'secret-sale'
            2. Подарки - 'generic-bundle', secret-sale не может применяться к подарку
            3. Все остальные (для примера взят тип 'blue-3p-flash-discount')
        """
        # Проверяем, что Закрытая распродажа приоритетнее, чем Подарки
        request = (
            'place={place}'
            '&rids={rids}'
            '&hid={category}'
            '&pp=18'
            '&rgb={color}'
            '&allow-collapsing=0'
            '&numdoc=100'
            '&allowed-promos={promo}'
        )
        response = self.report.request_json(
            request.format(place='prime', rids=MSK_RID, category=CATEGORY_ID, color='blue', promo=SECRET_SALE_ID)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': GENERIC_BUNDLE_PRIMARY_OFFER.waremd5,
                        'prices': {
                            'currency': Currency.RUR,
                            'value': str(
                                self.__apply_discount(
                                    discount=SECRET_SALE_DISCOUNT_PERCENT, price=GENERIC_BUNDLE_PRIMARY_OFFER.price
                                )
                            ),
                            'discount': {
                                'oldMin': str(GENERIC_BUNDLE_PRIMARY_OFFER.price),
                                'percent': SECRET_SALE_DISCOUNT_PERCENT,
                            },
                        },
                        'promos': [T.__get_secret_sale_fragment()],
                    },
                    {
                        'entity': 'offer',
                        'wareId': GENERIC_BUNDLE_SECONDARY_OFFER.waremd5,
                        'promos': [
                            {
                                'type': PromoType.GENERIC_BUNDLE_SECONDARY,
                            }
                        ],
                    },
                ]
            },
        )

        # Проверяем, что Подарки присутствуют на выдаче, если запрос без параметра allowed-promos
        request = (
            'place={place}' '&rids={rids}' '&hid={category}' '&pp=18' '&rgb={color}' '&allow-collapsing=0' '&numdoc=100'
        )
        response = self.report.request_json(
            request.format(place='prime', rids=MSK_RID, category=CATEGORY_ID, color='blue')
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': GENERIC_BUNDLE_PRIMARY_OFFER.waremd5,
                        'promos': [
                            {
                                'type': PromoType.GENERIC_BUNDLE,
                                'key': GENERIC_BUNDLE_ID,
                                'startDate': GENERIC_BUNDLE_START.strftime(DT_FORMAT),
                                'endDate': GENERIC_BUNDLE_END.strftime(DT_FORMAT),
                                'itemsInfo': {
                                    'additionalOffers': NotEmpty(),
                                },
                            }
                        ],
                    }
                ]
            },
        )

        # Проверяем, что Закрытая распродажа приоритетнее, чем Flash-скидки
        request = (
            'place={place}'
            '&rids={rids}'
            '&hid={category}'
            '&pp=18'
            '&rgb={color}'
            '&allow-collapsing=0'
            '&numdoc=100'
            '&allowed-promos={promo}'
        )
        response = self.report.request_json(
            request.format(place='prime', rids=MSK_RID, category=CATEGORY_ID, color='blue', promo=SECRET_SALE_ID)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': FLASH_3P_DISCOUNT_OFFER.waremd5,
                        'prices': {
                            'currency': Currency.RUR,
                            'value': str(
                                self.__apply_discount(
                                    discount=SECRET_SALE_DISCOUNT_PERCENT, price=FLASH_3P_DISCOUNT_OFFER.price
                                )
                            ),
                            'discount': {
                                'oldMin': str(FLASH_3P_DISCOUNT_OFFER.price),
                                'percent': SECRET_SALE_DISCOUNT_PERCENT,
                            },
                        },
                        'promos': [T.__get_secret_sale_fragment()],
                    }
                ]
            },
        )

        # Проверям, что Flash-скидки присутствуют на выдаче, если запрос без параметра allowed_promos
        request = (
            'place={place}' '&rids={rids}' '&hid={category}' '&pp=18' '&rgb={color}' '&allow-collapsing=0' '&numdoc=100'
        )
        response = self.report.request_json(
            request.format(place='prime', rids=MSK_RID, category=CATEGORY_ID, color='blue', promo=SECRET_SALE_ID)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': FLASH_3P_DISCOUNT_OFFER.waremd5,
                        'prices': {
                            'currency': Currency.RUR,
                            'value': str(FLASH_3P_DISCOUNT_PROMO_PRICE),
                            'discount': {
                                'oldMin': str(FLASH_3P_DISCOUNT_OLD_PRICE),
                                'percent': FLASH_3P_DISCOUNT_PERCENT,
                            },
                        },
                        'promos': [
                            {
                                'type': PromoType.BLUE_3P_FLASH_DISCOUNT,
                                'key': FLASH_3P_DISCOUNT_ID,
                                'startDate': FLASH_3P_DISCOUNT_START.strftime(DT_FORMAT),
                            }
                        ],
                    }
                ]
            },
        )

    def test_filter_match_blue_promo(self):
        """
        Проверяем работу фильтра по поисковому литералу 'match_blue_promo'
        """
        # Проверяем Закрытую распродажу
        request = (
            'place=prime'
            '&rgb={color}'
            '&rids={rid}'
            '&hid={category}'
            '&promoid={promos}'
            '&allowed-promos={promos}'
            '&pp=18'
            '&allow-collapsing=0'
            '&numdoc=100'
        )
        response = self.report.request_json(
            request.format(color='blue', rid=MSK_RID, category=CATEGORY_ID, promos=SECRET_SALE_ID)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': offer.waremd5, 'promos': [T.__get_secret_sale_fragment()]}
                    for offer in [
                        SECRET_SALE_OFFER,
                        GENERIC_BUNDLE_PRIMARY_OFFER,
                        DISCOUNT_SECRET_SALE_OFFER,
                        RECOM_BUNDLE_SALE_OFFER,
                    ]
                ]
            },
            allow_different_len=False,
        )

        # Проверяем фильтр для флеш-скидок
        request = (
            'place=prime'
            '&rgb={color}'
            '&rids={rid}'
            '&hid={category}'
            '&promoid={promos}'
            '&pp=18'
            '&allow-collapsing=0'
            '&numdoc=100'
        )
        response = self.report.request_json(
            request.format(color='blue', rid=MSK_RID, category=CATEGORY_ID, promos=FLASH_3P_DISCOUNT_ID)
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'offer', 'wareId': offer.waremd5, 'promos': FLASH_3P_DISCOUNT_PROMO_RESPONSE_FRAGMENT}
                    for offer in [FLASH_3P_DISCOUNT_OFFER]
                ]
            },
            allow_different_len=False,
        )

    def test_secret_sale_and_discount(self):
        """
        Проверям, что цена оффера пересчитывается и с учетом скидки, и с учетом условий закрытой распродажи
        """
        new_price = self.__apply_discount(discount=SECRET_SALE_DISCOUNT_PERCENT, price=DISCOUNT_SECRET_SALE_OFFER.price)
        fragment_list = [
            (DISCOUNT_OFFER, DISCOUNT_OFFER.price, DISCOUNT_OFFER.price_old, Absent()),
            (
                DISCOUNT_SECRET_SALE_OFFER,
                new_price,
                DISCOUNT_SECRET_SALE_OFFER.price_old,
                [T.__get_secret_sale_fragment()],
            ),
        ]
        response = self.report.request_json(
            PRIME_REQUEST_STRING.format(
                color='blue',
                rid=MSK_RID,
                category=CATEGORY_ID,
                waremd5_list=','.join([offer.waremd5 for offer, price, old_price, promos in fragment_list]),
                promos=SECRET_SALE_ID,
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'wareId': offer.waremd5,
                        'prices': {
                            'currency': Currency.RUR,
                            'value': str(price),
                            'discount': {
                                'oldMin': str(old_price),
                                'percent': self.__calculate_discount_percent(price=price, price_old=old_price),
                            },
                        },
                        'promos': promos,
                    }
                    for offer, price, old_price, promos in fragment_list
                ]
            },
        )

    def test_old_secret_sale(self):
        """
        Проверяем, что информация об уже прошедшей закрытой распродаже отсутсвует в выдаче
        """
        response = self.report.request_json(
            PRIME_REQUEST_STRING.format(
                color='blue',
                rid=MSK_RID,
                category=CATEGORY_ID,
                waremd5_list=','.join([OLD_SECRET_SALE_OFFER.waremd5, SECRET_SALE_OFFER.waremd5]),
                promos=OLD_SECRET_SALE_ID,
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    self.__get_offer_response_fragment(OLD_SECRET_SALE_OFFER),
                    self.__get_offer_response_fragment(SECRET_SALE_OFFER),
                ]
            },
        )

    def test_promo_equal_priority(self):
        """
        Проверяем, что порядок чтения акций равного приоритета не изменился после введения
        "динамической" приоритезации под экспериментальным флагом 'market_promo_secret_sale_enabled'

        В данном случае акции типа 'blue-3p-flash-discount' зачитываются по участвующим MSKU
        из yt_promo_detais.mmap раньше, чем 'bundle' из promo_details.mmap по конкретному офферу

        Также см. https://st.yandex-team.ru/MARKETINDEXER-31114
        """
        request = (
            'place=prime'
            '&rgb={color}'
            '&rids={rid}'
            '&hid={category}'
            '&offerid={waremd5_list}'
            '&pp=18'
            '&allow-collapsing=0'
            '&numdoc=100'
        )
        response_fragment = {
            'results': [
                {
                    'entity': 'offer',
                    'wareId': BUNDLE_PRIMARY_OFFER.waremd5,
                    'promos': [{'type': PromoType.BLUE_3P_FLASH_DISCOUNT}],
                },
                {'entity': 'offer', 'wareId': BUNDLE_SECONDARY_OFFER.waremd5, 'promos': Absent()},
            ]
        }
        response = self.report.request_json(
            request.format(
                color='blue',
                rid=MSK_RID,
                category=CATEGORY_ID,
                waremd5_list=','.join([BUNDLE_PRIMARY_OFFER.waremd5, BUNDLE_SECONDARY_OFFER.waremd5]),
            )
        )
        self.assertFragmentIn(response, response_fragment)

    def test_secret_sale_details_debug(self):
        """
        Проверяем, что описание закрытой распродажи отсутсвует в выдаче, если &debug=0
        """
        request = PRIME_REQUEST_STRING + '&debug={debug}'
        for param_value in [True, False]:
            response = self.report.request_json(
                request.format(
                    color='blue',
                    rid=MSK_RID,
                    category=CATEGORY_ID,
                    waremd5_list=SECRET_SALE_OFFER.waremd5,
                    promos=SECRET_SALE_ID,
                    debug=int(param_value),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': SECRET_SALE_OFFER.waremd5,
                            'promos': [T.__get_secret_sale_fragment(debug=param_value)],
                        }
                    ]
                },
            )

    @skip('Не работает пересечение белых и синих промо')
    def test_place_promo_bundle_disabled(self):
        """
        Проверяем, что на Синем выдача в place=promo_bundle пустая в случае, когда запрашиваемые Market SKU
        или модель участвуют в закрытой распродаже (текущие продуктовые требования таковы, что приоритет
        распродажи выше, чем у комплектов от рекомендаций)

        При этом валидация комплекта (параметр 'validate-bundle') не должна влиять на логику скрытия по приоритетам
        """
        request_prefix_list = [
            PROMO_BUNDLE_REQUEST_STRING.format(
                color='blue', rid=MSK_RID, promos=SECRET_SALE_ID, validate_bundle=validate
            )
            for validate in [0, 1]
        ]

        request_suffix_list = [
            '&market-sku={msku}'.format(msku=RECOM_BUNDLE_SALE_MSKU.sku),
            '&hyperid={model}'.format(model=MODEL_ID),
        ]

        for prefix in request_prefix_list:
            for suffix in request_suffix_list:
                request = prefix + suffix
                response = self.report.request_json(request)
                self.assertFragmentIn(response, {'results': EmptyList()})

    @skip('Не работает пересечение белых и синих промо')
    def test_place_promo_bundle(self):
        """
        Проверяем, что логика работы place=promo_bundle осталась прежней для случаев:
            1. Запрос на Белый Маркет
            2. Отсутсвует параметр 'allowed-promos'
        """
        # Запросы на Белый
        request_prefix_for_white = PROMO_BUNDLE_REQUEST_STRING.format(
            color='white', rid=MSK_RID, promos=SECRET_SALE_ID, validate_bundle=1
        )
        request_suffix_list = ['&hyperid={model}'.format(model=MODEL_ID)]
        for suffix in request_suffix_list:
            request = request_prefix_for_white + suffix
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'results': [PLACE_PROMO_BUNDLE_RESPONSE_FRAGMENT]})

        # Запросы на Синий
        request_prefix = 'place=promo_bundle' '&rgb={color}' '&pp=18' '&rids={rid}'
        # Параметр присутствует
        request_with_parameter = PROMO_BUNDLE_REQUEST_STRING.format(
            color='blue', rid=MSK_RID, promos=SECRET_SALE_ID, validate_bundle=1
        )

        request_suffix_list += ['&market-sku={msku}'.format(msku=RECOM_BUNDLE_SALE_MSKU.sku)]
        for suffix in request_suffix_list:
            request = request_with_parameter + suffix
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'results': EmptyList()})

        # Отсутсвует параметр 'allowed-promos'
        request_withut_parameter = request_prefix.format(color='blue', rid=MSK_RID)
        request_suffix_list += ['&market-sku={msku}'.format(msku=RECOM_BUNDLE_SALE_MSKU.sku)]
        for suffix in request_suffix_list:
            request = request_withut_parameter + suffix
            response = self.report.request_json(request)
            self.assertFragmentIn(response, {'results': [PLACE_PROMO_BUNDLE_RESPONSE_FRAGMENT]})


if __name__ == '__main__':
    main()
