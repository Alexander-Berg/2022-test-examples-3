#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryBucket, DynamicOfferOutlet, Offer, Outlet, PickupBucket, PickupOption, Shop
from core.testcase import TestCase, main

SHOP_DEFAULT = 1
SHOP_ENABLED_ALL = 2
SHOP_DISABLED_DESKTOP = 3
SHOP_DISABLED_ALL = 4


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers = [
            Offer(title='kiyanka 1', waremd5="09lEaAKkQll1XTjm0WPoIA", fesh=1, pickup_buckets=[5001]),
            Offer(title='kiyanka 2', waremd5="xMpCOKC5I4INzFCab3WEmQ", fesh=2, pickup_buckets=[5002]),
        ]

        cls.index.shops += [
            Shop(fesh=1, name='Shop1'),
            Shop(fesh=2, name='Shop2'),
        ]

        cls.index.outlets += [
            Outlet(point_id=101, fesh=1, region=213),
            Outlet(point_id=102, fesh=1, region=213),
            Outlet(point_id=103, fesh=2, region=2),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=101), PickupOption(outlet_id=102)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=103)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_outlets_disable(self):
        """
        0. Проверим, что все офферы и аутлеты находятся;
        1. Забаним один аутлет и проверим, что он пропал (а все остальные есть);
        2. Забаним еще один аутлет и проверим, что и он пропал.
        """

        response = self.report.request_json('place=geo&text=kiyanka')

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "09lEaAKkQll1XTjm0WPoIA",
                    "outlet": {
                        "id": "101",
                    },
                },
                {
                    "entity": "offer",
                    "wareId": "09lEaAKkQll1XTjm0WPoIA",
                    "outlet": {
                        "id": "102",
                    },
                },
                {
                    "entity": "offer",
                    "wareId": "xMpCOKC5I4INzFCab3WEmQ",
                    "outlet": {
                        "id": "103",
                    },
                },
            ],
        )

        self.dynamic.market_dynamic.disabled_offer_outlets += [DynamicOfferOutlet("09lEaAKkQll1XTjm0WPoIA", 101)]

        response = self.report.request_json('place=geo&text=kiyanka')

        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "09lEaAKkQll1XTjm0WPoIA",
                    "outlet": {
                        "id": "101",
                    },
                }
            ],
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "09lEaAKkQll1XTjm0WPoIA",
                    "outlet": {
                        "id": "102",
                    },
                }
            ],
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "xMpCOKC5I4INzFCab3WEmQ",
                    "outlet": {
                        "id": "103",
                    },
                }
            ],
        )

        self.dynamic.market_dynamic.disabled_offer_outlets += [DynamicOfferOutlet("xMpCOKC5I4INzFCab3WEmQ", 103)]

        response = self.report.request_json('place=geo&text=kiyanka')

        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "09lEaAKkQll1XTjm0WPoIA",
                    "outlet": {
                        "id": "101",
                    },
                }
            ],
        )

        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "09lEaAKkQll1XTjm0WPoIA",
                    "outlet": {
                        "id": "102",
                    },
                }
            ],
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "wareId": "xMpCOKC5I4INzFCab3WEmQ",
                    "outlet": {
                        "id": "103",
                    },
                }
            ],
        )


if __name__ == '__main__':
    main()
