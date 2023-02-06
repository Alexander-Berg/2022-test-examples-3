import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


class TestGLFilterRegionalStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestGLFilterRegionalStats, self).setUp()

        self.gl1 = {
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
            'is_blue_offer': True,
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
        self.gl4 = {
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
            ],
            'contex_info': {
                'original_msku_id': 100,
            },
        }

        # Direct offer
        self.gl5 = {
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
            ],
            'flags': OfferFlags.IS_DIRECT.value,
        }

        # Lavka offer
        self.gl6 = {
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
            ],
            'flags': OfferFlags.IS_LAVKA.value,
        }

        # Eda offer
        self.gl7 = {
            'model_id': 1,
            'downloadable': True,
            'delivery_flag': True,
            'regions': '3 4 5',
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
            ],
            'flags': OfferFlags.IS_EDA_RESTAURANTS.value,
        }

        self.gls = [self.gl1, self.gl2, self.gl3, self.gl4, self.gl5, self.gl6, self.gl7]

    def test_filters_in_region_stats(self):
        category_gl_params_path = self.tmp_file_path('category_gl_params.csv')

        self.run_stats_calc('GLFilterRegionalStats', json.dumps(self.gls))
        expected = ['10,50,1,2,3,4,5',
                    '10,51,5,10,20']
        with open(category_gl_params_path) as f:
            content = f.read().splitlines()
            self.assertEquals(len(content), 2)
            for line in content:
                self.assertTrue(line in expected)
