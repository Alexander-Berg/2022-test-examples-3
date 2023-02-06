#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DeliveryServiceRegionToRegionInfo,
    DynamicBlueGenericBundlesPromos,
    DynamicDaysSet,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    OfferDimensions,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Promo,
    PromoType,
    RegionalDelivery,
    Shop,
    TimeInfo,
    TimeIntervalInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.matcher import Absent
from core.types.offer_promo import make_generic_bundle_content


# alt_reason            = (reason,           details)
CHEAPER = ('cheaper', Absent())
FASTER = ('faster', Absent())
GIFT = ('gift', Absent())
DELIVERY_TYPE_PICKUP = ('delivery_type', 'PICKUP')
DELIVERY_TYPE_COURIER = ('delivery_type', 'DELIVERY')
DELIVERY_TYPE_POST = ('delivery_type', 'POST')
ABSENT_REASON = (Absent(), Absent())


class _Shops(object):
    blue_virtual_shop = Shop(
        fesh=1,
        datafeed_id=1,
        client_id=1,
        fulfillment_virtual=True,
        name='virtual_shop',
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        priority_region=213,
        tax_system=Tax.OSN,
        cpa=Shop.CPA_REAL,
        delivery_service_outlets=[1010, 2020, 3030] + [4000 + i for i in range(2000)],
    )

    supplier_3p = Shop(
        fesh=3,
        datafeed_id=3,
        warehouse_id=1,
        fulfillment_program=True,
        name="3p-feed of crossdock supplier",
        priority_region=213,
        tax_system=Tax.OSN,
        client_id=3,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )

    dropship_shop = Shop(
        fesh=5,
        datafeed_id=5,
        warehouse_id=5,
        fulfillment_program=False,
        ignore_stocks=False,
        name="Dropship",
        client_id=5,
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )


class _Offers(object):
    OFFER_DIMENSIONS = OfferDimensions(length=10, width=20, height=30)

    fast_delivery = BlueOffer(
        price=109,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        weight=10,
        dimensions=OFFER_DIMENSIONS,
        waremd5='FastFastFast_________g',
    )

    cheapest = BlueOffer(
        price=50,
        vat=Vat.VAT_10,
        feedid=_Shops.dropship_shop.datafeed_id,
        delivery_buckets=[805],
        weight=10,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CheapAsChips_________g',
    )

    any_delivery_type = BlueOffer(
        price=104,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='DeliveryDelivery_____g',
        pickup_buckets=[1012, 2022, 4040],
        delivery_buckets=[806],
        post_buckets=[3030],
        post_term_delivery=True,
    )

    with_gift = BlueOffer(
        price=110,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[805],
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='WithGift_____________g',
        offerid='offer_with_gift',
    )

    secondary_offer = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[805],
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Secondary____________g',
        offerid='secondary_offer',
    )


class _SKUs(object):

    full_house = MarketSku(
        title="FullHouse",
        hyperid=5,
        sku=5,
        blue_offers=[_Offers.any_delivery_type, _Offers.fast_delivery, _Offers.cheapest, _Offers.with_gift],
    )


class _Promos(object):
    all_promos = [
        Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=offer.feedid,
            key='JVvklxUgdnawSJPG4UhZ-{}'.format(num),
            url='http://localhost.ru/',
            generic_bundles_content=[
                make_generic_bundle_content(offer.offerid, _Offers.secondary_offer.offerid, 1),
            ],
        )
        for num, offer in enumerate([_Offers.with_gift])
    ]


class T(TestCase):
    @classmethod
    def prepare(cls):
        def warehouse_and_delivery_service(warehouse_id):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=warehouse_id,
                delivery_service_id=157,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=22, region_to=213, packaging_time=TimeInfo(3))
                ],
            )

        for num, offer in enumerate([_Offers.with_gift]):
            offer.promo = _Promos.all_promos[num]

        cls.settings.report_subrole = 'blue-main'
        cls.settings.lms_autogenerate = False
        cls.settings.nordstream_autogenerate = False
        cls.settings.loyalty_enabled = True
        cls.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0;market_nordstream_relevance=0'
        ]

        cls.index.shops += [
            _Shops.supplier_3p,
            _Shops.dropship_shop,
            _Shops.blue_virtual_shop,
        ]
        cls.index.promos += _Promos.all_promos

        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in _Promos.all_promos])]

        cls.index.mskus += [
            _SKUs.full_house,
        ]

        for warehouse_id in (1, 5):
            cls.dynamic.lms.append(DynamicWarehouseInfo(id=warehouse_id, home_region=213, holidays_days_set_key=1))

        cls.dynamic.lms += [
            DynamicDaysSet(key=1, days=[1]),
            DynamicTimeIntervalsSet(key=1, intervals=[TimeIntervalInfo(TimeInfo(23, 0), TimeInfo(23, 45))]),
            DynamicDeliveryServiceInfo(
                157,
                "c_157",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=1)]
                    )
                ],
            ),
            warehouse_and_delivery_service(_Shops.supplier_3p.warehouse_id),
            warehouse_and_delivery_service(_Shops.dropship_shop.warehouse_id),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225], warehouse_with_priority=[WarehouseWithPriority(i, 100) for i in (1, 5)]
            )
        ]

        cls.index.outlets += [
            Outlet(
                point_id=1010,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2020,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=1, day_to=1, price=150),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=3030,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115230,
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=2, day_to=4, price=400),
                working_days=[i for i in range(30)],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=4000 + i,
                delivery_service_id=157,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=157, day_from=1, day_to=4, price=400),
                working_days=list(range(20)),
            )
            for i in range(2000)
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=800 + fesh,
                fesh=fesh,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=5, day_from=fesh - 1, day_to=fesh)])
                ],
            )
            for fesh in (1, 5)
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=900,
                fesh=2,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=0, day_to=0)])],
            )
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=1010 + fesh,
                dc_bucket_id=40 + fesh,
                fesh=fesh,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[PickupOption(outlet_id=1010, day_from=1, day_to=2, price=5)],
            )
            for fesh in (1, 5)
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=2020 + fesh,
                dc_bucket_id=50 + fesh,
                fesh=fesh,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[PickupOption(outlet_id=2020)],
            )
            for fesh in (1, 5)
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=3030,
                dc_bucket_id=60,
                fesh=2,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[PickupOption(outlet_id=3030)],
            )
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=4040,
                dc_bucket_id=70,
                fesh=2,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                options=[PickupOption(outlet_id=4000 + i) for i in range(2000)],
            )
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Shops.supplier_3p.warehouse_id, [_Shops.supplier_3p.warehouse_id]),
            DynamicWarehouseLink(_Shops.dropship_shop.warehouse_id, [_Shops.dropship_shop.warehouse_id]),
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                _Shops.supplier_3p.warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=11000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            max_payment_weight=500000,
                            density=10,
                            min_days=2,
                            max_days=2,
                            delivery_service_id=1,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=20000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            max_payment_weight=500000,
                            density=10,
                            min_days=5,
                            max_days=6,
                            delivery_service_id=1,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            min_payment_weight=24000,
                            max_payment_weight=26000,
                            density=10,
                            min_days=2,
                            max_days=3,
                            delivery_service_id=1,
                            delivery_type=2,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            min_payment_weight=14000,
                            max_payment_weight=16000,
                            density=10,
                            min_days=5,
                            max_days=6,
                            delivery_service_id=1,
                            delivery_type=1,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            min_payment_weight=14000,
                            max_payment_weight=16000,
                            density=10,
                            min_days=5,
                            max_days=6,
                            delivery_service_id=1,
                            delivery_type=2,
                        ),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                _Shops.dropship_shop.warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=11000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            max_payment_weight=500000,
                            density=10,
                            min_days=5,
                            max_days=6,
                            delivery_service_id=2,
                        ),
                    ],
                },
            ),
        ]

    @staticmethod
    def __expected_response(alt_offers):
        return {
            "results": [
                {
                    "wareId": offer.waremd5,
                }
                for (offer, (reason, details)) in alt_offers
            ]
        }

    @staticmethod
    def __build_sku_offers_request():
        return "place=productoffers&rids=213&rgb={}&market-sku={}&hyperid={}&enable_multioffer=1&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0"

    def test_productoffers_explicit_offer_request(self):
        for rgb in ('blue', 'green_with_blue'):
            request_template = self.__build_sku_offers_request()
            for msku, offer, shop, alt_offers in (
                (
                    _SKUs.full_house,
                    _Offers.fast_delivery,
                    _Shops.supplier_3p,
                    [(_Offers.with_gift, FASTER), (_Offers.cheapest, DELIVERY_TYPE_PICKUP)],
                ),
            ):
                request = request_template.format(rgb, msku.sku, msku.hyperid)
                response = self.report.request_json(request)
                self.assertFragmentIn(response, self.__expected_response(alt_offers))


if __name__ == '__main__':
    main()
