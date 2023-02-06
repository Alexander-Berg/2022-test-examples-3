# -*- coding: utf-8 -*-
import socket

from mock import patch

from rtcc.core.session import Session
from rtcc.dataprovider.dnsprovider import DnsDataProvider


def test_dns_data_provider_should_work():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        getaddrinfo_mock.return_value = [
            (socket.AF_INET, 0, 0, '', ('127.0.0.1', 0)),
            (socket.AF_INET6, 0, 0, '', ('::1', 0, 0, 0)),
        ]
        adapter = Session().register(DnsDataProvider(prefer_ipv6=False))
        host_info = adapter.get(host="somehost.yandex.ru")
        assert host_info.ipv4s == ['127.0.0.1']
        assert host_info.ipv6s == ['::1']
        assert host_info.name == 'somehost.yandex.ru'


def test_dns_data_provider_should_return_only_ipv6_by_default():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        getaddrinfo_mock.return_value = [
            (socket.AF_INET, 0, 0, '', ('127.0.0.1', 0)),
            (socket.AF_INET6, 0, 0, '', ('::1', 0, 0, 0)),
        ]
        adapter = Session().register(DnsDataProvider())
        host_info = adapter.get(host="somehost.yandex.ru")
        assert host_info.ipv4s == []
        assert host_info.ipv6s == ['::1']
        assert host_info.name == 'somehost.yandex.ru'
