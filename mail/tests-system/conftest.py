# flake8: noqa
import pytest
from pycommon.fake_server import *


def pytest_configure():
    pytest.fake_provider = fake_chunked_server(host="localhost", port=19998)
    pytest.fake_provider2 = fake_chunked_server(host="localhost", port=19997)
