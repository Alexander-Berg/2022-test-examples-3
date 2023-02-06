from __future__ import print_function

import mock

from crypta.graph.data_import.rtb_log.lib.task import RTBLogTask
from crypta.lib.python.yql_runner import tests as yql_tests


@yql_tests.clean_up(observed_paths=("//home", "//logs"))
@yql_tests.load_fixtures(
    ("//home/logfeller/logs/bs-rtb-log/30min/2020-11-01T17:35:00", "/fixtures/rtb.json"),
    ("//home/crypta/develop/graph/config/allowed_sspid_gaid", "/fixtures/allowed_sspid_gaid.json"),
)
@mock.patch("crypta.graph.data_import.rtb_log.lib.task.RTBLogTask._set_expiration", lambda self: 42)
@yql_tests.canonize_output
def test_rtb_task(local_yt, conf):
    """Check RTBLogTask works"""

    result = yql_tests.do_stream_test(RTBLogTask, local_yt, conf)
    return result
