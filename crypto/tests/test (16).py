from __future__ import print_function

import mock
import uuid

from crypta.graph.households.data_import.watchlog.lib import WatchLogParser
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, execute


@mock.patch.object(WatchLogParser, "_set_expiration_time", lambda self: 42)
@mock.patch("time.time", mock.MagicMock(return_value=1561460704.213396))
@mock.patch("uuid.uuid4", mock.MagicMock(return_value=uuid.UUID("7f8add24-a917-485a-a49a-9e55dce4d70d")))
@load_fixtures(
    ("//home/logfeller/logs/bs-watch-log/stream/5min/2018-10-18T11:45:00", "/fixtures/watch0.json"),
    ("//home/logfeller/logs/bs-watch-log/stream/5min/2018-10-18T11:50:00", "/fixtures/watch1.json"),
    ("//home/logfeller/logs/bs-watch-log/stream/5min/2018-10-18T12:00:00", "/fixtures/watch2.json"),
    ("//home/crypta/develop/state/households_new/processed", "/fixtures/processed.json"),
)
@canonize_output
def test_watchlog(local_yt, conf):
    """ Should check is fp parser correct """
    task = WatchLogParser()
    execute(task)

    def select_all(table_path):
        return list(local_yt.yt_client.read_table(table_path, format="json"))

    output_tables = (
        "//home/crypta/develop/state/households_new/processed",
        "//home/crypta/develop/state/households_new/processed_dates",
        "//home/crypta/develop/state/households_new/stream/1561460704-7f8add24-a917-485a-a49a-9e55dce4d70d"
    )
    return {table: sorted(select_all(table)) for table in output_tables}
