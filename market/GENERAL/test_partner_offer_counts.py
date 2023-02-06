#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryBucket, Offer, Outlet, PickupBucket, PickupOption, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(hyperid=1, adult=True),
            Offer(hyperid=1, adult=False),
            Offer(hyperid=2, adult=False),
            Offer(hyperid=3, fesh=1),
            Offer(hyperid=3, fesh=2),
            Offer(hyperid=4, fesh=3),
            Offer(hyperid=5, fesh=4),
            Offer(hyperid=5, fesh=5),
            Offer(hyperid=6, fesh=6),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=10),
            Shop(fesh=2, priority_region=20),
            Shop(fesh=3, priority_region=10),
            Shop(fesh=4, pickup_buckets=[5004]),
            Shop(fesh=5, pickup_buckets=[5005]),
        ]

        cls.index.outlets += [
            Outlet(fesh=4, point_type=Outlet.FOR_PICKUP, region=30, point_id=4),
            Outlet(fesh=5, point_type=Outlet.FOR_STORE, region=30, point_id=5),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5004,
                fesh=4,
                carriers=[99],
                options=[PickupOption(outlet_id=4)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                fesh=5,
                carriers=[99],
                options=[PickupOption(outlet_id=5)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.shops += [Shop(fesh=6, regions=[30])]

    def test_filter_by_adult(self):
        response = self.report.request_xml('place=partner_offer_counts&hyperid=1&hyperid=2&adult=0')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <model hyperid="1" total="1"/>
            <model hyperid="2" total="1"/>
        </search_results>''',
        )

        response = self.report.request_xml('place=partner_offer_counts&hyperid=1&hyperid=2&adult=1')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <model hyperid="1" total="2"/>
            <model hyperid="2" total="1"/>
        </search_results>''',
        )

    def test_filter_by_region(self):
        response = self.report.request_xml('place=partner_offer_counts&hyperid=3&hyperid=4&rids=10')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <model hyperid="3" total="1"/>
            <model hyperid="4" total="1"/>
        </search_results>''',
        )

        response = self.report.request_xml('place=partner_offer_counts&hyperid=3&hyperid=4&rids=20')
        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <model hyperid="3" total="1"/>
        </search_results>''',
        )

    def test_filter_by_shipping(self):
        response = self.report.request_xml(
            'place=partner_offer_counts&hyperid=5&hyperid=6&rids=30&offer-shipping=pickup,store,delivery'
        )

        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <model hyperid="5" total="2"/>
            <model hyperid="6" total="1"/>
        </search_results>''',
        )

        response = self.report.request_xml(
            'place=partner_offer_counts&hyperid=5&hyperid=6&rids=30&offer-shipping=pickup'
        )

        self.assertFragmentIn(
            response,
            '''
        <search_results>
            <model hyperid="5" total="1"/>
        </search_results>''',
        )

    def test_missing_pp(self):
        self.report.request_xml(
            'place=partner_offer_counts&hyperid=1&hyperid=2&adult=0&ip=127.0.0.1', add_defaults=False
        )


if __name__ == '__main__':
    main()
