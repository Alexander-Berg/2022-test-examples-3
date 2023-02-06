import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.services.training.lib import segments_dssm_features


def test_get_segment_dssm_features(patched_yt_client, segment_stats_table):
    output_tables = [
        tables.YsonTable(
            'segments_dssm_features.yson',
            config.SEGMENTS_DSSM_FEATURES_TABLE,
            yson_format='pretty',
        ),
    ]
    return tests.yt_test_func(
        yt_client=patched_yt_client,
        func=lambda: segments_dssm_features.get(nv_params=None, output='./result.json'),
        data_path=yatest.common.test_source_path('data/segments_dssm_features'),
        return_result=False,
        input_tables=[
            (
                segment_stats_table('segments_stats.yson', config.SEGMENTS_USER_DATA_STATS_TABLE),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'features_mapping.yson',
                    config.LAL_FEATURES_MAPPING_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.features_mapping_schema),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[(table, tests.Diff()) for table in output_tables],
    )
