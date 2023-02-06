from functools import partial
import os

import pytest
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.socdem_helpers import (
    socdem_config,
    test_utils,
)
from crypta.profile.lib.socdem_helpers.inference_utils import inference


@pytest.fixture()
def yandexuid_daily_vectors_table():
    on_write = tables.OnWrite(
        attributes={
            'schema': schema_utils.yt_schema_from_dict({
                'yandexuid': 'uint64',
                'vector': 'string',
            }),
        },
        row_transformer=test_utils.row_transformer_to_add_vector,
    )
    return tables.YsonTable('yandexuid_daily_vectors.yson', '//home/socdem/yandexuid_daily_vectors', on_write=on_write)


@pytest.fixture()
def neuro_raw_yandexuid_profiles_table():
    return tables.YsonTable('neuro_raw_yandexuid_profiles.yson',
                            '//home/socdem/neuro_raw_yandexuid_profiles',
                            yson_format='pretty')


def round_predictions(row):
    for socdem_type in socdem_config.yet_another_segment_names_by_label_type:
        for socdem_segment, probability in row[socdem_type].items():
            row[socdem_type][socdem_segment] = round(probability, 2)

    yield row


def test_batch_model_applier(yt_stuff, yandexuid_daily_vectors_table, neuro_raw_yandexuid_profiles_table):
    os.environ['YT_TOKEN'] = '__FAKE_YT_TOKEN__'
    yt_client = yt_stuff.get_yt_client()
    nn_model_list = test_utils.get_nn_models_list()
    format_functions = [partial(inference.get_format_function, socdem_type=socdem_type)
                        for socdem_type in socdem_config.SOCDEM_TYPES]
    nn_models_with_format_functions = list(zip(nn_model_list, format_functions))

    def get_neuro_predictions():
        yt_client.run_map(
            inference.BatchModelApplyerMapper(
                models_list=nn_models_with_format_functions,
                additional_columns=('yandexuid',),
                update_time=0,
                batch_size=4,
            ),
            yandexuid_daily_vectors_table.cypress_path,
            neuro_raw_yandexuid_profiles_table.cypress_path,
            spec={'title': 'Vector classification'},
        )
        yt_client.run_map(
            round_predictions,
            neuro_raw_yandexuid_profiles_table.cypress_path,
            neuro_raw_yandexuid_profiles_table.cypress_path,
            spec={'title': 'Round predictions for testing'},
        )

    return tests.yt_test_func(
        yt_client,
        func=get_neuro_predictions,
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (yandexuid_daily_vectors_table, tests.TableIsNotChanged()),
        ],
        output_tables=[
            (neuro_raw_yandexuid_profiles_table, tests.Diff()),
        ],
    )
