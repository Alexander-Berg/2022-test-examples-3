import pytest

from crypta.lib.python import test_utils
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient


pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def access_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "access-log")


@pytest.fixture(scope="session")
def change_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "change-log")


@pytest.fixture(scope="session")
def describe_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "describe-log")


@pytest.fixture(scope="session")
def describe_slow_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "describe-slow-log")


@pytest.fixture(scope="session")
def segmentate_log_logbroker_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "segmentate-log")


@pytest.fixture(scope="session")
def change_log_logbroker_client(change_log_logbroker_config):
    with SimpleLogbrokerClient(change_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def describe_log_logbroker_client(describe_log_logbroker_config):
    with SimpleLogbrokerClient(describe_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def describe_slow_log_logbroker_client(describe_slow_log_logbroker_config):
    with SimpleLogbrokerClient(describe_slow_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def segmentate_log_logbroker_client(segmentate_log_logbroker_config):
    with SimpleLogbrokerClient(segmentate_log_logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def change_log_producer(change_log_logbroker_client):
    producer = change_log_logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def describe_log_producer(describe_log_logbroker_client):
    producer = describe_log_logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def describe_slow_log_producer(describe_slow_log_logbroker_client):
    producer = describe_slow_log_logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def segmentate_log_producer(segmentate_log_logbroker_client):
    producer = segmentate_log_logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def mock_sandbox_server():
    with test_utils.mock_sandbox_server_with_udf("CRYPTA_SAMPLER_UDF", "yql/udfs/crypta/sampler/libcrypta_sampler_udf.so") as mock:
        yield mock
