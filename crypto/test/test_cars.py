import yatest.common
from yt.wrapper import ypath

from crypta.data_import.proto.cars_pb2 import TCars
from crypta.data_import.services.cars.lib.task.config_pb2 import TConfig
from crypta.lib.python import yaml_config
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.tx.services.common import schema


def get_garage_schema():
    return schema_utils.get_strict_schema([
        {"name": "user_id", "type": "string"},
        {"name": "license_plate", "type": "string"},
        {"name": "status", "type": "string"},
        {"name": "card", "type": "any"},
    ])


def get_navicarinfo_schema():
    return schema_utils.get_strict_schema([
        {"name": "uid", "type": "string"},
        {"name": "licensePlateNumber", "type": "string"},
        {"name": "vin", "type": "string"},
        {"name": "color", "type": "string"},
        {"name": "engine", "type": "string"},
        {"name": "model", "type": "string"},
        {"name": "vendor", "type": "string"},
        {"name": "year", "type": "string"},
    ])


def test_cars(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

    date = "2021-07-13"
    output_table = config.DataTable
    stats_daily_table = config.StatsTable

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/data_import/services/cars/bin/crypta-data-import-cars"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "garage.yson",
                    config.GarageTable,
                    get_garage_schema()
                ),
                [tests.TableIsNotChanged()],
            ),
            (
                tables.get_yson_table_with_schema(
                    "navicarinfo.yson",
                    ypath.ypath_join(config.NavicarinfoDir, date),
                    get_navicarinfo_schema(),
                ),
                [tests.TableIsNotChanged()],
            ),
            (
                tables.get_yson_table_with_schema(
                    "puid_to_crypta_id.yson",
                    config.MatchingTables[0],
                    schema.get_matching_schema(),
                ),
                [tests.TableIsNotChanged()]),
            (
                tables.get_yson_table_with_schema(
                    "prev_data.yson",
                    output_table,
                    schema_utils.get_schema_from_proto(TCars, key_columns=['UserId', 'UserIdType']),
                ),
                [],
            ),
        ],
        output_tables=[
            (
                tables.YsonTable("cars.yson", output_table, yson_format="pretty"),
                [
                    tests.Diff(),
                    tests.SchemaEquals(schema_utils.get_schema_from_proto(TCars, key_columns=['UserId', 'UserIdType'])),
                    tests.AttrEquals("optimize_for", "lookup"),
                ],
            ),
            (
                tables.YsonTable("stats_daily.yson", stats_daily_table, yson_format="pretty"),
                [tests.Diff()],
            ),
        ],
        env=local_yt_and_yql_env,
    )
