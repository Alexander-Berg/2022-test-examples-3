import json
import yatest

from crypta.graph.rtmr.proto.hit_event_data_pb2 import THitEventData
from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.lib.python import time_utils
from crypta.lib.python.rtmr.log_serializers import tskv_serializers
from crypta.lib.python.rtmr.test_framework import (
    config,
    lenval_to_json,
    json_to_lenval,
    runner,
)
import os


def get_geodata_options():
    return ["--geo-data-path {}".format(yatest.common.runtime.work_path("geodata6.bin"))]


def test_bs_event_mapper(tmpdir):
    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="bs_event_mapper",
        inputs=["raw_bs_event_log"],
        outputs=["output", "yclid", "errors"],
        attrs={
            "NotIncremental": False,
        },
        options=get_geodata_options(),
        expect_empty_tables=["errors"],
    )

    input_dir = yatest.common.test_source_path("data/input")
    input_formatters = {
        "raw_bs_event_log.json": tskv_serializers.BsEventLogSerializer(),
    }
    output_formatters = {
        "output": lenval_to_json.ProtoToJson(THitEventData),
        "yclid": lenval_to_json.ProtoToJson(TParsedBsWatchRow)
    }

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "bs_event_mapper", manifest)


def test_bs_hitlog_mapper(tmpdir, resource_service):
    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="bs_hit_mapper",
        inputs=["raw_bs_hit_log"],
        outputs=["output", "errors"],
        attrs={
            "NotIncremental": False,
        },
        options=get_geodata_options() + config.get_options(resource_service.url_prefix, os.environ["JUGGLER_PUSH_URL_PREFIX"]),
        expect_empty_tables=["errors"],
    )

    input_dir = yatest.common.test_source_path("data/input")
    input_formatters = {
        "raw_bs_hit_log.json": tskv_serializers.BsHitLogSerializer(),
    }
    output_formatters = {table: lenval_to_json.ProtoToJson(THitEventData) for table in manifest["output_tables"][:-1]}

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "bs_hit_mapper", manifest)


def test_hitlogid_reducer(tmpdir):
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1512345678"

    manifest = config.create_manifest_for_single_op(
        binary=yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
        op="hitlogid_reducer",
        inputs=["hitlogid_reducer"],
        outputs=["output", "todeduplicator", "errors"],
        attrs={
            "SupportsState": True,
            "NotIncremental": False,
        },
        options=get_geodata_options(),
        expect_empty_tables=["errors"],
        output_state=["rtcrypta_graph:hitlogid_reducer"],
    )

    input_dir = yatest.common.test_source_path("data/input")
    input_formatters = {
        "hitlogid_reducer.json": json_to_lenval.JsonToProto(THitEventData),
    }

    output_formatters = {
        "output": lenval_to_json.ProtoToJson(TParsedBsWatchRow),
        "todeduplicator": lenval_to_json.LenvalToJson(json.loads),
        "_STATE_DATA_rtcrypta_graph:hitlogid_reducer": lenval_to_json.StateProtoToJson(THitEventData),
    }

    return runner.run(tmpdir, input_formatters, output_formatters, input_dir, "hitlogid_reducer", manifest)
