import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.priors.lib import features_and_cluster_by_user


def test_features_and_cluster_by_user_calculate(yt_client, yql_client, date):
    prism_weights = tables.get_yson_table_with_schema(
        file_path='user_weights.yson',
        cypress_path='//inputs/user_weights',
        schema=schemas.user_weights_schema,
    )

    features_and_cluster_by_user_output = tables.YsonTable(
        file_path='features_and_cluster_by_user.yson',
        cypress_path='//outputs/features_and_cluster_by_user',
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            features_and_cluster_by_user.calculate,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
            prism_weights=prism_weights.cypress_path,
            output_table=features_and_cluster_by_user_output.cypress_path,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='user_sessions_clean.yson',
                    cypress_path=config.USER_SESSIONS_CLEAN_BY_DATE_TABLE.format(date),
                    schema=schemas.user_sessions_full_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='user_sessions_frauds.yson',
                    cypress_path=config.USER_SESSIONS_FRAUDS_BY_DATE_TABLE.format(date),
                    schema=schemas.user_sessions_full_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='user_sessions_staff.yson',
                    cypress_path=config.USER_SESSIONS_STAFF_BY_DATE_TABLE.format(date),
                    schema=schemas.user_sessions_full_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (prism_weights, tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema(
                    file_path='yandexuid_icookie_matching.yson',
                    cypress_path=config.YANDEXUID_ICOOKIE_MATCHING_TABLE,
                    schema=schemas.matching_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (features_and_cluster_by_user_output, tests.Diff()),
        ],
    )
