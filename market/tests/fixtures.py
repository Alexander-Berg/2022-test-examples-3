# coding: utf-8

"""Fixtures providing files for testing ghoul.
"""

from __future__ import print_function

import calendar
import os
import time

import pytest
import yatest.common


def extract_timestamp(ts_str):
    ts = time.strptime(ts_str, '%Y%m%d%H%M%S')
    return calendar.timegm(ts)


NOW = extract_timestamp('20180116160000')
CORE_LEN = 15
CORE_NAMES = [
    'core.20180109130531.market-feedpars.6.ps01ht.923682.hash.disabled',
    'core.20180110125127.market-feedpars.6.ps01ht.290615.hash.disabled',
    'core.20180110154857.bash.6.ps01ht.49782.hash.disabled',
    'core.20180110224213.stats-convert.6.ps01ht.718996.hash.disabled',
    'core.20180110224941.stats-convert.6.ps01ht.844657.hash.disabled',
    'core.20180110225020.stats-convert.6.ps01ht.847392.hash.disabled',
    'core.20180110225550.stats-convert.6.ps01ht.940832.hash.disabled',
    'core.20180110225922.stats-convert.6.ps01ht.978225.hash.disabled',
    'core.20180110230342.stats-convert.6.ps01ht.9717.hash.disabled',
    'core.20180114151145.stats-convert.6.ps01ht.319588.hash.disabled',
    'core.20180114151823.stats-convert.6.ps01ht.438648.hash.disabled',
    'core.20180114153442.stats-convert.6.ps01ht.716865.hash.disabled',
    'core.20180114154503.stats-convert.6.ps01ht.890912.hash.disabled',
    'core.20180115174554.market-feedpars.6.ps01ht.878322.hash.disabled',
    'core.20180116153853.market-feedpars.6.ps01ht.969546.hash.disabled',
    'core.20180116154252.market-feedpars.6.ps01ht.1001042.hash.disabled',
    'core.20180116154325.stats-convert.6.ps01ht.43044.hash.disabled',
    'core.20180116154438.market-feedpars.6.ps01ht.33929.hash.disabled',
    'core.20180116154857.bash.6.ps01ht.49782.hash.disabled',
    'core.20180116154857.market-feedpars.6.ps01ht.49782.hash.disabled',
]

LAST_FP = extract_timestamp('20180116154857')
LAST_SC = extract_timestamp('20180116154325')


BINARIES = [
    'stats-convert',
    'market-feedparser',
]


@pytest.fixture()
def cores_dir():
    dir_path = yatest.common.test_output_path('cores')
    os.mkdir(dir_path)

    for name in CORE_NAMES:
        core_path = os.path.join(dir_path, name)
        core_ts = extract_timestamp(name.split('.')[1])
        open(core_path, 'w').close()
        os.utime(core_path, (core_ts, core_ts))

    return dir_path


@pytest.fixture()
def ghoul_confs():
    conf_dir = yatest.common.test_output_path('ghoul.d')
    os.mkdir(conf_dir)

    with open(os.path.join(conf_dir, 'stats-calc.conf'), 'w') as conf_file:
        print('#bash', file=conf_file)
        print(BINARIES[0], file=conf_file)

    with open(os.path.join(conf_dir, 'feedparser.conf'), 'w') as conf_file:
        print('# за морями есть лимоновый сад', file=conf_file)
        print('# я найду лимон и буду рад', file=conf_file)
        print(BINARIES[1] + ' # но я тебе не дам', file=conf_file)

    return conf_dir
