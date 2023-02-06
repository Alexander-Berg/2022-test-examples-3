#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    GpsCoord,
    MarketSku,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    Payment,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    Shop,
    Tax,
    Vat,
)

from core.testcase import TestCase, main
from core.types.postpayment_price_thresholds import PostpaymentPriceThreshold


DELIVERY_SERVICE_ID = 157
POST_CODE = 115202


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[2001, 3001],
    )
    supplier_3p = Shop(
        fesh=2,
        datafeed_id=2,
        warehouse_id=1,
        name="3P поставщик",
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        fulfillment_program=True,
    )


class _Offers(object):
    def _create_offer(id, price):
        return BlueOffer(
            price=price,
            vat=Vat.VAT_10,
            feedid=_Shops.supplier_3p.datafeed_id,
            waremd5=id,
            weight=2.5,
            dimensions=OfferDimensions(length=10, width=20, height=30),
        )

    cheap_offer = _create_offer('Cheapo_______________g', price=500)
    expensive_offer = _create_offer('Luxurious____________g', price=50000)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213, name="Москва", region_type=Region.CITY),
            Region(rid=2, name="Питер", region_type=Region.CITY),
        ]

        cls.index.shops += [_Shops.blue_virtual_shop, _Shops.supplier_3p]

        cls.index.mskus += [
            MarketSku(
                sku=1,
                hyperid=1,
                blue_offers=[_Offers.cheap_offer, _Offers.expensive_offer],
                delivery_buckets=[1000],
                pickup_buckets=[2000],
                post_buckets=[3000],
            )
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1000,
                dc_bucket_id=1000,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

        cls.index.pickup_buckets += [
            # pickup
            PickupBucket(
                bucket_id=2000,
                dc_bucket_id=2000,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=4, price=200)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # post
            PickupBucket(
                bucket_id=3000,
                dc_bucket_id=3000,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                options=[PickupOption(outlet_id=3001, day_from=2, day_to=3, price=150)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=DELIVERY_SERVICE_ID,
                    day_from=1,
                    day_to=1,
                    order_before=2,
                    work_in_holiday=True,
                    price=100,
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=3001,
                delivery_service_id=DELIVERY_SERVICE_ID,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=POST_CODE,
                delivery_option=OutletDeliveryOption(shipper_id=DELIVERY_SERVICE_ID, day_from=1, day_to=1, price=500),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=1, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1, warehouse_to=1),
        ]

        cls.index.postpayment_price_thresholds += [
            PostpaymentPriceThreshold(1000, 0, "DeliveryService", DELIVERY_SERVICE_ID)
        ]

    def check_postpayment_available(self, offer, additional_params, postpayment_available):
        offers_list = '&offers-list={}:1'.format(offer.waremd5)
        pickup = '&pickup-options=grouped&pickup-options-extended-grouping=1'
        post = '&post-index={}'.format(POST_CODE)
        no_combinator = '&combinator=0'
        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        expected_payment_methods = ["YANDEX"]
        if postpayment_available:
            expected_payment_methods.extend(["CASH_ON_DELIVERY", "CARD_ON_DELIVERY"])

        pickup_options = [
            {
                "serviceId": DELIVERY_SERVICE_ID,
                "outletIds": [2001],
                "paymentMethods": expected_payment_methods,
            }
        ]
        post_options = [
            {
                "serviceId": DELIVERY_SERVICE_ID,
                "outletIds": [3001],
                "postCodes": [POST_CODE],
                "paymentMethods": expected_payment_methods,
            }
        ]
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            response = self.report.request_json(
                'rgb=blue&place=actual_delivery&rids=213&'
                + offers_list
                + pickup
                + post
                + no_combinator
                + additional_params
                + disable_post_as_pickup
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "offers": [{"wareId": offer.waremd5}],
                    "delivery": {
                        "options": [
                            {
                                "serviceId": str(DELIVERY_SERVICE_ID),
                                "shipmentDay": 0,
                                "paymentMethods": expected_payment_methods,
                            }
                        ],
                        "pickupOptions": pickup_options if disable_post_as_pickup else pickup_options + post_options,
                        "postOptions": post_options if disable_post_as_pickup else Absent(),
                    },
                },
                allow_different_len=False,
            )

    def test_threshold(self):
        '''
        Без превышения порога постоплата доступна, при превышении (expensive_offer) убираем её из всех способов доставки
        '''
        for flag_threshold_enabled in ('', '&rearr-factors=market_postpayment_price_limit=1'):
            self.check_postpayment_available(_Offers.cheap_offer, flag_threshold_enabled, True)
            self.check_postpayment_available(_Offers.expensive_offer, flag_threshold_enabled, False)

        flag_threshold_disabled = '&rearr-factors=market_postpayment_price_limit=0'
        for offer in (_Offers.cheap_offer, _Offers.expensive_offer):
            self.check_postpayment_available(offer, flag_threshold_disabled, True)


if __name__ == '__main__':
    main()
