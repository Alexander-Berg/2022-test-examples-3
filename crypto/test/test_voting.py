from functools import partial
import os

import yatest.common

import crypta.lib.python.yql.client as yql_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.socdem_helpers import test_utils
from crypta.profile.lib.socdem_helpers.inference_utils import voting
from crypta.profile.utils.config import config


yandexuid_socdem_schema = {
    'yandexuid': 'uint64',
    'update_time': 'uint64',
    'is_active': 'boolean',
    'gender': 'any',
    'user_age_6s': 'any',
    'income_5_segments': 'any',
    'exact_socdem': 'any',
    'age_scores': 'any',
    'gender_scores': 'any',
    'income_scores': 'any',
    'crypta_id': 'uint64',
}

devid_socdem_schema = {
    'id': 'string',
    'id_type': 'string',
    'update_time': 'uint64',
    'gender': 'any',
    'user_age_6s': 'any',
    'income_5_segments': 'any',
    'crypta_id': 'uint64',
}


def test_correct_yandexuid_profiles_without_crypta_id(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    yandexuid_socdem_without_crypta_id_table = tables.get_yson_table_with_schema(
        'yandexuid_socdem_without_crypta_id.yson',
        '//home/profiles/yandexuid_socdem_without_crypta_id',
        schema_utils.yt_schema_from_dict(yandexuid_socdem_schema)
    )

    output_tables = {}
    for table in [
        'yandexuid_socdem_without_crypta_id_active',
        'yandexuid_socdem_without_crypta_id_not_active',
    ]:
        output_tables[table] = tables.YsonTable(
            '{}.yson'.format(table),
            '//home/profiles/{}'.format(table),
            yson_format='pretty',
        )

    def test_func():
        yt_client.run_map(
            partial(voting.correct_yandexuid_profiles_without_crypta_id, thresholds=test_utils.test_thresholds),
            yandexuid_socdem_without_crypta_id_table.cypress_path,
            [
                output_tables['yandexuid_socdem_without_crypta_id_active'].cypress_path,
                output_tables['yandexuid_socdem_without_crypta_id_not_active'].cypress_path,
            ],
        )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=test_func,
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[(yandexuid_socdem_without_crypta_id_table, tests.TableIsNotChanged())],
        output_tables=[(table, tests.Diff()) for table in output_tables.values()],
    )


def get_output_tables_with_corrected_socdem():
    output_tables = {}

    for table in [
        'crypta_id_profiles_table',
        'corrected_devid_profiles_table',
        'corrected_yandexuid_profiles_table',
        'corrected_not_active_yandexuid_profiles_table',
    ]:
        output_tables[table] = tables.YsonTable(
            '{}.yson'.format(table),
            '//home/profiles/{}'.format(table),
            yson_format='pretty',
        )

    return output_tables


def test_calculate_crypta_id_socdem(yt_stuff):
    yt_client = yt_stuff.get_yt_client()

    input_tables = {
        'yandexuid_socdem_with_crypta_id': tables.get_yson_table_with_schema(
            'yandexuid_socdem_with_crypta_id.yson',
            '//home/profiles/yandexuid_socdem_with_crypta_id',
            schema_utils.yt_schema_from_dict(yandexuid_socdem_schema,
                                             sort_by=['crypta_id'])
        ),
        'devid_socdem_with_crypta_id': tables.get_yson_table_with_schema(
            'devid_socdem_with_crypta_id.yson',
            '//home/profiles/devid_socdem_with_crypta_id',
            schema_utils.yt_schema_from_dict(devid_socdem_schema,
                                             sort_by=['crypta_id'])
        ),
        'crypta_id_socdem_storage': tables.get_yson_table_with_schema(
            'crypta_id_socdem_storage.yson',
            '//home/profiles/crypta_id_socdem_storage',
            schema_utils.yt_schema_from_dict({
                'crypta_id': 'uint64',
                'age_scores': 'any',
                'gender_scores': 'any',
                'income_scores': 'any',
            }, sort_by=['crypta_id'])
        ),
    }

    output_tables = get_output_tables_with_corrected_socdem()

    def test_func():
        yt_client.run_reduce(
            voting.CalculateCryptaIdSocdem(thresholds=test_utils.test_thresholds),
            [
                input_tables['yandexuid_socdem_with_crypta_id'].cypress_path,
                input_tables['devid_socdem_with_crypta_id'].cypress_path,
                input_tables['crypta_id_socdem_storage'].cypress_path,
            ],
            [
                output_tables['crypta_id_profiles_table'].cypress_path,
                output_tables['corrected_devid_profiles_table'].cypress_path,
                output_tables['corrected_yandexuid_profiles_table'].cypress_path,
                output_tables['corrected_not_active_yandexuid_profiles_table'].cypress_path,
            ],
            reduce_by=['crypta_id'],
        )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=test_func,
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[(table, tests.TableIsNotChanged()) for table in input_tables.values()],
        output_tables=[(table, tests.Diff()) for table in output_tables.values()],
    )


def test_run_voting(local_yt, local_yt_and_yql_env):
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()
    yql_client = yql_helpers.create_yql_client(
        yt_proxy=local_yt.get_server(),
        pool='fake_pool',
        token=os.getenv('YQL_TOKEN'),
    )
    output_tables = get_output_tables_with_corrected_socdem()

    return tests.yt_test_func(
        yt_client=yt_client,
        func=partial(
            voting.run_voting,
            yt_client=yt_client,
            yql_client=yql_client,
            thresholds=test_utils.test_thresholds,
            date='2021-08-13',
            results_directory='//home/profiles/',
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'merged_raw_yandexuid_profiles.yson',
                    config.MERGED_RAW_YANDEXUID_PROFILES,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'yandexuid', 'update_time', 'gender', 'user_age_6s', 'income_5_segments',
                    ]), sort_by=['yandexuid']),
                ), tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'merged_raw_devid_profiles.yson',
                    config.MERGED_RAW_DEVID_PROFILES,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'id', 'id_type', 'update_time', 'gender', 'user_age_6s', 'income_5_segments',
                    ]), sort_by=['id', 'id_type']),
                ), tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'profiles_for_14days.yson',
                    config.YANDEXUID_EXPORT_PROFILES_14_DAYS_TABLE,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'yandexuid', 'exact_socdem',
                    ]), sort_by=['yandexuid']),
                ), tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'yandexuid_socdem_storage.yson',
                    config.YANDEXUID_SOCDEM_STORAGE_TABLE,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'yandexuid', 'gender_scores', 'age_scores', 'income_scores',
                    ]), sort_by=['yandexuid']),
                ), tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'crypta_id_socdem_storage.yson',
                    config.CRYPTA_ID_SOCDEM_STORAGE_TABLE,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'crypta_id', 'gender_scores', 'age_scores', 'income_scores',
                    ]), sort_by=['crypta_id']),
                ), tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'yandexuid_crypta_id_matching.yson',
                    config.YANDEXUID_CRYPTAID_TABLE,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'yandexuid', 'crypta_id',
                    ]), sort_by=['yandexuid']),
                ), tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'devid_crypta_id_matching.yson',
                    config.DEVID_CRYPTAID_TABLE,
                    schema_utils.yt_schema_from_dict(test_utils.get_schema_for_fields([
                        'id', 'id_type', 'crypta_id',
                    ]), sort_by=['id', 'id_type']),
                ), tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[(table, tests.Diff()) for table in output_tables.values()],
    )
