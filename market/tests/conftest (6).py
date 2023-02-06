import pytest
import os.path

import yatest.common
from pathlib import Path

from yatest.common.network import PortManager

from testsuite.databases.pgsql import discover

pytest_plugins = [
    'pytest_userver.plugins',
    'pytest_userver.plugins.samples',
    # Database related plugins
    'testsuite.databases.pgsql.pytest_plugin',
]

USERVER_CONFIG_HOOKS = [
    'hook_db_config',
]


@pytest.fixture(scope='session')
def pgsql_local(service_source_dir, pgsql_local_create):
    databases = discover.find_schemas(
        'admin', [Path(os.path.join(service_source_dir, 'schemas/postgresql'))],
    )
    return pgsql_local_create(list(databases.values()))


@pytest.fixture(scope='session')
def hook_db_config(pgsql_local):
    def _hook_db_config(config_yaml, config_vars):
        components = config_yaml['components_manager']['components']
        components['key-value-database']['dbconnection'] = pgsql_local['admin'].get_uri()
    return _hook_db_config


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
    path = os.path.relpath(yatest.common.test_source_path('../userver-samples-postgres_service'), yatest.common.source_path())
    return yatest.common.binary_path(
        path
    )


@pytest.fixture(scope='session')
def service_port():
    pm = PortManager()
    port = pm.get_port()
    return port
