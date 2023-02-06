import pytest
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.precalculated_tables import precalculated_tables


def get_direct_users_schema():
    return schema_utils.yt_schema_from_dict({
        "ClientID": "int64",
        "login": "string",
    })


def get_id_cryptaid_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
        "target_id": "string",
    })


def get_id_schema(id_column):
    return schema_utils.yt_schema_from_dict({
        id_column: "any",
        "comment": "any",
    })


def sort_rules_row_transformer(row):
    row["rule_lab_ids"] = sorted(row["rule_lab_ids"])
    return row


@pytest.fixture
def task(date):
    table_to_rule_lab_id = {
        '//home/yandexuid': {
            'rule-1': {
                'id_column': 'yandexuid',
                'id_type': 'yandexuid',
                'update_interval': 1,
            },
            'rule-2': {
                'id_column': 'yandexuid',
                'id_type': 'yandexuid',
                'update_interval': 7,
            },
        },
        '//home/idfa_gaid': {
            'rule-3': {
                'id_column': 'mobile_id',
                'id_type': 'idfa_gaid',
                'update_interval': 2,
            },
        },
        '//home/client_id': {
            'rule-4': {
                'id_column': 'direct_id',
                'id_type': 'client_id',
                'update_interval': 1,
            },
        },
    }

    rule_revision_ids = {1, 2, 3, 4}

    return precalculated_tables.GetStandardSegmentsByPrecalculatedTables(
        date=date,
        table_rules=table_to_rule_lab_id,
        rule_revision_ids=rule_revision_ids,
    )


@pytest.fixture
def empty_task(date):
    rule_revision_ids = {1, 2, 3, 4}

    return precalculated_tables.GetStandardSegmentsByPrecalculatedTables(
        date=date,
        table_rules={},
        rule_revision_ids=rule_revision_ids,
    )


def run_test(clean_local_yt, patched_config, task):
    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.get_yson_table_with_schema('direct_users.yson', task.input()["direct_users"].table, get_direct_users_schema()), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('gaid_cryptaid.yson', task.input()["gaid_cryptaid"].table, get_id_cryptaid_schema()), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('idfa_cryptaid.yson', task.input()["idfa_cryptaid"].table, get_id_cryptaid_schema()), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('yandexuid.yson', "//home/yandexuid", get_id_schema("yandexuid")), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('idfa_gaid.yson', "//home/idfa_gaid", get_id_schema("mobile_id")), tests.TableIsNotChanged()),
            (tables.get_yson_table_with_schema('client_id.yson', "//home/client_id", get_id_schema("direct_id")), tests.TableIsNotChanged()),
            (files.YtFile(
                yatest.common.binary_path("yql/udfs/crypta/identifiers/libcrypta_identifier_udf.so"),
                patched_config.CRYPTA_IDENTIFIERS_UDF_PATH,
            ), None),
        ],
        output_tables=[
            (tables.YsonTable(
                'rule_ids.yson',
                task.output().table,
                yson_format='pretty',
                on_read=tables.OnRead(row_transformer=sort_rules_row_transformer, sort_by=["id", "id_type"]),
            ), (tests.Diff())),
        ]
    )


def test_precalculated_tables(clean_local_yt, patched_config, task):
    return run_test(clean_local_yt, patched_config, task)


def test_no_precalculated_tables(clean_local_yt, patched_config, empty_task):
    return run_test(clean_local_yt, patched_config, empty_task)
