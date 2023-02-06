import random
import string

import pytest
from _pytest.fixtures import SubRequest

from mail.borador.devpack.components.borador import Borador, BoradorTesting
from mail.borador.devpack.components.unistat import Unistat
from mail.borador.devpack.components.all import All
from mail.devpack.tests.helpers.fixtures import coordinator_context
from library.python.testing.pyremock.lib.pyremock import mocked_http_server, MockHttpServer


def _random_string():
    letters = string.ascii_lowercase
    return ''.join(random.choice(letters) for _ in range(10))


@pytest.fixture(scope="session")
def coordinator():
    with coordinator_context(All) as coord:
        yield coord


@pytest.fixture(scope="session")
def borador(coordinator):
    return coordinator.components[Borador]


@pytest.fixture(scope="session")
def borador_testing(coordinator):
    return coordinator.components[BoradorTesting]


@pytest.fixture(scope="session")
def unistat(coordinator):
    return coordinator.components[Unistat]


@pytest.fixture(scope="session")
def local_port(borador: Borador) -> int:
    return borador.mock_port


@pytest.fixture(scope="session")
def aqua_mock(local_port) -> MockHttpServer:
    with mocked_http_server(local_port, host='localhost') as mock:
        yield mock


@pytest.fixture(scope="function")
def x_request_id(request: SubRequest):
    return f'{request.node.name}_{_random_string()}'
