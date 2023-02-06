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


def get_lavka_order_schema():
    return schema_utils.yt_schema_from_dict({
        "utc_created_dttm": "string",
        "utc_delivered_dttm": "string",
        "utc_cancelled_dttm": "string",
        "order_id": "string",
        "yandex_uid": "string",
        "region_id": "int64",
        "order_status": "int64",
        "order_sk": "string",
        "appmetrica_device_id": "string",
    })


def get_lavka_order_item_schema():
    return schema_utils.yt_schema_from_dict({
        "item_name": "string",
        "item_price_w_vat_rub": "double",
        "item_cnt": "int64",
        "lvl4_subcategory_name": "string",
        "order_sk": "string",
        "currency_code": "string",
    })


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    output_table = helpers.get_tx_table_path(config.DataDir, ETransactionSource.LAVKA)
    stats_daily_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "daily"), ETransactionSource.LAVKA)
    stats_monthly_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "monthly"), ETransactionSource.LAVKA)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "lavka",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("lavka_dm_order.yson", yt.ypath_join(config.LavkaDmOrderDir, "table"), get_lavka_order_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("lavka_dm_order_item.yson", yt.ypath_join(config.LavkaDmOrderItemDir, "table"), get_lavka_order_item_schema()), [tests.TableIsNotChanged()]),
            (tables.get_empty_table_with_schema(config.MatchingTables[0], schema.get_matching_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (
                tables.YsonTable("lavka.yson", output_table, yson_format="pretty"),
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
