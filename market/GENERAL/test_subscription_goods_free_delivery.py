#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime

from core.matcher import Absent
from core.types import (
    Model,
    Shop,
    MarketSku,
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    RegionalDelivery,
    Currency,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicDeliveryServiceInfo,
    DeliveryServiceRegionToRegionInfo,
    TimeIntervalsForRegion,
    TimeIntervalsForDaysInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DateSwitchTimeAndRegionInfo,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    Payment,
)
from core.testcase import TestCase, main
from core.types.delivery import BlueDeliveryTariff
from core.types.offer import OfferDimensions
from core.types.taxes import Vat, Tax
from market.combinator.proto.grpc.combinator_pb2 import DeliveryService, Time
from core.types.combinator import (
    create_delivery_option,
    DeliveryType,
    Destination,
    CombinatorOffer,
    RoutePoint,
    RoutePath,
)


# Tests for MARKETOUT-43720: free delivery for yandex subscription goods

DS_SELF_DELIVERY = 99
DS_COURIER = 210
DS_POST = 300
WARE_ID1 = '1' + '_' * 20 + 'g'
WARE_ID2 = '2' + '_' * 20 + 'g'
MSK_RID = 213
WAREHOUSE_ID = 264


def COURIER_BUCKET(id, ds, rid, days, price):
    return DeliveryBucket(
        bucket_id=id,
        dc_bucket_id=id,
        carriers=[ds],
        regional_options=[
            RegionalDelivery(
                rid=rid,
                options=[DeliveryOption(price=price, day_from=days, day_to=days, shop_delivery_price=5)],
                payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
            )
        ],
        delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
    )


def OUTLET(id, ds, rid, days, price, fesh=None, type=Outlet.FOR_PICKUP, bool_props=[]):
    return (
        Outlet(
            point_id=id,
            delivery_service_id=ds,
            fesh=fesh,
            region=rid,
            point_type=type,
            delivery_option=OutletDeliveryOption(
                shipper_id=ds,
                day_from=days,
                day_to=days,
                order_before=24,
                work_in_holiday=True,
                price=price + 2,  # Эта цена нигде не должна появляться. Сделаем ее отличной от цены в бакете
            ),
            working_days=[i for i in range(10)],
            bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"] + bool_props,
        ),
        PickupBucket(
            bucket_id=id,
            dc_bucket_id=id,
            carriers=[ds if ds else DS_SELF_DELIVERY],
            options=[PickupOption(outlet_id=id, day_from=days, day_to=days, price=price)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        ),
    )


def MAKE_REQUEST(ware_id, rid, rearrFlag=None):
    request = (
        'place=actual_delivery&rgb=blue&offers-list={}:1&rids={}&regset=1&show-subscription-goods=1'
        '&no-delivery-discount=1&pickup-options=grouped&pickup-options-extended-grouping=1&combinator=0'
    )
    if rearrFlag is not None:
        request = request + '&rearr-factors=enable_subscription_goods_free_delivery={}'.format(rearrFlag)
    return request.format(ware_id, rid)


def MAKE_RESP_FRAGMENT(courier_price, pickup_price, post_price, disable_post_as_pickup):
    pickup_options = [{'price': {'value': pickup_price}}]
    post_options = [{'price': {'value': post_price}}]
    return {
        'search': {
            'results': [
                {
                    'delivery': {
                        'options': [{'price': {'value': courier_price}}],
                        'pickupOptions': pickup_options if disable_post_as_pickup else pickup_options + post_options,
                        'postOptions': post_options if disable_post_as_pickup else Absent(),
                    }
                }
            ]
        }
    }


def SERVICE_TIME(hour, minute, day=0):
    return datetime.datetime(year=2021, month=12, day=2 + day, hour=hour, minute=minute)


class T(TestCase):
    @classmethod
    def prepare(cls):
        c_bucket = COURIER_BUCKET(id=2100, ds=DS_COURIER, rid=MSK_RID, days=1, price=620)

        cls.index.delivery_buckets += [c_bucket]

        pickup_outlet, p_bucket = OUTLET(id=2000, ds=DS_SELF_DELIVERY, rid=MSK_RID, days=1, price=600)
        post_outlet, post_bucket = OUTLET(id=3000, ds=DS_POST, type=Outlet.FOR_POST, rid=MSK_RID, days=1, price=600)

        cls.index.pickup_buckets += [p_bucket, post_bucket]

        cls.index.outlets += [pickup_outlet, post_outlet]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RID, holidays_days_set_key=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WAREHOUSE_ID, warehouse_to=WAREHOUSE_ID),
            DynamicDeliveryServiceInfo(
                DS_SELF_DELIVERY,
                "self-delivery",
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=MSK_RID, region_to=MSK_RID, days_key=1)
                ],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=MSK_RID, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=1)]
                    )
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=WAREHOUSE_ID,
                delivery_service_id=DS_SELF_DELIVERY,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=MSK_RID)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=WAREHOUSE_ID,
                delivery_service_id=DS_COURIER,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=MSK_RID)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=WAREHOUSE_ID,
                delivery_service_id=DS_POST,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=MSK_RID)],
            ),
        ]
        cls.index.blue_delivery_modifiers.add_modifier(
            [
                BlueDeliveryTariff(user_price=100, large_size=0),
                BlueDeliveryTariff(user_price=500, large_size=1),
                BlueDeliveryTariff(user_price=1234),
            ],
            regions=[MSK_RID],
            subscription_goods_free_delivery=True,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            [BlueDeliveryTariff(user_price=1234)],
        )
        cls.index.shops += [
            Shop(
                fesh=11317159,
                business_fesh=11317160,
                datafeed_id=1,
                priority_region=MSK_RID,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                delivery_service_outlets=[pickup_outlet.point_id, post_outlet.point_id],
            ),
            Shop(
                fesh=12345,
                datafeed_id=2,
                business_fesh=12346,
                priority_region=MSK_RID,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                warehouse_id=WAREHOUSE_ID,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=431782,
                datafeed_id=100,
                priority_region=213,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]
        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=431782,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_PREPAYMENT_CARD,
                        ],
                    )
                ],
            )
        ]

        cls.index.models += [Model(hyperid=100), Model(hyperid=200)]

        blue_offer_sub = BlueOffer(
            title='subscription offer',
            offerid="sub_offer_id",
            business_id=11317160,
            feedid=1,
            weight=1,
            blue_weight=1,
            price=299,
            vat=Vat.VAT_10,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            blue_dimensions=OfferDimensions(length=20, width=30, height=10),
            cargo_types=[44, 55, 66],
            delivery_buckets=[c_bucket.bucket_id],
            pickup_buckets=[p_bucket.bucket_id],
            post_buckets=[post_bucket.bucket_id],
            post_term_delivery=True,
            waremd5=WARE_ID1,
        )

        blue_offer_non_sub = BlueOffer(
            title='regular offer',
            business_id=12346,
            feedid=2,
            weight=1,
            blue_weight=1,
            price=199,
            vat=Vat.VAT_10,
            dimensions=OfferDimensions(length=20, width=30, height=10),
            blue_dimensions=OfferDimensions(length=20, width=30, height=10),
            cargo_types=[44, 55, 66],
            delivery_buckets=[c_bucket.bucket_id],
            waremd5=WARE_ID2,
        )

        cls.index.mskus += [
            MarketSku(hyperid=100, sku=1001, blue_offers=[blue_offer_sub]),
            MarketSku(hyperid=200, sku=2001, blue_offers=[blue_offer_non_sub]),
        ]

        endPointRegion = RoutePoint(
            point_ids=Destination(region_id=MSK_RID),
            segment_id=512005,
            segment_type="handing",
            services=(
                (
                    DeliveryService.OUTBOUND,
                    "HANDING",
                    SERVICE_TIME(20, 5),
                    datetime.timedelta(minutes=15),
                    (Time(hour=10), Time(hour=22, minute=30)),
                ),
            ),
        )

        deliveryDateFrom = datetime.date(year=2021, month=12, day=2)
        deliveryDateTo = datetime.date(year=2021, month=12, day=12)
        deliveryTimeFrom = datetime.time(hour=10, minute=0)
        deliveryTimeTo = datetime.time(hour=22, minute=30)
        warehouse = RoutePoint(
            point_ids=Destination(partner_id=WAREHOUSE_ID),
            segment_id=512001,
            segment_type="warehouse",
            services=(
                (DeliveryService.INTERNAL, "PROCESSING", SERVICE_TIME(13, 25), datetime.timedelta(hours=2)),
                (DeliveryService.OUTBOUND, "SHIPMENT", SERVICE_TIME(15, 25), datetime.timedelta(minutes=35)),
            ),
            partner_type="FULFILLMENT",
        )
        simplePath = [RoutePath(point_from=0, point_to=1)]
        combOffer = CombinatorOffer(
            shop_sku=blue_offer_sub.offerid, shop_id=11317159, partner_id=WAREHOUSE_ID, available_count=3
        )
        cls.combinator.on_delivery_route_request(
            delivery_type=DeliveryType.COURIER,
            destination=endPointRegion,
            delivery_option=create_delivery_option(
                date_from=deliveryDateFrom,
                date_to=deliveryDateTo,
                time_from=deliveryTimeFrom,
                time_to=deliveryTimeTo,
            ),
            total_price=blue_offer_sub.price,
        ).respond_with_delivery_route(
            offers=[
                combOffer,
            ],
            points=[
                warehouse,
                endPointRegion,
            ],
            paths=simplePath,
            date_from=deliveryDateFrom,
            date_to=deliveryDateTo,
            shipment_warehouse=WAREHOUSE_ID,
        )

    def test_subscription_goods_free_courier_delivery(self):
        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        for rearr in (None, 1):
            response = self.report.request_json(
                MAKE_REQUEST(WARE_ID1, MSK_RID, rearr) + '&debug=1' + disable_post_as_pickup_rearr
            )
            self.assertFragmentIn(
                response,
                MAKE_RESP_FRAGMENT(
                    courier_price="0",
                    pickup_price="0",
                    post_price="0",
                    disable_post_as_pickup=disable_post_as_pickup_rearr,
                ),
                allow_different_len=False,
            )
            # у магазина заданы опции оплаты после доставки, но для офферов по подписке они не должны показываться
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'payments': {
                        'deliveryCard': False,
                        'deliveryCash': False,
                        'prepaymentCard': True,
                    },
                },
            )
            self.assertFragmentIn(response, 'Removing post-delivery payment method for meidaservises offer')

    def test_subscription_goods_delivery_route(self):
        request = (
            'place=delivery_route&feedid=1&rids=213&adult=1&delivery-type=courier&cost=0&payments=prepayment_card'
            '&delivery-interval=20211202.1000-20211212.2230'
            '&delivery-subtype=ordinary&show-promoted=0&client=checkout&rgb=blue&offers-list={}:1&show-preorder=1&ignore-has-gone=0&show-subscription-goods=1&use-virt-shop=0'
        ).format(WARE_ID1)
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'search': {'results': [{'delivery': {'option': {'price': {'value': '0'}}}}]}})

    def test_non_subscription_goods_non_free_courier_delivery(self):
        response = self.report.request_json(MAKE_REQUEST(WARE_ID2, MSK_RID, 1))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'delivery': {
                                'options': [{'price': {'value': "100"}}],
                            }
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_subscription_goods_non_free_courier_delivery_with_false_rearr(self):
        disable_post_as_pickup_rearr = '&rearr-factors=market_use_post_as_pickup=0'
        for disable_post_as_pickup in ['', disable_post_as_pickup_rearr]:
            response = self.report.request_json(MAKE_REQUEST(WARE_ID1, MSK_RID, 0) + disable_post_as_pickup)
            self.assertFragmentIn(
                response,
                MAKE_RESP_FRAGMENT(
                    courier_price="100",
                    pickup_price="100",
                    post_price="100",
                    disable_post_as_pickup=disable_post_as_pickup,
                ),
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
