import functools
import os

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.rt_socdem.services.training.lib import raw_train_sample


def test_raw_train_sample_get(yt_client, yql_client, date):
    offline_socdem_training_table = '//inputs/offline_socdem/{}/raw_train_sample'
    beh_regular_input_dir = '//inputs/beh_regular_input_dir'

    offline_age_train_sample = tables.get_yson_table_with_schema(
        file_path='offline_age_train_sample.yson',
        cypress_path=offline_socdem_training_table.format('age'),
        schema=[
            {'name': 'yandexuid', 'type': 'uint64'},
            {'name': 'age_segment', 'type': 'string'},
            {'name': 'crypta_id', 'type': 'uint64'},
        ],
    )

    offline_gender_train_sample = tables.get_yson_table_with_schema(
        file_path='offline_gender_train_sample.yson',
        cypress_path=offline_socdem_training_table.format('gender'),
        schema=[
            {'name': 'yandexuid', 'type': 'uint64'},
            {'name': 'crypta_id', 'type': 'uint64'},
            {'name': 'gender', 'type': 'string'},
        ],
    )

    offline_income_train_sample = tables.get_yson_table_with_schema(
        file_path='offline_income_train_sample.yson',
        cypress_path=offline_socdem_training_table.format('income'),
        schema=[
            {'name': 'yandexuid', 'type': 'uint64'},
            {'name': 'crypta_id', 'type': 'uint64'},
            {'name': 'income_segment', 'type': 'string'},
            {'name': 'income_segment_weight', 'type': 'double'},
        ],
    )

    beh_regular_input = tables.get_yson_table_with_schema(
        file_path='beh_regular_input.yson',
        cypress_path=os.path.join(beh_regular_input_dir, date),
        schema=[
            {'name': 'UniqID', 'type': 'uint64'},
            {'name': 'TimeStamp', 'type': 'uint32'},
            {'name': 'ProfileDump', 'type': 'string'},
        ],
    )

    raw_sample_output_table = tables.YsonTable(
        file_path='raw_sample_output_table.yson',
        cypress_path='//home/outputs/raw_sample_output_table',
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            raw_train_sample.get,
            yt_client=yt_client,
            yql_client=yql_client,
            yesterday=date,
            beh_regular_input_dir=beh_regular_input_dir,
            offline_socdem_training_table=offline_socdem_training_table,
            output_table=raw_sample_output_table.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (offline_age_train_sample, tests.TableIsNotChanged()),
            (offline_gender_train_sample, tests.TableIsNotChanged()),
            (offline_income_train_sample, tests.TableIsNotChanged()),
            (beh_regular_input, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (raw_sample_output_table, tests.Diff()),
        ],
    )
