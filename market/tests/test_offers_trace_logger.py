# coding: utf-8
import pytest
import datetime
from hamcrest import assert_that
import yatest.common

from market.idx.pylibrary.trace_log.offers_trace_log import OffersTraceLog
from market.idx.pylibrary.trace_log.trace_log_record import TraceLogRecord
from market.idx.pylibrary.trace_log.yatf.test_env import TraceTestEnv
from market.idx.yatf.matchers.env_matchers import ContainsOfferTraceLogRecord


DATE = datetime.datetime.today()


@pytest.fixture(scope="module")
def trace_params():
    return {
        "source_host": "Source",
        "target_host": "Target",
        "target_module": "TargetModuleParent",
        "request_method": "RequestMethodParent",
        "protocol": "https",
        "http_method": "POST",
        "type": "TypeParent"
    }


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
def log_path():
    return yatest.common.output_path("offers_trace.log")


@pytest.fixture(scope="module")
def logger(log_path, trace_params):
    return OffersTraceLog(log_path, **trace_params)


@pytest.fixture(scope="module")
def test_env(log_path):
    return TraceTestEnv(log_path)


def test_writes_log_record_to_file(logger, log_path, trace_log_record, test_env):
    logger.log(trace_log_record)
    expected = {
        "date": str(DATE),
        "request_id": "123",
        "source_module": "23:32:23|SourceModule",
        "source_host": "Source",
        "target_module": "TargetModule",
        "target_host": "Target",
        "request_method": "METHOD",
        "http_code": "200",
        "retry_num": "2",
        "duration_ms": "10",
        "error_code": "I11",
        "protocol": "https",
        "http_method": "POST",
        "kv.p2": "2",
        "kv.p1": "1\n"
    }
    assert_that(test_env, ContainsOfferTraceLogRecord(expected))
