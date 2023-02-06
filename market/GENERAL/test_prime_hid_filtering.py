#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree += [
            HyperCategory(hid=1, children=[HyperCategory(hid=11)]),
            HyperCategory(hid=2),
        ]

        cls.index.offers += [
            Offer(hid=1, title='просто котик'),
            Offer(hid=11, title='сверх котик'),
            Offer(hid=2, title='убер котик'),
        ]

    def test_hid_filtering(self):
        """
        MARKETOUT-24525
        Check filtering using category subtraction
        """

        for hid in [1, -2]:
            response = self.report.request_json('place=prime&hid=%s&text=котик' % str(hid))
            self.assertFragmentIn(
                response,
                [
                    {
                        "entity": "offer",
                        "categories": [{"id": 1}],
                    },
                    {
                        "entity": "offer",
                        "categories": [{"id": 11}],
                    },
                ],
                allow_different_len=False,
            )

        # test that nested category subtraction works
        response = self.report.request_json('place=prime&hid=1,-11&text=котик')
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "categories": [{"id": 1}],
                },
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
