#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Model, Opinion


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'goods'
        cls.index.models += [
            Model(
                title='model_without_histogram',
                hyperid=300,
                opinion=Opinion(total_count=10, rating=4.0, precise_rating=4.0),
            ),
            Model(
                title='model_with_histogram',
                hyperid=301,
                opinion=Opinion(rating_histogram=[0, 1, 2, 3, 4], total_count=10, rating=4.0, precise_rating=4.0),
            ),
        ]

        cls.index.offers += [
            Offer(title='offer_without_histogram', hyperid=300),
            Offer(title='offer_with_histogram', hyperid=301),
        ]

    def test_rating_histogram(self):
        self.assertFragmentIn(
            self.report.request_json('place=prime&text=offer_without_histogram'),
            {"model": {"id": 300, "opinions": 10, "preciseRating": 4, "rating": 4}},
        )
        self.assertFragmentIn(
            self.report.request_json('place=prime&text=offer_with_histogram'),
            {
                "model": {
                    "id": 301,
                    "opinions": 10,
                    "preciseRating": 4,
                    "rating": 4,
                    "ratingHistogram": [0, 1, 2, 3, 4],
                }
            },
        )


if __name__ == '__main__':
    main()
