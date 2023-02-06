# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from threading import Thread

import mock
import pytest
from django.core.cache import caches
from django.core.cache.backends import locmem

from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.db import caching

from travel.rasp.library.python.common23.db.caching import (
    global_cache_set, global_cache_delete, global_cache_add, global_cache_sync_add, global_cache_sync_set, global_cache_sync_delete
)


_old_start = Thread.start


def _start_stub(self):
    """
    В тестах дожидаемся пока thread отработает полностью.
    Работает, если для thread в коде join не вызывали.
    """
    _old_start(self)
    self.join()


_old_join = Thread.join


def _join_stub(self, timeout=None):
    """
    В тестах дожидаемся пока thread отработает полностью.
    Работает, если в коде вызывается join для thread. Игнорируем таймаут, чтобы точно дождаться завершения.
    """
    _old_join(self)


class TestGlobalCache(object):
    @pytest.fixture(autouse=True)
    def auto_fixture(self):
        if hasattr(caches._caches, 'caches'):
            del caches._caches.caches
        with mock.patch('travel.rasp.library.python.common23.db.memcache_backend.MemcachedCache', locmem.LocMemCache), \
                replace_setting('CACHES', {
                    'default': {
                        'BACKEND': 'travel.rasp.library.python.common23.db.memcache_backend.MemcachedCache',
                        'TIMEOUT': 60,
                        'LONG_TIMEOUT': 60 * 60,
                        'LOCATION': 'uniq_1'  # https://docs.djangoproject.com/en/1.11/topics/cache/#local-memory-caching
                    },
                    'other_dc_cache': {
                        'BACKEND': 'travel.rasp.library.python.common23.db.memcache_backend.MemcachedCache',
                        'TIMEOUT': 60,
                        'LONG_TIMEOUT': 60 * 60,
                        'LOCATION': 'uniq_2'
                    },
                }):
            assert isinstance(caches['default'], locmem.LocMemCache)
            assert isinstance(caches['other_dc_cache'], locmem.LocMemCache)
            yield
            caches['default'].clear()
            caches['other_dc_cache'].clear()
            if hasattr(caches._caches, 'caches'):
                del caches._caches.caches

    @mock.patch.object(caching.Thread, 'start', _start_stub)
    def test_add(self):
        global_cache_add('a', 'value_a')
        assert caches['default'].get('a') == 'value_a'
        assert caches['other_dc_cache'].get('a') == 'value_a'
        global_cache_add('b', 'value_b')
        global_cache_add('b', 'new_value_b')
        assert caches['default'].get('b') == 'value_b'
        assert caches['other_dc_cache'].get('b') == 'value_b'

        with replace_setting('ENABLE_REDIS_CACHE', True):
            global_cache_add('c', 'value_c')
            assert caches['default'].get('c') == 'value_c'
            assert caches['other_dc_cache'].get('c') is None

            global_cache_add('c', 'new_value_c')
            assert caches['default'].get('c') == 'value_c'
            assert caches['other_dc_cache'].get('c') is None

    @mock.patch.object(caching.Thread, 'start', _start_stub)
    def test_set(self):
        global_cache_set('a', 'value_a')
        assert caches['default'].get('a') == 'value_a'
        assert caches['other_dc_cache'].get('a') == 'value_a'
        global_cache_set('b', 'value_b')
        global_cache_set('b', 'new_value_b')
        assert caches['default'].get('b') == 'new_value_b'
        assert caches['other_dc_cache'].get('b') == 'new_value_b'

        with replace_setting('ENABLE_REDIS_CACHE', True):
            global_cache_set('c', 'value_c')
            assert caches['default'].get('c') == 'value_c'
            assert caches['other_dc_cache'].get('c') is None

            global_cache_set('c', 'new_value_c')
            assert caches['default'].get('c') == 'new_value_c'
            assert caches['other_dc_cache'].get('c') is None

    @mock.patch.object(caching.Thread, 'start', _start_stub)
    def test_delete(self):
        global_cache_set('a', 'value_a')
        global_cache_add('b', 'value_b')

        global_cache_delete('a')
        assert caches['default'].get('a') is None
        assert caches['other_dc_cache'].get('a') is None
        global_cache_delete('b')
        assert caches['default'].get('b') is None
        assert caches['other_dc_cache'].get('b') is None

        with replace_setting('ENABLE_REDIS_CACHE', True):
            global_cache_set('c', 'value_c')
            global_cache_delete('c')
            assert caches['default'].get('c') is None
            assert caches['other_dc_cache'].get('c') is None

    @mock.patch.object(caching.Thread, 'join', _join_stub)
    def test_sync_add(self):
        global_cache_sync_add('a', 'value_a')
        assert caches['default'].get('a') == 'value_a'
        assert caches['other_dc_cache'].get('a') == 'value_a'
        global_cache_sync_add('b', 'value_b')
        global_cache_sync_add('b', 'new_value_b')
        assert caches['default'].get('b') == 'value_b'
        assert caches['other_dc_cache'].get('b') == 'value_b'

    @mock.patch.object(caching.Thread, 'join', _join_stub)
    def test_sync_set(self):
        global_cache_sync_set('a', 'value_a')
        assert caches['default'].get('a') == 'value_a'
        assert caches['other_dc_cache'].get('a') == 'value_a'
        global_cache_sync_set('b', 'value_b')
        global_cache_sync_set('b', 'new_value_b')
        assert caches['default'].get('b') == 'new_value_b'
        assert caches['other_dc_cache'].get('b') == 'new_value_b'

    @mock.patch.object(caching.Thread, 'join', _join_stub)
    def test_sync_delete(self):
        global_cache_set('a', 'value_a')
        global_cache_add('b', 'value_b')

        global_cache_sync_delete('a')
        assert caches['default'].get('a') is None
        assert caches['other_dc_cache'].get('a') is None
        global_cache_sync_delete('b')
        assert caches['default'].get('b') is None
        assert caches['other_dc_cache'].get('b') is None
