import json

import protobuf_to_dict
import yatest.common

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.s2s.lib import serializers


def test_scheduler(
    config_path,
    config,
    download_log_logbroker_client,
    conversion_source_client,
    mock_solomon_server,
):
    binary = yatest.common.binary_path("crypta/s2s/services/scheduler/bin/crypta-s2s-scheduler")
    yatest.common.execute([binary, "--config", config_path], stdout=None, stdin=None)
    download_commands = [serializers.deserialize_download_command(m) for m in consumer_utils.read_all(download_log_logbroker_client.create_consumer())]

    return {
        "grut": {
            conversion_source.meta.id: protobuf_to_dict.protobuf_to_dict(conversion_source)
            for conversion_source in conversion_source_client.select_all(attribute_selector=["/meta/id", "/meta/type_code", "/spec"])
        },
        "logbroker": {
            download_command.ConversionSource.meta.id: protobuf_to_dict.protobuf_to_dict(download_command)
            for download_command in download_commands
        },
        "solomon": dict([_serialize_solomon_request(request) for request in mock_solomon_server.dump_requests()]),
    }


def _serialize_solomon_request(request):
    args = request["args"]

    project = args["project"][0]
    cluster = args["cluster"][0]
    service = args["service"][0]

    key = f"{project}_{cluster}_{service}"
    value = json.loads(request["request_data"])

    return key, value
