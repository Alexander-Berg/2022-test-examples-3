#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(hyperid=14115323, title="abc", fesh=719, price=719, waremd5='RcSMzi4tf73qGvxRx8atJf'),
            Offer(hyperid=14115324, title="def", fesh=720, price=720, waremd5='RcSMzi4tf73qGvxRx8atJg', is_sample=True),
        ]

    def test_without_param(self):
        response = self.report.request_json('place=offerinfo&offerid=RcSMzi4tf73qGvxRx8atJf&rids=0&regset=2')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [{"entity": "offer", "shop": {"id": 719}, "isSample": Absent()}],
            },
            allow_different_len=False,
        )

    def test_with_param(self):
        response = self.report.request_json('place=offerinfo&offerid=RcSMzi4tf73qGvxRx8atJg&rids=0&regset=2')
        self.assertFragmentIn(
            response,
            {
                "total": 1,
                "results": [{"entity": "offer", "shop": {"id": 720}, "isSample": True}],
            },
            allow_different_len=False,
        )

    def test_hidden_on_specific_places(self):
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=14115324&debug=1'.format(place))
            self.assertFragmentIn(response, {"total": 0, "results": []}, allow_different_len=False)
            self.assertFragmentIn(
                response,
                {
                    "filters": {
                        "HIDE_SAMPLE_OFFERS": 1,
                    }
                },
            )


if __name__ == '__main__':
    main()
