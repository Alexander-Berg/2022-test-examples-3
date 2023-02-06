# coding: utf8
from contextlib import contextmanager
from uuid import uuid4

import mock

from django.conf import settings
from django.core.cache import caches, CacheHandler
from django.core.cache.backends import locmem


@contextmanager
def replace_django_cache(alias):
    """Заменяем заданный кэш на LocMemCache.

    with replace_django_cache('my_cache_name') as m_cache:
        m_cache.set(key, value, timeout)
        do_something()

    """

    local_cache = None
    name = uuid4()
    try:
        settings.CACHES[name] = {
            'BACKEND': 'django.core.cache.backends.locmem.LocMemCache',
            'LOCATION': name,
        }
        local_cache = caches[name]
        real_getitem = CacheHandler.__getitem__

        def getitem(self, item):
            if self is caches and alias == item:
                return local_cache
            else:
                return real_getitem(self, item)

        with mock.patch.object(CacheHandler, '__getitem__', getitem):
            yield local_cache

    finally:
        del settings.CACHES[name]
        if local_cache:
            del locmem._caches[name]
