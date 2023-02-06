import pytest

from crypta.lib.python.solomon.test_utils.mock_solomon_server import MockSolomonServer


@pytest.fixture(scope="function")
def mock_solomon_server():
    with MockSolomonServer() as mock:
        yield mock
