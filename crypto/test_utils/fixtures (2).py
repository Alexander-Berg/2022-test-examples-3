import pytest

from crypta.lib.python.juggler.test_utils.mock_juggler_server import MockJugglerServer


@pytest.fixture(scope="function")
def mock_juggler_server():
    with MockJugglerServer() as mock:
        yield mock


@pytest.fixture(scope="session")
def session_mock_juggler_server():
    with MockJugglerServer() as mock:
        yield mock
