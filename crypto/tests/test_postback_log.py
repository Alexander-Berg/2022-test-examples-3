from __future__ import print_function

import mock

from crypta.graph.data_import.postback_log.lib.task import PostbackLogTask
from crypta.lib.python.yql_runner import tests as yql_tests


@yql_tests.clean_up(observed_paths=("//home", "//logs"))
@yql_tests.load_fixtures(
    ("//home/logfeller/logs/bs-uniform-postback-log/stream/5min/2020-11-01T17:00:00", "/fixtures/postback_log.json"),
)
@mock.patch("crypta.graph.data_import.postback_log.lib.task.PostbackLogTask._set_expiration", lambda self: 42)
@yql_tests.canonize_output
def test_postback_task(local_yt, conf):
    """Check RTBLogTask works"""

    result = yql_tests.do_stream_test(PostbackLogTask, local_yt, conf)
    return result
