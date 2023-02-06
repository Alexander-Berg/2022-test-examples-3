# -*- coding: utf-8 -*-
import datetime
import json
import socket

import pytest
import requests
from mock import Mock, patch, call

from rtcc import settings
from rtcc.dataprovider.dnsprovider import DnsDataProvider
from rtcc.dataprovider.hwdata import HWData
from rtcc.dataprovider.hwdata import host_id
from rtcc.dataprovider.topology.runtime import RuntimeInstanceResolver
from rtcc.dataprovider.topology.user import UserInstanceResolver
from rtcc.model.grouping import LocationGrouppingPolicy
from rtcc.model.grouping import NoneGroupingPolicy
from rtcc.model.raw import Instance

try:
    from library.sky.hostresolver.resolver import Resolver
except ImportError:
    if not settings.LOCAL:
        raise


def test_grouping():
    with patch.object(HWData, '_init_hwdata') as hwdata_mock:
        hwdata_mock.return_value = {host_id("somehost", ".yandex.ru"): {"dc": "man", "cpu": "dummy_value"},
                                    host_id("anotherhost", ".yandex.ru"): {"dc": "sas", "cpu": "dummy_value"}, }

        instance1 = Instance("somehost.yandex.ru", "8080")
        instance2 = Instance("anotherhost.yandex.ru", "8080")
        grouping = LocationGrouppingPolicy(HWData())
        assert len(grouping.groups([instance1, instance2])) == 2


def test_grouping_none():
    with patch.object(HWData, '_init_hwdata') as hwdata_mock:
        hwdata_mock.return_value = {host_id("somehost", ".yandex.ru"): {"dc": "man", "cpu": "dummy_value"},
                                    host_id("anotherhost", ".yandex.ru"): {"dc": "sas", "cpu": "dummy_value"}, }

        instance1 = Instance("somehost.yandex.ru", "8080")
        instance2 = Instance("anotherhost.yandex.ru", "8080")
        grouping = NoneGroupingPolicy(HWData())
        assert len(grouping.groups([instance1, instance2])) == 1


def test_grouping_extended():
    with patch.object(HWData, '_init_hwdata') as hwdata_mock:
        hwdata_mock.return_value = {host_id("somehost", ".yandex.ru"): {"dc": "man", "cpu": "dummy_value"},
                                    host_id("anotherhost", ".yandex.ru"): {"dc": "sas", "cpu": "dummy_value"},
                                    host_id("someanotherhost", ".yandex.ru"): {"dc": "sas", "cpu": "dummy_value"}, }

        instance1 = Instance("somehost.yandex.ru", "8080")
        instance2 = Instance("anotherhost.yandex.ru", "8080")
        instance3 = Instance("someanotherhost.yandex.ru", "8080")
        grouping = LocationGrouppingPolicy(HWData())
        assert len(grouping.groups([instance1, instance2, instance3])) == 2
        assert len(grouping.groups([instance1, instance2, instance3])["SAS"]) == 2


def test_user_resolve():
    resolver = UserInstanceResolver()
    instances = resolver.resolve("localhost", "12000")
    groups = NoneGroupingPolicy(HWData()).groups(instances)
    assert len(groups) == 1


def test_ip_resolver_should_work():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        getaddrinfo_result = [(socket.AF_INET, None, None, None, ('127.0.0.1',))]
        getaddrinfo_mock.return_value = getaddrinfo_result
        host_info = DnsDataProvider().get(host='yandex.ru')
        assert host_info.ipv4s == ['127.0.0.1']


def test_ip_resolver_should_be_resistant_to_errors():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        max_fail_attempts = settings.IP_RESOLVER_MAX_ATTEMPTS - 1
        getaddrinfo_result = [(socket.AF_INET, None, None, None, ('127.0.0.2',))]
        getaddrinfo_mock.side_effect = [socket.gaierror] * max_fail_attempts + [getaddrinfo_result]
        host_info = DnsDataProvider().get(host='yandex.ru')
        assert host_info.ipv4s == ['127.0.0.2']


def test_ip_resolver_should_prefer_ipv6_over_ipv4_by_default():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        getaddrinfo_result = [
            (socket.AF_INET, None, None, None, ('127.0.0.3',)),
            (socket.AF_INET6, None, None, None, ('::1',)),
        ]
        getaddrinfo_mock.return_value = getaddrinfo_result
        host_info = DnsDataProvider().get(host='yandex.ru')
        assert host_info.ipv4s == []
        assert host_info.ipv6s == ['::1']


def test_ip_resolver_may_not_prefer_ipv6_over_ipv4():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        getaddrinfo_result = [
            (socket.AF_INET, None, None, None, ('127.0.0.3',)),
            (socket.AF_INET6, None, None, None, ('::1',)),
        ]
        getaddrinfo_mock.return_value = getaddrinfo_result
        host_info = DnsDataProvider(prefer_ipv6=False).get(host='yandex.ru')
        assert host_info.ipv4s == ['127.0.0.3']
        assert host_info.ipv6s == ['::1']


def test_ip_resolver_should_use_ipv6_host_name_by_default():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        getaddrinfo_result_ipv4 = (socket.AF_INET, None, None, None, ('127.0.0.4',))
        getaddrinfo_result_ipv6 = (socket.AF_INET6, None, None, None, ('::2',))
        getaddrinfo_mock.return_value = [getaddrinfo_result_ipv4, getaddrinfo_result_ipv6]
        host_info = DnsDataProvider().get(host='host.yandex.ru')
        assert host_info.ipv4s == []
        assert host_info.ipv6s == ['::2']
        getaddrinfo_mock.assert_has_calls([
            call('host.search.yandex.net', None, socket.AF_UNSPEC, socket.SOCK_RAW),
        ])


def test_ip_resolver_may_use_all_possible_host_names():
    with patch('socket.getaddrinfo') as getaddrinfo_mock:
        socket_result_ipv4 = [(socket.AF_INET, None, None, None, ('127.0.0.4',))]
        socket_result_ipv6 = [(socket.AF_INET6, None, None, None, ('::2',))]
        getaddrinfo_mock.side_effect = [socket_result_ipv4, socket_result_ipv6]
        host_info = DnsDataProvider(prefer_ipv6=False).get(host='host.yandex.ru')
        assert host_info.ipv4s == ['127.0.0.4']
        assert host_info.ipv6s == ['::2']
        getaddrinfo_mock.assert_has_calls([
            call('host.yandex.ru', None, socket.AF_UNSPEC, socket.SOCK_RAW),
            call('host.search.yandex.net', None, socket.AF_UNSPEC, socket.SOCK_RAW),
        ])


def test_runtime_resolver_should_work():
    with patch('requests.get') as requests_mock:
        requests_result = Mock(content=json.dumps({
            'instances': [
                {
                    'host': 'sas-111.search.yandex.net',
                    'port': 8888,
                    'extra': {
                        'shard': 'none',
                    }
                },
            ],
            'meta': {
                'timestamp': int(datetime.datetime(2015, 12, 31, 23, 59, 59).strftime('%s')),
                'backend': {
                    'host': 'wsXX-YYY.search.yandex.net',
                    'port': '10210',
                }
            },
        }))
        requests_mock.return_value = requests_result
        resolver = RuntimeInstanceResolver()
        result = resolver.resolve('C@HEAD . I@ALL')
        assert result == [Instance('sas-111.search.yandex.net', '8888')]


def test_runtime_resolver_should_be_resistant_to_errors():
    with patch('requests.get') as requests_mock:
        max_fail_attempts = settings.IP_RESOLVER_MAX_ATTEMPTS - 1
        requests_result = Mock(content=json.dumps({
            'instances': [
                {
                    'host': 'sas-222.search.yandex.net',
                    'port': 8888,
                    'extra': {
                        'shard': 'none',
                    }
                },
            ],
            'meta': {
                'timestamp': int(datetime.datetime(2015, 12, 31, 23, 59, 59).strftime('%s')),
                'backend': {
                    'host': 'wsXX-YYY.search.yandex.net',
                    'port': '10210',
                }
            },
        }))
        requests_mock.side_effect = [requests.RequestException] * max_fail_attempts + [requests_result]
        resolver = RuntimeInstanceResolver()
        result = resolver.resolve('C@HEAD . I@ALL')
        assert result == [Instance('sas-222.search.yandex.net', '8888')]


def test_runtime_resolver_should_fail_if_it_does_not_work():
    with patch('requests.get') as requests_mock:
        max_fail_attempts = settings.RUNTIME_RESOLVER_MAX_ATTEMPTS
        requests_mock.side_effect = [requests.RequestException] * max_fail_attempts
        resolver = RuntimeInstanceResolver()
        with pytest.raises(requests.RequestException):
            resolver.resolve('C@HEAD . I@ALL')
