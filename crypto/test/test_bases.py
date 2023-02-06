import functools
import os

import mock
import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import bases
from crypta.affinitive_geo.services.org_embeddings.lib.utils import (
    config,
    test_utils,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers


def get_org_affinitive_banners_schema():
    return test_utils.get_orgs_info_schema() + [
        {'name': 'top_banners', 'type_v3': {'type_name': 'dict', 'key': 'string', 'value': 'string'}}
    ]


def get_orgs_weights_schema():
    return [
        {'name': 'permalink', 'type': 'uint64'},
        {'name': 'weight', 'type': 'int64'},
        {'name': 'region_id', 'type': 'int32'},
        {'name': 'geo_id', 'type': 'int32'},
    ]


def get_geohash_permalink_schema():
    return [
        {'name': 'geohash', 'type': 'string'},
        {'name': 'permalink', 'type': 'uint64'},
    ]


def get_org_embedding_base_schema():
    return [
        {'name': 'permalink', 'type': 'uint64'},
        {'name': 'embedding', 'type_v3': {'type_name': 'list', 'item': 'double'}},
    ]


def test_bases(yt_client, yql_client, date):
    prev_date = date_helpers.get_yesterday(date)

    with mock.patch.object(config, 'TOP_BANNERS_FOR_DASH_CNT', 2):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=functools.partial(
                bases.make,
                yt_client=yt_client,
                yql_client=yql_client,
                date=date,
            ),
            data_path=yatest.common.test_source_path('data/test_bases'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        file_path='org_affinitive_banners.yson',
                        cypress_path=os.path.join(config.ORG_AFFINITIVE_BANNERS_DIR, date),
                        schema=get_org_affinitive_banners_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        file_path='orgs_weights.yson',
                        cypress_path=os.path.join(config.ORGS_WEIGHTS_DIR, date),
                        schema=get_orgs_weights_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        file_path='caesar_info.yson',
                        cypress_path=os.path.join(config.CAESAR_INFO_DIR, date),
                        schema=test_utils.get_caesar_info_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        file_path='geohash_permalink.yson',
                        cypress_path=os.path.join(config.GEOHASH_PERMALINK_DIR, date),
                        schema=get_geohash_permalink_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        file_path='org_embedding_base_{}.yson'.format(prev_date),
                        cypress_path=os.path.join(config.ORG_EMBEDDING_BASE_DAILY_DIR, prev_date),
                        schema=get_org_embedding_base_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        file_path='org_embedding_base.yson',
                        cypress_path=os.path.join(config.ORG_EMBEDDING_BASE_DAILY_DIR, date),
                        yson_format='pretty',
                        on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                        on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        file_path='org_embedding_base_{}.yson'.format(date),
                        cypress_path=os.path.join(config.ORG_EMBEDDING_BASE_DAILY_DIR, date),
                        yson_format='pretty',
                        on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                        on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        file_path='geohash_org_base.yson',
                        cypress_path=config.GEOHASH_ORG_BASE_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        file_path='org_avg_distances.yson',
                        cypress_path=config.ORG_AVG_DISTANCES_TABLE,
                        yson_format='pretty',
                        on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                        on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                    ),
                    tests.Diff(),
                ),
                (
                    tables.YsonTable(
                        file_path='yandex_top_banners.yson',
                        cypress_path=config.YANDEX_TOP_BANNERS_TABLE,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
        )
