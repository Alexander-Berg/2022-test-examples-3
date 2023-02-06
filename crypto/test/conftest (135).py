import os

import numpy as np
import pytest
import yatest.common

from crypta.lib.proto.user_data.user_data_pb2 import TUserData
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.custom_ml import training_config
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
)
from crypta.lib.python.yt.test_helpers import row_transformers
from crypta.profile.services.train_custom_model.proto.config_pb2 import TConfig


pytest_plugins = [
    'crypta.lib.python.test_utils.fixtures',
    'crypta.lib.python.yql.test_helpers.fixtures',
]


@pytest.fixture()
def config_file(local_yt, mock_sandbox_server_with_identifiers_udf):
    output_directory = '//home/crypta/custom_training'
    config_file_path = yatest.common.test_output_path('config.yaml')
    templater.render_file(
        yatest.common.source_path('crypta/profile/services/train_custom_model/bundle/config.yaml'),
        config_file_path,
        {
            'yt_proxy': local_yt.get_server(),
            'output_dir_path': output_directory,
            'sample_table_path': os.path.join(output_directory, 'raw_sample'),
            'sample_file_path': None,
            'audience_id': None,
            'validate_segments': True,
            'model_name': None,
            'positive_output_segment_size': 3,
            'negative_output_segment_size': 3,
            'crypta_identifier_udf_url': mock_sandbox_server_with_identifiers_udf.get_udf_url(),
            'send_results_to_api': False,
            'environment': 'testing',
            'logins_to_share': 'a, b',
        },
        strict=True,
    )
    return config_file_path


@pytest.fixture()
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)


@pytest.fixture()
def cat_features_tables():
    tables_list = []
    for cat_feature_type in ('heuristic_common', 'longterm_interests'):
        schema = {
            'feature_index': 'uint64',
            'feature': 'uint64',
        }
        tables_list.append(tables.get_yson_table_with_schema(
            '{}.yson'.format(cat_feature_type),
            os.path.join(training_config.CATEGORICAL_FEATURES_CUSTOM_ML_MATCHING_DIR, cat_feature_type),
            schema_utils.yt_schema_from_dict(schema),
        ))
    return tables_list


@pytest.fixture()
def raw_sample_table(config):
    return tables.get_yson_table_with_schema(
        'raw_sample.yson',
        os.path.join(config.OutputDirPath, 'raw_sample'),
        schema_utils.yt_schema_from_dict({
            'id': 'string',
            'id_type': 'string',
            'retro_date': 'string',
            'segment_name': 'string',
        })
    )


@pytest.fixture()
def puid_matching_table():
    return tables.get_yson_table_with_schema(
        "puid_matching_table.yson",
        training_config.MATCHING_TABLE_TEMPLATE.format('phone_md5'),
        schema_utils.yt_schema_from_dict({
            'id': 'string',
            'id_type': 'string',
            'target_id': 'string',
        })
    )


@pytest.fixture()
def indevice_yandexuid_table():
    return tables.get_yson_table_with_schema(
        "indevice_yandexuid.yson",
        training_config.INDEVICE_YANDEXUID,
        schema_utils.yt_schema_from_dict({
            'id': 'string',
            'id_type': 'string',
            'yandexuid': 'uint64',
        })
    )


@pytest.fixture()
def segments_info_table():
    return tables.get_yson_table_with_schema(
        "segments_info.yson",
        training_config.LAB_SEGMENTS_INFO_TABLE,
        schema_utils.yt_schema_from_dict({
            'name': 'string',
            'exportKeywordId': 'int64',
            'exportSegmentId': 'int64',
        })
    )


def fill_user_data_row_transformer():
    np.random.seed(0)
    proto_dict_to_yson = row_transformers.proto_dict_to_yson(TUserData)

    def row_transformer(row):
        row['Vectors'] = {'Vector': {'Data': list(np.random.rand(training_config.VECTOR_SIZE))}}
        return proto_dict_to_yson(row)

    return row_transformer


@pytest.fixture()
def yandexuid_user_data_table():
    on_write = tables.OnWrite(attributes={'schema': schema_utils.get_schema_from_proto(TUserData, ['yuid'])},
                              row_transformer=fill_user_data_row_transformer())
    return tables.YsonTable('user_data_yandexuid.yson', training_config.USER_DATA_TABLE, on_write=on_write)


def add_vector_to_features_row_transformer():
    np.random.seed(0)

    def row_transformer(row):
        row['FloatFeatures'] = list(np.random.rand(training_config.VECTOR_SIZE)) + row['FloatFeatures']
        return row

    return row_transformer


@pytest.fixture()
def catboost_features_table():
    on_write = tables.OnWrite(
        attributes={
            'schema': schema_utils.yt_schema_from_dict({
                'PassThrough': 'string',
                'FloatFeatures': 'any',
                'CatFeatures': 'any',
            }),
        },
        row_transformer=add_vector_to_features_row_transformer(),
    )
    return tables.YsonTable('catboost_features_table.yson', training_config.CATBOOST_FEATURES, on_write=on_write)


@pytest.fixture()
def yandexuid_crypta_id_table():
    return tables.get_yson_table_with_schema(
        'yandexuid_crypta_id_table.yson',
        training_config.YANDEXUID_CRYPTAID_TABLE,
        schema_utils.yt_schema_from_dict({
            'yandexuid': 'uint64',
            'crypta_id': 'uint64',
        }),
    )


@pytest.fixture()
def metrics_file(config):
    file_name = 'metrics'
    return files.YtFile(file_name, os.path.join(config.OutputDirPath, file_name))


@pytest.fixture()
def resulting_segments_table(config):
    return tables.YsonTable(
        'resulting_segments.yson',
        os.path.join(config.OutputDirPath, 'resulting_segments'),
        yson_format='pretty',
    )
