import yatest.common

from crypta.data_import.proto.realty_pb2 import TRealty
from crypta.data_import.services.realty.lib.task.config_pb2 import TConfig
from crypta.lib.python import yaml_config
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def get_homework_unified_id_schema():
    return schema_utils.get_strict_schema([
        {"name": "unified_id", "type": "string"},
        {"name": "source_unified_id", "type": "string"},
        {"name": "predicted_home", "type": "any"},
        {"name": "manual_homes", "type": "any"},
    ])


def get_house_prices_stat_schema():
    return schema_utils.get_strict_schema([
        {"name": "latitude", "type": "double"},
        {"name": "longitude", "type": "double"},
        {"name": "median_sell_price_per_sqm", "type": "double"},
    ])


def test_realty(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

    latest_homework_date = "2021-07-05"
    output_table = config.DataTable
    stats_weekly_table = config.StatsTable

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/data_import/services/realty/bin/crypta-data-import-realty"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "homework_unified_id.yson",
                    config.HomeworkUnifiedIdTable.format(latest_homework_date),
                    get_homework_unified_id_schema(),
                ),
                [tests.TableIsNotChanged()],
            ),
            (
                tables.get_yson_table_with_schema(
                    "house_prices_stat.yson",
                    config.HousePricesStatTable,
                    get_house_prices_stat_schema()
                ),
                [tests.TableIsNotChanged()],
            ),
            (
                tables.YsonTable(
                    "/dev/null",
                    output_table,
                    on_write=tables.OnWrite(
                        attributes={
                            config.DataOutputAttributeName: "2021-07-04",
                        },
                    ),
                ),
                [],
            ),
        ],
        output_tables=[
            (
                tables.YsonTable("realty.yson", output_table, yson_format="pretty"),
                [
                    tests.Diff(),
                    tests.SchemaEquals(schema_utils.get_schema_from_proto(TRealty, key_columns=['UserId', 'UserIdType'])),
                    tests.AttrEquals("optimize_for", "lookup"),
                ],
            ),
            (
                tables.YsonTable("stats_weekly.yson", stats_weekly_table, yson_format="pretty"),
                [tests.Diff()],
            ),
        ],
        env=local_yt_and_yql_env,
    )
