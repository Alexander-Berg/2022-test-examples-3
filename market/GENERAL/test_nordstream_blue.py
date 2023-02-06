#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.combinator import DeliveryStats, make_offer_id
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    MarketSku,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
)
from core.types.offer import OfferDimensions


def build_request(**kwds):
    kvlist = ['{}={}'.format(key.replace('_', '-'), kwds[key]) for key in kwds]
    return '&'.join(kvlist)


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


REGION_MOSCOW = 213
REGION_PITER = 2


def make_blue_offer(idx, fesh, **kwds):
    return BlueOffer(
        title='offer blue {}'.format(idx),
        offerid='offer blue {}'.format(idx),
        waremd5=('offer_blue_{}'.format(idx).ljust(20, '-') + 'eg')[:22],
        feedid=fesh,
        fesh=fesh,
        business_id=fesh,
        weight=5,
        dimensions=OfferDimensions(length=10, width=20, height=30),
        **kwds
    )


class Spec:
    Number = 0

    def __init__(self, **kwds):
        Spec.Number += 111
        num = Spec.Number

        self.fesh = num
        self.msku = num
        self.hyperid = num
        self.warehouse = num
        self.delivery_bucket_id = num
        self.pickup_bucket_id = num
        self.outlet_id = num
        # shop
        self.shop_region = REGION_MOSCOW
        # Доставка из бакетов.
        self.bucket_region = REGION_MOSCOW
        self.bucket_courier_days = None
        self.bucket_pickup_days = None
        # Доставка из nordstream.
        self.ns_region = REGION_MOSCOW
        self.ns_courier_days = None
        self.ns_pickup_days = None
        self.ns_courier_from_feed = False
        # Доставка из combinator.
        self.combi_courier_days = None
        self.combi_pickup_days = None

        for key in kwds:
            self.__dict__[key] = kwds[key]

        self._make()

    def _make(self):
        self.shop = Shop(
            business_fesh=self.fesh,
            datafeed_id=self.fesh,
            fesh=self.fesh,
            blue=Shop.BLUE_REAL,
            supplier_type=Shop.THIRD_PARTY,
            name='shop blue {}'.format(self.fesh),
            warehouse_id=self.warehouse,
            priority_region=self.shop_region,
            regions=[self.shop_region],
        )

        self.delivery_bucket = None
        if self.bucket_courier_days:
            self.delivery_bucket = DeliveryBucket(
                bucket_id=self.delivery_bucket_id,
                fesh=self.fesh,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=self.bucket_region,
                        options=[DeliveryOption(day_from=self.bucket_courier_days, day_to=self.bucket_courier_days)],
                    )
                ],
            )

        self.outlet = Outlet(
            point_id=self.outlet_id,
            fesh=self.fesh,
            region=self.bucket_region,
            point_type=Outlet.MIXED_TYPE,
            delivery_option=OutletDeliveryOption(),
            working_days=[i for i in range(100)],
        )

        self.pickup_bucket = None
        if self.bucket_pickup_days:
            self.pickup_bucket = PickupBucket(
                bucket_id=self.pickup_bucket_id,
                fesh=self.fesh,
                options=[
                    PickupOption(
                        outlet_id=self.outlet_id,
                        day_from=self.bucket_pickup_days,
                        day_to=self.bucket_pickup_days,
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            )

        self.offer = make_blue_offer(
            idx=self.fesh,
            fesh=self.fesh,
            sku=self.msku,
            delivery_buckets=[self.delivery_bucket_id] if self.delivery_bucket else None,
            pickup_buckets=[self.pickup_bucket_id] if self.pickup_bucket else None,
            has_delivery_options=False,
        )

    def prepare(self, cls):
        cls.index.shops += [self.shop]
        # cls.index.offers += [self.offer]
        cls.index.mskus += [
            MarketSku(
                title='msku {}'.format(self.msku),
                hyperid=self.hyperid,
                sku=self.msku,
                blue_offers=[
                    self.offer,
                ],
            ),
        ]

        # bucket
        if self.delivery_bucket:
            cls.index.delivery_buckets += [self.delivery_bucket]
        if self.pickup_bucket:
            cls.index.pickup_buckets += [self.pickup_bucket]
            cls.index.outlets += [self.outlet]

        # nordstream
        if self.ns_region:
            rules = []
            if self.ns_courier_days:
                rules.append(
                    DynamicDeliveryRestriction(
                        min_days=self.ns_courier_days,
                        max_days=self.ns_courier_days,
                        delivery_service_id=42,
                    )
                )
            if self.ns_pickup_days:
                rules.append(
                    DynamicDeliveryRestriction(
                        min_days=self.ns_pickup_days,
                        max_days=self.ns_pickup_days,
                        delivery_type=1,  # pickup
                        delivery_service_id=42,
                    )
                )
            if rules:
                cls.dynamic.nordstream.append(DynamicWarehouseLink(self.warehouse, [self.warehouse]))
                cls.dynamic.nordstream.append(DynamicWarehouseDelivery(self.warehouse, {self.ns_region: rules}))

        # combinator
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(self.offer, self.shop),
            courier_stats=DeliveryStats(cost=15, day_from=self.combi_courier_days, day_to=self.combi_courier_days)
            if self.combi_courier_days
            else None,
            external_pickup_stats=DeliveryStats(cost=20, day_from=self.combi_pickup_days, day_to=self.combi_pickup_days)
            if self.combi_pickup_days
            else None,
        )


spec_diff_delivery = Spec(
    bucket_region=REGION_PITER,
    bucket_courier_days=1,
    bucket_pickup_days=2,
    ns_region=REGION_MOSCOW,
    ns_courier_days=3,
    ns_pickup_days=4,
    combi_courier_days=11,
    combi_pickup_days=22,
)
spec_same_delivery = Spec(
    bucket_region=REGION_MOSCOW,
    bucket_courier_days=1,
    bucket_pickup_days=2,
    ns_region=REGION_MOSCOW,
    ns_courier_days=3,
    ns_pickup_days=4,
    combi_courier_days=11,
    combi_pickup_days=22,
)
spec_only_nordstream_delivery = Spec(
    ns_region=REGION_MOSCOW,
    ns_courier_days=3,
    ns_pickup_days=4,
    combi_courier_days=11,
    combi_pickup_days=22,
)
spec_only_bucket_delivery = Spec(
    bucket_region=REGION_MOSCOW,
    bucket_courier_days=1,
    bucket_pickup_days=2,
    combi_courier_days=11,
    combi_pickup_days=22,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False

        cls.settings.check_combinator_errors = True
        cls.combinator.set_start_date(microseconds_for_disabled_random=REQUEST_TIMESTAMP * 10**6)

        spec_diff_delivery.prepare(cls)
        spec_same_delivery.prepare(cls)
        spec_only_nordstream_delivery.prepare(cls)
        spec_only_bucket_delivery.prepare(cls)

    def assert_0_offer(self, response):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 0,
                    "totalOffers": 0,
                }
            },
        )

    def assert_1_offer(self, response):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "totalOffers": 1,
                }
            },
        )

    def test_productoffers(self):
        spec_feature_region_want = [
            (spec_diff_delivery, REGION_MOSCOW, True),
            (spec_diff_delivery, REGION_PITER, False),
            (spec_same_delivery, REGION_MOSCOW, True),
            (spec_only_nordstream_delivery, REGION_MOSCOW, True),
            (spec_only_bucket_delivery, REGION_MOSCOW, False),
        ]
        for spec, region, want in spec_feature_region_want:
            response = self.report.request_json(
                build_request(
                    combinator=1,
                    debug=1,
                    market_sku=spec.msku,
                    offerid=spec.offer.waremd5,
                    pickup_options='grouped',
                    place='productoffers',
                    pp=18,
                    regset=2,
                    rids=region,
                    rearr_factors=make_rearr(
                        market_nordstream=1,
                        market_nordstream_relevance=1,
                    ),
                )
            )
            if not want:
                self.assert_0_offer(response)
                continue
            self.assert_1_offer(response)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "hasPickup": True,
                        "options": [
                            {
                                "dayFrom": spec.combi_courier_days,
                                "dayTo": spec.combi_courier_days,
                                "serviceId": "99",
                                "partnerType": "market_delivery",
                            }
                        ],
                        "pickupOptions": [
                            {
                                "serviceId": 99,
                                "dayFrom": spec.combi_pickup_days,
                                "dayTo": spec.combi_pickup_days,
                            }
                        ],
                    }
                },
            )

    def test_offerinfo(self):
        spec_feature_region_want = [
            (spec_diff_delivery, REGION_MOSCOW, True),
            (spec_diff_delivery, REGION_PITER, False),
            (spec_same_delivery, REGION_MOSCOW, True),
            (spec_only_nordstream_delivery, REGION_MOSCOW, True),
            (spec_only_bucket_delivery, REGION_MOSCOW, False),
        ]
        for spec, region, want in spec_feature_region_want:
            response = self.report.request_json(
                build_request(
                    debug=1,
                    offerid=spec.offer.waremd5,
                    pickup_options='grouped',
                    place='offerinfo',
                    regset=2,
                    rids=region,
                    show_urls='direct',
                    rearr_factors=make_rearr(
                        market_nordstream=1,
                        market_nordstream_relevance=1,
                    ),
                )
            )
            if not want:
                self.assert_0_offer(response)
                continue
            self.assert_1_offer(response)
            self.assertFragmentIn(
                response,
                {
                    "delivery": {
                        "hasPickup": True,
                        "options": [
                            {
                                "dayFrom": spec.ns_courier_days,  # Сроки из nordstream.
                                "dayTo": spec.ns_courier_days,
                                "serviceId": "42",
                                "partnerType": "regular",
                            }
                        ],
                        "pickupOptions": [
                            {
                                "dayFrom": spec.ns_pickup_days,  # Сроки из nordstream.
                                "dayTo": spec.ns_pickup_days,
                                "serviceId": 42,
                            }
                        ],
                    },
                },
            )


if __name__ == '__main__':
    main()
