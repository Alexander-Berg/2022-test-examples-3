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
    quality_metrics,
    table_paths,
)
from crypta.profile.lib import date_helpers


def test_quality_metrics(yt_client, yql_client, date):
    previous_date = date_helpers.get_date_from_past(current_date=date, days=1)

    lookalike = [tables.get_yson_table_with_schema(
        file_path='lookalike.yson',
        cypress_path=os.path.join(config.PRISM_LAL_DIR, table_name),
        schema=schemas.lookalike_schema,
    ) for table_name in [previous_date, date]]

    yandexuid_profile_export = [tables.get_yson_table_with_schema(
        file_path='yandexuid_profile_export.yson',
        cypress_path=os.path.join(config.YANDEXUID_PROFILES_EXPORT_DIR, table_name),
        schema=schemas.yandexuid_profile_export_schema,
    ) for table_name in [previous_date, date]]

    user_weights = [tables.get_yson_table_with_schema(
        file_path='user_weights.yson',
        cypress_path=os.path.join(config.PRISM_OFFLINE_USER_WEIGHTS_DIR, table_name),
        schema=schemas.user_weights_schema,
    ) for table_name in [previous_date, date]]

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            quality_metrics.send_correlation_metrics,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in lookalike
        ] + [
            (table, tests.TableIsNotChanged()) for table in yandexuid_profile_export
        ] + [
            (table, tests.TableIsNotChanged()) for table in user_weights
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'day_to_day_correlation.yson',
                    config.PRISM_DAY_TO_DAY_CORRELATION_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )


def test_prepare_gmv_table(yt_client, yql_client, date):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            quality_metrics.prepare_gmv_table,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='market_sales.yson',
                    cypress_path=os.path.join(config.MARKET_SALES_NO_EXTERNAL_FOLDER, date),
                    schema=schemas.market_sales_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='market_takerate.yson',
                    cypress_path=config.MARKET_TAKERATE_TABLE,
                    schema=schemas.market_takerate_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'gmv.yson',
                    os.path.join(config.PRISM_GMV_DIRECTORY, date),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )


def test_prepare_adv_table(yt_client, yql_client, date):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            quality_metrics.prepare_adv_table,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='chevent_log.yson',
                    cypress_path=os.path.join(config.BS_CHEVENT_COOKED_LOG, date),
                    schema=schemas.bs_chevent_log_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'adv.yson',
                    os.path.join(config.PRISM_CHEVENT_LOG_DIRECTORY, date),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )


def test_prepare_yandex_google_visits_table(yt_client, yql_client, date):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            quality_metrics.prepare_yandex_google_visits_table,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='traffic.yson',
                    cypress_path=os.path.join(config.TRAFFIC_V3_DIR, date),
                    schema=schemas.traffic_v3_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'yandex_google_visits.yson',
                    os.path.join(config.PRISM_YANDEX_GOOGLE_VISITS_DIRECTORY, date),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )


def test_send_auc_metrics(yt_client, yql_client, date):
    resolved_table_paths = table_paths.resolve(None, date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            quality_metrics.send_auc_metrics,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='gmv.yson',
                    cypress_path=os.path.join(config.PRISM_GMV_DIRECTORY, date),
                    schema=schemas.gmv_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='adv.yson',
                    cypress_path=os.path.join(config.PRISM_CHEVENT_LOG_DIRECTORY, date),
                    schema=schemas.adv_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='yandex_google_visits.yson',
                    cypress_path=os.path.join(config.PRISM_YANDEX_GOOGLE_VISITS_DIRECTORY, date),
                    schema=schemas.yandex_google_visits_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='user_weights.yson',
                    cypress_path=resolved_table_paths['user_weights'],
                    schema=schemas.user_weights_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'prism_auc_metrics.yson',
                    config.DATALENS_PRISM_PRODUCTION_METRICS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
