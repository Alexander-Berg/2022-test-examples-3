import datetime
import os

import pytest

from crypta.lib.python import time_utils
from crypta.s2s.lib import test_helpers


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture
def state_ttl_days():
    return 1


@pytest.fixture
def frozen_time(state_ttl_days):
    result = str(1500000000 + int(datetime.timedelta(days=state_ttl_days).total_seconds()))
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture
def config_file(local_yt, state_ttl_days):
    return test_helpers.render_config_file(yt_proxy=local_yt.get_server(), state_ttl_days=state_ttl_days)


@pytest.fixture
def config(config_file):
    return test_helpers.read_config(config_file)
