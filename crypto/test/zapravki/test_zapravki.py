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


def get_zapravki_schema():
    return schema_utils.yt_schema_from_dict({
        "_id": "string",
        "document": "any",
    })


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    output_table = helpers.get_tx_table_path(config.DataDir, ETransactionSource.ZAPRAVKI)
    stats_daily_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "daily"), ETransactionSource.ZAPRAVKI)
    stats_monthly_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "monthly"), ETransactionSource.ZAPRAVKI)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "zapravki",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("tanker_orders.yson", config.ZapravkiTankerOrdersTable, get_zapravki_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("dictionary_gas_station.yson", config.ZapravkiGasStationDictTable, get_zapravki_schema()), [tests.TableIsNotChanged()]),
            (tables.get_empty_table_with_schema(config.MatchingTables[0], schema.get_matching_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (
                tables.YsonTable("zapravki.yson", output_table, yson_format="pretty"),
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
