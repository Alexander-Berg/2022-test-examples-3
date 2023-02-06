import mail.python.no_yamake_tests  # noqa

import logging
import os
from urllib.parse import quote_plus

import pytest
from _pytest.fixtures import SubRequest
from library.python.testing.pyremock.lib.pyremock import mocked_http_server, MockHttpServer
from yatest.common import network

from mail.callmeback.devpack.components.api import CallmebackApi
from mail.callmeback.devpack.components.root import Callmeback
from mail.callmeback.tests.integration.helpers import CALLBACK_URI, cleanup
from mail.devpack.lib.coordinator import Coordinator
from mail.devpack.tests.helpers.fixtures import coordinator_context

log = logging.getLogger(__name__)

KEYS = []


@pytest.fixture(scope="session")
def coordinator() -> Coordinator:
    with coordinator_context(Callmeback) as coord:
        yield coord


@pytest.fixture(scope="session")
def api_url(coordinator) -> str:
    api: CallmebackApi = coordinator.components[CallmebackApi]
    return api.hostport


def find_free_port():
    with network.PortManager() as pm:
        return pm.get_port(8989)


@pytest.fixture(scope="session")
def target_local_port() -> int:
    return int(os.environ.get('CALLMEBACK_TARGET_LOCAL_PORT', find_free_port()))


@pytest.fixture(scope="session")
def target_hostname() -> str:
    return os.environ.get('CALLMEBACK_TARGET_HOSTNAME', 'localhost')


@pytest.fixture(scope="session")
def target(target_local_port: int, target_hostname: str) -> MockHttpServer:
    with mocked_http_server(target_local_port, host=target_hostname) as mock:
        yield mock


@pytest.fixture(scope="session")
def session_id():
    import string
    import random
    sid = ''.join(random.choices(string.hexdigits, k=5))
    log.info('Test session id: %s', sid)
    return sid


@pytest.fixture(scope="module")
def module_name(request: SubRequest):
    return request.module.__name__


@pytest.fixture(scope="function")
def test_name(request: SubRequest):
    return request.node.name


@pytest.fixture(scope="function")
def group_key(session_id, module_name, test_name):
    key = '_'.join((session_id, module_name, test_name))
    KEYS.append(key)
    return key


@pytest.fixture(scope="function")
def event_key():
    return 'event_key'


@pytest.fixture(scope="function")
def callback_uri(group_key):
    return f'{CALLBACK_URI}/{quote_plus(group_key)}'


@pytest.fixture(scope="session", autouse=True)
def cleanup_after_tests(api_url):
    yield
    for key in KEYS:
        cleanup(api_url, key)


@pytest.fixture(scope="function", autouse=True)
def step_reset(target):
    yield
    target.reset()
