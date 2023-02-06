import pytest

import yatest

from crypta.siberia.bin.segmentator.lib.test_helpers.local_segmentator import LocalSegmentator


pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def local_segmentator(segmentate_log_logbroker_config, local_ydb, setup):
    working_dir = yatest.common.test_output_path("segmentator")
    with LocalSegmentator(working_dir, local_ydb.endpoint, local_ydb.database, segmentate_log_logbroker_config) as segmentator:
        yield segmentator
