import os

import pytest

from crypta.lib.python import time_utils


pytest_plugins = [
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def frozen_time():
    result = "1500000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    return result
