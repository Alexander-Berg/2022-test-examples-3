#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    RtyOffer,
    Shop,
    Tax,
    Vat,
)
from core.testcase import TestCase, main
from unittest import skip


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class T(TestCase):
    MSKU_1_WAREMD5 = 'Sku1-wdDXWsIiLVm1goleg'
    MSKU_2_WAREMD5 = 'Sku2-wdDXWsIiLVm1goleg'

    BLUE_OFFER_1_WAREMD5 = 'Sku1Price5-IiLVm1Goleg'
    BLUE_OFFER_2_WAREMD5 = 'Sku2Price5-IiLVm1Goleg'

    PICKUP_OPTION_1 = {'day_from': 2, 'day_to': 3, 'price': 150}
    PICKUP_OPTION_2 = {'day_from': 1, 'day_to': 3, 'price': 200}

    POST_OPTION_1 = {'day_from': 10, 'day_to': 15, 'price': 250}
    POST_OPTION_2 = {'day_from': 9, 'day_to': 10, 'price': 500}

    NON_EXISTENT_BUCKET_ID = 100500

    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.rty_delivery = True
        cls.settings.index_sort = 'feed-offer'
        cls.settings.rty_merger_policy = 'NONE'
        cls.disable_check_empty_output()
        cls.settings.blue_market_free_delivery_threshold = 6000

        cls.index.regiontree += [
            Region(rid=10, name='Регион 10', region_type=Region.FEDERAL_DISTRICT),
            Region(rid=20, name='Регион 20', region_type=Region.FEDERAL_DISTRICT),
            Region(rid=30, name='Регион 30', region_type=Region.FEDERAL_DISTRICT),
            Region(rid=213, name='Москва', region_type=Region.FEDERAL_DISTRICT),
        ]

        delivery_10_1 = RegionalDelivery(
            rid=10,
            options=[
                DeliveryOption(price=50, day_from=3, day_to=4),
                DeliveryOption(price=100, day_from=4, day_to=5),
                DeliveryOption(price=150, day_from=3, day_to=6),
            ],
        )
        delivery_10_2 = RegionalDelivery(
            rid=10,
            options=[
                DeliveryOption(price=60, day_from=1, day_to=2),
                DeliveryOption(price=100, day_from=2, day_to=3),
            ],
        )
        _ = RegionalDelivery(rid=20, options=[DeliveryOption(price=200)])
        delivery_30 = RegionalDelivery(rid=30, options=[DeliveryOption(price=300)])
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1,
                dc_bucket_id=1000,
                regional_options=[delivery_10_1],
                carriers=[500],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=2,
                dc_bucket_id=2000,
                regional_options=[delivery_10_2],
                carriers=[501],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(bucket_id=3, dc_bucket_id=3000, regional_options=[delivery_30]),
        ]

        cls.index.pickup_buckets += [
            # Pickup buckets:
            PickupBucket(
                bucket_id=10,
                dc_bucket_id=100,
                fesh=22,
                carriers=[500],
                options=[
                    PickupOption(outlet_id=1, **cls.PICKUP_OPTION_1),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=11,
                dc_bucket_id=101,
                fesh=22,
                carriers=[501],
                options=[
                    PickupOption(outlet_id=2, **cls.PICKUP_OPTION_2),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Post buckets:
            PickupBucket(
                bucket_id=20,
                dc_bucket_id=200,
                fesh=22,
                carriers=[100],
                options=[
                    PickupOption(outlet_id=4, **cls.POST_OPTION_1),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=21,
                dc_bucket_id=201,
                fesh=22,
                carriers=[100],
                options=[
                    PickupOption(outlet_id=5, **cls.POST_OPTION_2),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            # Pickup outlets:
            Outlet(
                point_id=1,
                delivery_service_id=500,
                region=10,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=500, **cls.PICKUP_OPTION_1),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=2,
                delivery_service_id=501,
                region=10,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=501, **cls.PICKUP_OPTION_2),
                working_days=[i for i in range(10)],
            ),
            # Post outlets:
            Outlet(
                point_id=4,
                delivery_service_id=100,
                region=10,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=100, **cls.POST_OPTION_1),
                working_days=[i for i in range(31)],
            ),
            Outlet(
                point_id=5,
                delivery_service_id=100,
                region=10,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=100, **cls.POST_OPTION_2),
                working_days=[i for i in range(31)],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=21, priority_region=213, regions=[213], name='Moskvichoff'),
            Shop(
                fesh=22,
                datafeed_id=2,
                priority_region=213,
                regions=[213],
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                delivery_service_outlets=[1, 2, 4, 5],
            ),
        ]

        cls.index.offers += [
            Offer(title='iphone', fesh=21, feedid=25, offerid='fff', price=10000, delivery_buckets=[1]),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku 1",
                sku=1,
                hyperid=1,
                waremd5=cls.MSKU_1_WAREMD5,
                blue_offers=[
                    BlueOffer(
                        price=500,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.1',
                        waremd5=cls.BLUE_OFFER_1_WAREMD5,
                        delivery_buckets=[1],
                        pickup_buckets=[10],
                        post_buckets=[20],
                    )
                ],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku 2",
                sku=2,
                hyperid=1,
                waremd5=cls.MSKU_2_WAREMD5,
                blue_offers=[
                    BlueOffer(
                        price=5000,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.2',
                        waremd5=cls.BLUE_OFFER_2_WAREMD5,
                        delivery_buckets=[1],
                        pickup_buckets=[10],
                        post_buckets=[20],
                    )
                ],
                post_term_delivery=True,
            ),
        ]

    @skip('Forced to work on place sku_offers only (for now)')
    def test_courier_delivery_on_place_prime(self):
        self.rty.offers += [
            RtyOffer(
                feedid=25,
                offerid='fff',
                price=400,
                delivery_buckets=[
                    DeliveryBucket(dc_bucket_id=2000),
                ],
            ),
        ]

        self.rty_controller.reopen_indexes()
        self.rty_controller.do_merge()

        def check(data, rty_delivery):
            for rid, total in data.items():
                response = self.report.request_json(
                    'place=prime&text=iphone&pp=18&rids={}&rearr-factors=market_nordstream_relevance=0;rty_qoffer_delivery={}'.format(
                        rid, rty_delivery
                    )
                )
                self.assertFragmentIn(response, {'search': {'total': total}})

        # legacy data
        check_data = {10: 1, 20: 0, 30: 0}
        check(check_data, 0)

        # rty data
        check_data = {10: 0, 20: 1, 30: 0}
        check(check_data, 1)

    def _do_blue_offer_card_request(self, msku_id, offer_id, user_region, rty_enabled=False):
        url = 'place=sku_offers&market-sku={msku_id}&offerid={offer_id}&rids={user_region}&rgb=blue&pickup-options=grouped&regional-delivery=1&rearr-factors=market_nordstream_relevance=0;rty_qoffer_delivery={rty_enabled}'.format(  # noqa
            msku_id=msku_id,
            offer_id=offer_id,
            user_region=user_region,
            rty_enabled=int(rty_enabled),
        )
        url += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        return self.report.request_json(url)

    def _check_delivery(self, response, delivery_dict):
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'offers': {
                                'items': [
                                    {
                                        'delivery': delivery_dict,
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    @skip('TODO')
    def test_fallback(self):
        # global NON_EXISTENT_BUCKET_ID
        self.rty.offers += [
            RtyOffer(
                feedid=3,
                offerid='blue.offer.2',
                price=5000,
                delivery_buckets=[
                    DeliveryBucket(bucket_id=self.NON_EXISTENT_BUCKET_ID, dc_bucket_id=self.NON_EXISTENT_BUCKET_ID),
                ],
                pickup_buckets=[
                    PickupBucket(bucket_id=self.NON_EXISTENT_BUCKET_ID, dc_bucket_id=self.NON_EXISTENT_BUCKET_ID),
                ],
                post_buckets=[
                    PickupBucket(bucket_id=self.NON_EXISTENT_BUCKET_ID, dc_bucket_id=self.NON_EXISTENT_BUCKET_ID),
                ],
            ),
        ]

        self.rty_controller.reopen_indexes()
        self.rty_controller.do_merge()

        response = self._do_blue_offer_card_request(2, 'blue.offer.2', 10, True)
        self._check_delivery(
            response,
            {
                'pickupOptions': [
                    {
                        'serviceId': 500,
                    },
                ],
            },
        )

    def test_change_offer_buckets(self):
        self.rty.offers += [
            RtyOffer(
                feedid=2,
                offerid='blue.offer.1',
                price=5000,
                delivery_buckets=[
                    # bucket_1 -> bucket_2
                    DeliveryBucket(dc_bucket_id=2000),
                ],
                pickup_buckets=[
                    # bucket_10 -> bucket_11
                    PickupBucket(dc_bucket_id=101),
                ],
                post_buckets=[
                    # bucket_20 -> bucket_21
                    PickupBucket(dc_bucket_id=201),
                ],
            ),
        ]

        self.rty_controller.reopen_indexes()
        self.rty_controller.do_merge()

        response = self._do_blue_offer_card_request(1, 'blue.offer.1', 10, False)
        self._check_delivery(
            response,
            {
                'isAvailable': True,
                'hasPickup': True,
                'hasPost': True,
                'options': [
                    {
                        'price': {'currency': 'RUR', 'value': '99'},
                        'dayFrom': 3,
                        'dayTo': 4,
                        'serviceId': '500',
                    },
                ],
                'pickupOptions': [
                    {
                        'price': {'currency': 'RUR', 'value': '99'},
                        'dayFrom': 2,
                        'dayTo': 3,
                        'serviceId': 500,
                    }
                ],
                'postStats': {
                    'minDays': 10,
                    'maxDays': 15,
                    'minPrice': {'currency': 'RUR', 'value': '99'},
                    'maxPrice': {'currency': 'RUR', 'value': '99'},
                },
            },
        )

        response = self._do_blue_offer_card_request(1, 'blue.offer.1', 10, True)
        self._check_delivery(
            response,
            {
                'isAvailable': True,
                'hasPickup': True,
                'hasPost': True,
                'options': [
                    {
                        'price': {'currency': 'RUR', 'value': '99'},
                        'dayFrom': 1,
                        'dayTo': 2,
                        'serviceId': '501',
                    },
                ],
                'pickupOptions': [
                    {
                        'price': {'currency': 'RUR', 'value': '99'},
                        'dayFrom': 1,
                        'dayTo': 3,
                        'serviceId': 501,
                    },
                ],
                'postStats': {
                    'minDays': 9,
                    'maxDays': 10,
                    'minPrice': {'currency': 'RUR', 'value': '99'},
                    'maxPrice': {'currency': 'RUR', 'value': '99'},
                },
            },
        )


if __name__ == '__main__':
    main()
