import json
import yt.yson as yson

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags


class TestBlueGLFilterRegionalStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestBlueGLFilterRegionalStats, self).setUp()

        self.gl1 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ]
        }

        self.gl2 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [3, 4, 5],
                    'is_numeric': True
                },
                {
                    'id': 51,
                    'values': [20, 5, 10],
                    'is_numeric': False
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 80,
                    'values': [189.8]
                }
            ]
        }

        # this record must be skipped
        self.gl3 = {
            'is_blue_offer': False,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [3, 4, 5],
                    'is_numeric': True
                },
                {
                    'id': 51,
                    'values': [20, 5, 10],
                    'is_numeric': False
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 80,
                    'values': [189.8]
                }
            ]
        }

        # this record must be skipped
        self.gl4 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [5, 6, 7],
                    'is_numeric': True
                },
                {
                    'id': 51,
                    'values': [30, 40, 50],
                    'is_numeric': False
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 80,
                    'values': [189.8]
                }
            ],
            'has_gone': True
        }

        # this record must be skipped
        self.gl5 = {
            'is_blue_offer': True,
            'flags': OfferFlags.MARKET_SKU.value,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ]
        }

        # this record must be skipped by buybox
        self.gl6 = {
            'is_blue_offer': True,
            'is_buyboxes': False,
            'flags': OfferFlags.MARKET_SKU.value,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ]
        }

        # this record must be skipped because of disabled_flags
        self.gl7 = {
            'is_blue_offer': True,
            'disabled_flags': DisabledFlags.MARKET_STOCK.value,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [5, 6, 7],
                    'is_numeric': True
                },
                {
                    'id': 51,
                    'values': [30, 40, 50],
                    'is_numeric': False
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 80,
                    'values': [189.8]
                }
            ]
        }

        # this record must be skipped because of contex
        self.gl8 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ],
            'contex_info': {'original_msku_id': 100},
        }

        # direct
        self.gl9 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ],
            'flags': OfferFlags.IS_DIRECT.value
        }

        # Lavka
        self.gl10 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ],
            'flags': OfferFlags.IS_LAVKA.value
        }

        # Eda
        self.gl11 = {
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
            'int_regions' : [yson.YsonUint64(3),
                             yson.YsonUint64(4),
                             yson.YsonUint64(5)],
            'priority_regions': '3',
            'category_id': 10,
            'mbo_params': [
                {
                    'id': 50,
                    'values': [1, 2, 3],
                    'is_numeric': True
                }
            ],
            'numeric_params': [
                {
                    'id': 61,
                    'precision': 20,
                    'values': [100.5, 200.1, 300.3, 189.8]
                }
            ],
            'flags': OfferFlags.IS_EDA_RESTAURANTS.value
        }

        self.gls = [
            self.gl1, self.gl2, self.gl3, self.gl4, self.gl5, self.gl6, self.gl7,
            self.gl8, self.gl9, self.gl10, self.gl11
        ]

    def test_filters_in_region_stats(self):
        category_gl_params_path = self.tmp_file_path('blue_category_gl_params.csv')

        self.run_stats_calc('ShopRegionalCategoriesStats', json.dumps(self.gls))
        expected = ['10,50,1,2,3,4,5',
                    '10,51,5,10,20']
        with open(category_gl_params_path) as f:
            content = f.read().splitlines()
            self.assertEquals(len(content), 2)
            for line in content:
                self.assertTrue(line in expected)
