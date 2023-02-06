# import json
import os

import pytest
import yatest

from crypta.graph.rtmr.proto.deduplicator_state_pb2 import TDeduplicateReducerState
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
    if "deduplicate_reducer_input.json" in filenames
]


@pytest.mark.parametrize("test_case", TEST_CASES, ids=[path.replace(INPUT_DIR + "/", "") for path in TEST_CASES])
def test_deduplicate_reducer(tmpdir, test_case):
    empty_tables = []
    if os.path.exists(test_case + "/empty_output1"):
        empty_tables.append("Output1")
    if os.path.exists(test_case + "/empty_output2"):
        empty_tables.append("Output2")

    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="deduplicate_reducer",
        options=["--deduplicate-period=1800"],
        inputs=["deduplicate_reducer_input"],
        outputs=["Output1", "Output2"],
        attrs={
            "SupportsState": True,
            "NotIncremental": False,
        },
        expect_empty_tables=empty_tables,
        input_state=["deduplicate_reducer_state"],
        output_state=["rtcrypta_graph:deduplicate_reducer"]
    )

    input_formatters = {
        "deduplicate_reducer_input.json": json_to_lenval.JsonToLenval(),
        "deduplicate_reducer_state.json": json_to_lenval.JsonToProtoState(TDeduplicateReducerState),
    }
    output_formatters = {
        "Output1": lenval_to_json.LenvalToJson(),
        "Output2": lenval_to_json.LenvalToJson(),
        "_STATE_DATA_rtcrypta_graph:deduplicate_reducer": lenval_to_json.StateProtoToJson(TDeduplicateReducerState),
    }

    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1512345678"

    return runner.run(tmpdir, input_formatters, output_formatters, os.path.join(INPUT_DIR, test_case), "deduplicate_reducer", manifest)
