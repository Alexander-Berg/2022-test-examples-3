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
    EnableLongLocalDelivery = "market_long_local_delivery={}"
    HideLocalDelivery = "market_hide_long_delivery_offers={}"


class _DeliveryConstants:
    long_delivery_info = DeliveryOption(day_from=35, day_to=40, price=100)
    no_long_delivery_info = DeliveryOption(day_from=1, day_to=7, price=100)


class _CombinatorResponseStats:
    long_courier_stats = DeliveryStats(
        cost=_DeliveryConstants.long_delivery_info.price,
        day_from=_DeliveryConstants.long_delivery_info.day_from,
        day_to=_DeliveryConstants.long_delivery_info.day_to,
    )
    long_pickup_stats = DeliveryStats(
        cost=_DeliveryConstants.long_delivery_info.price,
        day_from=_DeliveryConstants.long_delivery_info.day_from,
        day_to=_DeliveryConstants.long_delivery_info.day_to,
    )
    no_long_courier_stats = DeliveryStats(
        cost=_DeliveryConstants.no_long_delivery_info.price,
        day_from=_DeliveryConstants.no_long_delivery_info.day_from,
        day_to=_DeliveryConstants.no_long_delivery_info.day_to,
    )
    no_long_pickup_stats = DeliveryStats(
        cost=_DeliveryConstants.no_long_delivery_info.price,
        day_from=_DeliveryConstants.no_long_delivery_info.day_from,
        day_to=_DeliveryConstants.no_long_delivery_info.day_to,
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


class _Mskus:
    HYPERID = 32
    HID = 20
    msku_for_long_offer = MarketSku(
        title="sku1",
        hyperid=HYPERID,
        sku=606062,
        hid=HID,
    )
    msku_for_no_long_offer = MarketSku(
        title="sku2",
        hyperid=HYPERID,
        sku=606063,
        hid=HID,
    )
    msku_for_no_long_offer_p = MarketSku(
        title="sku3",
        hyperid=HYPERID,
        sku=606064,
        hid=HID,
    )
    mskus = [msku_for_long_offer, msku_for_no_long_offer, msku_for_no_long_offer_p]


class _Offers:
    long_delivery_offer = Offer(
        fesh=8,
        feedid=123,
        cpa=Offer.CPA_REAL,
        sku=_Mskus.msku_for_long_offer.sku,
        hyperid=_Mskus.msku_for_long_offer.hyperid,
        hid=_Mskus.HID,
        price=50,
        title='long_delivery_offer',
        offerid="123",
        waremd5=Offer.generate_waremd5("long_delivery"),
        weight=1,
        dimensions=OfferDimensions(length=22, width=22, height=22),
        delivery_options=[_DeliveryConstants.long_delivery_info],
        pickup_option=_DeliveryConstants.long_delivery_info,
        delivery_buckets=[123],
        pickup_buckets=[125],
        business_id=8,
    )
    not_long_delivery_offer = Offer(
        fesh=8,
        feedid=123,
        sku=_Mskus.msku_for_no_long_offer.sku,
        hyperid=_Mskus.msku_for_no_long_offer.hyperid,
        hid=_Mskus.HID,
        price=50,
        weight=1,
        dimensions=OfferDimensions(length=22, width=22, height=22),
        offerid="321",
        title='not_long_delivery_offer',
        waremd5=Offer.generate_waremd5("not_long_delivery"),
        delivery_options=[_DeliveryConstants.no_long_delivery_info],
        pickup_option=_DeliveryConstants.no_long_delivery_info,
        delivery_buckets=[124],
        pickup_buckets=[126],
        cpa=Offer.CPA_REAL,
        business_id=8,
    )
    no_long_delivery_offer_pickup = Offer(
        fesh=8,
        feedid=123,
        sku=_Mskus.msku_for_no_long_offer_p.sku,
        hyperid=_Mskus.msku_for_no_long_offer_p.hyperid,
        hid=_Mskus.HID,
        price=50,
        weight=1,
        dimensions=OfferDimensions(length=22, width=22, height=22),
        offerid="123_p",
        title='no_long_delivery_offer_p',
        waremd5=Offer.generate_waremd5("no_long_delivery_p"),
        delivery_options=[],
        pickup_option=_DeliveryConstants.no_long_delivery_info,
        pickup_buckets=[126],
        cpa=Offer.CPA_REAL,
        business_id=8,
    )
    offers = [long_delivery_offer, not_long_delivery_offer, no_long_delivery_offer_pickup]


class _Outlets:
    long_delivery_outlet = Outlet(
        point_id=123456,
        fesh=_Shops.dsbs_shop.fesh,
        delivery_option=OutletDeliveryOption(
            shipper_id=99,
            day_from=_DeliveryConstants.long_delivery_info.day_from,
            day_to=_DeliveryConstants.long_delivery_info.day_to,
        ),
        region=_Shops.dsbs_shop.priority_region,
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
        point_type=Outlet.FOR_PICKUP,
    )
    no_long_delivery_outlet = Outlet(
        point_id=123457,
        fesh=_Shops.dsbs_shop.fesh,
        delivery_option=OutletDeliveryOption(
            shipper_id=99,
            day_from=_DeliveryConstants.no_long_delivery_info.day_from,
            day_to=_DeliveryConstants.no_long_delivery_info.day_to,
        ),
        region=_Shops.dsbs_shop.priority_region,
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
        point_type=Outlet.FOR_PICKUP,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += _Shops.shops

        cls.index.regiontree += [
            Region(
                rid=1,
                name='???????????????????? ??????????????',
                children=[
                    Region(
                        rid=213,
                        name='????????????',
                    ),
                ],
            ),
        ]
        cls.index.mskus += _Mskus.mskus
        cls.index.offers += _Offers.offers
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
                        ],
                    ),
                ],
            ),
        ]
        cls.index.shop_buckets += [
            ShopBuckets(shop_id=_Shops.dsbs_shop.fesh, pickup_buckets=[125]),
            ShopBuckets(shop_id=_Shops.dsbs_shop.fesh, pickup_buckets=[126]),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=125,
                fesh=_Shops.dsbs_shop.fesh,
                carriers=[99],
                options=[PickupOption(outlet_id=_Outlets.long_delivery_outlet.point_id)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                payment_methods=[
                    Payment.PT_ALL,
                    Payment.PT_PREPAYMENT_CARD,
                    Payment.PT_PREPAYMENT_OTHER,
                    Payment.PT_CARD_ON_DELIVERY,
                ],
            ),
            PickupBucket(
                bucket_id=126,
                fesh=_Shops.dsbs_shop.fesh,
                carriers=[99],
                options=[PickupOption(outlet_id=_Outlets.no_long_delivery_outlet.point_id)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                payment_methods=[
                    Payment.PT_ALL,
                    Payment.PT_PREPAYMENT_CARD,
                    Payment.PT_PREPAYMENT_OTHER,
                    Payment.PT_CARD_ON_DELIVERY,
                ],
            ),
        ]

        cls.index.outlets += [_Outlets.long_delivery_outlet, _Outlets.no_long_delivery_outlet]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=123,
                fesh=_Shops.dsbs_shop.fesh,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[_DeliveryConstants.long_delivery_info],
                        payment_methods=[
                            Payment.PT_ALL,
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=124,
                fesh=_Shops.dsbs_shop.fesh,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=214,
                        options=[_DeliveryConstants.no_long_delivery_info],
                        payment_methods=[
                            Payment.PT_ALL,
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_combinator(cls):
        cls.settings.check_combinator_errors = True
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

        def add_offer_delivery(offer, shop, delivery_info, courier_stats, pickup_stats, outlet):
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(offer, shop),
                courier_stats=DeliveryStats(*courier_stats) if courier_stats is not None else None,
                external_pickup_stats=DeliveryStats(*pickup_stats),
                # ?? ???????????????????? ???????????????????? ?????????? ???????????????? ???? ????????
                offer_feed_delivery_option=delivery_info if courier_stats is not None else None,
                offer_feed_pickup_option=delivery_info,
            )

            if courier_stats is not None:
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
                            payment_methods=[
                                PaymentMethod.PT_ALL,
                            ],
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
                            PointTimeInterval(point_id=1, time_from=time(10, 0), time_to=time(22, 0)),
                        ],
                        dsbs_ids=[
                            str(outlet.point_id),
                        ],
                    ),
                ],
                virtual_box=create_virtual_box(weight=18000, length=100, width=80, height=50),
            )

        add_offer_delivery(
            _Offers.long_delivery_offer,
            _Shops.dsbs_shop,
            _DeliveryConstants.long_delivery_info,
            _CombinatorResponseStats.long_courier_stats,
            _CombinatorResponseStats.long_pickup_stats,
            _Outlets.long_delivery_outlet,
        )

        add_offer_delivery(
            _Offers.not_long_delivery_offer,
            _Shops.dsbs_shop,
            _DeliveryConstants.no_long_delivery_info,
            _CombinatorResponseStats.no_long_courier_stats,
            _CombinatorResponseStats.no_long_pickup_stats,
            _Outlets.no_long_delivery_outlet,
        )

        add_offer_delivery(
            _Offers.no_long_delivery_offer_pickup,
            _Shops.dsbs_shop,
            _DeliveryConstants.no_long_delivery_info,
            None,
            _CombinatorResponseStats.no_long_pickup_stats,
            _Outlets.no_long_delivery_outlet,
        )

    def test_long_delivery_offer_filter(self):
        '''
        ??????????????????, ?????? ?????????? ?? ???????????? ?????????????????? ?????????? ?????????? c ???????????? market_long_local_delivery
        '''

        def long_offer_exist(report_response):
            # ??????????????????, ?????????????????????? ?????????????????????? ????????????
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.long_delivery_offer.waremd5,
                    "delivery": {
                        "options": [
                            {
                                "isEstimated": True,
                            }
                        ],
                        "pickupOptions": [
                            {
                                "isEstimated": True,
                            }
                        ],
                    },
                },
                allow_different_len=False,
            )

        def no_long_offer_exist(report_response):
            # ??????????????????, ?????? ?? ?????????????? ?????????? ???????????????????????????? ???????? ???? ????????????????
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.not_long_delivery_offer.waremd5,
                    "delivery": {
                        "options": [
                            {
                                "isEstimated": Absent(),
                            }
                        ],
                        "pickupOptions": [
                            {
                                "isEstimated": Absent(),
                            }
                        ],
                    },
                },
            )

        def long_offer_not_exist(report_response):
            # ??????????????????, ?????? ???????????????????? ?????????? ??????????
            self.assertFragmentNotIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.long_delivery_offer.waremd5,
                },
            )

        def long_offer_pickup_only_exist(report_response):
            # ?????????? ???????????? ?? ???????????????? ???????????? ???????????????? ???? ???????????? ????????????
            # ???? ?????????? ??????????????????????????
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.no_long_delivery_offer_pickup.waremd5,
                },
            )

        for flag, expect in [
            (0, [long_offer_exist, no_long_offer_exist, long_offer_pickup_only_exist]),
            (1, [long_offer_not_exist, no_long_offer_exist, long_offer_pickup_only_exist]),
            (None, [long_offer_not_exist, no_long_offer_exist, long_offer_pickup_only_exist]),
        ]:
            for request in [
                'pickup-options=grouped&place=prime&rids=213&hyperid={hyperid}'.format(hyperid=_Mskus.HYPERID),
                'pickup-options=grouped&place=productoffers&rids=213&hyperid={hyperid}'.format(hyperid=_Mskus.HYPERID),
            ]:
                rearr = "&rearr-factors=" + _RequestConstants.EnableLongLocalDelivery.format(1)
                if flag is not None:
                    rearr += ";" + _RequestConstants.HideLocalDelivery.format(flag)
                request += rearr
                response = self.report.request_json(request)
                for check in expect:
                    check(response)

    def test_actual_delivery_payment_methods(self):
        # ?????????????????? ?????????????? ???????????? ?????? ?????????????? ?? ???????????? ??????????????????
        def long_offer_exist(report_response):
            # ??????????????????, ?????????????????????? ?????????????????????? ????????????
            self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
            self.assertFragmentIn(
                report_response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [{"isEstimated": True, "paymentMethods": ["YANDEX"]}],
                        "pickupOptions": [{"isEstimated": True, "paymentMethods": ["YANDEX"]}],
                    },
                },
                allow_different_len=False,
            )

        def long_offer_not_exist(report_response):
            # ??????????????????, ?????? ?????????? c ???????????? ?????????????????? ??????????
            self.assertFragmentNotIn(
                report_response,
                {
                    "entity": "offer",
                    "wareId": _Offers.long_delivery_offer.waremd5,
                },
            )

        def no_long_offer_check(report_response):
            # ??????????????????, ?????????????????????? ?????????????????????? ????????????
            self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
            self.assertFragmentIn(
                report_response,
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

        for flag, expect in [(1, long_offer_not_exist), (0, long_offer_exist), (None, long_offer_not_exist)]:
            request_tmp = 'total-price=50&pickup-options=grouped&place=actual_delivery&rids=213&offers-list={offerid}:1&rearr-factors=use_dsbs_string_point_ids=1'
            # ???? ?????????????? ?????? ?????????? ?????????????????? ???????????????????? ???????????? ????????????
            # ???????????? ?? ???????? ?????? ?????????? ???? ???????????? ???????????? ????????????????
            # ???????????????? ?????????????????? ???????????? ?? ?????????????????? ???????? "????????????"
            rearr_part = '&rearr-factors=enable_dsbs_combinator_request_in_actual_delivery=1;use_dsbs_combinator_response_in_actual_delivery=1;'
            rearr_part += ";" + _RequestConstants.EnableLongLocalDelivery.format(1)
            if flag is not None:
                rearr_part += ";" + _RequestConstants.HideLocalDelivery.format(flag)
            request_tmp += rearr_part
            # ?????????????????? ?????????? ?? ???????????? ??????????????????, ?? ?????? ?????? ?????????????????? ?????????????? ???? ??????????
            request = request_tmp.format(offerid=_Offers.long_delivery_offer.waremd5)
            response = self.report.request_json(request)
            expect(response)

            # ?????????????????? ?????????? ?????? ???????????? ????????????????, ?? ?????? ?????? ?????????????????? ???? ?????????????? ???? ??????????
            request = request_tmp.format(offerid=_Offers.not_long_delivery_offer.waremd5)
            response = self.report.request_json(request)
            no_long_offer_check(response)


if __name__ == '__main__':
    main()
