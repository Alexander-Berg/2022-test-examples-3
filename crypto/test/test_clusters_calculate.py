import functools
import os

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.offline_weighting.lib import (
    clusters,
    table_paths,
)


def test_clusters_calculate(custom_output_dir, yt_client, yql_client, date):
    resolved_table_paths = table_paths.resolve(custom_output_dir, date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            clusters.calculate_by_date,
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
                    file_path='yandexuid_profile_export.yson',
                    cypress_path=os.path.join(config.YANDEXUID_PROFILES_EXPORT_DIR, date),
                    schema=schemas.yandexuid_profile_export_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='lookalike.yson',
                    cypress_path=resolved_table_paths['lookalike'],
                    schema=schemas.lookalike_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='antifraud_daily.yson',
                    cypress_path=config.ATNIFRAUD_EXPORT_BY_DATE_TABLE.format(date),
                    schema=schemas.antifraud_daily_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='yuid_with_all_info.yson',
                    cypress_path=config.YUID_WITH_ALL_INFO_TABLE,
                    schema=schemas.yuid_with_all_info_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='yandexuid_crypta_id.yson',
                    cypress_path=config.YANDEXUID_CRYPTAID_MATCHING_TABLE,
                    schema=schemas.matching_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'clusters_output.yson',
                    resolved_table_paths['clusters'],
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    'frauds_output.yson',
                    resolved_table_paths['frauds'],
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
