import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.services.custom_lookalike.lib import lookalike


def test_lookalike_get_by_sample(yt_client, yql_client, describe_input, model_version, date):
    sample_table = tables.get_yson_table_with_schema(
        file_path='sample.yson',
        cypress_path='//home/inputs/sample',
        schema=[
            {'name': 'GroupID', 'type': 'string'},
            {'name': 'IdType', 'type': 'string'},
            {'name': 'IdValue', 'type': 'string'},
        ],
    )

    sizes_table = tables.get_yson_table_with_schema(
        file_path='sizes.yson',
        cypress_path='//home/inputs/sizes',
        schema=[
            {'name': 'GroupID', 'type': 'string'},
            {'name': 'Size', 'type': 'uint64'},
        ],
    )

    lookalike_table = tables.YsonTable(
        file_path='lookalike.yson',
        cypress_path='//home/outputs/lookalike',
        yson_format='pretty',
        on_read=tables.OnRead(row_transformer=tests.float_to_str),
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            lookalike.get_by_sample,
            yt_client=yt_client,
            yql_client=yql_client,
            sample_table=sample_table.cypress_path,
            lookalike_table=lookalike_table.cypress_path,
            sizes_table=sizes_table.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=describe_input + model_version(date) + [
            (sample_table, tests.TableIsNotChanged()),
            (sizes_table, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (lookalike_table, tests.Diff()),
        ],
    )
