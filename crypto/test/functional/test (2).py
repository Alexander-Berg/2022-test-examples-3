import os

import json
import yatest

from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.lib.python.rtmr.log_serializers.bs_watch_log_serializer import BsWatchLogSerializer
from crypta.lib.python.rtmr.test_framework import (
    config,
    lenval_to_json,
    runner,
)


def test_parse_bs_watch_log(tmpdir, resource_service):
    geo_data_options = ["--geo-data-path {}".format(yatest.common.runtime.work_path("geodata6.bin"))]

    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="parse_bs_watch_log",
        inputs=["bs_watch_log"],
        outputs=["OutputToJoinRedir", "OutputToLogbroker", "OutputToJoinSsl", "OutputToJoinYclid", "OutputToExtFp", "OutputToExtFpDelayed", "OutputToDeduplicator", "Errors"],
        attrs={
            "NotIncremental": False,
        },
        options=geo_data_options + config.get_options(resource_service.url_prefix, os.environ["JUGGLER_PUSH_URL_PREFIX"], 1512345678),  # + ["--ext-fp-itp-filter=no"]
        expect_empty_tables=["Errors", "OutputToExtFp", "OutputToExtFpDelayed"],
    )

    input_dir = yatest.common.test_source_path("data/input")

    input_formatters = {
        "bs_watch_log.json": BsWatchLogSerializer(),
    }
    output_formatters = {
        "OutputToJoinRedir": lenval_to_json.ProtoToJson(TParsedBsWatchRow),
        "OutputToLogbroker": lenval_to_json.LenvalToJson(json.loads),
        "OutputToJoinSsl": lenval_to_json.ProtoToJson(TParsedBsWatchRow),
        "OutputToJoinYclid": lenval_to_json.ProtoToJson(TParsedBsWatchRow),
        "OutputToExtFp": lenval_to_json.LenvalToJson(json.loads),
        "OutputToExtFpDelayed": lenval_to_json.LenvalToJson(json.loads),
        "OutputToDeduplicator": lenval_to_json.LenvalToJson(json.loads),
    }

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "parse_bs_watch_log", manifest)
