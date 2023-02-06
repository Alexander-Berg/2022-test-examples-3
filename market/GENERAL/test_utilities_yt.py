import market.dynamic_pricing.deprecated.utilities.lib.yt as tested_module
from core.testenv import testenv
from core.yt_context import YtContext
from collections import OrderedDict
from datetime import datetime, timedelta
from pytz import timezone
import os, json, logging
import pytest

# File contents for the local file systen
typical_content = [
    (1, 7000, 0.45),
    (1, 8000, 0.35),
    (1, 9000, 0.25)
]

# Local file system in the order of creation
filesystem_json = OrderedDict()
filesystem_json["2019-12-10T00:00:00"] = {
    "content": typical_content[:],
}
filesystem_json["newer"] = {
    "content": typical_content[:],
}
filesystem_json["2019-12-10"] = {
    "content": typical_content[:],
}
filesystem_json["completely_new"] = {
    "content": typical_content[:],
}
filesystem_json["sort_of_new"] = {
    "content": typical_content[:],
}
filesystem_json["2019-12"] = {
    "content": typical_content[:],
}
filesystem_json["latest"] = {
    "content": typical_content[:],
}
filesystem_json["2019-12-10 12:30:00"] = {
    "content": typical_content[:],
}
filesystem_json["latest_for_real_this_time"] = {
    "content": typical_content[:],
}
filesystem_json["2019-12-10T17:15:00"] = {
    "content": typical_content[:],
}


@pytest.fixture
def yt_context(testenv):
    ctx = YtContext(testenv)
    for file in filesystem_json:
        ctx.create_demand_table(file, filesystem_json[file].get("attributes", {}))
        for line in filesystem_json[file].get("content", []):
            ctx.get_table(file).add_row(*line)

    with ctx:
        yield ctx


def test_latest_by_creation_ts(yt_context):
    # Write out the strftime mask
    pipeline_TS_format = '%Y-%m-%dT%H:%M:%S'
    # Test that the local YT is up
    assert yt_context.client.exists(yt_context.root)
    # Test that all tables are created
    assert all([yt_context.client.exists(yt_context.root + '/' + table) for table in filesystem_json])

    # Test latest_by_creation_ts at timestamp after the YT is filled up
    # It checks regexp filtering and sorting of the filenames
    assert map(
        str,
        tested_module.latest_by_creation_ts(
            "2150-12-31T12:00:00",
            yt_context.root, yt_context.client
        )
    ) == ["2019-12", "2019-12-10", "2019-12-10 12:30:00", "2019-12-10T00:00:00", "2019-12-10T17:15:00"]

    # Test latest_by_creation_ts at timestamp before the YT is filled up
    # It checks the creation timestamp filtering
    assert map(
        str,
        tested_module.latest_by_creation_ts(
            "1990-01-01T12:00:00",
            yt_context.root, yt_context.client
        )
    ) == []

    # Test latest_by_creation_ts at timestamp while the YT is filling up
    # It checks the creation timestamp filtering, regexp filtering, and sorting of the filenames
    UTC = timezone('UTC')
    MSK = timezone('Europe/Moscow')
    convert_UTC_to_MSK = lambda x: UTC.localize(datetime.strptime(x, pipeline_TS_format + '.%fZ')).astimezone(MSK)

    ts_midtest = convert_UTC_to_MSK(yt_context.client.get_attribute(yt_context.root + "/sort_of_new", 'creation_time'))
    assert map(
        str,
        tested_module.latest_by_creation_ts(
            ts_midtest.strftime(pipeline_TS_format),
            yt_context.root, yt_context.client
        )
    ) == ["2019-12-10", "2019-12-10T00:00:00"]

    ts_midtest = convert_UTC_to_MSK(yt_context.client.get_attribute(yt_context.root + "/newer", 'creation_time'))
    assert map(
        str,
        tested_module.latest_by_creation_ts(
            ts_midtest.strftime(pipeline_TS_format),
            yt_context.root, yt_context.client
        )
    ) == ["2019-12-10T00:00:00"]

    ts_midtest = convert_UTC_to_MSK(yt_context.client.get_attribute(yt_context.root + "/latest", 'creation_time'))
    assert map(
        str,
        tested_module.latest_by_creation_ts(
            ts_midtest.strftime(pipeline_TS_format),
            yt_context.root, yt_context.client
        )
    ) == ["2019-12", "2019-12-10", "2019-12-10T00:00:00"]

    ts_midtest = convert_UTC_to_MSK(yt_context.client.get_attribute(yt_context.root + "/latest_for_real_this_time", 'creation_time'))
    assert map(
        str,
        tested_module.latest_by_creation_ts(
            ts_midtest.strftime(pipeline_TS_format),
            yt_context.root, yt_context.client
        )
    ) == ['2019-12', '2019-12-10', '2019-12-10 12:30:00', '2019-12-10T00:00:00']


def latest_by_title_ts(yt_context):
    # Test that the local YT is up
    assert yt_context.client.exists(yt_context.root)
    # Test that all tables are created
    assert all([yt_context.client.exists(yt_context.root + '/' + table) for table in filesystem_json])

    # Test at different formats
    timestamp_post_test = "2020-07-04T13:24:53"
    assert map(
        str,
        tested_module.latest_by_title_ts(
            timestamp_post_test, yt_context.root,
            yt_context.client, "%Y-%m"
        )
    ) == ["2019-12"]

    assert map(
        str,
        tested_module.latest_by_title_ts(
            timestamp_post_test, yt_context.root,
            yt_context.client, "%Y-%m-%d"
        )
    ) == ["2019-12-10"]

    assert map(
        str,
        tested_module.latest_by_title_ts(
            timestamp_post_test, yt_context.root,
            yt_context.client, "%Y-%m-%d %H:%M:%S"
        )
    ) == ["2019-12-10 12:30:00"]

    assert map(
        str,
        tested_module.latest_by_title_ts(
            timestamp_post_test, yt_context.root,
            yt_context.client, "%Y-%m-%dT%H:%M:%S"
        )
    ) == ["2019-12-10T00:00:00", "2019-12-10T17:15:00"]

    # Test at different timestamps
    assert map(
        str,
        tested_module.latest_by_title_ts(
            "2019-12-10T10:45:00", yt_context.root,
            yt_context.client, "%Y-%m-%dT%H:%M:%S"
        )
    ) == ["2019-12-10T00:00:00"]

    assert map(
        str,
        tested_module.latest_by_title_ts(
            "2019-01-01T00:00:01", yt_context.root,
            yt_context.client, "%Y-%m-%dT%H:%M:%S"
        )
    ) == []
