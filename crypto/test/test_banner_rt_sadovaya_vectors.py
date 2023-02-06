import functools
import os

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import banner_rt_sadovaya_vectors
from crypta.affinitive_geo.services.org_embeddings.lib.utils import (
    config,
    test_utils,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers


def get_caesar_latest_dump_schema():
    return [
        {'name': 'BannerID', 'type': 'uint64'},
        {'name': 'Resources', 'type': 'string'},
        {'name': 'TsarVectors', 'type': 'string'},
    ]


def get_active_banners_schema():
    return [
        {'name': 'bannerid', 'type': 'int64'},
    ]


def test_banner_rt_sadovaya_vectors(yt_client, yql_client, date, mock_bigb_udf):
    prev_date = date_helpers.get_yesterday(date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            banner_rt_sadovaya_vectors.get,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data/test_banner_rt_sadovaya_vectors'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='caesar_latest_dump.yson',
                    cypress_path=config.CAESAR_LATEST_DUMP,
                    schema=get_caesar_latest_dump_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='active_banners.yson',
                    cypress_path=config.ACTIVE_BANNERS_TABLE,
                    schema=get_active_banners_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='caesar_info_{}.yson'.format(prev_date),
                    cypress_path=os.path.join(config.CAESAR_INFO_DIR, prev_date),
                    schema=test_utils.get_caesar_info_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='caesar_info_{}.yson'.format(date),
                    cypress_path=os.path.join(config.CAESAR_INFO_DIR, date),
                    yson_format='pretty',
                    on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                    on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='banner_avg_distances.yson',
                    cypress_path=config.BANNER_AVG_DISTANCES_TABLE,
                    yson_format='pretty',
                    on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                    on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                ),
                tests.Diff(),
            ),
        ],
    )
