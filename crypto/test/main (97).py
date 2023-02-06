import pytest
import yatest.common

from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


EVENT_LOG_TABLE = "//xxx/event_log"
TMP_DIR = "//xxx/tmp"


class LogTest(tests.YtTest):
    def __init__(self, target_filename):
        super(LogTest, self).__init__()
        self.target_filename = target_filename

    def teardown(self, stdout_filename, yt_stuff):
        with open(stdout_filename) as f:
            lines = f.readlines()[3:]
        with open(self.target_filename, "w") as f:
            f.writelines(sorted(lines))
        return [yatest.common.canonical_file(self.target_filename, local=True)]


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        "yt_proxy": yt_stuff.get_server(),
        "yt_pool": "pool",
        "yt_tmp_dir": TMP_DIR,
        "fake_hostname_for_graphite": "xxx.local",
        "send_data_to_graphite": False,
        "event_log_table": EVENT_LOG_TABLE,
        "pool_regexp": ".*xxx.*",
        "time_range_hours": 1,
        "row_range": 4
    }


def test_yt_stats(yt_stuff, config):
    yt_stuff.yt_wrapper.create("map_node", TMP_DIR, recursive=True)

    config_path = yatest.common.test_output_path("config.yaml")
    log_config_path = yatest.common.test_source_path("data/logger.conf")
    stdout_file = yatest.common.test_output_path("stdout.txt")
    input_table_file = yatest.common.test_source_path("data/event_log.yson")

    yaml_config.dump(config, config_path)

    result = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/utils/yt_stats/bin/crypta-yt-stats"),
        args=[
            "--config", config_path,
            "--log-config", log_config_path
        ],
        input_tables=[
            (tables.YsonTable(input_table_file, EVENT_LOG_TABLE), tests.TableIsNotChanged())
        ],
        output_tables=[],
        stdout_fname=stdout_file,
        stdout_test=LogTest(yatest.common.test_output_path("trimmed_stdout.txt")),
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: "1527246001",
            "YT_TOKEN": "XXX"
        }
    )
    assert not yt_stuff.yt_wrapper.list(TMP_DIR)
    return result
