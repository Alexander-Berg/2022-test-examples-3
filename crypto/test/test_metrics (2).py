import os

from yt.wrapper import ypath

from crypta.lib.native.yt.processed_tables_tracker.proto.tracked_source_pb2 import TTrackedSource
from crypta.lib.python import time_utils
from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.processed_tables_tracker import ProcessedTablesTracker
from crypta.spine.pushers.yt_processed_tables_metrics import lib


def test_get_metrics():
    yt_proxy = os.getenv("YT_PROXY")
    yt_client = yt_helpers.get_yt_client(yt_proxy, yt_token="unused")

    tracked_source = TTrackedSource(SourceDir="//log1", TrackTable="//track_table1")

    tables = [
        ypath.ypath_join(tracked_source.SourceDir, "2019-05-07T10:00:00"),
        ypath.ypath_join(tracked_source.SourceDir, "2019-05-07T11:30:00"),
        ypath.ypath_join(tracked_source.SourceDir, "2019-05-07T12:00:00"),
        ypath.ypath_join(tracked_source.SourceDir, "2019-05-07T13:30:00"),
    ]

    for table in tables:
        yt_client.create("table", table, recursive=True)

    tracker = ProcessedTablesTracker(tracked_source)
    tracker.add_processed_tables(yt_client, [tables[0], tables[2]])

    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1557219600"
    return lib.get_metrics(yt_client, tracked_source.TrackTable)


def test_all_processed():
    yt_proxy = os.getenv("YT_PROXY")
    yt_client = yt_helpers.get_yt_client(yt_proxy, yt_token="unused")

    tracked_source = TTrackedSource(SourceDir="//log2", TrackTable="//track_table2")

    tables = [
        ypath.ypath_join(tracked_source.SourceDir, "2019-05-07T10:00:00"),
    ]

    for table in tables:
        yt_client.create("table", table, recursive=True)

    tracker = ProcessedTablesTracker(tracked_source)
    tracker.add_processed_tables(yt_client, [tables[0]])

    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1557219600"
    return lib.get_metrics(yt_client, tracked_source.TrackTable)
