import os

import pytest

from crypta.lib.python import time_utils
from crypta.ltp.viewer.lib.ydb.client import Client

pytest_plugins = [
    "crypta.lib.python.ydb.test_helpers.fixtures",
]


@pytest.fixture
def frozen_time():
    result = "1590000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture
def client(clean_local_ydb, frozen_time):
    return Client(
        clean_local_ydb.endpoint,
        clean_local_ydb.database,
        "FAKE",
    )


@pytest.fixture
def clean_local_ydb(local_ydb):
    local_ydb.remove_all()
    return local_ydb
