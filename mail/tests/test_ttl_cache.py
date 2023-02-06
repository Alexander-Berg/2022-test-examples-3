import time

from django.test import TestCase

from fan.utils.cache import ttl_cache

_go = False


class TTLCacheTestCase(TestCase):
    def test_ttl_cache_no_exception(self):
        @ttl_cache(maxsize=128, ttl=1)
        def func(key):
            return key

        self.assertEqual(func(1), 1)
        self.assertEqual(func(1), 1)
        self.assertEqual(func(1), 1)

        self.assertEqual(func.cache_info().misses, 1)
        self.assertEqual(func.cache_info().hits, 2)

        # проверим, что после указанного времени кэш заэкспайрился
        time.sleep(1.1)
        self.assertEqual(func(1), 1)
        self.assertEqual(func.cache_info().hits, 2)
        self.assertEqual(func.cache_info().misses, 2)

    def test_ttl_cache_exception(self):
        global _go

        @ttl_cache(maxsize=128, ttl=1, use_stale_on=(Exception,))
        def func(key):
            global _go
            if _go:
                raise Exception()
            return key

        self.assertEqual(func(1), 1)
        time.sleep(1.1)

        _go = True
        self.assertEqual(func(1), 1)
        self.assertEqual(func.cache_info().stales, 1)
