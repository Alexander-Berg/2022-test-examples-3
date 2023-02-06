import os

import pytest

from crypta.lib.python import time_utils

pytest_plugins = [
    "crypta.audience.lib.test_helpers.fixtures",
]


@pytest.fixture
def frozen_time():
    result = "1590000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result
