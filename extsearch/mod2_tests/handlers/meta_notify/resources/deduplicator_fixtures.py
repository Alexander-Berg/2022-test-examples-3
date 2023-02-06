import mock
import pytest

from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.deduplicator import (
    SQSDeduplicator, KeyGetter, MessageCondition
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.meta_notify_dispatcher.message import (
    NotifySQSMessage
)
from extsearch.video.ugc.sqs_moderation.models.metrics import MetricsCollection


class MockRedis:
    mock_methods = ['watch', 'multi', 'execute']

    def __init__(self):
        self.storage = {}
        self.ttl_storage = {}
        for method in self.mock_methods:
            setattr(self, method, mock.Mock())

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        return

    def set(self, key: str, value: str):
        self.storage[key] = str(value).encode('utf8')
        return 0

    def setex(self, key: str, ttl: int, value: str):
        self.ttl_storage[key] = ttl
        self.storage[key] = str(value).encode('utf8')
        return 0

    def expire(self, key, ttl):
        self.ttl_storage[key] = ttl

    def ttl(self, key: str):
        return self.ttl_storage.get(key, -2)

    def getset(self, key: str, new_value: str):
        val = self.storage.get(key)
        self.storage[key] = str(new_value).encode('utf8')
        return val

    def get(self, key: str):
        val = self.storage.get(key)
        return val

    def delete(self, key):
        if key not in self.storage.keys():
            return
        self.storage.pop(key)

    def lock(self, key, blocking_timeout=None, *args, **kwargs):
        class Lock:
            def __init__(self, key, blocking_timeout=None):
                self.key = key
                self.blocking_timeout = blocking_timeout

            def __enter__(self):
                pass

            def __exit__(self, exc_type, exc_val, exc_tb):
                pass
        return Lock(key, blocking_timeout)

    def bitcount(self, key):
        val = self.storage.get(key)
        count = 0
        if not val:
            return count
        for ch in val:
            count += 1 if ch == '1' else 0
        return count

    def bitfield(self, key: str):

        class BFiled:
            def __init__(self, key: str, storage: dict):
                self.key = key
                self.storage = storage

            def set(self, i_type, offset, val):
                if self.key not in self.storage.keys():
                    self.storage[self.key] = []
                for _ in range(len(self.storage[self.key])-1, offset):
                    self.storage[self.key].append(0)
                self.storage[self.key][offset] = str(val).encode('utf8')

            def execute(self):
                pass

        return BFiled(key, self.storage)

    def pipeline(self):
        return self


@pytest.fixture
def mock_redis():
    r = MockRedis()
    return r


@pytest.fixture
def deduplicator(mock_redis, boto_client):
    return SQSDeduplicator(
        mock_redis, boto_client=boto_client, meta_notify_queue='meta_notify_queue', metrics=MetricsCollection()
    )


@pytest.fixture(scope='function')
def message_state(mock_redis, deduplicator_test_message):
    return MessageCondition(
        deduplicator_test_message.deduplication_info, mock_redis
    )


@pytest.fixture
def deduplicator_test_message(meta_notify_message):
    return NotifySQSMessage(meta_notify_message)


@pytest.fixture
def key_getter(deduplicator_test_message):
    return KeyGetter(deduplicator_test_message.deduplication_info)
