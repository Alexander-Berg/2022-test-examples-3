import json

import pytest

import os.path

import yatest.common
from yatest.common.network import PortManager

pytest_plugins = [
    'pytest_userver.plugins',
    'pytest_userver.plugins.samples',
    # Database related plugins
    'testsuite.databases.redis.pytest_plugin',
]

# /// [service_env value]
SECDIST_CONFIG = {
    'redis_settings': {
        'taxi-tmp': {
            'password': '',
            'sentinels': [{'host': 'localhost', 'port': 26379}],
            'shards': [{'name': 'test_master0'}],
        },
    },
}


@pytest.fixture(scope='session')
def redis_settings(redis_sentinels):
    single_setting = {
        'password': '',
        'sentinels': redis_sentinels,
        'shards': [{
            'name': 'test_master0',
        }]
    }
    return {
        'taxi-tmp': single_setting
    }


@pytest.fixture(scope='session')
def service_env(redis_settings):
    return {'SECDIST_CONFIG': json.dumps({'redis_settings': redis_settings})}


@pytest.fixture
def client_deps(redis_store):
    pass


@pytest.fixture(scope='session')
def service_source_dir():
    return yatest.common.test_source_path('../')


@pytest.fixture(scope='session')
def build_dir():
    return yatest.common.test_source_path('../')


@pytest.fixture(scope='session')
def config_fallback_path():
    return yatest.common.test_source_path('../dynamic_config_fallback.json')


@pytest.fixture(scope='session')
def service_config_path():
    return yatest.common.test_source_path('../static_config.yaml')


@pytest.fixture(scope='session')
def service_binary():
    path = os.path.relpath(yatest.common.test_source_path('../userver-samples-redis_service'), yatest.common.source_path())
    return yatest.common.binary_path(
        path
    )


@pytest.fixture(scope='session')
def service_port():
    pm = PortManager()
    port = pm.get_port()
    return port
