import os
import tempfile

try:
    import cPickle as pickle
except:
    import pickle
import mock
import numpy as np
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
from crypta.profile.lib.socdem_helpers.simple_nn import SimpleNN
from crypta.profile.lib.socdem_helpers.train_utils import training
from crypta.profile.utils.loggers import get_stderr_logger


def get_nn_socdem_train_sample_table(socdem_type):
    on_write = tables.OnWrite(
        attributes={
            'schema': schema_utils.yt_schema_from_dict({
                'crypta_id': 'uint64',
                'gender': 'string',
                'income_segment': 'string',
                'income_segment_weight': 'double',
                'vector': 'string',
            }),
        },
        row_transformer=test_utils.row_transformer_to_add_vector,
    )
    return tables.YsonTable('socdem_train_sample.yson',
                            '//home/socdem/train_sample_{}'.format(socdem_type),
                            on_write=on_write)


@pytest.mark.parametrize('socdem_type', ['gender', 'income'])
def test_train_nn_model(yt_stuff, socdem_type):
    os.environ['YT_TOKEN'] = '__FAKE_YT_TOKEN__'
    yt_client = yt_stuff.get_yt_client()

    with mock.patch('crypta.profile.utils.utils.get_socdem_thresholds_from_api',
                    return_value=test_utils.test_thresholds), \
            tempfile.NamedTemporaryFile() as model_file:
        socdem_train_sample_table = get_nn_socdem_train_sample_table(socdem_type)
        tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: training.train_nn_model(
                yt_client=yt_client,
                socdem_type=socdem_type,
                train_table_path=socdem_train_sample_table.cypress_path,
                output_nn_model_file=model_file.name,
                logger=get_stderr_logger(),
            ),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[(socdem_train_sample_table, tests.TableIsNotChanged())],
        )

        model_file.seek(0)
        nn_model = pickle.load(model_file)
        assert isinstance(nn_model, SimpleNN)

        features = np.random.random((10, socdem_config.VECTOR_SIZE))
        predictions = nn_model.predict(features)
        assert features.shape[0] == predictions.shape[0], 'Samples number is wrong'

        socdem_segment = socdem_config.socdem_type_to_segment_name[socdem_type]
        assert predictions.shape[1] == len(socdem_config.segment_names_by_label_type[socdem_segment]), \
            'Classes number is wrong'


def get_catboost_predictions_table(socdem_type):
    schema = test_utils.get_predictions_table_schema(socdem_type)

    return tables.get_yson_table_with_schema(
        'catboost_predictions.yson',
        '//home/socdem/catboost_predictions_{}'.format(socdem_type),
        schema_utils.yt_schema_from_dict(schema),
    )


def test_calculate_and_send_metrics(yt_stuff):
    socdem_type = 'age'
    os.environ['YT_TOKEN'] = '__FAKE_YT_TOKEN__'
    yt_client = yt_stuff.get_yt_client()

    catboost_predictions_table = get_catboost_predictions_table(socdem_type)
    with mock.patch('crypta.profile.utils.utils.get_socdem_thresholds_from_api', return_value=test_utils.test_thresholds):
        tests.yt_test_func(
            yt_client=yt_client,
            func=lambda: training.calculate_and_send_metrics(
                yt_client=yt_client,
                socdem_type=socdem_type,
                table_path=catboost_predictions_table.cypress_path,
                send_metrics=False,
            ),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=[(catboost_predictions_table, tests.TableIsNotChanged())],
        )
