import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.rt_socdem.services.training.lib import catboost_train_sample


def test_catboost_train_sample_prepare(yt_client, yql_client, get_table_with_beh_profile):
    features_mapping_table = tables.get_yson_table_with_schema(
        file_path='features_mapping_table.yson',
        cypress_path='//home/inputs/features_mapping_table',
        schema=[
            {'name': 'feature_index', 'type': 'uint64'},
            {'name': 'description', 'type': 'string'},
            {'name': 'feature', 'type': 'string'},
        ],
    )

    income_raw_train_table = get_table_with_beh_profile(
        file_path='income_raw_train_table.yson',
        cypress_path='//home/inputs/income_raw_train_table',
        schema=[
            {'name': 'yandexuid', 'type': 'uint64'},
            {'name': 'crypta_id', 'type': 'uint64'},
            {'name': 'income_segment', 'type': 'int32'},
            {'name': 'income_segment_weight', 'type': 'double'},
            {'name': 'profile', 'type': 'string'},
        ],
        beh_profile_field='profile',
    )

    catboost_pool_file = tables.YsonTable(
        file_path='catboost_pool_file.yson',
        cypress_path='//home/outputs/catboost_pool_file',
        yson_format='pretty',
    )

    catboost_train_table = tables.YsonTable(
        file_path='catboost_train_table.yson',
        cypress_path='//home/outputs/catboost_train_table',
        yson_format='pretty',
    )

    catboost_val_table = tables.YsonTable(
        file_path='catboost_val_table.yson',
        cypress_path='//home/outputs/catboost_val_table',
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            catboost_train_sample.prepare,
            yt_client=yt_client,
            features_mapping_table_path=features_mapping_table.cypress_path,
            catboost_pool_file=catboost_pool_file.cypress_path,
            segments=['income_segment'],
            train_val_kwargs={
                'catboost_train_table': catboost_train_table.cypress_path,
                'catboost_val_table': catboost_val_table.cypress_path,
                'raw_train_table': income_raw_train_table.cypress_path,
                'validation_sample_percentage': 2,
                'validation_sample_rest': 1,
            },
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (features_mapping_table, tests.TableIsNotChanged()),
            (income_raw_train_table, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (catboost_pool_file, tests.Diff()),
            (catboost_train_table, tests.Diff()),
            (catboost_val_table, tests.Diff()),
        ],
    )
