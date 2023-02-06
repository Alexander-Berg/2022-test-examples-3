import os
import subprocess

import yatest

import common

COLLECT_FML = yatest.common.source_path(os.path.join(common.RD_PREFIX, 'collect_fml.py'))


def check_fml_list(in_fml, out, expected_list):
    got_list = []
    dir_name = os.path.dirname(in_fml)
    for l in out.splitlines():
        assert os.path.dirname(l) == dir_name
        got_list += [os.path.basename(l)]
    assert sorted(expected_list) == sorted(got_list)


def test_collect():
    in_fml = os.path.join(common.DATA_PATH, 'maps_ru_drake.xtd')
    out = subprocess.check_output([COLLECT_FML, '--dry-run', '--overwrite', '--embed_log', in_fml])
    check_fml_list(in_fml, out.strip(), [
        'maps_ru_drake_sub1.info',
        'maps_ru_drake_sub2.info',
        'maps_ru_drake_sub3.info',
    ])


def test_collect_list():
    in_fml = os.path.join(common.DATA_PATH, 'fresh.touch.ua.152379.xtd')
    out = subprocess.check_output([COLLECT_FML, '--list-only', '--overwrite', '--embed_log', in_fml])
    check_fml_list(in_fml, out.strip(), [
        'fresh.touch.ua.152379_sub1.info',
        'fresh.touch.ua.152379_sub2.info',
        'fresh.touch.ua.152379_sub3.info',
        'fresh.touch.ua.152379_sub4.info',
        'fresh.touch.ua.152379_sub5.info',
        'fresh.touch.ua.152379_sub6.info',
        'fresh.touch.ua.152379_sub7.info',
    ])
