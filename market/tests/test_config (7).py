# coding: utf-8

import pytest

import itertools
import os

from market.idx.datacamp.miner.yatf.test_env import miner_binary_path
from market.idx.yatf.utils.common_proxy import validate_config_file

import yatest.common


def etc_dir():
    return yatest.common.source_path(os.path.join('market', 'idx', 'datacamp', 'miner', 'etc'))


def conf_file(service):
    conf_file = os.path.join(etc_dir(), 'miner.{service}.cfg'.format(service=service))
    if not os.path.exists(conf_file):
        raise RuntimeError('Config file ({}) does not exists'.format(conf_file))

    return conf_file


def env_file(env, service):
    if service == 'united':
        env_file = os.path.join(etc_dir(), 'env', '{env}.white'.format(env=env))
    else:
        env_file = os.path.join(etc_dir(), 'env', '{env}.{service}'.format(env=env, service=service))
    if not os.path.exists(env_file):
        raise RuntimeError('Env file ({}) does not exists'.format(env_file))

    return env_file


@pytest.mark.parametrize(
    'env, service, dc',
    itertools.product(
        ['testing', 'production'],
        ['united', 'external', 'msku'],
        ['sas', 'vla', 'man']
    )
)
def test_config_file(env, service, dc):
    conf = conf_file(service)
    env = env_file(env, service)

    lua_variables = {
        'DC': dc,
        'DATACAMP_TVM_SECRET': 'tsss',
        'PORT': 12345,
    }

    validate_config_file(miner_binary_path(), conf, [env], lua_variables, logs_dir='app/log/miner')
