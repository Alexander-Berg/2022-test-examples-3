import functools
import os

import yatest.common

from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.offline_weighting.lib import (
    cluster_stats,
    table_paths,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_cluster_stats_calculate(custom_output_dir, yt_client, yql_client, date):
    resolved_table_paths = table_paths.resolve(custom_output_dir, date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            cluster_stats.calculate_by_date,
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
                    file_path='statbox_product_money.yson',
                    cypress_path=os.path.join(config.STATBOX_PRODUCT_MONEY_DIR, date),
                    schema=schemas.statbox_event_money_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='user_sessions_pub.yson',
                    cypress_path=config.USER_SESSIONS_NANO_BY_DATE_TABLE.format(date),
                    schema=[
                        {'name': 'key', 'type': 'string'},
                    ],
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='clusters.yson',
                    cypress_path=resolved_table_paths['clusters'],
                    schema=schemas.clusters_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='cluster_stats_output.yson',
                    cypress_path=resolved_table_paths['cluster_stats'],
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
