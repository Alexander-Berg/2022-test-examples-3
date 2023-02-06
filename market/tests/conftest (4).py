import pytest
import os.path

from testsuite import utils

import yatest.common
from yatest.common.network import PortManager

pytest_plugins = ['pytest_userver.plugins', 'pytest_userver.plugins.samples']

# /// [patch configs]
USERVER_CONFIG_HOOKS = ['userver_config_translations']


@pytest.fixture(scope='session')
def userver_config_translations(mockserver_info):
    def do_patch(config_yaml, config_vars):
        components = config_yaml['components_manager']['components']
        components['cache-http-translations'][
            'translations-url'
        ] = mockserver_info.url('v1/translations')

    return do_patch
    # /// [patch configs]


# /// [mockserver]
@pytest.fixture(autouse=True)
def mock_translations(mockserver, translations, mocked_time):
    @mockserver.json_handler('/v1/translations')
    def mock(request):
        return {
            'content': translations,
            'update_time': utils.timestring(mocked_time.now()),
        }

    return mock
    # /// [mockserver]


# /// [translations]
@pytest.fixture
def translations():
    return {
        'hello': {'en': 'hello', 'ru': 'Привет'},
        'welcome': {'ru': 'Добро пожаловать', 'en': 'Welcome'},
    }
    # /// [translations]


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
    path = os.path.relpath(yatest.common.test_source_path('../userver-samples-http_caching'), yatest.common.source_path())
    return yatest.common.binary_path(
        path
    )


@pytest.fixture(scope='session')
def service_port():
    pm = PortManager()
    port = pm.get_port()
    return port
