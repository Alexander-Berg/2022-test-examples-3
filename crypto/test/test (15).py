import json
import os
import yatest

from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.lib.python import time_utils
from crypta.lib.python.rtmr.test_framework import (
    config,
    json_to_lenval,
    lenval_to_json,
    runner,
)


def test_yclid_reducer(tmpdir):
    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="yclid_reducer",
        inputs=["yclid_matches"],
        outputs=["output", "errors"],
        attrs={
            "SupportsState": True,
            "NotIncremental": False,
        },
        expect_empty_tables=["errors"],
        output_state=["rtcrypta_graph:yclid_reducer"],
    )

    input_dir = yatest.common.test_source_path("data")
    input_formatters = {
        "yclid_matches.json": json_to_lenval.JsonToProto(TParsedBsWatchRow),
    }
    output_formatters = {
        "output": lenval_to_json.LenvalToJson(json.loads),
        "_STATE_DATA_rtcrypta_graph:yclid_reducer": lenval_to_json.StateProtoToJson(TParsedBsWatchRow),
    }

    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1512345678"
    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "yclid_reducer", manifest)
