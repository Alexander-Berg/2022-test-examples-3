import logging
import pytest

import yatest.common
import yt.wrapper as yt

from crypta.lab.lib.describe import PastDescribe
from crypta.lab.lib.test.test_utils import (
    Fields,
    sample_stats_schema,
    src_with_dates_schema,
    matching_schema,
)
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

logger = logging.getLogger(__name__)


@pytest.fixture(scope='function')
def get_past_describe_task(api_client_mock):
    def get_task(sample_id, view_id):
        task = PastDescribe(sample_id=sample_id, view_id=view_id)
        task.api = api_client_mock
        return task

    return get_task


def test_past_describe(clean_local_yt, config, get_past_describe_task, dated_user_data_table):
    sample_id = 'sample-for-describe'
    output_tables = [
        tables.DynamicYsonTable('sample_stats.yson', config.paths.lab.sample_stats, yson_format='pretty'),
        tables.YsonTable(
            'src.yson',
            yt.ypath_join('//home/crypta/testing/lab/samples', sample_id, 'src'),
            yson_format='pretty',
        ),
    ]

    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(get_past_describe_task(sample_id, 'src')),
        data_path=yatest.common.test_source_path('data/test_past_describe'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'src.yson',
                    yt.ypath_join('//home/crypta/testing/lab/samples', sample_id, 'src'),
                    schema=src_with_dates_schema(),
                ),
                tests.Exists(),
            ),
            (dated_user_data_table(file_path='user_data_2021-07-15.yson', id_type=Fields.CRYPTA_ID, date='2021-07-15'), tests.TableIsNotChanged()),
            (dated_user_data_table(file_path='user_data_2021-07-22.yson', id_type=Fields.CRYPTA_ID, date='2021-07-22'), tests.TableIsNotChanged()),
            (dated_user_data_table(file_path='user_data_2021-07-29.yson', id_type=Fields.CRYPTA_ID, date='2021-07-29'), tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema(
                    'yuid_to_crypta_id.yson',
                    yt.ypath_join(config.paths.matching.root, 'yandexuid', 'crypta_id'),
                    schema=matching_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'sample_stats.yson',
                    config.paths.lab.sample_stats,
                    sample_stats_schema(),
                    dynamic=True,
                ),
                None,
            )],
        output_tables=[(output_table, tests.Diff()) for output_table in output_tables],
    )
