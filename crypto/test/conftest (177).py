import logging

import pytest

from crypta.lib.python.logging import logging_helpers

pytest_plugins = [
    "crypta.styx.services.common.test_utils.fixtures",
]

logging_helpers.configure_stdout_logger(logging.getLogger())


@pytest.fixture(scope="module")
def min_delete_interval_sec():
    yield 60
