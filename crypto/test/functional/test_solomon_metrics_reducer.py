import json

import yatest

from crypta.lib.python.rtmr.test_framework import (
    json_to_lenval,
    lenval_to_json,
    runner
)


def test_solomon_metrics_reducer(tmpdir):
    config_path = yatest.common.test_source_path("solomon_metrics_reducer.cfg")

    manifest = {
        "config": config_path,
        "dynlibs": [yatest.common.binary_path("crypta/lib/native/rtmr/rtcrypta_solomon_utils/dynlib/librtcrypta_solomon_utils-dynlib.so")],
        "input_tables": ["metrics1", "metrics2"],
        "output_tables": ["merged"],
        "input_state": [
            "state"
        ],
        "output_state": [
            "rtcrypta_solomon_utils:solomon_metrics_reducer"
        ]
    }

    input_dir = yatest.common.test_source_path("data")

    input_formatters = {
        "metrics1.json": json_to_lenval.JsonToLenval(json.dumps),
        "metrics2.json": json_to_lenval.JsonToLenval(json.dumps),
        "state.json": json_to_lenval.JsonToState(json.dumps)
    }

    output_formatters = {
        "_STATE_DATA_rtcrypta_solomon_utils:solomon_metrics_reducer": lenval_to_json.StateToJson(json.loads),
        "merged": lenval_to_json.LenvalToJson(json.loads)
    }

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "solomon_metrics_reducer", manifest, no_strict_check_output=True)
