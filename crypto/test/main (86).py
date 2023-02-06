import copy

import yatest.common

from crypta.lib.python import time_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.common import test_helpers


def test_matching_table_uploader(local_ydb, local_yt, local_yt_and_yql_env, config, config_path, ydb_token):
    env = copy.deepcopy(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1579478400"
    env["YDB_TOKEN"] = ydb_token

    tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/matching_table_uploader/bin/crypta-siberia-matching-table-uploader"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("id_to_crypta_id__yt.yson", config.InputTable, _get_id_to_crypta_id_table_schema()), tests.TableIsNotChanged()),
        ],
        env=env,
    )

    return test_helpers.dump_id_to_crypta_id_dir(local_ydb)


def _get_id_to_crypta_id_table_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "crypta_id": "uint64",
    }, sort_by=["id", "id_type"]))
