from crypta.graph.data_import.export_access_log.lib import EalImportTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, load_fixtures, do_stream_test


@clean_up(observed_paths=("//home",))
@load_fixtures(("//home/logfeller/logs/export-access-log/30min/2019-06-24T12:00:00", "/fixtures/eal.json"))
@canonize_output
def test_bt_task(local_yt, conf):
    return do_stream_test(EalImportTask, local_yt, conf)
