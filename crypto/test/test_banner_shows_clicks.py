import functools
import os

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import banner_shows_clicks
from crypta.affinitive_geo.services.org_embeddings.lib.utils import config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers


def get_chevent_schema():
    return [
        {'name': 'logid', 'type': 'int32'},
        {'name': 'bannerid', 'type': 'int32'},
        {'name': 'countertype', 'type': 'int32'},
        {'name': 'cryptaidv2', 'type': 'int32'},
        {'name': 'fraudbits', 'type': 'int32'},
        {'name': 'groupbannerid', 'type': 'int32'},
        {'name': 'placeid', 'type': 'int32'},
        {'name': 'regionid', 'type': 'int32'},
    ]


def get_banner_desc_schema():
    return [
        {'name': 'BannerID', 'type': 'int32'},
        {'name': 'Body', 'type': 'string'},
    ]


def test_banner_shows_clicks(yt_client, yql_client, date):
    first_date = date_helpers.get_date_from_past(date, days=config.DAYS_BACK_TO_START_GETTING_CHEVENT_INFO)
    last_date = date_helpers.get_yesterday(date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            banner_shows_clicks.get,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data/test_banner_shows_clicks'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='chevent_{}.yson'.format(first_date),
                    cypress_path=os.path.join(config.CHEVENT_LOG_DIR, first_date),
                    schema=get_chevent_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='chevent_{}.yson'.format(last_date),
                    cypress_path=os.path.join(config.CHEVENT_LOG_DIR, last_date),
                    schema=get_chevent_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='banner_desc.yson',
                    cypress_path=config.BANNER_DESCRIPTION_TABLE,
                    schema=get_banner_desc_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='chevent_shows.yson',
                    cypress_path=config.CHEVENT_SHOWS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='chevent_clicks.yson',
                    cypress_path=config.CHEVENT_CLICKS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='active_banners.yson',
                    cypress_path=config.ACTIVE_BANNERS_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
