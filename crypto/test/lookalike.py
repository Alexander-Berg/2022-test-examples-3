import logging
import pytest

import yatest.common
import yt.wrapper as yt

from crypta.lab.lib.lookalike import Lookalike
from crypta.lab.lib.test.test_utils import (
    Fields,
    sample_stats_schema,
    strict_src_with_dates_schema,
    yuid_to_crypta_id_schema,
)
from crypta.lib.proto.user_data.user_data_stats_pb2 import TUserDataStats
from crypta.lib.python.bt import test_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

logger = logging.getLogger(__name__)


@pytest.fixture(scope='function')
def get_lookalike_task(api_client_mock):
    def get_task(sample_id, dst_view):
        task = Lookalike(sample_id=sample_id, src_view='src', dst_view=dst_view)
        task.api = api_client_mock
        return task

    return get_task


def test_lookalike_run(clean_local_yt, config, get_lookalike_task, dated_user_data_table, crypta_id_user_data_table, model_version):
    sample_id = 'sample'
    output_view = 'dst'
    output_table = tables.YsonTable(
        'dst.yson',
        yt.ypath_join("//home/crypta/testing/lab/samples", sample_id, output_view),
        yson_format='pretty',
        on_read=tables.OnRead(row_transformer=tests.float_to_str),
    )

    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(get_lookalike_task(sample_id, output_view)),
        data_path=yatest.common.test_source_path('data/test_lookalike_run'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'src.yson',
                    yt.ypath_join("//home/crypta/testing/lab/samples", sample_id, 'src'),
                    schema=strict_src_with_dates_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (dated_user_data_table(file_path='user_data.yson'), tests.TableIsNotChanged()),
            (
                crypta_id_user_data_table('crypta_id_user_data.yson', config.paths.user_data_stats_by_cryptaid),
                tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    'id_to_crypta_id.yson',
                    '//home/crypta/production/siberia/id_to_crypta_id',
                    on_write=tables.OnWrite(
                        sort_by=[Fields.ID_FIELD, Fields.ID_TYPE],
                        attributes={'schema': yuid_to_crypta_id_schema()},
                    ),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'user_data_stats.yson',
                    yt.ypath_join(config.paths.lab.data.userdata_stats),
                    schema=schema_utils.get_schema_from_proto(TUserDataStats),
                ),
                tests.TableIsNotChanged(),
            )] + model_version('2021-10-10'),
        output_tables=[(output_table, tests.Diff())],
    )


def test_lookalike_run_with_dates(clean_local_yt, config, get_lookalike_task, dated_user_data_table, model_version):
    sample_id = 'sample-with-dates'
    output_view = 'dst_with_dates'
    output_table = tables.YsonTable(
        'dst_with_dates.yson',
        yt.ypath_join("//home/crypta/testing/lab/samples", sample_id, output_view),
        yson_format='pretty',
        on_read=tables.OnRead(row_transformer=tests.float_to_str),
    )

    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=lambda: test_helpers.execute(get_lookalike_task(sample_id, output_view)),
        data_path=yatest.common.test_source_path('data/test_lookalike_run_with_dates'),
        input_tables=[
            (
                tables.YsonTable(
                    'src.yson',
                    yt.ypath_join('//home/crypta/testing/lab/samples', sample_id, 'src'),
                    on_write=tables.OnWrite(
                        attributes={
                            'schema': strict_src_with_dates_schema(),
                            'min_used_dates': '2021-07-22',
                        }
                    ),
                ),
                tests.TableIsNotChanged(),
            ),
            (dated_user_data_table(file_path='user_data.yson'), tests.TableIsNotChanged()),
            (
                tables.get_yson_table_with_schema(
                    'sample_stats.yson',
                    config.paths.lab.sample_stats,
                    sample_stats_schema(),
                    dynamic=True,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'user_data_stats.yson',
                    yt.ypath_join(config.paths.lab.data.crypta_id.userdata_stats),
                    schema=schema_utils.get_schema_from_proto(TUserDataStats),
                ),
                tests.TableIsNotChanged(),
            )] + model_version('2021-10-10', monthly=True),
        output_tables=[(output_table, tests.Diff())],
    )
