# coding: utf-8

import os
import itertools
import pytest
import yatest.common

from market.idx.datacamp.dispatcher.yatf.test_env import DispatcherTestEnv
from market.idx.yatf.utils.common_proxy import validate_config_file


@pytest.fixture(scope='module')
def etc_dir():
    conf_dirs = [
        yatest.common.source_path(os.path.join('market', 'idx', 'datacamp', 'dispatcher', 'etc')),
        yatest.common.source_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'piper', 'etc')),
        yatest.common.source_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'etc'))
    ]

    etc = yatest.common.test_output_path('etc')
    os.mkdir(etc)
    for conf_dir in conf_dirs:
        for f in os.listdir(conf_dir):
            if not os.path.exists(os.path.join(etc, f)):
                os.symlink(os.path.join(conf_dir, f), os.path.join(etc, f))
    return etc


@pytest.mark.parametrize(
    'env, dc',
    itertools.product(
        ['testing', 'production'],
        ['sas', 'man', 'vla']
    )
)
def test_dispatcher_config_files(etc_dir, env, dc):
    conf_file = os.path.join(etc_dir, 'united.deploy.cfg')
    env_file = os.path.join(etc_dir, 'env', 'dispatcher.{}.united'.format(env))
    lua_variables = {
        'DC': dc,
        'DATACAMP_TVM_SECRET': 'tsss',
        'PORT': 12345,
        'QUOTER_PORT': 12346
    }
    validate_config_file(DispatcherTestEnv.dispatcher_bin(), conf_file, [env_file], lua_variables, logs_dir='app/log/dispatcher')
