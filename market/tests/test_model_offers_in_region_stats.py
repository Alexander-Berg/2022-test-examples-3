# -*- coding: utf-8 -*-


import json
import yt.yson as yson

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


class TestModelOffersInRegionStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestModelOffersInRegionStats, self).setUp()

        self.gl1 = {
            'model_id': -1,
            'cluster_id': -1,
            'downloadable': True,
            'regions': '1000000',
            'int_regions': [yson.YsonUint64(1000000)]
        }

        self.gl2 = {
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5)],
            'priority_regions': '3',
        }

        self.gl3 = {
            'model_id': 2,
            'downloadable': True,
            'regions': '4 5',
            'int_regions': [yson.YsonUint64(4),
                            yson.YsonUint64(5)]
        }

        self.gl4 = {
            'model_id': 3,
            'delivery_flag': True,
            'regions': '3 4 5 6',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5),
                            yson.YsonUint64(6)],
            'priority_regions': '1 2 6'
        }

        self.gl5 = {
            'cluster_id': 4,
            'downloadable': True,
            'regions': '7',
            'int_regions': [yson.YsonUint64(7)]
        }

        self.gl6 = {
            'model_id': 5,
        }

        self.gl7 = {
            'model_id': 6,
            'priority_regions': '3',
            'geo_regions': '3 7 8',
            'int_geo_regions': [yson.YsonUint64(3),
                                yson.YsonUint64(7),
                                yson.YsonUint64(8)]
        }

        self.gl8 = {
            'cluster_id': 7,
            'priority_regions': '3',
            'geo_regions': '3 7 8',
            'int_geo_regions': [yson.YsonUint64(3),
                                yson.YsonUint64(7),
                                yson.YsonUint64(8)]
        }

        self.gl9 = {
            'cluster_id': 7,
            'delivery_flag': True,
            'regions': '3 4 5 6',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5),
                            yson.YsonUint64(6)],
            'priority_regions': '3 6 7'
        }

        self.gl10 = {
            'cluster_id': 7,
            'delivery_flag': True,
            'regions': '3 4 5 6',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5),
                            yson.YsonUint64(6)],
            'priority_regions': '3 6 7',
            'contex_info': {
                'original_msku_id': 100,
            },
        }

        self.gl11 = {
            'cluster_id': 7,
            'delivery_flag': True,
            'regions': '3 4 5 6',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5),
                            yson.YsonUint64(6)],
            'priority_regions': '3 6 7',
            'flags': OfferFlags.IS_DIRECT.value
        }

        self.gl12 = {
            'cluster_id': 7,
            'delivery_flag': True,
            'regions': '3 4 5 6',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5),
                            yson.YsonUint64(6)],
            'priority_regions': '3 6 7',
            'flags': OfferFlags.IS_LAVKA.value
        }

        self.gl13 = {
            'cluster_id': 7,
            'delivery_flag': True,
            'regions': '3 4 5 6',
            'int_regions': [yson.YsonUint64(3),
                            yson.YsonUint64(4),
                            yson.YsonUint64(5),
                            yson.YsonUint64(6)],
            'priority_regions': '3 6 7',
            'flags': OfferFlags.IS_EDA_RESTAURANTS.value
        }

        self.gls = [
            self.gl1, self.gl2, self.gl3, self.gl4, self.gl5, self.gl6, self.gl7, self.gl8,
            self.gl9, self.gl10, self.gl11, self.gl12, self.gl13
        ]

    def test_model_offers_in_region_stats(self):
        local_delivery_path = self.tmp_file_path('model_local_offers_geo_stats.mmap')
        offline_delivery_path = self.tmp_file_path('model_offline_offers_geo_stats.mmap')
        local_delivery_model = {
            '1': {'3': 1, '4': 1, '5': 1},
            '2': {'4': 1, '5': 1},
            '3': {'6': 1},
            '4': {'7': 1},
            '6': {'3': 1},
            '7': {'3': 1, '6': 1}
        }
        offline_delivery_model = {}

        self.run_stats_calc('ModelGeoStats', json.dumps(self.gls))

        local_delivery_actual = self.get_stats_from_mmap(local_delivery_path, 'ModelGeoStats')
        offline_delivery_actual = self.get_stats_from_mmap(offline_delivery_path, 'ModelGeoStats')

        self.assertEquals(local_delivery_model, local_delivery_actual)
        self.assertEquals(offline_delivery_model, offline_delivery_actual)
