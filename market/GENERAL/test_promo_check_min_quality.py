#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Offer, Promo, PromoType
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [
            Offer(
                title='offer 1',
                benefit_price=100,
                price_history=90,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='promo1', discount_value=50),
            ),
            Offer(
                title='offer 2',
                benefit_price=90,
                price_history=100,
                promo=Promo(promo_type=PromoType.PROMO_CODE, key='promo2', discount_value=50),
            ),
        ]

    def test_promo_quality_filter(self):
        response = self.report.request_json('place=prime&text=offer&promo-check-min-quality=1')
        self.assertFragmentIn(response, {"titles": {"raw": "offer 2"}})
        self.assertFragmentNotIn(response, {"titles": {"raw": "offer 1"}})

        for args in ['place=prime&text=offer&promo-check-min-quality=0', 'place=prime&text=offer']:
            response = self.report.request_json(args)
            self.assertFragmentIn(response, {"titles": {"raw": "offer 2"}})
            self.assertFragmentIn(response, {"titles": {"raw": "offer 1"}})


if __name__ == '__main__':
    main()
