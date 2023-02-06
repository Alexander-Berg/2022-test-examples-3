import functools
import os

import mock
import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import org_affinitive_banners
from crypta.affinitive_geo.services.org_embeddings.lib.utils import (
    config,
    test_utils,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def get_orgvisits_for_description_schema():
    return [
        {'name': 'IdValue', 'type': 'string'},
        {'name': 'GroupID', 'type': 'string'},
    ]


def get_chevent_shows_clicks_schema():
    return [
        {'name': 'crypta_id', 'type': 'uint64'},
        {'name': 'groupbannerid', 'type': 'int64'},
        {'name': 'regionid', 'type': 'int64'},
    ]


def get_active_banners_schema():
    return [
        {'name': 'groupbannerid', 'type': 'int64'},
        {'name': 'banner_body', 'type': 'string'},
    ]


def test_org_affinitive_banners(yt_client, yql_client, date):
    with mock.patch.object(config, 'BANNERS_CNT_PER_ORG', 1):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=functools.partial(
                org_affinitive_banners.get,
                yt_client=yt_client,
                yql_client=yql_client,
                date=date,
            ),
            data_path=yatest.common.test_source_path('data/test_org_affinitive_banners'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        file_path='orgvisits_for_description.yson',
                        cypress_path=config.ORGVISITS_FOR_DESCRIPTION_TABLE,
                        schema=get_orgvisits_for_description_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
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
                        file_path='chevent_shows.yson',
                        cypress_path=config.CHEVENT_SHOWS_TABLE,
                        schema=get_chevent_shows_clicks_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        file_path='chevent_clicks.yson',
                        cypress_path=config.CHEVENT_CLICKS_TABLE,
                        schema=get_chevent_shows_clicks_schema(),
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
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        file_path='org_affinitive_banners.yson',
                        cypress_path=os.path.join(config.ORG_AFFINITIVE_BANNERS_DIR, date),
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
        )
