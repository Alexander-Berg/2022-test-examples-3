# coding: utf-8

import contextlib
import os
import itertools
import pytest
import yatest.common

from market.idx.yatf.common import get_binary_path
from market.idx.yatf.utils.common_proxy import validate_config_file


@pytest.fixture(scope='module')
def etc_dir():
    conf_dirs = [
        yatest.common.source_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'scanner', 'etc')),
        yatest.common.source_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'etc'))
    ]

    etc = yatest.common.test_output_path('etc')
    os.mkdir(etc)
    for conf_dir in conf_dirs:
        for f in os.listdir(conf_dir):
            os.symlink(os.path.join(conf_dir, f), os.path.join(etc, f))
    return etc


@contextlib.contextmanager
def _testdir(path):
    top = yatest.common.test_output_path()
    os.makedirs(os.path.join(top, path))
    root_dir = path.split(os.sep)[0]
    os.symlink(os.path.join(top, root_dir), root_dir)

    try:
        yield
    finally:
        pass
        os.unlink(root_dir)


@pytest.mark.parametrize(
    'env, color, dc',
    itertools.product(
        ['testing', 'production'],
        ['united'],
        ['sas', 'vla', 'man']
    )
)
def test_new_piper_config_files(etc_dir, env, color, dc):
    conf_file = os.path.join(etc_dir, '{}.cfg'.format(color))
    env_file = os.path.join(etc_dir, 'env', '{}.{}'.format(env, color))
    lua_variables = {
        'DC': dc,
        'DATACAMP_TVM_SECRET': 'tsss',
        'PORT': 12345,
        'QUOTER_PORT': 12346,
        'CONTROLLER_PORT': 82,
        # override some env variables because piper and dispatcher have the same cfg file
        # what makes it difficult to check graph coherence
        'DISABLE_GATEWAY_FOR_REPORT': False,
    }
    bin_path = get_binary_path(
        os.path.join('market', 'idx', 'datacamp', 'controllers', 'scanner', 'test_config_bin', 'test_config'))
    validate_config_file(bin_path, conf_file, [env_file], lua_variables, logs_dir='app/log/scanner')
