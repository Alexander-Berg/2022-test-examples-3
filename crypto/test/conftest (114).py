import pytest

from crypta.lib.python import (
    test_utils,
    time_utils,
)


pytest_plugins = [
    'crypta.profile.lib.test_helpers.fixtures',
]


@pytest.fixture
def earliest_log_timestamp():
    yield '1500000000'


@pytest.fixture
def current_timestamp(earliest_log_timestamp):
    timestamp = str(int(earliest_log_timestamp) + 24 * 60 * 60)
    with test_utils.EnvironmentContextManager({time_utils.CRYPTA_FROZEN_TIME_ENV: timestamp}):
        yield timestamp
