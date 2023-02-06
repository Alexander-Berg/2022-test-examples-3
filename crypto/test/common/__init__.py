import json
import os

import yatest

from crypta.graph.rtmr.proto.deduplicator_state_pb2 import TDeduplicateReducerState
from crypta.graph.rtmr.proto.hit_event_data_pb2 import THitEventData
from crypta.graph.rtmr.proto.join_fingerprints_state_pb2 import TJoinFingerprintsState
from crypta.graph.rtmr.proto.parsed_bs_watch_row_pb2 import TParsedBsWatchRow
from crypta.graph.rtmr.proto.tls_reducer_state_pb2 import TTlsReducerState
from crypta.graph.rtmr.proto.yuid_message_pb2 import TYuidMessage


from crypta.lib.python import templater
from crypta.lib.python.rtmr.log_serializers import (
    bs_watch_log_serializer,
    tskv_serializers,
)
from crypta.lib.python.rtmr.test_framework import (
    lenval_to_json,
    json_to_lenval,
)
from rtmapreduce.config.user_tasks import rtcrypta_graph


def sort_solomon_output(value):
    result = json.loads(value)
    result["sensors"] = sorted(result["sensors"])
    return result


TASK = "graph"

INPUT_FORMATTERS = {
    "bs_event_log.json" : tskv_serializers.BsEventLogSerializer(),
    "bs_hit_log.json" : tskv_serializers.BsHitLogSerializer(),
    "bs_watch_log.json": bs_watch_log_serializer.BsWatchLogSerializer(),
    "join_fingerprints_state.json": json_to_lenval.JsonToProtoState(TJoinFingerprintsState),
    "hitlogid_reducer_state.json": json_to_lenval.JsonToProtoState(THitEventData),
    "raw_redir_log.json": tskv_serializers.RedirLogSerializer(),
}

OUTPUT_FORMATTERS = {
    rtcrypta_graph.FINGERPRINT_MATCHES: lenval_to_json.LenvalToJson(json.loads),
    rtcrypta_graph.TO_DEDUPLICATOR: lenval_to_json.LenvalToJson(json.loads),
    rtcrypta_graph.TO_FPEXTMATCHER: lenval_to_json.LenvalToJson(json.loads),
    rtcrypta_graph.TO_FPEXTMATCHER_DELAYED: lenval_to_json.LenvalToJson(json.loads),
    rtcrypta_graph.PARSED_BS_HIT_EVENT_ROWS: lenval_to_json.ProtoToJson(THitEventData),
    rtcrypta_graph.TLS_MATCHES: lenval_to_json.ProtoToJson(TParsedBsWatchRow),
    rtcrypta_graph.YCLID_MATCHES: lenval_to_json.ProtoToJson(TParsedBsWatchRow),
    rtcrypta_graph.PARSED_WITH_YUID: lenval_to_json.ProtoToJson(TYuidMessage),

    "_STATE_DATA_rtcrypta_graph:join_fingerprints": lenval_to_json.StateProtoToJson(TJoinFingerprintsState),
    "_STATE_DATA_rtcrypta_graph:tls_reducer": lenval_to_json.StateProtoToJson(TTlsReducerState),
    "_STATE_DATA_rtcrypta_graph:hitlogid_reducer": lenval_to_json.StateProtoToJson(THitEventData),
    "_STATE_DATA_rtcrypta_graph:yclid_reducer": lenval_to_json.StateProtoToJson(TParsedBsWatchRow),
    "_STATE_DATA_rtcrypta_graph:deduplicate_reducer": lenval_to_json.StateProtoToJson(TDeduplicateReducerState),

    rtcrypta_graph.JOIN_FINGERPRINTS_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_ADSTAT_LOG_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_BS_WATCH_LOG_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_REDIR_LOG_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_ZEN_LOG_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_EXTFP_MATCH_LOG_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.TLS_REDUCER_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.YCLID_REDUCER_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_BS_EVENT_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.PARSE_BS_HIT_ERRORS: lenval_to_json.ErrorsToJson(),
    rtcrypta_graph.HITLOGID_REDUCER_ERRORS: lenval_to_json.ErrorsToJson(),

    rtcrypta_graph.SOLOMON_METRICS_MERGED: lenval_to_json.LenvalToJson(sort_solomon_output),
}


def get_input_dir():
    return yatest.common.source_path("crypta/graph/rtmr/test/data/input")


def create_manifest(resource_service_url_prefix, template_path=None, extra_vars=None):
    template_path = template_path or yatest.common.source_path("crypta/graph/rtmr/test/data/graph.cfg")
    vars = {
        "resource_service_url_prefix": resource_service_url_prefix,
        "juggler_url_prefix": os.environ["JUGGLER_PUSH_URL_PREFIX"],
        "geo_data_path": yatest.common.runtime.work_path("geodata6.bin"),
    }
    if extra_vars:
        vars.update(extra_vars)

    config_path = yatest.common.test_output_path(os.path.basename(template_path))
    templater.render_file(template_path, config_path, vars)

    manifest = {
        "config": config_path,
        "dynlibs": [
            yatest.common.binary_path("crypta/graph/rtmr/dynlib/librtcrypta_graph-dynlib.so"),
            yatest.common.binary_path("crypta/lib/native/rtmr/rtcrypta_solomon_utils/dynlib/librtcrypta_solomon_utils-dynlib.so"),
        ],
        "input_tables": [
            "bs_event_log",
            "bs_hit_log",
            "bs_watch_log",
            "raw_adstat_log",
            "raw_extfp_match_log",
            "raw_redir_log",
            "raw_zen_log",
        ],
        "output_tables": [
            rtcrypta_graph.FINGERPRINT_MATCHES,
            rtcrypta_graph.TO_FPEXTMATCHER,
            rtcrypta_graph.TO_FPEXTMATCHER_DELAYED,
            rtcrypta_graph.TO_DEDUPLICATOR,
            rtcrypta_graph.JOIN_FINGERPRINTS_ERRORS,
            rtcrypta_graph.PARSE_BS_WATCH_LOG_ERRORS,
            rtcrypta_graph.PARSED_BS_HIT_EVENT_ROWS,
            rtcrypta_graph.PARSE_BS_HIT_ERRORS,
            rtcrypta_graph.PARSE_BS_EVENT_ERRORS,
            rtcrypta_graph.PARSE_REDIR_LOG_ERRORS,
            rtcrypta_graph.PARSE_ZEN_LOG_ERRORS,
            rtcrypta_graph.PARSE_EXTFP_MATCH_LOG_ERRORS,
            rtcrypta_graph.SOLOMON_METRICS_MERGED,
            rtcrypta_graph.TLS_REDUCER_ERRORS,
            rtcrypta_graph.YCLID_REDUCER_ERRORS,
            rtcrypta_graph.HITLOGID_REDUCER_ERRORS,
            rtcrypta_graph.TLS_MATCHES,
            rtcrypta_graph.YCLID_MATCHES,
            rtcrypta_graph.PARSED_WITH_YUID,
            rtcrypta_graph.PARSED_BS_HIT_EVENT_ROWS,
        ],
        "input_state": [
            "join_fingerprints_state",
            "hitlogid_reducer_state",
        ],
        "output_state": [
            "rtcrypta_graph:join_fingerprints",
            "rtcrypta_graph:tls_reducer",
            "rtcrypta_graph:hitlogid_reducer",
            "rtcrypta_graph:yclid_reducer",
            "rtcrypta_graph:deduplicate_reducer",
        ]
    }

    for path in manifest["output_tables"]:
        dir_name = yatest.common.test_output_path(os.path.dirname(path))
        if not os.path.isdir(dir_name):
            os.makedirs(dir_name)

    return manifest
