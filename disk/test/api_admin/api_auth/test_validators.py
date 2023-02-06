# -*- coding: utf-8 -*-
from django.test import SimpleTestCase

from django.core.exceptions import ValidationError
from hamcrest import (
    assert_that,
    calling,
    raises,
)
from nose_parameterized import parameterized

from api_admin.api_auth.validators import validate_ip_range


class ValidateIPRangeTestCase(SimpleTestCase):
    @parameterized.expand([
        ('ipv4_single', '192.168.2.24'),
        ('ipv6_single', '2a02:6b8:0:40c:39d1:987e:9a1:c187'),
        ('ipv4_block', '10.0.1.5 - 10.0.7.5'),
        ('ipv6_block', '2a02:6b8:0:40b:39d1:AAAA:9a1:c187 - 2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187'),
        ('ipv6_block_with_mixed_spaces', '2a02:6b8:0:40b:39d1:AAAA:9a1:c187 -2a02:6b8:F:FFFF:39d1:AAAA:9a1:c187'),
        ('ipv4_network', '10.234.91.17/26'),
        ('ipv6_network', 'fd50:2eef:1102:36be::/64')
    ])
    def test_positive(self, case_name, ip_range):
        validate_ip_range(ip_range)

    @parameterized.expand([
        # 256 - out of range
        ('ipv4_single_out_of_range', '192.256.2.24'),
        # 6b8ff - out of range
        ('ipv6_single_out_of_range', '2a02:6b8ff:0:40c:39d1:987e:9a1:c187'),
        ('ip_block_multiple_separators', '10.0.1.5 - 10.0.7.5 - 10.0.9.12'),
        ('ip_block_mixed_address_family', '2a02:6b8:0:40b:39d1:AAAA:9a1:c187 - 192.82.12.9'),
        ('ip_block_mixed_with_network', '192.82.12.9/22 - 192.82.12.9'),
        ('ip_network_multiple_separators', '10.234.91.17/26/22'),
        ('ip_network_too_big_mask', '10.234.91.17/35'),
        ('ip_network_with_not_existent_address', 'fd50:2eef:110ff:36be::/64'),
        ('ip_network_with_wrong_format', 'fd50:2eef:110f:::/64'),
    ])
    def test_negative(self, case_name, ip_range):
        u"""Проверяем обработку невалидных значений.

        Должно быть:
          * вызвано правильное исключение;
          * в сообщении исключения должно быть предоставлено некорректное значение

        """
        assert_that(calling(validate_ip_range).with_args(ip_range),
                    raises(ValidationError, '.*%s.*' % ip_range))
