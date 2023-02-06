import yatest.common
import yt.wrapper as yt

from crypta.lib.python import yaml_config
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.tx.proto.transaction_source_pb2 import ETransactionSource
from crypta.tx.services.common import (
    helpers,
    schema,
)
from crypta.tx.services.data_import.lib.common_task.config_pb2 import TConfig


def get_edadeal_master_data_schema():
    return schema_utils.get_strict_schema([
        {"name": "area", "type": "string"},
        {"name": "brand_name_butara", "type": "string"},
        {"name": "category_butara", "type": "string"},
        {"name": "chain", "type": "string"},
        {"name": "check_date", "type": "string"},
        {"name": "check_id", "type": "int64"},
        {"name": "check_uploaded_at_timestamp", "type": "uint64"},
        {"name": "data_provider", "type": "string"},
        {"name": "description", "type": "string"},
        {"name": "price", "type": "double"},
        {"name": "quantity", "type": "double"},
        {"name": "user_id", "type": "string"},
        {"name": "check_geo_id", "type": "int64"},
        {"name": "check_inn", "type": "string"},
    ])


def get_edadeal_matching_schema():
    return schema_utils.get_strict_schema([
        {"name": "edadealUID", "type": "string"},
        {"name": "GAID", "type": "string"},
        {"name": "IDFA", "type": "string"},
        {"name": "puid", "type": "string"},
        {"name": "DeviceID", "type": "string"},
        {"name": "LastSeen", "type": "uint64"},
    ])


def get_edadeal_devices_light_schema():
    return schema_utils.yt_schema_from_dict({
        "edadealUID": "string",
        "puid": "string",
        "DeviceID": "string",
        "LastSeen": "string",
    })


def get_edadeal_geo_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "int64"},
        {"name": "geoid", "type": "int64"},
    ])


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    output_table = helpers.get_tx_table_path(config.DataDir, ETransactionSource.EDADEAL)
    stats_daily_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "daily"), ETransactionSource.EDADEAL)
    stats_monthly_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "monthly"), ETransactionSource.EDADEAL)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "edadeal",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("edadeal_master_data.yson", config.EdadealMasterDataTable, get_edadeal_master_data_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("edadeal_matching.yson", config.EdadealMatchingTable, get_edadeal_matching_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("edadeal_devices_light.yson", config.EdadealDevicesLightTable, get_edadeal_devices_light_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("edadeal_geo.yson", config.EdadealGeoTable, get_edadeal_geo_schema()), [tests.TableIsNotChanged()]),
            (tables.get_empty_table_with_schema(config.MatchingTables[0], schema.get_matching_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (
                tables.YsonTable("edadeal.yson", output_table, yson_format="pretty"),
                [tests.Diff(), tests.SchemaEquals(schema.get_transaction_schema()), tests.AttrEquals("optimize_for", "lookup")]
            ),
            (
                tables.YsonTable("stats_daily.yson", stats_daily_table, yson_format="pretty"),
                [tests.Diff()]
            ),
            (
                tables.YsonTable("stats_monthly.yson", stats_monthly_table, yson_format="pretty"),
                [tests.Diff()]
            ),
        ],
        env=local_yt_and_yql_env,
    )
