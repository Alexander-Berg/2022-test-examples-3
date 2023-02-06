import functools
import os

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import orgvisits_for_description
from crypta.affinitive_geo.services.org_embeddings.lib.utils import config
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils.config import config as lal_config
from crypta.profile.lib import date_helpers


def get_aggregated_orgvisits_schema():
    return [
        {'name': 'crypta_id', 'type': 'uint64'},
        {'name': 'permalinks', 'type_v3': {'type_name': 'list', 'item': 'uint64'}},
    ]


def get_company_pretty_format_schema():
    return [
        {'name': 'permalink', 'type': 'int64'},
        {'name': 'address', 'type': 'string'},
        {'name': 'country_name', 'type': 'string'},
        {'name': 'geo_id', 'type': 'int32'},
        {'name': 'lat', 'type': 'double'},
        {'name': 'lon', 'type': 'double'},
        {'name': 'main_rubric_id', 'type': 'int64'},
        {'name': 'main_rubric_name_ru', 'type': 'string'},
        {'name': 'name', 'type': 'string'},
        {'name': 'publishing_status', 'type': 'string'},
    ]


def test_orgvisits_for_description(yt_client, yql_client, date):
    lal_model_dir = 'crypta/affinitive_geo/services/org_embeddings/lib/test/sandbox_data/crypta_look_alike_model'

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            orgvisits_for_description.get,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data/test_orgvisits_for_description'),
        input_tables=[
            (
                files.YtFile(
                    file_path=yatest.common.build_path(os.path.join(lal_model_dir, 'segments_dict')),
                    cypress_path=os.path.join(lal_config.LOOKALIKE_VERSIONS_DIRECTORY, '1653517651', 'segments_dict.json'),
                ),
                tests.YtTest(),
            ),
            (
                files.YtFile(
                    file_path=yatest.common.build_path(os.path.join(lal_model_dir, 'dssm_lal_model.applier')),
                    cypress_path=os.path.join(lal_config.LOOKALIKE_VERSIONS_DIRECTORY, '1653517651', 'dssm_model.applier'),
                ),
                tests.YtTest(),
            ),
            (
                tables.YsonTable(
                    file_path='/dev/null',
                    cypress_path=os.path.join(lal_config.LOOKALIKE_VERSIONS_DIRECTORY, '1653517651', 'user_embeddings'),
                ),
                tests.YtTest(),
            ),
            (
                files.YtFile(
                    file_path='/dev/null',
                    cypress_path=os.path.join(config.LOOKALIKE_VERSION_DIR, 'segments_dict.json'),
                ),
                tests.YtTest(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='aggregated_orgvisits.yson',
                    cypress_path=os.path.join(
                        config.AGGREGATED_ORGVISITS_DIR,
                        date_helpers.get_date_from_past(date, days=config.DAYS_BACK_TO_GET_ORGVISITS),
                    ),
                    schema=get_aggregated_orgvisits_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='company_pretty_format.yson',
                    cypress_path=config.COMPANY_INFO_PRETTY_FORMAT_TABLE,
                    schema=get_company_pretty_format_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                files.YtFile(
                    file_path='segments_dict.json',
                    cypress_path=os.path.join(config.LOOKALIKE_VERSION_DIR, 'segments_dict.json'),
                ),
                tests.Diff(),
            ),
            (
                files.YtFile(
                    file_path='dssm_model.applier',
                    cypress_path=os.path.join(config.LOOKALIKE_VERSION_DIR, 'dssm_model.applier'),
                ),
                tests.Exists(),
            ),
            (
                tables.YsonTable(
                    file_path='orgvisits_for_description.yson',
                    cypress_path=config.ORGVISITS_FOR_DESCRIPTION_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    file_path='orgs_info.yson',
                    cypress_path=config.ORGS_INFO_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
