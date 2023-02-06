# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.utils.caching import cache_method_result, get_package_cache_root


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


def test_get_package_cache_root():
    with replace_setting('PKG_VERSION', 'service_version'):
        result = get_package_cache_root()
        assert result == '/yandex/rasp/service_version/'
