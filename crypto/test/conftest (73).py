import pytest

from crypta.lib.python.logbroker.test_helpers import consumer_utils


pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
]


@pytest.fixture(scope="function", autouse=True)
def setup(logbroker_client):
    consumer = logbroker_client.create_consumer()
    consumer_utils.read_all(consumer)
    consumer.stop().result()
