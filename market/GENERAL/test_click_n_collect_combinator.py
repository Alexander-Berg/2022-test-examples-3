#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from datetime import datetime, timedelta

from core.combinator import DeliveryStats, make_offer_id
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryCalcFeedInfo,
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
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.types.combinator import CombinatorOffer, DeliveryItem, PickupPointGrouped
from core.types.delivery import OutletType
from market.pylibrary.const.payment_methods import PaymentMethod

# Категории
DEFAULT_HID = 1

# Регионы и склады
DEFAULT_RIDS = 225
MSK_RIDS = 213

CNC_WH_ID = 2
DEFAULT_WH_ID = 145

# Идентификаторы магазинов и поставщиков
CNC_SHOP_ID = 1
CNC_FEED_ID = 1
CNC_CLIENT_ID = 1

VIRTUAL_SHOP_ID = 2
VIRTUAL_SHOP_FEED_ID = 2
VIRTUAL_CLIENT_ID = 2

THIRD_PARTY_SHOP_ID = 3
THIRD_PARTY_FEED_ID = 3
THIRD_PARTY_CLIENT_ID = 3

# Службы доставки
DEFAULT_SERVICE_ID = 103
SELF_SERVICE_ID = 99

# Условия доставки
DEFAULT_DAY_FROM = 3
DEFAULT_DAY_TO = 4
DEFAULT_DELIVERY_COST = 107

DT_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)
COMBINATOR_DAY_FROM = 1
COMBINATOR_DAY_TO = 2
COMBINATOR_DATE_FROM = DT_NOW + timedelta(days=COMBINATOR_DAY_FROM)
COMBINATOR_DATE_TO = DT_NOW + timedelta(days=COMBINATOR_DAY_TO)
COMBINATOR_DELIVERY_COST = 99

# Запросы
ACTUAL_DELIVERY_REQUEST = (
    'place=actual_delivery&'
    'rgb=blue&'
    'pp=18&'
    'rids={rids}&'
    'pickup-options=grouped&'
    'pickup-options-extended-grouping=1&'
    'offers-list={offers}&'
    'rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1&'
    'rearr-factors=market_nordstream={nordstream}&'
    'combinator={combinator_delivery}'
)

SKU_OFFERS_REQUEST = (
    'place=sku_offers&'
    'rgb=blue&'
    'pp=18&'
    'rids={rids}&'
    'pickup-options=grouped&'
    'pickup-options-extended-grouping=1&'
    'market-sku={mskus}&'
)

COMBINE_REQUEST = (
    'place=combine&'
    'rgb=blue&'
    'pp=18&'
    'rids={rids}&'
    'bsformat=2&'
    'pickup-options=grouped&'
    'pickup-options-extended-grouping=1&'
    'offers-list={offers}&'
    'rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1&'
    'rearr-factors=calculate_delivery_day_on_combine=1&'
    'combinator={combinator_delivery}'
)


def __create_outlet(outlet_id, outlet_type, fesh=None, delivery_service_id=None):
    return Outlet(
        point_id=outlet_id,
        fesh=fesh,
        delivery_service_id=delivery_service_id if fesh is None else None,
        point_type=outlet_type,
        region=MSK_RIDS,
        working_days=list(range(10)),
        bool_props=['cashAllowed', 'cardAllowed', 'prepayAllowed'],
        delivery_option=OutletDeliveryOption(
            shipper_id=delivery_service_id,
            day_from=DEFAULT_DAY_FROM,
            day_to=DEFAULT_DAY_TO,
            price=DEFAULT_DELIVERY_COST,
        ),
    )


CNC_OUTLET = __create_outlet(outlet_id=1, outlet_type=OutletType.FOR_POST_TERM, delivery_service_id=SELF_SERVICE_ID)
DEFAULT_OUTLET = __create_outlet(outlet_id=2, outlet_type=OutletType.FOR_PICKUP, delivery_service_id=DEFAULT_SERVICE_ID)


def __create_pickup_bucket(bucket_id, fesh, delivery_service_id, outlets):
    def get_dc_bucket_id(bucket_id):
        dc_bucket_id_base = 1000
        return dc_bucket_id_base + bucket_id

    return PickupBucket(
        bucket_id=bucket_id,
        dc_bucket_id=get_dc_bucket_id(bucket_id),
        fesh=fesh,
        carriers=[delivery_service_id],
        options=[
            PickupOption(outlet_id=i, day_from=DEFAULT_DAY_FROM, day_to=DEFAULT_DAY_TO, price=DEFAULT_DELIVERY_COST)
            for i in outlets
        ],
    )


SELF_SERVICE_BUCKET = __create_pickup_bucket(
    bucket_id=1, fesh=CNC_SHOP_ID, delivery_service_id=SELF_SERVICE_ID, outlets=[CNC_OUTLET.point_id]
)
DEFAULT_SERVICE_BUCKET = __create_pickup_bucket(
    bucket_id=2, fesh=VIRTUAL_SHOP_ID, delivery_service_id=DEFAULT_SERVICE_ID, outlets=[DEFAULT_OUTLET.point_id]
)


def __create_blue_offer(msku, supplier, pickup_buckets):
    def get_model_id(msku):
        return DEFAULT_HID * 100 + msku

    waremd5_template = 'MarketSku{msku}_ModelId{model}w'
    shop_sku_template = 'Shop1_sku{msku}'
    model_id = get_model_id(msku)
    post_term_delivery = supplier is CNC_FEED_ID
    blue_offer = BlueOffer(
        price=150,
        offerid=shop_sku_template.format(msku=msku),
        waremd5=waremd5_template.format(msku=msku, model=model_id),
        feedid=supplier,
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    msku = MarketSku(
        hid=DEFAULT_HID,
        hyperid=model_id,
        sku=str(msku),
        blue_offers=[blue_offer],
        pickup_buckets=pickup_buckets,
        post_term_delivery=post_term_delivery,
    )
    return msku, blue_offer


CNC_MSKU, CNC_OFFER = __create_blue_offer(msku=1, supplier=CNC_FEED_ID, pickup_buckets=[SELF_SERVICE_BUCKET.bucket_id])
ORDINARY_MSKU, ORDINARY_OFFER = __create_blue_offer(
    msku=2, supplier=THIRD_PARTY_FEED_ID, pickup_buckets=[DEFAULT_SERVICE_BUCKET.bucket_id]
)

# Магазины и поставщики
CNC_SHOP = Shop(
    fesh=CNC_SHOP_ID,
    datafeed_id=CNC_FEED_ID,
    client_id=CNC_CLIENT_ID,
    warehouse_id=CNC_WH_ID,
    priority_region=MSK_RIDS,
    name="C&C поставщик",
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    fulfillment_program=False,
    ignore_stocks=True,
    delivery_service_outlets=[CNC_OUTLET.point_id],
)
THIRD_PARTY_SHOP = Shop(
    fesh=THIRD_PARTY_SHOP_ID,
    datafeed_id=THIRD_PARTY_FEED_ID,
    client_id=THIRD_PARTY_CLIENT_ID,
    warehouse_id=DEFAULT_WH_ID,
    priority_region=MSK_RIDS,
    name="3P поставщик",
    currency=Currency.RUR,
    tax_system=Tax.OSN,
    supplier_type=Shop.THIRD_PARTY,
    blue=Shop.BLUE_REAL,
    fulfillment_program=True,
)


class T(TestCase):
    """
    Набор тестов на расчет доставки для C&C офферов при условии наличия Комбинатора

    Комбинатор ничего не знает про C&C, поэтому сроки доставки для таких офферов
    должны быть рассчитаны Репортом, а для всех остальных - Комбинатором

    Там, где это необходимо (например, place=sku_offers), ответы Репорта и Комбинатора
    необходимо сливать воедино
    """

    @classmethod
    def beforePrepare(cls):
        cls.settings.logbroker_enabled = True
        cls.settings.init_combinator_topics = True

    @classmethod
    def prepare(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.settings.report_subrole = 'blue-main'
        cls.settings.loyalty_enabled = True

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

    @classmethod
    def prepare_regions(cls):
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

    @classmethod
    def prepare_shops_and_outlets(cls):
        cls.index.shops += [
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                client_id=VIRTUAL_CLIENT_ID,
                priority_region=MSK_RIDS,
                name="Тестовый виртуальный магазин",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[DEFAULT_OUTLET.point_id],
            ),
            CNC_SHOP,
            THIRD_PARTY_SHOP,
        ]
        cls.index.outlets += [CNC_OUTLET, DEFAULT_OUTLET]

    @classmethod
    def prepare_buckets(cls):
        cls.index.pickup_buckets += [SELF_SERVICE_BUCKET, DEFAULT_SERVICE_BUCKET]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [CNC_MSKU, ORDINARY_MSKU]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]
        for wh_id, ds_id in [(DEFAULT_WH_ID, DEFAULT_SERVICE_ID), (CNC_WH_ID, SELF_SERVICE_ID)]:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=ds_id,
                    name="DefaultDeliveryService",
                    region_to_region_info=[
                        DeliveryServiceRegionToRegionInfo(region_from=MSK_RIDS, region_to=DEFAULT_RIDS, days_key=1)
                    ],
                ),
                DynamicWarehouseAndDeliveryServiceInfo(
                    warehouse_id=wh_id,
                    delivery_service_id=ds_id,
                    operation_time=0,
                    date_switch_time_infos=[
                        DateSwitchTimeAndRegionInfo(
                            date_switch_hour=2,
                            region_to=DEFAULT_RIDS,
                            date_switch_time=TimeInfo(19, 0),
                            packaging_time=TimeInfo(3, 30),
                        )
                    ],
                ),
                DynamicWarehouseInfo(id=wh_id, home_region=MSK_RIDS),
                DynamicWarehouseToWarehouseInfo(warehouse_from=wh_id, warehouse_to=wh_id),
            ]

    @classmethod
    def prepare_delivery_services_and_warehouses(cls):
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[DEFAULT_RIDS],
                warehouse_with_priority=[
                    WarehouseWithPriority(warehouse_id=wh_id, priority=100) for wh_id in [CNC_WH_ID, DEFAULT_WH_ID]
                ],
            )
        ]

    @classmethod
    def prepare_delivery_calc(cls):
        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(
                feed_id=CNC_FEED_ID,
                generation_id=1,
                warehouse_id=CNC_WH_ID,
                pickupBuckets=[SELF_SERVICE_BUCKET.bucket_id],
            )
        ]

        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=10, length=20, width=30, warehouse_id=CNC_WH_ID
        ).respond([], [SELF_SERVICE_BUCKET.dc_bucket_id], [])
        cls.delivery_calc.on_request_offer_buckets(
            weight=5, height=10, length=20, width=30, warehouse_id=DEFAULT_WH_ID
        ).respond([], [DEFAULT_SERVICE_BUCKET.dc_bucket_id], [])

    @classmethod
    def prepare_combinator(cls):
        # GetPickupPointsGrouped (place=actual_delivery)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']
        for wh_id, ds_id in [(CNC_WH_ID, SELF_SERVICE_ID), (DEFAULT_WH_ID, DEFAULT_SERVICE_ID)]:
            for offer, supplier_id in (
                (CNC_OFFER, CNC_SHOP_ID),
                (ORDINARY_OFFER, THIRD_PARTY_SHOP_ID),
            ):
                cls.combinator.on_pickup_points_grouped_request(
                    items=[
                        DeliveryItem(
                            required_count=1,
                            weight=5000,
                            dimensions=[10, 20, 30],
                            cargo_types=[],
                            offers=[
                                CombinatorOffer(
                                    shop_sku=offer.offerid, shop_id=supplier_id, partner_id=wh_id, available_count=1
                                )
                            ],
                            price=offer.price,
                        )
                    ],
                    destination_regions=[MSK_RIDS],
                    point_types=[],
                    total_price=offer.price,
                ).respond_with_grouped_pickup_points(
                    groups=[
                        PickupPointGrouped(
                            ids_list=[CNC_OUTLET.point_id],
                            outlet_type=OutletType.FOR_PICKUP,
                            service_id=ds_id,
                            cost=COMBINATOR_DELIVERY_COST,
                            date_from=COMBINATOR_DATE_FROM,
                            date_to=COMBINATOR_DATE_TO,
                            payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                        )
                    ]
                )

        # GetOffersDeliveryStats (place=sku_offers)
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        for offer, shop in [(CNC_OFFER, CNC_SHOP), (ORDINARY_OFFER, THIRD_PARTY_SHOP)]:
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(offer, shop),
                external_pickup_stats=DeliveryStats(
                    cost=COMBINATOR_DELIVERY_COST, day_from=COMBINATOR_DAY_FROM, day_to=COMBINATOR_DAY_TO
                ),
                outlet_types=[OutletType.FOR_PICKUP, OutletType.FOR_POST_TERM],
            )

    def test_actual_delivery(self):
        """
        Проверяем, что в place=actual_delivery опции доставки для C&C
        офферов вычисляются в Репорте, а не извлекаются из ответа Комбинатора
        по запросу 'GetPickupPointsGrouped'
        """

        response = self.report.request_json(
            ACTUAL_DELIVERY_REQUEST.format(
                rids=MSK_RIDS,
                offers='{}:1'.format(CNC_OFFER.waremd5),
                combinator_delivery=1,
                nordstream=0,
            )
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
                                    'serviceId': SELF_SERVICE_ID,
                                    'dayFrom': DEFAULT_DAY_FROM,
                                    'dayTo': DEFAULT_DAY_TO,
                                    'price': {'currency': 'RUR', 'value': str(DEFAULT_DELIVERY_COST)},
                                }
                            ]
                        },
                    }
                ]
            },
        )

        response = self.report.request_json(
            ACTUAL_DELIVERY_REQUEST.format(
                rids=MSK_RIDS,
                offers='{}:1'.format(ORDINARY_OFFER.waremd5),
                combinator_delivery=1,
                nordstream=0,
            )
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
                                    'serviceId': DEFAULT_SERVICE_ID,
                                    'dayFrom': COMBINATOR_DAY_FROM,
                                    'dayTo': COMBINATOR_DAY_TO,
                                }
                            ]
                        },
                    }
                ]
            },
        )

    def test_actual_delivery_nordstream(self):
        """
        Проверяем, что в place=actual_delivery опции доставки для C&C
        не исчезают при использовании nordstream'a
        """

        response = self.report.request_json(
            ACTUAL_DELIVERY_REQUEST.format(
                rids=MSK_RIDS,
                offers='{}:1'.format(CNC_OFFER.waremd5),
                combinator_delivery=1,
                nordstream=1,
            )
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
                                    'serviceId': SELF_SERVICE_ID,
                                    'dayFrom': DEFAULT_DAY_FROM,
                                    'dayTo': DEFAULT_DAY_TO,
                                    'price': {'currency': 'RUR', 'value': str(DEFAULT_DELIVERY_COST)},
                                }
                            ]
                        },
                    }
                ]
            },
        )

    def test_sku_offers(self):
        """
        Проверяем, что в place=sku_offers статистика доставки для C&C офферов
        вычисляется в Репорте, а для всех остальных - извлекается из ответа Комбинатора
        по запросу 'GetOffersDeliveryStats'
        """

        def get_response_fragment(combinator_sku):
            day_from = COMBINATOR_DAY_FROM if combinator_sku else DEFAULT_DAY_FROM
            day_to = COMBINATOR_DAY_TO if combinator_sku else DEFAULT_DAY_TO
            delivery_cost = COMBINATOR_DELIVERY_COST if combinator_sku else DEFAULT_DELIVERY_COST
            result = {
                'pickupOptions': [
                    {'dayFrom': day_from, 'dayTo': day_to, 'price': {'currency': 'RUR', 'value': str(delivery_cost)}}
                ]
            }
            return result

        USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"
        request = SKU_OFFERS_REQUEST.format(
            rids=MSK_RIDS, mskus=','.join([msku.sku for msku in [CNC_MSKU, ORDINARY_MSKU]])
        )
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'delivery': get_response_fragment(combinator_sku=combinator_sku),
                                    'marketSku': msku.sku,
                                    'wareId': offer.waremd5,
                                }
                            ]
                        },
                    }
                    for msku, offer, combinator_sku in [
                        (CNC_MSKU, CNC_OFFER, False),
                        (ORDINARY_MSKU, ORDINARY_OFFER, True),
                    ]
                ]
            },
        )

    def test_delivery_type_offers(self):
        """
        Проверяем что у оффера в доставке всегда есть deliveryPartnerTypes
        """

        response = self.report.request_json(
            SKU_OFFERS_REQUEST.format(rids=MSK_RIDS, mskus=','.join([msku.sku for msku in [CNC_MSKU, ORDINARY_MSKU]]))
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'sku',
                        'offers': {
                            'items': [
                                {
                                    'entity': 'offer',
                                    'marketSku': msku.sku,
                                    'wareId': offer.waremd5,
                                    'delivery': {
                                        'deliveryPartnerTypes': [delivery_type],
                                    },
                                }
                            ]
                        },
                    }
                    for msku, offer, delivery_type in [
                        (CNC_MSKU, CNC_OFFER, "SHOP"),
                        (ORDINARY_MSKU, ORDINARY_OFFER, "YANDEX_MARKET"),
                    ]
                ]
            },
        )

    def test_combine(self):
        """
        Проверяем, что в place=combine вычисление оптимистичного "срока доставки от"
        происходит на основе данных от Комбинатора для обычных офферов и Репортом - для C&C
        """
        offer_list = [(CNC_OFFER, CNC_MSKU), (ORDINARY_OFFER, ORDINARY_MSKU)]
        response = self.report.request_json(
            COMBINE_REQUEST.format(
                rids=MSK_RIDS,
                offers=','.join(
                    [
                        '{waremd5}:1;msku:{msku}'.format(waremd5=offer.waremd5, msku=msku.sku)
                        for offer, msku in offer_list
                    ]
                ),
                combinator_delivery=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'split-strategy',
                        'default': True,
                        'buckets': [
                            {
                                'warehouseId': wh_id,
                                'deliveryDayFrom': day_from,
                                'offers': [{'wareId': offer.waremd5, 'count': 1}],
                            }
                            for offer, wh_id, day_from in [
                                (CNC_OFFER, CNC_WH_ID, DEFAULT_DAY_FROM),
                                (ORDINARY_OFFER, DEFAULT_WH_ID, COMBINATOR_DAY_FROM),
                            ]
                        ],
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
