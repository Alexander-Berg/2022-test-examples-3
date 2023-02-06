import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
import crypta.profile.lib.test_helpers.task_helpers as task_test_helpers
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.urls import url_rules
from crypta.profile.utils.segment_utils.url_filter import UrlFilter


def get_input_schema():
    return schema_utils.yt_schema_from_dict({
        "host": "string",
        "url": "string",
        "referer_host": "string",
        "yandexuid": "uint64",
    })


class TestTask(url_rules.DailyUrlRulesProcessor):
    source = "test"
    input_dir = "//input"
    index_dir = "//index"


def test_daily_url_rules(clean_local_yt_with_chyt, date, patched_config):
    url_filter = UrlFilter()
    url_filter.add_url(1, "javdb.com", r"javdb\\.com/rankings.*")
    url_filter.add_url(2, "fedsp.com", r"fedsp\\.com/.*")
    url_filter.add_url(2, "javdb.com", r"javdb\\.com/.*")
    url_filter.add_url(3, "fedsp.com", r"fedsp\\.com/.*")

    rule_revision_ids = {1, 2, 3}

    task = TestTask(
        date=date,
        url_filter=url_filter,
        rule_revision_ids=rule_revision_ids,
    )

    return task_test_helpers.run_and_test_task(
        task=task,
        yt=clean_local_yt_with_chyt,
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
