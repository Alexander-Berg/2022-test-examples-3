import logging

import pytest

from crypta.lib.python.logbroker.test_helpers import consumer_utils


logger = logging.getLogger(__name__)

pytest_plugins = [
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.siberia.bin.common.test_helpers.fixtures",
    "crypta.siberia.bin.mutator.lib.test_helpers.fixtures",
]


@pytest.fixture(scope="function", autouse=True)
def setup(local_ydb, change_log_logbroker_client):
    logger.info("SETUP")
    local_ydb.remove_all()
    consumer_utils.read_all(change_log_logbroker_client.create_consumer())
