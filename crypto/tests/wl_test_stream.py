import os

from library.python import resource

from crypta.graph.data_import.watch_log.lib import WatchLogImportTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, load_fixtures, do_stream_test


@clean_up(observed_paths=("//home",))
@load_fixtures(
    ("//home/logfeller/logs/bs-watch-log/stream/5min/2019-06-24T12:00:00", "/fixtures/bswatch_log.json"),
    ("//home/crypta/develop/graph/config/wl_client_user_id/approved_domens", "/fixtures/approved_domens.json"),
)
@canonize_output
def test_bt_task(local_yt, conf):
    os.environ["METRIKA_PRIVATE_KEY"] = resource.find("/fixtures/test_key")
    return do_stream_test(WatchLogImportTask, local_yt, conf)
