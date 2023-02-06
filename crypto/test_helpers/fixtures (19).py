import pytest
import yatest.common

from crypta.siberia.bin.describer.lib.test_helpers.siberia_describer import SiberiaDescriber


pytest_plugins = [
    "crypta.siberia.bin.common.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def siberia_describer(request, describe_log_logbroker_config, local_ydb, setup):
    app_working_dir = yatest.common.test_output_path("describer")

    with SiberiaDescriber(working_dir=app_working_dir,
                          logbroker_config=describe_log_logbroker_config,
                          ydb_endpoint=local_ydb.endpoint,
                          ydb_database=local_ydb.database,
                          describing_batch_size=2,
                          stats_update_threshold=1,
                          frozen_time=getattr(request.module, "FROZEN_TIME", None)) as processor:
        yield processor
