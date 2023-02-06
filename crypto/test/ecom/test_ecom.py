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


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    def _get_ecom_path(folder):
        return helpers.get_tx_table_path(folder, ETransactionSource.ECOM)

    def _get_input_table(table_name):
        cypress_path = yt.ypath_join(config.EcomSalesNoExternalDir, table_name)
        local_path = "{}.yson".format(table_name)
        return tables.get_yson_table_with_schema(local_path, cypress_path, _get_ecom_schema())

    output_table = _get_ecom_path(config.DataDir)
    stats_daily_table = _get_ecom_path(yt.ypath_join(config.StatsDir, "daily"))
    stats_monthly_table = _get_ecom_path(yt.ypath_join(config.StatsDir, "monthly"))

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "ecom",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (table, [tests.TableIsNotChanged()])
            for table in (
                _get_input_table("2021-07-05"),
                _get_input_table("2021-07-01"),
                _get_input_table("2021-06-01"),
                tables.get_empty_table_with_schema(config.MatchingTables[0], schema.get_matching_schema()),
            )
        ],
        output_tables=[
            (
                tables.YsonTable("ecom.yson", output_table, yson_format="pretty"),
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


def _get_ecom_schema():
    return schema_utils.get_strict_schema([
        {"name": "additional_attributes", "type": "any"},
        {"name": "currency", "type": "string"},
        {"name": "domain", "type": "string"},
        {"name": "geo_id", "type": "int64"},
        {"name": "icookie", "type": "string"},
        {"name": "multiplier", "type": "double"},
        {"name": "order_date", "type": "string"},
        {"name": "order_id", "type": "string"},
        {"name": "order_time", "type": "string"},
        {"name": "parent2_name", "type": "string"},
        {"name": "parent1_name", "type": "string"},
        {"name": "parent0_name", "type": "string"},
        {"name": "price_per_item", "type": "double"},
        {"name": "quantity", "type": "double"},
        {"name": "shop_category", "type": "string"},
        {"name": "shop_title", "type": "string"},
        {"name": "shop_vendor", "type": "string"},
        {"name": "vendor_name", "type": "string"},
        {"name": "yandexuid", "type": "string"},
    ])
