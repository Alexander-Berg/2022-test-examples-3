# -*- coding: utf-8 -*-

from travel.avia.library.python.common.utils.caching import cache_method_result


def test_cache_method_result():
    class C(object):
        call_counter = 0
        @cache_method_result
        def method(self, a, m=None):
            self.call_counter += 1
            return self.call_counter

    c = C()

    assert c.method(1, m=10) == 1
    assert c.method(1, m=10) == 1
    assert c.method(1, m=10) == 1
