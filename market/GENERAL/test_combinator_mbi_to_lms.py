#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from datetime import date, datetime, timedelta

from core.matcher import Absent
from core.report import REQUEST_TIMESTAMP
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
from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    DeliveryItem,
    DeliveryType,
    Destination,
    PickupPointGrouped,
    RoutePoint,
    RoutePath,
)
from core.types.delivery import OutletType
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService
from market.pylibrary.const.payment_methods import PaymentMethod


# Категории
DEFAULT_HID = 1

# Регионы и склады
DEFAULT_RIDS = 225
MSK_RIDS = 213
DEFAULT_WH_ID = 145
DELIVERY_SERVICE_ID = 1

# Магазины и поставщики
SUPPLIER_SHOP_ID = 1
SUPPLIER_FEED_ID = 1
VIRTUAL_SHOP_ID = 2
VIRTUAL_SHOP_FEED_ID = 2

# Запросы
ACTUAL_DELIVERY_REQUEST = (
    'place=actual_delivery&'
    'rgb=blue&'
    'rids={rids}&'
    'pickup-options=grouped&'
    'pickup-options-extended-grouping=1&'
    'offers-list={offers}&'
    'combinator={combinator_delivery}'
)

DELIVERY_ROUTE_REQUEST = (
    'place=delivery_route&'
    'rgb=blue&'
    'rids={rids}&'
    'offers-list={offers}&'
    'delivery-type=pickup&'
    'point_id={point_id}&'
    'combinator={combinator_delivery}'
)


# ПВЗ
def __create_outlet(outlet_id, outlet_type, mbi_alias_id=None, post_code=None):
    return Outlet(
        point_id=outlet_id,
        mbi_alias_point_id=mbi_alias_id,
        delivery_service_id=DELIVERY_SERVICE_ID,
        point_type=outlet_type,
        post_code=post_code,
        region=MSK_RIDS,
        working_days=list(range(10)),
        bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed'],
        delivery_option=OutletDeliveryOption(shipper_id=DELIVERY_SERVICE_ID, day_from=1, day_to=5, price=100),
    )


LMS_OUTLET_ID_BASE = 10**10
ALIAS_OUTLET_ID_BASE = 10

MBI_ONLY_PICKUP_OUTLET = __create_outlet(outlet_id=1, outlet_type=Outlet.FOR_PICKUP)

PICKUP_OUTLET = __create_outlet(
    outlet_id=LMS_OUTLET_ID_BASE + 1, mbi_alias_id=ALIAS_OUTLET_ID_BASE + 1, outlet_type=Outlet.FOR_PICKUP
)

POST_OUTLET = __create_outlet(
    outlet_id=LMS_OUTLET_ID_BASE + 2,
    mbi_alias_id=ALIAS_OUTLET_ID_BASE + 2,
    outlet_type=Outlet.FOR_POST,
    post_code=115201,
)

DELIVERY_SERVICE_OUTLETS = [MBI_ONLY_PICKUP_OUTLET, PICKUP_OUTLET, POST_OUTLET]


# Точки в логистическом графе Комбинатора
def __service_time(hour, minute):
    return datetime(year=2020, month=8, day=2, hour=hour, minute=minute)


WAREHOUSE_SEGMENT = RoutePoint(
    point_ids=Destination(partner_id=DEFAULT_WH_ID),
    segment_id=512001,
    segment_type='warehouse',
    services=[
        (DeliveryService.INTERNAL, "PROCESSING", __service_time(13, 25), timedelta(hours=2)),
        (DeliveryService.OUTBOUND, "SHIPMENT", __service_time(15, 25), timedelta(minutes=35)),
    ],
)

MOVEMENT_SEGMENT = RoutePoint(
    point_ids=Destination(partner_id=DELIVERY_SERVICE_ID),
    segment_id=512002,
    segment_type='movement',
    services=[
        (DeliveryService.INTERNAL, "INBOUND", __service_time(16, 15), timedelta(minutes=40)),
        (DeliveryService.INTERNAL, "MOVEMENT", __service_time(17, 5), timedelta(minutes=30)),
    ],
)

LINEHAUL_SEGMENT = RoutePoint(
    point_ids=Destination(partner_id=DELIVERY_SERVICE_ID),
    segment_id=512003,
    segment_type='linehaul',
    services=[
        (DeliveryService.INTERNAL, "DELIVERY", __service_time(17, 35), timedelta(minutes=55)),
        (DeliveryService.INTERNAL, "LAST_MILE", __service_time(18, 30), timedelta(minutes=30)),
    ],
)

PICKUP_SEGMENT = RoutePoint(
    point_ids=Destination(logistic_point_id=PICKUP_OUTLET.point_id),
    segment_id=512004,
    segment_type='pickup',
    services=[(DeliveryService.OUTBOUND, "HANDING", __service_time(19, 15), timedelta(minutes=15))],
)


# Опции от Калькулятора Доставки
def __create_pickup_bucket(bucket_id, dc_bucket_id, outlets):
    return PickupBucket(
        bucket_id=bucket_id,
        dc_bucket_id=dc_bucket_id,
        fesh=SUPPLIER_SHOP_ID,
        carriers=[DELIVERY_SERVICE_ID],
        options=[PickupOption(outlet_id=i) for i in outlets],
    )


DC_BUCKET_ID_BASE = 100

PICKUP_BUCKET = __create_pickup_bucket(
    bucket_id=1, dc_bucket_id=DC_BUCKET_ID_BASE + 1, outlets=[MBI_ONLY_PICKUP_OUTLET.point_id, PICKUP_OUTLET.point_id]
)

POST_BUCKET = __create_pickup_bucket(bucket_id=2, dc_bucket_id=DC_BUCKET_ID_BASE + 2, outlets=[POST_OUTLET.point_id])

# Опции от Комбинатора
DT_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)
COMBINATOR_DAY_FROM = 1
COMBINATOR_DAY_TO = 2
COMBINATOR_DATE_FROM = DT_NOW + timedelta(days=COMBINATOR_DAY_FROM)
COMBINATOR_DATE_TO = DT_NOW + timedelta(days=COMBINATOR_DAY_TO)
COMBINATOR_DELIVERY_COST = 99

# Синие офферы и Market SKU
WAREMD5_TEMPLATE = 'MarketSku{msku}_ModelId{model}w'
SHOP_SKU_TEMPLATE = 'Shop1_sku{msku}'


def __get_model_id(msku):
    return DEFAULT_HID * 100 + msku


def __create_blue_offer(msku, pickup_buckets, post_buckets):
    model_id = __get_model_id(msku)
    blue_offer = BlueOffer(
        price=150,
        offerid=SHOP_SKU_TEMPLATE.format(msku=msku),
        waremd5=WAREMD5_TEMPLATE.format(msku=msku, model=model_id),
        feedid=SUPPLIER_FEED_ID,
        weight=5,
        dimensions=OfferDimensions(length=10, width=20, height=30),
        cargo_types=[200],
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
    msku=1, pickup_buckets=[PICKUP_BUCKET.bucket_id], post_buckets=[POST_BUCKET.bucket_id]
)


class T(TestCase):
    """
    Набор тестов на корректную работу с отображением идентификаторов ПВЗ в нумерациях LMS и MBI
    в контексте работы с Комбинатором

    Комбинатор ничего не знает о "старых" точках в нумерации MBI, поэтому для того, чтобы убрать
    зависимость между проектами по запуску Комбинатора и "ПВЗ через LMS", используем преобразование
    идентификторов в place=actual_delivery и place=delivery_route Репорта

    См. https://st.yandex-team.ru/COMBINATOR-526
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
    def beforePrepare(cls):
        cls.settings.delivery_calendar_start_date = date(day=18, month=5, year=2020)

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
            T.__create_delivery_service(id=DELIVERY_SERVICE_ID, name="DefaultDeliveryService"),
            T.__create_warehouse_delivery_service_link(
                warehouse_id=DEFAULT_WH_ID, delivery_service_id=DELIVERY_SERVICE_ID
            ),
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

    @classmethod
    def prepare_buckets(cls):
        cls.index.pickup_buckets += [PICKUP_BUCKET, POST_BUCKET]

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

        cls.combinator.on_pickup_points_grouped_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=5000,
                    dimensions=[10, 20, 30],
                    cargo_types=[200],
                    offers=[
                        CombinatorOffer(
                            shop_sku=ORDINARY_OFFER.offerid,
                            shop_id=SUPPLIER_FEED_ID,
                            partner_id=DEFAULT_WH_ID,
                            available_count=1,
                        )
                    ],
                    price=ORDINARY_OFFER.price,
                ),
            ],
            destination_regions=[MSK_RIDS],
            point_types=[],
            total_price=ORDINARY_OFFER.price,
        ).respond_with_grouped_pickup_points(
            groups=[
                PickupPointGrouped(
                    ids_list=[PICKUP_OUTLET.point_id],
                    outlet_type=OutletType.FOR_PICKUP,
                    service_id=DELIVERY_SERVICE_ID,
                    cost=COMBINATOR_DELIVERY_COST,
                    date_from=COMBINATOR_DATE_FROM,
                    date_to=COMBINATOR_DATE_TO,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
                PickupPointGrouped(
                    ids_list=[POST_OUTLET.point_id],
                    post_ids=[POST_OUTLET.post_code],
                    outlet_type=OutletType.FOR_POST,
                    service_id=DELIVERY_SERVICE_ID,
                    cost=COMBINATOR_DELIVERY_COST,
                    date_from=COMBINATOR_DATE_FROM,
                    date_to=COMBINATOR_DATE_TO,
                    payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                ),
            ]
        )

        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.PICKUP,
            destination=PICKUP_SEGMENT,
            delivery_option=create_delivery_option(),
            total_price=ORDINARY_OFFER.price,
        ).respond_with_delivery_route(
            offers=[
                CombinatorOffer(
                    shop_sku=ORDINARY_OFFER.offerid,
                    shop_id=SUPPLIER_FEED_ID,
                    partner_id=DEFAULT_WH_ID,
                    available_count=1,
                )
            ],
            points=[WAREHOUSE_SEGMENT, MOVEMENT_SEGMENT, LINEHAUL_SEGMENT, PICKUP_SEGMENT],
            paths=[RoutePath(point_from=i, point_to=i + 1) for i in range(0, 3)],
            date_from=COMBINATOR_DATE_FROM,
            date_to=COMBINATOR_DATE_TO,
        )

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [ORDINARY_MSKU]

    def test_actual_delivery(self):
        """
        Проверяем, что в place=actual_delivery опции доставки самовывозом и почтой,
        вычисленные Комбинатором, присутствуют на выдаче Репорта с идентификаторами
        в нумерации MBI при отсутсвующем или выставленном в 0 rearr-флаге
        'market_use_lms_outlets' и выключенном флаги почты как ПВЗ

        Также проверяем, что при значении rearr-флага равном 1 на выдаче присутсвуют
        идентификаторы в нумерации LMS
        """
        request_params = [
            ('', True),
            ('&rearr-factors=market_use_lms_outlets=0', False),
            ('&rearr-factors=market_use_lms_outlets=1', True),
        ]
        for rearr, flag_value in request_params:
            request = ACTUAL_DELIVERY_REQUEST + rearr + '&rearr-factors=market_use_post_as_pickup=0'
            response = self.report.request_json(
                request.format(rids=MSK_RIDS, offers='{}:1'.format(ORDINARY_OFFER.waremd5), combinator_delivery=1)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': {
                                'pickupOptions': [
                                    {
                                        'serviceId': DELIVERY_SERVICE_ID,
                                        'dayFrom': COMBINATOR_DAY_FROM,
                                        'dayTo': COMBINATOR_DAY_TO,
                                        'outletIds': [
                                            PICKUP_OUTLET.point_id if flag_value else PICKUP_OUTLET.mbi_alias_point_id
                                        ],
                                    }
                                ],
                                'postOptions': [
                                    {
                                        'serviceId': DELIVERY_SERVICE_ID,
                                        'dayFrom': COMBINATOR_DAY_FROM,
                                        'dayTo': COMBINATOR_DAY_TO,
                                        'outletIds': [
                                            POST_OUTLET.point_id if flag_value else POST_OUTLET.mbi_alias_point_id
                                        ],
                                        'postCodes': [POST_OUTLET.post_code],
                                    }
                                ],
                            },
                        }
                    ]
                },
            )

    def test_actual_delivery_post_as_pickup(self):
        """
        Проверяем, что в place=actual_delivery при использовании почты как ПВЗ
        опции доставки самовывозом и почтой, вычисленные Комбинатором, присутствуют
        на выдаче Репорта с идентификаторами в нумерации MBI при отсутсвующем или
        выставленном в 0 rearr-флаге 'market_use_lms_outlets'

        Также проверяем, что при значении rearr-флага равном 1 на выдаче присутсвуют
        идентификаторы в нумерации LMS
        """
        request_params = [
            ('', True),
            ('&rearr-factors=market_use_lms_outlets=0', False),
            ('&rearr-factors=market_use_lms_outlets=1', True),
        ]
        for rearr, flag_value in request_params:
            request = ACTUAL_DELIVERY_REQUEST + rearr
            response = self.report.request_json(
                request.format(rids=MSK_RIDS, offers='{}:1'.format(ORDINARY_OFFER.waremd5), combinator_delivery=1)
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'delivery': {
                                'pickupOptions': [
                                    {
                                        'serviceId': DELIVERY_SERVICE_ID,
                                        'dayFrom': COMBINATOR_DAY_FROM,
                                        'dayTo': COMBINATOR_DAY_TO,
                                        'outletIds': [
                                            PICKUP_OUTLET.point_id if flag_value else PICKUP_OUTLET.mbi_alias_point_id
                                        ],
                                    },
                                    {
                                        'serviceId': DELIVERY_SERVICE_ID,
                                        'dayFrom': COMBINATOR_DAY_FROM,
                                        'dayTo': COMBINATOR_DAY_TO,
                                        'outletIds': [
                                            POST_OUTLET.point_id if flag_value else POST_OUTLET.mbi_alias_point_id
                                        ],
                                    },
                                ],
                            },
                        }
                    ]
                },
            )

    def test_delivery_route(self):
        """
        Проверяем, что запрос в place=delivery_route за опциями самовывоза
        безусловно оборачивается в преобразование mbi_id -> lms_id, а ответ
        отдается "как есть" - с точками в нумерации LMS
        """
        request_params = ['', '&rearr-factors=market_use_lms_outlets=0', '&rearr-factors=market_use_lms_outlets=1']
        for rearr in request_params:
            request = DELIVERY_ROUTE_REQUEST + rearr
            response = self.report.request_json(
                request.format(
                    rids=MSK_RIDS,
                    offers='{}:1'.format(ORDINARY_OFFER.waremd5),
                    point_id=PICKUP_OUTLET.mbi_alias_point_id,
                    combinator_delivery=1,
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'deliveryGroup',
                            'offers': [{'entity': 'offer', 'wareId': ORDINARY_OFFER.waremd5}],
                            'delivery': {
                                'route': {
                                    'route': {
                                        'paths': [{'point_from': i, 'point_to': i + 1} for i in range(0, 2)],
                                        'points': [
                                            {
                                                'ids': {'partner_id': p.point_ids.partner_id}
                                                if p.point_ids.partner_id is not None
                                                else {'logistic_point_id': p.point_ids.logistic_point_id},
                                                'segment_id': p.segment_id,
                                                'segment_type': p.segment_type,
                                            }
                                            for p in [WAREHOUSE_SEGMENT, LINEHAUL_SEGMENT, PICKUP_SEGMENT]
                                        ],
                                        'tariff_id': 100147,
                                        'cost': 50,
                                        'cost_for_shop': 50,
                                        'delivery_type': DeliveryType.to_string(DeliveryType.PICKUP),
                                        'date_from': {
                                            'year': COMBINATOR_DATE_FROM.year,
                                            'month': COMBINATOR_DATE_FROM.month,
                                            'day': COMBINATOR_DATE_FROM.day,
                                        },
                                        'date_to': {
                                            'year': COMBINATOR_DATE_TO.year,
                                            'month': COMBINATOR_DATE_TO.month,
                                            'day': COMBINATOR_DATE_TO.day,
                                        },
                                    },
                                },
                                'option': {
                                    'price': {'currency': 'RUR', 'value': str(COMBINATOR_DELIVERY_COST)},
                                    'dayFrom': COMBINATOR_DAY_FROM,
                                    'dayTo': COMBINATOR_DAY_TO,
                                    'tariffId': 100147,
                                    'outletIds': Absent(),
                                },
                            },
                        }
                    ]
                },
            )


if __name__ == '__main__':
    main()
