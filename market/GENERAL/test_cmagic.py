#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DeliveryBucket, Offer, Outlet, PickupBucket, PickupOption, Shop
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.shops += [
            Shop(fesh=1, priority_region=213),
        ]
        cls.index.outlets += [
            Outlet(point_id=100, fesh=1, region=213, point_type=Outlet.FOR_PICKUP),
        ]

        cls.index.offers += [
            Offer(hyperid=1, fesh=1, price=100500, cmagic='f2dfc75bbd15ae22fbd2e35b21675aab', pickup_buckets=[5001]),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=100)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_place_has_cmagic_xml(self):
        for place in ['shopoffers', 'defaultoffer']:
            response = self.report.request_xml(
                'place={}&hyperid=1&shop-offers-chunk=1&debug=da&fesh=1&rids=213'.format(place)
            )
            self.assertFragmentIn(
                response, "<classifier_magic_id>f2dfc75bbd15ae22fbd2e35b21675aab</classifier_magic_id>"
            )
            self.assertFragmentIn(response, '<param name="fsgta"><value>classifier_magic_id</value></param>')

    def test_place_has_cmagic_json(self):
        for place in ['defaultoffer', 'productoffers', 'prime', 'geo']:
            response = self.report.request_json('place={}&hyperid=1&debug=1&rids=213'.format(place))
            self.assertFragmentIn(response, {"classifierMagicId": "f2dfc75bbd15ae22fbd2e35b21675aab"})
            self.assertFragmentIn(response, {"fsgta": ["classifier_magic_id"]})


if __name__ == '__main__':
    main()
