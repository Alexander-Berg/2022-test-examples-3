# -*- coding: utf-8 -*-

import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


class TestCategoryStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestCategoryStats, self).setUp()

        self.gls = [
            # categories with hid = 91491 and hid = 215930 are leaf and category with hid = 91461 is not
            # (see tovar.old.categories_plus_extra.xml in Sandbox)

            # valid genlogs
            # for hid = 91491 (45345.54 + 44356.5 + 4345.64) / 3 = 31349.23
            # for hid = 215930 (0.02 + 0.04) / 2 = 0.03
            {
                'category_id': 91491,
                'binary_price': '45345.54 1 0 RUR RUR'
            },
            {
                'category_id': 91491,
                'binary_price': '44356.5 1 0 RUR RUR'
            },
            {
                'category_id': 215930,
                'binary_price': '0.02 1 0 RUR RUR'
            },
            {
                'category_id': 91491,
                'binary_price': '4345.64 1 0 RUR RUR'
            },
            {
                'category_id': 215930,
                'binary_price': '0.04 1 0 RUR RUR'
            },

            # invalid genlogs - should be skipped
            # no category_id
            {
                'binary_price': '14 1 0 RUR RUR'
            },
            # no binary_price
            {
                'category_id': 91491
            },
            # non-leaf category
            {
                'category_id': 91461,
                'binary_price': '19 1 0 RUR RUR'
            },
            # contex
            {
                'category_id': 91491,
                'binary_price': '45345.54 1 0 RUR RUR',
                'contex_info': {'original_msku_id': 100},
            },
            # direct
            {
                'category_id': 91491,
                'binary_price': '12345.67 1 0 RUR RUR',
                'flags': OfferFlags.IS_DIRECT.value
            },
            # Lavka
            {
                'category_id': 91491,
                'binary_price': '12345.67 1 0 RUR RUR',
                'flags': OfferFlags.IS_LAVKA.value
            },
            # Eda
            {
                'category_id': 91491,
                'binary_price': '12345.67 1 0 RUR RUR',
                'flags': OfferFlags.IS_EDA_RESTAURANTS.value
            },
        ]

    def test_category_stats(self):
        self.run_stats_calc(
            'CategoryStats',
            json.dumps(self.gls)
        )

        file_path = self.tmp_file_path('category_stats.csv')

        expected = {'91491\t31349.23\n', '215930\t0.03\n'}

        with open(file_path, 'r') as input:
            got = set(input.readlines())
            self.assertEqual(expected, got)
