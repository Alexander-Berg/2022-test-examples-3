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


def get_taxi_order_schema():
    return schema_utils.get_strict_schema([
        {"name": "currency_code", "type": "string"},
        {"name": "order_id", "type": "string"},
        {"name": "order_source_code", "type": "string"},
        {"name": "plan_destination_address", "type": "string"},
        {"name": "source_address", "type": "string"},
        {"name": "source_lat", "type": "double"},
        {"name": "source_lon", "type": "double"},
        {"name": "tariff_cost_w_discount", "type": "double"},
        {"name": "user_status", "type": "string"},
        {"name": "utc_order_created_dttm", "type": "string"},
        {"name": "utc_order_updated_dttm", "type": "string"},
        {"name": "yandex_uid", "type": "string"},
    ])


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    output_table = helpers.get_tx_table_path(config.DataDir, ETransactionSource.TAXI)
    stats_daily_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "daily"), ETransactionSource.TAXI)
    stats_monthly_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "monthly"), ETransactionSource.TAXI)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "taxi",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("taxi_order.yson", yt.ypath_join(config.TaxiOrderDir, "table"), get_taxi_order_schema()), [tests.TableIsNotChanged()]),
            (tables.get_empty_table_with_schema(config.MatchingTables[0], schema.get_matching_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (
                tables.YsonTable("taxi.yson", output_table, yson_format="pretty"),
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
