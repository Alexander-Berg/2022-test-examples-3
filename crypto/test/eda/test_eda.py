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


def get_eda_order_schema():
    return schema_utils.get_strict_schema([
        {"name": "currency_code", "type": "string"},
        {"name": "id", "type": "int64"},
        {"name": "latest_revision_id", "type": "int64"},
        {"name": "location_latitude", "type": "double"},
        {"name": "location_longitude", "type": "double"},
        {"name": "phone_number", "type": "string"},
        {"name": "place_id", "type": "int64"},
        {"name": "status", "type": "int64"},
        {"name": "user_order_id", "type": "string"},
        {"name": "utc_cancelled_dttm", "type": "string"},
        {"name": "utc_created_dttm", "type": "string"},
        {"name": "utc_delivered_dttm", "type": "string"},
    ])


def get_eda_order_revision_schema():
    return schema_utils.get_strict_schema([
        {"name": "composition_id", "type": "int64"},
        {"name": "id", "type": "int64"},
    ])


def get_eda_order_revision_item_schema():
    return schema_utils.get_strict_schema([
        {"name": "composition_id", "type": "int64"},
        {"name": "name", "type": "string"},
        {"name": "place_menu_item_id", "type": "int64"},
        {"name": "price", "type": "int64"},
        {"name": "quantity", "type": "int64"},
    ])


def get_eda_place_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "int64"},
        {"name": "name", "type": "string"},
    ])


def get_eda_place_menu_categoty_schema():
    return schema_utils.get_strict_schema([
        {"name": "category_name", "type": "string"},
        {"name": "place_id", "type": "int64"},
        {"name": "place_menu_category_id", "type": "int64"},
    ])


def get_eda_place_menu_item_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "int64"},
        {"name": "place_menu_category_id", "type": "int64"},
    ])


def get_eda_dm_order_schema():
    return schema_utils.get_strict_schema([
        {"name": "order_id", "type": "int64"},
        {"name": "place_type", "type": "string"},
        {"name": "flow_type", "type": "string"},
        {"name": "order_items_cnt", "type": "int64"},
        {"name": "application_platform", "type": "string"},
        {"name": "payment_method_id", "type": "int64"},
    ])


def get_eda_matching_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "primary_entity_code": "string",
        "primary_entity_text": "string",
        "secondary_entity_code": "string",
        "secondary_entity_text": "string",
    }))


def get_eda_users_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "user_id": "uint64",
        "yandex_uid": "string",
    }))


def test_basic(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    output_table = helpers.get_tx_table_path(config.DataDir, ETransactionSource.EDA)
    stats_daily_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "daily"), ETransactionSource.EDA)
    stats_monthly_table = helpers.get_tx_table_path(yt.ypath_join(config.StatsDir, "monthly"), ETransactionSource.EDA)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/tx/services/data_import/bin/crypta-tx-data-import"),
        args=[
            "--config", config_file,
            "eda",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("eda_order.yson", yt.ypath_join(config.EdaOrderDir, "table"), get_eda_order_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("eda_order_revision.yson", yt.ypath_join(config.EdaOrderRevisionDir, "table"), get_eda_order_revision_schema()), [tests.TableIsNotChanged()]),
            (
                tables.get_yson_table_with_schema("eda_order_revision_item.yson", yt.ypath_join(config.EdaOrderRevisionItemDir, "table"), get_eda_order_revision_item_schema()),
                [tests.TableIsNotChanged()]
            ),
            (tables.get_yson_table_with_schema("eda_place.yson", config.EdaPlaceTable, get_eda_place_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("eda_place_menu_category.yson", config.EdaPlaceMenuCategoryTable, get_eda_place_menu_categoty_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("eda_place_menu_item.yson", yt.ypath_join(config.EdaPlaceMenuItemDir, "table"), get_eda_place_menu_item_schema()), [tests.TableIsNotChanged()]),
            (tables.get_empty_table_with_schema(config.MatchingTables[0], schema.get_matching_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("eda_dm_order.yson", yt.ypath_join(config.EdaDmOrderDir, "table"), get_eda_dm_order_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("eda_matching.yson", yt.ypath_join(config.EdaMatchingDir, "table"), get_eda_matching_schema()), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("eda_users.yson", config.EdaUsersTable, get_eda_users_schema()), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (
                tables.YsonTable("eda.yson", output_table, yson_format="pretty"),
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
