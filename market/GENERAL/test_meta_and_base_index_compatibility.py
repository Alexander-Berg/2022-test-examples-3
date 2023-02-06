#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DeliveryBucket, GLParam, GLType, Model, Offer, Outlet, PickupBucket, PickupOption, Region, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        region_tree = [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name="Санкт-Петербург"),
                    Region(rid=193, name='Воронеж', preposition='в', locative='Воронеже'),
                    Region(rid=56, name='Челябинск', preposition='в', locative='Челябинске'),
                    Region(rid=35, name='Краснодар', preposition='в', locative='Краснодаре'),
                ],
            )
        ]

        gltypes = [
            GLType(param_id=1, hid=501, gltype=GLType.NUMERIC),
        ]

        cls.index.regiontree += region_tree
        cls.base_index.regiontree += region_tree

        cls.index.gltypes += gltypes
        cls.base_index.gltypes += gltypes

        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[193, 225],
                name='Московская пепячечная "Доставляем"',
                pickup_buckets=[5001],
            ),
        ]
        cls.base_index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                regions=[193, 225],
                name='Московская пепячечная "Доставляем"',
                pickup_buckets=[5001],
            ),
            Shop(
                fesh=2,
                priority_region=213,
                regions=[193, 225],
                name='Московская пепячечная "Доставляем еще лучше"',
                pickup_buckets=[5002],
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=1, region=193, point_type=Outlet.FOR_PICKUP, point_id=10001),
        ]
        cls.base_index.outlets += [
            Outlet(fesh=1, region=193, point_type=Outlet.FOR_PICKUP, point_id=10001),
            Outlet(fesh=2, region=193, point_type=Outlet.FOR_PICKUP, point_id=10002),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=10001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]
        cls.base_index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=10001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=10002)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(hid=501, hyperid=100, title='Some model', glparams=[GLParam(param_id=1, value=1)]),
        ]
        cls.base_index.models += [
            Model(hid=501, hyperid=100, title='Some model', glparams=[GLParam(param_id=1, value=1)]),
        ]

        cls.index.offers += [
            Offer(hyperid=100, fesh=1, price=100, pickup_buckets=[5001], glparams=[GLParam(param_id=1, value=1)]),
        ]
        cls.base_index.offers += [
            Offer(hyperid=100, fesh=1, price=100, pickup_buckets=[5001], glparams=[GLParam(param_id=1, value=1)]),
            Offer(hyperid=100, fesh=2, price=100, pickup_buckets=[5002], glparams=[GLParam(param_id=1, value=1)]),
        ]

    def test_new_shops(self):
        response = self.report.request_json('place=productoffers&hyperid=100&rids=193')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'totalOffers': 2,
                }
            },
        )


if __name__ == '__main__':
    main()
