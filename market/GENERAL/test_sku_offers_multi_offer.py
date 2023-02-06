#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
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
    DynamicWarehouseToWarehouseInfo,
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

from core.combinator import DeliveryStats, make_offer_id
from core.matcher import Absent, EqualToOneOf
from core.testcase import TestCase, main
from core.types.offer_promo import make_generic_bundle_content
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.types.delivery import OutletType
from core.report import REQUEST_TIMESTAMP


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

    crossdock_shop = Shop(
        fesh=2,
        datafeed_id=2,
        warehouse_id=2,
        name='crossdock shop',
        fulfillment_program=True,
        direct_shipping=False,
        supplier_type=Shop.THIRD_PARTY,
        priority_region=213,
        tax_system=Tax.OSN,
        blue=Shop.BLUE_REAL,
        client_id=2,
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

    click_n_collect_shop = Shop(
        fesh=4,
        datafeed_id=4,
        warehouse_id=4,
        fulfillment_program=False,
        ignore_stocks=True,
        name="Click & collect",
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
        client_id=4,
        delivery_service_outlets=[3030] + [14000 + i for i in range(2000)],
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

    supplier_1p = Shop(
        fesh=6,
        datafeed_id=6,
        warehouse_id=1,
        fulfillment_program=True,
        name="1P supplier",
        priority_region=213,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue=Shop.BLUE_REAL,
    )

    shop_with_same_client_as_crossdock = Shop(
        fesh=7,
        datafeed_id=7,
        warehouse_id=2,
        name='same client shop',
        fulfillment_program=True,
        direct_shipping=False,
        supplier_type=Shop.THIRD_PARTY,
        priority_region=213,
        tax_system=Tax.OSN,
        blue=Shop.BLUE_REAL,
        client_id=2,
    )


class _Offers(object):
    OFFER_DIMENSIONS = OfferDimensions(length=10, width=20, height=30)

    offer_1p = BlueOffer(
        price=35.99,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_1p.datafeed_id,
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Offer_1P_____________g',
    )
    offer_1p2 = BlueOffer(
        price=35.99,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_1p.datafeed_id,
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Offer_1P2____________g',
        offerid='Offer_1P2',
        pickup_buckets=[1016, 2026],
        delivery_buckets=[900],
    )
    offer_3p = BlueOffer(
        price=500,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        post_buckets=[3030],
        weight=5,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Offer_3P_____________g',
        offerid='Offer_3P',
    )
    offer_3p2 = BlueOffer(
        price=35,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Offer3P2_____________g',
        pickup_buckets=[1012, 2022],
        post_term_delivery=True,
    )
    offer_3p_cheaper_than_cnc = BlueOffer(
        price=30,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        post_buckets=[3030],
        weight=5,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Offer_3P3____________g',
        offerid='Offer_3P3',
    )
    crossdock_offer = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=_Shops.crossdock_shop.datafeed_id,
        delivery_buckets=[802],
        weight=5,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CrossdockOffer_______g',
        offerid='CrossdockOffer',
    )
    crossdock_offer2 = BlueOffer(
        price=15,
        vat=Vat.VAT_10,
        feedid=_Shops.shop_with_same_client_as_crossdock.datafeed_id,
        delivery_buckets=[802],
        weight=5,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CrossdockOffer2______g',
        offerid="crossdock_offer_with_gift",
    )
    dropship_offer = BlueOffer(
        price=200,
        vat=Vat.VAT_10,
        feedid=_Shops.dropship_shop.datafeed_id,
        delivery_buckets=[805],
        pickup_buckets=[1015, 2025],
        weight=2,
        dimensions=OFFER_DIMENSIONS,
        waremd5='DropshipOffer________g',
        offerid='DropshipOffer',
        post_term_delivery=True,
    )
    click_n_collect_offer = BlueOffer(
        price=30,
        vat=Vat.VAT_10,
        feedid=_Shops.click_n_collect_shop.datafeed_id,
        pickup_buckets=[14040],
        post_term_delivery=True,
        weight=2,
        dimensions=OFFER_DIMENSIONS,
        waremd5='ClickAndCollectOffer_g',
    )
    cnc_offer_more_expensive_than_3p = BlueOffer(
        price=500,
        vat=Vat.VAT_10,
        feedid=_Shops.click_n_collect_shop.datafeed_id,
        pickup_buckets=[14040],
        post_term_delivery=True,
        weight=2,
        dimensions=OFFER_DIMENSIONS,
        waremd5='ClickAndCollectOffer2g',
    )
    cnc_offer_cheaper_than_any_fast = BlueOffer(
        price=50,
        vat=Vat.VAT_10,
        feedid=_Shops.click_n_collect_shop.datafeed_id,
        pickup_buckets=[14040],
        post_term_delivery=True,
        weight=2,
        dimensions=OFFER_DIMENSIONS,
        waremd5='ClickAndCollectOffer3g',
    )
    any_delivery_type = BlueOffer(
        price=104,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='DeliveryDelivery_____g',
        offerid='DeliveryDelivery',
        pickup_buckets=[1012, 2022, 4040],
        delivery_buckets=[806],
        post_buckets=[3030],
        post_term_delivery=True,
    )
    any_delivery_type2 = BlueOffer(
        price=104,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='DeliveryDelivery2____g',
        pickup_buckets=[1012, 2022, 4040],
        delivery_buckets=[806],
        post_buckets=[3030],
        post_term_delivery=True,
    )
    fast_delivery = BlueOffer(
        price=109,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        weight=10,
        dimensions=OFFER_DIMENSIONS,
        waremd5='FastFastFast_________g',
        offerid='FastFastFast',
    )
    fast_delivery2 = BlueOffer(
        price=109,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        weight=7,
        dimensions=OFFER_DIMENSIONS,
        waremd5='FastFastFast2________g',
        offerid='FastFastFast2',
    )
    fast_delivery_offer_low_price = BlueOffer(
        price=100,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        weight=9,
        dimensions=OFFER_DIMENSIONS,
        waremd5='FastFastFast3________g',
        offerid='FastFastFast3',
    )
    fast_delivery_offer_mid_price = BlueOffer(
        price=200,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        weight=11,
        dimensions=OFFER_DIMENSIONS,
        waremd5='FastFastFast4________g',
        offerid='FastFastFast4',
    )
    fast_delivery_offer_high_price = BlueOffer(
        price=300,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        weight=13,
        dimensions=OFFER_DIMENSIONS,
        waremd5='FastFastFast5________g',
        offerid='FastFastFast5',
    )
    cheapest = BlueOffer(
        price=50,
        vat=Vat.VAT_10,
        feedid=_Shops.dropship_shop.datafeed_id,
        delivery_buckets=[805],
        weight=10,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CheapAsChips_________g',
        offerid='CheapAsChips',
    )
    cheapest2 = BlueOffer(
        price=50,
        vat=Vat.VAT_10,
        feedid=_Shops.dropship_shop.datafeed_id,
        delivery_buckets=[805],
        weight=10,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CheapAsChips2________g',
        offerid='CheapAsChips2',
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
    with_gift2 = BlueOffer(
        price=110,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[805],
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='WithGift2____________g',
        offerid='offer_with_gift2',
    )
    with_gift3 = BlueOffer(
        price=100,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[805],
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='WithGift3____________g',
        offerid='offer_with_gift3',
    )
    with_gift4 = BlueOffer(
        price=110,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[805],
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='WithGift4____________g',
        offerid='offer_with_gift4',
    )
    with_gift_fast_delivery_and_any_type = BlueOffer(
        price=51,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        delivery_buckets=[900],
        pickup_buckets=[1012, 2022, 4040],
        post_buckets=[3030],
        post_term_delivery=True,
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='WithGift5____________g',
        offerid='with_gift_fast_delivery_and_any_type',
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
    secondary_offer2 = BlueOffer(
        price=10,
        vat=Vat.VAT_10,
        feedid=_Shops.shop_with_same_client_as_crossdock.datafeed_id,
        delivery_buckets=[802],
        weight=6,
        dimensions=OFFER_DIMENSIONS,
        waremd5='Secondary2___________g',
        offerid='secondary_offer2',
    )

    pickup_bb = BlueOffer(
        price=103,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        weight=17,
        dimensions=OFFER_DIMENSIONS,
        waremd5='PickupBB_____________g',
        offerid='PickupBB',
        pickup_buckets=[1012, 2022, 4040],
    )
    courier_alternative = BlueOffer(
        price=104,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_1p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CourierAlternative___g',
        pickup_buckets=[1012, 2022, 4040],
        delivery_buckets=[806],
    )

    courier_pickup_bb = BlueOffer(
        price=103,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_3p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CourierPickupBB______g',
        delivery_buckets=[801],
        pickup_buckets=[1012, 2022, 4040],
    )
    cheaper_alternative = BlueOffer(
        price=52,
        vat=Vat.VAT_10,
        feedid=_Shops.dropship_shop.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='CheaperAlternative___g',
        offerid='CheaperAlternative',
        delivery_buckets=[801],
        pickup_buckets=[1012, 2022, 4040],
    )
    post_alternative_expensive = BlueOffer(
        price=102,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_1p.datafeed_id,
        weight=15,
        dimensions=OFFER_DIMENSIONS,
        waremd5='PostAlternativeExp___g',
        delivery_buckets=[801],
        pickup_buckets=[1012, 2022, 4040],
        post_buckets=[3030],
    )
    post_alternative = BlueOffer(
        price=101,
        vat=Vat.VAT_10,
        feedid=_Shops.supplier_1p.datafeed_id,
        weight=20,
        dimensions=OFFER_DIMENSIONS,
        waremd5='PostAlternative______g',
        offerid='PostAlternative',
        delivery_buckets=[801],
        pickup_buckets=[1012, 2022, 4040],
        post_buckets=[3030],
    )


class _SKUs(object):
    cnc_and_3p = MarketSku(
        title="CnC_and_3P", hyperid=1, sku=1, blue_offers=[_Offers.click_n_collect_offer, _Offers.offer_3p]
    )

    dropship_and_crossdock = MarketSku(
        title="Dropship_Crossdock",
        hyperid=2,
        sku=2,
        blue_offers=[_Offers.crossdock_offer, _Offers.crossdock_offer2, _Offers.dropship_offer],
    )

    only_1p = MarketSku(title="Only1P", hyperid=3, sku=3, delivery_buckets=[806], blue_offers=[_Offers.offer_1p])
    o1p_and_3p = MarketSku(
        title="1PAnd3P", hyperid=4, sku=4, delivery_buckets=[806], blue_offers=[_Offers.offer_1p2, _Offers.offer_3p2]
    )
    full_house = MarketSku(
        title="FullHouse",
        hyperid=5,
        sku=5,
        blue_offers=[_Offers.any_delivery_type, _Offers.fast_delivery, _Offers.cheapest, _Offers.with_gift],
    )
    secondary = MarketSku(
        title="Secondary", hyperid=6, sku=6, blue_offers=[_Offers.secondary_offer, _Offers.secondary_offer2]
    )
    one_best_alternative = MarketSku(
        title="one_best_alternative",
        hyperid=7,
        sku=7,
        blue_offers=[
            _Offers.any_delivery_type2,
            _Offers.fast_delivery2,
            _Offers.cheapest2,
            _Offers.with_gift_fast_delivery_and_any_type,
            _Offers.with_gift2,
        ],
    )
    buybox_has_gift = MarketSku(
        title="FullHouse", hyperid=8, sku=8, blue_offers=[_Offers.with_gift3, _Offers.with_gift4]
    )
    cnc_and_3p2 = MarketSku(
        title="CnC_and_3P2",
        hyperid=9,
        sku=9,
        blue_offers=[_Offers.cnc_offer_more_expensive_than_3p, _Offers.offer_3p_cheaper_than_cnc],
    )
    cnc_and_3fast = MarketSku(
        title="CnC_and_3Fast",
        hyperid=10,
        sku=10,
        blue_offers=[
            _Offers.cnc_offer_cheaper_than_any_fast,
            _Offers.fast_delivery_offer_high_price,
            _Offers.fast_delivery_offer_low_price,
            _Offers.fast_delivery_offer_mid_price,
        ],
    )
    courier_alternative = MarketSku(
        title="CourierAlternative", hyperid=5, sku=11, blue_offers=[_Offers.pickup_bb, _Offers.courier_alternative]
    )
    post_alternative = MarketSku(
        title="PostAlternative",
        hyperid=5,
        sku=12,
        blue_offers=[
            _Offers.courier_pickup_bb,
            _Offers.cheaper_alternative,
            _Offers.post_alternative_expensive,
            _Offers.post_alternative,
        ],
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
        for num, offer in enumerate(
            [_Offers.with_gift, _Offers.with_gift3, _Offers.with_gift4, _Offers.with_gift_fast_delivery_and_any_type]
        )
    ]
    all_promos.append(
        Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=_Offers.crossdock_offer2.feedid,
            key='JVvklxUgdnawSJPG4UhA-1',
            url='http://localhost.ru/',
            generic_bundles_content=[
                make_generic_bundle_content(_Offers.crossdock_offer2.offerid, _Offers.secondary_offer2.offerid, 1),
            ],
        )
    )


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

        for num, offer in enumerate(
            [
                _Offers.with_gift,
                _Offers.with_gift3,
                _Offers.with_gift4,
                _Offers.with_gift_fast_delivery_and_any_type,
                _Offers.crossdock_offer2,
            ]
        ):
            offer.promo = _Promos.all_promos[num]

        cls.settings.report_subrole = 'blue-main'
        cls.settings.lms_autogenerate = False
        cls.settings.nordstream_autogenerate = False
        cls.settings.loyalty_enabled = True
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        cls.index.shops += [
            _Shops.blue_virtual_shop,
            _Shops.crossdock_shop,
            _Shops.supplier_1p,
            _Shops.supplier_3p,
            _Shops.click_n_collect_shop,
            _Shops.dropship_shop,
            _Shops.shop_with_same_client_as_crossdock,
        ]

        cls.index.promos += _Promos.all_promos

        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in _Promos.all_promos])]

        cls.index.mskus += [
            _SKUs.cnc_and_3p,
            _SKUs.dropship_and_crossdock,
            _SKUs.only_1p,
            _SKUs.o1p_and_3p,
            _SKUs.full_house,
            _SKUs.secondary,
            _SKUs.one_best_alternative,
            _SKUs.buybox_has_gift,
            _SKUs.cnc_and_3p2,
            _SKUs.cnc_and_3fast,
            _SKUs.courier_alternative,
            _SKUs.post_alternative,
        ]

        for warehouse_id in (1, 2, 4, 5):
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
            DynamicDeliveryServiceInfo(
                99,
                "self-delivery",
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213, intervals=[TimeIntervalsForDaysInfo(intervals_key=1, days_key=1)]
                    )
                ],
            ),
            warehouse_and_delivery_service(_Shops.supplier_3p.warehouse_id),
            warehouse_and_delivery_service(_Shops.dropship_shop.warehouse_id),
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1, warehouse_to=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=4, warehouse_to=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=5, warehouse_to=5),
            DynamicWarehouseToWarehouseInfo(
                warehouse_from=_Shops.crossdock_shop.warehouse_id,
                warehouse_to=_Shops.supplier_3p.warehouse_id,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=225, packaging_time=TimeInfo(1))
                ],
                inbound_time=TimeInfo(2, 30),
                transfer_time=TimeInfo(3),
                operation_time=0,
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=_Shops.click_n_collect_shop.warehouse_id,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=12, region_to=225, packaging_time=TimeInfo(1))
                ],
            ),
        ]
        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[225], warehouse_with_priority=[WarehouseWithPriority(i, 100) for i in (1, 2, 4, 5)]
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
        cls.index.outlets += [
            Outlet(
                point_id=14000 + i,
                delivery_service_id=99,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=99, day_from=8, day_to=9, price=100),
                working_days=list(range(10)),
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
            for fesh in (1, 2, 4, 5, 6)
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
            for fesh in (1, 2, 4, 5, 6)
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
            for fesh in (1, 2, 4, 5, 6)
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
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=14040,
                dc_bucket_id=80,
                fesh=4,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                options=[PickupOption(outlet_id=14000 + i) for i in range(2000)],
            )
        ]

        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=_Shops.click_n_collect_shop.datafeed_id, pickupBuckets=[14040])
        ]

    @classmethod
    def prepare_sku_offers(cls):
        report_start_time_in_ms = REQUEST_TIMESTAMP * 10**6
        cls.combinator.set_start_date(microseconds_for_disabled_random=report_start_time_in_ms)

        def stats(data):
            return DeliveryStats(*data) if data is not None else data

        for o in (
            _Offers.fast_delivery,
            _Offers.fast_delivery2,
            _Offers.fast_delivery_offer_low_price,
            _Offers.fast_delivery_offer_mid_price,
            _Offers.fast_delivery_offer_high_price,
        ):
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(o, _Shops.supplier_3p),
                courier_stats=DeliveryStats(cost=0, day_from=1, day_to=2),
                pickup_stats=None,
                post_stats=None,
                outlet_types=[],
            )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.offer_3p_cheaper_than_cnc, _Shops.supplier_3p),
            courier_stats=DeliveryStats(cost=0, day_from=2, day_to=3),
            pickup_stats=None,
            post_stats=None,
            outlet_types=[],
        )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.cheapest2, _Shops.dropship_shop),
            courier_stats=DeliveryStats(cost=0, day_from=1, day_to=2),
            pickup_stats=None,
            post_stats=None,
            outlet_types=[],
        )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.offer_3p, _Shops.supplier_3p),
            courier_stats=DeliveryStats(cost=0, day_from=2, day_to=2),
            pickup_stats=None,
            post_stats=None,
            outlet_types=[OutletType.FOR_PICKUP],
        )
        for o, s in (
            (_Offers.cheapest, _Shops.dropship_shop),
            (_Offers.offer_1p2, _Shops.supplier_1p),
            (_Offers.crossdock_offer, _Shops.crossdock_shop),
            (_Offers.cheaper_alternative, _Shops.dropship_shop),
        ):
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(o, s),
                courier_stats=DeliveryStats(cost=0, day_from=3, day_to=4),
                pickup_stats=None,
                post_stats=None,
                outlet_types=[],
            )
        for o, s in (
            (_Offers.any_delivery_type, _Shops.supplier_3p),
            (_Offers.pickup_bb, _Shops.supplier_3p),
            (_Offers.dropship_offer, _Shops.dropship_shop),
        ):
            cls.combinator.add_offer_delivery(
                offer_ids=make_offer_id(o, s),
                courier_stats=None,
                pickup_stats=DeliveryStats(cost=0, day_from=3, day_to=4),
                post_stats=None,
                outlet_types=[OutletType.FOR_PICKUP],
            )
        cls.combinator.add_offer_delivery(
            offer_ids=make_offer_id(_Offers.post_alternative, _Shops.supplier_1p),
            courier_stats=None,
            pickup_stats=None,
            post_stats=stats(DeliveryStats(cost=0, day_from=3, day_to=4)),
            outlet_types=[OutletType.FOR_POST],
        )

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(_Shops.supplier_3p.warehouse_id, [_Shops.supplier_3p.warehouse_id]),
            DynamicWarehouseLink(_Shops.dropship_shop.warehouse_id, [_Shops.dropship_shop.warehouse_id]),
            DynamicWarehouseLink(_Shops.crossdock_shop.warehouse_id, [_Shops.supplier_3p.warehouse_id]),
        ]
        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                _Shops.supplier_3p.warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=18000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            min_payment_weight=16000,
                            max_payment_weight=500000,
                            density=10,
                            min_days=5,
                            max_days=6,
                            delivery_service_id=1,
                            delivery_type=1,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=21000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            min_payment_weight=19000,
                            max_payment_weight=500000,
                            density=10,
                            min_days=5,
                            max_days=6,
                            delivery_service_id=1,
                            delivery_type=2,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            max_payment_weight=500000,
                            density=10,
                            min_days=6,
                            max_days=7,
                            delivery_service_id=3,
                        ),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                _Shops.dropship_shop.warehouse_id,
                {
                    213: [
                        DynamicDeliveryRestriction(
                            max_phys_weight=23000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            max_payment_weight=500000,
                            density=10,
                            min_days=3,
                            max_days=4,
                            delivery_service_id=2,
                        ),
                        DynamicDeliveryRestriction(
                            max_phys_weight=40000,
                            max_dim_sum=150,
                            max_dimensions=[50, 50, 50],
                            max_payment_weight=500000,
                            density=10,
                            min_days=6,
                            max_days=7,
                            delivery_service_id=4,
                        ),
                    ],
                },
            ),
        ]

    @staticmethod
    def __multioffer_flag(enable_multioffer):
        return '&enable_multioffer={}'.format('0') if enable_multioffer is not None else ""

    @staticmethod
    def __build_sku_offers_request(enable_multioffer):
        return (
            "place=sku_offers&rids=213&rgb=blue&rearr-factors=market_blue_buybox_courier_delivery_priority=0;market_blue_buybox_generic_bundle_promo_rate=0;market_blue_buybox_max_price_rel_add_diff=0&market-sku={}"  # noqa
            + T.__multioffer_flag(enable_multioffer)
        )

    @staticmethod
    def __expected_response(msku, offers, alt_offers, multioffer_enabled):
        if not multioffer_enabled or alt_offers is None:
            additionalOffers = Absent()
        else:
            additionalOffers = Absent()

        return {
            "results": [
                {
                    "entity": "sku",
                    "id": msku.sku,
                    "offers": {
                        "items": [
                            {
                                "entity": "offer",
                                "supplier": {"name": shop.name, "warehouseId": shop.warehouse_id},
                                "wareId": offer.waremd5,
                            }
                            for offer, shop in offers
                        ]
                    },
                    "additionalOffers": additionalOffers,
                }
            ]
        }

    def test_sku_offers_explicit_offer_request(self):
        """Проверка работы с запросом конкретного оффера"""
        for flag in (None, 0, 1):
            request_template = self.__build_sku_offers_request(flag) + "&offerid={}"

            for msku, offer, shop, alt_offers in (
                (
                    _SKUs.dropship_and_crossdock,
                    _Offers.dropship_offer,
                    _Shops.dropship_shop,
                    [(_Offers.crossdock_offer, CHEAPER), (_Offers.crossdock_offer2, GIFT)],
                ),
                (
                    _SKUs.dropship_and_crossdock,
                    _Offers.crossdock_offer,
                    _Shops.crossdock_shop,
                    [(_Offers.dropship_offer, DELIVERY_TYPE_PICKUP)],
                ),
                (
                    _SKUs.cnc_and_3p,
                    _Offers.click_n_collect_offer,
                    _Shops.click_n_collect_shop,
                    [(_Offers.offer_3p, FASTER)],
                ),
                (_SKUs.cnc_and_3p, _Offers.offer_3p, _Shops.supplier_3p, [(_Offers.click_n_collect_offer, CHEAPER)]),
                (
                    _SKUs.cnc_and_3p2,
                    _Offers.cnc_offer_more_expensive_than_3p,
                    _Shops.click_n_collect_shop,
                    [(_Offers.offer_3p_cheaper_than_cnc, CHEAPER)],
                ),
                (
                    _SKUs.cnc_and_3p2,
                    _Offers.offer_3p_cheaper_than_cnc,
                    _Shops.supplier_3p,
                    [(_Offers.cnc_offer_more_expensive_than_3p, DELIVERY_TYPE_PICKUP)],
                ),
                (_SKUs.full_house, _Offers.fast_delivery, _Shops.supplier_3p, [(_Offers.cheapest, CHEAPER)]),
                (
                    _SKUs.cnc_and_3fast,
                    _Offers.cnc_offer_cheaper_than_any_fast,
                    _Shops.click_n_collect_shop,
                    [(_Offers.fast_delivery_offer_low_price, FASTER)],
                ),
            ):
                response = self.report.request_json(request_template.format(msku.sku, offer.waremd5))
                self.assertFragmentIn(
                    response,
                    self.__expected_response(msku, ((offer, shop),), alt_offers, flag == 1),
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_sku_offers_msku_request(self):
        """Проверка работы с вычислением байбокса market-sku"""
        for flag in (None, 0, 1):
            request_template = self.__build_sku_offers_request(flag)

            for msku, offer, shop, alt_offers in (
                # здесь dropship оффер отображается, так как у него есть тип доставки отличный от того что в buybox
                # + здесь проверяется что crossdock_offer2 (лучше чем buybox по наличию подарка) не отображается,
                # т.к у него один поставщик с buybox
                (
                    _SKUs.dropship_and_crossdock,
                    _Offers.crossdock_offer,
                    _Shops.crossdock_shop,
                    [(_Offers.dropship_offer, DELIVERY_TYPE_PICKUP)],
                ),
                (
                    _SKUs.cnc_and_3p,
                    _Offers.click_n_collect_offer,
                    _Shops.click_n_collect_shop,
                    [(_Offers.offer_3p, FASTER)],
                ),
                (_SKUs.only_1p, _Offers.offer_1p, _Shops.supplier_1p, None),
                (_SKUs.o1p_and_3p, _Offers.offer_3p2, _Shops.supplier_3p, [(_Offers.offer_1p2, FASTER)]),
                (
                    _SKUs.full_house,
                    _Offers.cheapest,
                    _Shops.dropship_shop,
                    [
                        (_Offers.with_gift, GIFT),
                        (_Offers.fast_delivery, FASTER),
                        (_Offers.any_delivery_type, DELIVERY_TYPE_PICKUP),
                    ],
                ),
                (
                    _SKUs.one_best_alternative,
                    _Offers.cheapest2,
                    _Shops.dropship_shop,
                    [(_Offers.with_gift_fast_delivery_and_any_type, GIFT)],
                ),
                (_SKUs.buybox_has_gift, _Offers.with_gift3, _Shops.supplier_3p, None),
                (
                    _SKUs.cnc_and_3fast,
                    _Offers.cnc_offer_cheaper_than_any_fast,
                    _Shops.click_n_collect_shop,
                    [(_Offers.fast_delivery_offer_low_price, FASTER)],
                ),
            ):
                response = self.report.request_json(request_template.format(msku.sku))
                self.assertFragmentIn(
                    response,
                    self.__expected_response(msku, ((offer, shop),), alt_offers, flag == 1),
                    preserve_order=True,
                    allow_different_len=False,
                )

    def test_sku_offers_delivery_type_alternative(self):
        """Проверка вычисления альтернативных оферов с дополнительным типом доставки"""
        request_template = self.__build_sku_offers_request(1) + "&offerid={}"

        for msku, offer, shop, alt_offers in (
            (
                _SKUs.courier_alternative,
                _Offers.pickup_bb,
                _Shops.supplier_3p,
                [(_Offers.courier_alternative, DELIVERY_TYPE_COURIER)],
            ),
            (
                _SKUs.post_alternative,
                _Offers.courier_pickup_bb,
                _Shops.supplier_3p,
                [(_Offers.cheaper_alternative, CHEAPER), (_Offers.post_alternative, DELIVERY_TYPE_POST)],
            ),
        ):
            response = self.report.request_json(request_template.format(msku.sku, offer.waremd5))
            self.assertFragmentIn(
                response,
                self.__expected_response(msku, ((offer, shop),), alt_offers, True),
                preserve_order=True,
                allow_different_len=False,
            )

    def test_sku_sibling_offers(self):
        """Проверка обработки запроса с несколькими офферами одного MSKU"""
        for msku, offers, shops in (
            (
                _SKUs.cnc_and_3p,
                (_Offers.click_n_collect_offer, _Offers.offer_3p),
                (_Shops.click_n_collect_shop, _Shops.supplier_3p),
            ),
            (
                _SKUs.dropship_and_crossdock,
                (_Offers.crossdock_offer, _Offers.dropship_offer),
                (_Shops.crossdock_shop, _Shops.dropship_shop),
            ),
        ):
            offers_arg = ''.join(map(lambda o: '&offerid={}'.format(o.waremd5), offers))
            base_request = "place=sku_offers&rids=213&rgb=blue&market-sku={}&debug=1".format(msku.sku) + offers_arg

            # cart_multioffer in (None, 0), enable_multioffer in (None, 0, 1)
            self.error_log.expect("More than one offer requested for MSKU '{}'".format(msku.sku)).times(6)
            # cart_multioffer == 1, enable_multioffer == 1
            # may occur 0 or 1 times, depending on the order in which offers are received from base report
            self.error_log.ignore("Alternative offer search requires a single buybox offer, MSKU={}".format(msku.sku))

            for cart_multioffer in (None, 0, 1):
                cart_multioffer_flag = (
                    ''
                    if cart_multioffer is None
                    else '&rearr-factors=market_cart_multi_offer={}'.format(cart_multioffer)
                )

                for enable_multioffer in (None, 0, 1):
                    request = base_request + self.__multioffer_flag(enable_multioffer) + cart_multioffer_flag
                    response = self.report.request_json(request)
                    if cart_multioffer in (None, 0):
                        self.assertFragmentIn(
                            response,
                            {
                                "results": [
                                    {
                                        "entity": "sku",
                                        "id": msku.sku,
                                        "offers": {
                                            "items": [
                                                {
                                                    "entity": "offer",
                                                    "wareId": EqualToOneOf(*[o.waremd5 for o in offers]),
                                                }
                                            ]
                                        },
                                        "additionalOffers": Absent(),
                                    }
                                ]
                            },
                            allow_different_len=False,
                        )
                    else:
                        self.assertFragmentIn(
                            response,
                            self.__expected_response(msku, zip(offers, shops), None, enable_multioffer),
                            allow_different_len=False,
                        )

    def test_show_all_alternative_offers(self):
        """Проверка флага show_all_alternative_offers, под которым показываются все офферы данного msku"""
        request_template = (
            self.__build_sku_offers_request(1)
            + "&offerid={}"
            + "&rearr-factors=sku_offers_show_all_alternative_offers=1;market_blue_buybox_max_price_rel_add_diff=0"
        )
        msku_offers_list = (
            (
                _Offers.fast_delivery_offer_low_price,
                _Shops.supplier_3p,
                (
                    (_Offers.fast_delivery_offer_mid_price, ABSENT_REASON),
                    (_Offers.fast_delivery_offer_high_price, ABSENT_REASON),
                    (_Offers.cnc_offer_cheaper_than_any_fast, CHEAPER),
                ),
            ),
            (
                _Offers.fast_delivery_offer_mid_price,
                _Shops.supplier_3p,
                (
                    (_Offers.fast_delivery_offer_low_price, ABSENT_REASON),
                    (_Offers.fast_delivery_offer_high_price, ABSENT_REASON),
                    (_Offers.cnc_offer_cheaper_than_any_fast, CHEAPER),
                ),
            ),
            (
                _Offers.fast_delivery_offer_high_price,
                _Shops.supplier_3p,
                (
                    (_Offers.fast_delivery_offer_mid_price, ABSENT_REASON),
                    (_Offers.fast_delivery_offer_low_price, ABSENT_REASON),
                    (_Offers.cnc_offer_cheaper_than_any_fast, CHEAPER),
                ),
            ),
            (
                _Offers.cnc_offer_cheaper_than_any_fast,
                _Shops.click_n_collect_shop,
                (
                    (_Offers.fast_delivery_offer_mid_price, ABSENT_REASON),
                    (_Offers.fast_delivery_offer_high_price, ABSENT_REASON),
                    (_Offers.fast_delivery_offer_low_price, FASTER),
                ),
            ),
        )
        for offer, shop, alt_offers_list in msku_offers_list:
            response = self.report.request_json(request_template.format(_SKUs.cnc_and_3fast.sku, offer.waremd5))
            self.assertFragmentIn(
                response,
                self.__expected_response(_SKUs.cnc_and_3fast, ((offer, shop),), alt_offers_list, True),
                preserve_order=False,
                allow_different_len=False,
            )


if __name__ == '__main__':
    main()
