#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    MarketSku,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    Tax,
    TimeInfo,
)

# Категории
DEFAULT_HID = 1

# Регионы и склады
DEFAULT_RIDS = 225
MSK_RIDS = 213
DEFAULT_WH_ID = 145

# Магазины и поставщики
SUPPLIER_SHOP_ID = 1
SUPPLIER_FEED_ID = 1
VIRTUAL_SHOP_ID = 2
VIRTUAL_SHOP_FEED_ID = 2

# Службы доставки
SL_SERVICE_ID = 1
DPD_SERVICE_ID = 2


# ПВЗ
def __create_outlet(outlet_id, outlet_type, mbi_alias_id=None, delivery_service_id=None, fesh=None):
    return Outlet(
        point_id=outlet_id,
        mbi_alias_point_id=mbi_alias_id,
        fesh=fesh,
        delivery_service_id=delivery_service_id if fesh is None else None,
        point_type=outlet_type,
        region=MSK_RIDS,
        working_days=list(range(10)),
        bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed'],
        delivery_option=OutletDeliveryOption(shipper_id=delivery_service_id, day_from=1, day_to=5, price=100),
    )


LMS_OUTLET_ID_BASE = 10000000000

SL_MBI_PICKUP_OUTLET = __create_outlet(outlet_id=1, outlet_type=Outlet.FOR_PICKUP, delivery_service_id=SL_SERVICE_ID)
SL_MBI_POST_OUTLET = __create_outlet(outlet_id=2, outlet_type=Outlet.FOR_POST, delivery_service_id=SL_SERVICE_ID)
DPD_MBI_PICKUP_OUTLET = __create_outlet(outlet_id=3, outlet_type=Outlet.FOR_PICKUP, delivery_service_id=DPD_SERVICE_ID)
DPD_MBI_POST_OUTLET = __create_outlet(outlet_id=4, outlet_type=Outlet.FOR_POST, delivery_service_id=DPD_SERVICE_ID)
SHOP_MBI_OUTLET = __create_outlet(outlet_id=5, outlet_type=Outlet.FOR_STORE, fesh=VIRTUAL_SHOP_ID)

SL_MBI_ALIAS_PICKUP_OUTLET_A = __create_outlet(
    outlet_id=6, outlet_type=Outlet.FOR_PICKUP, delivery_service_id=SL_SERVICE_ID
)
SL_LMS_PICKUP_OUTLET_A = __create_outlet(
    outlet_id=LMS_OUTLET_ID_BASE + 1,
    mbi_alias_id=SL_MBI_ALIAS_PICKUP_OUTLET_A.point_id,
    outlet_type=Outlet.FOR_PICKUP,
    delivery_service_id=SL_SERVICE_ID,
)

SL_MBI_ALIAS_PICKUP_OUTLET_B_ID = 7
SL_LMS_PICKUP_OUTLET_B = __create_outlet(
    outlet_id=LMS_OUTLET_ID_BASE + 2,
    mbi_alias_id=SL_MBI_ALIAS_PICKUP_OUTLET_B_ID,
    outlet_type=Outlet.FOR_PICKUP,
    delivery_service_id=SL_SERVICE_ID,
)

SL_MBI_ALIAS_POST_OUTLET_A = __create_outlet(
    outlet_id=8, outlet_type=Outlet.FOR_POST, delivery_service_id=SL_SERVICE_ID
)
SL_LMS_POST_OUTLET_A = __create_outlet(
    outlet_id=LMS_OUTLET_ID_BASE + 3,
    mbi_alias_id=SL_MBI_ALIAS_POST_OUTLET_A.point_id,
    outlet_type=Outlet.FOR_POST,
    delivery_service_id=SL_SERVICE_ID,
)

SL_MBI_ALIAS_POST_OUTLET_B_ID = 9
SL_LMS_POST_OUTLET_B = __create_outlet(
    outlet_id=LMS_OUTLET_ID_BASE + 4,
    mbi_alias_id=SL_MBI_ALIAS_POST_OUTLET_B_ID,
    outlet_type=Outlet.FOR_POST,
    delivery_service_id=SL_SERVICE_ID,
)

DELIVERY_SERVICE_OUTLETS = [
    SL_MBI_PICKUP_OUTLET,
    SL_MBI_POST_OUTLET,
    DPD_MBI_PICKUP_OUTLET,
    DPD_MBI_POST_OUTLET,
    SL_MBI_ALIAS_PICKUP_OUTLET_A,
    SL_LMS_PICKUP_OUTLET_A,
    SL_LMS_PICKUP_OUTLET_B,
    SL_MBI_ALIAS_POST_OUTLET_A,
    SL_LMS_POST_OUTLET_A,
    SL_LMS_POST_OUTLET_B,
]


# Опции доставки
def __create_pickup_bucket(bucket_id, delivery_service_id, outlets):
    return PickupBucket(
        bucket_id=bucket_id,
        dc_bucket_id=bucket_id,
        fesh=SUPPLIER_SHOP_ID,
        carriers=[delivery_service_id],
        options=[PickupOption(outlet_id=i) for i in outlets],
    )


SL_MBI_PICKUP_BUCKET = __create_pickup_bucket(
    bucket_id=1, delivery_service_id=SL_SERVICE_ID, outlets=[SL_MBI_PICKUP_OUTLET.point_id]
)
SL_MBI_POST_BUCKET = __create_pickup_bucket(
    bucket_id=2, delivery_service_id=SL_SERVICE_ID, outlets=[SL_MBI_POST_OUTLET.point_id]
)
SL_LMS_PICKUP_BUCKET = __create_pickup_bucket(
    bucket_id=3,
    delivery_service_id=SL_SERVICE_ID,
    outlets=[SL_LMS_PICKUP_OUTLET_A.point_id, SL_LMS_PICKUP_OUTLET_B.point_id],
)
SL_LMS_POST_BUCKET = __create_pickup_bucket(
    bucket_id=4,
    delivery_service_id=SL_SERVICE_ID,
    outlets=[SL_LMS_POST_OUTLET_A.point_id, SL_LMS_POST_OUTLET_B.point_id],
)
DPD_MBI_PICKUP_BUCKET = __create_pickup_bucket(
    bucket_id=5, delivery_service_id=DPD_SERVICE_ID, outlets=[DPD_MBI_PICKUP_OUTLET.point_id]
)
DPD_MBI_POST_BUCKET = __create_pickup_bucket(
    bucket_id=6, delivery_service_id=DPD_SERVICE_ID, outlets=[DPD_MBI_POST_OUTLET.point_id]
)

# Синие офферы и Market SKU
WAREMD5_TEMPLATE = 'MarketSku{msku}_ModelId{model}w'
SHOP_SKU_TEMPLATE = 'Shop1_sku{msku}'


def __get_model_id(msku):
    return DEFAULT_HID * 100 + msku


def __create_blue_offer(msku, pickup_buckets, post_buckets):
    model_id = __get_model_id(msku)
    blue_offer = BlueOffer(
        offerid=SHOP_SKU_TEMPLATE.format(msku=msku),
        waremd5=WAREMD5_TEMPLATE.format(msku=msku, model=model_id),
        feedid=SUPPLIER_FEED_ID,
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    msku = MarketSku(
        hid=DEFAULT_HID,
        hyperid=model_id,
        sku=str(msku),
        blue_offers=[blue_offer],
        pickup_buckets=pickup_buckets,
        post_buckets=post_buckets,
    )
    return msku, blue_offer


ORDINARY_MSKU, ORDINARY_OFFER = __create_blue_offer(
    msku=1,
    pickup_buckets=[SL_MBI_PICKUP_BUCKET.bucket_id, SL_LMS_PICKUP_BUCKET.bucket_id, DPD_MBI_PICKUP_BUCKET.bucket_id],
    post_buckets=[SL_MBI_POST_BUCKET.bucket_id, SL_LMS_POST_BUCKET.bucket_id, DPD_MBI_POST_BUCKET.bucket_id],
)

LMS_ONLY_MSKU, LMS_ONLY_OFFER = __create_blue_offer(
    msku=2, pickup_buckets=[SL_LMS_PICKUP_BUCKET.bucket_id], post_buckets=[SL_LMS_POST_BUCKET.bucket_id]
)


class T(TestCase):
    """
    Набор тестов на корректную работу с отображением идентификаторов ПВЗ из нумерации LMS в нумерацию MBI

    Данное отображение используется для постепенного перевода синего Report на работу с ПВЗ от LMS
    (служба за службой) под экспериментальным флагом 'market_use_lms_outlets'

    См. https://st.yandex-team.ru/MARKETOUT-30653
    """

    @staticmethod
    def __create_delivery_service(id, name):
        return DynamicDeliveryServiceInfo(
            id=id,
            name=name,
            region_to_region_info=[
                DeliveryServiceRegionToRegionInfo(region_from=MSK_RIDS, region_to=DEFAULT_RIDS, days_key=1)
            ],
        )

    @staticmethod
    def __create_warehouse_delivery_service_link(warehouse_id, delivery_service_id):
        return DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=warehouse_id,
            delivery_service_id=delivery_service_id,
            operation_time=0,
            date_switch_time_infos=[
                DateSwitchTimeAndRegionInfo(
                    date_switch_hour=2,
                    region_to=DEFAULT_RIDS,
                    date_switch_time=TimeInfo(19, 0),
                    packaging_time=TimeInfo(3, 30),
                )
            ],
        )

    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[]),
            DynamicWarehouseInfo(id=DEFAULT_WH_ID, home_region=MSK_RIDS),
            DynamicWarehouseToWarehouseInfo(warehouse_from=DEFAULT_WH_ID, warehouse_to=DEFAULT_WH_ID),
            T.__create_delivery_service(id=SL_SERVICE_ID, name="SberLogistics"),
            T.__create_delivery_service(id=DPD_SERVICE_ID, name="DPD"),
            T.__create_warehouse_delivery_service_link(warehouse_id=DEFAULT_WH_ID, delivery_service_id=SL_SERVICE_ID),
            T.__create_warehouse_delivery_service_link(warehouse_id=DEFAULT_WH_ID, delivery_service_id=DPD_SERVICE_ID),
        ]

    @classmethod
    def prepare_shops_and_outlets(cls):
        cls.index.shops += [
            Shop(
                fesh=SUPPLIER_SHOP_ID,
                datafeed_id=SUPPLIER_FEED_ID,
                priority_region=MSK_RIDS,
                regions=[DEFAULT_RIDS],
                warehouse_id=DEFAULT_WH_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                fulfillment_program=True,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                priority_region=MSK_RIDS,
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[outlet.point_id for outlet in DELIVERY_SERVICE_OUTLETS],
            ),
        ]
        cls.index.outlets += DELIVERY_SERVICE_OUTLETS
        cls.index.outlets += [SHOP_MBI_OUTLET]

    @classmethod
    def prepare_pickup_buckets(cls):
        cls.index.pickup_buckets += [
            SL_MBI_PICKUP_BUCKET,
            SL_MBI_POST_BUCKET,
            SL_LMS_PICKUP_BUCKET,
            SL_LMS_POST_BUCKET,
            DPD_MBI_PICKUP_BUCKET,
            DPD_MBI_POST_BUCKET,
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [ORDINARY_MSKU, LMS_ONLY_MSKU]

    @staticmethod
    def __get_post_options_fragment(use_lms_outlets):
        return [
            {
                'serviceId': SL_SERVICE_ID,
                'outletIds': [
                    SL_MBI_POST_OUTLET.point_id,
                    SL_LMS_POST_OUTLET_A.point_id if use_lms_outlets else SL_MBI_ALIAS_POST_OUTLET_A.point_id,
                    SL_LMS_POST_OUTLET_B.point_id if use_lms_outlets else SL_MBI_ALIAS_POST_OUTLET_B_ID,
                ],
            },
            {
                'serviceId': DPD_SERVICE_ID,
                'outletIds': [
                    DPD_MBI_POST_OUTLET.point_id,
                ],
            },
        ]

    @staticmethod
    def __get_pickup_options_fragment(use_lms_outlets):
        return [
            {
                'serviceId': SL_SERVICE_ID,
                'outletIds': [
                    SL_MBI_PICKUP_OUTLET.point_id,
                    SL_LMS_PICKUP_OUTLET_A.point_id if use_lms_outlets else SL_MBI_ALIAS_PICKUP_OUTLET_A.point_id,
                    SL_LMS_PICKUP_OUTLET_B.point_id if use_lms_outlets else SL_MBI_ALIAS_PICKUP_OUTLET_B_ID,
                ],
            },
            {'serviceId': DPD_SERVICE_ID, 'outletIds': [DPD_MBI_PICKUP_OUTLET.point_id]},
        ]

    def test_place_actual_delivery(self):
        """
        Проверям в 'place=actual_delivery' на Синем и Белом коллекции:
            - 'pickupOptions' и 'postOptions' в группе доставки ('entity': 'deliveryGroup')
            - 'pickupOptions' в оффере
        """
        request_params = [
            ('', True),
            ('&rearr-factors=market_use_lms_outlets=0', False),
            ('&rearr-factors=market_use_lms_outlets=1', True),
        ]
        request = (
            'place=actual_delivery&'
            'pp=18&'
            'rgb={color}&'
            'rids={rids}&'
            'pickup-options-extended-grouping=1&'
            'pickup-options=grouped&debug=1&'
            'offers-list={waremd5_list}&'
            'rearr-factors=market_use_post_as_pickup=0&'
            'combinator=0&'
        )

        for rearr_factors, use_lms_outlets in request_params:
            for color in ['blue', 'white']:
                response = self.report.request_json(
                    request.format(
                        color=color, rids=MSK_RIDS, waremd5_list='{waremd5}:1'.format(waremd5=ORDINARY_OFFER.waremd5)
                    )
                    + rearr_factors
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'deliveryGroup',
                                'delivery': {
                                    'pickupOptions': T.__get_pickup_options_fragment(use_lms_outlets=use_lms_outlets),
                                    'postOptions': T.__get_post_options_fragment(use_lms_outlets=use_lms_outlets),
                                },
                            }
                        ]
                    },
                    allow_different_len=False,
                )

    def test_place_actual_delivery_use_post_as_pickup(self):
        """
        Проверям в 'place=actual_delivery' на Синем и Белом коллекции с почтой как ПВЗ:
            - 'pickupOptions'  вгруппе доставки ('entity': 'deliveryGroup')
            - 'pickupOptions' в оффере
        """
        request_params = [
            ('', True),
            ('&rearr-factors=market_use_lms_outlets=0', False),
            ('&rearr-factors=market_use_lms_outlets=1', True),
        ]
        request = (
            'place=actual_delivery&'
            'pp=18&'
            'rgb={color}&'
            'rids={rids}&'
            'pickup-options-extended-grouping=1&'
            'pickup-options=grouped&debug=1&'
            'offers-list={waremd5_list}&'
            'combinator=0&'
        )

        for rearr_factors, use_lms_outlets in request_params:
            for color in ['blue', 'white']:
                response = self.report.request_json(
                    request.format(
                        color=color, rids=MSK_RIDS, waremd5_list='{waremd5}:1'.format(waremd5=ORDINARY_OFFER.waremd5)
                    )
                    + rearr_factors
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'deliveryGroup',
                                'delivery': {
                                    'pickupOptions': T.__get_pickup_options_fragment(use_lms_outlets=use_lms_outlets)
                                    + T.__get_post_options_fragment(use_lms_outlets=use_lms_outlets),
                                },
                            }
                        ]
                    },
                    allow_different_len=False,
                )

    @staticmethod
    def __get_pickup_outlets(use_lms_outlets):
        return [
            DPD_MBI_PICKUP_OUTLET.point_id,
            SL_MBI_PICKUP_OUTLET.point_id,
            SL_LMS_PICKUP_OUTLET_A.point_id if use_lms_outlets else SL_MBI_ALIAS_PICKUP_OUTLET_A.point_id,
            SL_LMS_PICKUP_OUTLET_B.point_id if use_lms_outlets else SL_MBI_ALIAS_PICKUP_OUTLET_B_ID,
        ]

    def test_place_geo(self):
        """
        Проверяем в 'place=geo' на Синем и Белом коллекции:
            - 'bundled' и 'outlet' в оффере
        """
        request_params = [
            ('', True),
            ('&rearr-factors=market_use_lms_outlets=0', False),
            ('&rearr-factors=market_use_lms_outlets=1', True),
        ]
        request = (
            'place=geo&'
            'pp=18&'
            'rgb={color}&'
            'rids={rids}&'
            'hyperid={model}&'
            'rearr-factors=market_nordstream_relevance=0'
        )

        for rearr_factors, use_lms_outlets in request_params:
            for color in ['blue', 'white']:
                response = self.report.request_json(
                    request.format(color=color, rids=MSK_RIDS, model=ORDINARY_MSKU.hyperid) + rearr_factors
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': ORDINARY_OFFER.waremd5,
                                'bundled': {'outletId': outlet},
                                'outlet': {'entity': 'outlet', 'id': str(outlet)},
                            }
                            for outlet in T.__get_pickup_outlets(use_lms_outlets)
                        ]
                    },
                    allow_different_len=False,
                )

    @staticmethod
    def __get_outlet_for_prime(use_lms_outlets):
        return SL_LMS_PICKUP_OUTLET_A if use_lms_outlets else SL_MBI_ALIAS_PICKUP_OUTLET_A

    def test_place_prime(self):
        """
        Проверяем в 'place=prime' на Синем и Белом коллекции:
            - 'outlet' в оффере

        Чтобы на выдаче гарантировано присутсвовали ПВЗ от LMS используем для теста 'LMS_ONLY_OFFER',
        среди бакетов с опциями доставки для которого есть только бакеты с ПВЗ в нумерации LMS
        """
        request_params = [
            ('', True),
            ('&rearr-factors=market_use_lms_outlets=0', False),
            ('&rearr-factors=market_use_lms_outlets=1', True),
        ]
        request = (
            'place=prime&'
            'pp=18&'
            'rgb={color}&'
            'rids={rids}&'
            'hyperid={model}&'
            'allow-collapsing=0&'
            'rearr-factors=market_nordstream_relevance=0'
        )

        for rearr_factors, use_lms_outlets in request_params:
            for color in ['blue', 'white']:
                response = self.report.request_json(
                    request.format(color=color, rids=MSK_RIDS, model=LMS_ONLY_MSKU.hyperid) + rearr_factors
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            {
                                'entity': 'offer',
                                'wareId': LMS_ONLY_OFFER.waremd5,
                                'outlet': {
                                    'entity': 'outlet',
                                    'id': str(T.__get_outlet_for_prime(use_lms_outlets).point_id),
                                },
                            }
                        ]
                    },
                )


if __name__ == '__main__':
    main()
