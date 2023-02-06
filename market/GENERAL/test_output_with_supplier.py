#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DeliveryBucket, Model, Offer, Outlet, PickupBucket, PickupOption, Region, Shop, WhiteSupplier


class T(TestCase):
    iphone_all_options = {
        'entity': 'offer',
        'titles': {'raw': 'Apple Iphone'},
        'delivery': {'isAvailable': True, 'hasPickup': True},
    }

    galaxy_all_options = {
        'entity': 'offer',
        'titles': {'raw': 'Samsung Galaxy'},
        'delivery': {'isAvailable': True, 'hasLocalStore': True},
    }

    @classmethod
    def prepare(cls):
        # Regions: 100+
        # Shops: 10..19
        # Outlets: 20..29
        # Hyperid: 30..39

        cls.index.regiontree += [
            Region(rid=100, children=[Region(rid=101)]),
            Region(rid=110, children=[Region(rid=111)]),
        ]

        cls.index.shops += [
            Shop(fesh=10, regions=[100, 110], name='Apple Store', pickup_buckets=[5001]),
            Shop(
                fesh=11,
                regions=[100, 110],
                name='Samsung Store',
                pickup_buckets=[5002],
                marketplace_status=Shop.MARKETPLACE_REAL,
            ),
            Shop(
                fesh=12,
                regions=[100],
                name='Google Store',
                pickup_buckets=[5003],
                marketplace_status=Shop.MARKETPLACE_SANDBOX,
            ),
            Shop(
                fesh=13,
                regions=[100],
                name='Shop without marketplaces',
                pickup_buckets=[5004],
                marketplace_status=Shop.MARKETPLACE_NO,
            ),
        ]

        cls.index.outlets += [
            Outlet(fesh=10, region=100, point_type=Outlet.FOR_PICKUP, point_id=101),
            Outlet(fesh=10, region=101, point_type=Outlet.FOR_PICKUP, point_id=102),
            Outlet(fesh=10, region=110, point_type=Outlet.FOR_PICKUP, point_id=103),
            Outlet(fesh=10, region=111, point_type=Outlet.FOR_PICKUP, point_id=104),
            Outlet(fesh=11, region=100, point_type=Outlet.FOR_STORE, point_id=111),
            Outlet(fesh=11, region=101, point_type=Outlet.FOR_STORE, point_id=112),
            Outlet(fesh=11, region=110, point_type=Outlet.FOR_STORE, point_id=113),
            Outlet(fesh=11, region=111, point_type=Outlet.FOR_STORE, point_id=114),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_PICKUP, point_id=20),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_PICKUP, point_id=21),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_STORE, point_id=22),
            Outlet(fesh=12, region=100, point_type=Outlet.FOR_STORE, point_id=23),
            Outlet(fesh=12, region=100, point_id=24),
            Outlet(fesh=12, region=100, point_id=25),
            Outlet(fesh=12, region=110, point_type=Outlet.FOR_PICKUP, point_id=26),
            Outlet(fesh=12, region=110, point_type=Outlet.FOR_STORE, point_id=27),
            Outlet(fesh=12, region=110, point_id=28),
            Outlet(fesh=13, region=100, point_type=Outlet.FOR_PICKUP, point_id=121),
            Outlet(fesh=13, region=101, point_type=Outlet.FOR_PICKUP, point_id=122),
            Outlet(fesh=13, region=110, point_type=Outlet.FOR_PICKUP, point_id=123),
            Outlet(fesh=13, region=111, point_type=Outlet.FOR_PICKUP, point_id=124),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=10,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=101),
                    PickupOption(outlet_id=102),
                    PickupOption(outlet_id=103),
                    PickupOption(outlet_id=104),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=11,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=111),
                    PickupOption(outlet_id=112),
                    PickupOption(outlet_id=113),
                    PickupOption(outlet_id=114),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                fesh=12,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=20),
                    PickupOption(outlet_id=21),
                    PickupOption(outlet_id=22),
                    PickupOption(outlet_id=23),
                    PickupOption(outlet_id=24),
                    PickupOption(outlet_id=25),
                    PickupOption(outlet_id=26),
                    PickupOption(outlet_id=27),
                    PickupOption(outlet_id=28),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=13,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=121),
                    PickupOption(outlet_id=122),
                    PickupOption(outlet_id=123),
                    PickupOption(outlet_id=124),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(title='Apple Iphone', fesh=10),
            Offer(title='Samsung Galaxy', fesh=11),
            Offer(title='Google Pixel', fesh=12, hyperid=30, waremd5='CCCCCCCCCCCCCCCCCCCCCC'),
        ]

        cls.index.white_suppliers += [
            WhiteSupplier(ogrn="1235", jur_name="name1", jur_address="address1"),
            WhiteSupplier(ogrn="1238", jur_name="name2", jur_address="address2"),
            WhiteSupplier(ogrn="125100", jur_name="name3", jur_address="address3"),
        ]

    @classmethod
    def prepare_offer_with_supplier(cls):
        cls.index.offers += [
            Offer(title="offer 1", mp_supl_ogrn="1235", fesh=32),
            Offer(title="offer 2", mp_supl_ogrn="1238", fesh=32),
            Offer(title="offer 3", mp_supl_ogrn="124100", fesh=32),
            Offer(title="offer 4", fesh=32),
            Offer(title="offer 5", mp_supl_ogrn="1235", fesh=13),
            Offer(title="offer 6", fesh=13),
        ]

    def test_offer_with_supplier(self):
        """ """
        response = self.report.request_json('place=prime&text=offer&numdoc=48')
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "offer 1"},
                    "white_supplier": {"ogrn": "1235", "jur_name": "name1", "jur_address": "address1"},
                },
                {
                    "titles": {"raw": "offer 2"},
                    "white_supplier": {"ogrn": "1238", "jur_name": "name2", "jur_address": "address2"},
                },
                {"titles": {"raw": "offer 4"}},
                {"titles": {"raw": "offer 6"}},
            ],
        )

        # offer 3 is hidden because ogrn is not present in white_suppliers.pb
        self.assertFragmentNotIn(
            response,
            [
                {"titles": {"raw": "offer 3"}},
            ],
        )

        # offer 5 is hidden because marketplace disabled in shops.dat
        self.assertFragmentNotIn(
            response,
            [
                {"titles": {"raw": "offer 5"}},
            ],
        )

        # offer 3 still present in output when debug == 1
        response = self.report.request_json('place=prime&text=offer&numdoc=48&debug=1')
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "offer 1"},
                    "white_supplier": {"ogrn": "1235", "jur_name": "name1", "jur_address": "address1"},
                },
                {
                    "titles": {"raw": "offer 2"},
                    "white_supplier": {"ogrn": "1238", "jur_name": "name2", "jur_address": "address2"},
                },
                {"titles": {"raw": "offer 3"}, "white_supplier": {"ogrn": "124100"}},
                {"titles": {"raw": "offer 4"}},
            ],
        )

    @classmethod
    def prepare_productoffer_with_supplier(cls):
        cls.index.models += [
            Model(hid=2222, hyperid=123, title='Model test'),
        ]
        cls.index.offers += [
            Offer(mp_supl_ogrn="1235", fesh=10, title="offer 11", price=100, hyperid=123),
            Offer(mp_supl_ogrn="1235", fesh=11, title="offer 12", price=101, hyperid=123),
            Offer(mp_supl_ogrn="125100", fesh=12, title="offer 13", price=200, hyperid=123),
            Offer(fesh=12, title="offer 14", price=200, hyperid=123),
            Offer(mp_supl_ogrn="1", fesh=12, title="offer 15", price=200, hyperid=123),
            Offer(mp_supl_ogrn="1235", fesh=13, title="offer 16", price=95, hyperid=123),
        ]

    def test_productoffer_with_supplier(self):
        response = self.report.request_json('place=productoffers&hyperid=123&rids=101')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer 11"},
                        "white_supplier": {"ogrn": "1235", "jur_name": "name1", "jur_address": "address1"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer 12"},
                        "white_supplier": {"ogrn": "1235", "jur_name": "name1", "jur_address": "address1"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer 13"},
                        "white_supplier": {"ogrn": "125100", "jur_name": "name3", "jur_address": "address3"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer 14"},
                    },
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer 15"},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "offer 16"},
                    },
                ]
            },
        )


if __name__ == '__main__':
    main()
