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


def get_market_schema():
    return schema_utils.get_strict_schema([
        {"name": "category_name", "type": "string"},
        {"name": "created_datetime", "type": "string"},
        {"name": "id", "type": "int64"},
        {"name": "item_count", "type": "int64"},
        {"name": "offer_price_rub_numeric", "type": "string"},
        {"name": "item_was_cancelled", "type": "boolean"},
        {"name": "item_was_delivered", "type": "boolean"},
        {"name": "msku_vendor_name", "type": "string"},
        {"name": "offer_name", "type": "string"},
        {"name": "order_buyer_region_populated_locality_id", "type": "int64"},
        {"name": "order_cancelled_datetime", "type": "string"},
        {"name": "order_delivered_datetime", "type": "string"},
        {"name": "order_puid", "type": "int64"},
        {"name": "order_uuid", "type": "string"},
        {"name": "order_yandexuid", "type": "string"},
        {"name": "supplier_name", "type": "string"},
        {"name": "category_hid", "type": "int64"},
        {"name": "model_id", "type": "int64"},
    ])


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    output_table = helpers.get_tx_table_path(config.DataDir, ETransactionSource.MARKET)
    stats_daily_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "daily"), ETransactionSource.MARKET)
    stats_monthly_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "monthly"), ETransactionSource.MARKET)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "market",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("market.yson", yt.ypath_join(config.MarketDir, "table"), get_market_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("puid_to_crypta_id.yson", config.MatchingTables[0], schema.get_matching_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (
                tables.YsonTable("market.yson", output_table, yson_format="pretty"),
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
