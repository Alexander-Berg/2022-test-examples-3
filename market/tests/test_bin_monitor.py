# coding: utf-8

"""Integration tests for the ghoul library.
"""

import os
import subprocess
import six
import yatest.common

import fixtures
from fixtures import cores_dir, ghoul_confs  # noqa
from market.idx.admin.ghoul.lib.ghoul import FRESH_CORES_PROMPT, ALL_CORES_PROMPT


def run_ghoul_monitor(cores_dir, ghoul_confs):  # noqa
    devnull = open(os.devnull, 'w')
    cmd = [
        yatest.common.binary_path('market/idx/admin/ghoul/bin/monitor/market-ghoul-monitor'),
        '--cores', cores_dir,
        '--conf-dir', ghoul_confs,
        '--core-len', str(fixtures.CORE_LEN),
        '--relevant-time', str(fixtures.NOW - 5 * 24 * 60 * 60),
        '--fresh-time', str(fixtures.NOW - 5 * 24 * 60 * 60),
        '--verbose',
    ]
    return six.ensure_str(subprocess.check_output(cmd, stderr=devnull))


def test_ghoul_binary(cores_dir, ghoul_confs):  # noqa
    output = run_ghoul_monitor(cores_dir, ghoul_confs)
    assert output.startswith('1;')
    assert FRESH_CORES_PROMPT in output
    assert ALL_CORES_PROMPT in output


def test_ghoul_binary_bad_cores(ghoul_confs):  # noqa
    output = run_ghoul_monitor('/bogus', ghoul_confs)
    assert output.startswith('2;')
    assert FRESH_CORES_PROMPT not in output
    assert ALL_CORES_PROMPT not in output


def test_ghoul_binary_bad_binaries(cores_dir):  # noqa
    output = run_ghoul_monitor(cores_dir, '/bogus')
    assert output.startswith('2;')
    assert FRESH_CORES_PROMPT not in output
    assert ALL_CORES_PROMPT not in output
