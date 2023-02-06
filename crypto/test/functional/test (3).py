import json
import os

import pytest
import yatest

from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.graph.rtmr.proto.tls_reducer_state_pb2 import TTlsReducerState
from crypta.lib.python import time_utils
from crypta.lib.python.rtmr.test_framework import (
    config,
    json_to_lenval,
    lenval_to_json,
    runner,
)

INPUT_DIR = yatest.common.test_source_path("data/input")

TEST_CASES = [
    dirpath for dirpath, _, filenames in os.walk(INPUT_DIR)
    if "tls_matches_state.json" in filenames
]


@pytest.mark.parametrize("test_case", TEST_CASES, ids=[path.replace(INPUT_DIR + "/", "") for path in TEST_CASES])
def test_tls_reducer(tmpdir, test_case):
    empty_tables = ["Errors"]
    if os.path.exists(test_case + "/empty_output"):
        empty_tables.append("OutputToLogbroker")

    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="tls_reducer",
        inputs=["tls_matches"],
        outputs=["OutputToLogbroker", "Errors"],
        attrs={
            "SupportsState": True,
            "NotIncremental": False,
            "BatchTimeout": 5,
        },
        expect_empty_tables=empty_tables,
        input_state=["tls_matches_state"],
        output_state=["rtcrypta_graph:tls_reducer"]
    )

    input_formatters = {
        "tls_matches.json": json_to_lenval.JsonToProto(TParsedBsWatchRow),
        "tls_matches_state.json": json_to_lenval.JsonToProtoState(TTlsReducerState),
    }
    output_formatters = {
        "OutputToLogbroker": lenval_to_json.LenvalToJson(json.loads),
        "_STATE_DATA_rtcrypta_graph:tls_reducer": lenval_to_json.StateProtoToJson(TTlsReducerState),
    }

    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1512345678"

    return runner.run(tmpdir, input_formatters, output_formatters, os.path.join(INPUT_DIR, test_case), "tls_reducer", manifest)
