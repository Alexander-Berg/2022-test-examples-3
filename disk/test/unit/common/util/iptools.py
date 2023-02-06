# -*- coding: utf-8 -*-
from unittest import TestCase
from hamcrest import assert_that, equal_to
from nose_parameterized import parameterized

from mpfs.common.util.iptools import (
    IPRange,
    IPRangeList,
    ip_to_long,
)


class IPToLongTestCase(TestCase):
    @parameterized.expand([('ipv4', '192.168.2.24', 3232236056),
                           ('ipv6', '2a02:6b8:0:40c:39d1:987e:9a1:c187', 55838096689123079966870757580512280967L)])
    def test_ip_to_long(self, case, ip, expected):
        assert_that(ip_to_long(ip), equal_to(expected))


class IPRangeTestCase(TestCase):
    @parameterized.expand([('ipv4', '192.168.0.10', '192.168.0.0/26'),
                           ('ipv6', '2a02:6b8:0:40c:39d1:987e:9a1:c187', '2a02:6b8:0:40c:39d1:987e:9a1:c187/64')])
    def test_ip_in_network_range(self, case, ip, network):
        assert ip in IPRange(network)

    @parameterized.expand([('ipv4', '192.168.0.253', '192.168.0.0/26'),
                           ('ipv6', '2a02:6b8:0:FFFF:39d1:987e:9a1:c187', '2a02:6b8:0:40c:39d1:987e:9a1:c187/64')])
    def test_ip_not_in_network_range(self, case, ip, network):
        assert ip not in IPRange(network)

    @parameterized.expand([('ipv4', '10.0.2.5', '10.0.2.5'),
                           ('ipv6', '2a02:6b8:0:40c:39d1:987e:9a1:c187', '2a02:6b8:0:40c:39d1:987e:9a1:c187')])
    def test_ip_in_single_address_range(self, case, ip, single_address):
        assert ip in IPRange(single_address)

    @parameterized.expand([('ipv4', '10.1.2.5', '10.0.2.5'),
                           ('ipv6', '2a02:6b8:0:FFFF:39d1:987e:9a1:c187', '2a02:6b8:0:FFFF:39d1:AAAA:9a1:c187')])
    def test_ip_not_in_single_address_range(self, case, ip, single_address):
        assert ip not in IPRange(single_address)

    @parameterized.expand([('ipv4', '10.0.2.5',
                            '10.0.1.5', '10.0.7.5'),
                           ('ipv6', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            '2a02:6b8:0:40b:39d1:AAAA:9a1:c187', '2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187'),
                           ('ipv4_single_arg_with_spaces', '10.0.2.5',
                            '10.0.1.5 - 10.0.7.5', None),
                           ('ipv4_single_arg_without_spaces', '10.0.2.5',
                            '10.0.1.5-10.0.7.5', None),
                           ('ipv4_single_arg_with_space_after', '10.0.2.5',
                            '10.0.1.5- 10.0.7.5', None),
                           ('ipv4_single_arg_with_space_before', '10.0.2.5',
                            '10.0.1.5 -10.0.7.5', None),
                           ('ipv6_single_arg_with_spaces', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            '2a02:6b8:0:40b:39d1:AAAA:9a1:c187 - 2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None),
                           ('ipv6_single_arg_without_spaces', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            '2a02:6b8:0:40b:39d1:AAAA:9a1:c187-2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None),
                           ('ipv6_single_arg_with_space_after', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            '2a02:6b8:0:40b:39d1:AAAA:9a1:c187- 2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None),
                           ('ipv6_single_arg_with_space_before', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            '2a02:6b8:0:40b:39d1:AAAA:9a1:c187 -2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None)])
    def test_ip_in_range(self, case, ip, start, end):
        assert ip in IPRange(start, end)

    @parameterized.expand([('ipv4', '11.1.2.5',
                            '10.0.1.5', '10.0.7.5'),
                           ('ipv6', '2a02:6b8:10:FFFF:39d1:987e:9a1:c187',
                            '2a02:6b8:0:ffff:39d1:aaaa:9a1:c187', '2a02:6b8:f:ffff:39d1:aaaa:9a1:c187'),
                           ('ipv4_single_arg_with_spaces', '11.1.2.5',
                            '10.0.1.5 - 10.0.7.5', None),
                           ('ipv4_single_arg_without_spaces', '11.1.2.5',
                            '10.0.1.5-10.0.7.5', None),
                           ('ipv4_single_arg_with_space_after', '11.1.2.5',
                            '10.0.1.5- 10.0.7.5', None),
                           ('ipv4_single_arg_with_space_before', '11.1.2.5',
                            '10.0.1.5 -10.0.7.5', None),
                           ('ipv6_single_arg_with_spaces', '2a02:6b8:10:FFFF:39d1:987e:9a1:c187',
                            '2a02:6b8:0:FFFF:39d1:AAAA:9a1:c187 - 2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None),
                           ('ipv6_single_arg_without_spaces', '2a02:6b8:10:FFFF:39d1:987e:9a1:c187',
                            '2a02:6b8:0:FFFF:39d1:AAAA:9a1:c187-2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None),
                           ('ipv6_single_arg_with_space_after', '2a02:6b8:10:FFFF:39d1:987e:9a1:c187',
                            '2a02:6b8:0:FFFF:39d1:AAAA:9a1:c187- 2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None),
                           ('ipv6_single_arg_with_space_before', '2a02:6b8:10:FFFF:39d1:987e:9a1:c187',
                            '2a02:6b8:0:FFFF:39d1:AAAA:9a1:c187 -2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187', None)])
    def test_ip_not_in_range(self, case, ip, start, end):
        assert ip not in IPRange(start, end)


class IPRangeListTestCase(TestCase):
    @parameterized.expand([('ipv4_in_ipv4_only', '192.168.0.10',
                            ('192.168.17.222', '192.168.0.0/26', '192.168.20.222 - 192.168.20.253')),
                           ('ipv6_in_ipv6_only', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            ('2a0a:6b8:0:40c:39d1:987e:9a1:c187',
                             '2a02:6b8:0:40c:39d1:987e:9a1:c187/64',
                             '2a01:6b8:0:40c:39d1:987e:9a1:c187 - 2a01:6b8:0:40c:39d1:987e:9a1:ffff')),
                           ('ipv4_in_dual_stack', '192.168.0.10',
                            ('2a0a:6b8:0:40c:39d1:987e:9a1:c187',
                             # среди адресов подсети
                             '192.168.0.0/26',
                             '2a01:6b8:0:40c:39d1:987e:9a1:c187 - 2a01:6b8:0:40c:39d1:987e:9a1:ffff',
                             '192.168.20.222 - 192.168.20.253')),
                           ('ipv6_in_dual_stack', '2a02:6b8:0:40c:39d1:987e:9a1:c187',
                            ('2a0a:6b8:0:40c:39d1:987e:9a1:c187',
                             '192.168.0.0/26',
                             # в диапазоне
                             '2a02:6b8:0:40c:39d1:987a:9a1:c187 - 2a02:6b8:0:40c:39d1:987f:9a1:c187',
                             '192.168.20.222 - 192.168.20.253')),
                           ])
    def test_ip_in_range(self, case, ip, args):
        assert ip in IPRangeList(*args)

    @parameterized.expand([('ipv4_not_in_ipv4_only', '192.168.0.253',
                            ('10.0.0.2', '192.168.0.0/26', '10.0.0.2 - 10.0.0.19')),
                           ('ipv6_not_in_ipv6_only', '2a02:6b8:0:ffff:39d1:987e:9a1:c187',
                            ('2a05:6b8:0:40c:39d1:987e:9a1:c187',
                             '2a02:6b8:0:40c:39d1:987e:9a1:c187/64',
                             '2a07:6b8:0:40c:39d1:987e:9a1:c187 - 2a07:6b8:0:40c:39d1:987e:9a1:ffff')),
                           ('ipv4_not_in_dual_stack', '192.168.0.253',
                            ('10.0.0.2',
                             '10.0.0.2 - 10.0.0.19',
                             '2a02:6b8:0:40c:39d1:987e:9a1:c187/64',
                             '2a05:6b8:0:40c:39d1:987e:9a1:c187')),
                           ('ipv6_not_in_dual_stack', '2a02:6b8:0:FFFF:39d1:987e:9a1:c187',
                            ('10.0.0.2',
                             '10.0.0.2 - 10.0.0.19',
                             '2a02:6b8:0:40c:39d1:987e:9a1:c187/64',
                             '2a05:6b8:0:40c:39d1:987e:9a1:c187'))])
    def test_ip_not_in_range(self, case, ip, args):
        assert ip not in IPRangeList(*args)
