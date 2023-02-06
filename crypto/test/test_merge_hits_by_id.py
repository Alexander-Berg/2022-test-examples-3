import os

import mock
import pytest
import yatest.common

from crypta.profile.lib import date_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.tasks.features.merge_hits_by_id import(
    MergeHitsByCryptaid,
    MergeHitsByYandexuid,
)
from crypta.profile.utils.config import config


def get_hits_table_schema():
    return schema_utils.yt_schema_from_dict({
        'yandexuid': 'uint64',
        'site_weights': 'any',
    }, sort_by=['yandexuid'])


@pytest.mark.parametrize(
    'data_source',
    [
        'metrics',
        'bar',
    ],
    ids=[
        'metrics',
        'bar',
    ],
)
def test_merge_hits_by_yandexuid(local_yt, patched_config, date, data_source):
    with mock.patch('crypta.profile.tasks.features.process_user_events.ProcessUserEvents.complete', return_value=True):
        task = MergeHitsByYandexuid(date=date, data_source=data_source)
        input_dir = os.path.dirname(task.input()['hits'].table)

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=local_yt,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'hits_2021-11-23.yson',
                        os.path.join(input_dir, date_helpers.get_date_from_past(date, days=config.STANDARD_AGGREGATION_PERIOD)),
                        schema=get_hits_table_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'hits_2021-12-27.yson',
                        os.path.join(input_dir, date_helpers.get_yesterday(date)),
                        schema=get_hits_table_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'yandexuid_merged_hits.yson',
                        task.output().table,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
            dependencies_are_missing=False,
        )


@pytest.mark.parametrize(
    'data_source',
    [
        'metrics',
        'bar',
    ],
    ids=[
        'metrics',
        'bar',
    ],
)
def test_merge_hits_by_cryptaid(clean_local_yt, patched_config, date, data_source):
    with mock.patch('crypta.profile.tasks.features.merge_hits_by_id.MergeHitsByYandexuid.complete', return_value=True):
        task = MergeHitsByCryptaid(date=date, data_source=data_source)

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=clean_local_yt,
            data_path=yatest.common.test_source_path('data'),
            input_tables=[
                (
                    tables.get_yson_table_with_schema(
                        'yandexuid_merged_hits.yson',
                        task.input().table,
                        schema=schema_utils.yt_schema_from_dict({
                            'yandexuid': 'uint64',
                            'raw_site_weights': 'any',
                            'site_weights': 'any',
                        }, sort_by=['yandexuid']),
                    ),
                    tests.TableIsNotChanged(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        'yandexuid_cryptaid.yson',
                        patched_config.YANDEXUID_CRYPTAID_TABLE,
                        schema=schema_utils.yt_schema_from_dict({
                            'yandexuid': 'uint64',
                            'crypta_id': 'uint64',
                        }, sort_by=['yandexuid']),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        'cryptaid_merged_hits.yson',
                        task.output().table,
                        yson_format='pretty',
                    ),
                    tests.Diff(),
                ),
            ],
            dependencies_are_missing=False,
        )
