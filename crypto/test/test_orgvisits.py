import os

import yatest.common

from crypta.affinitive_geo.services.orgvisits.lib.config_pb2 import TConfig
from crypta.graph.matching.direct.proto.types_pb2 import TDirectEdge
from crypta.lib.python import yaml_config
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib import date_helpers


def get_matching_schema():
    return schema_utils.get_schema_from_proto(TDirectEdge)


def get_geocube_maps_schema():
    return [
        {'name': 'yandexuid', 'type': 'string'},
        {'name': 'device_id', 'type': 'string'},
        {'name': 'passport_uid', 'type': 'string'},
        {'name': 'answers_and_clicks', 'type': 'any'},
    ]


def get_orgvisits_export_schema():
    return [
        {'name': 'devid', 'type': 'string'},
        {'name': 'puid', 'type': 'string'},
        {'name': 'permalink', 'type': 'int64'},
        {'name': 'methods', 'type': 'any'},
    ]


def get_org_info_schema():
    return schema_utils.get_strict_schema([
        {'name': 'permalink', 'type': 'int64'},
        {'name': 'head_permalink', 'type': 'int64'},
        {'name': 'publishing_status', 'type': 'string'},
        {'name': 'is_online', 'type': 'boolean'},
        {'name': 'main_rubric_id', 'type': 'int64'},
        {'name': 'popularity', 'type': 'double'},
    ])


def get_businesses_rubrics_schema():
    return schema_utils.get_strict_schema([
        {'name': 'rubric', 'type': 'int64'},
    ])


def get_orgs_weights_schema():
    return schema_utils.get_strict_schema([
        {'name': 'permalink', 'type': 'uint64'},
        {'name': 'weight', 'type': 'int64'},
    ])


def get_cryptaid_userdata_schema():
    return schema_utils.get_strict_schema([
        {'name': 'CryptaID', 'type': 'string'},
    ])


def test_orgvisits(local_yt, local_yt_and_yql_env, config_file, frozen_time):
    config = yaml_config.parse_config(TConfig, config_file)

    last_orgvisits_export_date = '2022-04-24'
    fist_orgvisits_export_date = date_helpers.get_date_from_past(
        current_date=last_orgvisits_export_date,
        months=config.MonthsBackToAggregateOrgvisits,
    )

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path('crypta/affinitive_geo/services/orgvisits/bin/crypta-affinitive-geo-orgvisits'),
        args=[
            '--config', config_file,
        ],
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'geocube_maps_{}.yson'.format(last_orgvisits_export_date),
                    os.path.join(config.GeocubeDir, last_orgvisits_export_date, 'maps'),
                    get_geocube_maps_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'geocube_maps_{}.yson'.format(fist_orgvisits_export_date),
                    os.path.join(config.GeocubeDir, fist_orgvisits_export_date, 'maps'),
                    get_geocube_maps_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'orgvisits_export_{}.yson'.format(last_orgvisits_export_date),
                    os.path.join(config.OrgvisitsExportDir, last_orgvisits_export_date),
                    get_orgvisits_export_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'orgvisits_export_{}.yson'.format(fist_orgvisits_export_date),
                    os.path.join(config.OrgvisitsExportDir, fist_orgvisits_export_date),
                    get_orgvisits_export_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'org_info.yson',
                    config.OrgInfoTable,
                    get_org_info_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'businesses_rubrics.yson',
                    config.BusinessesRubricsTable,
                    get_businesses_rubrics_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'orgs_weights.yson',
                    os.path.join(config.OrgsWeightsDir, last_orgvisits_export_date),
                    get_orgs_weights_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'cryptaid_userdata.yson',
                    config.CryptaidUserdataTable,
                    get_cryptaid_userdata_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'puid_to_crypta_id.yson',
                    config.MatchingPuidTable,
                    get_matching_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'yandexuid_to_crypta_id.yson',
                    config.MatchingYandexuidTables[0],
                    get_matching_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'gaid_to_crypta_id.yson',
                    config.MatchingDevidTables[0],
                    get_matching_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'aggregated_orgvisits.yson',
                    os.path.join(config.AggregatedOrgvisitsDir, last_orgvisits_export_date),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
            (
                tables.YsonTable(
                    'bigb_orgvisits.yson',
                    os.path.join(config.BigbOrgvisitsDir, last_orgvisits_export_date),
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
        env=local_yt_and_yql_env,
    )
