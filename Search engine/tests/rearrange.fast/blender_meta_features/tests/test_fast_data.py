# coding: utf-8
from os.path import join as pj
import yatest.common


def test_fast_data():
    rearrs = 'search/web/rearrs_upper'
    fast_data_path = yatest.common.build_path(pj(rearrs, 'rearrange.fast/blender_meta_features'))
    tester_bin = yatest.common.binary_path(
        pj(rearrs, 'tests/rearrange.fast/blender_meta_features/fast_data_loader/fast_data_loader')
    )
    result = yatest.common.execute(command=[tester_bin, '-p', fast_data_path])
    assert result.exit_code == 0, result.std_err
