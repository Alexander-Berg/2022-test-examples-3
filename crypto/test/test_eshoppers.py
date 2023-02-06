import os

import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.test_helpers import task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.coded_segments.common import eshoppers


def get_segment_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "segment_name": "string",
    }))


def get_market_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "order_date": "string",
        "yandexuid": "string",
    }))


def get_inputs(task):
    return [
        (
            tables.YsonTable(
                'market_cpa_clicks_log.yson',
                task.input()["MarketCpaClicksLog"].table,
                on_write=tables.OnWrite(attributes={'generate_date': task.date, "schema": get_segment_schema()}),
            ),
            tests.TableIsNotChanged()
        ),
        (
            tables.get_yson_table_with_schema(
                'eshoppers_by_ecom.yson',
                task.input()["EShoppersByEcom"].table,
                get_market_schema(),
            ),
            tests.TableIsNotChanged()
        ),
    ]


def get_outputs(task):
    table = task.output().table

    return [
        (
            tables.YsonTable(
                '{}.yson'.format(os.path.basename(table)),
                table,
                yson_format='pretty',
            ),
            tests.Diff(),
        ),
    ]


def test_get_export_tables(local_yt, patched_config, date):
    task = eshoppers.Eshoppers(date=date)

    return task_test_helpers.run_and_test_task(
        task=task,
        data_path=yatest.common.test_source_path('data/test_eshoppers'),
        yt=local_yt,
        input_tables=get_inputs(task),
        output_tables=get_outputs(task),
        dependencies_are_missing=False,
    )
