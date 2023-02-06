# -*- coding: utf-8 -*-

import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.words import word_rules
from crypta.profile.utils.segment_utils.yql_word_filter import YqlWordFilter


def get_input_schema():
    return schema_utils.yt_schema_from_dict({
        "lemmas": "any",
        "yandexuid": "uint64",
    })


class TestTask(word_rules.DailyWordRulesProcessor):
    input_dir = "//input"
    index_dir = "//index"
    yql_word_filter_data_size_per_job = "100M"


def test_daily_word_rules(clean_local_yt, date, patched_config):
    word_filter = YqlWordFilter()
    word_filter.add_condition_string(
        rule_revision_id=1,
        condition_string=u'(форум OR forum OR портал) AND (военный OR исторический) AND NOT китай'
    )
    word_filter.add_condition_string(
        rule_revision_id=2,
        condition_string=u'((дримкас AND ф) OR ((модуль AND касса) OR модулькасса))'
    )
    word_filter.add_condition_string(
        rule_revision_id=3,
        condition_string=u'телевизор'
    )

    rule_revision_ids = {1, 2, 3}

    task = TestTask(
        date=date,
        yql_filter=word_filter,
        rule_revision_ids=rule_revision_ids,
    )

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=clean_local_yt,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.get_yson_table_with_schema('input.yson', yt.ypath_join(TestTask.input_dir, date), get_input_schema()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable('index.yson', yt.ypath_join(TestTask.index_dir, date), yson_format='pretty'), tests.Diff()),
            (tables.YsonTable('rules.yson', task.output().table, yson_format='pretty'), tests.Diff()),
        ],
        dependencies_are_missing=False,
        need_io_cleanup=False,
    )


def test_empty_daily_word_rules(clean_local_yt, date, patched_config):
    task = TestTask(
        date=date,
        yql_filter=YqlWordFilter(),
        rule_revision_ids={1},
    )

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=clean_local_yt,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.get_yson_table_with_schema('input.yson', yt.ypath_join(TestTask.input_dir, date), get_input_schema()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable('index.yson', yt.ypath_join(TestTask.index_dir, date), yson_format='pretty'), tests.Diff()),
            (tables.YsonTable('rules.yson', task.output().table, yson_format='pretty'), tests.Diff()),
        ],
        dependencies_are_missing=False,
        need_io_cleanup=False,
    )
