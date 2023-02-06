import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.services.training.lib import negatives


def test_make_negatives(patched_yt_client):
    output_tables = [
        tables.YsonTable(
            'negatives.yson',
            config.NEGATIVES_TABLE,
            yson_format='pretty',
        ),
    ]
    return tests.yt_test_func(
        yt_client=patched_yt_client,
        func=lambda: negatives.make(nv_params=None),
        data_path=yatest.common.test_source_path('data/negatives'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'segments_with_counts.yson',
                    config.TRAIN_SEGMENTS_WITH_COUNTS_TABLE,
                    schemas.segments_counts_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'users_ranked.yson',
                    config.RANKED_USER_DATA_YANDEXUIDS_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.yuids_ranked),
                ),
                tests.TableIsNotChanged(),
            )
        ],
        output_tables=[(table, tests.Diff()) for table in output_tables],
    )
