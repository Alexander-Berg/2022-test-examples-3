# -*- coding: utf-8 -*-

import difflib
import hashlib
import logging
import os
import sys

import yatest.common

from library.python import resource


logger = logging.getLogger(__name__)

if yatest.common.context.test_stderr:
    logger.addHandler(logging.StreamHandler(stream=sys.stderr))


def test_statistics_config():
    configs_updater_dir = yatest.common.binary_path('market/yamarec/tools/config_updater')
    output_path = os.path.join(yatest.common.test_output_path(), 'statistics.config')

    cmd = [
        os.path.join(configs_updater_dir, 'config_updater'),
        '--output-path', output_path
    ]

    yatest.common.execute(cmd, check_exit_code=True)

    statistic_config = resource.find('/statistics.config')
    digest = hashlib.md5(statistic_config).hexdigest()

    with open(output_path, 'rb') as f:
        expected_statistic_config = f.read()
        expected_digest = hashlib.md5(expected_statistic_config).hexdigest()

    udiff_gen = difflib.unified_diff(
        open(output_path, 'r').read().splitlines(),
        statistic_config.splitlines(),
        fromfile="expected_statistics.config",
        tofile="current_statistics.config"
    )
    udiff = '\n'.join(udiff_gen)

    config_in_arcadia_path = 'market/yamarec/yamarec/yamarec1/resources/statistics_config/__root__'

    assert expected_digest == digest, "Generated config file '%s' was modified manually. Diff:\n%s" % (config_in_arcadia_path, udiff)
