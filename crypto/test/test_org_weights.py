import functools
import os

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import org_weights
from crypta.affinitive_geo.services.org_embeddings.lib.utils import (
    config,
    test_utils,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers


def get_dssm_vectors_schema():
    return [
        {'name': 'GroupID', 'type': 'string'},
        {'name': 'segment_vector', 'type_v3': {'type_name': 'list', 'item': 'double'}},
    ]


def get_orgs_weights_schema():
    return [
        {'name': 'region_name', 'type': 'string'},
        {'name': 'category', 'type': 'string'},
        {'name': 'lat', 'type': 'double'},
        {'name': 'lon', 'type': 'double'},
        {'name': 'permalink', 'type': 'uint64'},
        {'name': 'region_id', 'type': 'int32'},
        {'name': 'geo_id', 'type': 'int32'},
        {'name': 'rank', 'type': 'uint64'},
        {'name': 'title', 'type': 'string'},
        {'name': 'weight', 'type': 'int64'},
    ]


def test_org_weights(yt_client, yql_client, date):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            org_weights.calculate,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data/test_org_weights'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='orgs_info.yson',
                    cypress_path=config.ORGS_INFO_TABLE,
                    schema=test_utils.get_orgs_info_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='orgs_dssm_vectors.yson',
                    cypress_path=config.ORGS_DSSM_VECTORS_TABLE,
                    schema=get_dssm_vectors_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='regions_dssm_vectors.yson',
                    cypress_path=config.REGIONS_DSSM_VECTORS_TABLE,
                    schema=get_dssm_vectors_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='orgs_weights_2022-05-25.yson',
                    cypress_path=os.path.join(config.ORGS_WEIGHTS_DIR, date_helpers.get_yesterday(date)),
                    schema=get_orgs_weights_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='orgs_weights_2022-05-26.yson',
                    cypress_path=os.path.join(config.ORGS_WEIGHTS_DIR, date),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='org_weight_rank_correlation.yson',
                    cypress_path=config.ORG_WEIGHT_RANK_CORRELATION_TABLE,
                    yson_format='pretty',
                    on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                    on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                ),
                tests.Diff(),
            ),
        ],
    )
