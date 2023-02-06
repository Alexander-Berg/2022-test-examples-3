try:
    import cPickle as pickle
except:
    import pickle
import json
import os

import flask
import mock
import numpy as np
import pytest
import yatest.common

from crypta.lib.python.test_utils import flask_mock_server
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.socdem_helpers import socdem_config
from crypta.profile.lib.socdem_helpers.simple_nn import SimpleNN
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.runners.export_profiles.lib.profiles_generation.get_basic_profiles import GetBasicProfiles
from crypta.profile.runners.export_profiles.lib.profiles_generation.get_devid_profiles import GetBasicDevidProfiles

resource_to_folder = {
    GetBasicProfiles.resource_type: 'socdem_data',
    GetBasicDevidProfiles.resource_type: 'mobile_socdem_data',
}

resources_path = 'crypta/profile/runners/export_profiles/lib/profiles_generation/test_socdem/sandbox_data'


def mock_sandbox_server_with_resource(resource_type, released):
    class MockSandboxServer(flask_mock_server.FlaskMockServer):
        def __init__(self):
            super(MockSandboxServer, self).__init__("Sandbox")
            self.commands = []

            @self.app.route("/last/{}/<file_name>".format(resource_type))
            def get_resource(file_name):
                return flask.send_file(yatest.common.build_path(os.path.join(resources_path, resource_to_folder[resource_type], file_name)))

        def get_resource_url(self, file_name):
            return 'http://localhost:{port}/last/{resource_type}/{file_name}?attrs={{"released":"{released}"}}'.format(
                port=self.port,
                resource_type=resource_type,
                file_name=file_name,
                released=released,
            )

    return MockSandboxServer()


def get_daily_vectors_table(input_file, output_table, is_mobile=False):
    def row_transformer_to_add_vector(row):
        row['vector'] = np.ones(socdem_config.VECTOR_SIZE, dtype=np.float32).tostring()
        return row

    schema = {'vector': 'string'}
    if is_mobile:
        schema.update({
            'id': 'string',
            'id_type': 'string',
        })
    else:
        schema.update({'yandexuid': 'uint64'})
    on_write = tables.OnWrite(
        attributes={'schema': schema_utils.yt_schema_from_dict(schema)},
        row_transformer=row_transformer_to_add_vector,
    )
    return tables.YsonTable(input_file, output_table, on_write=on_write)


@pytest.fixture()
def mock_get_simple_nn_model():
    def get_simple_nn_model(**args):
        with open(yatest.common.build_path(os.path.join(resources_path, resource_to_folder[args['resource']], args['model_path'])),
                  'rb') as model_file_to_read:
            nn = pickle.load(model_file_to_read)
            assert isinstance(nn, SimpleNN)

            for layer_idx in range(len(nn.layers)):
                nn.layers[layer_idx] = ((
                    (nn.layers[layer_idx][0][0].astype(np.float16), nn.layers[layer_idx][0][1].astype(np.float16)),
                    nn.layers[layer_idx][1],
                ))

            return nn

    return mock.patch('crypta.profile.lib.socdem_helpers.inference_utils.inference.get_simple_nn_model', get_simple_nn_model)


@pytest.fixture()
def mock_get_features_dict():
    def get_get_features_dict(**args):
        with open(yatest.common.build_path(os.path.join(resources_path, resource_to_folder[args['resource_type']], args['file_name'])), 'r') as dict_file:
            return json.load(dict_file)

    return mock.patch('crypta.profile.lib.socdem_helpers.inference_utils.inference.get_features_dict', get_get_features_dict)


def mock_sandbox_resource(is_mobile=False):
    return mock_sandbox_server_with_resource(GetBasicDevidProfiles.resource_type if is_mobile else GetBasicProfiles.resource_type, 'testing')


def mock_get_catboost_models_file_paths(mock_sandbox_resource):
    def get_catboost_models_file_paths(**args):
        return mock_sandbox_resource.get_resource_url('{}_catboost_model.bin'.format(args['socdem_type']))

    return mock.patch('crypta.profile.lib.socdem_helpers.inference_utils.inference.get_catboost_models_file_paths',
                      get_catboost_models_file_paths)


def test_get_basic_profiles_task(
    local_yt,
    patched_config,
    date,
    mock_get_simple_nn_model,
    mock_get_features_dict,
):
    patched_config.environment = 'testing'

    with mock.patch('crypta.profile.tasks.features.calculate_id_vectors.GetDailyYandexuidVectors.complete', return_value=True), \
            mock.patch('crypta.profile.tasks.features.merge_hits_by_id.MergeHitsByYandexuid.complete', return_value=True), \
            mock.patch('crypta.profile.runners.export_profiles.lib.profiles_generation.get_merged_segments.'
                       'CombineSegmentsByYandexuid.complete', return_value=True),\
            mock_get_simple_nn_model,\
            mock_get_features_dict, \
            mock_sandbox_resource(False) as mock_catboost_models, \
            mock_get_catboost_models_file_paths(mock_catboost_models):

        task = GetBasicProfiles(date=date)
        output_table_path = os.path.join(patched_config.RAW_YANDEXUID_PROFILES_YT_DIRECTORY, date)

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=local_yt,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (
                    get_daily_vectors_table(
                        input_file='yandexuid_daily_vectors.yson',
                        output_table=patched_config.DAILY_YANDEXUID2VEC,
                        is_mobile=False,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'segments_storage_by_yandexuid.yson',
                        patched_config.SEGMENTS_STORAGE_BY_YANDEXUID_TABLE,
                        schema=schema_utils.yt_schema_from_dict({
                            'yandexuid': 'uint64',
                            'heuristic_common': 'any',
                            'longterm_interests': 'any',
                        }, sort_by=['yandexuid']),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'merged_hits_by_yandexuid.yson',
                        patched_config.YANDEXUID_METRICS_MERGED_HITS_TABLE,
                        schema=schema_utils.yt_schema_from_dict({
                            'yandexuid': 'uint64',
                            'raw_site_weights': 'any',
                        }, sort_by=['yandexuid']),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'yandexuid_profiles_{}.yson'.format(date),
                        output_table_path,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
            dependencies_are_missing=False,
        )


def test_get_devid_profiles_task(
    local_yt,
    patched_config,
    date,
    mock_get_simple_nn_model,
    mock_get_features_dict,
):
    patched_config.environment = 'testing'

    with mock.patch('crypta.profile.tasks.features.get_app_metrica_data.GetDailyDevidVectors.complete', return_value=True), \
            mock.patch('crypta.profile.tasks.features.get_app_metrica_data.GetDailyAppByDevid.complete', return_value=True), \
            mock_get_simple_nn_model, \
            mock_get_features_dict, \
            mock_sandbox_resource(True) as mock_catboost_models, \
            mock_get_catboost_models_file_paths(mock_catboost_models):

        task = GetBasicDevidProfiles(date=date)
        output_table_path = os.path.join(patched_config.RAW_DEVID_PROFILES_YT_DIRECTORY, date)

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=local_yt,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (
                    get_daily_vectors_table(
                        input_file='devid_daily_vectors.yson',
                        output_table=patched_config.DAILY_DEVID2VEC,
                        is_mobile=True,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'app_by_devid_daily.yson',
                        patched_config.APP_BY_DEVID_DAILY_TABLE,
                        schema=schema_utils.yt_schema_from_dict({
                            'id': 'string',
                            'id_type': 'string',
                            'model': 'string',
                            'main_region_obl': 'uint64',
                            'categories': 'any',
                            'manufacturer': 'string',
                        }, sort_by=['id', 'id_type']),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'mobile_profiles_{}.yson'.format(date),
                        output_table_path,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
            dependencies_are_missing=False,
        )
