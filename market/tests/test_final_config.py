# coding: utf-8

import json
import logging
import os
import pytest
import six

from hamcrest import assert_that, equal_to
from kazoo.exceptions import NodeExistsError

from market.idx.admin.config_daemon.yatf.resources.local_configs import UserConfig
from market.idx.admin.config_daemon.yatf.resources.user_pidfile import UserPidfile
from market.idx.admin.config_daemon.yatf.test_env import DaemonTestEnv
from market.idx.yatf.resources.zookeeper_conf import ZookeeperConf

logger = logging.getLogger('tests.test_final_config')


@pytest.fixture()
def zk_path():
    return os.path.join('fast_config', 'offersrobot', 'testing', 'planeshift.stratocaster', 'sas')


@pytest.fixture()
def user_cfg():
    return {
        'user_section0': {
            'user_option0': 'user_value0',
            'user_option1': 'user_value1'
        },
        'user_section1': {
            'user_option1': 'user_value1'
        }
    }


@pytest.fixture()
def env_cfg():
    return {
        'env_section0': {
            'env_option0': 'env_value0'
        },
        'user_section1': {
            'user_option1': 'env_value1'
        }
    }


@pytest.fixture()
def mitype_cfg():
    return {
        'mitype_section0': {
            'mitype_option1': 'mitype_value1',
        },
        'user_section0': {
            'user_option1': 'mitype_value1'
        }
    }


@pytest.fixture()
def dc_cfg():
    return {
        'dc_section0': {
            'dc_option0': 'dc_option0'
        },
        'user_section0': {
            'user_option1': 'dc_option1'
        }
    }


@pytest.fixture()
def prepared_zk(zk, zk_path, user_cfg, env_cfg, mitype_cfg, dc_cfg):
    try:
        zk.create(zk_path, makepath=True)
    except NodeExistsError:
        pass

    path_nodes = zk_path.split('/')

    zk.set(os.path.join(*path_nodes[:2]), six.ensure_binary(json.dumps(user_cfg)))
    zk.set(os.path.join(*path_nodes[:3]), six.ensure_binary(json.dumps(env_cfg)))
    zk.set(os.path.join(*path_nodes[:4]), six.ensure_binary(json.dumps(mitype_cfg)))
    zk.set(os.path.join(*path_nodes), six.ensure_binary(json.dumps(dc_cfg)))
    return zk


@pytest.fixture(params=[False, True], ids=['once', 'daemon'])
def run_once(request):
    return request.param


@pytest.fixture(params=[False, True])
def config_daemon(request, prepared_zk, zk_path, run_once):
    yaconf_mode = request.param
    resources = {
        'user_config': UserConfig(yaconf_mode=yaconf_mode),
        'zk_config': ZookeeperConf(),
        'user_pidfile': UserPidfile()
    }

    daemon = DaemonTestEnv(env_type='testing',
                           mitype='planeshift.stratocaster',
                           zk=prepared_zk,
                           yaconf_mode=yaconf_mode,
                           run_once=run_once,
                           **resources)

    with daemon:
        daemon.execute()
        prepared_zk.delete(zk_path, recursive=True)
        yield daemon


def test_final_config(config_daemon):
    '''Тест для проверки уточнения параметров конфига при удалении от корня
    и корректной обработки переданного демону сигнала'''

    EXPECTED_CONFIG = {
        'user_section0': {
            'user_option0': 'user_value0',
            'user_option1': 'dc_option1'
        },
        'user_section1': {
            'user_option1': 'env_value1'
        },
        'env_section0': {
            'env_option0': 'env_value0'
        },
        'mitype_section0': {
            'mitype_option1': 'mitype_value1'
        },
        'dc_section0': {
            'dc_option0': 'dc_option0'
        }
    }
    result = config_daemon.final_config
    assert_that(result, equal_to(EXPECTED_CONFIG))
