import os
import subprocess

import yatest

import common

EXTRACT_FML = yatest.common.source_path(os.path.join(common.RD_PREFIX, 'extract_fml.py'))


def check_fml_list(in_fml, out, expected_list):
    got_list = []
    dir_name = os.path.dirname(in_fml)
    for l in out.splitlines():
        assert os.path.dirname(l) == dir_name
        got_list += [os.path.basename(l)]
    assert sorted(expected_list) == sorted(got_list)


def test_extract():
    in_fml = os.path.join(common.DATA_PATH, 'realty_ru_desktop_softmax_ck5_3_otrok.xtd')
    out = subprocess.check_output([EXTRACT_FML, '--dry-run', '--overwrite', '--out_log', in_fml])
    check_fml_list(in_fml, out.strip(), [
        'realty_ru_desktop_softmax_ck5_3_otrok.xtd',
        'realty_ru_desktop_softmax_ck5_3_otrok_sub1.info',
        'realty_ru_desktop_softmax_ck5_3_otrok_sub2.info',
        'realty_ru_desktop_softmax_ck5_3_otrok_sub3.info',
        'realty_ru_desktop_softmax_ck5_3_otrok_sub4.mnmc',
    ])


def test_extract_extracted():
    in_fml = os.path.join(common.DATA_PATH, 'maps_ru_drake.xtd')
    out = subprocess.check_output([EXTRACT_FML, '--dry-run', '--overwrite', '--out_log', in_fml])
    check_fml_list(in_fml, out.strip(), [])
