import numpy as np

from crypta.profile.lib.socdem_helpers import socdem_config
import crypta.profile.lib.socdem_helpers.train_utils.models as models_utils


test_thresholds = {
    'gender': {'f': 0.6, 'm': 0.62},
    'income_5_segments': {'A': 0.21, 'B1': 0.42, 'B2': 0.24, 'C1': 0.27, 'C2': 0.08},
    'income_segments': {'A': 0.32, 'B': 0.5, 'C': 0.38},
    'user_age_6s': {'0_17': 0.3, '18_24': 0.25, '25_34': 0.36, '35_44': 0.32, '45_54': 0.27, '55_99': 0.34}
}

fields_types = {
    'gender': 'any',
    'user_age_6s': 'any',
    'income_5_segments': 'any',
    'age_scores': 'any',
    'gender_scores': 'any',
    'income_scores': 'any',
    'exact_socdem': 'any',
    'yandexuid': 'uint64',
    'id': 'string',
    'id_type': 'string',
    'is_active': 'boolean',
    'crypta_id': 'uint64',
    'update_time': 'uint64',
}


def row_transformer_to_add_vector(row):
    row['vector'] = np.ones(socdem_config.VECTOR_SIZE, dtype=np.float32).tostring()
    return row


def check_cat_features_dicts(cat_features_dicts):
    features_number = 0
    for cat_feature_dict in cat_features_dicts.values():
        assert len(cat_feature_dict) > 1, \
            'Categorical feature dictionary for socdem training can not be empty (have size 1)'
        features_number += len(cat_feature_dict)
    return features_number


def check_features_by_socdem_type_equality(training_result):
    # features need to be equal for all socdem types
    assert np.allclose(training_result['gender'], training_result['age']), \
        'Features are not equal for different socdem types'
    assert np.allclose(training_result['gender'], training_result['income']), \
        'Features are not equal for different socdem types'

    return training_result['gender']


def get_nn_models_list():
    nn_models = [
        models_utils.convert_simple_keras_to_numpy(
            models_utils.get_custom_neuro_model(
                n_classes=len(socdem_config.segment_names_by_label_type[socdem_segment_type]),
                neuro_offset=socdem_config.VECTOR_SIZE,
                random_seed=0,
            )
        ) for socdem_segment_type in socdem_config.SOCDEM_SEGMENT_TYPES
    ]
    return nn_models


def get_predictions_table_schema(socdem_type):
    schema = {
        'Label': 'string',
        'Weight': 'double',
    }
    socdem_segment = socdem_config.socdem_type_to_segment_name[socdem_type]
    for label in range(len(socdem_config.segment_names_by_label_type[socdem_segment])):
        schema['Probability:Class={}'.format(label)] = 'double'

    return schema


def get_schema_for_fields(fields):
    schema = {}
    for field in fields:
        schema[field] = fields_types[field]
    return schema
