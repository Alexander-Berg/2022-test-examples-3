import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils.config import config
from crypta.lookalike.services.training.lib import segments


def test_select_segments(patched_yt_client):

    output_tables = [
        tables.YsonTable(
            'segments_with_counts.yson',
            config.SEGMENTS_WITH_COUNTS_TABLE,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'segments_for_28_days.yson',
            config.SEGMENTS_FOR_LAL_TRAINING_TABLE,
            yson_format='pretty',
        ),
    ]

    with mock.patch('crypta.lookalike.lib.python.utils.config.config.ID_TYPES', ['idfa', 'gaid']), \
            mock.patch('crypta.lookalike.lib.python.utils.config.config.MIN_USERS_PER_SEGMENT', 3), \
            mock.patch('crypta.lookalike.lib.python.utils.config.config.MIN_USERS_PER_APP', 1), \
            mock.patch('crypta.lookalike.lib.python.utils.config.config.MIN_USERS_PER_RMP_GOAL', 3):
        return tests.yt_test_func(
            yt_client=patched_yt_client,
            func=lambda: segments.select(nv_params=None),
            data_path=yatest.common.test_source_path('data/segments'),
            return_result=False,
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'segments_simple.yson',
                        config.AUDIENCE_SEGMENTS_INFO_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.segments_simple_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'segments_full_counts.yson',
                        config.AUDIENCE_SEGMENTS_USERS_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.segments_full_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'goal_audiences.yson',
                        config.GOALS_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.goals_yandexuid_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'metrika_segments.yson',
                        config.METRIKA_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.metrika_yandexuid_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'user_data.yson',
                        config.USER_DATA_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.user_data_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'user_data_by_cryptaid.yson',
                        config.USER_DATA_BY_CRYPTAID_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.user_data_by_cryptaid_schema),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'goals_for_training.yson',
                        config.TRAINING_GOALS_TABLE,
                        schemas.goals_for_training_schema,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'segments_for_training.yson',
                        config.TRAINING_SEGMENTS_TABLE,
                        schemas.segments_for_training_schema,
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'mobile_goals_table.yson',
                        config.MOBILE_GOALS_TRACKER_TABLE,
                        schema_utils.yt_schema_from_dict(schemas.mobile_goals_tracker_schema),
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'postback_log.yson',
                        config.POSTBACK_MOBILE_LOG + '/2022-01-01',
                        schema_utils.yt_schema_from_dict(schemas.postback_log_schema),
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'apps_by_devid_monthly.yson',
                        config.APP_BY_DEVID_MONTHLY,
                        schemas.apps_by_devid_schema,
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'gaid_matching.yson',
                        config.PUBLIC_MATCHING_DIRECTORY + '/gaid/crypta_id',
                        schema_utils.yt_schema_from_dict(schemas.matching_schema),
                    ),
                    tests.Exists(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'idfa_matching.yson',
                        config.PUBLIC_MATCHING_DIRECTORY + '/idfa/crypta_id',
                        schema_utils.yt_schema_from_dict(schemas.matching_schema),
                    ),
                    tests.Exists(),
                ),
            ],
            output_tables=[(table, tests.Diff()) for table in output_tables],
        )
