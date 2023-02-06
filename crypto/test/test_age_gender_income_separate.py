import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.rt_socdem.services.training.lib import age_gender_income


def test_age_gender_income_separate(yt_client, yql_client):
    raw_train_sample = tables.get_yson_table_with_schema(
        file_path='raw_train_sample.yson',
        cypress_path='//home/inputs/raw_train_sample',
        schema=[
            {'name': 'yandexuid', 'type': 'uint64'},
            {'name': 'age_segment', 'type': 'string'},
            {'name': 'crypta_id', 'type': 'uint64'},
            {'name': 'gender', 'type': 'string'},
            {'name': 'income_segment', 'type': 'string'},
            {'name': 'income_segment_weight', 'type': 'double'},
            {'name': 'profile', 'type': 'string'},
        ],
    )

    gender_output_table = tables.YsonTable(
        file_path='gender_output_table.yson',
        cypress_path='//home/outputs/gender_output_table',
        yson_format='pretty',
    )

    age_output_table = tables.YsonTable(
        file_path='age_output_table.yson',
        cypress_path='//home/outputs/age_output_table',
        yson_format='pretty',
    )

    income_output_table = tables.YsonTable(
        file_path='income_output_table.yson',
        cypress_path='//home/outputs/income_output_table',
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            age_gender_income.separate,
            yt_client=yt_client,
            yql_client=yql_client,
            raw_train_sample_table=raw_train_sample.cypress_path,
            gender_output_table=gender_output_table.cypress_path,
            age_output_table=age_output_table.cypress_path,
            income_output_table=income_output_table.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (raw_train_sample, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (gender_output_table, tests.Diff()),
            (age_output_table, tests.Diff()),
            (income_output_table, tests.Diff()),
        ],
    )
