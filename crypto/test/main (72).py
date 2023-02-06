from datetime import datetime

from grut.libs.proto.objects.autogen import schema_pb2
import protobuf_to_dict
import pytest

from crypta.s2s.services.scheduler.lib import scheduler
from crypta.s2s.lib.proto.download_command_pb2 import TDownloadCommand


def test_make_filter():
    return scheduler.make_filter(150000000)


@pytest.mark.parametrize("conversion_source,current_datetime,result_datetime", [
    pytest.param(
        {"meta": {"id": 1}},
        datetime(year=2020, month=1, day=10, hour=14),
        datetime(year=2020, month=1, day=10, hour=1),
        id="before",
    ),
    pytest.param(
        {"meta": {"id": 47}},
        datetime(year=2020, month=1, day=10, hour=14),
        datetime(year=2020, month=1, day=9, hour=23),
        id="after",
    )
])
def test_generate_last_scheduled_time(conversion_source, current_datetime, result_datetime):
    assert result_datetime.timestamp() == scheduler.generate_last_scheduled_time(_conversion_source_from_dict(conversion_source), current_datetime)


@pytest.mark.parametrize("conversion_source,current_datetime,result", [
    pytest.param(
        {
            "meta": {"id": 1, "type_code": 5},
            "spec": {
                "google_sheets": {"url": "xxx"},
                "crm_api_destination": {"counter_id": 1111, "access_uid": 4444},
                "processing_info": {
                    "next_scheduled_time": int(datetime(year=2020, month=1, day=1, hour=1).timestamp()),
                },
            },
        },
        datetime(year=2020, month=1, day=10, hour=14),
        {
            "meta": {"id": 1, "type_code": 5},
            "spec": {
                "processing_info": {
                    "last_scheduled_time": int(datetime(year=2020, month=1, day=10, hour=1).timestamp()),
                },
            },
        },
        id="basic",
    ),
])
def test_make_conversion_source_update(conversion_source, current_datetime, result):
    conversion_source = _conversion_source_from_dict(conversion_source)
    result = _conversion_source_from_dict(result)
    assert result == scheduler.make_conversion_source_update(conversion_source, current_datetime)


@pytest.mark.parametrize("conversion_source,timestamp,result", [
    pytest.param(
        {
            "meta": {"id": 1, "type_code": 5},
            "spec": {
                "google_sheets": {"url": "xxx"},
                "crm_api_destination": {"counter_id": 1111, "access_uid": 4444},
                "processing_info": {
                    "next_scheduled_time": 1700000000,
                },
            },
        },
        1500000000,
        {
            "ConversionSource": {
                "meta": {"id": 1, "type_code": 5},
                "spec": {
                    "google_sheets": {"url": "xxx"},
                    "crm_api_destination": {"counter_id": 1111, "access_uid": 4444},
                },
            },
            "Timestamp": 1500000000,
        },
        id="basic",
    ),
])
def test_make_download_command(conversion_source, timestamp, result):
    assert _download_command_from_dict(result) == scheduler.make_download_command(_conversion_source_from_dict(conversion_source), timestamp)


def _conversion_source_from_dict(dict_):
    return protobuf_to_dict.dict_to_protobuf(schema_pb2.TConversionSource, dict_)


def _download_command_from_dict(dict_):
    return protobuf_to_dict.dict_to_protobuf(TDownloadCommand, dict_)
