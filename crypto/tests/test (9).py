from crypta.graph.data_import.redir_log.lib import RedirLogImportTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, do_stream_test, load_fixtures


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(("//home/logfeller/logs/common-redir-log/stream/5min/2019-06-24T12:00:00", "/fixtures/redir.json"))
@canonize_output
def test_bt_task(local_yt, conf):
    return do_stream_test(RedirLogImportTask, local_yt, conf)
