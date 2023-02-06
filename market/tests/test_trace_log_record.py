# coding: utf-8
import pytest
import datetime
from hamcrest import assert_that, equal_to

from market.idx.pylibrary.trace_log.trace_log_record import TraceLogRecord
from market.idx.pylibrary.trace_log.trace_logger import TraceParams


DATE = datetime.datetime.today()


@pytest.fixture(scope="module")
def trace_params():
    return TraceParams(
        source_host="Source",
        target_host="Target",
        target_module="TargetModuleParent",
        request_method="RequestMethodParent",
        protocol="https",
        http_method="POST",
        type="TypeParent"
    )


@pytest.fixture(scope="module")
def trace_log_record():
    record = {
        "source_module": "SourceModule",
        "request_method": "METHOD",
        "target_module": "TargetModule",
        "datetime": DATE,
        "request_id": "123",
        "http_code": 200,
        "retry_num": 2,
        "duration_ms": 10,
        "error_code": "I11",
        "key_value_params": {
            "p1": "1",
            "p2": 2
        },
        "time_local": "23:32:23"
    }
    return TraceLogRecord(**record)


@pytest.fixture(scope="module")
def trace_log_record_with_missed_fields():
    record = {
        "source_module": "SourceModule",
        "datetime": DATE,
        "request_id": "123",
        "http_code": 200,
        "retry_num": 2,
        "duration_ms": 10,
        "error_code": "I11",
        "key_value_params": {
            "p1": "1",
            "p2": 2
        },
        "time_local": "23:32:23"
    }
    return TraceLogRecord(**record)


def test_contains_required_fields(trace_params, trace_log_record):
    expected = "tskv\ttskv_format=trace-log\t" \
               "date={}\t" \
               "request_id=123\t" \
               "source_module=23:32:23|SourceModule\t" \
               "source_host=Source\t" \
               "target_module=TargetModule\t" \
               "target_host=Target\t" \
               "request_method=METHOD\t" \
               "http_code=200\t" \
               "retry_num=2\t" \
               "duration_ms=10\t" \
               "error_code=I11\t" \
               "protocol=https\t" \
               "http_method=POST\t" \
               "kv.p2=2\t" \
               "kv.p1=1".format(DATE)
    result = trace_log_record.to_tskv(trace_params)
    assert_that(result, equal_to(expected))


def test_receives_missed_parameters_from_trace_params(trace_params, trace_log_record_with_missed_fields):
    expected = "tskv\ttskv_format=trace-log\t" \
               "date={}\t" \
               "request_id=123\t" \
               "source_module=23:32:23|SourceModule\t" \
               "source_host=Source\t" \
               "target_module=TargetModuleParent\t" \
               "target_host=Target\t" \
               "request_method=RequestMethodParent\t" \
               "http_code=200\t" \
               "retry_num=2\t" \
               "duration_ms=10\t" \
               "error_code=I11\t" \
               "protocol=https\t" \
               "http_method=POST\t" \
               "kv.p2=2\t" \
               "kv.p1=1".format(DATE)
    result = trace_log_record_with_missed_fields.to_tskv(trace_params)
    assert_that(result, equal_to(expected))
