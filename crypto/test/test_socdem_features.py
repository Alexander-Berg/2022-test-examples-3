import os

import numpy as np
import yatest.common

from crypta.lib.python.yt.test_helpers import tests
from crypta.profile.lib.socdem_helpers import socdem_config
from crypta.profile.lib.socdem_helpers.socdem import (
    MakeCatboostFeatures,
    MakeCatboostTrainingFeatures,
)
from crypta.profile.lib.socdem_helpers import test_utils
import crypta.profile.lib.socdem_helpers.tools.features as features_utils
from crypta.profile.lib.socdem_helpers.train_utils.train_helper import get_flat_cat_features_dict


def inference(yt_client, flat_features_dict, sample_for_catboost_classification,
              neuro_raw_yandexuid_profiles, merged_segments, merged_hits_by_yandexuid):
    with yt_client.Transaction(), yt_client.TempTable() as raw_yandexuid_profiles:
        yt_client.run_reduce(
            MakeCatboostFeatures(cat_features_dict=flat_features_dict),
            [
                neuro_raw_yandexuid_profiles,
                merged_segments,
                merged_hits_by_yandexuid,
            ],
            [
                raw_yandexuid_profiles,
                sample_for_catboost_classification,
            ],
            reduce_by='yandexuid',
        )

        features_inference = list(yt_client.read_table(sample_for_catboost_classification))
        features_inference = np.array([row['FloatFeatures'] for row in features_inference])
        return features_inference


def training(yt_client, flat_features_dict, socdem_train_sample):
    nn_models = test_utils.get_nn_models_list()

    with yt_client.Transaction(), yt_client.TempTable() as catboost_train_sample_table, \
            yt_client.TempTable() as catboost_test_sample_table:
        features_training_by_socdem_type = {}

        for socdem_type in socdem_config.SOCDEM_TYPES:
            training_data_offset = 1  # target
            has_weights = features_utils.check_weight_column_in_table(yt_client, socdem_type, socdem_train_sample)
            if has_weights:
                training_data_offset += 1  # weight

            yt_client.run_map(
                MakeCatboostTrainingFeatures(
                    socdem_type=socdem_type,
                    models_list=nn_models,
                    flat_features_dict=flat_features_dict,
                    has_weights=has_weights,
                ),
                socdem_train_sample,
                [
                    catboost_train_sample_table,
                    catboost_test_sample_table,
                ],
                spec={
                    'title': 'Prepare {} samples for catboost training'.format(socdem_type),
                },
            )

            features_training = np.array([
                list(map(float, row['value'].split('\t'))) for row in yt_client.read_table(catboost_train_sample_table)
            ])
            offset = socdem_config.SOCDEM_OFFSET + training_data_offset
            features_training_by_socdem_type[socdem_type] = features_training[:, offset:]

        return features_training_by_socdem_type


def test_features_equality(yt_stuff, socdem_train_sample, cat_features,
                           neuro_raw_yandexuid_profiles, merged_segments, merged_hits_by_yandexuid,
                           sample_for_catboost_classification):
    os.environ['YT_TOKEN'] = '__FAKE_YT_TOKEN__'
    yt_client = yt_stuff.get_yt_client()

    features_dicts, _ = tests.yt_test_func(
        yt_client,
        func=lambda: features_utils.download_cat_features_to_dict(
            yt=yt_client,
            yt_folder_path=cat_features['directory'],
            is_mobile=False,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[(table, tests.TableIsNotChanged()) for table in cat_features['tables']],
    )[0]
    test_utils.check_cat_features_dicts(features_dicts)
    flat_features_dict = get_flat_cat_features_dict(features_dicts, features_utils.cat_feature_types)

    training_result = tests.yt_test_func(
        yt_client,
        func=lambda: training(yt_client, flat_features_dict, socdem_train_sample.cypress_path),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[(socdem_train_sample, tests.TableIsNotChanged())]
    )

    features_training = test_utils.check_features_by_socdem_type_equality(training_result[0])
    assert features_training.shape[1] == len(flat_features_dict), 'Features number on training is wrong'

    inference_result = tests.yt_test_func(
        yt_client,
        func=lambda: inference(yt_client, flat_features_dict, sample_for_catboost_classification.cypress_path,
                               neuro_raw_yandexuid_profiles.cypress_path, merged_segments.cypress_path,
                               merged_hits_by_yandexuid.cypress_path),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[
            (neuro_raw_yandexuid_profiles, tests.TableIsNotChanged()),
            (merged_segments, tests.TableIsNotChanged()),
            (merged_hits_by_yandexuid, tests.TableIsNotChanged()),
        ],
    )
    features_inference = inference_result[0][:, socdem_config.SOCDEM_OFFSET:]
    assert features_inference.shape[1] == len(flat_features_dict), 'Features number on inference is wrong'

    assert np.allclose(features_training, features_inference), 'Feature values are different'

    return tests.yt_test_func(
        yt_client,
        func=lambda: inference(yt_client, flat_features_dict, sample_for_catboost_classification.cypress_path,
                               neuro_raw_yandexuid_profiles.cypress_path, merged_segments.cypress_path,
                               merged_hits_by_yandexuid.cypress_path),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        output_tables=[
            (sample_for_catboost_classification, tests.Diff()),
        ],
    )
