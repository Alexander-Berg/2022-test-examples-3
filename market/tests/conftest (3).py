import pytest
import os.path

import yatest.common
from yatest.common.network import PortManager

pytest_plugins = ['pytest_userver.plugins', 'pytest_userver.plugins.samples']


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
    path = os.path.relpath(yatest.common.test_source_path('../userver-samples-hello_service'), yatest.common.source_path())
    return yatest.common.binary_path(
        path
    )


@pytest.fixture(scope='session')
def service_port():
    pm = PortManager()
    port = pm.get_port()
    return port
