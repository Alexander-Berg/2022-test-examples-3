#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(title="first", fesh=720, price=342, waremd5='RcSMzi4tf73qGvxRx8atJg'),
            Offer(title="second", fesh=721, price=123, waremd5='RcSMzi4tf73qGvxRx8atJf'),
        ]

        cls.index.shops += [
            Shop(fesh=720, safety_guarantee=True),
            Shop(fesh=721),
        ]

    def test_property(self):
        response = self.report.request_json('place=offerinfo&rids=0&regset=2&offerid=RcSMzi4tf73qGvxRx8atJg')
        self.assertFragmentIn(
            response,
            {
                "shop": {
                    "hasSafetyGuarantee": True,
                }
            },
        )

        response = self.report.request_json('place=offerinfo&rids=0&regset=2&offerid=RcSMzi4tf73qGvxRx8atJf')
        self.assertFragmentNotIn(
            response,
            {
                "shop": {
                    "hasSafetyGuarantee": True,
                }
            },
        )


if __name__ == '__main__':
    main()
