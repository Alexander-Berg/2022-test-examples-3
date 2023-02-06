# -*- coding: utf-8 -*-
import pytest
import time

from travel.avia.library.python.ticket_daemon.memo import (
    CacheSimpleDict, CacheWithKeyTTL, memoize
)

TTL = 1


@pytest.mark.parametrize(
    'cache_for_memoize', [
        CacheSimpleDict(),
        CacheWithKeyTTL(TTL),
        # FIXME CacheInMemcache(cache, TTL),
    ],
    ids=lambda v: v.__class__.__name__
)
def test_memoize(cache_for_memoize):
    @memoize(lambda key, r: key, cache_for_memoize)
    def cat(key_arg, return_arg):
        return return_arg

    assert cat('key', 1) == 1, 'Evaluation work'
    assert cat('key', 2) == 1, 'Cache work'
    assert cat('k2', 2) == 2, 'Keyfunc work'

    cat._cache.reset()
    assert cat('key', 2) == 2, 'Reset work'
    assert cat('key', 3) == 2, 'Cache work'

    assert cat('False', False) is False, 'caching False work'


@pytest.mark.parametrize(
    'cache_with_ttl_for_memoize', [
        CacheWithKeyTTL(TTL),
        # FIXME CacheInMemcache(cache, TTL),
    ],
    ids=lambda v: v.__class__.__name__
)
def test_memoize_with_ttl_cache(cache_with_ttl_for_memoize):
    @memoize(lambda key, r: key, cache_with_ttl_for_memoize)
    def cat(key_arg, return_arg):
        return return_arg

    assert cat('key', 1) == 1, 'Evaluation work'
    assert cat('key', 2) == 1, 'Cache work'

    time.sleep(TTL + .1)
    assert cat('key', 2) == 2, 'TTL work'
    assert cat('key', 1) == 2, 'Cache work'


def test_cache_with_key_ttl_maxsize():
    @memoize(lambda key, r: key, CacheWithKeyTTL(TTL, maxsize=2))
    def cat(key_arg, return_arg):
        return return_arg
    assert cat('key', 'cached') == 'cached', 'Evaluation work'
    assert cat('key', 'new') == 'cached', 'Cache work'

    assert cat('k2', 2) == 2, 'Keyfunc work'
    assert cat('k3', 3) == 3, 'Keyfunc work'

    # First key should be dropped from cache by maxsize and lru
    assert cat('key', 'new') == 'new', 'Maxsize work'
    # Now k2 should be dropped and k3 still cached
    assert cat('k3', 'whatever') == 3, 'Lru detailed work'
