import yatest.common

import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.frozen_dict import FrozenDict
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors import search_requests


def get_input_schema():
    return schema_utils.yt_schema_from_dict({
        "ans_hosts": "any",
        "yandexuid": "uint64",
    })


def get_output_schema():
    return schema_utils.yt_schema_from_dict({
        "yandexuid": "uint64",
        "rule_id": "uint64",
    }, sort_by=["yandexuid"])


def test_search_result(clean_local_yt, patched_config, date):
    host_to_rule_revision_id = {
        "a.com": {1, 2},
        "b.ru": {2, 3},
    }
    rule_revision_ids = {1, 2, 3}

    task = search_requests.GetStandardSegmentsBySearchResultsDayProcessor(
        date=date,
        host_to_rule_revision_id=FrozenDict(host_to_rule_revision_id),
        rule_revision_ids=rule_revision_ids,
    )

    return task_test_helpers.run_and_test_task(
        yt=clean_local_yt,
        task=task,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.get_yson_table_with_schema('reqans.yson', yt.ypath_join(task.input_dir, date), get_input_schema()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                'index.yson',
                task.input().table,
                yson_format='pretty',
            ), (tests.Diff())),
            (tables.YsonTable(
                'rule_ids.yson',
                task.output().table,
                yson_format='pretty',
            ), (tests.Diff())),
        ],
        dependencies_are_missing=False,
        need_io_cleanup=False,
    )
