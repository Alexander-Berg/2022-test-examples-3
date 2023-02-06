import logging

import pytest

from crypta.lib.python.logging import logging_helpers


@pytest.fixture(scope="session")
def mp_log():
    logging_helpers.configure_multiprocessing_deploy_logger(logging.getLogger())
