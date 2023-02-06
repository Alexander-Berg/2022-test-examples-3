import os

import yatest

from crypta.graph.rtmr.proto.yuid_message_pb2 import TYuidMessage
from crypta.lib.python.rtmr.test_framework import (
    config,
    lenval_to_json,
    runner,
)


def test_parse_zen_log(tmpdir, resource_service):
    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="parse_zen_log",
        inputs=["raw_zen_log"],
        outputs=["output", "errors"],
        attrs={
            "NotIncremental": False,
        },
        options=config.get_options(resource_service.url_prefix, os.environ["JUGGLER_PUSH_URL_PREFIX"]),
        expect_empty_tables=["errors"],
    )

    input_dir = yatest.common.test_source_path("data/input")

    output_formatters = {
        "output": lenval_to_json.ProtoToJson(TYuidMessage),
    }

    return runner.run(tmpdir, {}, output_formatters, input_dir, "parse_zen_log", manifest)
