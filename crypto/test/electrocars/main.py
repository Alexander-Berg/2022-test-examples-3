import yatest.common
from yt.wrapper import ypath

from crypta.lib.python import time_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_electrocars(local_yt, local_yt_and_yql_env, config_file, config):
    diff = tests.Diff()

    local_yt_and_yql_env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1623099600"  # 2021-06-08 00:00:00

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/profile/services/precalculate_tables/bin/crypta-profile-precalculate-tables"),
        args=[
            "--config", config_file,
            "electrocars",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (_matching(config), [tests.TableIsNotChanged()]),
            (_geocube_log(config), [tests.TableIsNotChanged()]),
            (_electrocars_log(config), []),
        ],
        output_tables=[
            (tables.YsonTable("processed_tables.yson", config.GeocubeProdLog.TrackTable, yson_format="pretty"), [diff]),
            (tables.YsonTable("electrocars.yson", config.ElectroCarsTable, yson_format="pretty"), [diff]),
        ],
        env=local_yt_and_yql_env,
    )


def _matching(config):
    schema = schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "yandexuid": "uint64",
    }, sort_by=["id", "id_type"])
    return tables.get_yson_table_with_schema("matching.yson", config.IndeviceYandexuidMatchingTable, schema)


def _geocube_log(config):
    schema = schema_utils.yt_schema_from_dict({
        "yandexuid": "string",
        "uuid": "string",
        "usecase": "string",
        "origin": "string",
        "request_rubrics": "string",
        "timestamp": "int64",
    })
    return tables.get_yson_table_with_schema("geocube.yson", ypath.ypath_join(config.GeocubeProdLog.SourceDir, "2021-06-08", "navi"), schema)


def _electrocars_log(config):
    schema = schema_utils.yt_schema_from_dict({
        "yandexuid": "string",
        "timestamp": "int64",
    })
    return tables.get_yson_table_with_schema("electrocars.yson", config.ElectroCarsTable, schema)
