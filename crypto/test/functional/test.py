import json
import os

import pytest
import yatest

from crypta.graph.rtmr.proto.join_fingerprints_state_pb2 import TJoinFingerprintsState
from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.graph.rtmr.proto.yuid_message_pb2 import TYuidMessage
from crypta.lib.python.rtmr.test_framework import (
    config,
    json_to_lenval,
    lenval_to_json,
    runner,
)


INPUT_DIR = yatest.common.test_source_path("data/input")

TEST_CASES = [
    dirpath for dirpath, _, filenames in os.walk(INPUT_DIR)
    if "join_fingerprints_state.json" in filenames
]


@pytest.mark.parametrize("test_case", TEST_CASES, ids=[path.replace(INPUT_DIR + "/", "") for path in TEST_CASES])
def test_join_fingerprints(tmpdir, test_case, resource_service):
    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="join_fingerprints",
        inputs=[
            "parsed_bs_watch_rows",
            "parsed_redir_rows",
        ],
        outputs=[
            "output",
            "errors"
        ],
        attrs={
            "SupportsState": True,
            "NotIncremental": False,
            "BatchTimeout": 5,
        },
        options=config.get_options(resource_service.url_prefix, os.environ["JUGGLER_PUSH_URL_PREFIX"], 1512345678),
        input_state=["join_fingerprints_state"],
        output_state=["rtcrypta_graph:join_fingerprints"],
    )

    input_formatters = {
        "parsed_redir_rows.json": json_to_lenval.JsonToProto(TYuidMessage),
        "parsed_bs_watch_rows.json": json_to_lenval.JsonToProto(TParsedBsWatchRow),
        "join_fingerprints_state.json": json_to_lenval.JsonToProtoState(TJoinFingerprintsState),
    }
    output_formatters = {
        "output": lenval_to_json.LenvalToJson(json.loads),
        "_STATE_DATA_rtcrypta_graph:join_fingerprints": lenval_to_json.StateProtoToJson(TJoinFingerprintsState),
    }

    return runner.run(tmpdir, input_formatters, output_formatters, os.path.join(INPUT_DIR, test_case), "join_fingerprints", manifest, no_strict_check_output=True)
