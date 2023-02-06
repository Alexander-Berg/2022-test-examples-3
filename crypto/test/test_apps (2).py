import yatest.common

import pytest

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.mobile_apps import apps


def get_apps_schema():
    return schema_utils.yt_schema_from_dict({
        "app": "string",
        "id": "string",
        "id_type": "string",
    }, sort_by=["app"])


@pytest.fixture
def app_to_rule_lab_id():
    return {
        "com.android.app1": {"rule-1", "rule-2"},
        "com.android.app2": {"rule-2", "rule-3"},
        "com.ios.app1": {"rule-4"},
    }


@pytest.fixture
def rule_revision_ids():
    return {1, 2, 3, 4}


@pytest.fixture
def task(date, rule_revision_ids, app_to_rule_lab_id):
    return apps.GetStandardSegmentsByMobileApp(
        date=date,
        app_to_rule_lab_id=app_to_rule_lab_id,
        rule_revision_ids=rule_revision_ids,
    )


def test_apps(clean_local_yt, patched_config, task):
    return tests.yt_test_func(
        yt_client=clean_local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (tables.YsonTable('apps.yson', task.input().table, on_write=tables.OnWrite(
                attributes={"schema": get_apps_schema()},
                sort_by=["app"],
            )), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                'rule_ids.yson',
                task.output().table,
                yson_format='pretty',
                on_read=tables.OnRead(sort_by=["id", "id_type"]),
            ), (tests.Diff())),
        ]
    )
