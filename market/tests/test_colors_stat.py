import json
import hashlib

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from utils import string_regions_to_list

COLOR_GLOB = 13887626
COLOR_VENDOR = 14871214


def _mbo_params(params_dict):
    """Maps a dictionary representation of mbo_params
    to the one that JSON can handle.
    """
    return [
        {'id': key, 'values': value}
        for key, value
        in params_dict.iteritems()]


def record(model_id, regions, mbo_params, contex_info=None, flags=0):
    return {
        'model_id': model_id,
        'binary_price': '122 1 0 RUR RUR',
        'geo_regions': regions,
        'int_regions': string_regions_to_list(regions),
        'int_geo_regions': string_regions_to_list(regions),
        'mbo_params': _mbo_params(mbo_params),
        'category_id': 2,
        'hid': 2,
        'binary_ware_md5': hashlib.md5(str(model_id) + str(regions) + str(mbo_params)).hexdigest(),
        'classifier_magic_id': 'ad1d66153519254f804f33eda7868cbd',
        'contex_info': contex_info,
        'flags': flags,
    }


def load_csv_colors(filename):
    def find_idcs(f):
        for s in f:
            if not s.startswith('@'):
                continue
            sp = s.split('\t', -1)
            return sp.index('NOFFER_COLOR_GLOB'), sp.index('NOFFER_COLOR_VENDOR')
        return -1, -1

    res = {}
    with open(filename) as f:
        color_glob_i, color_vend_i = find_idcs(f)
        assert color_glob_i > 0
        assert color_vend_i > 0
        for s in f:
            if not s.startswith('a'):
                continue
            sp = s.split('\t', -1)
            model_id = sp[1]
            noff = sp[2]
            c_gl = sp[color_glob_i]
            c_vr = sp[color_vend_i]
            if model_id not in res:
                res[model_id] = []
            res[model_id].append((noff, c_gl, c_vr))

    return res


class TestColorsStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestColorsStats, self).setUp()

        # generation logs:
        # - offer id is assigned internally as a model_id-unique counter
        # - model_id doesn't have to be real
        self.gls = [
            record(1, '213 215', {1: [2]}),                              # no colors
            record(1, '213 215', {COLOR_GLOB: [1], 3: [2]}),             # color_glob
            record(1, '215 217', {COLOR_VENDOR: [1]}),                   # color_vendor
            record(1, '213 215', {COLOR_GLOB: [1], COLOR_VENDOR: [2]}),  # color_glob, color_vendor

            record(2, '213', {1: [1]}),               # new model_id, no colors
            record(2, '213', {1: [1], 2: [3, 4]}),    # no color

            record(3, '213 215', {1: [2]}, contex_info={'original_msku': 100}),
            record(3, '213 215', {1: [2]}, flags=OfferFlags.IS_DIRECT.value),
            record(3, '213 215', {1: [2]}, flags=OfferFlags.IS_LAVKA.value),
            record(3, '213 215', {1: [2]}, flags=OfferFlags.IS_EDA_RESTAURANTS.value),
        ]

    def test_colors(self):
        self.run_stats_calc('GroupRegionalStats', json.dumps(self.gls))
        actual_stat = load_csv_colors(self.tmp_file_path('model_region_stats.csv'))
        assert len(actual_stat['2']) == 1
        assert actual_stat['2'] == [('2', '0', '0')]  # two offers without colors

        assert len(actual_stat['1']) == 3
        expected = [
            ('1', '0', '1'),    # 217
            ('3', '2', '1'),    # 213
            ('4', '2', '2'),    # 215
        ]
        for e in expected:
            assert e in actual_stat['1']
