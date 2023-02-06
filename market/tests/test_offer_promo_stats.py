# -*- coding: utf-8 -*-

import json

from context import StatsCalcBaseTestCase
# from market.idx.pylibrary.offer_flags.flags import OfferFlags


class TestOfferPromoStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestOfferPromoStats, self).setUp()

        self.gls = [
            {
                'promo_type': 1,
                'binary_promos_md5_base64': ['a', 'b', 'c']
            }, {
                'promo_type': 1,
                'binary_promos_md5_base64': ['a', 'b']
            }, {
                'promo_type': 1,
                'binary_promos_md5_base64': ['a']
            }, {
                'promo_type': 1,
                'binary_promos_md5_base64': ['d']
            }, {
                'promo_type': 1,
                'binary_promos_md5_base64': []
            },
        ]

    def test_category_stats(self):
        self.run_stats_calc(
            'OfferPromoStats',
            json.dumps(self.gls)
        )

        file_path = self.tmp_file_path('offer_promo_stats.csv')

        expected = {'a\t3\n', 'b\t2\n', 'c\t1\n', 'd\t1\n', 'sum\t4\n'}

        with open(file_path, 'r') as input:
            got = set(input.readlines())
            self.assertEqual(expected, got)
