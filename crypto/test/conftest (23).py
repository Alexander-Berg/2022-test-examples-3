import logging
import random

import pytest

from crypta.cm.services.common.test_utils import id_utils
from crypta.lib.python.logging import logging_helpers

pytest_plugins = [
    "crypta.cm.services.common.test_utils.fixtures",
]

logging_helpers.configure_stdout_logger(logging.getLogger())


@pytest.fixture
def add_prefix_func():
    return id_utils.IdGenerator(random.randint(1000, 2000))
