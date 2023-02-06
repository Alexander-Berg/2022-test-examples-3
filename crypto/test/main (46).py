import os

from yt.wrapper import transaction

from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.processed_tracker import ProcessedTracker


def test_processed_tables_tracker():
    yt_proxy = os.getenv("YT_PROXY")
    yt_client = yt_helpers.get_yt_client(yt_proxy, yt_token="unused")

    track_table = "//tmp/track"

    tracker = ProcessedTracker(yt_client, track_table)

    item_1 = "item_1"
    item_2 = "item_2"
    item_3 = "item_3"
    item_4 = "item_4"

    processed_items = []

    def f(tx, unprocessed_item):
        assert isinstance(tx, transaction.Transaction)
        processed_items.append(unprocessed_item)

    tracker.for_each_unprocessed([item_1, item_2], f)
    assert [item_2, item_1] == processed_items
    assert _wrap([item_2, item_1]) == list(yt_client.read_table(track_table))

    tracker.for_each_unprocessed([item_2, item_3, item_4], f)
    assert [item_2, item_1, item_4, item_3] == processed_items
    assert _wrap([item_4, item_3, item_2]) == list(yt_client.read_table(track_table))


def _wrap(l):
    return [{"item": i} for i in l]
