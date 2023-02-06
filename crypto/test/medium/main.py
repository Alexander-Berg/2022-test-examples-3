import os

import pytest
import yatest.common

from crypta.dmp.common.data.python import (
    bindings,
    meta,
)
from crypta.dmp.common.metrics import (
    coverage_metrics,
    meta_metrics,
)
from crypta.lib.python import time_utils
from crypta.lib.python.yql import executer
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


DATA_PATH = yatest.common.test_source_path("data")


@pytest.mark.parametrize("days", [0, 1], ids=["Total", "OneDay"])
def test_coverage_metrics(local_yt, local_yt_and_yql_env, days):
    os.environ.update(local_yt_and_yql_env)
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1500086400"

    local_yt.yt_wrapper.remove("//dmp", recursive=True, force=True)

    ext_id_bindings_table = tables.get_yson_table_with_schema("ext_id_bindings.yson", "//dmp/ext_id_bindings", bindings.get_ext_id_schema())
    yandexuid_bindings_table = tables.get_yson_table_with_schema("yandexuid_bindings.yson", "//dmp/yandexuid_bindings", bindings.get_yandexuid_schema())

    yql_executer = executer.get_executer(local_yt.get_server(), "pool", "//tmp")

    return tests.yt_test_func(
        local_yt.get_yt_client(),
        lambda: coverage_metrics.get(local_yt.yt_wrapper, yql_executer, ext_id_bindings_table.cypress_path, yandexuid_bindings_table.cypress_path, days),
        data_path=DATA_PATH,
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in [ext_id_bindings_table, yandexuid_bindings_table]
        ],
        return_result=True,
    )[0]


def test_meta_metrics(local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)

    local_yt.yt_wrapper.remove("//dmp", recursive=True, force=True)

    meta_table = tables.get_yson_table_with_schema("meta.yson", "//dmp/meta", meta.get_schema_with_sizes())

    return tests.yt_test_func(
        local_yt.get_yt_client(),
        lambda: meta_metrics.get(local_yt.yt_wrapper.read_table(meta_table.cypress_path)),
        data_path=DATA_PATH,
        input_tables=[(meta_table, tests.TableIsNotChanged())],
        return_result=True,
    )[0]
