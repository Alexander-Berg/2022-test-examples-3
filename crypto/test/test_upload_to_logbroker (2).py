import datetime
import os
import signal
import subprocess

import pytest

import yatest.common
import yt.wrapper as yt

from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


@pytest.fixture
def lb_log_dir():
    binary = yatest.common.binary_path("logbroker/push-client/tests/test-httpd/test-httpd")
    log_dir = yatest.common.test_output_path("lb_mock")

    if not os.path.isdir(log_dir):
        os.makedirs(log_dir)

    log_file = os.path.join(log_dir, "test-httpd.log")
    cmd = [
        binary, "--ipv6",
        "--dir", log_dir,
        "--log", log_file
    ]

    process = subprocess.Popen(cmd)
    yield log_dir

    try:
        process.send_signal(signal.SIGKILL)
    except OSError:
        pass
    process.wait()


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        "yt-proxy": yt_stuff.get_server(),
        "yt-pool": "pool",
        "source-dir": "//home/crypta/xxx/source_dir",
        "drop-src": "true",
        "value-column": "value",

        "push-client-file-path": yatest.common.binary_path("logbroker/push-client/bin/push-client"),
        "logbroker-hostname": "localhost",
        "logbroker-ident": "crypta",
        "log-type": "dmp-segments-log",

        "logbroker-master-port": "9080",
        "logbroker-data-port": "9080",

        "errors-dir": "//home/crypta/xxx/errors",
        "errors-ttl": 7
    }


def make_table(config, table):
    return tables.YsonTable(table, yt.ypath_join(config["source-dir"], table))


def canonize_lb_data(log_dir, canon_file):
    with open(os.path.join(log_dir, "proc", "self", "fd", "0")) as f:
        lines = f.readlines()

    with open(canon_file, "w") as f:
        f.writelines(sorted(lines))


def test_run_upload_to_logbroker(yt_stuff, config, lb_log_dir):
    config_path = yaml_config.dump(config)
    ttl = datetime.timedelta(days=config["errors-ttl"])

    tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/utils/upload_to_logbroker/task/run_upload_to_logbroker"),
        args=[
            "--config", config_path,
            "--bin", yatest.common.binary_path("crypta/utils/upload_to_logbroker/bin/upload_to_logbroker")
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (make_table(config, "test-1"), tests.IsAbsent()),
            (make_table(config, "test-2"), tests.IsAbsent()),
            (make_table(config, "test-3"), tests.IsAbsent())
        ],
        output_tables=[
            (
                tables.YsonTable("errors", yt.ypath_join(config["errors-dir"], "*")),
                (tests.RowCount(0), tests.ExpirationTime(ttl))
            )
        ]
    )

    canon_file = yatest.common.test_output_path("recieved")
    canonize_lb_data(lb_log_dir, canon_file)
    return [yatest.common.canonical_file(canon_file, local=True)]
