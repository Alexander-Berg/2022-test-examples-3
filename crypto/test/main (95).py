import datetime

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.utils.merge_to_bigb_collector.lib import config_pb2
from crypta.lib.python import time_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)

FROZEN_TIME = "1600000000"


@pytest.fixture(scope="function")
def config(yt_stuff):
    proto = config_pb2.TConfig()
    proto.FreshDir = "//collector/fresh"
    proto.OutputDir = "//collector/output"
    proto.TtlDays = 1

    return proto


def test_basic(yt_stuff, config):
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/utils/merge_to_bigb_collector/bin/crypta-merge-to-bigb-collector"),
        args=[
            "--yt-proxy", yt_stuff.get_server(),
            "--yt-pool", "dummy",
            "--fresh-dir", config.FreshDir,
            "--output-dir", config.OutputDir,
            "--ttl-days", str(config.TtlDays),
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("input1.yson", yt.ypath_join(config.FreshDir, "1_1300000000")), tests.IsAbsent()),
            (tables.YsonTable("input2.yson", yt.ypath_join(config.FreshDir, "2_1400000000")), tests.IsAbsent()),
        ],
        output_tables=[(
            tables.YsonTable("output.yson", yt.ypath_join(config.OutputDir, datetime.datetime.fromtimestamp(int(FROZEN_TIME)).isoformat())),
            (tests.Diff(), tests.ExpirationTime(datetime.timedelta(days=config.TtlDays))),
        )],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: FROZEN_TIME,
        },
    )
