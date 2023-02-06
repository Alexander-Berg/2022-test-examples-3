import os

import yatest.common

from crypta.lib.python.prism_quality import check_quality
import crypta.lib.python.yql.client as yql_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.utils.config import config


def get_clusters_table_schema():
    return schema_utils.yt_schema_from_dict({
        'cluster': 'uint64',
        'prism_segment': 'string',
        'yandexuid': 'string',
    })


def get_cost_table_schema():
    return schema_utils.yt_schema_from_dict({
        'cost': 'int64',
        'os': 'string',
        'yandexuid': 'uint64',
    })


def get_visits_table_schema():
    return schema_utils.yt_schema_from_dict({
        'google_visits': 'uint64',
        'yandex_visits': 'uint64',
        'os': 'string',
        'yandexuid': 'uint64',
    })


def test_check_quality(clean_local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    yt_client = clean_local_yt.get_yt_client()
    yql_client = yql_helpers.create_yql_client(
        yt_proxy=clean_local_yt.get_server(),
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )

    working_dir = '//tmp'
    date = '2022-01-01'
    input_path = os.path.join(working_dir, 'clusters')
    output_tables = [
        tables.YsonTable(
            '{}.yson'.format(metric),
            os.path.join(working_dir, metric),
            yson_format='pretty',
            on_read=tables.OnRead(row_transformer=tests.float_to_str),
        )
        for metric in ('gmv', 'adv', 'share')
    ]

    return tests.float_to_str(tests.yt_test_func(
        yt_client=yt_client,
        func=lambda: check_quality(
            yt_client=yt_client,
            yql_client=yql_client,
            tables_to_check=[input_path],
            dates=[date],
            output_dirs=[working_dir],
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=True,
        input_tables=[
            (
                tables.get_yson_table_with_schema('clusters.yson', input_path, get_clusters_table_schema()),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'chevent_log.yson',
                    os.path.join(config.PRISM_CHEVENT_LOG_DIRECTORY, date),
                    get_cost_table_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'prism_gmv.yson',
                    os.path.join(config.PRISM_GMV_DIRECTORY, date),
                    get_cost_table_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'visits.yson',
                    os.path.join(config.PRISM_YANDEX_GOOGLE_VISITS_DIRECTORY, date),
                    get_visits_table_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[(table, tests.Diff()) for table in output_tables],
    )[0])
