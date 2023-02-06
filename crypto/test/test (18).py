import json

import yatest

from crypta.lib.python.rtmr.test_framework import (
    json_to_lenval,
    lenval_to_json,
    runner,
)


def test_errors_to_solomon_metrics_mapper(tmpdir):
    config_path = yatest.common.test_source_path("errors_to_solomon_metrics_mapper.cfg")

    manifest = {
        "config": config_path,
        "dynlibs": [yatest.common.binary_path("crypta/lib/native/rtmr/rtcrypta_solomon_utils/dynlib/librtcrypta_solomon_utils-dynlib.so")],
        "input_tables": [
            "parse_bs_watch_log_mapper_errors",
            "classify_host_event_mapper_errors",
            "classify_word_event_mapper_errors",
            "merge_probs_reducer_errors",
            "duid_classify_host_event_mapper_errors",
            "duid_classify_word_event_mapper_errors",
            "duid_merge_probs_reducer_errors",
        ],
        "output_tables": ["solomon_metrics"],
    }

    input_dir = yatest.common.test_source_path("data")

    input_formatters = {"{}.json".format(table): json_to_lenval.JsonToLenval() for table in manifest["input_tables"]}

    output_formatters = {
        "solomon_metrics": lenval_to_json.LenvalToJson(json.loads)
    }

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "errors_to_solomon_metrics_mapper", manifest)
