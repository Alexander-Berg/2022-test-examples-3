#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    PickupOption,
    PickupBucket,
    OutletDeliveryOption,
    DeliveryBucket,
    DeliveryOption,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    Payment,
    OfferDimensions,
    Outlet,
    ShopBuckets,
    MarketSku,
)

from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    create_virtual_box,
    DeliveryItem,
    Destination,
    PickupPointGrouped,
    PointTimeInterval,
)
from core.logs import ErrorCodes
from core.types.delivery import OutletType

from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

from datetime import datetime, time, timedelta

from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.combinator import DeliveryStats, make_offer_id
from core.matcher import Absent

TODAY = datetime.fromtimestamp(REQUEST_TIMESTAMP)


class _RequestConstants:
    ENABLE_UNIQUE_OFFERS = "market_enable_unique_offers={}"


class _DeliveryConstants:
    delivery_info = DeliveryOption(day_from=1, day_to=5, price=100)


class _CombinatorResponseStats:
    courier_stats = DeliveryStats(
        cost=_DeliveryConstants.delivery_info.price,
        day_from=_DeliveryConstants.delivery_info.day_from,
        day_to=_DeliveryConstants.delivery_info.day_to,
    )
    pickup_stats = DeliveryStats(
        cost=_DeliveryConstants.delivery_info.price,
        day_from=_DeliveryConstants.delivery_info.day_from,
        day_to=_DeliveryConstants.delivery_info.day_to,
    )


class _Shops:
    dsbs_shop = Shop(
        fesh=8,
        priority_region=213,
        cpa=Shop.CPA_REAL,
        client_id=11,
        datafeed_id=123,
        warehouse_id=75,
    )
    shops = [dsbs_shop]


class _Outlets:
    outlet = Outlet(
        point_id=123456,
        fesh=_Shops.dsbs_shop.fesh,
        delivery_option=OutletDeliveryOption(
            shipper_id=99,
            day_from=_DeliveryConstants.delivery_info.day_from,
            day_to=_DeliveryConstants.delivery_info.day_to,
        ),
        region=_Shops.dsbs_shop.priority_region,
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
        point_type=Outlet.FOR_PICKUP,
    )


class _Mskus:
    HYPERID = 32
    HID = 20
    msku_for_unique_offer = MarketSku(
        title="unique sku",
        hyperid=HYPERID,
        sku=606062,
        hid=HID,
    )
    msku_for_simple_offer = MarketSku(
        title="simple sku",
        hyperid=HYPERID,
        sku=606063,
        hid=HID,
    )
    msku_for_another_unique_offer = MarketSku(
        title="anotherunique sku",
        hyperid=HYPERID,
        sku=606064,
        hid=HID,
    )
    mskus = [msku_for_unique_offer, msku_for_another_unique_offer, msku_for_simple_offer]


class _Offers:
    unique_dsbs_offer = Offer(
        fesh=_Shops.dsbs_shop.fesh,
        feedid=123,
        cpa=Offer.CPA_REAL,
        hyperid=_Mskus.HYPERID,
        price=50,
        title='unique_offer',
        waremd5=Offer.generate_waremd5("unique_offer"),
        delivery_options=[
            DeliveryOption(price=500, day_from=5, day_to=10),
        ],
        delivery_buckets=[123],
        weight=5,
        hid=_Mskus.HID,
        offerid="123",
        dimensions=OfferDimensions(length=20, width=30, height=10),
        sku=_Mskus.msku_for_unique_offer.sku,
        unique=True,
        pickup_buckets=[125],
        pickup_option=_DeliveryConstants.delivery_info,
    )
    simple_dsbs_offer = Offer(
        fesh=_Shops.dsbs_shop.fesh,
        feedid=123,
        cpa=Offer.CPA_REAL,
        hyperid=_Mskus.HYPERID,
        price=50,
        title='simple_offer',
        waremd5=Offer.generate_waremd5("simple_offer"),
        delivery_options=[
            DeliveryOption(price=500, day_from=5, day_to=10),
        ],
        delivery_buckets=[123],
        weight=5,
        offerid="321",
        hid=_Mskus.HID,
        sku=_Mskus.msku_for_simple_offer.sku,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        pickup_buckets=[125],
        pickup_option=_DeliveryConstants.delivery_info,
    )
    another_unique_dsbs_offer = Offer(
        fesh=_Shops.dsbs_shop.fesh,
        feedid=123,
        cpa=Offer.CPA_REAL,
        hyperid=_Mskus.HYPERID,
        price=50,
        title='another_unique_dsbs_offer',
        waremd5=Offer.generate_waremd5("another_unique_dsbs_offer"),
        delivery_options=[
            DeliveryOption(price=500, day_from=5, day_to=10),
        ],
        delivery_buckets=[123],
        weight=5,
        hid=_Mskus.HID,
        offerid="1232",
        dimensions=OfferDimensions(length=20, width=30, height=10),
        sku=_Mskus.msku_for_another_unique_offer.sku,
        unique=True,
        pickup_buckets=[125],
        pickup_option=_DeliveryConstants.delivery_info,
    )
    offers = [unique_dsbs_offer, simple_dsbs_offer, another_unique_dsbs_offer]


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.shops += _Shops.shops
        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=_Shops.dsbs_shop.fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_ALL,
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                children=[
                    Region(
                        rid=213,
                        name='Москва',
                    ),
                ],
            ),
        ]
        cls.index.mskus += _Mskus.mskus
        cls.index.offers += _Offers.offers
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=123,
                fesh=_Shops.dsbs_shop.fesh,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[_DeliveryConstants.delivery_info],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )
        ]
        cls.index.shop_buckets += [
            ShopBuckets(shop_id=_Shops.dsbs_shop.fesh, pickup_buckets=[125]),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=125,
                fesh=_Shops.dsbs_shop.fesh,
                carriers=[99],
                options=[PickupOption(outlet_id=_Outlets.outlet.point_id)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                payment_methods=[
                    Payment.PT_ALL,
                    Payment.PT_PREPAYMENT_CARD,
                    Payment.PT_PREPAYMENT_OTHER,
                    Payment.PT_CARD_ON_DELIVERY,
                ],
            ),
        ]
        cls.index.outlets += [
            _Outlets.outlet,
        ]

    @classmethod
    def prepare_combinator(cls):
        cls.settings.check_combinator_errors = True
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

        def add_offer_delivery(offer, shop, delivery_info, courier_stats, pickup_stats, outlet):
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(offer, shop),
                courier_stats=DeliveryStats(*courier_stats),
                external_pickup_stats=DeliveryStats(*pickup_stats),
                # В комбинатор передаются опции доставки из фида
                offer_feed_delivery_option=delivery_info,
                offer_feed_pickup_option=delivery_info,
            )

            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=offer.weight * 1000,
                        dimensions=[
                            offer.dimensions.width,
                            offer.dimensions.height,
                            offer.dimensions.length,
                        ],
                        cargo_types=[],
                        offers=[
                            CombinatorOffer(
                                shop_sku=offer.offerid,
                                shop_id=shop.fesh,
                                partner_id=shop.warehouse_id,
                                available_count=1,
                                feed_id=offer.feed_id(),
                                offer_feed_delivery_option=delivery_info,
                                offer_feed_pickup_option=delivery_info,
                            ),
                        ],
                        price=offer.price,
                    ),
                ],
                destination=Destination(region_id=213),
                payment_methods=[],
                total_price=offer.price,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        date_from=TODAY + timedelta(days=delivery_info.day_from),
                        date_to=TODAY + timedelta(days=delivery_info.day_to),
                        delivery_service_id=99,
                    )
                ],
                virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
            )

            cls.combinator.on_pickup_points_grouped_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=offer.weight * 1000,
                        dimensions=[
                            offer.dimensions.width,
                            offer.dimensions.height,
                            offer.dimensions.length,
                        ],
                        cargo_types=[],
                        offers=[
                            CombinatorOffer(
                                shop_sku=offer.offerid,
                                shop_id=shop.fesh,
                                partner_id=shop.warehouse_id,
                                available_count=1,
                                feed_id=offer.feed_id(),
                                offer_feed_delivery_option=delivery_info,
                                offer_feed_pickup_option=delivery_info,
                            ),
                        ],
                        price=offer.price,
                    ),
                ],
                destination_regions=[213, 1],
                point_types=[],
                total_price=offer.price,
            ).respond_with_grouped_pickup_points(
                groups=[
                    PickupPointGrouped(
                        ids_list=[
                            outlet.point_id,
                        ],
                        outlet_type=OutletType.FOR_PICKUP,
                        service_id=99,
                        cost=100,
                        date_from=TODAY + timedelta(days=delivery_info.day_from),
                        date_to=TODAY + timedelta(days=delivery_info.day_to),
                        payment_methods=[PaymentMethod.PT_ALL],
                        external_logistics=True,
                        dsbs_to_market_outlet=False,
                        delivery_intervals=[
                            PointTimeInterval(point_id=outlet.point_id, time_from=time(10, 0), time_to=time(22, 0)),
                        ],
                        dsbs_ids=[
                            str(outlet.point_id),
                        ],
                    ),
                ],
                virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
            )

        add_offer_delivery(
            _Offers.unique_dsbs_offer,
            _Shops.dsbs_shop,
            _DeliveryConstants.delivery_info,
            _CombinatorResponseStats.courier_stats,
            _CombinatorResponseStats.pickup_stats,
            _Outlets.outlet,
        )

        add_offer_delivery(
            _Offers.another_unique_dsbs_offer,
            _Shops.dsbs_shop,
            _DeliveryConstants.delivery_info,
            _CombinatorResponseStats.courier_stats,
            _CombinatorResponseStats.pickup_stats,
            _Outlets.outlet,
        )

        add_offer_delivery(
            _Offers.simple_dsbs_offer,
            _Shops.dsbs_shop,
            _DeliveryConstants.delivery_info,
            _CombinatorResponseStats.courier_stats,
            _CombinatorResponseStats.pickup_stats,
            _Outlets.outlet,
        )

    def test_unique_offer_filter(self):
        '''
        Проверяем, как отображаются уникальные оффера:
        * Отображается поле "isUniqueOffer" - флаг уникальности оффера
        * Отображается "orderReturnPolicy" - описание политики возврата для оффера
        * Отображается "orderCancelPolicy" - описание политики отмены для оффера

        + проверяем, что уникальный оффер скрыт под эксп. флагом market_enable_unique_offers
        '''

        def offer_exist(report_response):
            # Проверяем, отображение уникального оффера
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.unique_dsbs_offer.waremd5,
                    "isUniqueOffer": True,
                    "payments": {
                        "deliveryCard": False,
                        "deliveryCash": False,
                        "prepaymentCard": True,
                        "prepaymentOther": True,
                    },
                    "orderReturnPolicy": {
                        "type": "forbidden",
                        "reason": "unique-order",
                        "description": "товар под заказ",
                    },
                    "orderCancelPolicy": {
                        "type": "time-limit",
                        "daysForCancel": 3,
                        "reason": "unique-order",
                        "description": "товар под заказ",
                    },
                },
            )
            # проверяем, что в обычный оффер дополнительные поля не проросли
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.simple_dsbs_offer.waremd5,
                    "isUniqueOffer": Absent(),
                    "orderReturnPolicy": Absent(),
                    "orderCancelPolicy": Absent(),
                    "payments": {
                        "deliveryCard": True,
                        "deliveryCash": True,
                        "prepaymentCard": True,
                        "prepaymentOther": True,
                    },
                },
            )

        def offer_not_exist(report_response):
            # проверяем, что уникальный оффер скрыт
            self.assertFragmentNotIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.unique_dsbs_offer.waremd5,
                },
            )
            # проверяем, что в обычный оффер дополнительные поля не проросли
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.simple_dsbs_offer.waremd5,
                    "isUniqueOffer": Absent(),
                    "orderReturnPolicy": Absent(),
                    "orderCancelPolicy": Absent(),
                    "payments": {
                        "deliveryCard": True,
                        "deliveryCash": True,
                        "prepaymentCard": True,
                        "prepaymentOther": True,
                    },
                },
            )

        for flag, expect in [(0, offer_not_exist), (1, offer_exist), (None, offer_not_exist)]:
            for place in ['prime', 'productoffers']:
                request = 'place={}&rids=213&hyperid={}'.format(place, _Offers.unique_dsbs_offer.hyperid)
                if flag is not None:
                    request += "&rearr-factors={}".format(_RequestConstants.ENABLE_UNIQUE_OFFERS.format(flag))
                response = self.report.request_json(request)
                expect(response)

    def test_payment_methods_in_actual_delivery(self):
        request_tmp = 'total-price={price}&pickup-options=grouped&place=actual_delivery&rids=213&offers-list={offerid}:1&rearr-factors=use_dsbs_string_point_ids=1'
        # По дефолту эти флаги выключены настройкой внутри лайтов
        # однако в коде эти флаги на данный момент включены
        # приводим состояние экспов к продовому виду "руками"
        rearr_part = '&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=1;use_dsbs_combinator_response_in_actual_delivery=1;'
        rearr_part += ";" + _RequestConstants.ENABLE_UNIQUE_OFFERS.format(1)
        request_tmp += rearr_part

        # Проверяем оффер с долгой доставкой, и что его поведение зависит от флага
        request = request_tmp.format(offerid=_Offers.unique_dsbs_offer.waremd5, price=_Offers.unique_dsbs_offer.price)
        response = self.report.request_json(request)
        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [{"paymentMethods": ["YANDEX"]}],
                    "pickupOptions": [{"paymentMethods": ["YANDEX"]}],
                },
            },
            allow_different_len=False,
        )

        # Проверяем оффер без долгой доставки, и что его поведение не зависит от флага
        request = request_tmp.format(offerid=_Offers.simple_dsbs_offer.waremd5, price=_Offers.simple_dsbs_offer.price)
        response = self.report.request_json(request)
        # Проверяем, отображение уникального оффера
        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "isEstimated": Absent(),
                            "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                        }
                    ],
                    "pickupOptions": [
                        {
                            "isEstimated": Absent(),
                            "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                        }
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_combine(self):
        cart = [
            _Offers.unique_dsbs_offer.waremd5,
            _Offers.another_unique_dsbs_offer.waremd5,
            _Offers.simple_dsbs_offer.waremd5,
        ]
        req = (
            "place=combine&rgb=green_with_blue&use-virt-shop=0&rids=213"
            + "&offers-list="
            + ",".join(
                "{ware}:{count};cart_item_id:{cartid}".format(ware=ware, count=1, cartid=cartid)
                for cartid, ware in enumerate(cart)
            )
        )
        rearr_factors = "&rearr-factors=" + _RequestConstants.ENABLE_UNIQUE_OFFERS.format(1)
        response = self.report.request_json(req + rearr_factors)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "name": "consolidate-without-crossdock",
                        "buckets": [
                            {
                                "shopId": _Shops.dsbs_shop.fesh,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": _Offers.simple_dsbs_offer.waremd5,
                                        "replacedId": _Offers.simple_dsbs_offer.waremd5,
                                        "count": 1,
                                        "cartItemIds": [2],
                                    }
                                ],
                            },
                            {
                                "shopId": _Shops.dsbs_shop.fesh,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": _Offers.another_unique_dsbs_offer.waremd5,
                                        "replacedId": _Offers.another_unique_dsbs_offer.waremd5,
                                        "count": 1,
                                        "cartItemIds": [1],
                                    }
                                ],
                            },
                            {
                                "shopId": _Shops.dsbs_shop.fesh,
                                "isFulfillment": False,
                                "warehouseId": Absent(),
                                "offers": [
                                    {
                                        "wareId": _Offers.unique_dsbs_offer.waremd5,
                                        "replacedId": _Offers.unique_dsbs_offer.waremd5,
                                        "count": 1,
                                        "cartItemIds": [0],
                                    }
                                ],
                            },
                        ],
                    }
                ],
                "offers": {"items": [{"wareId": ware} for ware in cart]},
            },
        )


if __name__ == '__main__':
    main()
