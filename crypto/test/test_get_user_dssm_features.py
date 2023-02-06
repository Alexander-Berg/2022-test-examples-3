import os

import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)

from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.services.training.lib import users_dssm_features


def test_get_user_dssm_features(patched_yt_client, user_data_table):

    output_tables = [
        tables.YsonTable(
            'user_data_dssm_features.yson',
            config.USER_DSSM_FEATURES_TABLE,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'features_mapping.yson',
            config.LAL_FEATURES_MAPPING_TABLE,
            yson_format='pretty',
        ),
    ]
    with mock.patch('crypta.lookalike.lib.python.utils.utils.calculate_features_dicts', return_value=None), \
            mock.patch('crypta.lookalike.lib.python.utils.config.config.USER_DATA_SAMPLING_RATE', 1.0):
        return tests.yt_test_func(
            yt_client=patched_yt_client,
            func=lambda: users_dssm_features.get(nv_params=None, output='./result.json'),
            data_path=yatest.common.test_source_path('data/user_dssm_features'),
            return_result=False,
            input_tables=[
                (user_data_table(filename='user_data.yson', path=config.USER_DATA_TABLE), tests.TableIsNotChanged()),
                (
                    tables.get_yson_table_with_schema(
                        'main_region_city.yson',
                        os.path.join(config.CATEGORICAL_FEATURES_MATCHING_DIR, 'main_region_city'),
                        schema_utils.yt_schema_from_dict(schemas.cities_dict_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'trainable_segments.yson',
                        os.path.join(config.CATEGORICAL_FEATURES_MATCHING_DIR, 'trainable_segments'),
                        schema_utils.yt_schema_from_dict(schemas.features_dicts_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'heuristic_common.yson',
                        os.path.join(config.CATEGORICAL_FEATURES_MATCHING_DIR, 'heuristic_common'),
                        schema_utils.yt_schema_from_dict(schemas.features_dicts_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'longterm_interests.yson',
                        os.path.join(config.CATEGORICAL_FEATURES_MATCHING_DIR, 'longterm_interests'),
                        schema_utils.yt_schema_from_dict(schemas.features_dicts_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    files.YtFile('segments_dict.json', config.SEGMENTS_DICT_FILE),
                    tests.Exists(),
                ),
            ],
            output_tables=[(table, tests.Diff()) for table in output_tables],
        )
