from crypta.graph.data_import.bar_navig_log.lib import BarNavigImportTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, load_fixtures, do_stream_test


@clean_up(observed_paths=("//home",))
@load_fixtures(("//home/logfeller/logs/bar-navig-log/stream/5min/2019-06-24T12:00:00", "/fixtures/barlog.json"))
@canonize_output
def test_bt_task(local_yt, conf):
    return do_stream_test(BarNavigImportTask, local_yt, conf)
