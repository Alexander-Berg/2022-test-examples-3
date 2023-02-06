import os

from yt.wrapper import (
    transaction,
    ypath,
)

from crypta.lib.native.yt.processed_tables_tracker.proto.tracked_source_pb2 import TTrackedSource
from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.processed_tables_tracker import ProcessedTablesTracker


def test_processed_tables_tracker():
    tracked_source = TTrackedSource(SourceDir="//tmp/log", TrackTable="//tmp/.processed")

    yt_proxy = os.getenv("YT_PROXY")
    yt_client = yt_helpers.get_yt_client(yt_proxy, yt_token="unused")

    tracker = ProcessedTablesTracker(tracked_source)

    table_1 = ypath.ypath_join(tracked_source.SourceDir, "1")
    table_2 = ypath.ypath_join(tracked_source.SourceDir, "2")
    table_3 = ypath.ypath_join(tracked_source.SourceDir, "3")
    table_4 = ypath.ypath_join(tracked_source.SourceDir, "4")

    for table_path in [table_1, table_2]:
        yt_client.create("table", table_path, recursive=True)

    assert [table_2, table_1] == tracker.get_unprocessed_tables(yt_client)
    assert [table_2] == tracker.get_unprocessed_tables(yt_client, 1)

    tracker.add_processed_tables(yt_client, [table_1, table_2])
    assert [] == tracker.get_unprocessed_tables(yt_client)

    yt_client.create("table", table_3, recursive=True)
    yt_client.create("table", table_4, recursive=True)
    assert [table_4, table_3] == tracker.get_unprocessed_tables(yt_client)

    was_in_function = []

    def f(tx, table_path):
        assert isinstance(tx, transaction.Transaction)
        was_in_function.append(table_path)

    tracker.for_each_unprocessed(yt_client, f)
    assert [] == tracker.get_unprocessed_tables(yt_client)
    assert [table_4, table_3] == was_in_function
