#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.combinator import DeliveryStats, make_offer_id
from core.matcher import Absent
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    DeliveryBucket,
    DeliveryOption,
    DsbsCourierRule,
    DsbsPickupRule,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
)

"""Тесты про nordstream для dsbs.
Это фича:
1) market_nordstream_dsbs=1
"""

REGION_MOSCOW = 213
REGION_PITER = 2


def build_request(**kwds):
    kvlist = ['{}={}'.format(key.replace('_', '-'), kwds[key]) for key in kwds]
    return '&'.join(kvlist)


def make_rearr(**kwds):
    kvlist = ['{}={}'.format(key, kwds[key]) for key in kwds]
    kvlist.sort(key=lambda x: x[0])
    return ';'.join(kvlist)


def make_dsbs_shop(fesh, warehouse, **kwds):
    return Shop(
        business_fesh=fesh,
        datafeed_id=fesh,
        fesh=fesh,
        cpa=Shop.CPA_REAL,
        is_dsbs=True,
        name='shop dsbs {}'.format(fesh),
        warehouse_id=warehouse,
        **kwds
    )


def make_dsbs_offer(idx, fesh, **kwds):
    return Offer(
        title='offer dsbs {}'.format(idx),
        offerid='offer dsbs {}'.format(idx),
        price=111,
        weight=5,
        feedid=fesh,
        fesh=fesh,
        business_id=fesh,
        cpa=Offer.CPA_REAL,
        waremd5=('offer_dsbs_{}'.format(idx).ljust(20, '-') + 'eg')[:22],
        **kwds
    )


class Spec:
    Number = 0

    def __init__(self, **kwds):
        Spec.Number += 111
        num = Spec.Number

        self.fesh = num
        self.msku = num
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
        self.shop = make_dsbs_shop(
            fesh=self.fesh,
            warehouse=self.warehouse,
            priority_region=self.shop_region,
        )

        self.delivery_bucket = None
        if self.bucket_courier_days:
            self.delivery_bucket = DeliveryBucket(
                bucket_id=self.delivery_bucket_id,
                fesh=self.fesh,
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

        self.offer = make_dsbs_offer(
            idx=self.fesh,
            fesh=self.fesh,
            sku=self.msku,
            delivery_buckets=[self.delivery_bucket_id] if self.delivery_bucket else None,
            pickup_buckets=[self.pickup_bucket_id] if self.pickup_bucket else None,
            has_delivery_options=False,
        )

    def prepare(self, cls):
        cls.index.shops += [self.shop]
        cls.index.offers += [self.offer]

        # bucket
        if self.delivery_bucket:
            cls.index.delivery_buckets += [self.delivery_bucket]
        if self.pickup_bucket:
            cls.index.pickup_buckets += [self.pickup_bucket]
            cls.index.outlets += [self.outlet]

        # nordstream
        if self.ns_region:
            if self.ns_courier_from_feed:
                cls.dynamic.nordstream.append(DsbsCourierRule(self.warehouse, self.ns_region, is_from_feed=True))
            if self.ns_courier_days:
                cls.dynamic.nordstream.append(
                    DsbsCourierRule(
                        warehouse_id=self.warehouse,
                        region_id=self.ns_region,
                        min_days=self.ns_courier_days,
                        max_days=self.ns_courier_days,
                    )
                )
            if self.ns_pickup_days:
                cls.dynamic.nordstream.append(
                    DsbsPickupRule(
                        warehouse_id=self.warehouse,
                        region_id=self.ns_region,
                        min_days=self.ns_pickup_days,
                        max_days=self.ns_pickup_days,
                    )
                )

        # combinator
        cstats = (
            DeliveryStats(cost=15, day_from=self.combi_courier_days, day_to=self.combi_courier_days)
            if self.combi_courier_days
            else None
        )
        pstats = (
            DeliveryStats(cost=20, day_from=self.combi_pickup_days, day_to=self.combi_pickup_days)
            if self.combi_pickup_days
            else None
        )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(self.offer, self.shop),
            courier_stats=cstats,
            external_pickup_stats=pstats,
        )


spec_diff_delivery = Spec(
    # bucket: доставка только в Питер.
    bucket_region=REGION_PITER,
    bucket_courier_days=1,
    bucket_pickup_days=2,
    # nordstream: доставка только в Москву.
    ns_region=REGION_MOSCOW,
    ns_courier_days=3,
    ns_pickup_days=4,
    # сроки доставки от комбинатора.
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
spec_yml_courier = Spec(
    bucket_region=REGION_MOSCOW,
    bucket_courier_days=1,
    # nordstream: курьерка в yml/feed.
    ns_region=REGION_MOSCOW,
    ns_courier_from_feed=True,
    # Сроки доставки из combinator.
    combi_courier_days=11,
    combi_pickup_days=22,
)
spec_yml_courier_and_pickup = Spec(
    bucket_region=REGION_MOSCOW,
    bucket_courier_days=1,
    bucket_pickup_days=2,
    # nordstream
    ns_region=REGION_MOSCOW,
    ns_courier_from_feed=True,
    ns_pickup_days=3,
    # Сроки доставки из combinator.
    combi_courier_days=11,
    combi_pickup_days=22,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False

        cls.settings.check_combinator_errors = True
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

        spec_diff_delivery.prepare(cls)
        spec_same_delivery.prepare(cls)
        spec_only_nordstream_delivery.prepare(cls)
        spec_only_bucket_delivery.prepare(cls)
        spec_yml_courier.prepare(cls)
        spec_yml_courier_and_pickup.prepare(cls)

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
        """place=productoffers делает поход в combinator.GetOffersDeliveryStats.
        Поэтому доставка в nordstream влияет только на фильтрацию.
        Сроки доставки будут использованы из combinator.
        """
        dsbs_rel_region = [
            (spec_diff_delivery, 0, REGION_PITER, True),  # Используем бакеты, есть доставка в Питер
            (spec_diff_delivery, 0, REGION_MOSCOW, False),  # Используем бакеты, но в них НЕТ доставки в Москву.
            (spec_diff_delivery, 1, REGION_MOSCOW, True),  # Используем nordstream, есть доставка в Москву.
            (spec_same_delivery, 0, REGION_MOSCOW, True),
            (spec_same_delivery, 1, REGION_MOSCOW, True),
            (spec_only_nordstream_delivery, 0, REGION_MOSCOW, False),  # Используем бакеты, нет доставки.
            (spec_only_nordstream_delivery, 1, REGION_MOSCOW, True),
            (spec_only_bucket_delivery, 0, REGION_MOSCOW, True),
            (spec_only_bucket_delivery, 1, REGION_MOSCOW, False),  # Используем nordstream, нет доставки.
            (spec_yml_courier, 0, REGION_MOSCOW, True),
            (spec_yml_courier, 1, REGION_MOSCOW, True),  # nordstream: курьерка в feed => fallback бакеты.
            (spec_yml_courier_and_pickup, 0, REGION_MOSCOW, True),
            (spec_yml_courier_and_pickup, 1, REGION_MOSCOW, True),  # nordstream: курьерка в feed => fallback бакеты.
        ]
        for spec, nordstream_dsbs, region, want in dsbs_rel_region:
            response = self.report.request_json(
                build_request(
                    place='productoffers',
                    offerid=spec.offer.waremd5,
                    rids=region,
                    regset=2,
                    pickup_options='grouped',
                    market_sku=spec.msku,
                    combinator=1,
                    pp=18,
                    debug=1,
                    rearr_factors=make_rearr(
                        market_nordstream_dsbs=nordstream_dsbs,
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
                        "hasPost": False,
                        "options": [
                            {
                                "dayFrom": spec.combi_courier_days,  # Сроки доставки из combinator.
                                "dayTo": spec.combi_courier_days,
                                "serviceId": "99",
                            }
                        ],
                        "pickupOptions": [
                            {
                                "dayFrom": spec.combi_pickup_days,
                                "dayTo": spec.combi_pickup_days,
                                "serviceId": 99,
                            }
                        ],
                    }
                },
            )

    def test_offerinfo(self):
        """place=offerinfo НЕ делает поход в combinator.GetOffersDeliveryStats.
        Поэтому доставка используется только из nordstream.
        """
        BUCKET = 0  # courier & pickup из бакетов.
        NORDSTREAM = 1  # courier из nordstream, pickup=true но без сроков.
        BUCKET_NONE = 2  # courier из yml/feed, pickup нет.
        tests = [
            (spec_diff_delivery, 0, REGION_MOSCOW, None),  # Бакеты, нет доставки в Москву.
            (spec_diff_delivery, 0, REGION_PITER, BUCKET),  # Бакеты, есть доставка в Питер.
            (spec_diff_delivery, 1, REGION_MOSCOW, NORDSTREAM),
            (spec_diff_delivery, 1, REGION_PITER, None),
            (spec_same_delivery, 0, REGION_MOSCOW, BUCKET),
            (spec_same_delivery, 1, REGION_MOSCOW, NORDSTREAM),
            (spec_only_nordstream_delivery, 0, REGION_MOSCOW, None),
            (spec_only_nordstream_delivery, 1, REGION_MOSCOW, NORDSTREAM),
            (spec_only_bucket_delivery, 0, REGION_MOSCOW, BUCKET),
            (spec_only_bucket_delivery, 1, REGION_MOSCOW, None),
            (spec_yml_courier, 0, REGION_MOSCOW, BUCKET_NONE),
            (spec_yml_courier, 1, REGION_MOSCOW, BUCKET_NONE),
            (spec_yml_courier_and_pickup, 0, REGION_MOSCOW, BUCKET),
            (spec_yml_courier_and_pickup, 1, REGION_MOSCOW, BUCKET),
        ]
        for spec, nordstream_dsbs, region, want in tests:
            response = self.report.request_json(
                build_request(
                    place='offerinfo',
                    debug=1,
                    pickup_options='grouped',
                    regset=2,
                    show_urls='direct',
                    rids=region,
                    offerid=spec.offer.waremd5,
                    rearr_factors=make_rearr(
                        market_nordstream_dsbs=nordstream_dsbs,
                    ),
                )
            )
            if want is None:
                self.assert_0_offer(response)
                continue
            self.assert_1_offer(response)
            if want == BUCKET:
                self.assertFragmentIn(
                    response,
                    {
                        "delivery": {
                            "hasPickup": True,
                            "hasPost": False,
                            "options": [
                                {
                                    "dayFrom": spec.bucket_courier_days,  # Сроки из bucket.
                                    "dayTo": spec.bucket_courier_days,
                                    "serviceId": "99",
                                    "price": {"currency": "RUR", "value": "99"},
                                }
                            ],
                            "pickupOptions": [
                                {
                                    "dayFrom": spec.bucket_pickup_days,
                                    "dayTo": spec.bucket_pickup_days,
                                    "serviceId": 99,
                                    "price": {"currency": "RUR", "value": "0"},
                                }
                            ],
                        },
                    },
                )
            elif want == NORDSTREAM:
                self.assertFragmentIn(
                    response,
                    {
                        "delivery": {
                            "hasPickup": True,
                            "hasPost": False,
                            "options": [
                                {
                                    "dayFrom": spec.ns_courier_days,  # Сроки из nordstream.
                                    "dayTo": spec.ns_courier_days,
                                    "serviceId": "99",
                                    "price": {"currency": "RUR", "value": "99"},
                                }
                            ],
                            "pickupOptions": [
                                {
                                    "dayFrom": spec.ns_pickup_days,  # Сроки из nordstream.
                                    "dayTo": spec.ns_pickup_days,
                                    "serviceId": 99,
                                    "price": {"currency": "RUR", "value": "0"},
                                }
                            ],
                        },
                    },
                )
            elif want == BUCKET_NONE:
                self.assertFragmentIn(
                    response,
                    {
                        "delivery": {
                            "hasPickup": False,
                            "hasPost": False,
                            "options": [
                                {
                                    "dayFrom": spec.bucket_courier_days,
                                    "dayTo": spec.bucket_courier_days,
                                    "serviceId": "99",
                                }
                            ],
                            "pickupOptions": Absent(),
                        }
                    },
                )


if __name__ == '__main__':
    main()
