import pytest

import yatest

from crypta.siberia.bin.mutator.lib.test_helpers.local_mutator import LocalMutator


pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def local_mutator(change_log_logbroker_config, local_ydb, setup):
    working_dir = yatest.common.test_output_path("mutator")
    with LocalMutator(working_dir, local_ydb.endpoint, local_ydb.database, change_log_logbroker_config) as mutator:
        yield mutator
