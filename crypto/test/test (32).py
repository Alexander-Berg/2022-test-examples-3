import yatest.common
from yt.wrapper import ypath

from crypta.lib.python import time_utils
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python import test_utils


def test_lal_refresher(yt_stuff, config, config_path, slow_logbroker_client, fast_logbroker_client):
    frozen_time = "1500000000"

    output_tables_tests = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/lal_refresher/bin/crypta-lookalike-lal-refresher"),
        args=[
            "--config", config_path,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("input", config.LalsPath, on_write=test_utils.lals_on_write(), yson_format="binary"), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("errors", ypath.ypath_join(config.ErrorsDir, frozen_time), on_read=test_utils.errors_on_read()), [tests.Diff()]),
        ],
        env={
            "YT_TOKEN": "FAKE",
            time_utils.CRYPTA_FROZEN_TIME_ENV: frozen_time,
        },
    )

    return {
        "output_tables": output_tables_tests,
        "slow_data_written": consumer_utils.read_all(slow_logbroker_client.create_consumer(), timeout=30),
        "fast_data_written": consumer_utils.read_all(fast_logbroker_client.create_consumer(), timeout=30),
    }
