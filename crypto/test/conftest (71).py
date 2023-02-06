import mock
import pytest

NORMALIZED_FQDN = "socket_fqdn"
FQDN = "socket.fqdn"
TIMESTAMP = 1514374718.487364
TIMESTAMP_INT = int(TIMESTAMP)


@pytest.fixture
def mock_getfqdn():
    return mock.patch("crypta.lib.python.graphite.sender.socket.getfqdn", return_value=FQDN)


@pytest.fixture
def mock_time():
    return mock.patch("crypta.lib.python.graphite.sender.time.time", return_value=TIMESTAMP)
