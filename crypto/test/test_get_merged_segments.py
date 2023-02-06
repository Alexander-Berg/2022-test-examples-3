import mock
import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.runners.export_profiles.lib.profiles_generation import get_merged_segments


def get_inputs(task, patched_config):
    return [
        (
            tables.get_yson_table_with_schema(
                'vertices_no_multi_profile.yson',
                patched_config.VERTICES_NO_MULTI_PROFILE,
                schema=schema_utils.yt_schema_from_dict({
                    'id': 'string',
                    'id_type': 'string',
                    'cryptaId': 'string',
                }, sort_by=['id', 'id_type']),
            ),
            tests.TableIsNotChanged(),
        ),
        (
            tables.get_yson_table_with_schema(
                'segment_storage.yson',
                task.input()['segment_storage'].table,
                schema=schema_utils.yt_schema_from_dict({
                    'id': 'string',
                    'id_type': 'string',
                    'update_time': 'uint64',
                    'probabilistic_segments': 'any',
                    'interests_composite': 'any',
                    'marketing_segments': 'any',
                    'heuristic_segments': 'any',
                    'lal_internal': 'any',
                    'lal_common': 'any',
                    'lal_private': 'any',
                    'heuristic_common': 'any',
                    'heuristic_internal': 'any',
                    'heuristic_private': 'any',
                    'audience_segments': 'any',
                    'longterm_interests': 'any',
                    'shortterm_interests': 'any',
                }, sort_by=['id', 'id_type']),
            ),
            tests.TableIsNotChanged(),
        ),
        (
            tables.get_yson_table_with_schema(
                'cryptaid_yandexuid.yson',
                patched_config.CRYPTAID_YANDEXUID_TABLE,
                schema=schema_utils.yt_schema_from_dict({
                    'crypta_id': 'uint64',
                    'yandexuid': 'uint64',
                }, sort_by=['crypta_id']),
            ),
            tests.TableIsNotChanged(),
        ),
    ]


def get_outputs(task):
    return [
        (
            tables.YsonTable(
                '{}.yson'.format(key),
                output.table,
                yson_format='pretty',
            ),
            tests.Diff(),
        )
        for key, output in task.output().iteritems()
    ]


def test_expand_segments_storage(local_yt, patched_config, date):
    with mock.patch('crypta.profile.tasks.features.get_crypta_ids.GetCryptaIds.complete') as mock_complete:
        mock_complete.return_value = True

        task = get_merged_segments.ExpandSegmentsStorage(date=date)

        return task_test_helpers.run_and_test_task(
            task=task,
            yt=local_yt,
            data_path=yatest.common.test_source_path('data/expand_segments_storage'),
            input_tables=get_inputs(task, patched_config),
            output_tables=get_outputs(task),
            dependencies_are_missing=False,
        )
