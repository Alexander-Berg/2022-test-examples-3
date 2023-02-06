import json

import pytest

import os.path

import yatest.common
from yatest.common.network import PortManager

from testsuite import utils

pytest_plugins = ['pytest_userver.plugins', 'pytest_userver.plugins.samples']


# /// [config hook]
USERVER_CONFIG_HOOKS = ['userver_config_configs_client']


@pytest.fixture(scope='session')
def userver_config_configs_client(mockserver_info):
    def do_patch(config_yaml, config_vars):
        components = config_yaml['components_manager']['components']
        components['dynamic-config-client'][
            'config-url'
        ] = mockserver_info.base_url.rstrip('/')

    return do_patch
    # /// [config hook]


@pytest.fixture(autouse=True)
def mock_config_server(mockserver, mocked_time):
    @mockserver.json_handler('/configs/values')
    def mock_configs_values(request):
        return {
            'configs': [],
            'updated_at': utils.timestring(mocked_time.now()),
        }

    return mock_configs_values


@pytest.fixture
def config_default_values(config_fallback_path):
    return json.load(config_fallback_path)


@pytest.fixture(scope='session')
def service_config_vars_path():
    return yatest.common.test_source_path('../config_vars.yaml')


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
    path = os.path.relpath(yatest.common.test_source_path('../userver-samples-production_service'), yatest.common.source_path())
    return yatest.common.binary_path(
        path
    )


@pytest.fixture(scope='session')
def service_port():
    pm = PortManager()
    port = pm.get_port()
    return port


@pytest.fixture(scope='session')
def monitor_port():
    pm = PortManager()
    port = pm.get_port()
    return port
