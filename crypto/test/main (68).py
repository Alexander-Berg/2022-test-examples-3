import itertools
import os
import time

from grut.libs.proto.objects.autogen import schema_pb2
import protobuf_to_dict
import requests
import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    files,
    tables,
    tests,
)
from crypta.s2s.lib import serializers
from crypta.s2s.lib.proto.process_command_pb2 import TProcessCommand
from crypta.s2s.services.conversions_processor.lib.processor import stats


COUNTER = itertools.count(1)

ETypeCode = schema_pb2.TConversionSourceMetaBase.ETypeCode


def test_conversions_processor(
    yt_stuff,
    local_conversions_processor,
    process_commands,
    process_log_logbroker_client,
    process_log_producer,
    mock_cdp_api,
    conversion_source_client,
):
    input_data = [
        "1_1100000000",
        "2_1200000000",
        "3_1300000000",
        "4_1400000000",
        "5_1500000000",
        "6_1600000000",
        "7_1700000000",
    ]

    yt_client = yt_stuff.get_yt_client()

    config = local_conversions_processor.config

    data_dir = yatest.common.test_source_path("data")
    for filename in input_data:
        files.YtFile(os.path.join(data_dir, filename), ypath.ypath_join(config.DownloadDir, filename)).write_to_local(yt_client)

    for directory, filename in (
        (config.DownloadedBackupDir, "1_1000000000"),
        (config.UploadedBackupDir, "1_1000000000-upload"),
    ):
        files.YtFile(os.path.join(data_dir, filename), ypath.ypath_join(directory, "1", filename)).write_to_local(yt_client)

    tables.YsonTable(os.path.join(data_dir, "1_1000000000-errors"), ypath.ypath_join(config.ErrorsDir, "1", "1_1000000000-errors")).write_to_local(yt_client)

    for process_command in process_commands:
        process_log_producer.write(next(COUNTER), serializers.serialize_process_command(protobuf_to_dict.dict_to_protobuf(TProcessCommand, process_command)))

    time.sleep(10)

    assert not consumer_utils.read_all(process_log_logbroker_client.create_consumer())
    assert ["4_1400000000"] == yt_client.list(config.DownloadDir)

    return {
        "downloaded_backup": _canonize(yt_client, config.DownloadedBackupDir, "downloaded_backup"),
        "uploaded_backup": _canonize(yt_client, config.UploadedBackupDir, "uploaded_backup"),
        "cdp_api": mock_cdp_api.requests,
        "grut": {
            conversion_source.meta.id: protobuf_to_dict.protobuf_to_dict(conversion_source)
            for conversion_source in conversion_source_client.select_all(attribute_selector=["/meta/id", "/meta/type_code", "/spec"])
        },
        "errors": _canonize(yt_client, config.ErrorsDir, "errors"),
        "stats": _canonize_stats(requests.get("http://{}:{}/metrics".format(config.StatsHost, config.StatsPort)).json()),
    }


def _canonize(yt_client, directory, tag):
    diff = tests.TestNodesInMapNodeChildren([tests.Diff()], tag)
    return {
        os.path.basename(item["file"]["uri"]): item
        for item in diff.teardown(cypress.CypressNode(directory), yt_client)
    }


def _canonize_stats(data):
    canondata = {}
    for item in data["sensors"]:
        labels = item["labels"]
        kind = item["kind"]
        destination = labels[stats.LabelNames.destination]
        sensor = labels[stats.LabelNames.sensor]
        if stats.LabelNames.category in labels:
            key = f"{kind}.{destination}.{labels[stats.LabelNames.category]}.{sensor}"
        else:
            key = f"{kind}.{destination}.{sensor}"

        if kind == "HIST_RATE":
            value = sum(item["hist"]["buckets"]) + item["hist"]["inf"]
        else:
            value = item["value"]

        canondata[key] = value

    return canondata
