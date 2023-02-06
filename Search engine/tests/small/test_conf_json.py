import yatest.common

import common

import os
import re


def test_calcer_patch_absence():
    conf_path = yatest.common.build_path(os.path.join(common.REARRS_UPPER_BUILD_PREFIX, "conf", "conf.json"))
    with open(conf_path) as conf_json:
        calcer_patch_pattern = re.compile(r'\bCalcerPatch\b')
        for line in conf_json:
            assert calcer_patch_pattern.search(line) is None, 'https://wiki.yandex-team.ru/jandekspoisk/kachestvopoiska/blender/blenderfmlpatch/#vykatka'
