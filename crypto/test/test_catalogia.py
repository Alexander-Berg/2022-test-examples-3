import yatest.common

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.lib.frozen_dict import FrozenDict
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.catalogia import catalogia


def get_input_schema():
    return schema_utils.yt_schema_from_dict({
        "yandexuid": "uint64",
        "category": "uint64",
        "timestamp": "uint64",
    })


def test_catalogia(clean_local_yt, date, patched_config):
    catalogia_id_to_rule_revision_id = {
        1: [1],
        2: [2, 3],
    }
    rule_revision_ids = {1, 2, 3}

    task = catalogia.GetStandardSegmentsByCatalogiaDailyProcessor(
        date=date,
        catalogia_id_to_rule_revision_id=FrozenDict(catalogia_id_to_rule_revision_id),
        rule_revision_ids=rule_revision_ids,
    )

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=clean_local_yt,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.YsonTable('input.yson', task.input().table, on_write=tables.OnWrite(attributes={
                "schema": get_input_schema(),
                "closed": True,
            })), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable('rules.yson', task.output().table, yson_format='pretty'), tests.Diff()),
        ],
        dependencies_are_missing=False,
        need_io_cleanup=False,
    )
