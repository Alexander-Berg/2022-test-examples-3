#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, time, timedelta

from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicCapacityDaysOff,
    DynamicCapacityInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseInfo,
    MarketSku,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    TimeInfo,
    TimeIntervalInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
)
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment
from core.types.combinator import CombinatorOffer, create_delivery_option, create_virtual_box, DeliveryItem, Destination
from core.types.delivery import get_depth_days


MSK_RIDS = 213

RIDS_0 = 120543  # Якиманка
RIDS_1 = 117065  # Арбат
RIDS_11 = 1170651  # Новый Арбат
RIDS_2 = 120554  # Текстильщики
RIDS_3 = 120546  # Кузьминки
RIDS_4 = 120560  # Ломоносовский
RIDS_5 = 117019  # Войковский
RIDS_6 = 20394  # Первомайская

WAREHOUSE_ID = 145
DELIVERY_SERVICE_ID = 1

LAVKA_DELIVERY_SERVICE_IDS_BY_REGION = {
    RIDS_0: 1005471,
    RIDS_1: 1003567,
    RIDS_2: 1006419,
    RIDS_3: 1006422,
    RIDS_4: 1006425,
    RIDS_5: 1006428,
    RIDS_6: 1005471,
}

DC_BUCKET_ID_BASE = 10000
DELIVERY_BUCKET_ID = 1000
DC_DELIVERY_BUCKET_ID = DC_BUCKET_ID_BASE + DELIVERY_BUCKET_ID
LAVKA_DELIVERY_BUCKET_IDS_BY_REGION = {
    RIDS_0: 2000,
    RIDS_1: 2001,
    RIDS_2: 2002,
    RIDS_3: 2003,
    RIDS_4: 2004,
    RIDS_5: 2005,
    RIDS_6: 2006,
}
DC_LAVKA_DELIVERY_BUCKET_IDS_BY_REGION = {
    rids: DC_BUCKET_ID_BASE + LAVKA_DELIVERY_BUCKET_IDS_BY_REGION[rids] for rids in LAVKA_DELIVERY_BUCKET_IDS_BY_REGION
}

ALL_DELIVERY_BUCKET_IDS = [LAVKA_DELIVERY_BUCKET_IDS_BY_REGION[rids] for rids in LAVKA_DELIVERY_BUCKET_IDS_BY_REGION]
ALL_DELIVERY_BUCKET_IDS += [DELIVERY_BUCKET_ID]
ALL_DC_DELIVERY_BUCKET_IDS = [bucket + DC_BUCKET_ID_BASE for bucket in ALL_DELIVERY_BUCKET_IDS]

CATEGORY_ID = 10
MODEL_ID = 100

SUPPLIER_SHOP_ID = 1
SUPPLIER_FEED_ID = 1
VIRTUAL_SHOP_ID = 2
VIRTUAL_SHOP_FEED_ID = 2

DT_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)
COMBINATOR_DAY_FROM = 3
COMBINATOR_DAY_TO = 5
COMBINATOR_DATE_FROM = DT_NOW + timedelta(days=COMBINATOR_DAY_FROM)
COMBINATOR_DATE_TO = DT_NOW + timedelta(days=COMBINATOR_DAY_TO)


def __create_blue_offer(title, sku, buckets, price, offer_dimensions=OfferDimensions(length=30, width=30, height=30)):
    blue_offer = BlueOffer(
        price=price,
        offerid='Shop1_sku{msku}'.format(msku=sku),
        waremd5='Sku{msku}Price{price}k-vm1Goleg'.format(msku=sku, price=str(price)[:2]),
        feedid=SUPPLIER_FEED_ID,
        weight=5,
        dimensions=offer_dimensions,
    )
    msku = MarketSku(
        title=title, hid=CATEGORY_ID, hyperid=MODEL_ID, sku=str(sku), blue_offers=[blue_offer], delivery_buckets=buckets
    )
    return msku, blue_offer


ORDINARY_MSKU, ORDINARY_OFFER = __create_blue_offer(
    title="Обычный оффер", sku=10, buckets=[DELIVERY_BUCKET_ID], price=2000
)

LAVKA_MSKU, LAVKA_OFFER = __create_blue_offer(
    title="Оффер с опициями доставки от Лавки", sku=11, price=22000, buckets=ALL_DELIVERY_BUCKET_IDS
)

EXPENSIVE_MSKU, EXPENSIVE_OFFER = __create_blue_offer(
    title="Слишком дорогой оффер", sku=12, buckets=ALL_DELIVERY_BUCKET_IDS, price=52000
)

LARGE_LAVKA_MSKU, LARGE_LAVKA_OFFER = __create_blue_offer(
    title="Большой оффер с опциями доставки от Лавки",
    sku=13,
    buckets=ALL_DELIVERY_BUCKET_IDS,
    price=1500,
    offer_dimensions=OfferDimensions(length=110, width=60, height=60),
)


class T(TestCase):
    @staticmethod
    def __create_actual_delivery_request(color, rids, offers_list, use_combinator=0):
        request_base = (
            'place=actual_delivery&'
            'rgb={color}&'
            'rids={rids}&'
            'pickup-options=grouped&'
            'pickup-options-extended-grouping=1&'
            'regset=2&'
            'combinator={use_combinator}&'
            'rearr-factors='
        )

        offers_list_part = ''
        if offers_list:
            offers_list_part = '&offers-list=' + ','.join(
                ['{}:{}'.format(ware_md5, amount) for ware_md5, amount in offers_list]
            )

        return request_base.format(color=color, rids=rids, use_combinator=use_combinator) + offers_list_part

    @staticmethod
    def __create_courier_bucket(bucket_id, delivery_service_id, options, rids):
        return DeliveryBucket(
            bucket_id=bucket_id,
            dc_bucket_id=DC_BUCKET_ID_BASE + bucket_id,
            fesh=SUPPLIER_FEED_ID,
            carriers=[delivery_service_id],
            regional_options=[
                RegionalDelivery(
                    rid=rids,
                    options=options,
                    payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                )
            ],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.index.regiontree += [
            Region(
                rid=MSK_RIDS,
                name="Москва",
                children=[
                    Region(rid=RIDS_0, name="Якиманка"),
                    Region(rid=RIDS_1, name="Арбат", children=[Region(rid=RIDS_11, name="Новый Арбат")]),
                    Region(rid=RIDS_2, name="Текстильщики"),
                    Region(rid=RIDS_3, name="Кузьминки"),
                    Region(rid=RIDS_4, name="Ломоносовский"),
                    Region(rid=RIDS_5, name="Войковский"),
                    Region(rid=RIDS_6, name="Первомайская"),
                ],
            ),
        ]

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            Shop(
                fesh=SUPPLIER_FEED_ID,
                datafeed_id=SUPPLIER_FEED_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]

    @classmethod
    def prepare_courier_buckets(cls):
        default_options = [DeliveryOption(price=10, day_from=1, day_to=1, shop_delivery_price=5)]
        # Лавка доставляет по конкретным регионам Москвы, а курьерка - по всей Москве
        delivery_data = [
            (DELIVERY_BUCKET_ID, DELIVERY_SERVICE_ID, default_options, MSK_RIDS),
        ]
        for rid in LAVKA_DELIVERY_SERVICE_IDS_BY_REGION:
            delivery_data += [
                (
                    LAVKA_DELIVERY_BUCKET_IDS_BY_REGION[rid],
                    LAVKA_DELIVERY_SERVICE_IDS_BY_REGION[rid],
                    default_options,
                    rid,
                ),
            ]
        cls.index.delivery_buckets += [
            T.__create_courier_bucket(
                bucket_id=bucket_id,
                delivery_service_id=delivery_service_id,
                options=options,
                rids=rids,
            )
            for bucket_id, delivery_service_id, options, rids in delivery_data
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=5, width=30, height=30, length=30).respond(
            dc_courier_bucket_indexes=ALL_DC_DELIVERY_BUCKET_IDS, dc_pickup_bucket_indexes=[], dc_post_bucket_indexes=[]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=60, height=60, length=110).respond(
            dc_courier_bucket_indexes=ALL_DC_DELIVERY_BUCKET_IDS, dc_pickup_bucket_indexes=[], dc_post_bucket_indexes=[]
        )

    @classmethod
    def prepare_lms(cls):
        for rids in LAVKA_DELIVERY_SERVICE_IDS_BY_REGION:
            cls.dynamic.lms += [
                DynamicDeliveryServiceInfo(
                    id=LAVKA_DELIVERY_SERVICE_IDS_BY_REGION[rids],
                    name='lavka_delivery_service',
                    rating=2,
                    time_intervals=[
                        TimeIntervalsForRegion(
                            region=rids, intervals=[TimeIntervalsForDaysInfo(intervals_key=2, days_key=1)]
                        )
                    ],
                ),
            ]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(
                id=DELIVERY_SERVICE_ID,
                name='ordinary_delivery_sevice',
                rating=2,
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=MSK_RIDS, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=1)]
                    )
                ],
            ),
            DynamicDaysSet(key=1, days=[0, 1, 2, 3, 4, 5, 6]),
            DynamicDaysSet(key=2, days=list(range(get_depth_days() - 7))),  # no available working days in the calendar
            DynamicTimeIntervalsSet(
                key=1,
                intervals=[
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(17, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=2,
                intervals=[
                    TimeIntervalInfo(TimeInfo(12, 0), TimeInfo(17, 30)),
                ],
            ),
            # for capacity test
            # for capacity test
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WAREHOUSE_ID, warehouse_to=WAREHOUSE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=WAREHOUSE_ID,
                delivery_service_id=LAVKA_DELIVERY_SERVICE_IDS_BY_REGION[RIDS_1],
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=21, region_to=225)],
                capacity_by_region=[
                    DynamicCapacityInfo(
                        RIDS_11,
                        capacity_days_off=[
                            DynamicCapacityDaysOff(delivery_type=DynamicCapacityDaysOff.DT_COURIER, days_key=2)
                        ],
                    )
                ],
            ),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [ORDINARY_MSKU, EXPENSIVE_MSKU, LAVKA_MSKU, LARGE_LAVKA_MSKU]

    @classmethod
    def prepare_combinator(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)
        cls.settings.default_search_experiment_flags += ['enable_dsbs_combinator_request_in_actual_delivery=0']

        for rids in LAVKA_DELIVERY_SERVICE_IDS_BY_REGION:
            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=5000,
                        dimensions=[30, 30, 30],
                        cargo_types=[],
                        offers=[
                            CombinatorOffer(
                                shop_sku=LAVKA_OFFER.offerid,
                                shop_id=SUPPLIER_FEED_ID,
                                partner_id=WAREHOUSE_ID,
                                available_count=1,
                            )
                        ],
                        price=LAVKA_OFFER.price,
                    )
                ],
                destination=Destination(region_id=rids),
                payment_methods=[],
                total_price=LAVKA_OFFER.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=5,
                        date_from=COMBINATOR_DATE_FROM,
                        date_to=COMBINATOR_DATE_TO,
                        time_from=time(10, 0),
                        time_to=time(22, 0),
                        delivery_service_id=DELIVERY_SERVICE_ID,
                    )
                ],
                virtual_box=create_virtual_box(weight=5000, length=30, width=30, height=30),
            )

    def expected_delivery_options(self, is_lavka_available, region=None):
        options = [
            {
                'serviceId': str(DELIVERY_SERVICE_ID),
                'dayFrom': i,
                'dayTo': i,
                'paymentMethods': [
                    Payment.to_dc_type(pm)
                    for pm in (Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY)
                ],
            }
            for i in range(1, 6)
        ]

        if is_lavka_available:
            options += [
                {
                    'serviceId': str(LAVKA_DELIVERY_SERVICE_IDS_BY_REGION[region]),
                    'dayFrom': i,
                    'dayTo': i,
                    'timeIntervals': [{'from': "12:00", 'to': "17:30"}],
                    'paymentMethods': [Payment.to_dc_type(Payment.PT_YANDEX)],
                }
                for i in range(1, 6)
            ]

        return {'results': [{'delivery': {'options': options}}]}

    def test_delivery_via_split_box(self):
        """
        Проверяем, что логика разбиения пользовательской корзины
        сохраняется для всех СД кроме Яндекс.Лавки
        """
        for rids in LAVKA_DELIVERY_SERVICE_IDS_BY_REGION:
            response = self.report.request_json(
                T.__create_actual_delivery_request(
                    color='blue',
                    rids=rids,
                    offers_list=[(offer.waremd5, 1) for offer in [LAVKA_OFFER, LARGE_LAVKA_OFFER]],
                )
            )
            self.assertFragmentIn(
                response, self.expected_delivery_options(is_lavka_available=False), allow_different_len=False
            )

    def test_expensive_offer(self):
        """
        Проверяем, что опции доставки Яндекс.Лавкой не доступны
        для заказов стоимостью выше определенного порога (20 000 руб)
        """
        for rids in LAVKA_DELIVERY_SERVICE_IDS_BY_REGION:
            response = self.report.request_json(
                T.__create_actual_delivery_request(color='blue', rids=rids, offers_list=[(EXPENSIVE_OFFER.waremd5, 1)])
            )
            self.assertFragmentIn(
                response, self.expected_delivery_options(is_lavka_available=False), allow_different_len=False
            )

    def test_lavka_exceeds_capacity(self):
        """
        В регион RIDS11 Лавка не доставляется из-за превышения капасити.
        Проверяем, что её опций на выдаче нет
        """
        response = self.report.request_json(
            T.__create_actual_delivery_request(color='blue', rids=RIDS_11, offers_list=[(LAVKA_OFFER.waremd5, 1)])
        )
        self.assertFragmentIn(
            response, self.expected_delivery_options(is_lavka_available=False), allow_different_len=False
        )


if __name__ == '__main__':
    main()
