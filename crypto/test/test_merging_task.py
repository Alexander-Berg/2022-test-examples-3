import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lib.python.custom_ml.tools.utils import get_industry_dir_from_industry_name
from crypta.profile.services.merge_new_trainable_segments_sample.proto.config_pb2 import TConfig


def get_config_file(local_yt, add_to_training):
    config_file_path = yatest.common.test_output_path('config.yaml')
    templater.render_file(
        yatest.common.source_path('crypta/profile/services/merge_new_trainable_segments_sample/bundle/config.yaml'),
        config_file_path,
        {
            'yt_proxy': local_yt.get_server(),
            'new_sample_by_puid_path': '//home/crypta/training_results/sample_by_puid',
            'model_name': 'SomeModel',
            'partner': 'some_partner',
            'login': 'some_login',
            'add_to_training': add_to_training,
            'retrain_model': False,
        },
        strict=True,
    )
    return config_file_path


def get_input_tables(config_file):
    config = yaml_config.parse_config(TConfig, config_file)
    industry_yt_dir = get_industry_dir_from_industry_name(config.ModelName)

    common_schema = {
        'id': 'string',
        'id_type': 'string',
        'retro_date': 'string',
        'segment_name': 'string',
    }
    glued_schema = common_schema.copy()
    glued_schema.update({
        'partner': 'string',
        'use_for_training': 'boolean',
    })

    return {
        'glued': tables.get_yson_table_with_schema(
            'glued.yson',
            os.path.join(industry_yt_dir, 'glued'),
            schema_utils.yt_schema_from_dict(glued_schema),
        ),
        'sample_by_puid': tables.get_yson_table_with_schema(
            'sample_by_puid.yson',
            os.path.join(industry_yt_dir, 'sample_by_puid'),
            schema_utils.yt_schema_from_dict(common_schema),
        ),
        'new_sample_by_puid': tables.get_yson_table_with_schema(
            'new_sample_by_puid.yson',
            config.NewSampleByPuidPath,
            schema_utils.yt_schema_from_dict(common_schema),
        ),
    }


def remove_existing_tables(yt_client, tables_to_delete):
    for table in tables_to_delete:
        if yt_client.exists(table.cypress_path):
            yt_client.remove(table.cypress_path, force=True)


@pytest.mark.parametrize('add_to_training', [False, True], ids=['only_to_glued', 'to_training'])
def test_adding_new_sample(local_yt, local_yt_and_yql_env, add_to_training):
    config_file = get_config_file(
        local_yt=local_yt,
        add_to_training=add_to_training,
    )
    yt_client = local_yt.get_yt_client()
    input_tables = get_input_tables(config_file)
    remove_existing_tables(yt_client, input_tables.values())

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path('crypta/profile/services/merge_new_trainable_segments_sample/bin/crypta-merge-sample'),
        args=[
            '--config', config_file,
        ],
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (input_tables['new_sample_by_puid'], [tests.TableIsNotChanged()]),
            (input_tables['glued'], [tests.Exists()]),
            (input_tables['sample_by_puid'], [tests.Exists()]),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'edited_glued.yson',
                    input_tables['glued'].cypress_path,
                    yson_format='pretty',
                ),
                [tests.Diff()],
            ),
            (
                tables.YsonTable(
                    'edited_sample_by_puid.yson',
                    input_tables['sample_by_puid'].cypress_path,
                    yson_format='pretty',
                ),
                [tests.Diff()],
            ),
        ],
        env=local_yt_and_yql_env,
    )
