import os

import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

from crypta.lookalike.services.training.lib import ads_goals_segments
from crypta.lookalike.lib.python.test_utils import schemas
from crypta.lookalike.lib.python.utils.config import config


def test_update_ads_goals_segments(patched_yt_client):
    output_tables = [
        tables.YsonTable(
            'update_dates.yson',
            config.ADS_UPDATE_DATES_TABLE,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'training_goals_table.yson',
            config.TRAINING_GOALS_TABLE,
            yson_format='pretty',
        ),
        tables.YsonTable(
            'training_segments_table.yson',
            config.TRAINING_SEGMENTS_TABLE,
            yson_format='pretty',
        ),
    ]

    return tests.yt_test_func(
        yt_client=patched_yt_client,
        func=lambda: ads_goals_segments.update(nv_params=None),
        data_path=yatest.common.test_source_path('data/ads_goals_segments'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    'meaningful_goals_table.yson',
                    config.MEANINGFUL_GOALS_IDS_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.goals_ids_schema),
                ),
                tests.Exists(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'retargeting_ids_table.yson',
                    config.RETARGETING_IDS_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.goals_ids_schema),
                ),
                tests.Exists(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'multipliers_ids_table.yson',
                    config.MULTIPLIERS_IDS_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.goals_ids_schema),
                ),
                tests.Exists(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'update_dates.yson',
                    config.ADS_UPDATE_DATES_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.update_dates_schema),
                ),
                tests.Exists(),
            ),
            (
                tables.get_yson_table_with_schema(
                    '2021-08-18-meaningful_goals.yson',
                    os.path.join(config.MEANINGFUL_GOALS_DIR, '2021-08-18'),
                    schemas.meaningful_goals_log_schema,
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    '2021-08-18-retargeting_ids.yson',
                    os.path.join(config.RETARGETING_DIR, '2021-08-18'),
                    schema_utils.yt_schema_from_dict(schemas.retargeting_log_schema),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    '2021-08-18-multipliers_ids.yson',
                    os.path.join(config.MULTIPLIERS_DIR, '2021-08-18'),
                    schema_utils.yt_schema_from_dict(schemas.retargeting_log_schema),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    'autobudget_goals_table.yson',
                    config.AUTOBUDGET_GOALS_LOG_TABLE,
                    schema_utils.yt_schema_from_dict(schemas.autobudget_log_schema),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[(output_table, tests.Diff()) for output_table in output_tables],
    )
