import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


# outlets_data = (point_id, region_id, in_stock)


class TestBookNowStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestBookNowStats, self).setUp()

        self.gl1 = {
            'model_id': 1,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                # 2 in reg_id = 1, 1 in reg_id = 2
                1, 1, 1,
                2, 1, 1,
                3, 2, 1,
            ]
        }

        self.gl2 = {
            'model_id': 1,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                # 2 in reg_id = 1, 1 in reg_id = 2
                1, 1, 1,
                2, 1, 1,
                3, 2, 1,
            ]
        }

        self.gl3 = {
            'model_id': 2,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                # 2 in reg_id = 1, 1 in reg_id = 2
                1, 1, 1,
                2, 1, 1,
                3, 2, 1,
            ]
        }

        self.gl4 = {
            'model_id': 1,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
            ]
        }

        self.gl5 = {
            'model_id': 1,
            'feed_id': 1000000001,  # is_booknow = false
            'shop_id': 1000000001,  # is_booknow = false
            'outlets_data': [
                1, 1, 1,
                2, 1, 1,
                3, 2, 1,
            ]
        }

        self.gl6 = {
            'offer_id': "123",
            'model_id': 1,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                1, 1, 1,
                2, 1, 1,
                3, 2, 1,
            ]
        }

        self.gl7 = {
            'offer_id': 'foo',
            'model_id': 4,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                1, 1, 1,
            ]
        }

        self.gl8 = {
            'model_id': 1,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                # 2 in reg_id = 1, 1 in reg_id = 2
                1, 1, 1,
                2, 1, 1,
                3, 2, 1,
            ],
            'contex_info': {'original_msku_id': 100},
        }

        self.gl9 = {
            'offer_id': 'direct',
            'model_id': 4,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                1, 1, 1,
            ],
            'flags': OfferFlags.IS_DIRECT.value
        }

        self.gl10 = {
            'offer_id': 'lavka',
            'model_id': 4,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                1, 1, 1,
            ],
            'flags': OfferFlags.IS_LAVKA.value
        }

        self.gl11 = {
            'offer_id': 'eda',
            'model_id': 4,
            'feed_id': 1000000000,
            'shop_id': 1000000000,
            'outlets_data': [
                1, 1, 1,
            ],
            'flags': OfferFlags.IS_EDA_RESTAURANTS.value
        }

        self.gls_mrs = [
            self.gl1,
            self.gl2,
            self.gl3,
            self.gl4,
            self.gl5,
            self.gl6,
            self.gl7,
            self.gl8,
            self.gl9,
            self.gl10,
            self.gl11,
        ]
        self.gls_shops = [
            self.gl1,
            self.gl2,
            self.gl3,
            self.gl4,
            self.gl5,
            self.gl8,
            self.gl9,
            self.gl10,
            self.gl11,
        ]

    def process_genlogs(self, gls):
        self.run_stats_calc('BookNowModelRegionStats', json.dumps(gls))
        file_path = self.tmp_file_path('book_now_model.mmap')

        actual_totals_json = self.get_stats_from_mmap(file_path)
        actual_totals = (
            (record['model_id'], record['region_id'], record['outlets_count'])
            for record in actual_totals_json
        )

        return sorted(actual_totals)

    def test_model_region_stats(self):
        # total:
        # (1, 1) -> 4
        # (1, 2) -> 2
        # (2, 1) -> 2
        # (2, 2) -> 1
        # (3, 1) -> 2 (3 is a group model for 2)
        # (3, 2) -> 1 (3 is a group model for 2)
        totals = [
            (1, 1, 6),
            (1, 2, 3),
            (2, 1, 2),
            (2, 2, 1),
            (3, 1, 2),
            (3, 2, 1),
            (4, 1, 1),
        ]

        actual_totals = self.process_genlogs(self.gls_mrs)
        self.assertEquals(totals, actual_totals)

    def test_shop_stats(self):
        self.run_stats_calc('BookNowShopStats', json.dumps(self.gls_shops))

        # total:
        # 1000000000 -> 9
        # 1000000001 -> 3
        totals = [
            (1000000000, 9),
            (1000000001, 3),
        ]

        file_path = self.tmp_file_path('book_now_shop.tsv')
        with open(file_path, 'r') as file_object:
            for shop_id, outlet_count in totals:
                line = file_object.readline()
                parts = line.split('\t')
                self.assertEquals(shop_id, int(parts[0]))
                self.assertEquals(outlet_count, int(parts[1]))
