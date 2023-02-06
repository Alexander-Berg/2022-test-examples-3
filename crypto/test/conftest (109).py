import os
import pytest

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import tables
from crypta.profile.lib.socdem_helpers.mobile_socdem import weighted_mobile_training_sample_schema
from crypta.profile.lib.socdem_helpers.tools.features import cat_feature_mobile_types
from crypta.profile.lib.socdem_helpers.test_utils import row_transformer_to_add_vector


@pytest.fixture()
def mobile_socdem_train_sample():
    on_write = tables.OnWrite(
        attributes={
            'schema': schema_utils.yt_schema_from_dict(weighted_mobile_training_sample_schema),
        },
        row_transformer=row_transformer_to_add_vector,
    )
    return tables.YsonTable('mobile_socdem_train_sample.yson',
                            '//home/crypta/mobile_socdem/train_sample',
                            on_write=on_write)


@pytest.fixture()
def mobile_cat_features():
    tables_directory = '//home/crypta/categorical_features'
    tables_list = []
    schema = {'feature_index': 'uint64'}
    for cat_feature_type in cat_feature_mobile_types:
        if cat_feature_type == 'main_region_obl':
            schema['feature'] = 'uint64'
        else:
            schema['feature'] = 'string'
        tables_list.append(tables.get_yson_table_with_schema(
            '{}.yson'.format(cat_feature_type),
            os.path.join(tables_directory, cat_feature_type),
            schema_utils.yt_schema_from_dict(schema),
        ))
    return {
        'tables': tables_list,
        'directory': tables_directory,
    }


@pytest.fixture()
def neuro_raw_devid_profiles():
    return tables.get_yson_table_with_schema(
        "neuro_raw_devid_profiles.yson",
        "//home/crypta/mobile_socdem/neuro_raw_devid_profiles",
        schema_utils.yt_schema_from_dict({
            'id': 'string',
            'id_type': 'string',
            'gender': 'any',
            'user_age_6s': 'any',
            'income_5_segments': 'any',
        }, sort_by=['id', 'id_type'])
    )


@pytest.fixture()
def app_by_devid_daily():
    return tables.get_yson_table_with_schema(
        "app_by_devid_daily.yson",
        "//home/crypta/mobile_socdem/app_by_devid_daily",
        schema_utils.yt_schema_from_dict({
            'id': 'string',
            'id_type': 'string',
            'model': 'string',
            'main_region_obl': 'uint64',
            'categories': 'any',
            'manufacturer': 'string',
        }, sort_by=['id', 'id_type'])
    )


@pytest.fixture()
def sample_for_catboost_classification():
    return tables.YsonTable('sample_for_catboost_classification.yson',
                            '//home/crypta/mobile_socdem/sample_for_catboost_classification',
                            yson_format='pretty')
