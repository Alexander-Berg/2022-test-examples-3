import yatest.common

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)

SOURCE_PATH = "//input"
DEST_PATH = "//output"


def test_upload_to_bb(yt_stuff, logbroker_client, logbroker_config):
    output_tables_tests = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/adhoc/smart/tasks/upload_to_bb/bin/upload_to_bb"),
        args=[
            "--yt-proxy", yt_stuff.get_server(),
            "--yt-pool", "test_pool",
            "--lb-url", logbroker_config.host,
            "--lb-port", str(logbroker_config.port),
            "--lb-source-tvm-id", "100000",
            "--topic", logbroker_config.topic,
            "--source-path", SOURCE_PATH,
            "--dest-path", DEST_PATH,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("input", SOURCE_PATH), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("output", DEST_PATH), [tests.Diff()]),
        ],
        env={
            "YT_TOKEN": "FAKE",
        },
    )

    return {
        "output_tables": output_tables_tests,
        "data_written": consumer_utils.read_all(logbroker_client.create_consumer(), timeout=10),
    }
