import pytest

import os.path

import yatest.common
from yatest.common.network import PortManager

pytest_plugins = [
    'pytest_userver.plugins',
    'pytest_userver.plugins.samples',
    # Database related plugins
    'testsuite.databases.mongo.pytest_plugin',
]

USERVER_CONFIG_HOOKS = ['mongo_config_hook']


@pytest.fixture(scope='session')
def get_mongo_collections():
    return {
    'translations': {
        'settings': {
            'collection': 'translations',
            'connection': 'admin',
            'database': 'admin',
        },
        'indexes': [],
    },
}


@pytest.fixture(scope='session')
def mongodb_settings(get_mongo_collections):
    return get_mongo_collections
    # /// [mongodb settings]


@pytest.fixture(scope='session')
def mongo_config_hook(mongo_connection_info):
    def _patch_config(config_yaml, config_vars):
        components = config_yaml['components_manager']['components']
        components['mongo-tr'][
            'dbconnection'
        ] = mongo_connection_info.get_uri('admin')

    return _patch_config


# /// [require mongodb]
@pytest.fixture
def client_deps(mongodb):
    pass
    # /// [require mongodb]


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
    path = os.path.relpath(yatest.common.test_source_path('../userver-samples-mongo_service'), yatest.common.source_path())
    return yatest.common.binary_path(
        path
    )


@pytest.fixture(scope='session')
def service_port():
    pm = PortManager()
    port = pm.get_port()
    return port
