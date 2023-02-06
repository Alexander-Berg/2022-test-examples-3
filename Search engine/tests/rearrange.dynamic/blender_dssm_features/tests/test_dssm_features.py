#!/usr/bin/env python2.7
# coding: utf-8

from os.path import join as pj
import yatest.common


def test_dssm_features():
    rearrs = 'search/web/rearrs_upper'
    dssm_features_path = yatest.common.build_path(pj(rearrs, 'rearrange.dynamic/blender_dssm_features'))
    tester_bin = yatest.common.binary_path(
        pj(rearrs, 'tests/rearrange.dynamic/blender_dssm_features/tester/tester')
    )
    result = yatest.common.execute(command=[tester_bin, '-p', dssm_features_path])
    assert result.exit_code == 0, result.std_err
