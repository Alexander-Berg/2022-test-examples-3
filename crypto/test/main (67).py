import itertools
import os
import time

import protobuf_to_dict
from grut.libs.proto.objects.autogen import schema_pb2

from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tests,
)
from crypta.s2s.lib.proto.download_command_pb2 import TDownloadCommand
from crypta.s2s.lib import serializers


COUNTER = itertools.count(1)

ETypeCode = schema_pb2.TConversionSourceMetaBase.ETypeCode


def test_conversions_downloader(
    yt_stuff,
    conversion_source_client,
    local_conversions_downloader,
    download_log_logbroker_client,
    download_log_producer,
    process_log_logbroker_client,
    valid_link_credentials,
    invalid_link_credentials,
    valid_sftp_credentials,
    invalid_sftp_credentials,
    valid_ftp_credentials,
    invalid_ftp_credentials,
    mock_solomon_server,
):
    config = local_conversions_downloader.config

    download_commands = [{
        "ConversionSource": _gen_conversion_source(1, ETypeCode.TC_LINK, {"link": valid_link_credentials}),
        "Timestamp": 1500000001,
    }, {
        "ConversionSource": _gen_conversion_source(2, ETypeCode.TC_LINK, {"link": invalid_link_credentials}),
        "Timestamp": 1500000002,
    }, {
        "ConversionSource": _gen_conversion_source(4, ETypeCode.TC_SFTP, {"sftp": valid_sftp_credentials}),
        "Timestamp": 1500000004,
    }, {
        "ConversionSource": _gen_conversion_source(5, ETypeCode.TC_SFTP, {"sftp": invalid_sftp_credentials}),
        "Timestamp": 1500000005,
    }, {
        "ConversionSource": _gen_conversion_source(6, ETypeCode.TC_APP, None),
        "Timestamp": 1500000006,
    }, {
        "ConversionSource": _gen_conversion_source(7, ETypeCode.TC_FTP, {"ftp": valid_ftp_credentials}),
        "Timestamp": 1500000007,
    }, {
        "ConversionSource": _gen_conversion_source(8, ETypeCode.TC_FTP, {"ftp": invalid_ftp_credentials}),
        "Timestamp": 1500000008,
    }, {
        "ConversionSource": _gen_conversion_source(20, ETypeCode.TC_LINK, {"link": valid_link_credentials}),
        "Timestamp": 1500000020,
    }]
    for download_command in download_commands:
        download_log_producer.write(next(COUNTER), serializers.serialize_download_command(protobuf_to_dict.dict_to_protobuf(TDownloadCommand, download_command)))

    time.sleep(20)

    download_log = consumer_utils.read_all(download_log_logbroker_client.create_consumer())
    process_log = consumer_utils.read_all(process_log_logbroker_client.create_consumer())

    return {
        "grut": {
            conversion_source.meta.id: protobuf_to_dict.protobuf_to_dict(conversion_source)
            for conversion_source in conversion_source_client.select_all(attribute_selector=["/meta/id", "/meta/type_code", "/spec"])
        },
        "yt": {
            os.path.basename(item["file"]["uri"]): item
            for item in tests.TestNodesInMapNode([tests.Diff()], "download").teardown(cypress.CypressNode(config.DownloadDir), yt_stuff.get_yt_client())
        },
        "process-log": {
            (command := serializers.deserialize_process_command(msg)).ConversionSource.meta.id: protobuf_to_dict.protobuf_to_dict(command)
            for msg in process_log
        },
        "download-log": {
            (command := serializers.deserialize_download_command(msg)).ConversionSource.meta.id: protobuf_to_dict.protobuf_to_dict(command)
            for msg in download_log
        },
        "solomon": mock_solomon_server.dump_push_requests(),
    }


def _gen_conversion_source(id_, type_code, settings):
    conversion_source = {
        "meta": {
            "id": id_,
            "type_code": type_code,
        },
        "spec": {
            "conversion_actions": [{
                "fixed": {
                    "cost": id_ * 10,
                }
            }],
            "crm_api_destination": {
                "access_uid": id_ * 1111,
                "counter_id": id_ * 11,
            },
        },
    }

    if settings is not None:
        conversion_source["spec"].update(settings)

    return conversion_source
