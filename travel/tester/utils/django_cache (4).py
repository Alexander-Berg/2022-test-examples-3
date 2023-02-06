# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from contextlib import contextmanager

import mock
from django.core.cache import caches, CacheHandler
from django.core.cache.backends.base import BaseCache, DEFAULT_TIMEOUT

from travel.rasp.library.python.common23.db.switcher import switcher


class CacheStub(BaseCache):
    def __init__(self, params=None):
        super(CacheStub, self).__init__(params or {})
        self._storage = {}

    def get(self, key, default=None, version=None):
        return self._storage.get(key, default)

    def set(self, key, value, timeout=DEFAULT_TIMEOUT, version=None):
        self._storage[key] = value

    def add(self, key, value, timeout=DEFAULT_TIMEOUT, version=None):
        if key not in self._storage:
            self._storage[key] = value

    def delete(self, key, version=None):
        if key in self._storage:
            del self._storage[key]

    def clear(self):
        self._storage = {}


@contextmanager
def replace_django_cache(alias):
    """Заменяем заданный кэш на CacheStub.

    with replace_django_cache('my_cache_name') as m_cache:
        m_cache.set(key, value, timeout)
        do_something()
    """
    cache_stub = CacheStub()
    real_getitem = CacheHandler.__getitem__

    def getitem(self, item):
        if self is caches and alias == item:
            return cache_stub
        else:
            return real_getitem(self, item)

    with mock.patch.object(CacheHandler, '__getitem__', getitem):
        yield cache_stub


def clear_cache_until_switch():
    switcher.data_updated.send(None)
