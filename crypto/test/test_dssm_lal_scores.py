import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.services.training.lib import dssm_lal_scores


def test_dssm_lal_scores(patched_yt_client):

    output_tables = [
        tables.YsonTable(
            'dssm_lal_scores.yson',
            config.TEST_DSSM_LAL_DISTANCES,
            yson_format='pretty',
        ),
    ]
    return tests.yt_test_func(
        yt_client=patched_yt_client,
        func=lambda: dssm_lal_scores.calculate(nv_params=None),
        data_path=yatest.common.test_source_path('data/dssm_lal_scores'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'users_dssm_vectors.yson',
                    config.TEST_USERS_DSSM_VECTORS,
                    schemas.users_vectors_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'segments_dssm_vectors.yson',
                    config.TEST_SEGMENTS_DSSM_VECTORS,
                    schemas.segment_vectors_schema,
                ),
                tests.TableIsNotChanged(),
            )
        ],
        output_tables=[(table, tests.Diff()) for table in output_tables],
    )
