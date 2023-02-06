import pytest

from crypta.lib.python.logbroker.test_helpers import local_logbroker
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient


@pytest.fixture(scope="session")
def logbroker_port():
    return local_logbroker.get_port()


@pytest.fixture(scope="session")
def logbroker_config(request, logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "default-topic")


@pytest.fixture(scope="session")
def logbroker_client(logbroker_config):
    with SimpleLogbrokerClient(logbroker_config) as client:
        yield client


@pytest.fixture(scope="session")
def producer(logbroker_client):
    producer = logbroker_client.create_producer()
    yield producer
    producer.stop().result()


@pytest.fixture(scope="session")
def consumer(logbroker_client):
    consumer = logbroker_client.create_consumer()
    yield consumer
    consumer.stop().result()
