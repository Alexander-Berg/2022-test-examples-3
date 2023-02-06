import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.offline_weighting.lib import (
    table_paths,
    user_weights,
)


def test_user_weights_calculate(custom_output_dir, yt_client, yql_client, date):
    resolved_table_paths = table_paths.resolve(custom_output_dir, date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            user_weights.calculate_by_date,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
            custom_output_dir=custom_output_dir,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='clusters.yson',
                    cypress_path=resolved_table_paths['clusters'],
                    schema=schemas.clusters_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='cluster_weights.yson',
                    cypress_path=resolved_table_paths['cluster_stats'],
                    schema=schemas.cluster_stats_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='user_sessions_pub.yson',
                    cypress_path=config.USER_SESSIONS_NANO_BY_DATE_TABLE.format(date),
                    schema=schemas.user_sessions_nano_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='longterm_norm_segment_weights.yson',
                    cypress_path=config.LONGTERM_NORM_SEGMENT_WEIGHTS_TABLE,
                    schema=schemas.longterm_norm_segment_weights_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='user_weights_output.yson',
                    cypress_path=resolved_table_paths['user_weights'],
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
