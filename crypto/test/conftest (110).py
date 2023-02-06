import os
import pytest

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import tables
from crypta.profile.lib.socdem_helpers.socdem import weighted_socdem_sample_schema
from crypta.profile.lib.socdem_helpers.tools.features import cat_feature_types
from crypta.profile.lib.socdem_helpers.test_utils import row_transformer_to_add_vector


@pytest.fixture()
def socdem_train_sample():
    on_write = tables.OnWrite(
        attributes={
            'schema': schema_utils.yt_schema_from_dict(weighted_socdem_sample_schema),
        },
        row_transformer=row_transformer_to_add_vector,
    )
    return tables.YsonTable('socdem_train_sample.yson',
                            '//home/crypta/socdem/train_sample',
                            on_write=on_write)


@pytest.fixture()
def cat_features():
    tables_directory = '//home/crypta/categorical_features'
    tables_list = []
    schema = {'feature_index': 'uint64'}
    for cat_feature_type in cat_feature_types:
        if cat_feature_type in ('heuristic_common', 'longterm_interests'):
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
def neuro_raw_yandexuid_profiles():
    return tables.get_yson_table_with_schema(
        "neuro_raw_yandexuid_profiles.yson",
        "//home/crypta/socdem/neuro_raw_yandexuid_profiles",
        schema_utils.yt_schema_from_dict({
            'yandexuid': 'uint64',
            'gender': 'any',
            'user_age_6s': 'any',
            'income_5_segments': 'any',
        }, sort_by=['yandexuid'])
    )


@pytest.fixture()
def merged_segments():
    return tables.get_yson_table_with_schema(
        "segments_storage_by_yandexuid.yson",
        "//home/crypta/socdem/segments_storage_by_yandexuid",
        schema_utils.yt_schema_from_dict({
            'yandexuid': 'uint64',
            'heuristic_common': 'any',
            'heuristic_segments': 'any',
            'longterm_interests': 'any',
        }, sort_by=['yandexuid'])
    )


@pytest.fixture()
def merged_hits_by_yandexuid():
    return tables.get_yson_table_with_schema(
        "yandexuid_merged_hits.yson",
        "//home/crypta/socdem/yandexuid_merged_hits",
        schema_utils.yt_schema_from_dict({
            'yandexuid': 'uint64',
            'raw_site_weights': 'any',
        }, sort_by=['yandexuid'])
    )


@pytest.fixture(scope="function")
def sample_for_catboost_classification():
    return tables.YsonTable('sample_for_catboost_classification.yson',
                            '//home/crypta/socdem/sample_for_catboost_classification',
                            yson_format='pretty')
