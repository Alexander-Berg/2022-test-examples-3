from __future__ import unicode_literals
import time
import pytest
from mock import Mock, MagicMock, patch

from travel.avia.library.python.redis.ping import (
    RedisChecker,
    RedisServerError,
    SharedCacheChecker,
    SharedCacheServerError
)
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.api_settings import REDIS_MAXIMUM_LAG
from travel.avia.ticket_daemon_api.jsonrpc.health import (
    MysqlChecker,
    MysqlServerError
)


class HealthCheckerTest(TestCase):
    def setUp(self):
        self._fake_connection = MagicMock()
        self._fake_shared_cache = MagicMock()
        self._fake_redis_cache = MagicMock()
        self._shared_cache_check = SharedCacheChecker('ticket-daemon-api', self._fake_shared_cache)
        self._mysql_check = MysqlChecker(self._fake_connection)
        self._redis_check = RedisChecker(self._fake_redis_cache, REDIS_MAXIMUM_LAG)
        reset_all_caches()

    def test_check(self):
        storage = {}

        def fake_set(key, data, _):
            storage[key] = data

        def fake_get(key):
            return storage.get(key)

        self._fake_shared_cache.set = Mock(side_effect=fake_set)
        self._fake_shared_cache.get = Mock(side_effect=fake_get)
        self._fake_redis_cache.set = Mock(side_effect=fake_set)
        self._fake_redis_cache.get = Mock(side_effect=fake_get)
        timestamp = int(time.time() * 1000.0)
        self._fake_redis_cache.set(RedisChecker.KEY_PING_CHECK, timestamp, 0)
        checks = [self._mysql_check, self._redis_check, self._shared_cache_check]

        with patch('random.random', return_value='random_value'):
            for c in checks:
                assert c.check() == 'alive'

    def test_mcr_is_not_working(self):
        self._fake_shared_cache.set = Mock(side_effect=Exception('Boom!'))

        with pytest.raises(Exception):
            with patch('random.random', return_value='random_value'):
                self._shared_cache_check.check()

    def test_mcr_can_not_find_the_set_data(self):
        storage = {}

        def fake_set(key, data, _):
            storage[key] = data

        def fake_get(_):
            return None

        self._fake_shared_cache.set = Mock(side_effect=fake_set)
        self._fake_shared_cache.get = Mock(side_effect=fake_get)

        with pytest.raises(SharedCacheServerError):
            with patch('random.random', return_value='random_value'):
                self._shared_cache_check.check()

    def test_mysql_error(self):
        self._fake_connection.cursor = Mock(side_effect=Exception('Boom!'))

        with pytest.raises(MysqlServerError):
            self._mysql_check.check()

    def test_redis_lag_exceeding(self):
        storage = {}

        def fake_set(key, data):
            storage[key] = data

        def fake_get(key):
            return storage.get(key)

        self._fake_redis_cache.set = Mock(side_effect=fake_set)
        self._fake_redis_cache.get = Mock(side_effect=fake_get)
        timestamp = int(time.time() * 1000.0) - REDIS_MAXIMUM_LAG - 1
        self._fake_redis_cache.set('avia-backend/ping_check', timestamp)

        with pytest.raises(RedisServerError):
            self._redis_check.check()
