import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


NON_NUMERIC_PARAM_RECORD = 0
REGION_RECORD = 1
BOOKING_RECORD = 2
NUMERIC_PARAM_RECORD = 3


def _mbo_params(params_dict):
    """Maps a dictionary representation of mbo_params
    to the one that JSON can handle.
    """
    return [
        {'id': key, 'values': value}
        for key, value
        in params_dict.iteritems()]


def _numeric_params(params_dict):
    return [{'id': key, 'ranges': value} for key, value in params_dict.iteritems()]


class TestOfferFiltersForModelsStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestOfferFiltersForModelsStats, self).setUp()

        # generation logs:
        # - offer id is assigned internally as a model_id-unique counter
        # - model_id doesn't have to be real
        # - mbo_params: only 1 & 2 are valid keys, other numbers are ignored
        # - numeric_params: only 41 & 42 are valid keys, other numbers are ignored
        # - downloadable: the easiest way to get stats-calc to use regions
        # - regions: don't have to be real
        self.gls = [
            # valid offers
            {
                # id = 0
                # will be used: (the first valid genlog)
                'model_id': 1,
                'mbo_params': _mbo_params({1: [2]}),
                'numeric_params': _numeric_params({41: [141]}),
                'downloadable': True,
                'regions': '1 2',
                'int_regions': [1, 2],
                'outlets_data': [
                    1, 1, 1,
                    2, 1, 4,
                    3, 2, 1,
                ],
            },
            {
                # id = 1
                # will be used: different mbo_params
                'model_id': 1,
                'mbo_params': _mbo_params({1: [1], 3: [666]}),
                'numeric_params': _numeric_params({41: [140], 43: [141]}),
                'downloadable': True,
                'regions': '1 2',
                'int_regions': [1, 2],
                'outlets_data': [
                    1, 1, 1,
                ],
            },
            {
                # id = 2
                # will be used: to enrich the previous one's regions
                'model_id': 1,
                'mbo_params': _mbo_params({1: [1]}),
                'numeric_params': _numeric_params({41: [140]}),
                'downloadable': True,
                'regions': '2 3',
                'int_regions': [2, 3],
            },
            {
                # id = 3
                # will be used: has only mbo_params
                'model_id': 1,
                'mbo_params': _mbo_params({1: [1]}),
                'downloadable': True,
                'regions': '1 2',
                'int_regions': [1, 2],
            },
            {
                # id = 4
                # will be used: has only numeric_params
                'model_id': 1,
                'numeric_params': _numeric_params({41: [140]}),
                'downloadable': True,
                'regions': '1 2',
                'int_regions': [1, 2],
            },
            {
                # id = 0
                # will be used: new model_id
                'model_id': 2,
                'mbo_params': _mbo_params({1: [1]}),
                'numeric_params': _numeric_params({41: [140]}),
                'downloadable': True,
                'regions': '1',
                'int_regions': [1]
            },
            {
                # id = 1
                # will be used: different mbo_params
                'model_id': 2,
                'mbo_params': _mbo_params({1: [1], 2: [3, 4]}),
                'numeric_params': _numeric_params({41: [140], 42: [141, 142]}),
                'downloadable': True,
                'regions': '1',
                'int_regions': [1]
            },

            # invalid genlogs
            {
                # skipped: downloadable == False
                'model_id': 2,
                'mbo_params': _mbo_params({2: [5]}),
                'numeric_params': _numeric_params({42: [143]}),
                'downloadable': False,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
            },
            {
                # skipped: no valid mbo params
                'model_id': 2,
                'mbo_params': _mbo_params({3: [5]}),
                'numeric_params': _numeric_params({43: [143]}),
                'downloadable': True,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
            },
            {
                # model_id == 0, skipped
                'model_id': 0,
                'mbo_params': _mbo_params({1: [2]}),
                'numeric_params': _numeric_params({41: [141]}),
                'downloadable': True,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
            },
            {
                # contex, skipped
                'model_id': 2,
                'mbo_params': _mbo_params({2: [5]}),
                'numeric_params': _numeric_params({42: [143]}),
                'downloadable': True,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
                'contex_info': {
                    'original_msku_id': 100,
                },
            },
            {
                # direct, skip
                'model_id': 2,
                'mbo_params': _mbo_params({2: [5]}),
                'numeric_params': _numeric_params({43: [144]}),
                'downloadable': True,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
                'flags': OfferFlags.IS_DIRECT.value
            },
            {
                # Lavka, skip
                'model_id': 2,
                'mbo_params': _mbo_params({2: [5]}),
                'numeric_params': _numeric_params({43: [144]}),
                'downloadable': True,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
                'flags': OfferFlags.IS_LAVKA.value
            },
            {
                # Eda, skip
                'model_id': 2,
                'mbo_params': _mbo_params({2: [5]}),
                'numeric_params': _numeric_params({43: [144]}),
                'downloadable': True,
                'regions': '1 2 3',
                'int_regions': [1, 2, 3],
                'flags': OfferFlags.IS_EDA_RESTAURANTS.value
            },
        ]

    def test_sanity(self):
        self.run_stats_calc(
            'OfferFiltersForModelsStats',
            json.dumps(self.gls))

        stats = [
            {
                'model_id': 1,
                'record_type': NON_NUMERIC_PARAM_RECORD,
                'param_id': 1,
                'non_numeric_param_value': 1,
                'offers': [1, 2],
            },
            {
                'model_id': 1,
                'record_type': NON_NUMERIC_PARAM_RECORD,
                'param_id': 1,
                'non_numeric_param_value': 2,
                'offers': [3],
            },
            {
                'model_id': 1,
                'record_type': NUMERIC_PARAM_RECORD,
                'param_id': 41,
                'numeric_param_value': 140,
                'offers': [0, 2],
            },
            {
                'model_id': 1,
                'record_type': NUMERIC_PARAM_RECORD,
                'param_id': 41,
                'numeric_param_value': 141,
                'offers': [3],
            },
            {
                'model_id': 1,
                'record_type': REGION_RECORD,
                'region_id': 1,
                'offers': [0, 1, 2, 3],
            },
            {
                'model_id': 1,
                'record_type': REGION_RECORD,
                'region_id': 2,
                'offers': [0, 1, 2, 3],
            },
            {
                'model_id': 1,
                'record_type': REGION_RECORD,
                'region_id': 3,
                'offers': [2],
            },
            {
                'model_id': 1,
                'record_type': BOOKING_RECORD,
                'region_id': 1,
                'offers': [2, 3],
            },
            {
                'model_id': 1,
                'record_type': BOOKING_RECORD,
                'region_id': 2,
                'offers': [3],
            },
            {
                'model_id': 2,
                'record_type': NON_NUMERIC_PARAM_RECORD,
                'param_id': 1,
                'non_numeric_param_value': 1,
                'offers': [0, 1],
            },
            {
                'model_id': 2,
                'record_type': NON_NUMERIC_PARAM_RECORD,
                'param_id': 2,
                'non_numeric_param_value': 3,
                'offers': [1],
            },
            {
                'model_id': 2,
                'record_type': NON_NUMERIC_PARAM_RECORD,
                'param_id': 2,
                'non_numeric_param_value': 4,
                'offers': [1],
            },
            {
                'model_id': 2,
                'record_type': NUMERIC_PARAM_RECORD,
                'param_id': 41,
                'numeric_param_value': 140,
                'offers': [0, 1],
            },
            {
                'model_id': 2,
                'record_type': NUMERIC_PARAM_RECORD,
                'param_id': 42,
                'numeric_param_value': 141,
                'offers': [1],
            },
            {
                'model_id': 2,
                'record_type': NUMERIC_PARAM_RECORD,
                'param_id': 42,
                'numeric_param_value': 142,
                'offers': [1],
            },
            {
                'model_id': 2,
                'record_type': REGION_RECORD,
                'region_id': 1,
                'offers': [0, 1],
            },
        ]

        stat_path = self.tmp_file_path('offer_filters_for_models_stat.mmap')
        actual_stats = self.get_stats_from_mmap(stat_path, 'ModelFiltersInvertedIntersectorMmap')
        self.maxDiff = None
        self.assertEquals(sorted(stats), sorted(actual_stats))
