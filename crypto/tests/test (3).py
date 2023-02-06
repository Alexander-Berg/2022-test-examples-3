import pytest

from crypta.graph.data_import.access_log.lib import AccessLogImportTask
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, do_stream_test


@clean_up(observed_paths=("//home", "//logs"))
@pytest.mark.usefixtures("access_log_symlinks")
@canonize_output
def test_bt_task(local_yt, conf):
    return do_stream_test(AccessLogImportTask, local_yt, conf)
