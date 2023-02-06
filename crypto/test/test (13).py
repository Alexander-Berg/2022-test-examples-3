import os

import json
import yatest

from crypta.graph.rtmr.proto.yuid_message_pb2 import TYuidMessage
from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.lib.python.rtmr.log_serializers import (
    tskv_serializers,
)
from crypta.lib.python.rtmr.test_framework import (
    config,
    lenval_to_json,
    runner,
)


def test_parse_redir_log(tmpdir, resource_service):
    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="parse_redir_log",
        inputs=["raw_redir_log"],
        outputs=["output-fp", "output-yclid", "errors"],
        attrs={
            "NotIncremental": False,
        },
        options=config.get_options(resource_service.url_prefix, os.environ["JUGGLER_PUSH_URL_PREFIX"]),
    )

    input_dir = yatest.common.test_source_path("data/input")

    input_formatters = {
        "raw_redir_log.json": tskv_serializers.RedirLogSerializer(),
    }
    formatters = (
        lenval_to_json.ProtoToJson(TYuidMessage),  # redir rows
        lenval_to_json.ProtoToJson(TParsedBsWatchRow),  # wl rows with ysclid
        lenval_to_json.LenvalToJson(json.dumps),  # errors as raw data
    )
    output_formatters = dict(zip(manifest["output_tables"], formatters))

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "parse_redir_log", manifest)
