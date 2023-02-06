#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Offer, Shop
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_pricedrops(cls):
        cls.index.shops += [
            Shop(fesh=20267000),
            Shop(fesh=20267001),
            Shop(fesh=20267002),
            Shop(fesh=20267003),
            Shop(fesh=20267004),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=20267000),
        ]

        cls.index.offers += [
            Offer(title="offer1", fesh=20267000, enable_auto_discounts=1, price=100000, price_history=None),
            Offer(
                title="offer2",
                fesh=20267001,
                enable_auto_discounts=1,
                price=100000,
                price_old=150000,
                price_history=None,
            ),
            Offer(title="offer3", fesh=20267002, enable_auto_discounts=1, price=100000, price_history=120000),
            Offer(
                title="offer4",
                fesh=20267003,
                enable_auto_discounts=1,
                price=100000,
                price_old=150000,
                price_history=120000,
            ),
            Offer(title="offer5", fesh=20267004, enable_auto_discounts=1, price=150000, price_history=120000),
        ]

    def test_autosale_no_disount_if_nothing_old(self):
        response = self.report.request_json('place=prime&fesh=20267000')
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "offer1"},
                "prices": {"value": "100000", "discount": Absent()},
            },
        )

    def test_autosale_disount_if_only_history(self):
        response = self.report.request_json('place=prime&fesh=20267002')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer3"}, "prices": {"value": "100000", "discount": {"percent": 17}}}
        )

    def test_autosale_disount_if_oldprice_and_hprice(self):
        response = self.report.request_json('place=prime&fesh=20267003')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer4"}, "prices": {"value": "100000", "discount": {"percent": 17}}}
        )

    def test_drop_bad_autosale(self):
        response = self.report.request_json('place=prime&fesh=20267004')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer5"}, "prices": {"value": "150000", "discount": Absent()}}
        )


if __name__ == '__main__':
    main()
