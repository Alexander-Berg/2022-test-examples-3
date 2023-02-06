from __future__ import print_function

import mock

from crypta.graph.households.data_import.increment_day.lib import IncrementDay
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, execute


@mock.patch.object(IncrementDay, "_set_expiration_time", lambda self: 42)
@mock.patch.object(IncrementDay, "date", property(lambda self: "2018-10-19"))
@load_fixtures(
    ("//home/crypta/develop/state/households_new/processed", "/fixtures/processed.json"),
    ("//home/crypta/develop/state/households_new/processed_dates", "/fixtures/dates.json"),
    ("//home/crypta/develop/state/households_new/storage/2018-10-17", "/fixtures/17.json"),
    ("//home/crypta/develop/state/households_new/storage/2018-10-18", "/fixtures/18.json"),
    ("//home/crypta/develop/state/households_new/stream/tbl1", "/fixtures/tbl1.json"),
    ("//home/crypta/develop/state/households_new/stream/tbl2", "/fixtures/tbl2.json"),
)
@canonize_output
def test_increment_day(local_yt, conf):
    """ Should check is fp parser correct """
    task = IncrementDay()
    execute(task)

    # stream tables should be removed while inrement day
    assert not local_yt.yt_client.exists("//home/crypta/develop/state/households_new/stream/tbl1")
    assert not local_yt.yt_client.exists("//home/crypta/develop/state/households_new/stream/tbl2")

    def select_all(table_path):
        return list(local_yt.yt_client.read_table(table_path, format="json"))

    output_tables = (
        "//home/crypta/develop/state/households_new/processed",
        "//home/crypta/develop/state/households_new/processed_dates",
        "//home/crypta/develop/state/households_new/storage/2018-10-17",  # should not be changed
        "//home/crypta/develop/state/households_new/storage/2018-10-18",  # should be updated
        "//home/crypta/develop/state/households_new/storage/2018-10-19",  # should be created new
    )
    return {table: sorted(select_all(table)) for table in output_tables}
