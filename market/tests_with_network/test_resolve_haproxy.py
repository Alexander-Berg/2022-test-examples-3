# coding: utf8
from _socket import gaierror

import pytest
from market.sre.tools.balancer_regenerate.lib.resolver import Resolver


def test_resolve_valid_fqdn():
    generator = Resolver(dc='iva')
    hosts = generator.get_hosts('GREY%market.yandex.ru')
    assert hosts == [{'name': 'fdee:fdee::2:22'}]


def test_resolve_ipv6_address():
    generator = Resolver(dc='iva')
    hosts = generator.get_hosts('GREY%2a02:6b8::2:22')
    assert hosts == [{'name': 'fdee:fdee::2:22'}]


def test_resolve_nonexistent_fqdn():
    generator = Resolver(dc='iva')
    with pytest.raises(gaierror):
        generator.get_hosts('GREY%invalid.hostname.ru')
