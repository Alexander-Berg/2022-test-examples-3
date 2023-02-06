# coding: utf-8
from os.path import join as pj
import yatest.common


def test_fast_data():
    rearrs = 'search/web/rearrs_upper'
    models_path = yatest.common.build_path(pj(rearrs, 'rearrange.fast/special_event/coronavirus'))
    tester_bin = yatest.common.binary_path(
        pj(rearrs, 'tests/rearrange.fast/coronavirus/fast_data_tester/fast_data_tester')
    )
    result = yatest.common.execute(command=[tester_bin, '-p', models_path])
    assert result.exit_code == 0, result.std_err
